package org.halocambodia.services;

import java.util.List;

import org.halocambodia.data.PayrollAttendanceDaySnapshot;
import org.halocambodia.data.PayrollAttendanceDaySnapshotRepository;
import org.halocambodia.data.PayrollRunRepository;
import org.halocambodia.enums.PayrollRunType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds the payroll attendance snapshot from approved attendance.
 * Heavy copy and summary aggregation remain set-based PostgreSQL operations,
 * exposed through Spring Data repositories rather than JdbcTemplate.
 */
@Service
public class PayrollAttendanceSnapshotService {

    private final PayrollAttendanceDaySnapshotRepository attendanceDaySnapshotRepository;
    private final PayrollRunRepository payrollRunRepository;

    public PayrollAttendanceSnapshotService(
            PayrollAttendanceDaySnapshotRepository attendanceDaySnapshotRepository,
            PayrollRunRepository payrollRunRepository) {
        this.attendanceDaySnapshotRepository = attendanceDaySnapshotRepository;
        this.payrollRunRepository = payrollRunRepository;
    }

    @Transactional
    public void refresh(Long runId, Long periodId, @Nullable Long payrollEmployeeId) {
        if (runId == null) {
            throw new IllegalArgumentException("Payroll run is required.");
        }
        if (periodId == null) {
            throw new IllegalArgumentException("Payroll period is required.");
        }

        attendanceDaySnapshotRepository.deleteForPayrollRun(runId, payrollEmployeeId);

        boolean adjustment = payrollRunRepository.findById(runId)
                .map(run -> PayrollRunType.ADJUSTMENT.matches(run.getRunType()))
                .orElseThrow(() -> new IllegalArgumentException("Payroll run was not found."));

        if (adjustment) {
            attendanceDaySnapshotRepository.copyReconciledAttendanceToAdjustmentRun(
                    runId, periodId, payrollEmployeeId);
        } else {
            attendanceDaySnapshotRepository.copyApprovedAttendanceToRun(
                    runId, periodId, payrollEmployeeId);
        }
        attendanceDaySnapshotRepository.rebuildAttendanceSummary(
                runId, periodId, payrollEmployeeId);
    }
    @Transactional(readOnly = true)
    public List<PayrollAttendanceDaySnapshot> findDays(Long payrollEmployeeId) {
        return payrollEmployeeId == null
                ? List.of()
                : attendanceDaySnapshotRepository
                        .findByPayrollEmployeeIdOrderByAttendanceDateAsc(payrollEmployeeId);
    }

}
