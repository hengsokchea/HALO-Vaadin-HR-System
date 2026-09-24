package org.halocambodia.data;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Set-based PostgreSQL operations for ADJUSTMENT payroll runs. */
@Repository
public class PayrollAdjustmentCalculationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public int ensureAdjustmentComponents(Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_component
                    (component_code, component_name_en, component_name_kh, component_type,
                     calculation_method, taxable, subject_to_nssf, active, sort_order,
                     created_by, updated_by)
                VALUES
                    ('PAYROLL_ADJUSTMENT_EARNING', 'Payroll Adjustment Earning',
                     'ចំណូលកែតម្រូវប្រាក់បៀវត្ស', 'EARNING', 'FORMULA', FALSE, FALSE, TRUE, 900,
                     :userId, :userId),
                    ('PAYROLL_ADJUSTMENT_DEDUCTION', 'Payroll Adjustment Deduction',
                     'ការកាត់កែតម្រូវប្រាក់បៀវត្ស', 'DEDUCTION', 'FORMULA', FALSE, FALSE, TRUE, 910,
                     :userId, :userId)
                ON CONFLICT (component_code) DO UPDATE SET
                    component_name_en = EXCLUDED.component_name_en,
                    component_name_kh = EXCLUDED.component_name_kh,
                    component_type = EXCLUDED.component_type,
                    calculation_method = EXCLUDED.calculation_method,
                    taxable = EXCLUDED.taxable,
                    subject_to_nssf = EXCLUDED.subject_to_nssf,
                    active = TRUE,
                    sort_order = EXCLUDED.sort_order,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = EXCLUDED.updated_by,
                    version = payroll_component.version + 1
                """)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int deleteResultItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_employee pe, public.payroll_component pc
                WHERE pe.payroll_employee_id = i.payroll_employee_id
                  AND pc.payroll_component_id = i.payroll_component_id
                  AND pe.payroll_run_id = :runId
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND pc.component_code IN
                      ('PAYROLL_ADJUSTMENT_EARNING','PAYROLL_ADJUSTMENT_DEDUCTION')
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int deleteBaselineItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_employee pe
                WHERE pe.payroll_employee_id = i.payroll_employee_id
                  AND pe.payroll_run_id = :runId
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND i.source_type = 'OTHER'
                  AND COALESCE(i.remarks, '') LIKE 'Adjustment baseline from regular item=%'
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int copyBaselineItems(
            Long periodId,
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH base_run AS (
                    SELECT payroll_run_id
                    FROM public.payroll_run
                    WHERE payroll_period_id = :periodId
                      AND run_type = 'REGULAR'
                      AND status IN ('APPROVED','PAID')
                    ORDER BY run_number DESC
                    LIMIT 1
                ), employee_pair AS (
                    SELECT current_employee.payroll_employee_id AS current_employee_id,
                           base_employee.payroll_employee_id AS base_employee_id
                    FROM public.payroll_employee current_employee
                    JOIN base_run br ON TRUE
                    JOIN public.payroll_employee base_employee
                      ON base_employee.payroll_run_id = br.payroll_run_id
                     AND base_employee.emp_id = current_employee.emp_id
                    WHERE current_employee.payroll_run_id = :runId
                      AND current_employee.payroll_status = 'INCLUDED'
                      AND base_employee.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR current_employee.payroll_employee_id = :payrollEmployeeId)
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT ep.current_employee_id,
                       base_item.payroll_component_id,
                       base_item.description,
                       base_item.quantity,
                       base_item.rate,
                       base_item.amount,
                       base_item.currency_code,
                       base_item.exchange_rate,
                       base_item.payroll_amount,
                       'OTHER',
                       'Adjustment baseline from regular item='
                           || base_item.payroll_employee_item_id::text
                           || '; ' || COALESCE(base_item.remarks, ''),
                       :userId
                FROM employee_pair ep
                JOIN public.payroll_employee_item base_item
                  ON base_item.payroll_employee_id = ep.base_employee_id
                JOIN public.payroll_component component
                  ON component.payroll_component_id = base_item.payroll_component_id
                WHERE base_item.source_type IN ('MANUAL','OTHER')
                  AND component.component_code NOT IN
                      ('PAYROLL_ADJUSTMENT_EARNING','PAYROLL_ADJUSTMENT_DEDUCTION')
                """)
                .setParameter("periodId", periodId)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public long countMissingApprovedRegularEmployees(
            Long periodId,
            Long runId,
            @Nullable Long payrollEmployeeId) {
        Object result = entityManager.createNativeQuery("""
                WITH base_run AS (
                    SELECT payroll_run_id
                    FROM public.payroll_run
                    WHERE payroll_period_id = :periodId
                      AND run_type = 'REGULAR'
                      AND status IN ('APPROVED','PAID')
                    ORDER BY run_number DESC
                    LIMIT 1
                )
                SELECT COUNT(*)
                FROM public.payroll_employee current_employee
                WHERE current_employee.payroll_run_id = :runId
                  AND current_employee.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR current_employee.payroll_employee_id = :payrollEmployeeId)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM base_run br
                      JOIN public.payroll_employee base_employee
                        ON base_employee.payroll_run_id = br.payroll_run_id
                       AND base_employee.emp_id = current_employee.emp_id
                       AND base_employee.payroll_status = 'INCLUDED'
                  )
                """)
                .setParameter("periodId", periodId)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    /**
     * Calculates differences for one employee or, when payrollEmployeeId is null,
     * for every INCLUDED employee in the adjustment run in one set-based statement.
     */
    public int insertDifferenceItems(
            Long periodId,
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH base_run AS (
                    SELECT payroll_run_id
                    FROM public.payroll_run
                    WHERE payroll_period_id = :periodId
                      AND run_type = 'REGULAR'
                      AND status IN ('APPROVED','PAID')
                    ORDER BY run_number DESC
                    LIMIT 1
                ), employee_pair AS (
                    SELECT current_employee.payroll_employee_id AS current_employee_id,
                           base_employee.payroll_employee_id AS base_employee_id
                    FROM public.payroll_employee current_employee
                    JOIN base_run br ON TRUE
                    JOIN public.payroll_employee base_employee
                      ON base_employee.payroll_run_id = br.payroll_run_id
                     AND base_employee.emp_id = current_employee.emp_id
                    WHERE current_employee.payroll_run_id = :runId
                      AND current_employee.payroll_status = 'INCLUDED'
                      AND base_employee.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR current_employee.payroll_employee_id = :payrollEmployeeId)
                ), current_amounts AS (
                    SELECT ep.current_employee_id,
                           c.component_code,
                           c.component_name_en,
                           c.component_type,
                           COALESCE(SUM(i.payroll_amount), 0) AS payroll_amount
                    FROM employee_pair ep
                    JOIN public.payroll_employee_item i
                      ON i.payroll_employee_id = ep.current_employee_id
                    JOIN public.payroll_component c
                      ON c.payroll_component_id = i.payroll_component_id
                    WHERE i.source_type IN
                        ('SALARY','ATTENDANCE','LEAVE','OVERTIME','SENIORITY','TAX','NSSF')
                      AND c.component_type IN ('EARNING','DEDUCTION')
                    GROUP BY ep.current_employee_id,
                             c.component_code,
                             c.component_name_en,
                             c.component_type
                ), base_amounts AS (
                    SELECT ep.current_employee_id,
                           c.component_code,
                           c.component_name_en,
                           c.component_type,
                           COALESCE(SUM(i.payroll_amount), 0) AS payroll_amount
                    FROM employee_pair ep
                    JOIN public.payroll_employee_item i
                      ON i.payroll_employee_id = ep.base_employee_id
                    JOIN public.payroll_component c
                      ON c.payroll_component_id = i.payroll_component_id
                    WHERE i.source_type IN
                        ('SALARY','ATTENDANCE','LEAVE','OVERTIME','SENIORITY','TAX','NSSF')
                      AND c.component_type IN ('EARNING','DEDUCTION')
                    GROUP BY ep.current_employee_id,
                             c.component_code,
                             c.component_name_en,
                             c.component_type
                ), prior_adjustments AS (
                    SELECT current_employee.payroll_employee_id AS current_employee_id,
                           substring(item.remarks from 'component=([^;]+)') AS component_code,
                           SUM(CASE
                               WHEN adjustment_component.component_code = 'PAYROLL_ADJUSTMENT_EARNING'
                               THEN item.payroll_amount
                               ELSE -item.payroll_amount
                           END) AS settled_difference
                    FROM public.payroll_employee current_employee
                    JOIN public.payroll_run prior_run
                      ON prior_run.payroll_period_id = :periodId
                     AND prior_run.run_type = 'ADJUSTMENT'
                     AND prior_run.status = 'PAID'
                     AND prior_run.payroll_run_id < :runId
                    JOIN public.payroll_employee prior_employee
                      ON prior_employee.payroll_run_id = prior_run.payroll_run_id
                     AND prior_employee.emp_id = current_employee.emp_id
                     AND prior_employee.payroll_status = 'INCLUDED'
                    JOIN public.payroll_employee_item item
                      ON item.payroll_employee_id = prior_employee.payroll_employee_id
                    JOIN public.payroll_component adjustment_component
                      ON adjustment_component.payroll_component_id = item.payroll_component_id
                     AND adjustment_component.component_code IN
                         ('PAYROLL_ADJUSTMENT_EARNING','PAYROLL_ADJUSTMENT_DEDUCTION')
                    WHERE current_employee.payroll_run_id = :runId
                      AND current_employee.payroll_status = 'INCLUDED'
                      AND COALESCE(item.remarks, '') LIKE 'Difference from regular run; component=%'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR current_employee.payroll_employee_id = :payrollEmployeeId)
                    GROUP BY current_employee.payroll_employee_id,
                             substring(item.remarks from 'component=([^;]+)')
                ), differences AS (
                    SELECT COALESCE(c.current_employee_id, b.current_employee_id)
                               AS current_employee_id,
                           COALESCE(c.component_code, b.component_code) AS component_code,
                           COALESCE(c.component_name_en, b.component_name_en) AS component_name,
                           COALESCE(c.component_type, b.component_type) AS component_type,
                           COALESCE(c.payroll_amount, 0) AS current_amount,
                           COALESCE(b.payroll_amount, 0) AS base_amount,
                           COALESCE(c.payroll_amount, 0) - COALESCE(b.payroll_amount, 0)
                               - COALESCE(pa.settled_difference, 0) AS amount_difference
                    FROM current_amounts c
                    FULL JOIN base_amounts b
                      ON b.current_employee_id = c.current_employee_id
                     AND b.component_code = c.component_code
                    LEFT JOIN prior_adjustments pa
                      ON pa.current_employee_id = COALESCE(c.current_employee_id, b.current_employee_id)
                     AND pa.component_code = COALESCE(c.component_code, b.component_code)
                ), payable_differences AS (
                    SELECT d.*,
                           CASE
                               WHEN (d.component_type = 'EARNING' AND d.amount_difference > 0)
                                 OR (d.component_type = 'DEDUCTION' AND d.amount_difference < 0)
                               THEN 'PAYROLL_ADJUSTMENT_EARNING'
                               ELSE 'PAYROLL_ADJUSTMENT_DEDUCTION'
                           END AS adjustment_component_code
                    FROM differences d
                    WHERE d.amount_difference <> 0
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT d.current_employee_id,
                       target.payroll_component_id,
                       'Adjustment for ' || d.component_name,
                       1,
                       ABS(d.amount_difference),
                       ABS(d.amount_difference),
                       period.payroll_currency,
                       1,
                       ABS(d.amount_difference),
                       'OTHER',
                       'Difference from regular run; component=' || d.component_code
                           || '; regular=' || d.base_amount::text
                           || '; corrected=' || d.current_amount::text,
                       :userId
                FROM payable_differences d
                JOIN public.payroll_component target
                  ON target.component_code = d.adjustment_component_code
                 AND target.active = TRUE
                JOIN public.payroll_period period
                  ON period.payroll_period_id = :periodId
                """)
                .setParameter("periodId", periodId)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int deleteCurrentCalculatedItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_employee pe
                WHERE pe.payroll_employee_id = i.payroll_employee_id
                  AND pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND i.source_type IN
                      ('SALARY','ATTENDANCE','LEAVE','OVERTIME','SENIORITY','TAX','NSSF')
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }
}
