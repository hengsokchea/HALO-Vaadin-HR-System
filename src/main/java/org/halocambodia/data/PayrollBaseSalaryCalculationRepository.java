package org.halocambodia.data;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Native PostgreSQL operations for BASIC_SALARY payroll items.
 *
 * Heavy set-based SQL belongs here; workflow and validation stay in services.
 */
@Repository
public class PayrollBaseSalaryCalculationRepository {


    @PersistenceContext
    private EntityManager entityManager;

    public int insertMonthlyItems(Long runId, Long userId) {
        // BASIC_SALARY is automatic. Rebuild it so Join Date / Leaving Date
        // changes are reflected whenever the payroll is recalculated.
        deleteItems(runId, null);
        return entityManager.createNativeQuery("""
                WITH ctx AS (
                    SELECT p.period_start AS cycle_start,
                           p.period_end AS cycle_end
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                ), roster_days AS (
                    SELECT pe.payroll_employee_id,
                           roster.roster_date,
                           COALESCE(holiday.holiday_group_id NOT IN (2,3,4,6), TRUE)
                               AS scheduled_workday,
                           GREATEST(c.cycle_start, master.join_date) AS employment_start,
                           LEAST(
                               c.cycle_end,
                               CASE
                                   WHEN career_group.career_type_group_name IS NOT NULL
                                    AND UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                                    AND master.last_career_type_date IS NOT NULL
                                   THEN master.last_career_type_date - 1
                                   ELSE c.cycle_end
                               END
                           ) AS employment_end
                    FROM public.payroll_employee pe
                    JOIN public.emp_master master
                      ON master.emp_id = pe.emp_id
                    LEFT JOIN public.list_career_type career_type
                      ON career_type.career_type_id = master.emp_status_career_type_id
                    LEFT JOIN public.list_career_type_group career_group
                      ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
                    JOIN public.emp_roster roster
                      ON roster.emp_id = pe.emp_id
                    LEFT JOIN public.holiday holiday
                      ON holiday.holiday_id = roster.holiday_id
                    CROSS JOIN ctx c
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND roster.roster_date BETWEEN c.cycle_start AND c.cycle_end
                ), roster_workdays AS (
                    SELECT payroll_employee_id,
                           COUNT(*) FILTER (
                               WHERE scheduled_workday
                           )::numeric AS roster_full_workdays,
                           COUNT(*) FILTER (
                               WHERE scheduled_workday
                                 AND roster_date BETWEEN employment_start AND employment_end
                           )::numeric AS payable_workdays,
                           MIN(employment_start) AS employment_start,
                           MAX(employment_end) AS employment_end
                    FROM roster_days
                    GROUP BY payroll_employee_id
                ), calendar_workdays AS (
                    SELECT pe.payroll_employee_id,
                           COUNT(detail.cycle_date) FILTER (
                               WHERE COALESCE(calendar_holiday.holiday_group_id NOT IN (2,3,4,6), TRUE)
                           )::numeric AS full_workdays
                    FROM public.payroll_employee pe
                    CROSS JOIN ctx c
                    LEFT JOIN public.emp_personnel_allocation allocation
                      ON allocation.emp_id = pe.emp_id
                     AND allocation.year_number = EXTRACT(YEAR FROM c.cycle_start)::integer
                     AND allocation.month_number = EXTRACT(MONTH FROM c.cycle_start)::integer
                    LEFT JOIN public.shift_cycle_detail detail
                      ON detail.shift_cycle_id = allocation.shift_cycle_id
                     AND detail.cycle_date BETWEEN c.cycle_start AND c.cycle_end
                    LEFT JOIN public.holiday calendar_holiday
                      ON calendar_holiday.holiday_id = detail.holiday_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                    GROUP BY pe.payroll_employee_id
                ), workdays AS (
                    SELECT rw.payroll_employee_id,
                           CAST(:standardWorkdays AS numeric) AS full_workdays,
                           CASE
                               WHEN rw.employment_start <= c.cycle_start
                                AND rw.employment_end >= c.cycle_end
                               THEN CAST(:standardWorkdays AS numeric)
                               ELSE LEAST(rw.payable_workdays, CAST(:standardWorkdays AS numeric))
                           END AS payable_workdays,
                           rw.employment_start,
                           rw.employment_end
                    FROM roster_workdays rw
                    CROSS JOIN ctx c
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT pe.payroll_employee_id,
                       pc.payroll_component_id,
                       'Monthly basic salary',
                       ROUND(w.payable_workdays / w.full_workdays, 4),
                       pe.basic_salary,
                       ROUND(pe.basic_salary * w.payable_workdays / w.full_workdays, 2),
                       pe.salary_currency,
                       1,
                       ROUND(pe.basic_salary * w.payable_workdays / w.full_workdays, 2),
                       'SALARY',
                       'Employment window=' || w.employment_start::text || ' to '
                           || w.employment_end::text
                           || '; payable workdays=' || w.payable_workdays::text
                           || '; full workdays=' || w.full_workdays::text,
                       :userId
                FROM public.payroll_employee pe
                JOIN workdays w
                  ON w.payroll_employee_id = pe.payroll_employee_id
                JOIN public.payroll_component pc
                  ON pc.component_code = 'BASIC_SALARY'
                 AND pc.component_type = 'EARNING'
                 AND pc.active = TRUE
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND w.full_workdays > 0
                  AND w.payable_workdays > 0
                  AND w.employment_start <= w.employment_end
                """)
                .setParameter("runId", runId)
                .setParameter("standardWorkdays", PayrollCalculationConstants.STANDARD_MONTH_WORKDAYS)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int ensureMonthlyItem(Long payrollEmployeeId, Long componentId, Long userId) {
        entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item
                WHERE payroll_employee_id = :payrollEmployeeId
                  AND payroll_component_id = :componentId
                  AND source_type = 'SALARY'
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .executeUpdate();

        return entityManager.createNativeQuery("""
                WITH ctx AS (
                    SELECT p.period_start AS cycle_start,
                           p.period_end AS cycle_end
                    FROM public.payroll_employee pe
                    JOIN public.payroll_run r
                      ON r.payroll_run_id = pe.payroll_run_id
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE pe.payroll_employee_id = :payrollEmployeeId
                ), roster_days AS (
                    SELECT pe.payroll_employee_id,
                           roster.roster_date,
                           COALESCE(holiday.holiday_group_id NOT IN (2,3,4,6), TRUE)
                               AS scheduled_workday,
                           GREATEST(c.cycle_start, master.join_date) AS employment_start,
                           LEAST(
                               c.cycle_end,
                               CASE
                                   WHEN career_group.career_type_group_name IS NOT NULL
                                    AND UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                                    AND master.last_career_type_date IS NOT NULL
                                   THEN master.last_career_type_date - 1
                                   ELSE c.cycle_end
                               END
                           ) AS employment_end
                    FROM public.payroll_employee pe
                    JOIN public.emp_master master
                      ON master.emp_id = pe.emp_id
                    LEFT JOIN public.list_career_type career_type
                      ON career_type.career_type_id = master.emp_status_career_type_id
                    LEFT JOIN public.list_career_type_group career_group
                      ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
                    JOIN public.emp_roster roster
                      ON roster.emp_id = pe.emp_id
                    LEFT JOIN public.holiday holiday
                      ON holiday.holiday_id = roster.holiday_id
                    CROSS JOIN ctx c
                    WHERE pe.payroll_employee_id = :payrollEmployeeId
                      AND pe.payroll_status = 'INCLUDED'
                      AND roster.roster_date BETWEEN c.cycle_start AND c.cycle_end
                ), roster_workdays AS (
                    SELECT payroll_employee_id,
                           COUNT(*) FILTER (
                               WHERE scheduled_workday
                           )::numeric AS roster_full_workdays,
                           COUNT(*) FILTER (
                               WHERE scheduled_workday
                                 AND roster_date BETWEEN employment_start AND employment_end
                           )::numeric AS payable_workdays,
                           MIN(employment_start) AS employment_start,
                           MAX(employment_end) AS employment_end
                    FROM roster_days
                    GROUP BY payroll_employee_id
                ), calendar_workdays AS (
                    SELECT pe.payroll_employee_id,
                           COUNT(detail.cycle_date) FILTER (
                               WHERE COALESCE(calendar_holiday.holiday_group_id NOT IN (2,3,4,6), TRUE)
                           )::numeric AS full_workdays
                    FROM public.payroll_employee pe
                    CROSS JOIN ctx c
                    LEFT JOIN public.emp_personnel_allocation allocation
                      ON allocation.emp_id = pe.emp_id
                     AND allocation.year_number = EXTRACT(YEAR FROM c.cycle_start)::integer
                     AND allocation.month_number = EXTRACT(MONTH FROM c.cycle_start)::integer
                    LEFT JOIN public.shift_cycle_detail detail
                      ON detail.shift_cycle_id = allocation.shift_cycle_id
                     AND detail.cycle_date BETWEEN c.cycle_start AND c.cycle_end
                    LEFT JOIN public.holiday calendar_holiday
                      ON calendar_holiday.holiday_id = detail.holiday_id
                    WHERE pe.payroll_employee_id = :payrollEmployeeId
                      AND pe.payroll_status = 'INCLUDED'
                    GROUP BY pe.payroll_employee_id
                ), workdays AS (
                    SELECT rw.payroll_employee_id,
                           CAST(:standardWorkdays AS numeric) AS full_workdays,
                           CASE
                               WHEN rw.employment_start <= c.cycle_start
                                AND rw.employment_end >= c.cycle_end
                               THEN CAST(:standardWorkdays AS numeric)
                               ELSE LEAST(rw.payable_workdays, CAST(:standardWorkdays AS numeric))
                           END AS payable_workdays,
                           rw.employment_start,
                           rw.employment_end
                    FROM roster_workdays rw
                    CROSS JOIN ctx c
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT pe.payroll_employee_id,
                       :componentId,
                       'Monthly basic salary',
                       ROUND(w.payable_workdays / w.full_workdays, 4),
                       pe.basic_salary,
                       ROUND(pe.basic_salary * w.payable_workdays / w.full_workdays, 2),
                       pe.salary_currency,
                       1,
                       ROUND(pe.basic_salary * w.payable_workdays / w.full_workdays, 2),
                       'SALARY',
                       'Employment window=' || w.employment_start::text || ' to '
                           || w.employment_end::text
                           || '; payable workdays=' || w.payable_workdays::text
                           || '; full workdays=' || w.full_workdays::text,
                       :userId
                FROM public.payroll_employee pe
                JOIN workdays w
                  ON w.payroll_employee_id = pe.payroll_employee_id
                WHERE pe.payroll_employee_id = :payrollEmployeeId
                  AND pe.payroll_status = 'INCLUDED'
                  AND w.full_workdays > 0
                  AND w.payable_workdays > 0
                  AND w.employment_start <= w.employment_end
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .setParameter("standardWorkdays", PayrollCalculationConstants.STANDARD_MONTH_WORKDAYS)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int deleteItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_employee pe, public.payroll_component pc
                WHERE pe.payroll_employee_id = i.payroll_employee_id
                  AND pc.payroll_component_id = i.payroll_component_id
                  AND pe.payroll_run_id = :runId
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND pc.component_code = 'BASIC_SALARY'
                  AND i.source_type = 'SALARY'
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int insertFinalPaymentItems(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long salaryComponentId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH ctx AS (
                    SELECT p.period_start AS cycle_start,
                           p.period_end AS cycle_end
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                ), roster_days AS (
                    SELECT pe.payroll_employee_id,
                           roster.roster_date,
                           COALESCE(holiday.holiday_group_id NOT IN (2,3,4,6), TRUE)
                               AS scheduled_workday,
                           GREATEST(c.cycle_start, master.join_date) AS employment_start,
                           LEAST(
                               c.cycle_end,
                               CASE
                                   WHEN career_group.career_type_group_name IS NOT NULL
                                    AND UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                                    AND master.last_career_type_date IS NOT NULL
                                   THEN master.last_career_type_date - 1
                                   ELSE c.cycle_end
                               END
                           ) AS employment_end
                    FROM public.payroll_employee pe
                    JOIN public.emp_master master
                      ON master.emp_id = pe.emp_id
                    LEFT JOIN public.list_career_type career_type
                      ON career_type.career_type_id = master.emp_status_career_type_id
                    LEFT JOIN public.list_career_type_group career_group
                      ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
                    JOIN public.emp_roster roster
                      ON roster.emp_id = pe.emp_id
                    LEFT JOIN public.holiday holiday
                      ON holiday.holiday_id = roster.holiday_id
                    CROSS JOIN ctx c
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                      AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                           OR pe.payroll_employee_id = :payrollEmployeeId)
                      AND roster.roster_date BETWEEN c.cycle_start AND c.cycle_end
                ), roster_workdays AS (
                    SELECT payroll_employee_id,
                           COUNT(*) FILTER (
                               WHERE scheduled_workday
                           )::numeric AS roster_full_workdays,
                           COUNT(*) FILTER (
                               WHERE scheduled_workday
                                 AND roster_date BETWEEN employment_start AND employment_end
                           )::numeric AS payable_workdays,
                           MIN(employment_start) AS employment_start,
                           MAX(employment_end) AS employment_end
                    FROM roster_days
                    GROUP BY payroll_employee_id
                ), calendar_workdays AS (
                    SELECT pe.payroll_employee_id,
                           COUNT(detail.cycle_date) FILTER (
                               WHERE COALESCE(calendar_holiday.holiday_group_id NOT IN (2,3,4,6), TRUE)
                           )::numeric AS full_workdays
                    FROM public.payroll_employee pe
                    CROSS JOIN ctx c
                    LEFT JOIN public.emp_personnel_allocation allocation
                      ON allocation.emp_id = pe.emp_id
                     AND allocation.year_number = EXTRACT(YEAR FROM c.cycle_start)::integer
                     AND allocation.month_number = EXTRACT(MONTH FROM c.cycle_start)::integer
                    LEFT JOIN public.shift_cycle_detail detail
                      ON detail.shift_cycle_id = allocation.shift_cycle_id
                     AND detail.cycle_date BETWEEN c.cycle_start AND c.cycle_end
                    LEFT JOIN public.holiday calendar_holiday
                      ON calendar_holiday.holiday_id = detail.holiday_id
                    WHERE pe.payroll_run_id = :runId
                      AND pe.payroll_status = 'INCLUDED'
                    GROUP BY pe.payroll_employee_id
                ), workdays AS (
                    SELECT rw.payroll_employee_id,
                           CAST(:standardWorkdays AS numeric) AS full_workdays,
                           CASE
                               WHEN rw.employment_start <= c.cycle_start
                                AND rw.employment_end >= c.cycle_end
                               THEN CAST(:standardWorkdays AS numeric)
                               ELSE LEAST(rw.payable_workdays, CAST(:standardWorkdays AS numeric))
                           END AS payable_workdays,
                           rw.employment_start,
                           rw.employment_end
                    FROM roster_workdays rw
                    CROSS JOIN ctx c
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description, quantity, rate,
                     amount, currency_code, exchange_rate, payroll_amount, source_type,
                     remarks, created_by)
                SELECT pe.payroll_employee_id,
                       :salaryComponentId,
                       'Final payment basic salary through ' || w.employment_end::text,
                       ROUND(w.payable_workdays / w.full_workdays, 4),
                       pe.basic_salary,
                       ROUND(pe.basic_salary * w.payable_workdays / w.full_workdays, 2),
                       pe.salary_currency,
                       1,
                       ROUND(pe.basic_salary * w.payable_workdays / w.full_workdays, 2),
                       'SALARY',
                       'Final payment; employment window=' || w.employment_start::text || ' to '
                           || w.employment_end::text
                           || '; payable workdays=' || w.payable_workdays::text
                           || '; full workdays=' || w.full_workdays::text,
                       :userId
                FROM public.payroll_employee pe
                JOIN workdays w
                  ON w.payroll_employee_id = pe.payroll_employee_id
                WHERE w.full_workdays > 0
                  AND w.payable_workdays > 0
                  AND w.employment_start <= w.employment_end
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee_item i
                      WHERE i.payroll_employee_id = pe.payroll_employee_id
                        AND i.payroll_component_id = :salaryComponentId
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("salaryComponentId", salaryComponentId)
                .setParameter("standardWorkdays", PayrollCalculationConstants.STANDARD_MONTH_WORKDAYS)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
