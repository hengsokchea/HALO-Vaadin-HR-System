package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/** JPA/native-query execution layer for seniority payroll calculations. */
@Repository
public class PayrollSeniorityCalculationDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public record SeniorityRunContext(
            String runType,
            int payrollYear,
            int payrollMonth,
            String payrollCurrency) {
    }

    public @Nullable SeniorityRunContext findRunContext(Long runId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT r.run_type,
                       p.payroll_year,
                       p.payroll_month,
                       UPPER(p.payroll_currency) AS payroll_currency
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
        return new SeniorityRunContext(
                string(row[0]),
                number(row[1]).intValue(),
                number(row[2]).intValue(),
                string(row[3]));
    }

    public long countActiveRules(LocalDate semesterEnd, int payrollMonth) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_seniority_rule r
                WHERE r.active = TRUE
                  AND r.effective_from <= :semesterEnd
                  AND (r.effective_to IS NULL OR r.effective_to >= :semesterEnd)
                  AND :payrollMonth IN (r.first_payment_month, r.second_payment_month)
                """)
                .setParameter("semesterEnd", semesterEnd)
                .setParameter("payrollMonth", payrollMonth));
    }

    public long countOverlappingActiveRules(LocalDate semesterEnd, int payrollMonth) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM (
                    SELECT eligible_contract_type_id
                    FROM public.payroll_seniority_rule r
                    WHERE r.active = TRUE
                      AND r.effective_from <= :semesterEnd
                      AND (r.effective_to IS NULL OR r.effective_to >= :semesterEnd)
                      AND :payrollMonth IN (r.first_payment_month, r.second_payment_month)
                    GROUP BY eligible_contract_type_id
                    HAVING COUNT(*) > 1
                ) duplicate_rule
                """)
                .setParameter("semesterEnd", semesterEnd)
                .setParameter("payrollMonth", payrollMonth));
    }

    public int deleteSeniorityItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_employee pe, public.payroll_component component
                WHERE pe.payroll_employee_id = i.payroll_employee_id
                  AND component.payroll_component_id = i.payroll_component_id
                  AND pe.payroll_run_id = :runId
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND (i.source_type = 'SENIORITY'
                       OR component.component_code = 'SENIORITY_PAYMENT')
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int deleteSeniorityCalculations(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_seniority_calculation c
                USING public.payroll_employee pe
                WHERE pe.payroll_employee_id = c.payroll_employee_id
                  AND pe.payroll_run_id = :runId
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int insertSeniorityCalculations(
            Long runId,
            @Nullable Long payrollEmployeeId,
            LocalDate semesterStart,
            LocalDate semesterEnd,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH run_context AS (
                    SELECT r.payroll_run_id,
                           r.run_type,
                           p.payroll_year,
                           p.payroll_month,
                           UPPER(p.payroll_currency) AS payroll_currency,
                           CAST(:semesterStart AS date) AS semester_start,
                           CAST(:semesterEnd AS date) AS semester_end,
                           CASE WHEN p.payroll_month = 6 THEN 1 ELSE 2 END AS semester_no
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                ), eligible_employees AS (
                    SELECT pe.payroll_employee_id,
                           pe.emp_id,
                           pe.salary_currency,
                           sr.payroll_seniority_rule_id,
                           sr.days_per_payment,
                           sr.minimum_eligible_days,
                           sr.workday_divisor,
                           x.payroll_year,
                           x.payroll_month,
                           x.semester_no,
                           x.semester_start,
                           x.semester_end,
                           GREATEST(e.join_date, e.probation_end_date + 1, x.semester_start)
                               AS eligibility_start,
                           LEAST(
                               CASE
                                   WHEN career_group.career_type_group_name IS NOT NULL
                                    AND UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                                    AND e.last_career_type_date IS NOT NULL
                                   THEN e.last_career_type_date - 1
                                   ELSE x.semester_end
                               END,
                               x.semester_end
                           ) AS eligibility_end
                    FROM public.payroll_employee pe
                    JOIN public.emp_master e ON e.emp_id = pe.emp_id
                    LEFT JOIN public.list_career_type career_type
                      ON career_type.career_type_id = e.emp_status_career_type_id
                    LEFT JOIN public.list_career_type_group career_group
                      ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
                    CROSS JOIN run_context x
                    JOIN public.payroll_seniority_rule sr
                      ON sr.eligible_contract_type_id = e.contract_type_id
                     AND sr.active = TRUE
                     AND sr.effective_from <= x.semester_end
                     AND (sr.effective_to IS NULL OR sr.effective_to >= x.semester_end)
                     AND x.payroll_month IN (sr.first_payment_month, sr.second_payment_month)
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                      AND (
                          sr.require_employed_at_semester_end = FALSE
                          OR career_group.career_type_group_name IS NULL
                          OR UPPER(TRIM(career_group.career_type_group_name)) = 'ACTIVE'
                          OR e.last_career_type_date IS NULL
                          OR e.last_career_type_date > x.semester_end
                      )
                ), eligible_employment AS (
                    SELECT ee.*,
                           (ee.eligibility_end - ee.eligibility_start + 1) AS eligible_calendar_days
                    FROM eligible_employees ee
                    WHERE ee.eligibility_end >= ee.eligibility_start
                      AND (ee.eligibility_end - ee.eligibility_start + 1) >= ee.minimum_eligible_days
                ), earning_run_candidates AS (
                    SELECT ee.payroll_employee_id AS current_payroll_employee_id,
                           ee.emp_id,
                           ee.payroll_seniority_rule_id,
                           ee.days_per_payment,
                           ee.minimum_eligible_days,
                           ee.workday_divisor,
                           ee.payroll_year,
                           ee.semester_no,
                           ee.semester_start,
                           ee.semester_end,
                           ee.eligibility_start,
                           ee.eligibility_end,
                           ee.eligible_calendar_days,
                           ee.salary_currency,
                           history_employee.payroll_employee_id AS history_payroll_employee_id,
                           period.payroll_month AS earning_month,
                           ROW_NUMBER() OVER (
                               PARTITION BY ee.payroll_employee_id, period.payroll_month
                               ORDER BY CASE WHEN history_run.payroll_run_id = :runId THEN 0 ELSE 1 END,
                                        history_run.run_number DESC
                           ) AS run_priority
                    FROM eligible_employment ee
                    JOIN public.payroll_period period
                      ON period.payroll_year = ee.payroll_year
                     AND period.payroll_month BETWEEN
                         CASE WHEN ee.semester_no = 1 THEN 1 ELSE 7 END
                         AND CASE WHEN ee.semester_no = 1 THEN 6 ELSE 12 END
                     AND period.period_end >= ee.eligibility_start
                    JOIN public.payroll_run history_run
                      ON history_run.payroll_period_id = period.payroll_period_id
                     AND (
                         history_run.payroll_run_id = :runId
                         OR (history_run.run_type = 'REGULAR'
                             AND history_run.status IN ('APPROVED','PAID'))
                     )
                    JOIN public.payroll_employee history_employee
                      ON history_employee.payroll_run_id = history_run.payroll_run_id
                     AND history_employee.emp_id = ee.emp_id
                     AND history_employee.payroll_status = 'INCLUDED'
                ), month_earnings AS (
                    SELECT c.current_payroll_employee_id,
                           c.payroll_seniority_rule_id,
                           c.days_per_payment,
                           c.minimum_eligible_days,
                           c.workday_divisor,
                           c.payroll_year,
                           c.semester_no,
                           c.semester_start,
                           c.semester_end,
                           c.eligibility_start,
                           c.eligibility_end,
                           c.eligible_calendar_days,
                           c.salary_currency,
                           c.earning_month,
                           COALESCE(SUM(i.payroll_amount), 0) AS eligible_earnings
                    FROM earning_run_candidates c
                    JOIN public.payroll_employee_item i
                      ON i.payroll_employee_id = c.history_payroll_employee_id
                    JOIN public.payroll_component component
                      ON component.payroll_component_id = i.payroll_component_id
                     AND component.component_type = 'EARNING'
                     AND component.subject_to_seniority = TRUE
                     AND component.component_code <> 'SENIORITY_PAYMENT'
                    WHERE c.run_priority = 1
                    GROUP BY c.current_payroll_employee_id,
                             c.payroll_seniority_rule_id,
                             c.days_per_payment,
                             c.minimum_eligible_days,
                             c.workday_divisor,
                             c.payroll_year,
                             c.semester_no,
                             c.semester_start,
                             c.semester_end,
                             c.eligibility_start,
                             c.eligibility_end,
                             c.eligible_calendar_days,
                             c.salary_currency,
                             c.earning_month
                ), semester_earnings AS (
                    SELECT m.current_payroll_employee_id,
                           m.payroll_seniority_rule_id,
                           m.days_per_payment,
                           m.workday_divisor,
                           m.payroll_year,
                           m.semester_no,
                           m.semester_start,
                           m.semester_end,
                           m.eligibility_start,
                           m.eligibility_end,
                           m.eligible_calendar_days,
                           m.salary_currency,
                           COUNT(*)::integer AS eligible_month_count,
                           ROUND(SUM(m.eligible_earnings), 2) AS total_eligible_earnings
                    FROM month_earnings m
                    GROUP BY m.current_payroll_employee_id,
                             m.payroll_seniority_rule_id,
                             m.days_per_payment,
                             m.workday_divisor,
                             m.payroll_year,
                             m.semester_no,
                             m.semester_start,
                             m.semester_end,
                             m.eligibility_start,
                             m.eligibility_end,
                             m.eligible_calendar_days,
                             m.salary_currency
                ), calculated AS (
                    SELECT s.*,
                           ROUND(s.total_eligible_earnings / s.eligible_month_count, 2)
                               AS average_monthly_earnings,
                           ROUND((s.total_eligible_earnings / s.eligible_month_count)
                                 / s.workday_divisor, 6) AS average_daily_earnings
                    FROM semester_earnings s
                    WHERE s.eligible_month_count > 0
                      AND s.total_eligible_earnings > 0
                )
                INSERT INTO public.payroll_seniority_calculation
                    (payroll_employee_id, payroll_seniority_rule_id,
                     seniority_year, semester_no, semester_start, semester_end,
                     eligibility_start, eligibility_end, eligible_calendar_days,
                     eligible_month_count, total_eligible_earnings,
                     average_monthly_earnings, workday_divisor,
                     average_daily_earnings, entitlement_days, amount,
                     currency_code, created_by)
                SELECT c.current_payroll_employee_id,
                       c.payroll_seniority_rule_id,
                       c.payroll_year,
                       c.semester_no,
                       c.semester_start,
                       c.semester_end,
                       c.eligibility_start,
                       c.eligibility_end,
                       c.eligible_calendar_days,
                       c.eligible_month_count,
                       c.total_eligible_earnings,
                       c.average_monthly_earnings,
                       c.workday_divisor,
                       c.average_daily_earnings,
                       c.days_per_payment,
                       ROUND(c.average_daily_earnings * c.days_per_payment, 2),
                       c.salary_currency,
                       :userId
                FROM calculated c
                """)
                .setParameter("semesterStart", semesterStart)
                .setParameter("semesterEnd", semesterEnd)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertSeniorityPayrollItems(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long componentId,
            Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description,
                     quantity, rate, amount, currency_code, exchange_rate,
                     payroll_amount, source_type, remarks, created_by)
                SELECT c.payroll_employee_id,
                       :componentId,
                       'Seniority Payment | ប្រាក់បំណាច់អតីតភាពការងារ',
                       c.entitlement_days,
                       c.average_daily_earnings,
                       c.amount,
                       c.currency_code,
                       1,
                       c.amount,
                       'SENIORITY',
                       'Year=' || c.seniority_year::text
                           || '; semester=' || c.semester_no::text
                           || '; eligible months=' || c.eligible_month_count::text
                           || '; semester earnings=' || c.total_eligible_earnings::text
                           || '; monthly average=' || c.average_monthly_earnings::text
                           || '; divisor=' || c.workday_divisor::text
                           || '; daily average=' || c.average_daily_earnings::text
                           || '; days=' || c.entitlement_days::text,
                       :userId
                FROM public.payroll_seniority_calculation c
                JOIN public.payroll_employee pe
                  ON pe.payroll_employee_id = c.payroll_employee_id
                WHERE pe.payroll_run_id = :runId
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                """)
                .setParameter("componentId", componentId)
                .setParameter("userId", userId)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public long countEligibleEarningComponents() {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_component
                WHERE active = TRUE
                  AND component_type = 'EARNING'
                  AND subject_to_seniority = TRUE
                  AND component_code <> 'SENIORITY_PAYMENT'
                """));
    }

    public long countRuleOverlap(
            Long contractTypeId,
            @Nullable Long ruleId,
            @Nullable LocalDate effectiveTo,
            LocalDate effectiveFrom) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_seniority_rule existing
                WHERE existing.active = TRUE
                  AND existing.eligible_contract_type_id = :contractTypeId
                  AND (CAST(:ruleId AS BIGINT) IS NULL
                       OR existing.payroll_seniority_rule_id <> :ruleId)
                  AND existing.effective_from <= COALESCE(CAST(:effectiveTo AS date), DATE '9999-12-31')
                  AND COALESCE(existing.effective_to, DATE '9999-12-31') >= CAST(:effectiveFrom AS date)
                """)
                .setParameter("contractTypeId", contractTypeId)
                .setParameter("ruleId", ruleId)
                .setParameter("effectiveTo", effectiveTo)
                .setParameter("effectiveFrom", effectiveFrom));
    }

    public int invalidateCalculatedRuns() {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_run
                SET status = 'DRAFT',
                    calculated_at = NULL,
                    calculated_by = NULL,
                    version = version + 1
                WHERE status = 'CALCULATED'
                """)
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

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
