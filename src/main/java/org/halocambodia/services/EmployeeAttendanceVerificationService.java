package org.halocambodia.services;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.halocambodia.data.*;
import org.halocambodia.enums.AttendanceEntryStatus;
import org.halocambodia.enums.AttendanceQcStatus;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.attendance_management.AttendanceVerificationView;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

@Service
public class EmployeeAttendanceVerificationService {
    private static final int ASSOCIATION_BATCH_SIZE = 500;
    private final EmployeeAttendanceDetailRepository repository;
    private final EmployeeRepository employeeRepository;
    private final AuthenticatedUser authenticatedUser;
    private final PayrollAttendanceLockService attendanceLockService;

    public EmployeeAttendanceVerificationService(EmployeeAttendanceDetailRepository repository,
            EmployeeRepository employeeRepository, AuthenticatedUser authenticatedUser,
            PayrollAttendanceLockService attendanceLockService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.authenticatedUser = authenticatedUser;
        this.attendanceLockService = attendanceLockService;
    }

    @Transactional
    public List<EmployeeAttendanceDetail> findForVerification(LocalDate start, LocalDate end) {
        requirePermission(AccessPageType.SELECTED_PAGE);
        validateRange(start, end);

        User user = currentUser();
        List<Long> allowedBranchIds = allowedBranchIds(user);

        // Security: a user without an allowed role location sees no records.
        if (allowedBranchIds.isEmpty()) return List.of();

        List<EmployeeAttendanceDetail> records =
                repository.findForQcVerification(
                        start, end, allowedBranchIds);

        if (!records.isEmpty()) {
            fetchTeamsInBatches(records.stream()
                    .map(EmployeeAttendanceDetail::getId)
                    .toList());
        }

        return records;
    }

    private void fetchTeamsInBatches(List<Long> attendanceIds) {
        for (int from = 0; from < attendanceIds.size(); from += ASSOCIATION_BATCH_SIZE) {
            int to = Math.min(from + ASSOCIATION_BATCH_SIZE, attendanceIds.size());
            repository.fetchTeamsForAttendanceIds(attendanceIds.subList(from, to));
        }
    }

    @Transactional
    public VerifyResult verify(Collection<Long> ids) {
        requirePermission(AccessPageType.UPDATED_PAGE);
        if (ids == null || ids.isEmpty()) return new VerifyResult(0, 0);
        User user = currentUser();
        Employee verifier = currentEmployee(user);
        List<Long> allowedBranchIds = allowedBranchIds(user);

        List<EmployeeAttendanceDetail> records =
                repository.findForQcActionByIds(ids);
        attendanceLockService.requireAllEditable(records.stream()
                .map(EmployeeAttendanceDetail::getAttendanceDate)
                .toList());
        Set<AttendanceDayKey> duplicateDays = findDuplicateDays(records);

        int verified = 0, skipped = 0;
        for (EmployeeAttendanceDetail record : records) {
            if (!hasBranchAccess(record, allowedBranchIds)
                    || !eligible(record)
                    || duplicateDays.contains(dayKey(record))) {
                skipped++;
                continue;
            }
            applyVerified(record, verifier, user);
            verified++;
        }
        return new VerifyResult(verified, skipped);
    }

