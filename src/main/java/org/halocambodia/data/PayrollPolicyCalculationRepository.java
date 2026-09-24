package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Date;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * JPA/native-query data access for automatic attendance, leave, overtime and
 * attendance-allowance payroll calculations.
 */
@Repository
public class PayrollPolicyCalculationRepository {


    @PersistenceContext
    private EntityManager entityManager;

    public record PolicyContext(
            int year,
            int month,
            String payrollCurrency,
            LocalDate periodStart,
            LocalDate periodEnd) {
    }

    public @Nullable PolicyContext findContext(Long runId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT p.payroll_year,
                       p.payroll_month,
                       UPPER(p.payroll_currency) AS payroll_currency,
                       p.period_start,
                       p.period_end
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
        return new PolicyContext(
                number(row[0]).intValue(),
                number(row[1]).intValue(),
                string(row[2]),
                localDate(row[3]),
                localDate(row[4]));
    }

    public long countCurrencyMismatch(
            Long runId,
            @Nullable Long payrollEmployeeId,
            String payrollCurrency) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee pe
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND UPPER(COALESCE(pe.salary_currency, '')) <> :payrollCurrency
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("payrollCurrency", payrollCurrency));
    }

    public long countMissingWorkdays(
            Long runId,
            @Nullable Long payrollEmployeeId,
            LocalDate monthStart,
            LocalDate monthEnd) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee pe
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.emp_roster roster
                      LEFT JOIN public.holiday holiday
                        ON holiday.holiday_id = roster.holiday_id
                      WHERE roster.emp_id = pe.emp_id
                        AND roster.roster_date BETWEEN :monthStart AND :monthEnd
                        AND COALESCE(holiday.holiday_group_id NOT IN (2,3,4,6), TRUE)
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("monthStart", monthStart)
                .setParameter("monthEnd", monthEnd));
    }

    public long countMissingHourlyBasis(
            Long runId,
            @Nullable Long payrollEmployeeId,
            LocalDate monthStart,
            LocalDate monthEnd) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee pe
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND EXISTS (
                      SELECT 1
                      FROM public.payroll_attendance_day_snapshot_detail overtime_day
                      WHERE overtime_day.payroll_employee_id = pe.payroll_employee_id
                        AND COALESCE(overtime_day.overtime_hours, 0) > 0
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_attendance_day_snapshot_detail workday
                      WHERE workday.payroll_employee_id = pe.payroll_employee_id
                        AND workday.attendance_date BETWEEN :monthStart AND :monthEnd
                        AND workday.scheduled_workday = TRUE
                        AND COALESCE(workday.normal_working_hours, 0) > 0
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("monthStart", monthStart)
                .setParameter("monthEnd", monthEnd));
    }

    public long countMissingLeavePeriodSnapshot(
            Long runId,
            @Nullable Long payrollEmployeeId,
            String triggerCode) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_attendance_day_snapshot_detail d
                JOIN public.payroll_employee pe
                  ON pe.payroll_employee_id = d.payroll_employee_id
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND (
                      UPPER(TRIM(COALESCE(d.attendance_category, ''))) = :triggerCode
                      OR UPPER(TRIM(COALESCE(d.attendance_code, ''))) = :triggerCode
                  )
                  AND d.leave_period_start IS NULL
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("triggerCode", triggerCode));
    }

    public int deleteAutomaticPolicyItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item
                WHERE payroll_employee_id IN (
                    SELECT pe.payroll_employee_id
                    FROM public.payroll_employee pe
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                )
                  AND source_type IN ('ATTENDANCE','LEAVE','OVERTIME')
                  AND COALESCE(remarks, '') ~ '^Rule [A-Z0-9_]+;'
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int insertAttendanceDeduction(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long componentId,
            String ruleCode,
            BigDecimal multiplier,
            int workdayDivisor,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH ctx AS (
                    SELECT p.period_start,
                           p.period_end
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                ),
                basis AS (
                    SELECT pe.payroll_employee_id,
                           pe.basic_salary,
                           pe.salary_currency,
                           CAST(:standardWorkdays AS numeric) AS workdays
                    FROM public.payroll_employee pe
                    CROSS JOIN ctx x
                    LEFT JOIN public.emp_roster roster
                      ON roster.emp_id = pe.emp_id
                     AND roster.roster_date BETWEEN x.period_start AND x.period_end
                    LEFT JOIN public.holiday holiday
                      ON holiday.holiday_id = roster.holiday_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                    GROUP BY pe.payroll_employee_id, pe.emp_id, pe.basic_salary, pe.salary_currency,
                             x.period_start, x.period_end
                ),
                qty AS (
                    SELECT d.payroll_employee_id,
                           COALESCE(SUM(d.day_value), 0) AS deduction_days
                    FROM public.payroll_attendance_day_snapshot_detail d
                    JOIN public.payroll_employee pe
                      ON pe.payroll_employee_id = d.payroll_employee_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                      AND (
                          UPPER(TRIM(COALESCE(d.attendance_category, ''))) = :ruleCode
                          OR UPPER(TRIM(COALESCE(d.attendance_code, ''))) = :ruleCode
                      )
                    GROUP BY d.payroll_employee_id
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT b.payroll_employee_id,
                       :componentId,
                       :description,
                       q.deduction_days,
                       ROUND((b.basic_salary / b.workdays) * :multiplier, 6),
                       ROUND(q.deduction_days * (b.basic_salary / b.workdays) * :multiplier, 2),
                       b.salary_currency,
                       1,
                       ROUND(q.deduction_days * (b.basic_salary / b.workdays) * :multiplier, 2),
                       'ATTENDANCE',
                       'Rule ' || :ruleCode || '; daily rate='
                           || ROUND(b.basic_salary / b.workdays, 6)::text
                           || '; multiplier=' || CAST(:multiplier AS text)
                           || '; payroll-month workdays=' || b.workdays::text,
                       :userId
                FROM basis b
                JOIN qty q ON q.payroll_employee_id = b.payroll_employee_id
                WHERE q.deduction_days > 0
                  AND b.workdays > 0
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .setParameter("description", "Automatic " + ruleCode + " attendance deduction")
                .setParameter("ruleCode", ruleCode)
                .setParameter("multiplier", multiplier)
                .setParameter("standardWorkdays", workdayDivisor)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertLeavePayAdjustment(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long componentId,
            String category,
            String description,
            String ruleCode,
            int initialFullPayMonths,
            int maxPaidMonths,
            BigDecimal payPercentage,
            int workdayDivisor,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH ctx AS (
                    SELECT p.period_start,
                           p.period_end
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                ),
                basis AS (
                    SELECT pe.payroll_employee_id,
                           pe.basic_salary,
                           pe.salary_currency,
                           CAST(:standardWorkdays AS numeric) AS workdays
                    FROM public.payroll_employee pe
                    CROSS JOIN ctx x
                    LEFT JOIN public.emp_roster roster
                      ON roster.emp_id = pe.emp_id
                     AND roster.roster_date BETWEEN x.period_start AND x.period_end
                    LEFT JOIN public.holiday holiday
                      ON holiday.holiday_id = roster.holiday_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                    GROUP BY pe.payroll_employee_id, pe.emp_id, pe.basic_salary, pe.salary_currency,
                             x.period_start, x.period_end
                ),
                staged AS (
                    SELECT d.payroll_employee_id,
                           d.day_value,
                           (EXTRACT(YEAR FROM AGE(d.attendance_date, d.leave_period_start)) * 12
                            + EXTRACT(MONTH FROM AGE(d.attendance_date, d.leave_period_start))
                            + 1)::integer AS leave_month_number
                    FROM public.payroll_attendance_day_snapshot_detail d
                    JOIN public.payroll_employee pe
                      ON pe.payroll_employee_id = d.payroll_employee_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                      AND (
                          UPPER(TRIM(COALESCE(d.attendance_category, ''))) = :category
                          OR UPPER(TRIM(COALESCE(d.attendance_code, ''))) = :category
                      )
                      AND d.leave_period_start IS NOT NULL
                ),
                reduction AS (
                    SELECT s.payroll_employee_id,
                           SUM(s.day_value) AS leave_days,
                           SUM(s.day_value * CASE
                               WHEN s.leave_month_number <= :initialFullPayMonths THEN 0
                               WHEN s.leave_month_number <= :maxPaidMonths
                                   THEN (100 - :payPercentage) / 100
                               ELSE 1
                           END) AS reduction_days
                    FROM staged s
                    GROUP BY s.payroll_employee_id
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT b.payroll_employee_id,
                       :componentId,
                       :description,
                       r.leave_days,
                       CASE WHEN r.leave_days = 0 THEN 0
                            ELSE ROUND((r.reduction_days * (b.basic_salary / b.workdays))
                                       / r.leave_days, 6)
                       END,
                       ROUND(r.reduction_days * (b.basic_salary / b.workdays), 2),
                       b.salary_currency,
                       1,
                       ROUND(r.reduction_days * (b.basic_salary / b.workdays), 2),
                       'LEAVE',
                       'Rule ' || :ruleCode || '; pay=' || CAST(:payPercentage AS text)
                           || '%; leave days=' || r.leave_days::text
                           || '; reduction-equivalent days=' || ROUND(r.reduction_days, 4)::text
                           || '; daily rate=' || ROUND(b.basic_salary / b.workdays, 6)::text,
                       :userId
                FROM basis b
                JOIN reduction r ON r.payroll_employee_id = b.payroll_employee_id
                WHERE r.leave_days > 0
                  AND b.workdays > 0
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .setParameter("category", category)
                .setParameter("description", description)
                .setParameter("ruleCode", ruleCode)
                .setParameter("initialFullPayMonths", initialFullPayMonths)
                .setParameter("maxPaidMonths", maxPaidMonths)
                .setParameter("payPercentage", payPercentage)
                .setParameter("standardWorkdays", workdayDivisor)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertOvertimePay(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long componentId,
            String ruleCode,
            String rateUnit,
            BigDecimal multiplier,
            int workdayDivisor,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH ctx AS (
                    SELECT p.period_start,
                           p.period_end
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                ),
                basis AS (
                    SELECT pe.payroll_employee_id,
                           pe.basic_salary,
                           pe.salary_currency,
                           CAST(:standardWorkdays AS numeric) AS workdays,
                           (
                               SELECT AVG(NULLIF(d.normal_working_hours, 0))
                               FROM public.payroll_attendance_day_snapshot_detail d
                               WHERE d.payroll_employee_id = pe.payroll_employee_id
                                 AND d.attendance_date BETWEEN x.period_start AND x.period_end
                                 AND d.scheduled_workday = TRUE
                           ) AS hours_per_workday
                    FROM public.payroll_employee pe
                    CROSS JOIN ctx x
                    LEFT JOIN public.emp_roster roster
                      ON roster.emp_id = pe.emp_id
                     AND roster.roster_date BETWEEN x.period_start AND x.period_end
                    LEFT JOIN public.holiday holiday
                      ON holiday.holiday_id = roster.holiday_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                    GROUP BY pe.payroll_employee_id, pe.emp_id, pe.basic_salary, pe.salary_currency,
                             x.period_start, x.period_end
                ),
                qty AS (
                    SELECT d.payroll_employee_id,
                           COALESCE(SUM(CASE
                               WHEN :rateUnit = 'HOUR' THEN COALESCE(d.overtime_hours, 0)
                               WHEN UPPER(TRIM(COALESCE(d.attendance_code, ''))) = :ruleCode
                                   THEN d.day_value
                               ELSE 0
                           END), 0) AS quantity
                    FROM public.payroll_attendance_day_snapshot_detail d
                    JOIN public.payroll_employee pe
                      ON pe.payroll_employee_id = d.payroll_employee_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                    GROUP BY d.payroll_employee_id
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT b.payroll_employee_id,
                       :componentId,
                       :description,
                       q.quantity,
                       CASE WHEN :rateUnit = 'HOUR'
                            THEN ROUND(((b.basic_salary / b.workdays) / b.hours_per_workday)
                                       * :multiplier, 6)
                            ELSE ROUND((b.basic_salary / b.workdays) * :multiplier, 6)
                       END,
                       CASE WHEN :rateUnit = 'HOUR'
                            THEN ROUND(q.quantity * ((b.basic_salary / b.workdays)
                                       / b.hours_per_workday) * :multiplier, 2)
                            ELSE ROUND(q.quantity * (b.basic_salary / b.workdays)
                                       * :multiplier, 2)
                       END,
                       b.salary_currency,
                       1,
                       CASE WHEN :rateUnit = 'HOUR'
                            THEN ROUND(q.quantity * ((b.basic_salary / b.workdays)
                                       / b.hours_per_workday) * :multiplier, 2)
                            ELSE ROUND(q.quantity * (b.basic_salary / b.workdays)
                                       * :multiplier, 2)
                       END,
                       'OVERTIME',
                       'Rule ' || :ruleCode || '; unit=' || :rateUnit
                           || '; multiplier=' || CAST(:multiplier AS text)
                           || '; quantity=' || q.quantity::text
                           || '; base rate=' || CASE WHEN :rateUnit = 'HOUR'
                               THEN ROUND((b.basic_salary / b.workdays)
                                    / b.hours_per_workday, 6)::text
                               ELSE ROUND(b.basic_salary / b.workdays, 6)::text
                           END,
                       :userId
                FROM basis b
                JOIN qty q ON q.payroll_employee_id = b.payroll_employee_id
                WHERE q.quantity > 0
                  AND b.workdays > 0
                  AND (:rateUnit <> 'HOUR' OR COALESCE(b.hours_per_workday, 0) > 0)
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .setParameter("description", "Automatic " + ruleCode + " overtime pay")
                .setParameter("ruleCode", ruleCode)
                .setParameter("rateUnit", rateUnit)
                .setParameter("multiplier", multiplier)
                .setParameter("standardWorkdays", workdayDivisor)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public long countAttendanceCodeOccurrences(
            Long runId,
            @Nullable Long payrollEmployeeId,
            String ruleCode) {
        return count(entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_attendance_day_snapshot_detail d
                JOIN public.payroll_employee pe
                  ON pe.payroll_employee_id = d.payroll_employee_id
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND UPPER(TRIM(COALESCE(d.attendance_code, ''))) = :ruleCode
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("ruleCode", ruleCode));
    }

    public int insertAttendanceAllowance(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long componentId,
            String ruleCode,
            String rateUnit,
            BigDecimal rateAmount,
            String rateCurrency,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH qty AS (
                    SELECT d.payroll_employee_id,
                           CASE
                               WHEN :rateUnit IN ('CYCLE','MONTH') THEN 1::numeric
                               ELSE COALESCE(SUM(d.day_value), 0)
                           END AS quantity
                    FROM public.payroll_attendance_day_snapshot_detail d
                    JOIN public.payroll_employee pe
                      ON pe.payroll_employee_id = d.payroll_employee_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                      AND UPPER(TRIM(COALESCE(d.attendance_code, ''))) = :ruleCode
                    GROUP BY d.payroll_employee_id
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT q.payroll_employee_id,
                       :componentId,
                       'Automatic ' || :ruleCode || ' attendance allowance',
                       q.quantity,
                       :rateAmount,
                       ROUND(q.quantity * :rateAmount, 2),
                       :rateCurrency,
                       1,
                       ROUND(q.quantity * :rateAmount, 2),
                       'ATTENDANCE',
                       'Rule ' || :ruleCode || '; unit=' || :rateUnit
                           || '; rate=' || CAST(:rateAmount AS text) || ' ' || :rateCurrency,
                       :userId
                FROM qty q
                WHERE q.quantity > 0
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .setParameter("ruleCode", ruleCode)
                .setParameter("rateUnit", rateUnit)
                .setParameter("rateAmount", rateAmount)
                .setParameter("rateCurrency", rateCurrency)
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

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static LocalDate localDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
