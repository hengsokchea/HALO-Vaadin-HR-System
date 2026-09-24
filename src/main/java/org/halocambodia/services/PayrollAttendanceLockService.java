package org.halocambodia.services;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;

import org.halocambodia.data.PayrollAttendanceControlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollAttendanceLockService {

    private final PayrollAttendanceControlRepository controlRepository;

    public PayrollAttendanceLockService(
            PayrollAttendanceControlRepository controlRepository) {
        this.controlRepository = controlRepository;
    }

    @Transactional(readOnly = true)
    public boolean isLocked(LocalDate attendanceDate) {
        return attendanceDate != null
                && controlRepository.findApprovedLockDate(attendanceDate).isPresent();
    }

    @Transactional(readOnly = true)
    public void requireEditable(LocalDate attendanceDate) {
        if (attendanceDate == null) {
            throw new IllegalArgumentException(
                    "Attendance date is required. | ត្រូវការកាលបរិច្ឆេទវត្តមាន។");
        }

        controlRepository.findApprovedLockDate(attendanceDate)
                .ifPresent(cutoffDate -> {
                    throw new IllegalStateException(
                            "Attendance on " + attendanceDate
                            + " is locked because payroll attendance was approved through "
                            + cutoffDate
                            + ". Only attendance after the cutoff date can be changed. "
                            + "| វត្តមាននៅថ្ងៃទី " + attendanceDate
                            + " ត្រូវបានចាក់សោរហូតដល់ថ្ងៃកំណត់ " + cutoffDate
                            + "។ អាចកែប្រែបានតែវត្តមានក្រោយថ្ងៃកំណត់ប៉ុណ្ណោះ។");
                });
    }

    @Transactional(readOnly = true)
    public void requireAllEditable(Collection<LocalDate> attendanceDates) {
        if (attendanceDates == null) return;
        attendanceDates.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(this::requireEditable);
    }
}
