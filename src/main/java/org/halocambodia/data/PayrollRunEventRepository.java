package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRunEventRepository extends JpaRepository<PayrollRunEvent, Long> {

    List<PayrollRunEvent> findByPayrollRunIdOrderByEventAtDescIdDesc(Long payrollRunId);

    List<PayrollRunEvent> findByPayrollPaymentBatchIdOrderByEventAtDescIdDesc(Long payrollPaymentBatchId);

    /**
     * Inserts one employee-level audit event for every still-open INCLUDED
     * correction in a single PostgreSQL statement. This avoids 1,000+
     * IDENTITY inserts when a whole payroll run is returned.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO public.payroll_run_event
                (payroll_period_id, payroll_run_id, payroll_employee_id,
                 event_type, from_status, to_status, event_detail, event_at, event_by)
            SELECT :periodId, :runId, pe.payroll_employee_id,
                   'EMPLOYEE_RECALCULATED', 'CORRECTION_REQUIRED', 'CORRECTED',
                   :detail, CURRENT_TIMESTAMP, :userId
            FROM public.payroll_employee pe
            WHERE pe.payroll_run_id = :runId
              AND pe.payroll_status = 'INCLUDED'
              AND pe.correction_required = TRUE
            """, nativeQuery = true)
    int insertReturnedEmployeeRecalculatedEvents(
            @Param("periodId") Long periodId,
            @Param("runId") Long runId,
            @Param("detail") String detail,
            @Param("userId") Long userId);
}
