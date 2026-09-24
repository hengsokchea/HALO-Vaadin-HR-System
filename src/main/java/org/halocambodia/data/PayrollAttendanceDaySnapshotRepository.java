package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

public interface PayrollAttendanceDaySnapshotRepository
        extends JpaRepository<PayrollAttendanceDaySnapshot, Long>,
        JpaSpecificationExecutor<PayrollAttendanceDaySnapshot> {

    List<PayrollAttendanceDaySnapshot> findByPayrollEmployeeIdOrderByAttendanceDateAsc(Long payrollEmployeeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM public.payroll_attendance_day_snapshot snapshot
            WHERE snapshot.payroll_employee_id IN (
                SELECT employee.payroll_employee_id
                FROM public.payroll_employee employee
                WHERE employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR employee.payroll_employee_id = :payrollEmployeeId)
            )
            """, nativeQuery = true)
    int deleteForPayrollRun(
            @Param("runId") Long runId,
            @Nullable @Param("payrollEmployeeId") Long payrollEmployeeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO public.payroll_attendance_day_snapshot
                (payroll_employee_id, attendance_date, leave_type_id,
                 payroll_policy_rule_id, holiday_id,
                 day_value, overtime_hours, normal_working_hours,
                 scheduled_workday, leave_period_start, source_type)
            SELECT employee.payroll_employee_id,
                   approved.attendance_date,
                   approved.leave_type_id,
                   approved.payroll_policy_rule_id,
                   approved.holiday_id,
                   approved.day_value,
                   approved.overtime_hours,
                   approved.normal_working_hours,
                   approved.scheduled_workday,
                   approved.leave_period_start,
                   approved.source_type
            FROM public.payroll_approved_attendance_day approved
            JOIN public.payroll_employee employee
              ON employee.emp_id = approved.emp_id
             AND employee.payroll_run_id = :runId
             AND employee.payroll_status = 'INCLUDED'
             AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                  OR employee.payroll_employee_id = :payrollEmployeeId)
            JOIN public.payroll_run payroll_run
              ON payroll_run.payroll_run_id = employee.payroll_run_id
            JOIN public.payroll_period payroll_period
              ON payroll_period.payroll_period_id = payroll_run.payroll_period_id
            JOIN public.emp_master master
              ON master.emp_id = employee.emp_id
            LEFT JOIN public.list_career_type career_type
              ON career_type.career_type_id = master.emp_status_career_type_id
            LEFT JOIN public.list_career_type_group career_group
              ON career_group.list_career_type_group_id = career_type.list_career_type_group_id
            WHERE approved.payroll_period_id = :periodId
              AND approved.attendance_date >= GREATEST(payroll_period.period_start, master.join_date)
              AND approved.attendance_date <= LEAST(
                  payroll_period.period_end,
                  CASE
                      WHEN career_group.career_type_group_name IS NOT NULL
                       AND UPPER(TRIM(career_group.career_type_group_name)) = 'INACTIVE'
                       AND master.last_career_type_date IS NOT NULL
                      THEN master.last_career_type_date - 1
                      ELSE payroll_period.period_end
                  END
              )
            """, nativeQuery = true)
    int copyApprovedAttendanceToRun(
            @Param("runId") Long runId,
            @Param("periodId") Long periodId,
            @Nullable @Param("payrollEmployeeId") Long payrollEmployeeId);

    /**
     * Builds an ADJUSTMENT attendance snapshot without changing the frozen
     * approved-attendance table used by the original REGULAR payroll.
     *
     * Days on/before the payroll payment date are copied from the latest
     * approved/paid REGULAR run (the exact payroll baseline). Days after the
     * payment date are rebuilt from the latest attendance + roster data, so
     * late attendance entered after payroll can be reconciled automatically.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            WITH selected_period AS (
                SELECT payroll_period_id, payroll_year, period_start, period_end,
                       COALESCE(payment_date, period_end) AS payment_date
                FROM public.payroll_period
                WHERE payroll_period_id = :periodId
            ), base_run AS (
                SELECT r.payroll_run_id
                FROM public.payroll_run r
                WHERE r.payroll_period_id = :periodId
                  AND r.run_type = 'REGULAR'
                  AND r.status IN ('APPROVED','PAID')
                ORDER BY r.run_number DESC
                LIMIT 1
            ), employee_pair AS (
                SELECT current_employee.payroll_employee_id AS current_employee_id,
                       current_employee.emp_id,
                       base_employee.payroll_employee_id AS base_employee_id
                FROM public.payroll_employee current_employee
                JOIN base_run br ON TRUE
                JOIN public.payroll_employee base_employee
                  ON base_employee.payroll_run_id = br.payroll_run_id
                 AND base_employee.emp_id = current_employee.emp_id
                 AND base_employee.payroll_status = 'INCLUDED'
                WHERE current_employee.payroll_run_id = :runId
                  AND current_employee.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR current_employee.payroll_employee_id = :payrollEmployeeId)
            ), frozen_days AS (
                SELECT ep.current_employee_id AS payroll_employee_id,
                       d.attendance_date, d.leave_type_id, d.payroll_policy_rule_id,
                       d.holiday_id, d.day_value, d.overtime_hours,
                       d.normal_working_hours, d.scheduled_workday,
                       d.leave_period_start, d.source_type
                FROM employee_pair ep
                JOIN public.payroll_attendance_day_snapshot d
                  ON d.payroll_employee_id = ep.base_employee_id
                JOIN selected_period p ON TRUE
                WHERE d.attendance_date <= p.payment_date
            ), attendance_ranked AS (
                SELECT ad.attendance_detail_id,
                       ad.attendance_emp_id AS emp_id,
                       ad.attendance_date, ad.leave_type_id, ad.leave_duration,
                       ad.number_of_day, ad.overtime_hours,
                       COALESCE(attendance_rule.rule_code, CASE
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%DEMINER%PARAMEDIC%' THEN 'ADP'
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%ACTING%TEAM%COMMANDER%' THEN 'ATC'
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%SENIOR%TEAM%COMMANDER%' THEN 'STC'
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%ACTING%SUPERVISOR%' THEN 'ASO'
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%CHAINSAW%' THEN 'CHS'
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%CLUTERNATOR%' THEN 'CLT'
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%HSTAMIDS%' THEN 'HST'
                           WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, '')) LIKE '%STRIMMER%' THEN 'STR'
                           WHEN REPLACE(UPPER(COALESCE(subtype.leave_sub_type_name_en, '')), '-', ' ') LIKE '%SUB UNIT%COMMANDER%' THEN 'SUC'
                           ELSE NULL END) AS operational_code,
                       ROW_NUMBER() OVER (
                           PARTITION BY ad.attendance_emp_id, ad.attendance_date
                           ORDER BY ad.hr_verification_date DESC NULLS LAST,
                                    ad.attendance_detail_id DESC) AS row_number,
                       SUM(COALESCE(ad.overtime_hours, 0)) OVER (
                           PARTITION BY ad.attendance_emp_id, ad.attendance_date) AS overtime_hours_total
                FROM selected_period p
                JOIN employee_pair ep
                  ON TRUE
                JOIN public.emp_attendance_detail ad
                  ON ad.attendance_emp_id = ep.emp_id
                 AND ad.attendance_date > p.payment_date
                 AND ad.attendance_date <= p.period_end
                LEFT JOIN public.list_leave_type_sub_type subtype
                  ON subtype.list_leave_type_sub_type_id = ad.leave_sub_type_id
                LEFT JOIN LATERAL (
                    SELECT UPPER(TRIM(rule.rule_code)) AS rule_code
                    FROM public.payroll_policy_rule rule
                    WHERE rule.rule_year = p.payroll_year
                      AND rule.active = TRUE
                      AND rule.calculation_source = 'ATTENDANCE'
                      AND rule.rule_type IN ('DEDUCTION','OVERTIME','ALLOWANCE')
                      AND (UPPER(COALESCE(ad.remark, '')) ~ ('(^|[^A-Z0-9])' || UPPER(TRIM(rule.rule_code)) || '([^A-Z0-9]|$)')
                           OR UPPER(TRIM(COALESCE(subtype.remarks, ''))) = UPPER(TRIM(rule.rule_code))
                           OR UPPER(TRIM(COALESCE(subtype.leave_sub_type_name_en, ''))) = UPPER(TRIM(rule.rule_code)))
                    ORDER BY LENGTH(TRIM(rule.rule_code)) DESC
                    LIMIT 1
                ) attendance_rule ON TRUE
            ), attendance_day AS (
                SELECT * FROM attendance_ranked WHERE row_number = 1
            ), live_base AS (
                SELECT ep.current_employee_id AS payroll_employee_id,
                       roster.roster_date AS attendance_date,
                       COALESCE(attendance.leave_type_id, roster.leave_type_id, holiday.leave_type_id,
                                CASE WHEN holiday.holiday_id IS NULL THEN present_type.leave_type_id END) AS leave_type_id,
                       holiday.holiday_id,
                       CASE WHEN attendance.number_of_day IS NOT NULL THEN attendance.number_of_day::numeric
                            WHEN COALESCE(attendance.leave_duration::text, roster.leave_duration::text, '')
                                 IN ('MORNING_ONLY','AFTERNOON_ONLY') THEN 0.5 ELSE 1 END::numeric AS day_value,
                       COALESCE(attendance.overtime_hours_total, 0) AS overtime_hours,
                       roster.total_working_hour AS normal_working_hours,
                       COALESCE(holiday.holiday_group_id NOT IN (2,3,4,6), TRUE) AS scheduled_workday,
                       COALESCE(attendance.operational_code, roster_rule.rule_code, CASE
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%DEMINER%PARAMEDIC%' THEN 'ADP'
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%ACTING%TEAM%COMMANDER%' THEN 'ATC'
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%SENIOR%TEAM%COMMANDER%' THEN 'STC'
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%ACTING%SUPERVISOR%' THEN 'ASO'
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%CHAINSAW%' THEN 'CHS'
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%CLUTERNATOR%' THEN 'CLT'
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%HSTAMIDS%' THEN 'HST'
                           WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')) LIKE '%STRIMMER%' THEN 'STR'
                           WHEN REPLACE(UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')), '-', ' ') LIKE '%SUB UNIT%COMMANDER%' THEN 'SUC'
                           ELSE NULL END) AS operational_code,
                       CASE WHEN attendance.attendance_detail_id IS NOT NULL THEN 'ATTENDANCE'
                            WHEN roster.leave_type_id IS NOT NULL THEN 'ROSTER LEAVE'
                            WHEN holiday.holiday_id IS NOT NULL THEN 'HOLIDAY'
                            ELSE 'ROSTER' END AS source_type,
                       ep.emp_id
                FROM selected_period p
                JOIN employee_pair ep ON TRUE
                JOIN public.emp_roster roster
                  ON roster.emp_id = ep.emp_id
                 AND roster.roster_date > p.payment_date
                 AND roster.roster_date <= p.period_end
                LEFT JOIN attendance_day attendance
                  ON attendance.emp_id = roster.emp_id
                 AND attendance.attendance_date = roster.roster_date
                LEFT JOIN public.list_leave_type_sub_type roster_subtype
                  ON roster_subtype.list_leave_type_sub_type_id = roster.leave_type_sub_type_id
                LEFT JOIN public.holiday holiday ON holiday.holiday_id = roster.holiday_id
                LEFT JOIN LATERAL (
                    SELECT UPPER(TRIM(rule.rule_code)) AS rule_code
                    FROM public.payroll_policy_rule rule
                    WHERE rule.rule_year = p.payroll_year AND rule.active = TRUE
                      AND rule.calculation_source = 'ATTENDANCE'
                      AND rule.rule_type IN ('DEDUCTION','OVERTIME','ALLOWANCE')
                      AND (UPPER(COALESCE(roster.remark, '')) ~ ('(^|[^A-Z0-9])' || UPPER(TRIM(rule.rule_code)) || '([^A-Z0-9]|$)')
                           OR UPPER(TRIM(COALESCE(roster_subtype.remarks, ''))) = UPPER(TRIM(rule.rule_code))
                           OR UPPER(TRIM(COALESCE(roster_subtype.leave_sub_type_name_en, ''))) = UPPER(TRIM(rule.rule_code)))
                    ORDER BY LENGTH(TRIM(rule.rule_code)) DESC LIMIT 1
                ) roster_rule ON TRUE
                LEFT JOIN LATERAL (
                    SELECT leave_type.leave_type_id
                    FROM public.list_leave_type leave_type
                    WHERE UPPER(TRIM(leave_type.leav_type_code)) = 'PR'
                      AND (leave_type.obsolete_date IS NULL OR leave_type.obsolete_date >= roster.roster_date)
                    ORDER BY leave_type.leave_type_id LIMIT 1
                ) present_type ON TRUE
            ), live_days AS (
                SELECT lb.payroll_employee_id, lb.attendance_date, lb.leave_type_id,
                       matched_rule.payroll_policy_rule_id, lb.holiday_id, lb.day_value,
                       lb.overtime_hours, lb.normal_working_hours, lb.scheduled_workday,
                       leave_period.leave_period_start, lb.source_type
                FROM live_base lb
                JOIN selected_period p ON TRUE
                LEFT JOIN LATERAL (
                    SELECT rule.payroll_policy_rule_id
                    FROM public.payroll_policy_rule rule
                    WHERE rule.rule_year = p.payroll_year AND rule.active = TRUE
                      AND rule.calculation_source = 'ATTENDANCE'
                      AND rule.rule_type IN ('DEDUCTION','OVERTIME','ALLOWANCE')
                      AND UPPER(TRIM(rule.rule_code)) = UPPER(TRIM(COALESCE(lb.operational_code, '')))
                    ORDER BY rule.sort_order, rule.payroll_policy_rule_id LIMIT 1
                ) matched_rule ON TRUE
                LEFT JOIN LATERAL (
                    SELECT MIN(request_period.leave_period_start) AS leave_period_start
                    FROM (
                        SELECT MIN(all_detail.from_date) AS leave_period_start
                        FROM public.emp_leave_detail current_detail
                        JOIN public.emp_leave employee_leave ON employee_leave.emp_leave_id = current_detail.emp_leave_id
                        JOIN public.emp_leave_detail all_detail
                          ON all_detail.emp_leave_id = employee_leave.emp_leave_id
                         AND all_detail.leave_type_id = current_detail.leave_type_id
                        WHERE employee_leave.emp_id = lb.emp_id
                          AND employee_leave.hr_verification_status_id = 6
                          AND current_detail.leave_type_id = lb.leave_type_id
                          AND lb.attendance_date BETWEEN current_detail.from_date AND current_detail.to_date
                        GROUP BY employee_leave.emp_leave_id
                    ) request_period
                ) leave_period ON TRUE
            )
            INSERT INTO public.payroll_attendance_day_snapshot
                (payroll_employee_id, attendance_date, leave_type_id,
                 payroll_policy_rule_id, holiday_id, day_value, overtime_hours,
                 normal_working_hours, scheduled_workday, leave_period_start, source_type)
            SELECT * FROM frozen_days
            UNION ALL
            SELECT * FROM live_days
            """, nativeQuery = true)
    int copyReconciledAttendanceToAdjustmentRun(
            @Param("runId") Long runId,
            @Param("periodId") Long periodId,
            @Nullable @Param("payrollEmployeeId") Long payrollEmployeeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            WITH balance AS (
                SELECT elb.emp_id,
                       MAX(elb.remaining_days_calc) FILTER (WHERE elb.leave_type_id = 1) AS al_balance,
                       MAX(elb.remaining_days_calc) FILTER (WHERE elb.leave_type_id = 9) AS sl_balance
                FROM public.emp_leave_balance elb
                JOIN public.payroll_period p ON p.payroll_period_id = :periodId
                WHERE elb.year = p.payroll_year
                GROUP BY elb.emp_id
            ),
            summary AS (
                SELECT d.payroll_employee_id,
                       COUNT(*)::numeric AS scheduled_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'P'), 0) AS worked_days,
                       COALESCE(SUM(d.day_value) FILTER (
                           WHERE d.attendance_category = 'H'
                             AND UPPER(TRIM(COALESCE(d.attendance_code, ''))) NOT IN ('OFF', 'OFF/PMM')
                       ), 0) AS holiday_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'A'), 0) AS absent_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'AL'), 0) AS annual_leave_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'SL'), 0) AS special_leave_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'S'), 0) AS sick_leave_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'ML'), 0) AS maternity_leave_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'OTHER'), 0) AS other_paid_leave_days,
                       COALESCE(SUM(d.day_value) FILTER (WHERE d.attendance_category = 'UL'), 0) AS unpaid_leave_days,
                       COALESCE(SUM(d.overtime_hours), 0) AS overtime_hours
                FROM public.payroll_attendance_day_snapshot_detail d
                JOIN public.payroll_employee pe
                  ON pe.payroll_employee_id = d.payroll_employee_id
                WHERE pe.payroll_run_id = :runId
                  AND pe.payroll_status = 'INCLUDED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                GROUP BY d.payroll_employee_id
            )
            INSERT INTO public.payroll_attendance_summary
            (payroll_employee_id, scheduled_days, worked_days, holiday_days,
             absent_days, annual_leave_days, special_leave_days, sick_leave_days,
             maternity_leave_days, other_paid_leave_days, unpaid_leave_days, overtime_hours,
             annual_leave_remaining, overused_annual_leave,
             special_leave_remaining, overused_special_leave, source_start_date, source_end_date)
            SELECT pe.payroll_employee_id,
                   COALESCE(s.scheduled_days, 0), COALESCE(s.worked_days, 0),
                   COALESCE(s.holiday_days, 0), COALESCE(s.absent_days, 0),
                   COALESCE(s.annual_leave_days, 0), COALESCE(s.special_leave_days, 0),
                   COALESCE(s.sick_leave_days, 0), COALESCE(s.maternity_leave_days, 0),
                   COALESCE(s.other_paid_leave_days, 0), COALESCE(s.unpaid_leave_days, 0),
                   COALESCE(s.overtime_hours, 0),
                   GREATEST(COALESCE(b.al_balance, 0), 0),
                   GREATEST(-COALESCE(b.al_balance, 0), 0),
                   GREATEST(COALESCE(b.sl_balance, 0), 0),
                   GREATEST(-COALESCE(b.sl_balance, 0), 0),
                   GREATEST(p.period_start, summary_employee.join_date),
                   LEAST(
                       p.period_end,
                       CASE
                           WHEN summary_career_group.career_type_group_name IS NOT NULL
                            AND UPPER(TRIM(summary_career_group.career_type_group_name)) = 'INACTIVE'
                            AND summary_employee.last_career_type_date IS NOT NULL
                           THEN summary_employee.last_career_type_date - 1
                           ELSE p.period_end
                       END
                   )
            FROM public.payroll_employee pe
            JOIN public.payroll_run r ON r.payroll_run_id = pe.payroll_run_id
            JOIN public.payroll_period p ON p.payroll_period_id = r.payroll_period_id
            JOIN public.emp_master summary_employee ON summary_employee.emp_id = pe.emp_id
            LEFT JOIN public.list_career_type summary_career_type
              ON summary_career_type.career_type_id = summary_employee.emp_status_career_type_id
            LEFT JOIN public.list_career_type_group summary_career_group
              ON summary_career_group.list_career_type_group_id = summary_career_type.list_career_type_group_id
            LEFT JOIN summary s ON s.payroll_employee_id = pe.payroll_employee_id
            LEFT JOIN balance b ON b.emp_id = pe.emp_id
            WHERE pe.payroll_run_id = :runId
              AND pe.payroll_status = 'INCLUDED'
              AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                   OR pe.payroll_employee_id = :payrollEmployeeId)
            ON CONFLICT (payroll_employee_id) DO UPDATE SET
                scheduled_days = EXCLUDED.scheduled_days,
                worked_days = EXCLUDED.worked_days,
                holiday_days = EXCLUDED.holiday_days,
                absent_days = EXCLUDED.absent_days,
                annual_leave_days = EXCLUDED.annual_leave_days,
                special_leave_days = EXCLUDED.special_leave_days,
                sick_leave_days = EXCLUDED.sick_leave_days,
                maternity_leave_days = EXCLUDED.maternity_leave_days,
                other_paid_leave_days = EXCLUDED.other_paid_leave_days,
                unpaid_leave_days = EXCLUDED.unpaid_leave_days,
                overtime_hours = EXCLUDED.overtime_hours,
                annual_leave_remaining = EXCLUDED.annual_leave_remaining,
                overused_annual_leave = EXCLUDED.overused_annual_leave,
                special_leave_remaining = EXCLUDED.special_leave_remaining,
                overused_special_leave = EXCLUDED.overused_special_leave,
                source_start_date = EXCLUDED.source_start_date,
                source_end_date = EXCLUDED.source_end_date,
                calculated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int rebuildAttendanceSummary(
            @Param("runId") Long runId,
            @Param("periodId") Long periodId,
            @Nullable @Param("payrollEmployeeId") Long payrollEmployeeId);
}
