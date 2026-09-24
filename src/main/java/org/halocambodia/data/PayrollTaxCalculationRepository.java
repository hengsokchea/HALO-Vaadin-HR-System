package org.halocambodia.data;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/** JPA/native-query execution layer for salary-tax calculation. */
@Repository
public class PayrollTaxCalculationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public record TaxContext(
            int taxYear,
            String payrollCurrency,
            BigDecimal usdToKhrRate,
            Long taxConfigId,
            String taxCurrency) {
    }

    public @Nullable TaxContext findContext(Long runId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT p.payroll_year,
                       UPPER(p.payroll_currency) AS payroll_currency,
                       p.usd_to_khr_rate,
                       tc.payroll_tax_config_id,
                       UPPER(tc.currency) AS tax_currency
                FROM public.payroll_run r
                JOIN public.payroll_period p
                  ON p.payroll_period_id = r.payroll_period_id
                LEFT JOIN public.payroll_tax_config tc
                  ON tc.tax_year = p.payroll_year
                 AND tc.active = TRUE
                WHERE r.payroll_run_id = :runId
                """)
                .setParameter("runId", runId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = (Object[]) rows.getFirst();
        return new TaxContext(
                number(row[0]).intValue(),
                string(row[1]),
                decimal(row[2]),
                row[3] == null ? null : number(row[3]).longValue(),
                string(row[4]));
    }

    public long countIncludedEmployees(Long runId, @Nullable Long payrollEmployeeId) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee
                WHERE payroll_run_id = :runId
                  AND payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR payroll_employee_id = :payrollEmployeeId)
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId));
    }

    public int deleteExistingTaxItems(
            Long salaryTaxComponentId,
            Long runId,
            @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item
                WHERE payroll_component_id = :salaryTaxComponentId
                  AND payroll_employee_id IN (
                      SELECT pe.payroll_employee_id
                      FROM public.payroll_employee pe
                      WHERE pe.payroll_run_id = :runId
                        AND pe.payroll_status = 'INCLUDED'
                        AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                             OR pe.payroll_employee_id = :payrollEmployeeId)
                  )
                """)
                .setParameter("salaryTaxComponentId", salaryTaxComponentId)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int insertCalculatedTaxItems(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long salaryTaxComponentId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH tax_context AS (
                    SELECT p.payroll_year,
                           p.usd_to_khr_rate,
                           tc.payroll_tax_config_id,
                           tc.dependent_allowance
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    JOIN public.payroll_tax_config tc
                      ON tc.tax_year = p.payroll_year
                     AND tc.active = TRUE
                    WHERE r.payroll_run_id = :runId
                ), taxable_earnings AS (
                    SELECT pe.payroll_employee_id,
                           COALESCE(pe.spouse_count, 0) AS spouse_count,
                           COALESCE(pe.tax_dependent_count, 0) AS tax_dependent_count,
                           GREATEST(
                               COALESCE(SUM(i.payroll_amount) FILTER (
                                   WHERE pc.component_type = 'EARNING'
                                     AND pc.taxable = TRUE), 0)
                               - COALESCE(SUM(i.payroll_amount) FILTER (
                                   WHERE pc.component_code IN (
                                       'ABSENCE_DEDUCTION',
                                       'SICK_LEAVE_DEDUCTION',
                                       'MATERNITY_DEDUCTION'
                                   )), 0),
                               0
                           ) AS taxable_usd
                    FROM public.payroll_employee pe
                    LEFT JOIN public.payroll_employee_item i
                      ON i.payroll_employee_id = pe.payroll_employee_id
                    LEFT JOIN public.payroll_component pc
                      ON pc.payroll_component_id = i.payroll_component_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                    GROUP BY pe.payroll_employee_id,
                             pe.spouse_count,
                             pe.tax_dependent_count
                ), tax_base AS (
                    SELECT e.payroll_employee_id,
                           x.payroll_year,
                           x.usd_to_khr_rate,
                           x.payroll_tax_config_id,
                           e.taxable_usd * x.usd_to_khr_rate AS taxable_salary_khr,
                           x.dependent_allowance * (e.spouse_count + e.tax_dependent_count)
                               AS allowance_khr
                    FROM taxable_earnings e
                    CROSS JOIN tax_context x
                ), normalized AS (
                    SELECT b.*,
                           ROUND(GREATEST(b.taxable_salary_khr - b.allowance_khr, 0), 0)
                               AS taxable_base_khr
                    FROM tax_base b
                ), calculated AS (
                    SELECT n.*,
                           bracket.tax_rate,
                           bracket.deduction_amount,
                           ROUND(GREATEST(
                               n.taxable_base_khr * bracket.tax_rate / 100
                               - bracket.deduction_amount,
                               0), 0) AS tax_khr
                    FROM normalized n
                    JOIN LATERAL (
                        SELECT b.tax_rate,
                               b.deduction_amount
                        FROM public.payroll_tax_bracket b
                        WHERE b.payroll_tax_config_id = n.payroll_tax_config_id
                          AND n.taxable_base_khr >= b.min_amount
                          AND (b.max_amount IS NULL
                               OR n.taxable_base_khr <= b.max_amount)
                        ORDER BY b.bracket_order
                        LIMIT 1
                    ) bracket ON TRUE
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT c.payroll_employee_id,
                       :salaryTaxComponentId,
                       'Automatic salary tax - ' || c.payroll_year,
                       1,
                       c.tax_rate,
                       c.tax_khr,
                       'KHR',
                       1 / c.usd_to_khr_rate,
                       ROUND(c.tax_khr / c.usd_to_khr_rate, 2),
                       'TAX',
                       'Taxable base: ' || ROUND(c.taxable_base_khr, 0)::text
                           || ' KHR; rate: ' || c.tax_rate::text
                           || '%; allowance: ' || ROUND(c.allowance_khr, 0)::text || ' KHR',
                       :userId
                FROM calculated c
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("salaryTaxComponentId", salaryTaxComponentId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private static long count(Query query) {
        Object value = query.getSingleResult();
        return value == null ? 0L : number(value).longValue();
    }

    private static Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
