package org.halocambodia.services;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Branch;
import org.halocambodia.data.EmployeeAttendanceDetail;
import org.halocambodia.data.EmployeeAttendanceDetailRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.attendance_management.AttendanceSummaryView;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class AttendanceSummaryService {

    private static final int ASSOCIATION_BATCH_SIZE = 500;

    private final EmployeeAttendanceDetailRepository repository;
    private final AuthenticatedUser authenticatedUser;
    private final PayrollAttendanceLockService attendanceLockService;

    public AttendanceSummaryService(EmployeeAttendanceDetailRepository repository,
            AuthenticatedUser authenticatedUser,
            PayrollAttendanceLockService attendanceLockService) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
        this.attendanceLockService = attendanceLockService;
    }

    /**
     * Loads the date range with two bulk queries only:
     * attendance + employee/reference data, then teams.
     */
    @Transactional
    public List<EmployeeAttendanceDetail> findSummary(LocalDate start, LocalDate end) {
        requirePermission();
        validateRange(start, end);

        List<Long> branchIds = allowedBranchIds(currentUser());
        if (branchIds.isEmpty()) {
            return List.of();
        }

        List<EmployeeAttendanceDetail> records =
                repository.findForAttendanceSummary(start, end, branchIds);

        if (!records.isEmpty()) {
            List<Long> ids = records.stream()
                    .map(EmployeeAttendanceDetail::getId)
                    .filter(Objects::nonNull)
                    .toList();
            fetchTeamsInBatches(ids);
        }
        return records;
    }

    @Transactional
    public int deleteAttendanceRecords(Collection<Long> attendanceIds) {
        requireDeletePermission();
        if (attendanceIds == null || attendanceIds.isEmpty()) {
            return 0;
        }

        User user = currentUser();
        Set<Long> allowedBranchIds = new LinkedHashSet<>(allowedBranchIds(user));
        if (allowedBranchIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "You do not have access to any attendance location. "
                            + "| អ្នកមិនមានសិទ្ធិចូលប្រើទីតាំងវត្តមានណាមួយទេ។");
        }

        List<Long> ids = attendanceIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return 0;
        }

        List<EmployeeAttendanceDetail> records = repository.findAllById(ids);
        for (EmployeeAttendanceDetail record : records) {
            if (record.getReportLocation() == null
                    || record.getReportLocation().getId() == null
                    || !allowedBranchIds.contains(record.getReportLocation().getId())) {
                throw new IllegalArgumentException(
                        "One or more selected attendance records are outside your allowed locations. "
                                + "| កំណត់ត្រាវត្តមានមួយចំនួនស្ថិតក្រៅទីតាំងដែលអ្នកមានសិទ្ធិ។");
            }
        }

        attendanceLockService.requireAllEditable(records.stream()
                .map(EmployeeAttendanceDetail::getAttendanceDate)
                .toList());

        for (EmployeeAttendanceDetail record : records) {
            // Load child collections so JPA cascade/orphan removal handles them
            // the same way as the existing Attendance Register delete flow.
            record.getAttendanceDetailTeams().size();
            record.getAttendanceDetailTimeLogs().size();
            repository.delete(record);
        }
        repository.flush();
        return records.size();
    }

    private void fetchTeamsInBatches(List<Long> attendanceIds) {
        for (int from = 0; from < attendanceIds.size(); from += ASSOCIATION_BATCH_SIZE) {
            int to = Math.min(from + ASSOCIATION_BATCH_SIZE, attendanceIds.size());
            repository.fetchTeamsForAttendanceIds(attendanceIds.subList(from, to));
        }
    }

    private void requireDeletePermission() {
        if (!authenticatedUser.hasPage(
                AttendanceSummaryView.class, AccessPageType.DELETED_PAGE)) {
            throw new IllegalArgumentException(
                    "You do not have permission to delete Attendance Summary records. "
                            + "| អ្នកមិនមានសិទ្ធិលុបកំណត់ត្រាសង្ខេបវត្តមានទេ។");
        }
    }

    private void requirePermission() {
        if (!authenticatedUser.hasPage(
                AttendanceSummaryView.class, AccessPageType.SELECTED_PAGE)) {
            throw new IllegalArgumentException(
                    "You do not have permission to view Attendance Summary. "
                            + "| អ្នកមិនមានសិទ្ធិមើលសង្ខេបវត្តមានទេ។");
        }
    }

    private User currentUser() {
        return authenticatedUser.get().orElseThrow(() ->
                new IllegalArgumentException("User is not logged in."));
    }

    private static List<Long> allowedBranchIds(User user) {
        if (user.getRoles() == null) {
            return List.of();
        }
        return user.getRoles().stream()
                .flatMap(role -> role.getBranchs() == null
                        ? java.util.stream.Stream.empty()
                        : role.getBranchs().stream())
                .map(Branch::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "Please select a valid date range. "
                            + "| សូមជ្រើសរើសចន្លោះកាលបរិច្ឆេទត្រឹមត្រូវ។");
        }
        if (start.plusMonths(2).isBefore(end)) {
            throw new IllegalArgumentException(
                    "The date range cannot exceed two months. "
                            + "| ចន្លោះកាលបរិច្ឆេទមិនអាចលើសពីពីរខែទេ។");
        }
    }
}
