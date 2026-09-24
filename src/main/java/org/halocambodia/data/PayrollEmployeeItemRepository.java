package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollEmployeeItemRepository
        extends JpaRepository<PayrollEmployeeItem, Long>, JpaSpecificationExecutor<PayrollEmployeeItem> {

    List<PayrollEmployeeItem> findByPayrollEmployeeIdOrderByIdAsc(Long payrollEmployeeId);

    @Query(value = """
            SELECT pei.payroll_employee_id,
                   pc.component_code,
                   COALESCE(SUM(pei.payroll_amount), 0) AS component_amount
            FROM public.payroll_employee_item pei
            JOIN public.payroll_employee pe
              ON pe.payroll_employee_id = pei.payroll_employee_id
            JOIN public.payroll_component pc
              ON pc.payroll_component_id = pei.payroll_component_id
            WHERE pe.payroll_run_id = :runId
              AND pc.component_code IN (
                  'NSSF_HEALTH_EMPLOYEE',
                  'NSSF_HEALTH_EMPLOYER',
                  'NSSF_RISK_EMPLOYEE',
                  'NSSF_RISK_EMPLOYER',
                  'NSSF_PENSION_EMPLOYEE',
                  'NSSF_PENSION_EMPLOYER'
              )
            GROUP BY pei.payroll_employee_id, pc.component_code
            """, nativeQuery = true)
    List<Object[]> findNssfComponentAmountsByRunId(@Param("runId") Long runId);

    boolean existsByPayrollComponentId(Long payrollComponentId);
}