    private Set<AttendanceDayKey> findDuplicateDays(
            List<EmployeeAttendanceDetail> records) {
        if (records == null || records.isEmpty()) return Set.of();

        List<Long> employeeIds = records.stream()
                .map(record -> record.getEmployee().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        LocalDate startDate = records.stream()
                .map(EmployeeAttendanceDetail::getAttendanceDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate endDate = records.stream()
                .map(EmployeeAttendanceDetail::getAttendanceDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElseThrow();

        Set<AttendanceDayKey> duplicates = new HashSet<>();
        for (Object[] row : repository.findDuplicateEmployeeDates(
                employeeIds, startDate, endDate)) {
            duplicates.add(new AttendanceDayKey(
                    ((Number) row[0]).longValue(),
                    (LocalDate) row[1]
            ));
        }
        return duplicates;
    }

    private static AttendanceDayKey dayKey(EmployeeAttendanceDetail record) {
        return new AttendanceDayKey(
                record.getEmployee().getId(),
                record.getAttendanceDate()
        );
    }

    @Transactional
    public VerifyResult verifyDay(Long id) { return verify(List.of(id)); }

    @Transactional
    public int returnDay(Collection<Long> ids, String reason) {
        requirePermission(AccessPageType.UPDATED_PAGE);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Return reason is required. "
                    + "| មូលហេតុនៃការបញ្ជូនត្រឡប់គឺចាំបាច់។");
        }
        User user = currentUser();
        Employee reviewer = currentEmployee(user);
        List<Long> allowedBranchIds = allowedBranchIds(user);
        int returned = 0;
        List<EmployeeAttendanceDetail> records = repository.findForQcActionByIds(ids);
        attendanceLockService.requireAllEditable(records.stream()
                .map(EmployeeAttendanceDetail::getAttendanceDate)
                .toList());
        for (EmployeeAttendanceDetail record : records) {
            if (!hasBranchAccess(record, allowedBranchIds)
                    || record.getEntryStatus() != AttendanceEntryStatus.SUBMITTED) continue;
            record.setEntryStatus(AttendanceEntryStatus.DRAFT);
            record.setQcStatus(AttendanceQcStatus.RETURNED);
            record.setQcReturnReason(reason.trim());
            record.setQcReturnBy(reviewer);
            record.setQcReturnDate(LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE));
            record.setQcByEmployee(null);
            record.setQcByPosition(null);
            record.setQcLocation(null);
            record.setQcDate(null);
            record.setUserUpdated(user);
            returned++;
        }
        return returned;
    }

    private static List<Long> allowedBranchIds(User user) {
        if (user.getRoles() == null) return List.of();
        return user.getRoles().stream()
                .flatMap(role -> role.getBranchs() == null
                        ? java.util.stream.Stream.empty()
                        : role.getBranchs().stream())
                .map(Branch::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static boolean hasBranchAccess(
            EmployeeAttendanceDetail record, Collection<Long> allowedBranchIds) {
        return record.getReportLocation() != null
                && record.getReportLocation().getId() != null
                && allowedBranchIds.contains(record.getReportLocation().getId());
    }

    private boolean eligible(EmployeeAttendanceDetail record) {
        return record.getEntryStatus() == AttendanceEntryStatus.SUBMITTED
                && record.getQcStatus() != AttendanceQcStatus.VERIFIED
                && record.getQcByEmployee() == null;
    }

    private void applyVerified(EmployeeAttendanceDetail record, Employee verifier, User user) {
        record.setQcByEmployee(verifier);
        record.setQcByPosition(verifier.getPositions());
        record.setQcLocation(verifier.getBranch());
        record.setQcDate(LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE));
        record.setQcStatus(AttendanceQcStatus.VERIFIED);
        record.setQcReturnReason(null);
        record.setQcReturnBy(null);
        record.setQcReturnDate(null);
        record.setUserUpdated(user);
    }

    private void requirePermission(AccessPageType type) {
        if (!authenticatedUser.hasPage(AttendanceVerificationView.class, type))
            throw new IllegalArgumentException("You do not have permission for QC verification.");
    }

    private User currentUser() {
        return authenticatedUser.get().orElseThrow(() -> new IllegalArgumentException("User is not logged in."));
    }

    private Employee currentEmployee(User user) {
        try {
            return employeeRepository.findByInsuranceNo(Integer.valueOf(user.getInsurance().trim()))
                    .orElseThrow(() -> new IllegalArgumentException("Employee for current user was not found."));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Current user is not linked to a valid employee.");
        }
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start))
            throw new IllegalArgumentException("Please select a valid date range.");
        if (start.plusMonths(2).isBefore(end))
            throw new IllegalArgumentException("The date range cannot exceed two months.");
    }

    public record VerifyResult(int verified, int skipped) { }

    private record AttendanceDayKey(Long employeeId, LocalDate attendanceDate) { }
}
