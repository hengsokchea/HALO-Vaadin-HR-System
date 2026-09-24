package org.halocambodia.data;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class PayrollApprovedAttendanceDayRepositoryCustomImpl
        implements PayrollApprovedAttendanceDayRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int captureApprovedAttendance(Long payrollPeriodId, Long approvedBy) {
        return entityManager.createNativeQuery("""
                WITH selected_period AS (
                    SELECT payroll_period_id, payroll_year, period_start, period_end
                    FROM public.payroll_period
                    WHERE payroll_period_id = :periodId
                ),
                attendance_ranked AS (
                    SELECT ad.attendance_detail_id,
                           ad.attendance_emp_id AS emp_id,
                           ad.attendance_date,
                           ad.leave_type_id,
                           ad.leave_duration,
                           ad.number_of_day,
                           ad.overtime_hours,
                           COALESCE(attendance_rule.rule_code, CASE
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%DEMINER%PARAMEDIC%' THEN 'ADP'
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%ACTING%TEAM%COMMANDER%' THEN 'ATC'
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%SENIOR%TEAM%COMMANDER%' THEN 'STC'
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%ACTING%SUPERVISOR%' THEN 'ASO'
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%CHAINSAW%' THEN 'CHS'
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%CLUTERNATOR%' THEN 'CLT'
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%HSTAMIDS%' THEN 'HST'
                               WHEN UPPER(COALESCE(subtype.leave_sub_type_name_en, ''))
                                    LIKE '%STRIMMER%' THEN 'STR'
                               WHEN REPLACE(UPPER(COALESCE(subtype.leave_sub_type_name_en, '')), '-', ' ')
                                    LIKE '%SUB UNIT%COMMANDER%' THEN 'SUC'
                               ELSE NULL
                           END) AS operational_code,
                           ROW_NUMBER() OVER (
                               PARTITION BY ad.attendance_emp_id, ad.attendance_date
                               ORDER BY ad.hr_verification_date DESC NULLS LAST,
                                        ad.attendance_detail_id DESC
                           ) AS row_number,
                           SUM(COALESCE(ad.overtime_hours, 0)) OVER (
                               PARTITION BY ad.attendance_emp_id, ad.attendance_date
                           ) AS overtime_hours_total
                    FROM selected_period period
                    JOIN public.emp_attendance_detail ad
                      ON ad.attendance_date BETWEEN period.period_start AND period.period_end
                    LEFT JOIN public.list_leave_type_sub_type subtype
                      ON subtype.list_leave_type_sub_type_id = ad.leave_sub_type_id
                    LEFT JOIN LATERAL (
                        SELECT UPPER(TRIM(rule.rule_code)) AS rule_code
                        FROM public.payroll_policy_rule rule
                        WHERE rule.rule_year = period.payroll_year
                          AND rule.active = TRUE
                          AND rule.calculation_source = 'ATTENDANCE'
                          AND rule.rule_type IN ('DEDUCTION', 'OVERTIME', 'ALLOWANCE')
                          AND (
                              UPPER(COALESCE(ad.remark, ''))
                                  ~ ('(^|[^A-Z0-9])' || UPPER(TRIM(rule.rule_code)) || '([^A-Z0-9]|$)')
                              OR UPPER(TRIM(COALESCE(subtype.remarks, '')))
                                  = UPPER(TRIM(rule.rule_code))
                              OR UPPER(TRIM(COALESCE(subtype.leave_sub_type_name_en, '')))
                                  = UPPER(TRIM(rule.rule_code))
                          )
                        ORDER BY LENGTH(TRIM(rule.rule_code)) DESC
                        LIMIT 1
                    ) attendance_rule ON TRUE
                ),
                attendance_day AS (
                    SELECT *
                    FROM attendance_ranked
                    WHERE row_number = 1
                ),
                daily_base AS (
                    SELECT roster.emp_id,
                           roster.roster_date,
                           period.payroll_year,
                           roster.total_working_hour,
                           holiday.holiday_group_id,
                           holiday.holiday_id,
                           roster.leave_type_id AS roster_leave_type_id,
                           CASE
                               WHEN attendance.number_of_day IS NOT NULL
                               THEN attendance.number_of_day::numeric
                               WHEN COALESCE(attendance.leave_duration::text,
                                             roster.leave_duration::text, '')
                                    IN ('MORNING_ONLY', 'AFTERNOON_ONLY')
                               THEN 0.5
                               ELSE 1
                           END::numeric AS day_value,
                           COALESCE(attendance.overtime_hours_total, 0) AS overtime_hours,
                           attendance.attendance_detail_id,
                           COALESCE(
                               attendance.operational_code,
                               roster_rule.rule_code,
                               CASE
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%DEMINER%PARAMEDIC%' THEN 'ADP'
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%ACTING%TEAM%COMMANDER%' THEN 'ATC'
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%SENIOR%TEAM%COMMANDER%' THEN 'STC'
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%ACTING%SUPERVISOR%' THEN 'ASO'
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%CHAINSAW%' THEN 'CHS'
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%CLUTERNATOR%' THEN 'CLT'
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%HSTAMIDS%' THEN 'HST'
                                   WHEN UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, ''))
                                        LIKE '%STRIMMER%' THEN 'STR'
                                   WHEN REPLACE(UPPER(COALESCE(roster_subtype.leave_sub_type_name_en, '')), '-', ' ')
                                        LIKE '%SUB UNIT%COMMANDER%' THEN 'SUC'
                                   ELSE NULL
                               END
                           ) AS operational_code,
                           COALESCE(
                               attendance.leave_type_id,
                               roster.leave_type_id,
                               holiday.leave_type_id,
                               CASE WHEN holiday.holiday_id IS NULL
                                    THEN present_type.leave_type_id END
                           ) AS leave_type_id,
                           CASE
                               WHEN attendance.attendance_detail_id IS NOT NULL THEN 'ATTENDANCE'
                               WHEN roster.leave_type_id IS NOT NULL THEN 'ROSTER LEAVE'
                               WHEN holiday.holiday_id IS NOT NULL THEN 'HOLIDAY'
                               ELSE 'ROSTER'
                           END AS source_type
                    FROM selected_period period
                    JOIN public.emp_roster roster
                      ON roster.roster_date BETWEEN period.period_start AND period.period_end
                    LEFT JOIN attendance_day attendance
                      ON attendance.emp_id = roster.emp_id
                     AND attendance.attendance_date = roster.roster_date
                    LEFT JOIN public.list_leave_type_sub_type roster_subtype
                      ON roster_subtype.list_leave_type_sub_type_id = roster.leave_type_sub_type_id
                    LEFT JOIN public.holiday holiday
                      ON holiday.holiday_id = roster.holiday_id
                    LEFT JOIN LATERAL (
                        SELECT UPPER(TRIM(rule.rule_code)) AS rule_code
                        FROM public.payroll_policy_rule rule
                        WHERE rule.rule_year = period.payroll_year
                          AND rule.active = TRUE
                          AND rule.calculation_source = 'ATTENDANCE'
                          AND rule.rule_type IN ('DEDUCTION', 'OVERTIME', 'ALLOWANCE')
                          AND (
                              UPPER(COALESCE(roster.remark, ''))
                                  ~ ('(^|[^A-Z0-9])' || UPPER(TRIM(rule.rule_code)) || '([^A-Z0-9]|$)')
                              OR UPPER(TRIM(COALESCE(roster_subtype.remarks, '')))
                                  = UPPER(TRIM(rule.rule_code))
                              OR UPPER(TRIM(COALESCE(roster_subtype.leave_sub_type_name_en, '')))
                                  = UPPER(TRIM(rule.rule_code))
                          )
                        ORDER BY LENGTH(TRIM(rule.rule_code)) DESC
                        LIMIT 1
                    ) roster_rule ON TRUE
                    LEFT JOIN LATERAL (
                        SELECT leave_type.leave_type_id
                        FROM public.list_leave_type leave_type
                        WHERE UPPER(TRIM(leave_type.leav_type_code)) = 'PR'
                          AND (leave_type.obsolete_date IS NULL
                               OR leave_type.obsolete_date >= roster.roster_date)
                        ORDER BY leave_type.leave_type_id
                        LIMIT 1
                    ) present_type ON TRUE
                ),
                normalized_days AS (
                    SELECT daily.*,
                           matched_rule.payroll_policy_rule_id
                    FROM daily_base daily
                    LEFT JOIN LATERAL (
                        SELECT rule.payroll_policy_rule_id
                        FROM public.payroll_policy_rule rule
                        WHERE rule.rule_year = daily.payroll_year
                          AND rule.active = TRUE
                          AND rule.calculation_source = 'ATTENDANCE'
                          AND rule.rule_type IN ('DEDUCTION', 'OVERTIME', 'ALLOWANCE')
                          AND UPPER(TRIM(rule.rule_code)) =
                              UPPER(TRIM(COALESCE(daily.operational_code, '')))
                        ORDER BY rule.sort_order, rule.payroll_policy_rule_id
                        LIMIT 1
                    ) matched_rule ON TRUE
                ),
                approved_days AS (
                    SELECT daily.*,
                           leave_period.leave_period_start
                    FROM normalized_days daily
                    LEFT JOIN LATERAL (
                        SELECT MIN(request_period.leave_period_start) AS leave_period_start
                        FROM (
                            SELECT MIN(all_detail.from_date) AS leave_period_start
                            FROM public.emp_leave_detail current_detail
                            JOIN public.emp_leave employee_leave
                              ON employee_leave.emp_leave_id = current_detail.emp_leave_id
                            JOIN public.emp_leave_detail all_detail
                              ON all_detail.emp_leave_id = employee_leave.emp_leave_id
                             AND all_detail.leave_type_id = current_detail.leave_type_id
                            WHERE employee_leave.emp_id = daily.emp_id
                              AND employee_leave.hr_verification_status_id = 6
                              AND current_detail.leave_type_id = daily.leave_type_id
                              AND daily.roster_date BETWEEN current_detail.from_date
                                                        AND current_detail.to_date
                            GROUP BY employee_leave.emp_leave_id
                        ) request_period
                    ) leave_period ON TRUE
                )
                INSERT INTO public.payroll_approved_attendance_day
                    (payroll_period_id, emp_id, attendance_date, leave_type_id,
                     payroll_policy_rule_id, holiday_id, day_value,
                     overtime_hours, normal_working_hours, scheduled_workday,
                     leave_period_start, source_type, approved_at, approved_by)
                SELECT :periodId,
                       daily.emp_id,
                       daily.roster_date,
                       daily.leave_type_id,
                       daily.payroll_policy_rule_id,
                       daily.holiday_id,
                       daily.day_value,
                       daily.overtime_hours,
                       daily.total_working_hour,
                       COALESCE(daily.holiday_group_id NOT IN (2, 3, 4, 6), TRUE),
                       daily.leave_period_start,
                       daily.source_type,
                       CURRENT_TIMESTAMP,
                       :approvedBy
                FROM approved_days daily
                """)
                .setParameter("periodId", payrollPeriodId)
                .setParameter("approvedBy", approvedBy)
                .executeUpdate();
    }
}
