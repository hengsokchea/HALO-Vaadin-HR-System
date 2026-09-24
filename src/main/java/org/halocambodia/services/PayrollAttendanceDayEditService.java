package org.halocambodia.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeAllocationRepository;
import org.halocambodia.data.EmployeeAttendanceDetail;
import org.halocambodia.data.EmployeeAttendanceDetailRepository;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.EmployeeRoster;
import org.halocambodia.data.EmployeeRosterRepository;
import org.halocambodia.data.LeaveType;
import org.halocambodia.data.LeaveTypeRepository;
import org.halocambodia.data.LeaveTypeSubType;
import org.halocambodia.data.LeaveTypeSubTypeRepository;
import org.halocambodia.data.Shift;
import org.halocambodia.data.User;
import org.halocambodia.data.enums.LeaveDuration;
import org.halocambodia.enums.AttendanceEntryStatus;
import org.halocambodia.enums.AttendanceQcStatus;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollAttendanceDayEditService {

    private final EmployeeAttendanceDetailRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAllocationRepository allocationRepository;
    private final EmployeeRosterRepository rosterRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeSubTypeRepository subTypeRepository;
    private final PayrollAttendanceLockService lockService;
    private final AuthenticatedUser authenticatedUser;

    public PayrollAttendanceDayEditService(
            EmployeeAttendanceDetailRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            EmployeeAllocationRepository allocationRepository,
            EmployeeRosterRepository rosterRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveTypeSubTypeRepository subTypeRepository,
            PayrollAttendanceLockService lockService,
            AuthenticatedUser authenticatedUser) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.allocationRepository = allocationRepository;
        this.rosterRepository = rosterRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.subTypeRepository = subTypeRepository;
        this.lockService = lockService;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional(readOnly = true)
    public Optional<EditData> find(Long employeeId, LocalDate attendanceDate) {
        Optional<EmployeeRoster> roster = rosterRepository
                .findByEmployeeIdAndRosterDate(employeeId, attendanceDate);
        ShiftDefaults assignedShift = findShiftDefaults(employeeId, attendanceDate)
                .orElse(null);
        Optional<EmployeeAttendanceDetail> attendance = attendanceRepository
                .findFirstByEmployeeIdAndAttendanceDateOrderByIdDesc(
                        employeeId, attendanceDate);

        if (attendance.isPresent()) {
            EmployeeAttendanceDetail record = attendance.get();
            EmployeeRoster shift = roster.orElse(null);
            return Optional.of(new EditData(
                    record.getId(),
                    record.getLeaveType() == null ? null : record.getLeaveType().getId(),
                    record.getLeaveTypeSubType() == null
                            ? null : record.getLeaveTypeSubType().getId(),
                    record.getLeaveDuration(),
                    firstNonNull(record.getFirstIn(),
                            firstNonNull(shift == null ? null : shift.getStartTime(),
                                    assignedShift == null ? null : assignedShift.firstIn())),
                    firstNonNull(record.getLastOut(),
                            firstNonNull(shift == null ? null : shift.getEndTime(),
                                    assignedShift == null ? null : assignedShift.lastOut())),
                    firstNonNull(record.getBreakMinutes(),
                            firstNonNull(shift == null ? null : shift.getBreakMinutes(),
                                    assignedShift == null ? null : assignedShift.breakMinutes())),
                    firstNonNull(record.getNormalHours(),
                            firstNonNull(shift == null ? null : shift.getTotalWorkingHour(),
                                    assignedShift == null ? null : assignedShift.normalHours())),
                    record.getRemark()));
        }

        return roster.map(shift -> new EditData(
                null,
                shift.getLeaveType() == null ? null : shift.getLeaveType().getId(),
                shift.getLeaveTypeSubType() == null
                        ? null : shift.getLeaveTypeSubType().getId(),
                shift.getLeaveDuration(),
                firstNonNull(shift.getStartTime(),
                        assignedShift == null ? null : assignedShift.firstIn()),
                firstNonNull(shift.getEndTime(),
                        assignedShift == null ? null : assignedShift.lastOut()),
                firstNonNull(shift.getBreakMinutes(),
                        assignedShift == null ? null : assignedShift.breakMinutes()),
                firstNonNull(shift.getTotalWorkingHour(),
                        assignedShift == null ? null : assignedShift.normalHours()),
                shift.getRemark()));
    }

    @Transactional(readOnly = true)
    public Optional<ShiftDefaults> findShiftDefaults(
            Long employeeId, LocalDate attendanceDate) {
        if (employeeId == null || attendanceDate == null) {
            return Optional.empty();
        }
        return employeeRepository.findById(employeeId)
                .flatMap(employee -> allocationRepository
                        .findShiftByEmployeeAndDate(employee, attendanceDate))
                .map(this::toShiftDefaults);
    }

    private ShiftDefaults toShiftDefaults(Shift shift) {
        return new ShiftDefaults(
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getBreakMinutes(),
                calculateNormalHours(
                        shift.getStartTime(), shift.getEndTime(), shift.getBreakMinutes()),
                shift.getMorningStartTime(),
                shift.getMorningEndTime(),
                shift.getMorningBreakMinutes(),
                calculateNormalHours(
                        shift.getMorningStartTime(), shift.getMorningEndTime(),
                        shift.getMorningBreakMinutes()),
                shift.getAfternoonStartTime(),
                shift.getAfternoonEndTime(),
                shift.getAfternoonBreakMinutes(),
                calculateNormalHours(
                        shift.getAfternoonStartTime(), shift.getAfternoonEndTime(),
                        shift.getAfternoonBreakMinutes()));
    }

    private static BigDecimal calculateNormalHours(
            LocalTime start, LocalTime end, Integer breakMinutes) {
        if (start == null || end == null) {
            return null;
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) {
            minutes += 24L * 60L;
        }
        minutes -= breakMinutes == null ? 0 : Math.max(breakMinutes, 0);
        return BigDecimal.valueOf(Math.max(minutes, 0))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public boolean isLocked(LocalDate attendanceDate) {
        return attendanceDate == null
                || !attendanceDate.isAfter(
                        LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE))
                || lockService.isLocked(attendanceDate);
    }

    @Transactional
    public EmployeeAttendanceDetail save(
            Long employeeId, LocalDate attendanceDate, EditCommand command) {
        requirePermission();
        LocalDate today = LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        if (attendanceDate == null || !attendanceDate.isAfter(today)) {
            throw new IllegalStateException(
                    "Attendance on or before today cannot be edited from Payroll Operation. "
                    + "Please use Daily Attendance. "
                    + "| មិនអាចកែប្រែវត្តមាននៅថ្ងៃនេះ ឬមុនថ្ងៃនេះពីប្រតិបត្តិការប្រាក់បៀវត្សបានទេ។ "
                    + "សូមប្រើទំព័រវត្តមានប្រចាំថ្ងៃ។");
        }
        lockService.requireEditable(attendanceDate);
        if (employeeId == null || command == null || command.leaveTypeId() == null) {
            throw new IllegalArgumentException(
                    "Employee, date and Duty / Leave are required. "
                    + "| ត្រូវការបុគ្គលិក កាលបរិច្ឆេទ និងកាតព្វកិច្ច / ច្បាប់។");
        }

        User user = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User is not logged in."));
        Employee currentEmployee = currentEmployee(user);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee was not found."));
        LeaveType leaveType = leaveTypeRepository.findById(command.leaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Duty / Leave was not found."));

        LeaveTypeSubType subType = null;
        if (command.leaveSubTypeId() != null) {
            subType = subTypeRepository.findById(command.leaveSubTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Sub Leave was not found."));
            if (subType.getLeaveType() == null
                    || !leaveType.getId().equals(subType.getLeaveType().getId())) {
                throw new IllegalArgumentException(
                        "The selected Sub Leave does not belong to the selected Duty / Leave.");
            }
        }

        EmployeeAttendanceDetail record = attendanceRepository
                .findFirstByEmployeeIdAndAttendanceDateOrderByIdDesc(
                        employeeId, attendanceDate)
                .orElseGet(EmployeeAttendanceDetail::new);
        boolean isNew = record.getId() == null;

        if (isNew) {
            record.setEmployee(employee);
            record.setAttendanceDate(attendanceDate);
            record.setReportedByEmployee(currentEmployee);
            record.setReportedByPosition(currentEmployee.getPositions());
            record.setReportedDate(LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE));
            record.setReportLocation(currentEmployee.getBranch());
            record.setDataEntryBy(currentEmployee);
            record.setDataEntryPosition(currentEmployee.getPositions());
            record.setDataEntryLocation(currentEmployee.getBranch());
            record.setDataEntryDate(LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE));
            record.setSource("PAYROLL_OPERATION");
            record.setUserCreated(user);
        }

        record.setLeaveType(leaveType);
        record.setLeaveTypeSubType(subType);
        record.setLeaveDuration(command.leaveDuration() == null
                ? LeaveDuration.FULL_DAY : command.leaveDuration());
        record.setFirstIn(command.firstIn());
        record.setLastOut(command.lastOut());
        record.setBreakMinutes(command.breakMinutes());
        record.setNormalHours(command.normalHours());
        record.setRemark(blankToNull(command.remark()));

        // Any live change after the payroll cutoff must go through attendance
        // verification again. The already-approved payroll snapshot remains frozen.
        record.setEntryStatus(AttendanceEntryStatus.DRAFT);
        record.setQcStatus(AttendanceQcStatus.PENDING);
        record.setQcByEmployee(null);
        record.setQcByPosition(null);
        record.setQcLocation(null);
        record.setQcDate(null);
        record.setQcReturnReason(null);
        record.setQcReturnBy(null);
        record.setQcReturnDate(null);
        record.setHrVerificationBy(null);
        record.setHrVerificationPosition(null);
        record.setHrVerificationDate(null);
        record.setUserUpdated(user);

        if (record.getReportLocation() == null) {
            throw new IllegalStateException(
                    "Current user must be linked to an employee location before editing attendance.");
        }
        return attendanceRepository.saveAndFlush(record);
    }

    private void requirePermission() {
        if (!authenticatedUser.hasPage(PayrollView.class, AccessPageType.UPDATED_PAGE)) {
            throw new IllegalArgumentException(
                    "You do not have permission to edit attendance from Payroll Operation.");
        }
    }

    private Employee currentEmployee(User user) {
        try {
            return employeeRepository.findByInsuranceNo(
                    Integer.valueOf(user.getInsurance().trim()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Employee for current user was not found."));
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Current user is not linked to a valid employee.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> T firstNonNull(T primary, T fallback) {
        return primary != null ? primary : fallback;
    }

    public record EditData(
            Long attendanceId,
            Long leaveTypeId,
            Long leaveSubTypeId,
            LeaveDuration leaveDuration,
            LocalTime firstIn,
            LocalTime lastOut,
            Integer breakMinutes,
            BigDecimal normalHours,
            String remark) { }

    public record EditCommand(
            Long leaveTypeId,
            Long leaveSubTypeId,
            LeaveDuration leaveDuration,
            LocalTime firstIn,
            LocalTime lastOut,
            Integer breakMinutes,
            BigDecimal normalHours,
            String remark) { }

    public record ShiftDefaults(
            LocalTime firstIn,
            LocalTime lastOut,
            Integer breakMinutes,
            BigDecimal normalHours,
            LocalTime morningFirstIn,
            LocalTime morningLastOut,
            Integer morningBreakMinutes,
            BigDecimal morningNormalHours,
            LocalTime afternoonFirstIn,
            LocalTime afternoonLastOut,
            Integer afternoonBreakMinutes,
            BigDecimal afternoonNormalHours) { }
}
