package org.halocambodia.data;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollAttendanceControlRepository extends JpaRepository<PayrollAttendanceControl, Long>,
        JpaSpecificationExecutor<PayrollAttendanceControl> {

    Optional<PayrollAttendanceControl> findByPayrollPeriodId(Long payrollPeriodId);

    @Query("""
            select max(control.attendanceCutoffDate)
            from PayrollAttendanceControl control, PayrollPeriod period
            where period.id = control.payrollPeriodId
              and control.status = 'APPROVED'
              and control.attendanceCutoffDate is not null
              and :attendanceDate between period.periodStart
                                      and control.attendanceCutoffDate
            """)
    Optional<LocalDate> findApprovedLockDate(
            @Param("attendanceDate") LocalDate attendanceDate);
}
