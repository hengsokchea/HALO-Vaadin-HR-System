package org.halocambodia.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollApprovedAttendanceDayRepository
        extends JpaRepository<PayrollApprovedAttendanceDay, Long>,
                PayrollApprovedAttendanceDayRepositoryCustom {

    long countByPayrollPeriodId(Long payrollPeriodId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from PayrollApprovedAttendanceDay approved
            where approved.payrollPeriodId = :payrollPeriodId
            """)
    int deleteByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);
}
