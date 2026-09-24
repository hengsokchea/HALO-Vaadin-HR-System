package org.halocambodia.data;

import static org.halocambodia.data.PayrollModels.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class PayrollAttendanceDataRepository {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @PersistenceContext
    private EntityManager entityManager;

    public AttendanceControlRow findControl(Long periodId) {
        Query query = entityManager.createNativeQuery("""
                WITH selected_period AS (
                    SELECT p.payroll_period_id, p.payroll_year,
                           p.period_start, p.period_end,
                           COALESCE(
                               c.attendance_cutoff_date,
                               (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Phnom_Penh')::date
                           ) AS validation_cutoff_date
                    FROM public.payroll_period p
                    LEFT JOIN public.payroll_attendance_control c
                      ON c.payroll_period_id = p.payroll_period_id
                    WHERE p.payroll_period_id = :periodId
                ),
                roster_days AS (
                    SELECT DISTINCT r.emp_id, r.roster_date
                    FROM selected_period p
                    JOIN public.emp_roster r
                      ON r.roster_date BETWEEN p.period_start AND p.period_end
                ),
                roster_employees AS (
                    SELECT DISTINCT emp_id FROM roster_days
                ),
                roster_stats AS (
                    SELECT COUNT(DISTINCT r.emp_id) AS roster_employee_count,
                           COUNT(r.roster_id) AS roster_day_count
                    FROM selected_period p
                    LEFT JOIN public.emp_roster r
                      ON r.roster_date BETWEEN p.period_start AND p.period_end
                ),
                attendance_in_period AS (
                    SELECT ad.*, p.validation_cutoff_date
                    FROM selected_period p
                    JOIN public.emp_attendance_detail ad
                      ON ad.attendance_date BETWEEN p.period_start AND p.period_end
                    JOIN roster_days rd
                      ON rd.emp_id = ad.attendance_emp_id
                     AND rd.roster_date = ad.attendance_date
                ),
                attendance_stats AS (
                    SELECT COUNT(ad.attendance_detail_id) AS attendance_record_count,
                           COUNT(ad.attendance_detail_id) FILTER (
                               WHERE ad.attendance_date <= ad.validation_cutoff_date
                                 AND (
                                      ad.entry_status IS DISTINCT FROM 'SUBMITTED'
                                   OR ad.qc_status IS DISTINCT FROM 'VERIFIED'
                                   OR ad.qc_by_emp_id IS NULL
                                   OR ad.hr_verification_date IS NULL
                                   OR ad.hr_verification_by_emp_id IS NULL
                                 )
                           ) AS unverified_attendance_count,
                           COALESCE(SUM(ad.overtime_hours), 0) AS overtime_hours
                    FROM attendance_in_period ad
                ),
                attendance_days AS (
                    SELECT DISTINCT ad.attendance_emp_id AS emp_id, ad.attendance_date
                    FROM attendance_in_period ad
                ),
                fallback_stats AS (
                    SELECT COUNT(*) FILTER (WHERE ad.emp_id IS NULL) AS roster_fallback_day_count
                    FROM selected_period p
                    JOIN public.emp_roster r
                      ON r.roster_date BETWEEN p.period_start AND p.period_end
                    LEFT JOIN attendance_days ad
                      ON ad.emp_id = r.emp_id AND ad.attendance_date = r.roster_date
                ),
                source_updates AS (
                    SELECT MAX(last_modified_at) AS source_last_modified_at
                    FROM (
                        SELECT MAX(COALESCE(r.updated_at, r.created_at)) AS last_modified_at
                        FROM selected_period p
                        JOIN public.emp_roster r
                          ON r.roster_date BETWEEN p.period_start AND p.period_end
                        UNION ALL
                        SELECT MAX(COALESCE(ad.updated_at, ad.created_at))
                        FROM attendance_in_period ad
                        UNION ALL
                        SELECT MAX(GREATEST(
                                   COALESCE(el.updated_at, el.created_at),
                                   COALESCE(eld.updated_at, eld.created_at)))
                        FROM selected_period p
                        JOIN public.emp_leave_detail eld
                          ON eld.from_date <= p.period_end
                         AND eld.to_date >= p.period_start
                         AND eld.leave_type_id IN (3,10)
                        JOIN public.emp_leave el ON el.emp_leave_id = eld.emp_leave_id
                        JOIN roster_employees re ON re.emp_id = el.emp_id
                        UNION ALL
                        SELECT MAX(COALESCE(elb.updated_at, elb.created_at))
                        FROM selected_period p
                        JOIN public.emp_leave_balance elb
                          ON elb.year = p.payroll_year
                         AND elb.leave_type_id IN (1,9)
                        JOIN roster_employees re ON re.emp_id = elb.emp_id
                    ) source_rows
                )
                SELECT p.payroll_period_id,
                       COALESCE(c.status, 'DRAFT') AS attendance_status,
                       COALESCE(r.roster_employee_count, 0) AS roster_employee_count,
                       COALESCE(r.roster_day_count, 0) AS roster_day_count,
                       COALESCE(a.attendance_record_count, 0) AS attendance_record_count,
                       COALESCE(f.roster_fallback_day_count, 0) AS roster_fallback_day_count,
                       COALESCE(a.unverified_attendance_count, 0) AS unverified_attendance_count,
                       COALESCE(a.overtime_hours, 0) AS overtime_hours,
                       s.source_last_modified_at,
                       c.checked_at,
                       COALESCE(checker.name, checker.username) AS checked_by_name,
                       c.approved_at,
                       COALESCE(approver.name, approver.username) AS approved_by_name,
                       c.notes,
                       CASE
                           WHEN c.status = 'CHECKED'
                            AND (
                                (s.source_last_modified_at IS NOT NULL
                                 AND (c.source_last_modified_at IS NULL
                                      OR s.source_last_modified_at > c.source_last_modified_at))
                                OR c.roster_employee_count IS DISTINCT FROM r.roster_employee_count
                                OR c.roster_day_count IS DISTINCT FROM r.roster_day_count
                                OR c.attendance_record_count IS DISTINCT FROM a.attendance_record_count
                                OR c.roster_fallback_day_count IS DISTINCT FROM f.roster_fallback_day_count
                                OR c.unverified_attendance_count IS DISTINCT FROM a.unverified_attendance_count
                                OR c.overtime_hours IS DISTINCT FROM a.overtime_hours
                            )
                           THEN TRUE ELSE FALSE
                       END AS stale
                FROM selected_period p
                CROSS JOIN roster_stats r
                CROSS JOIN attendance_stats a
                CROSS JOIN fallback_stats f
                CROSS JOIN source_updates s
                LEFT JOIN public.payroll_attendance_control c
                  ON c.payroll_period_id = p.payroll_period_id
                LEFT JOIN core_system.application_user checker ON checker.id = c.checked_by
                LEFT JOIN core_system.application_user approver ON approver.id = c.approved_by
                
                """);
        query.setParameter("periodId", periodId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(this::toAttendanceControlRow)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found."));
    }

    public List<AttendanceReviewRow> findSummary(
            LocalDate startDate, LocalDate endDate, String search, boolean issuesOnly) {
        Set<String> trackedPolicyCodes = findRuleCodesForDateRange(
                startDate.getYear(), endDate.getYear());

        Query query = entityManager.createNativeQuery("""
                WITH selected_period AS (
                    SELECT CAST(:startDate AS date) AS period_start,
                           CAST(:endDate AS date) AS period_end,
                           EXTRACT(YEAR FROM CAST(:endDate AS date))::integer AS payroll_year
                ),
                payroll_rule_codes AS (
                    SELECT DISTINCT UPPER(TRIM(r.rule_code)) AS rule_code
                    FROM selected_period p
                    JOIN public.payroll_policy_rule r
                      ON r.rule_year BETWEEN
                             EXTRACT(YEAR FROM p.period_start)::integer
                         AND EXTRACT(YEAR FROM p.period_end)::integer
                     AND r.active = TRUE
                     AND r.calculation_source = 'ATTENDANCE'
                     AND r.rule_type IN ('DEDUCTION','OVERTIME','ALLOWANCE')
                ),
                operational_subtypes AS (
                    SELECT st.list_leave_type_sub_type_id,
                           COALESCE(rule_match.rule_code, CASE
                               WHEN UPPER(TRIM(COALESCE(st.remarks, ''))) IN
                                    ('ADP','ATC','STC','ASO','CHS','CLT','HST','STR','SUC')
                               THEN UPPER(TRIM(st.remarks))
                               WHEN UPPER(TRIM(COALESCE(st.leave_sub_type_name_en, ''))) IN
                                    ('ADP','ATC','STC','ASO','CHS','CLT','HST','STR','SUC')
                               THEN UPPER(TRIM(st.leave_sub_type_name_en))
                               WHEN UPPER(CONCAT_WS(' ', st.leave_sub_type_name_en, st.remarks))
                                    ~ '(^|[^A-Z0-9])SUC([^A-Z0-9]|$)' THEN 'SUC'
                               WHEN REGEXP_REPLACE(
                                        UPPER(CONCAT_WS(' ', st.leave_sub_type_name_en, st.remarks)),
                                        '[^A-Z0-9]+', '', 'g')
                                    LIKE '%SUBUNITCOMMANDER%' THEN 'SUC'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%DEMINER%PARAMEDIC%' THEN 'ADP'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%ACTING%TEAM%COMMANDER%' THEN 'ATC'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%SENIOR%TEAM%COMMANDER%' THEN 'STC'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%ACTING%SUPERVISOR%' THEN 'ASO'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%CHAINSAW%' THEN 'CHS'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%CLUTERNATOR%' THEN 'CLT'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%HSTAMIDS%' THEN 'HST'
                               WHEN UPPER(COALESCE(st.leave_sub_type_name_en, ''))
                                    LIKE '%STRIMMER%' THEN 'STR'
                               WHEN REPLACE(UPPER(COALESCE(st.leave_sub_type_name_en, '')), '-', ' ')
                                    LIKE '%SUB UNIT%COMMANDER%' THEN 'SUC'
                               ELSE NULL
                           END) AS operational_code
                    FROM public.list_leave_type_sub_type st
                    LEFT JOIN LATERAL (
                        SELECT pr.rule_code
                        FROM payroll_rule_codes pr
                        WHERE UPPER(TRIM(COALESCE(st.remarks, ''))) = pr.rule_code
                           OR UPPER(TRIM(COALESCE(st.leave_sub_type_name_en, ''))) = pr.rule_code
                        ORDER BY LENGTH(pr.rule_code) DESC
                        LIMIT 1
                    ) rule_match ON TRUE
                ),
                attendance_ranked AS (
                    SELECT ad.attendance_detail_id, ad.attendance_emp_id, ad.attendance_date,
                           ad.leave_type_id, ad.leave_duration, ad.number_of_day,
                           ad.entry_status, ad.qc_status, ad.qc_by_emp_id,
                           ad.hr_verification_date, ad.hr_verification_by_emp_id, ad.remark,
                           lt.leav_type_code, lt.paid AS leave_paid,
                           ad_sub.operational_code AS attendance_operational_code,
                           COALESCE(remark_rule.rule_code, CASE
                               WHEN UPPER(COALESCE(ad.remark, ''))
                                    ~ '(^|[^A-Z0-9])SUC([^A-Z0-9]|$)' THEN 'SUC'
                               WHEN REGEXP_REPLACE(UPPER(COALESCE(ad.remark, '')),
                                        '[^A-Z0-9]+', '', 'g')
                                    LIKE '%SUBUNITCOMMANDER%' THEN 'SUC'
                               ELSE NULL
                           END) AS attendance_remark_operational_code,
                           ROW_NUMBER() OVER (
                               PARTITION BY ad.attendance_emp_id, ad.attendance_date
                               ORDER BY ad.hr_verification_date DESC NULLS LAST,
                                        ad.attendance_detail_id DESC
                           ) AS row_number,
                           COUNT(*) OVER (
                               PARTITION BY ad.attendance_emp_id, ad.attendance_date
                           ) AS duplicate_count,
                           SUM(COALESCE(ad.overtime_hours, 0)) OVER (
                               PARTITION BY ad.attendance_emp_id, ad.attendance_date
                           ) AS overtime_hours_total
                    FROM selected_period p
                    JOIN public.emp_attendance_detail ad
                      ON ad.attendance_date BETWEEN p.period_start AND p.period_end
                    LEFT JOIN public.list_leave_type lt ON lt.leave_type_id = ad.leave_type_id
                    LEFT JOIN operational_subtypes ad_sub
                      ON ad_sub.list_leave_type_sub_type_id = ad.leave_sub_type_id
                    LEFT JOIN LATERAL (
                        SELECT pr.rule_code
                        FROM payroll_rule_codes pr
                        WHERE UPPER(COALESCE(ad.remark, ''))
                              ~ ('(^|[^A-Z0-9])' || pr.rule_code || '([^A-Z0-9]|$)')
                        ORDER BY LENGTH(pr.rule_code) DESC
                        LIMIT 1
                    ) remark_rule ON TRUE
                ),
                attendance_day AS (
                    SELECT * FROM attendance_ranked WHERE row_number = 1
                ),
                leave_balance AS (
                    SELECT elb.emp_id,
                           MAX(elb.remaining_days_calc) FILTER (
                               WHERE elb.leave_type_id = 1
                           ) AS annual_leave_balance,
                           MAX(elb.remaining_days_calc) FILTER (
                               WHERE elb.leave_type_id = 9
                           ) AS special_leave_balance
                    FROM selected_period p
                    JOIN public.emp_leave_balance elb
                      ON elb.year = EXTRACT(YEAR FROM p.period_end)::integer
                    GROUP BY elb.emp_id
                )
                SELECT r.emp_id, em.insurance_no, em.name_en, em.name_kh,
                       pos.emp_position, ec.emp_category, t.team_name,
                       tt.list_team_type_name, loc.location_short_name,
                       c.contract_code, d.donor_short_name,
                       GREATEST(COALESCE(lb.annual_leave_balance, 0), 0)
                           AS annual_leave_remaining,
                       GREATEST(-COALESCE(lb.annual_leave_balance, 0), 0)
                           AS overused_annual_leave,
                       GREATEST(COALESCE(lb.special_leave_balance, 0), 0)
                           AS special_leave_remaining,
                       GREATEST(-COALESCE(lb.special_leave_balance, 0), 0)
                           AS overused_special_leave,
                       r.roster_date,
                       effective.attendance_code,
                       CASE
                           WHEN UPPER(effective.attendance_code) IN ('PR','P','PRESENT','√')
                           THEN 'P'
                           WHEN effective.leave_type_id = 1
                                OR UPPER(effective.attendance_code) = 'AL' THEN 'AL'
                           WHEN effective.leave_type_id = 9
                                OR UPPER(effective.attendance_code) = 'SL' THEN 'SL'
                           WHEN effective.leave_type_id = 10
                                OR UPPER(effective.attendance_code) = 'S' THEN 'S'
                           WHEN effective.leave_type_id = 3
                                OR UPPER(effective.attendance_code) = 'ML' THEN 'ML'
                           WHEN UPPER(effective.attendance_code) IN ('A','ABSENT') THEN 'A'
                           WHEN effective.leave_type_id IS NOT NULL
                                AND effective.leave_paid = FALSE THEN 'UL'
                           WHEN ad.attendance_detail_id IS NULL
                                AND r.leave_type_id IS NULL
                                AND h.holiday_id IS NOT NULL THEN 'H'
                           WHEN effective.leave_type_id IS NULL THEN 'P'
                           ELSE 'OTHER'
                       END AS attendance_category,
                       COALESCE(ad.leave_duration::text, r.leave_duration::text, 'FULL_DAY')
                           AS leave_duration,
                       COALESCE(
                           ad.number_of_day::numeric,
                           CASE
                               WHEN COALESCE(ad.leave_duration::text, r.leave_duration::text, '')
                                    IN ('MORNING_ONLY','AFTERNOON_ONLY')
                               THEN 0.5 ELSE 1
                           END
                       )::numeric AS day_value,
                       COALESCE(ad.overtime_hours_total, 0) AS overtime_hours,
                       CASE
                           WHEN ad.attendance_detail_id IS NOT NULL THEN 'ATTENDANCE'
                           WHEN r.leave_type_id IS NOT NULL THEN 'ROSTER LEAVE'
                           WHEN h.holiday_id IS NOT NULL THEN 'HOLIDAY'
                           ELSE 'ROSTER'
                       END AS attendance_source,
                       (ad.attendance_detail_id IS NULL OR (
                            ad.entry_status = 'SUBMITTED'
                            AND ad.qc_status = 'VERIFIED'
                            AND ad.qc_by_emp_id IS NOT NULL
                            AND ad.hr_verification_date IS NOT NULL
                            AND ad.hr_verification_by_emp_id IS NOT NULL
                       ))
                           AS hr_verified,
                       COALESCE(ad.duplicate_count, 1) AS duplicate_count,
                       COALESCE(NULLIF(ad.remark, ''), NULLIF(r.remark, ''), '') AS remarks
                FROM selected_period p
                JOIN public.emp_roster r
                  ON r.roster_date BETWEEN p.period_start AND p.period_end
                JOIN public.cam_view_emp_master em ON em.emp_id = r.emp_id
                LEFT JOIN public.emp_personnel_allocation pa
                  ON pa.emp_personnel_allocation_id = r.emp_personnel_allocation_id
                LEFT JOIN public.list_position pos ON pos.position_id = pa.position_id
                LEFT JOIN public.list_emp_category ec ON ec.category_id = pa.emp_category_id
                LEFT JOIN public.list_team t ON t.team_id = pa.team_id
                LEFT JOIN public.list_team_type tt ON tt.list_team_type_id = t.team_type_id
                LEFT JOIN public.list_location loc ON loc.location_id = pa.branch_id
                LEFT JOIN public.contracts c ON c.contracts_id = pa.contract_id
                LEFT JOIN public.donors d ON d.donors_id = c.donors_id
                LEFT JOIN attendance_day ad
                  ON ad.attendance_emp_id = r.emp_id AND ad.attendance_date = r.roster_date
                LEFT JOIN public.list_leave_type roster_lt
                  ON roster_lt.leave_type_id = r.leave_type_id
                LEFT JOIN operational_subtypes roster_sub
                  ON roster_sub.list_leave_type_sub_type_id = r.leave_type_sub_type_id
                LEFT JOIN LATERAL (
                    SELECT pr.rule_code
                    FROM payroll_rule_codes pr
                    WHERE UPPER(COALESCE(r.remark, ''))
                          ~ ('(^|[^A-Z0-9])' || pr.rule_code || '([^A-Z0-9]|$)')
                    ORDER BY LENGTH(pr.rule_code) DESC
                    LIMIT 1
                ) roster_remark_rule ON TRUE
                LEFT JOIN public.holiday h ON h.holiday_id = r.holiday_id
                LEFT JOIN public.list_leave_type holiday_lt
                  ON holiday_lt.leave_type_id = h.leave_type_id
                LEFT JOIN leave_balance lb ON lb.emp_id = r.emp_id
                CROSS JOIN LATERAL (
                    SELECT COALESCE(
                               ad.attendance_operational_code,
                               ad.attendance_remark_operational_code,
                               roster_remark_rule.rule_code,
                               roster_sub.operational_code,
                               ad.leav_type_code,
                               roster_lt.leav_type_code,
                               holiday_lt.leav_type_code,
                               h.holiday_code,
                               '√'
                           ) AS attendance_code,
                           COALESCE(ad.leave_type_id, r.leave_type_id, h.leave_type_id)
                               AS leave_type_id,
                           COALESCE(ad.leave_paid, roster_lt.paid,
                                    holiday_lt.paid, TRUE) AS leave_paid
                ) effective
                ORDER BY em.insurance_no, r.roster_date
                
                """);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<Long, AttendanceReviewAccumulator> grouped = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Long empId = longValue(row[0]);
            AttendanceReviewAccumulator accumulator = grouped.computeIfAbsent(
                    empId, ignored -> new AttendanceReviewAccumulator(row, trackedPolicyCodes));
            accumulator.addDay(row);
        }

        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return grouped.values().stream()
                .map(AttendanceReviewAccumulator::toRow)
                .filter(row -> term.isEmpty() || attendanceReviewMatches(row, term))
                .filter(row -> !issuesOnly || row.hasIssues())
                .toList();
    }

    public boolean hasDuplicateAttendance(Long periodId) {
        Query query = entityManager.createNativeQuery("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.emp_attendance_detail ad
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = :periodId
                    JOIN public.emp_roster r
                      ON r.emp_id = ad.attendance_emp_id
                     AND r.roster_date = ad.attendance_date
                    WHERE ad.attendance_date BETWEEN p.period_start AND p.period_end
                    GROUP BY ad.attendance_emp_id, ad.attendance_date
                    HAVING COUNT(*) > 1
                )
                
                """);
        query.setParameter("periodId", periodId);
        Object value = query.getSingleResult();
        if (value instanceof Boolean bool) return bool;
        return value != null && Boolean.parseBoolean(value.toString());
    }

    public Map<String, BigDecimal> findPolicyQuantities(Long payrollEmployeeId) {
        Query query = entityManager.createNativeQuery("""
                WITH active_rules AS (
                    SELECT UPPER(TRIM(r.rule_code)) AS rule_code,
                           r.rule_type, UPPER(TRIM(COALESCE(r.rate_unit, ''))) AS rate_unit
                    FROM public.payroll_employee pe
                    JOIN public.payroll_run run
                      ON run.payroll_run_id = pe.payroll_run_id
                    JOIN public.payroll_period period
                      ON period.payroll_period_id = run.payroll_period_id
                    JOIN public.payroll_policy_rule r
                      ON r.rule_year = period.payroll_year
                     AND r.active = TRUE
                     AND r.calculation_source = 'ATTENDANCE'
                     AND r.rule_type IN ('DEDUCTION','OVERTIME','ALLOWANCE')
                    WHERE pe.payroll_employee_id = :payrollEmployeeId
                )
                SELECT r.rule_code AS policy_code,
                       COALESCE(SUM(
                           CASE
                               WHEN r.rule_type = 'OVERTIME' AND r.rate_unit = 'HOUR'
                                   THEN d.overtime_hours
                               ELSE d.day_value
                           END
                       ), 0) AS quantity
                FROM active_rules r
                JOIN public.payroll_attendance_day_snapshot_detail d
                  ON d.payroll_employee_id = :payrollEmployeeId
                 AND (
                     (r.rule_type = 'OVERTIME' AND r.rate_unit = 'HOUR'
                          AND COALESCE(d.overtime_hours, 0) > 0)
                     OR UPPER(TRIM(COALESCE(d.attendance_code, ''))) = r.rule_code
                     OR UPPER(TRIM(COALESCE(d.attendance_category, ''))) = r.rule_code
                 )
                GROUP BY r.rule_code
                ORDER BY r.rule_code
                
                """);
        query.setParameter("payrollEmployeeId", payrollEmployeeId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(stringValue(row[0]), decimal(row[1]));
        }
        return Collections.unmodifiableMap(result);
    }

    public Set<String> findRuleCodesForPeriod(Long periodId) {
        Query query = entityManager.createNativeQuery("""
                SELECT DISTINCT UPPER(TRIM(r.rule_code))
                FROM public.payroll_period p
                JOIN public.payroll_policy_rule r
                  ON r.rule_year = p.payroll_year
                 AND r.active = TRUE
                 AND r.calculation_source = 'ATTENDANCE'
                 AND r.rule_type IN ('DEDUCTION','OVERTIME','ALLOWANCE')
                WHERE p.payroll_period_id = :periodId
                ORDER BY UPPER(TRIM(r.rule_code))
                
                """);
        query.setParameter("periodId", periodId);

        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        Set<String> result = new LinkedHashSet<>();
        for (Object row : rows) {
            if (row != null) result.add(row.toString());
        }
        return Collections.unmodifiableSet(result);
    }

    public Set<String> findRuleCodesForDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return Set.of();
        }
        return findRuleCodesForDateRange(startDate.getYear(), endDate.getYear());
    }

    private Set<String> findRuleCodesForDateRange(int startYear, int endYear) {
        Query query = entityManager.createNativeQuery("""
                SELECT DISTINCT UPPER(TRIM(r.rule_code))
                FROM public.payroll_policy_rule r
                WHERE r.rule_year BETWEEN :startYear AND :endYear
                  AND r.active = TRUE
                  AND r.calculation_source = 'ATTENDANCE'
                  AND r.rule_type IN ('DEDUCTION','OVERTIME','ALLOWANCE')
                ORDER BY UPPER(TRIM(r.rule_code))
                
                """);
        query.setParameter("startYear", startYear);
        query.setParameter("endYear", endYear);

        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();
        Set<String> result = new LinkedHashSet<>();
        for (Object row : rows) {
            if (row != null) result.add(row.toString());
        }
        return Collections.unmodifiableSet(result);
    }

    private AttendanceControlRow toAttendanceControlRow(Object[] row) {
        return new AttendanceControlRow(
                longValue(row[0]),
                stringValue(row[1]),
                longValueOrZero(row[2]),
                longValueOrZero(row[3]),
                longValueOrZero(row[4]),
                longValueOrZero(row[5]),
                longValueOrZero(row[6]),
                decimal(row[7]),
                offsetDateTime(row[8]),
                offsetDateTime(row[9]),
                stringValue(row[10]),
                offsetDateTime(row[11]),
                stringValue(row[12]),
                stringValue(row[13]),
                booleanValue(row[14]));
    }

    private static boolean attendanceReviewMatches(AttendanceReviewRow row, String term) {
        return contains(row.insuranceNo() == null ? null : row.insuranceNo().toString(), term)
                || contains(row.nameEn(), term)
                || contains(row.nameKh(), term)
                || contains(row.position(), term)
                || contains(row.employeeCategory(), term)
                || contains(row.team(), term)
                || contains(row.teamType(), term)
                || contains(row.location(), term)
                || contains(row.contract(), term)
                || contains(row.donor(), term);
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private static final class AttendanceReviewAccumulator {
        private final Long empId;
        private final Integer insuranceNo;
        private final String nameEn;
        private final String nameKh;
        private final String position;
        private final String employeeCategory;
        private final String team;
        private final String teamType;
        private final String location;
        private final String contract;
        private final String donor;
        private final BigDecimal annualLeaveRemaining;
        private final BigDecimal overusedAnnualLeave;
        private final BigDecimal specialLeaveRemaining;
        private final BigDecimal overusedSpecialLeave;
        private final Set<String> trackedPolicyCodes;
        private final Map<LocalDate, AttendanceDayCell> days = new LinkedHashMap<>();
        private final Set<String> remarks = new LinkedHashSet<>();

        private BigDecimal presentDays = ZERO;
        private BigDecimal absentDays = ZERO;
        private BigDecimal annualLeaveDays = ZERO;
        private BigDecimal unpaidLeaveDays = ZERO;
        private BigDecimal specialLeaveDays = ZERO;
        private BigDecimal sickLeaveDays = ZERO;
        private BigDecimal maternityLeaveDays = ZERO;
        private BigDecimal holidayDays = ZERO;
        private BigDecimal otherDays = ZERO;
        private final Map<String, BigDecimal> policyCodeDays = new LinkedHashMap<>();
        private BigDecimal overtimeHours = ZERO;
        private long unverifiedAttendanceCount;

        private AttendanceReviewAccumulator(Object[] row, Set<String> trackedPolicyCodes) {
            empId = longValue(row[0]);
            insuranceNo = integerValue(row[1]);
            nameEn = stringValue(row[2]);
            nameKh = stringValue(row[3]);
            position = stringValue(row[4]);
            employeeCategory = stringValue(row[5]);
            team = stringValue(row[6]);
            teamType = stringValue(row[7]);
            location = stringValue(row[8]);
            contract = stringValue(row[9]);
            donor = stringValue(row[10]);
            annualLeaveRemaining = decimal(row[11]);
            overusedAnnualLeave = decimal(row[12]);
            specialLeaveRemaining = decimal(row[13]);
            overusedSpecialLeave = decimal(row[14]);
            this.trackedPolicyCodes = trackedPolicyCodes == null
                    ? Set.of()
                    : Set.copyOf(trackedPolicyCodes);
        }

        private void addDay(Object[] row) {
            LocalDate date = localDate(row[15]);
            String attendanceCode = stringValue(row[16]);
            String category = stringValue(row[17]);
            String leaveDuration = stringValue(row[18]);
            BigDecimal dayValue = decimal(row[19]);
            BigDecimal dailyOvertime = decimal(row[20]);
            String source = stringValue(row[21]);
            boolean hrVerified = booleanValue(row[22]);
            int duplicateCount = intValue(row[23]);
            String dailyRemarks = stringValue(row[24]);
            String normalizedCode = attendanceCode == null
                    ? ""
                    : attendanceCode.trim().toUpperCase(Locale.ROOT);
            boolean trackedPolicyCode = trackedPolicyCodes.contains(normalizedCode);

            AttendanceDayCell day = new AttendanceDayCell(
                    date, attendanceCode, category, leaveDuration,
                    dayValue, dailyOvertime, source,
                    hrVerified, duplicateCount, dailyRemarks);
            days.put(date, day);

            if (trackedPolicyCode) {
                policyCodeDays.merge(normalizedCode, dayValue, BigDecimal::add);
            }

            switch (category == null ? "" : category) {
                case "P" -> presentDays = presentDays.add(dayValue);
                case "A" -> absentDays = absentDays.add(dayValue);
                case "AL" -> annualLeaveDays = annualLeaveDays.add(dayValue);
                case "UL" -> unpaidLeaveDays = unpaidLeaveDays.add(dayValue);
                case "SL" -> specialLeaveDays = specialLeaveDays.add(dayValue);
                case "S" -> sickLeaveDays = sickLeaveDays.add(dayValue);
                case "ML" -> maternityLeaveDays = maternityLeaveDays.add(dayValue);
                case "H" -> {
                    if (!Set.of("OFF", "OFF/PMM").contains(normalizedCode)) {
                        holidayDays = holidayDays.add(dayValue);
                    }
                }
                default -> {
                    if (!trackedPolicyCode) {
                        otherDays = otherDays.add(dayValue);
                    }
                }
            }

            overtimeHours = overtimeHours.add(dailyOvertime);
            if (!hrVerified) unverifiedAttendanceCount++;
            if (dailyRemarks != null && !dailyRemarks.isBlank()) {
                remarks.add("%s: %s".formatted(date, dailyRemarks.trim()));
            }
        }

        private AttendanceReviewRow toRow() {
            return new AttendanceReviewRow(
                    empId, insuranceNo, nameEn, nameKh, position, employeeCategory,
                    team, teamType, location, contract, donor,
                    annualLeaveRemaining, overusedAnnualLeave,
                    specialLeaveRemaining, overusedSpecialLeave,
                    Collections.unmodifiableMap(new LinkedHashMap<>(days)),
                    presentDays, absentDays, annualLeaveDays, unpaidLeaveDays,
                    specialLeaveDays, sickLeaveDays, maternityLeaveDays,
                    holidayDays, otherDays,
                    Collections.unmodifiableMap(new LinkedHashMap<>(policyCodeDays)),
                    overtimeHours, unverifiedAttendanceCount,
                    String.join("; ", remarks));
        }
    }

    private static Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static long longValueOrZero(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static Integer integerValue(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static int intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) return bool;
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private static LocalDate localDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private static OffsetDateTime offsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime dateTime) return dateTime;
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(value.toString());
    }
}
