package org.halocambodia.data;

import static org.halocambodia.data.PayrollModels.EmployeePayrollRow;
import static org.halocambodia.data.PayrollModels.PayrollCandidateRow;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Focused payroll-employee reads and correction-state updates. */
@Repository
public class PayrollEmployeeDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<PayrollCandidateRow> findAdjustmentCandidates(Long periodId, Long runId) {
        if (periodId == null || runId == null) {
            return List.of();
        }
        List<Object[]> result = entityManager.createNativeQuery("""
                WITH base_run AS (
                    SELECT payroll_run_id
                    FROM public.payroll_run
                    WHERE payroll_period_id = :periodId
                      AND run_type = 'REGULAR'
                      AND status IN ('APPROVED','PAID')
                    ORDER BY run_number DESC
                    LIMIT 1
                )
                SELECT e.emp_id, e.insurance_no, e.name_en, e.name_kh,
                       e.last_career_type_date, base.net_salary AS reference_net_pay
                FROM base_run br
                JOIN public.payroll_employee base
                  ON base.payroll_run_id = br.payroll_run_id
                JOIN public.emp_master e
                  ON e.emp_id = base.emp_id
                WHERE base.payroll_status = 'INCLUDED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee current_employee
                      WHERE current_employee.payroll_run_id = :runId
                        AND current_employee.emp_id = e.emp_id
                  )
                ORDER BY e.insurance_no
                """)
                .setParameter("periodId", periodId)
                .setParameter("runId", runId)
                .getResultList();
        return result.stream().map(PayrollEmployeeDataRepository::mapCandidate).toList();
    }

    @SuppressWarnings("unchecked")
    public List<PayrollCandidateRow> findFinalPaymentCandidates(Long periodId, Long runId) {
        if (periodId == null || runId == null) {
            return List.of();
        }
        List<Object[]> result = entityManager.createNativeQuery("""
                WITH rostered AS (
                    SELECT DISTINCT r.emp_id
                    FROM public.emp_roster r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = :periodId
                    WHERE r.roster_date BETWEEN p.period_start AND p.period_end
                )
                SELECT e.emp_id, e.insurance_no, e.name_en, e.name_kh,
                       e.last_career_type_date, 0::numeric AS reference_net_pay
                FROM public.emp_master e
                JOIN rostered rr
                  ON rr.emp_id = e.emp_id
                JOIN public.payroll_period p
                  ON p.payroll_period_id = :periodId
                JOIN public.list_career_type career_type
                  ON career_type.career_type_id = e.emp_status_career_type_id
                JOIN public.list_career_type_group career_group
                  ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
                WHERE UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                  AND e.last_career_type_date BETWEEN p.period_start AND p.period_end
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee current_employee
                      WHERE current_employee.payroll_run_id = :runId
                        AND current_employee.emp_id = e.emp_id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee paid_employee
                      JOIN public.payroll_run paid_run
                        ON paid_run.payroll_run_id = paid_employee.payroll_run_id
                      WHERE paid_run.payroll_period_id = p.payroll_period_id
                        AND paid_run.payroll_run_id <> :runId
                        AND paid_run.status <> 'CANCELLED'
                        AND paid_employee.emp_id = e.emp_id
                        AND paid_employee.payroll_status <> 'EXCLUDED'
                  )
                ORDER BY e.insurance_no
                """)
                .setParameter("periodId", periodId)
                .setParameter("runId", runId)
                .getResultList();
        return result.stream().map(PayrollEmployeeDataRepository::mapCandidate).toList();
    }

    @SuppressWarnings("unchecked")
    public List<PayrollCandidateRow> findRegularCandidates(Long periodId, Long runId) {
        if (periodId == null || runId == null) {
            return List.of();
        }
        List<Object[]> result = entityManager.createNativeQuery("""
                SELECT DISTINCT e.emp_id, e.insurance_no, e.name_en, e.name_kh,
                       e.last_career_type_date, 0::numeric AS reference_net_pay
                FROM public.emp_master e
                JOIN public.emp_roster roster
                  ON roster.emp_id = e.emp_id
                JOIN public.payroll_period p
                  ON p.payroll_period_id = :periodId
                LEFT JOIN public.list_career_type career_type
                  ON career_type.career_type_id = e.emp_status_career_type_id
                LEFT JOIN public.list_career_type_group career_group
                  ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
                WHERE roster.roster_date BETWEEN p.period_start AND p.period_end
                  AND e.join_date <= p.period_end
                  AND CASE
                          WHEN career_group.career_type_group_name IS NOT NULL
                           AND UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                           AND e.last_career_type_date IS NOT NULL
                          THEN e.last_career_type_date - 1
                          ELSE p.period_end
                      END >= p.period_start
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee current_employee
                      WHERE current_employee.payroll_run_id = :runId
                        AND current_employee.emp_id = e.emp_id
                  )
                ORDER BY e.insurance_no
                """)
                .setParameter("periodId", periodId)
                .setParameter("runId", runId)
                .getResultList();
        return result.stream().map(PayrollEmployeeDataRepository::mapCandidate).toList();
    }

    public int insertRegularPayrollEmployees(
            Long runId, Long periodId, Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee
                (payroll_run_id, emp_id, insurance_no, employee_name_en, employee_name_kh,
                 gender, bank_name, bank_account, spouse_count, tax_dependent_count,
                 salary_currency, basic_salary, payroll_status, created_by)
                SELECT :runId, e.emp_id, e.insurance_no, e.name_en, e.name_kh,
                       e.gender::text, b.bank_name, e.bank_account,
                       COALESCE(v.spouse, 0), COALESCE(v.total_dependency_tax, 0),
                       'USD', COALESCE(e.gross_salary_usd, 0), 'INCLUDED', :userId
                FROM public.emp_master e
                JOIN (
                    SELECT DISTINCT r.emp_id
                    FROM public.emp_roster r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = :periodId
                    WHERE r.roster_date >= p.period_start
                      AND r.roster_date <= p.period_end
                ) rostered
                  ON rostered.emp_id = e.emp_id
                LEFT JOIN public.list_bank b
                  ON b.bank_id = e.bank_id
                LEFT JOIN public.cam_view_emp_master v
                  ON v.emp_id = e.emp_id
                JOIN public.payroll_period employee_period
                  ON employee_period.payroll_period_id = :periodId
                LEFT JOIN public.list_career_type career_type
                  ON career_type.career_type_id = e.emp_status_career_type_id
                LEFT JOIN public.list_career_type_group career_group
                  ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
                WHERE e.join_date <= employee_period.period_end
                  AND CASE
                          WHEN career_group.career_type_group_name IS NOT NULL
                           AND UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                           AND e.last_career_type_date IS NOT NULL
                          THEN e.last_career_type_date - 1
                          ELSE employee_period.period_end
                      END >= employee_period.period_start
                  AND NOT EXISTS (
                    SELECT 1
                    FROM public.payroll_employee final_employee
                    JOIN public.payroll_run final_run
                      ON final_run.payroll_run_id = final_employee.payroll_run_id
                    WHERE final_run.payroll_period_id = :periodId
                      AND final_run.run_type = 'FINAL_PAYMENT'
                      AND final_run.status <> 'CANCELLED'
                      AND final_employee.emp_id = e.emp_id
                      AND final_employee.payroll_status <> 'EXCLUDED'
                )
                ON CONFLICT (payroll_run_id, emp_id) DO NOTHING
                """)
                .setParameter("runId", runId)
                .setParameter("periodId", periodId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertSelectedPayrollEmployees(
            Long runId, Long periodId, java.util.Collection<Long> empIds, Long userId) {
        if (empIds == null || empIds.isEmpty()) {
            return 0;
        }
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee
                (payroll_run_id, emp_id, insurance_no, employee_name_en, employee_name_kh,
                 gender, bank_name, bank_account, spouse_count, tax_dependent_count,
                 salary_currency, basic_salary, payroll_status, created_by)
                SELECT :runId, e.emp_id, e.insurance_no, e.name_en, e.name_kh,
                       e.gender::text, b.bank_name, e.bank_account,
                       COALESCE(v.spouse, 0), COALESCE(v.total_dependency_tax, 0),
                       'USD', COALESCE(e.gross_salary_usd, 0), 'INCLUDED', :userId
                FROM public.emp_master e
                JOIN (
                    SELECT DISTINCT r.emp_id
                    FROM public.emp_roster r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = :periodId
                    WHERE r.roster_date >= p.period_start
                      AND r.roster_date <= p.period_end
                ) rostered
                  ON rostered.emp_id = e.emp_id
                LEFT JOIN public.list_bank b
                  ON b.bank_id = e.bank_id
                LEFT JOIN public.cam_view_emp_master v
                  ON v.emp_id = e.emp_id
                WHERE e.emp_id IN (:empIds)
                ON CONFLICT (payroll_run_id, emp_id) DO NOTHING
                """)
                .setParameter("runId", runId)
                .setParameter("periodId", periodId)
                .setParameter("empIds", empIds)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<EmployeePayrollRow> findEmployees(Long runId, String search) {
        if (runId == null) {
            return List.of();
        }
        String term = search == null ? "" : search.trim();
        List<Object[]> result = entityManager.createNativeQuery("""
                SELECT pe.payroll_employee_id,
                       pe.payroll_run_id,
                       pe.emp_id,
                       pe.insurance_no,
                       pe.employee_name_en,
                       pe.employee_name_kh,
                       pe.gender,
                       pe.bank_name,
                       pe.bank_account,
                       pe.spouse_count,
                       pe.tax_dependent_count,
                       pe.salary_currency,
                       pe.basic_salary,
                       COALESCE(calculated_basic.amount, 0) AS calculated_basic_salary,
                       pe.total_earnings,
                       pe.total_deductions,
                       pe.salary_tax,
                       pe.employee_contribution,
                       pe.employer_contribution,
                       pe.net_salary,
                       pe.payroll_status,
                       pe.remarks,
                       pe.correction_required,
                       pe.correction_reason,
                       pe.correction_returned_at,
                       pe.correction_resolved_at
                FROM public.payroll_employee pe
                LEFT JOIN (
                    SELECT item.payroll_employee_id,
                           SUM(item.payroll_amount) AS amount
                    FROM public.payroll_employee_item item
                    JOIN public.payroll_employee item_employee
                      ON item_employee.payroll_employee_id = item.payroll_employee_id
                     AND item_employee.payroll_run_id = :runId
                    JOIN public.payroll_component component
                      ON component.payroll_component_id = item.payroll_component_id
                    WHERE component.component_code = 'BASIC_SALARY'
                    GROUP BY item.payroll_employee_id
                ) calculated_basic
                  ON calculated_basic.payroll_employee_id = pe.payroll_employee_id
                WHERE pe.payroll_run_id = :runId
                  AND (:term = ''
                       OR pe.insurance_no::text ILIKE '%' || :term || '%'
                       OR pe.employee_name_en ILIKE '%' || :term || '%'
                       OR pe.employee_name_kh ILIKE '%' || :term || '%')
                ORDER BY pe.insurance_no
                """)
                .setParameter("runId", runId)
                .setParameter("term", term)
                .getResultList();
        return result.stream().map(PayrollEmployeeDataRepository::mapEmployee).toList();
    }


    public int excludeAdjustmentEmployeesWithoutAttendanceChange(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                WITH run_context AS (
                    SELECT r.payroll_period_id, p.payment_date
                    FROM public.payroll_run r
                    JOIN public.payroll_period p ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                      AND r.run_type = 'ADJUSTMENT'
                ), base_run AS (
                    SELECT r.payroll_run_id
                    FROM public.payroll_run r
                    JOIN run_context ctx ON ctx.payroll_period_id = r.payroll_period_id
                    WHERE r.run_type = 'REGULAR'
                      AND r.status IN ('APPROVED', 'PAID')
                    ORDER BY r.run_number DESC
                    LIMIT 1
                )
                UPDATE public.payroll_employee current_employee
                SET payroll_status = 'EXCLUDED',
                    remarks = 'No post-payment attendance change after reconciliation',
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                FROM run_context ctx, base_run br
                WHERE current_employee.payroll_run_id = :runId
                  AND current_employee.payroll_status = 'INCLUDED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee base_employee
                      WHERE base_employee.payroll_run_id = br.payroll_run_id
                        AND base_employee.emp_id = current_employee.emp_id
                        AND base_employee.payroll_status = 'INCLUDED'
                        AND EXISTS (
                            SELECT 1
                            FROM (
                                SELECT attendance_date, leave_type_id, payroll_policy_rule_id, holiday_id,
                                       day_value, overtime_hours, normal_working_hours, scheduled_workday,
                                       leave_period_start
                                FROM public.payroll_attendance_day_snapshot
                                WHERE payroll_employee_id = current_employee.payroll_employee_id
                                  AND attendance_date > COALESCE(ctx.payment_date, attendance_date - 1)
                                EXCEPT
                                SELECT attendance_date, leave_type_id, payroll_policy_rule_id, holiday_id,
                                       day_value, overtime_hours, normal_working_hours, scheduled_workday,
                                       leave_period_start
                                FROM public.payroll_attendance_day_snapshot
                                WHERE payroll_employee_id = base_employee.payroll_employee_id
                                  AND attendance_date > COALESCE(ctx.payment_date, attendance_date - 1)
                                UNION ALL
                                SELECT attendance_date, leave_type_id, payroll_policy_rule_id, holiday_id,
                                       day_value, overtime_hours, normal_working_hours, scheduled_workday,
                                       leave_period_start
                                FROM public.payroll_attendance_day_snapshot
                                WHERE payroll_employee_id = base_employee.payroll_employee_id
                                  AND attendance_date > COALESCE(ctx.payment_date, attendance_date - 1)
                                EXCEPT
                                SELECT attendance_date, leave_type_id, payroll_policy_rule_id, holiday_id,
                                       day_value, overtime_hours, normal_working_hours, scheduled_workday,
                                       leave_period_start
                                FROM public.payroll_attendance_day_snapshot
                                WHERE payroll_employee_id = current_employee.payroll_employee_id
                                  AND attendance_date > COALESCE(ctx.payment_date, attendance_date - 1)
                            ) changed_day
                        )
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int excludeAdjustmentEmployeesWithoutDifference(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee pe
                SET payroll_status = 'EXCLUDED',
                    remarks = 'No financial difference after attendance reconciliation',
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND EXISTS (
                      SELECT 1 FROM public.payroll_run r
                      WHERE r.payroll_run_id = pe.payroll_run_id
                        AND r.run_type = 'ADJUSTMENT'
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee_item i
                      JOIN public.payroll_component c
                        ON c.payroll_component_id = i.payroll_component_id
                      WHERE i.payroll_employee_id = pe.payroll_employee_id
                        AND c.component_code IN
                            ('PAYROLL_ADJUSTMENT_EARNING','PAYROLL_ADJUSTMENT_DEDUCTION')
                        AND COALESCE(i.payroll_amount, 0) <> 0
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /**
     * Removes zero-difference employees from an ADJUSTMENT run completely.
     * EXCLUDED is used only as a short-lived internal marker while generated data is
     * cleaned; it is not retained as an adjustment employee record.
     */
    public int deleteExcludedAdjustmentEmployees(Long runId) {
        if (runId == null) {
            return 0;
        }

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_attendance_summary summary
                USING public.payroll_employee employee, public.payroll_run run
                WHERE summary.payroll_employee_id = employee.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND run.payroll_run_id = employee.payroll_run_id
                  AND run.run_type = 'ADJUSTMENT'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_attendance_day_snapshot snapshot
                USING public.payroll_employee employee, public.payroll_run run
                WHERE snapshot.payroll_employee_id = employee.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND run.payroll_run_id = employee.payroll_run_id
                  AND run.run_type = 'ADJUSTMENT'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item item
                USING public.payroll_employee employee, public.payroll_run run
                WHERE item.payroll_employee_id = employee.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND run.payroll_run_id = employee.payroll_run_id
                  AND run.run_type = 'ADJUSTMENT'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_recurring_application application
                USING public.payroll_employee employee, public.payroll_run run
                WHERE application.payroll_employee_id = employee.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND run.payroll_run_id = employee.payroll_run_id
                  AND run.run_type = 'ADJUSTMENT'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_loan_repayment repayment
                USING public.payroll_employee employee, public.payroll_run run
                WHERE repayment.payroll_employee_id = employee.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND run.payroll_run_id = employee.payroll_run_id
                  AND run.run_type = 'ADJUSTMENT'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_seniority_calculation calculation
                USING public.payroll_employee employee, public.payroll_run run
                WHERE calculation.payroll_employee_id = employee.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND run.payroll_run_id = employee.payroll_run_id
                  AND run.run_type = 'ADJUSTMENT'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee employee
                USING public.payroll_run run
                WHERE employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND run.payroll_run_id = employee.payroll_run_id
                  AND run.run_type = 'ADJUSTMENT'
                """)
                .setParameter("runId", runId)
                .executeUpdate();
    }

    public int countIncludedEmployees(Long runId) {
        Number value = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'INCLUDED'
                """)
                .setParameter("runId", runId)
                .getSingleResult();
        return value == null ? 0 : value.intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findIncludedEmployeeIds(Long runId) {
        List<Object> result = entityManager.createNativeQuery("""
                SELECT payroll_employee_id
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'INCLUDED'
                ORDER BY insurance_no
                """)
                .setParameter("runId", runId)
                .getResultList();
        return result.stream().map(value -> ((Number) value).longValue()).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findOpenCorrectionEmployeeIds(Long runId) {
        List<Object> result = entityManager.createNativeQuery("""
                SELECT payroll_employee_id
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'INCLUDED'
                  AND correction_required = TRUE
                ORDER BY insurance_no
                """)
                .setParameter("runId", runId)
                .getResultList();
        return result.stream().map(value -> ((Number) value).longValue()).toList();
    }


    /**
     * Refreshes the payroll snapshot for one returned employee from the current
     * employee-master data. This is intentionally limited to an INCLUDED employee
     * whose correction is still open, so reviewed employees outside the correction
     * scope remain frozen.
     */
    public int refreshReturnedEmployeeSnapshotFromMaster(
            Long runId, Long payrollEmployeeId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee pe
                SET insurance_no = e.insurance_no,
                    employee_name_en = e.name_en,
                    employee_name_kh = e.name_kh,
                    gender = e.gender::text,
                    bank_name = b.bank_name,
                    bank_account = e.bank_account,
                    spouse_count = COALESCE(v.spouse, 0),
                    tax_dependent_count = COALESCE(v.total_dependency_tax, 0),
                    salary_currency = 'USD',
                    basic_salary = COALESCE(e.gross_salary_usd, 0),
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = COALESCE(pe.version, 0) + 1
                FROM public.emp_master e
                LEFT JOIN public.list_bank b
                  ON b.bank_id = e.bank_id
                LEFT JOIN public.cam_view_emp_master v
                  ON v.emp_id = e.emp_id
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_employee_id = :payrollEmployeeId
                  AND pe.payroll_status = 'INCLUDED'
                  AND pe.correction_required = TRUE
                  AND e.emp_id = pe.emp_id
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /**
     * Bulk version used when every INCLUDED employee in a correction cycle is
     * being recalculated. One set-based UPDATE replaces hundreds or thousands
     * of per-employee refresh statements.
     */
    public int refreshAllReturnedEmployeeSnapshotsFromMaster(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee pe
                SET insurance_no = e.insurance_no,
                    employee_name_en = e.name_en,
                    employee_name_kh = e.name_kh,
                    gender = e.gender::text,
                    bank_name = b.bank_name,
                    bank_account = e.bank_account,
                    spouse_count = COALESCE(v.spouse, 0),
                    tax_dependent_count = COALESCE(v.total_dependency_tax, 0),
                    salary_currency = 'USD',
                    basic_salary = COALESCE(e.gross_salary_usd, 0),
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = COALESCE(pe.version, 0) + 1
                FROM public.emp_master e
                LEFT JOIN public.list_bank b
                  ON b.bank_id = e.bank_id
                LEFT JOIN public.cam_view_emp_master v
                  ON v.emp_id = e.emp_id
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND pe.correction_required = TRUE
                  AND e.emp_id = pe.emp_id
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int countIncluded(Long runId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'INCLUDED'
                """)
                .setParameter("runId", runId)
                .getSingleResult();
        return result == null ? 0 : ((Number) result).intValue();
    }

    public int countCorrectionRequired(Long runId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'INCLUDED'
                  AND correction_required = TRUE
                """)
                .setParameter("runId", runId)
                .getSingleResult();
        return result == null ? 0 : ((Number) result).intValue();
    }

    public boolean isIncludedInRun(Long runId, Long payrollEmployeeId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_employee_id = :payrollEmployeeId
                  AND payroll_status = 'INCLUDED'
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getSingleResult();
        return result != null && ((Number) result).longValue() > 0;
    }

    public boolean isReturnedForCorrection(Long runId, Long payrollEmployeeId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_employee_id = :payrollEmployeeId
                  AND correction_required = TRUE
                  AND correction_returned_at IS NOT NULL
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getSingleResult();
        return result != null && ((Number) result).longValue() > 0;
    }

    public boolean isIncluded(Long payrollEmployeeId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_employee_id = :payrollEmployeeId
                  AND payroll_status = 'INCLUDED'
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getSingleResult();
        return result != null && ((Number) result).longValue() > 0;
    }

    public int markCorrectionResolved(Long payrollEmployeeId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee
                SET correction_required = FALSE,
                    correction_resolved_at = CURRENT_TIMESTAMP,
                    correction_resolved_by = :userId,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE payroll_employee_id = :payrollEmployeeId
                  AND correction_required = TRUE
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /** Resolves every still-open INCLUDED correction in one UPDATE. */
    public int markAllCorrectionsResolved(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee
                SET correction_required = FALSE,
                    correction_resolved_at = CURRENT_TIMESTAMP,
                    correction_resolved_by = :userId,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'INCLUDED'
                  AND correction_required = TRUE
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public void clearExcludedCalculatedData(Long runId, Long userId) {
        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_employee pe, public.payroll_component pc
                WHERE pe.payroll_employee_id = i.payroll_employee_id
                  AND pc.payroll_component_id = i.payroll_component_id
                  AND pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'EXCLUDED'
                  AND (
                      i.source_type IN ('SALARY','ATTENDANCE','LEAVE','OVERTIME','SENIORITY','TAX','NSSF',
                                              'RECURRING','LOAN')
                      OR pc.component_code IN
                          ('PAYROLL_ADJUSTMENT_EARNING','PAYROLL_ADJUSTMENT_DEDUCTION',
                           'PRIOR_PAYROLL_RECOVERY')
                  )
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        // Excluded employees must not leave financial reservations behind.
        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_recurring_application application
                USING public.payroll_employee employee
                WHERE employee.payroll_employee_id = application.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND application.status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_loan_repayment repayment
                USING public.payroll_employee employee
                WHERE employee.payroll_employee_id = repayment.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                  AND repayment.status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_seniority_calculation calculation
                USING public.payroll_employee employee
                WHERE employee.payroll_employee_id = calculation.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'EXCLUDED'
                """)
                .setParameter("runId", runId)
                .executeUpdate();

        entityManager.createNativeQuery("""
                UPDATE public.payroll_employee
                SET total_earnings = 0,
                    total_deductions = 0,
                    salary_tax = 0,
                    employee_contribution = 0,
                    employer_contribution = 0,
                    net_salary = 0,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'EXCLUDED'
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }


    public int countEmployeesInRun(Long runId, java.util.Collection<Long> payrollEmployeeIds) {
        if (runId == null || payrollEmployeeIds == null || payrollEmployeeIds.isEmpty()) {
            return 0;
        }
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_employee_id IN (:employeeIds)
                """)
                .setParameter("runId", runId)
                .setParameter("employeeIds", payrollEmployeeIds)
                .getSingleResult();
        return result == null ? 0 : ((Number) result).intValue();
    }

    public int updatePayrollStatus(
            Long runId,
            java.util.Collection<Long> payrollEmployeeIds,
            String status,
            String statusNote,
            Long userId) {
        if (runId == null || payrollEmployeeIds == null || payrollEmployeeIds.isEmpty()) {
            return 0;
        }
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee
                SET payroll_status = :status,
                    remarks = CASE
                        WHEN CAST(:statusNote AS TEXT) IS NULL THEN remarks
                        WHEN remarks IS NULL OR BTRIM(remarks) = '' THEN :statusNote
                        ELSE remarks || CHR(10) || :statusNote
                    END,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE payroll_run_id = :runId
                  AND payroll_employee_id IN (:employeeIds)
                """)
                .setParameter("status", status)
                .setParameter("statusNote", statusNote)
                .setParameter("userId", userId)
                .setParameter("runId", runId)
                .setParameter("employeeIds", payrollEmployeeIds)
                .executeUpdate();
    }

    public int resetCorrectionScope(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee
                SET correction_required = FALSE,
                    correction_reason = NULL,
                    correction_returned_at = NULL,
                    correction_returned_by = NULL,
                    correction_resolved_at = NULL,
                    correction_resolved_by = NULL,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE payroll_run_id = :runId
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int markReturnedForCorrection(
            Long runId,
            java.util.Collection<Long> payrollEmployeeIds,
            String correctionReason,
            Long userId) {
        if (runId == null || payrollEmployeeIds == null || payrollEmployeeIds.isEmpty()) {
            return 0;
        }
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee
                SET correction_required = TRUE,
                    correction_reason = :correctionReason,
                    correction_returned_at = CURRENT_TIMESTAMP,
                    correction_returned_by = :userId,
                    correction_resolved_at = NULL,
                    correction_resolved_by = NULL,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE payroll_run_id = :runId
                  AND payroll_employee_id IN (:employeeIds)
                  AND payroll_status = 'INCLUDED'
                """)
                .setParameter("correctionReason", correctionReason)
                .setParameter("userId", userId)
                .setParameter("runId", runId)
                .setParameter("employeeIds", payrollEmployeeIds)
                .executeUpdate();
    }

    public int clearCorrectionScope(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee
                SET correction_required = FALSE,
                    correction_reason = NULL,
                    correction_returned_at = NULL,
                    correction_returned_by = NULL,
                    correction_resolved_at = NULL,
                    correction_resolved_by = NULL,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId,
                    version = version + 1
                WHERE payroll_run_id = :runId
                """)
                .setParameter("userId", userId)
                .setParameter("runId", runId)
                .executeUpdate();
    }

    private static PayrollCandidateRow mapCandidate(Object[] row) {
        return new PayrollCandidateRow(
                longValue(row[0]),
                intValue(row[1]),
                text(row[2]),
                text(row[3]),
                localDate(row[4]),
                decimal(row[5]));
    }

    private static EmployeePayrollRow mapEmployee(Object[] row) {
        return new EmployeePayrollRow(
                longValue(row[0]),
                longValue(row[1]),
                longValue(row[2]),
                intValue(row[3]),
                text(row[4]),
                text(row[5]),
                text(row[6]),
                text(row[7]),
                text(row[8]),
                intValue(row[9]),
                intValue(row[10]),
                text(row[11]),
                decimal(row[12]),
                decimal(row[13]),
                decimal(row[14]),
                decimal(row[15]),
                decimal(row[16]),
                decimal(row[17]),
                decimal(row[18]),
                decimal(row[19]),
                text(row[20]),
                text(row[21]),
                booleanValue(row[22]),
                text(row[23]),
                offsetDateTime(row[24]),
                offsetDateTime(row[25]));
    }

    private static Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean b ? b : value != null && Boolean.parseBoolean(value.toString());
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static LocalDate localDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private static OffsetDateTime offsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        throw new IllegalArgumentException("Unsupported timestamp value: " + value.getClass().getName());
    }
}
