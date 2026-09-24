package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** JPA/native-query execution layer for NSSF payroll calculation. */
@Repository
public class PayrollNssfCalculationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public record NssfContext(
            LocalDate paymentDate,
            String payrollCurrency,
            BigDecimal nssfUsdToKhrRate) {
    }

    public @Nullable NssfContext findContext(Long runId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT p.payment_date,
                       UPPER(p.payroll_currency) AS payroll_currency,
                       p.nssf_usd_to_khr_rate
                FROM public.payroll_run r
                JOIN public.payroll_period p
                  ON p.payroll_period_id = r.payroll_period_id
                WHERE r.payroll_run_id = :runId
                """)
                .setParameter("runId", runId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = (Object[]) rows.getFirst();
        return new NssfContext(
                localDate(row[0]),
                string(row[1]),
                decimal(row[2]));
    }

    @SuppressWarnings("unchecked")
    public List<String> findCoverageIssues(Long runId, @Nullable Long payrollEmployeeId) {
        return ((List<Object>) entityManager.createNativeQuery("""
                WITH employee_contracts AS (
                    SELECT DISTINCT em.contract_type_id,
                           COALESCE(ct.contract_type_name,
                                    'Contract type not assigned') AS contract_type_name,
                           p.payment_date
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    JOIN public.payroll_employee pe
                      ON pe.payroll_run_id = r.payroll_run_id
                     AND pe.payroll_status = 'INCLUDED'
                    JOIN public.emp_master em ON em.emp_id = pe.emp_id
                    LEFT JOIN public.contract_type ct
                      ON ct.contract_type_id = em.contract_type_id
                    WHERE r.payroll_run_id = :runId
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                ), resolved AS (
                    SELECT ec.*,
                           selected.payroll_nssf_config_id,
                           selected.currency,
                           selected.band_count
                    FROM employee_contracts ec
                    LEFT JOIN LATERAL (
                        SELECT c.payroll_nssf_config_id,
                               c.currency,
                               (SELECT COUNT(*)
                                FROM public.payroll_nssf_wage_band band
                                WHERE band.payroll_nssf_config_id = c.payroll_nssf_config_id)
                                   AS band_count
                        FROM public.payroll_nssf_config c
                        WHERE c.active = TRUE
                          AND ec.payment_date >= c.effective_from
                          AND (c.effective_to IS NULL OR ec.payment_date <= c.effective_to)
                          AND c.eligible_contract_type_id = ec.contract_type_id
                        ORDER BY c.effective_from DESC,
                                 c.payroll_nssf_config_id DESC
                        LIMIT 1
                    ) selected ON TRUE
                )
                SELECT DISTINCT CASE
                           WHEN UPPER(currency) <> 'KHR' THEN
                               'NSSF rule for ' || contract_type_name || ' must use KHR currency'
                           WHEN band_count = 0 THEN
                               'NSSF rule for ' || contract_type_name || ' has no wage bands'
                       END AS issue
                FROM resolved
                WHERE payroll_nssf_config_id IS NOT NULL
                  AND (UPPER(currency) <> 'KHR' OR band_count = 0)
                ORDER BY issue
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getResultList())
                .stream()
                .map(String::valueOf)
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findApplicableConfigIds(Long runId, @Nullable Long payrollEmployeeId) {
        return ((List<Object>) entityManager.createNativeQuery("""
                WITH employee_contracts AS (
                    SELECT DISTINCT em.contract_type_id,
                           p.payment_date
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    JOIN public.payroll_employee pe
                      ON pe.payroll_run_id = r.payroll_run_id
                     AND pe.payroll_status = 'INCLUDED'
                    JOIN public.emp_master em ON em.emp_id = pe.emp_id
                    WHERE r.payroll_run_id = :runId
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                )
                SELECT DISTINCT selected.payroll_nssf_config_id
                FROM employee_contracts ec
                JOIN LATERAL (
                    SELECT c.payroll_nssf_config_id
                    FROM public.payroll_nssf_config c
                    WHERE c.active = TRUE
                      AND ec.payment_date >= c.effective_from
                      AND (c.effective_to IS NULL OR ec.payment_date <= c.effective_to)
                      AND c.eligible_contract_type_id = ec.contract_type_id
                    ORDER BY c.effective_from DESC,
                             c.payroll_nssf_config_id DESC
                    LIMIT 1
                ) selected ON TRUE
                ORDER BY selected.payroll_nssf_config_id
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getResultList())
                .stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }

    public int deleteExistingItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_component c
                WHERE c.payroll_component_id = i.payroll_component_id
                  AND c.component_code IN (
                      'NSSF_EMPLOYEE', 'NSSF_EMPLOYER',
                      'NSSF_HEALTH_EMPLOYEE', 'NSSF_HEALTH_EMPLOYER',
                      'NSSF_RISK_EMPLOYEE', 'NSSF_RISK_EMPLOYER',
                      'NSSF_PENSION_EMPLOYEE', 'NSSF_PENSION_EMPLOYER'
                  )
                  AND i.payroll_employee_id IN (
                      SELECT pe.payroll_employee_id
                      FROM public.payroll_employee pe
                      WHERE pe.payroll_run_id = :runId
                        AND pe.payroll_status = 'INCLUDED'
                        AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                             OR pe.payroll_employee_id = :payrollEmployeeId)
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int insertCalculatedItems(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long healthEmployeeComponentId,
            Long healthEmployerComponentId,
            Long riskEmployeeComponentId,
            Long riskEmployerComponentId,
            Long pensionEmployeeComponentId,
            Long pensionEmployerComponentId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH nssf_context AS (
                    SELECT c.payroll_nssf_config_id,
                           c.eligible_contract_type_id,
                           c.effective_from,
                           p.nssf_usd_to_khr_rate,
                           c.health_employee_rate,
                           c.health_employer_rate,
                           c.risk_employee_rate,
                           c.risk_employer_rate,
                           c.pension_employee_rate,
                           c.pension_employer_rate,
                           c.pension_min_wage,
                           c.pension_max_wage
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    JOIN public.payroll_nssf_config c
                      ON c.active = TRUE
                     AND p.payment_date >= c.effective_from
                     AND (c.effective_to IS NULL OR p.payment_date <= c.effective_to)
                    WHERE r.payroll_run_id = :runId
                ), eligible_earnings AS (
                    SELECT pe.payroll_employee_id,
                           em.contract_type_id,
                           COALESCE(SUM(i.payroll_amount) FILTER (
                               WHERE pc.component_type = 'EARNING'
                                 AND pc.subject_to_nssf = TRUE), 0) AS nssf_earnings_usd
                    FROM public.payroll_employee pe
                    JOIN public.emp_master em ON em.emp_id = pe.emp_id
                    LEFT JOIN public.payroll_employee_item i
                      ON i.payroll_employee_id = pe.payroll_employee_id
                    LEFT JOIN public.payroll_component pc
                      ON pc.payroll_component_id = i.payroll_component_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                    GROUP BY pe.payroll_employee_id, em.contract_type_id
                ), bases AS (
                    SELECT e.payroll_employee_id,
                           x.*,
                           e.nssf_earnings_usd,
                           e.nssf_earnings_usd * x.nssf_usd_to_khr_rate AS gross_base_khr,
                           band.contributory_wage AS health_risk_base_khr,
                           CASE
                               WHEN e.nssf_earnings_usd <= 0 THEN 0
                               ELSE LEAST(
                                   GREATEST(e.nssf_earnings_usd * x.nssf_usd_to_khr_rate,
                                            x.pension_min_wage),
                                   x.pension_max_wage)
                           END AS pension_base_khr
                    FROM eligible_earnings e
                    JOIN LATERAL (
                        SELECT candidate.*
                        FROM nssf_context candidate
                        WHERE candidate.eligible_contract_type_id = e.contract_type_id
                        ORDER BY candidate.effective_from DESC,
                                 candidate.payroll_nssf_config_id DESC
                        LIMIT 1
                    ) x ON TRUE
                    LEFT JOIN LATERAL (
                        SELECT b.contributory_wage
                        FROM public.payroll_nssf_wage_band b
                        WHERE b.payroll_nssf_config_id = x.payroll_nssf_config_id
                          AND (b.max_salary IS NULL
                               OR e.nssf_earnings_usd * x.nssf_usd_to_khr_rate <= b.max_salary)
                        ORDER BY b.band_order
                        LIMIT 1
                    ) band ON TRUE
                ), calculated AS (
                    SELECT b.*,
                           ROUND(COALESCE(b.health_risk_base_khr, 0)
                                 * b.health_employee_rate / 100, 0) AS health_employee_khr,
                           ROUND(COALESCE(b.health_risk_base_khr, 0)
                                 * b.health_employer_rate / 100, 0) AS health_employer_khr,
                           ROUND(COALESCE(b.health_risk_base_khr, 0)
                                 * b.risk_employee_rate / 100, 0) AS risk_employee_khr,
                           ROUND(COALESCE(b.health_risk_base_khr, 0)
                                 * b.risk_employer_rate / 100, 0) AS risk_employer_khr,
                           ROUND(b.pension_base_khr * b.pension_employee_rate / 100, 0)
                               AS pension_employee_khr,
                           ROUND(b.pension_base_khr * b.pension_employer_rate / 100, 0)
                               AS pension_employer_khr
                    FROM bases b
                ), payable AS (
                    SELECT c.*
                    FROM calculated c
                    WHERE c.nssf_earnings_usd > 0
                      AND c.health_risk_base_khr IS NOT NULL
                ), lines AS (
                    SELECT t.payroll_employee_id,
                           CAST(:healthEmployeeComponentId AS bigint) AS component_id,
                           'NSSF Healthcare - Employee | ប.ស.ស. ថែទាំសុខភាព - និយោជិត'::text AS description,
                           'HEALTHCARE'::text AS scheme,
                           'EMPLOYEE'::text AS contribution_side,
                           t.health_employee_khr AS total_khr,
                           t.health_employee_rate AS rate_percent,
                           t.health_risk_base_khr AS contribution_base_khr,
                           t.nssf_usd_to_khr_rate,
                           t.payroll_nssf_config_id,
                           t.eligible_contract_type_id
                    FROM payable t
                    UNION ALL
                    SELECT t.payroll_employee_id,
                           CAST(:healthEmployerComponentId AS bigint),
                           'NSSF Healthcare - Employer | ប.ស.ស. ថែទាំសុខភាព - និយោជក'::text,
                           'HEALTHCARE'::text,
                           'EMPLOYER'::text,
                           t.health_employer_khr,
                           t.health_employer_rate,
                           t.health_risk_base_khr,
                           t.nssf_usd_to_khr_rate,
                           t.payroll_nssf_config_id,
                           t.eligible_contract_type_id
                    FROM payable t
                    UNION ALL
                    SELECT t.payroll_employee_id,
                           CAST(:riskEmployeeComponentId AS bigint),
                           'NSSF Occupational Risk - Employee | ប.ស.ស. ហានិភ័យការងារ - និយោជិត'::text,
                           'OCCUPATIONAL_RISK'::text,
                           'EMPLOYEE'::text,
                           t.risk_employee_khr,
                           t.risk_employee_rate,
                           t.health_risk_base_khr,
                           t.nssf_usd_to_khr_rate,
                           t.payroll_nssf_config_id,
                           t.eligible_contract_type_id
                    FROM payable t
                    UNION ALL
                    SELECT t.payroll_employee_id,
                           CAST(:riskEmployerComponentId AS bigint),
                           'NSSF Occupational Risk - Employer | ប.ស.ស. ហានិភ័យការងារ - និយោជក'::text,
                           'OCCUPATIONAL_RISK'::text,
                           'EMPLOYER'::text,
                           t.risk_employer_khr,
                           t.risk_employer_rate,
                           t.health_risk_base_khr,
                           t.nssf_usd_to_khr_rate,
                           t.payroll_nssf_config_id,
                           t.eligible_contract_type_id
                    FROM payable t
                    UNION ALL
                    SELECT t.payroll_employee_id,
                           CAST(:pensionEmployeeComponentId AS bigint),
                           'NSSF Pension - Employee | ប.ស.ស. សោធន - និយោជិត'::text,
                           'PENSION'::text,
                           'EMPLOYEE'::text,
                           t.pension_employee_khr,
                           t.pension_employee_rate,
                           t.pension_base_khr,
                           t.nssf_usd_to_khr_rate,
                           t.payroll_nssf_config_id,
                           t.eligible_contract_type_id
                    FROM payable t
                    UNION ALL
                    SELECT t.payroll_employee_id,
                           CAST(:pensionEmployerComponentId AS bigint),
                           'NSSF Pension - Employer | ប.ស.ស. សោធន - និយោជក'::text,
                           'PENSION'::text,
                           'EMPLOYER'::text,
                           t.pension_employer_khr,
                           t.pension_employer_rate,
                           t.pension_base_khr,
                           t.nssf_usd_to_khr_rate,
                           t.payroll_nssf_config_id,
                           t.eligible_contract_type_id
                    FROM payable t
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT l.payroll_employee_id,
                       l.component_id,
                       l.description,
                       1,
                       l.rate_percent,
                       l.total_khr,
                       'KHR',
                       1 / l.nssf_usd_to_khr_rate,
                       ROUND(l.total_khr / l.nssf_usd_to_khr_rate, 2),
                       'NSSF',
                       'Scheme=' || l.scheme
                           || '; side=' || l.contribution_side
                           || '; rule_id=' || l.payroll_nssf_config_id::text
                           || '; contract_type_id=' || COALESCE(l.eligible_contract_type_id::text, 'ALL')
                           || '; base=' || ROUND(l.contribution_base_khr, 0)::text || ' KHR'
                           || '; rate=' || l.rate_percent::text || '%'
                           || '; nssf_fx=' || l.nssf_usd_to_khr_rate::text || ' KHR/USD',
                       :userId
                FROM lines l
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("healthEmployeeComponentId", healthEmployeeComponentId)
                .setParameter("healthEmployerComponentId", healthEmployerComponentId)
                .setParameter("riskEmployeeComponentId", riskEmployeeComponentId)
                .setParameter("riskEmployerComponentId", riskEmployerComponentId)
                .setParameter("pensionEmployeeComponentId", pensionEmployeeComponentId)
                .setParameter("pensionEmployerComponentId", pensionEmployerComponentId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private static LocalDate localDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
