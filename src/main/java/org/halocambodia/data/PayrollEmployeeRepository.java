package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollEmployeeRepository
        extends JpaRepository<PayrollEmployee, Long>, JpaSpecificationExecutor<PayrollEmployee> {

    List<PayrollEmployee> findByPayrollRunIdOrderByInsuranceNoAsc(Long payrollRunId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            WITH totals AS (
                SELECT
                    pe.payroll_employee_id,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'EARNING'), 0) AS earnings,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'DEDUCTION'), 0) AS deductions,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'EMPLOYER_CONTRIBUTION'), 0) AS employer_contribution,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_code = 'SALARY_TAX'), 0) AS salary_tax,
                    COALESCE(SUM(i.payroll_amount) FILTER (
                        WHERE c.component_code IN (
                            'NSSF_EMPLOYEE',
                            'NSSF_HEALTH_EMPLOYEE',
                            'NSSF_RISK_EMPLOYEE',
                            'NSSF_PENSION_EMPLOYEE'
                        )
                    ), 0) AS employee_contribution
                FROM public.payroll_employee pe
                LEFT JOIN public.payroll_employee_item i
                  ON i.payroll_employee_id = pe.payroll_employee_id
                LEFT JOIN public.payroll_component c
                  ON c.payroll_component_id = i.payroll_component_id
                WHERE pe.payroll_status = 'INCLUDED'
                  AND pe.payroll_run_id = :runId
                GROUP BY pe.payroll_employee_id
            )
            UPDATE public.payroll_employee pe
            SET total_earnings = t.earnings,
                total_deductions = t.deductions,
                salary_tax = t.salary_tax,
                employee_contribution = t.employee_contribution,
                employer_contribution = t.employer_contribution,
                net_salary = t.earnings - t.deductions,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                version = pe.version + 1
            FROM totals t
            WHERE pe.payroll_employee_id = t.payroll_employee_id
            """, nativeQuery = true)
    int refreshTotalsForRun(@Param("runId") Long runId, @Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            WITH totals AS (
                SELECT
                    pe.payroll_employee_id,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'EARNING'), 0) AS earnings,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'DEDUCTION'), 0) AS deductions,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'EMPLOYER_CONTRIBUTION'), 0) AS employer_contribution,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_code = 'SALARY_TAX'), 0) AS salary_tax,
                    COALESCE(SUM(i.payroll_amount) FILTER (
                        WHERE c.component_code IN (
                            'NSSF_EMPLOYEE',
                            'NSSF_HEALTH_EMPLOYEE',
                            'NSSF_RISK_EMPLOYEE',
                            'NSSF_PENSION_EMPLOYEE'
                        )
                    ), 0) AS employee_contribution
                FROM public.payroll_employee pe
                LEFT JOIN public.payroll_employee_item i
                  ON i.payroll_employee_id = pe.payroll_employee_id
                LEFT JOIN public.payroll_component c
                  ON c.payroll_component_id = i.payroll_component_id
                WHERE pe.payroll_status = 'INCLUDED'
                  AND pe.payroll_run_id = :runId
                  AND pe.payroll_employee_id IN (:employeeIds)
                GROUP BY pe.payroll_employee_id
            )
            UPDATE public.payroll_employee pe
            SET total_earnings = t.earnings,
                total_deductions = t.deductions,
                salary_tax = t.salary_tax,
                employee_contribution = t.employee_contribution,
                employer_contribution = t.employer_contribution,
                net_salary = t.earnings - t.deductions,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                version = pe.version + 1
            FROM totals t
            WHERE pe.payroll_employee_id = t.payroll_employee_id
            """, nativeQuery = true)
    int refreshTotalsForEmployees(
            @Param("runId") Long runId,
            @Param("employeeIds") List<Long> employeeIds,
            @Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            WITH totals AS (
                SELECT
                    pe.payroll_employee_id,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'EARNING'), 0) AS earnings,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'DEDUCTION'), 0) AS deductions,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_type = 'EMPLOYER_CONTRIBUTION'), 0) AS employer_contribution,
                    COALESCE(SUM(i.payroll_amount) FILTER (WHERE c.component_code = 'SALARY_TAX'), 0) AS salary_tax,
                    COALESCE(SUM(i.payroll_amount) FILTER (
                        WHERE c.component_code IN (
                            'NSSF_EMPLOYEE',
                            'NSSF_HEALTH_EMPLOYEE',
                            'NSSF_RISK_EMPLOYEE',
                            'NSSF_PENSION_EMPLOYEE'
                        )
                    ), 0) AS employee_contribution
                FROM public.payroll_employee pe
                LEFT JOIN public.payroll_employee_item i
                  ON i.payroll_employee_id = pe.payroll_employee_id
                LEFT JOIN public.payroll_component c
                  ON c.payroll_component_id = i.payroll_component_id
                WHERE pe.payroll_status = 'INCLUDED'
                  AND pe.payroll_employee_id = :employeeId
                GROUP BY pe.payroll_employee_id
            )
            UPDATE public.payroll_employee pe
            SET total_earnings = t.earnings,
                total_deductions = t.deductions,
                salary_tax = t.salary_tax,
                employee_contribution = t.employee_contribution,
                employer_contribution = t.employer_contribution,
                net_salary = t.earnings - t.deductions,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                version = pe.version + 1
            FROM totals t
            WHERE pe.payroll_employee_id = t.payroll_employee_id
            """, nativeQuery = true)
    int refreshTotalsForEmployee(@Param("employeeId") Long employeeId, @Param("userId") Long userId);
}
