package org.halocambodia.data;

import static org.halocambodia.data.PayrollModels.EmployeePaymentRow;
import static org.halocambodia.data.PayrollModels.NssfPaymentRow;
import static org.halocambodia.data.PayrollModels.PaymentBatchRow;
import static org.halocambodia.data.PayrollModels.PaymentScheduleSummary;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * JPA/native-query data-access layer for payroll payment reads and bulk payment
 * generation. Workflow/status decisions remain in {@code PayrollPaymentService}.
 */
@Repository
public class PayrollPaymentDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public PaymentScheduleSummary detectPaymentSchedule(Long periodId) {
        List<?> rows = entityManager.createNativeQuery("""
                WITH period_context AS (
                    SELECT period.payroll_period_id,
                           period.period_start,
                           period.period_end,
                           COALESCE(period.payment_date, period.period_end) AS reference_date
                    FROM public.payroll_period period
                    WHERE period.payroll_period_id = :periodId
                ), company_setting AS (
                    SELECT setting.payment_frequency,
                           setting.first_payment_percent,
                           setting.shift_id
                    FROM public.payroll_payment_setting setting
                    CROSS JOIN period_context context
                    WHERE setting.emp_id IS NULL
                      AND setting.active = TRUE
                      AND setting.effective_from <= context.reference_date
                      AND (setting.effective_to IS NULL
                           OR setting.effective_to >= context.reference_date)
                    ORDER BY setting.effective_from DESC,
                             setting.payroll_payment_setting_id DESC
                    LIMIT 1
                ), rostered_employee AS (
                    SELECT DISTINCT ON (roster.emp_id)
                           roster.emp_id,
                           cycle.shift_id
                    FROM public.emp_roster roster
                    JOIN public.shift_cycle cycle
                      ON cycle.shift_cycle_id = roster.shift_cycle_id
                    CROSS JOIN period_context context
                    WHERE roster.roster_date
                          BETWEEN context.period_start AND context.period_end
                    ORDER BY roster.emp_id,
                             roster.roster_date DESC,
                             roster.roster_id DESC
                ), resolved_schedule AS (
                    SELECT rostered.emp_id,
                           CASE
                               WHEN company.payment_frequency = 'SEMI_MONTHLY'
                                   THEN 'SEMI_MONTHLY'
                               WHEN company.payment_frequency = 'MONTHLY'
                                    AND company.shift_id IS NOT NULL
                                    AND company.shift_id = rostered.shift_id
                                   THEN 'SEMI_MONTHLY'
                               WHEN company.payment_frequency = 'MONTHLY'
                                   THEN 'MONTHLY'
                               ELSE NULL
                           END AS payment_frequency,
                           company.payment_frequency = 'MONTHLY'
                               AND company.shift_id IS NOT NULL
                               AND company.shift_id = rostered.shift_id AS shift_override
                    FROM rostered_employee rostered
                    LEFT JOIN company_setting company ON TRUE
                )
                SELECT context.reference_date,
                       company.payment_frequency IS NOT NULL AS company_setting_found,
                       company.payment_frequency AS company_frequency,
                       COALESCE(company.first_payment_percent, 0) AS company_first_percent,
                       COUNT(resolved.emp_id) AS rostered_employee_count,
                       COUNT(resolved.emp_id) FILTER
                           (WHERE resolved.payment_frequency = 'MONTHLY') AS monthly_employee_count,
                       COUNT(resolved.emp_id) FILTER
                           (WHERE resolved.payment_frequency = 'SEMI_MONTHLY') AS semi_monthly_employee_count,
                       COUNT(resolved.emp_id) FILTER
                           (WHERE resolved.shift_override) AS shift_override_count,
                       COUNT(resolved.emp_id) FILTER
                           (WHERE resolved.payment_frequency IS NULL) AS unconfigured_employee_count
                FROM period_context context
                LEFT JOIN company_setting company ON TRUE
                LEFT JOIN resolved_schedule resolved ON TRUE
                GROUP BY context.reference_date,
                         company.payment_frequency,
                         company.first_payment_percent
                """)
                .setParameter("periodId", periodId)
                .getResultList();

        if (rows.isEmpty()) {
            return null;
        }
        Object[] row = (Object[]) rows.getFirst();
        return new PaymentScheduleSummary(
                localDate(row[0]),
                bool(row[1]),
                string(row[2]),
                decimal(row[3]),
                longValue(row[4]),
                longValue(row[5]),
                longValue(row[6]),
                longValue(row[7]),
                longValue(row[8]));
    }

    public List<PaymentBatchRow> findBatches(Long periodId) {
        List<?> rows = entityManager.createNativeQuery("""
                SELECT batch.payroll_payment_batch_id,
                       batch.payroll_period_id,
                       batch.payroll_run_id,
                       batch.installment_type,
                       batch.payment_date,
                       batch.status,
                       COUNT(detail.payroll_employee_payment_id) AS employee_count,
                       COALESCE(SUM(detail.payment_amount), 0) AS payment_amount,
                       COALESCE(SUM(detail.carry_forward_amount), 0) AS carry_forward_amount,
                       COALESCE(MIN(detail.currency_code), period.payroll_currency) AS currency_code,
                       batch.notes,
                       batch.approved_at,
                       batch.paid_at,
                       batch.payment_method,
                       batch.payment_reference,
                       batch.bank_reference,
                       batch.payment_file_name,
                       batch.payment_exported_at,
                       batch.payment_confirmed_at
                FROM public.payroll_payment_batch batch
                JOIN public.payroll_period period
                  ON period.payroll_period_id = batch.payroll_period_id
                LEFT JOIN public.payroll_employee_payment detail
                  ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                WHERE batch.payroll_period_id = :periodId
                GROUP BY batch.payroll_payment_batch_id, period.payroll_currency
                ORDER BY batch.payment_date, batch.payroll_payment_batch_id
                """)
                .setParameter("periodId", periodId)
                .getResultList();

        return rows.stream()
                .map(value -> (Object[]) value)
                .map(row -> new PaymentBatchRow(
                        longObject(row[0]),
                        longObject(row[1]),
                        longObject(row[2]),
                        string(row[3]),
                        localDate(row[4]),
                        string(row[5]),
                        longValue(row[6]),
                        decimal(row[7]),
                        decimal(row[8]),
                        string(row[9]),
                        string(row[10]),
                        offsetDateTime(row[11]),
                        offsetDateTime(row[12]),
                        string(row[13]),
                        string(row[14]),
                        string(row[15]),
                        string(row[16]),
                        offsetDateTime(row[17]),
                        offsetDateTime(row[18])))
                .toList();
    }

    public List<EmployeePaymentRow> findEmployeePayments(Long batchId, String search) {
        String term = search == null ? "" : search.trim();
        List<?> rows = entityManager.createNativeQuery("""
                SELECT detail.payroll_employee_payment_id,
                       detail.payroll_payment_batch_id,
                       detail.emp_id,
                       detail.payroll_employee_id,
                       detail.insurance_no_snapshot,
                       detail.employee_name_en_snapshot,
                       detail.employee_name_kh_snapshot,
                       detail.bank_name_snapshot,
                       detail.bank_account_snapshot,
                       detail.payment_frequency,
                       detail.first_payment_percent,
                       detail.basic_salary,
                       detail.full_net_amount,
                       detail.previous_paid_amount,
                       detail.payment_amount,
                       detail.carry_forward_amount,
                       detail.currency_code,
                       detail.remarks
                FROM public.payroll_employee_payment detail
                WHERE detail.payroll_payment_batch_id = :batchId
                  AND (:term = ''
                       OR CAST(detail.insurance_no_snapshot AS TEXT) ILIKE '%' || :term || '%'
                       OR COALESCE(detail.employee_name_en_snapshot, '') ILIKE '%' || :term || '%'
                       OR COALESCE(detail.employee_name_kh_snapshot, '') ILIKE '%' || :term || '%'
                       OR COALESCE(detail.bank_account_snapshot, '') ILIKE '%' || :term || '%')
                ORDER BY detail.insurance_no_snapshot, detail.payroll_employee_payment_id
                """)
                .setParameter("batchId", batchId)
                .setParameter("term", term)
                .getResultList();

        return rows.stream()
                .map(value -> (Object[]) value)
                .map(row -> new EmployeePaymentRow(
                        longObject(row[0]),
                        longObject(row[1]),
                        longObject(row[2]),
                        longObject(row[3]),
                        integer(row[4]),
                        string(row[5]),
                        string(row[6]),
                        string(row[7]),
                        string(row[8]),
                        string(row[9]),
                        decimal(row[10]),
                        decimal(row[11]),
                        decimal(row[12]),
                        decimal(row[13]),
                        decimal(row[14]),
                        decimal(row[15]),
                        string(row[16]),
                        string(row[17])))
                .toList();
    }

    /**
     * Returns the NSSF contribution amounts for one calculated payroll run.
     *
     * NSSF items are stored with their statutory source amount in KHR in
     * payroll_employee_item.amount. The payroll_amount column is the converted
     * payroll-currency value used by net-pay calculation, so it is intentionally
     * not used for this remittance export.
     */
    public List<NssfPaymentRow> findNssfPaymentRows(Long runId) {
        List<?> rows = entityManager.createNativeQuery("""
                WITH run_context AS (
                    SELECT run.payroll_run_id,
                           period.nssf_usd_to_khr_rate
                    FROM public.payroll_run run
                    JOIN public.payroll_period period
                      ON period.payroll_period_id = run.payroll_period_id
                    WHERE run.payroll_run_id = :runId
                ), nssf_earnings AS (
                    SELECT employee.payroll_employee_id,
                           COALESCE(SUM(item.payroll_amount) FILTER (
                               WHERE component.component_type = 'EARNING'
                                 AND component.subject_to_nssf = TRUE), 0)
                               AS gross_wage_usd
                    FROM public.payroll_employee employee
                    LEFT JOIN public.payroll_employee_item item
                      ON item.payroll_employee_id = employee.payroll_employee_id
                    LEFT JOIN public.payroll_component component
                      ON component.payroll_component_id = item.payroll_component_id
                    WHERE employee.payroll_run_id = :runId
                      AND employee.payroll_status = 'INCLUDED'
                    GROUP BY employee.payroll_employee_id
                ), nssf_snapshot AS (
                    SELECT employee.payroll_employee_id,
                           MAX(NULLIF(substring(item.remarks FROM
                               'nssf_fx=([0-9.]+) KHR/USD'), '')::numeric)
                               AS nssf_exchange_rate,
                           MAX(NULLIF(substring(item.remarks FROM
                               'base=([0-9.]+) KHR'), '')::numeric) FILTER (
                               WHERE component.component_code IN (
                                   'NSSF_HEALTH_EMPLOYEE',
                                   'NSSF_HEALTH_EMPLOYER',
                                   'NSSF_RISK_EMPLOYEE',
                                   'NSSF_RISK_EMPLOYER'
                               )) AS contribution_wage_khr
                    FROM public.payroll_employee employee
                    JOIN public.payroll_employee_item item
                      ON item.payroll_employee_id = employee.payroll_employee_id
                     AND item.source_type = 'NSSF'
                    LEFT JOIN public.payroll_component component
                      ON component.payroll_component_id = item.payroll_component_id
                    WHERE employee.payroll_run_id = :runId
                      AND employee.payroll_status = 'INCLUDED'
                    GROUP BY employee.payroll_employee_id
                )
                SELECT employee.payroll_employee_id,
                       employee.emp_id,
                       employee.insurance_no,
                       master.nsff_card_number,
                       master.id_card_number,
                       employee.employee_name_en,
                       employee.employee_name_kh,
                       CASE LOWER(CAST(master.gender AS text))
                           WHEN 'male' THEN 'Male | ប្រុស'
                           WHEN 'female' THEN 'Female | ស្រី'
                           ELSE COALESCE(CAST(master.gender AS text), '')
                       END AS sex,
                       CASE
                           WHEN nationality.nationality_en IS NULL
                                AND nationality.nationality_kh IS NULL THEN ''
                           WHEN COALESCE(nationality.nationality_kh, '') = ''
                                THEN COALESCE(nationality.nationality_en, '')
                           WHEN COALESCE(nationality.nationality_en, '') = ''
                                THEN COALESCE(nationality.nationality_kh, '')
                           ELSE nationality.nationality_en || ' | ' || nationality.nationality_kh
                       END AS nationality,
                       master.date_of_birth,
                       master.join_date,
                       COALESCE(earnings.gross_wage_usd, 0) AS gross_wage_usd,
                       ROUND(COALESCE(earnings.gross_wage_usd, 0)
                             * COALESCE(snapshot.nssf_exchange_rate,
                                        context.nssf_usd_to_khr_rate), 2)
                           AS gross_wage_khr,
                       snapshot.nssf_exchange_rate AS nssf_exchange_rate,
                       COALESCE(snapshot.contribution_wage_khr, 0)
                           AS contribution_wage_khr,
                       COALESCE(SUM(item.amount) FILTER (
                           WHERE component.component_code = 'NSSF_HEALTH_EMPLOYEE'), 0)
                           AS health_employee_khr,
                       COALESCE(SUM(item.amount) FILTER (
                           WHERE component.component_code = 'NSSF_HEALTH_EMPLOYER'), 0)
                           AS health_employer_khr,
                       COALESCE(SUM(item.amount) FILTER (
                           WHERE component.component_code = 'NSSF_RISK_EMPLOYEE'), 0)
                           AS risk_employee_khr,
                       COALESCE(SUM(item.amount) FILTER (
                           WHERE component.component_code = 'NSSF_RISK_EMPLOYER'), 0)
                           AS risk_employer_khr,
                       COALESCE(SUM(item.amount) FILTER (
                           WHERE component.component_code = 'NSSF_PENSION_EMPLOYEE'), 0)
                           AS pension_employee_khr,
                       COALESCE(SUM(item.amount) FILTER (
                           WHERE component.component_code = 'NSSF_PENSION_EMPLOYER'), 0)
                           AS pension_employer_khr
                FROM public.payroll_employee employee
                JOIN public.emp_master master
                  ON master.emp_id = employee.emp_id
                LEFT JOIN public.nationality nationality
                  ON nationality.nationality_id = master.nationality_id
                CROSS JOIN run_context context
                LEFT JOIN nssf_earnings earnings
                  ON earnings.payroll_employee_id = employee.payroll_employee_id
                LEFT JOIN nssf_snapshot snapshot
                  ON snapshot.payroll_employee_id = employee.payroll_employee_id
                LEFT JOIN public.payroll_employee_item item
                  ON item.payroll_employee_id = employee.payroll_employee_id
                 AND item.source_type = 'NSSF'
                LEFT JOIN public.payroll_component component
                  ON component.payroll_component_id = item.payroll_component_id
                 AND component.component_code IN (
                     'NSSF_HEALTH_EMPLOYEE',
                     'NSSF_HEALTH_EMPLOYER',
                     'NSSF_RISK_EMPLOYEE',
                     'NSSF_RISK_EMPLOYER',
                     'NSSF_PENSION_EMPLOYEE',
                     'NSSF_PENSION_EMPLOYER'
                 )
                WHERE employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'INCLUDED'
                GROUP BY employee.payroll_employee_id,
                         employee.emp_id,
                         employee.insurance_no,
                         master.nsff_card_number,
                         master.id_card_number,
                         employee.employee_name_en,
                         employee.employee_name_kh,
                         master.gender,
                         nationality.nationality_en,
                         nationality.nationality_kh,
                         master.date_of_birth,
                         master.join_date,
                         earnings.gross_wage_usd,
                         snapshot.nssf_exchange_rate,
                         snapshot.contribution_wage_khr,
                         context.nssf_usd_to_khr_rate
                HAVING COALESCE(SUM(item.amount) FILTER (
                           WHERE component.component_code IN (
                               'NSSF_HEALTH_EMPLOYEE',
                               'NSSF_HEALTH_EMPLOYER',
                               'NSSF_RISK_EMPLOYEE',
                               'NSSF_RISK_EMPLOYER',
                               'NSSF_PENSION_EMPLOYEE',
                               'NSSF_PENSION_EMPLOYER'
                           )), 0) <> 0
                ORDER BY employee.insurance_no, employee.payroll_employee_id
                """)
                .setParameter("runId", runId)
                .getResultList();

        return rows.stream()
                .map(value -> (Object[]) value)
                .map(row -> new NssfPaymentRow(
                        longObject(row[0]),
                        longObject(row[1]),
                        integer(row[2]),
                        string(row[3]),
                        string(row[4]),
                        string(row[5]),
                        string(row[6]),
                        string(row[7]),
                        string(row[8]),
                        localDate(row[9]),
                        localDate(row[10]),
                        decimal(row[11]),
                        decimal(row[12]),
                        decimal(row[13]),
                        decimal(row[14]),
                        decimal(row[15]),
                        decimal(row[16]),
                        decimal(row[17]),
                        decimal(row[18]),
                        decimal(row[19]),
                        decimal(row[20])))
                .toList();
    }

    public boolean hasEffectiveCompanyPaymentSetting(LocalDate referenceDate) {
        Object value = entityManager.createNativeQuery("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.payroll_payment_setting setting
                    WHERE setting.emp_id IS NULL
                      AND setting.active = TRUE
                      AND setting.effective_from <= :referenceDate
                      AND (setting.effective_to IS NULL
                           OR setting.effective_to >= :referenceDate)
                )
                """)
                .setParameter("referenceDate", referenceDate)
                .getSingleResult();
        return bool(value);
    }

    public int deleteEmployeePayments(Long batchId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_payment
                WHERE payroll_payment_batch_id = :batchId
                """)
                .setParameter("batchId", batchId)
                .executeUpdate();
    }

    public int insertFirstInstallment(
            Long periodId,
            LocalDate referenceDate,
            Long batchId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH company_setting AS (
                    SELECT setting.payment_frequency,
                           setting.first_payment_percent,
                           setting.shift_id
                    FROM public.payroll_payment_setting setting
                    WHERE setting.emp_id IS NULL
                      AND setting.active = TRUE
                      AND setting.effective_from <= :referenceDate
                      AND (setting.effective_to IS NULL OR setting.effective_to >= :referenceDate)
                    ORDER BY setting.effective_from DESC,
                             setting.payroll_payment_setting_id DESC
                    LIMIT 1
                ), rostered AS (
                    SELECT DISTINCT ON (roster.emp_id)
                           roster.emp_id,
                           cycle.shift_id
                    FROM public.emp_roster roster
                    JOIN public.shift_cycle cycle
                      ON cycle.shift_cycle_id = roster.shift_cycle_id
                    JOIN public.payroll_period period
                      ON period.payroll_period_id = :periodId
                    WHERE roster.roster_date BETWEEN period.period_start AND period.period_end
                    ORDER BY roster.emp_id,
                             roster.roster_date DESC,
                             roster.roster_id DESC
                ), resolved AS (
                    SELECT employee.emp_id,
                           employee.insurance_no,
                           employee.name_en,
                           employee.name_kh,
                           bank.bank_name,
                           employee.bank_account,
                           employee.gross_salary_usd,
                           period.payroll_currency,
                           period.usd_to_khr_rate,
                           CASE
                               WHEN company.payment_frequency = 'SEMI_MONTHLY'
                                   THEN 'SEMI_MONTHLY'
                               WHEN company.payment_frequency = 'MONTHLY'
                                    AND company.shift_id IS NOT NULL
                                    AND company.shift_id = rostered.shift_id
                                   THEN 'SEMI_MONTHLY'
                               ELSE 'MONTHLY'
                           END AS payment_frequency,
                           CASE
                               WHEN company.payment_frequency = 'SEMI_MONTHLY'
                                   THEN company.first_payment_percent
                               WHEN company.payment_frequency = 'MONTHLY'
                                    AND company.shift_id IS NOT NULL
                                    AND company.shift_id = rostered.shift_id
                                   THEN company.first_payment_percent
                               ELSE 0
                           END AS first_payment_percent
                    FROM rostered
                    JOIN public.emp_master employee ON employee.emp_id = rostered.emp_id
                    LEFT JOIN public.list_bank bank ON bank.bank_id = employee.bank_id
                    JOIN public.payroll_period period ON period.payroll_period_id = :periodId
                    CROSS JOIN company_setting company
                ), eligible AS (
                    SELECT resolved.*
                    FROM resolved
                    WHERE resolved.payment_frequency = 'SEMI_MONTHLY'
                      AND COALESCE(resolved.gross_salary_usd, 0) > 0
                ), amounts AS (
                    SELECT eligible.*,
                           CASE
                               WHEN UPPER(payroll_currency) = 'KHR'
                               THEN ROUND(gross_salary_usd * usd_to_khr_rate, 2)
                               ELSE ROUND(gross_salary_usd, 2)
                           END AS basic_salary_in_payment_currency
                    FROM eligible
                )
                INSERT INTO public.payroll_employee_payment
                    (payroll_payment_batch_id, emp_id, payroll_employee_id,
                     insurance_no_snapshot, employee_name_en_snapshot,
                     employee_name_kh_snapshot, bank_name_snapshot, bank_account_snapshot,
                     payment_frequency, first_payment_percent, basic_salary,
                     full_net_amount, previous_paid_amount, payment_amount,
                     carry_forward_amount, currency_code, remarks,
                     created_by, updated_by)
                SELECT :batchId, emp_id, NULL,
                       insurance_no, name_en, name_kh, bank_name, bank_account,
                       'SEMI_MONTHLY', first_payment_percent,
                       basic_salary_in_payment_currency,
                       0, 0,
                       ROUND(basic_salary_in_payment_currency
                             * first_payment_percent / 100, 2),
                       0, UPPER(payroll_currency),
                       'First installment: basic salary × '
                           || CAST(first_payment_percent AS TEXT) || '%',
                       :userId, :userId
                FROM amounts
                """)
                .setParameter("periodId", periodId)
                .setParameter("referenceDate", referenceDate)
                .setParameter("batchId", batchId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertFinalSettlement(
            Long periodId,
            LocalDate referenceDate,
            Long runId,
            Long batchId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH paid_first AS (
                    SELECT detail.emp_id,
                           SUM(detail.payment_amount) AS paid_amount,
                           MAX(detail.first_payment_percent) AS first_payment_percent
                    FROM public.payroll_employee_payment detail
                    JOIN public.payroll_payment_batch batch
                      ON batch.payroll_payment_batch_id = detail.payroll_payment_batch_id
                    WHERE batch.payroll_period_id = :periodId
                      AND batch.installment_type = 'FIRST_INSTALLMENT'
                      AND batch.status = 'PAID'
                    GROUP BY detail.emp_id
                ), company_setting AS (
                    SELECT setting.payment_frequency,
                           setting.first_payment_percent,
                           setting.shift_id
                    FROM public.payroll_payment_setting setting
                    WHERE setting.emp_id IS NULL
                      AND setting.active = TRUE
                      AND setting.effective_from <= :referenceDate
                      AND (setting.effective_to IS NULL OR setting.effective_to >= :referenceDate)
                    ORDER BY setting.effective_from DESC,
                             setting.payroll_payment_setting_id DESC
                    LIMIT 1
                ), rostered_shift AS (
                    SELECT DISTINCT ON (roster.emp_id)
                           roster.emp_id,
                           cycle.shift_id
                    FROM public.emp_roster roster
                    JOIN public.shift_cycle cycle
                      ON cycle.shift_cycle_id = roster.shift_cycle_id
                    JOIN public.payroll_period period
                      ON period.payroll_period_id = :periodId
                    WHERE roster.roster_date BETWEEN period.period_start AND period.period_end
                    ORDER BY roster.emp_id,
                             roster.roster_date DESC,
                             roster.roster_id DESC
                ), employee_setting AS (
                    SELECT payroll_employee.payroll_employee_id,
                           CASE
                               WHEN company.payment_frequency = 'SEMI_MONTHLY'
                                   THEN 'SEMI_MONTHLY'
                               WHEN company.payment_frequency = 'MONTHLY'
                                    AND company.shift_id IS NOT NULL
                                    AND company.shift_id = rostered_shift.shift_id
                                   THEN 'SEMI_MONTHLY'
                               ELSE 'MONTHLY'
                           END AS payment_frequency,
                           CASE
                               WHEN company.payment_frequency = 'SEMI_MONTHLY'
                                   THEN company.first_payment_percent
                               WHEN company.payment_frequency = 'MONTHLY'
                                    AND company.shift_id IS NOT NULL
                                    AND company.shift_id = rostered_shift.shift_id
                                   THEN company.first_payment_percent
                               ELSE 0
                           END AS first_payment_percent
                    FROM public.payroll_employee payroll_employee
                    LEFT JOIN rostered_shift
                      ON rostered_shift.emp_id = payroll_employee.emp_id
                    CROSS JOIN company_setting company
                    WHERE payroll_employee.payroll_run_id = :runId
                )
                INSERT INTO public.payroll_employee_payment
                    (payroll_payment_batch_id, emp_id, payroll_employee_id,
                     insurance_no_snapshot, employee_name_en_snapshot,
                     employee_name_kh_snapshot, bank_name_snapshot, bank_account_snapshot,
                     payment_frequency, first_payment_percent, basic_salary,
                     full_net_amount, previous_paid_amount, payment_amount,
                     carry_forward_amount, currency_code, remarks,
                     created_by, updated_by)
                SELECT :batchId, payroll_employee.emp_id, payroll_employee.payroll_employee_id,
                       payroll_employee.insurance_no,
                       payroll_employee.employee_name_en,
                       payroll_employee.employee_name_kh,
                       payroll_employee.bank_name,
                       payroll_employee.bank_account,
                       CASE WHEN COALESCE(paid_first.paid_amount, 0) > 0
                            THEN 'SEMI_MONTHLY'
                            ELSE employee_setting.payment_frequency END,
                       CASE WHEN COALESCE(paid_first.paid_amount, 0) > 0
                            THEN COALESCE(paid_first.first_payment_percent, 0)
                            ELSE employee_setting.first_payment_percent END,
                       payroll_employee.basic_salary,
                       payroll_employee.net_salary,
                       COALESCE(paid_first.paid_amount, 0),
                       GREATEST(payroll_employee.net_salary
                                - COALESCE(paid_first.paid_amount, 0), 0),
                       GREATEST(COALESCE(paid_first.paid_amount, 0)
                                - payroll_employee.net_salary, 0),
                       payroll_employee.salary_currency,
                       CASE WHEN COALESCE(paid_first.paid_amount, 0) > 0
                            THEN 'Final monthly net less paid first installment'
                            ELSE 'Full monthly net paid once' END,
                       :userId, :userId
                FROM public.payroll_employee payroll_employee
                JOIN employee_setting
                  ON employee_setting.payroll_employee_id = payroll_employee.payroll_employee_id
                LEFT JOIN paid_first ON paid_first.emp_id = payroll_employee.emp_id
                WHERE payroll_employee.payroll_run_id = :runId
                  AND payroll_employee.payroll_status = 'INCLUDED'
                """)
                .setParameter("periodId", periodId)
                .setParameter("referenceDate", referenceDate)
                .setParameter("runId", runId)
                .setParameter("batchId", batchId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertAdjustmentSettlement(
            Long periodId,
            Long runId,
            Long batchId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH paid_regular AS (
                    SELECT detail.emp_id,
                           COALESCE(SUM(detail.payment_amount), 0) AS paid_amount
                    FROM public.payroll_employee_payment detail
                    JOIN public.payroll_payment_batch batch
                      ON batch.payroll_payment_batch_id = detail.payroll_payment_batch_id
                    WHERE batch.payroll_period_id = :periodId
                      AND batch.installment_type IN ('FIRST_INSTALLMENT', 'FINAL_SETTLEMENT')
                      AND batch.status = 'PAID'
                    GROUP BY detail.emp_id
                )
                INSERT INTO public.payroll_employee_payment
                    (payroll_payment_batch_id, emp_id, payroll_employee_id,
                     insurance_no_snapshot, employee_name_en_snapshot,
                     employee_name_kh_snapshot, bank_name_snapshot, bank_account_snapshot,
                     payment_frequency, first_payment_percent, basic_salary,
                     full_net_amount, previous_paid_amount, payment_amount,
                     carry_forward_amount, currency_code, remarks,
                     created_by, updated_by)
                SELECT :batchId, adjustment_employee.emp_id,
                       adjustment_employee.payroll_employee_id,
                       adjustment_employee.insurance_no,
                       adjustment_employee.employee_name_en,
                       adjustment_employee.employee_name_kh,
                       adjustment_employee.bank_name,
                       adjustment_employee.bank_account,
                       'MONTHLY', 0,
                       adjustment_employee.basic_salary,
                       adjustment_employee.net_salary,
                       COALESCE(paid_regular.paid_amount, 0),
                       0,
                       ABS(adjustment_employee.net_salary),
                       adjustment_employee.salary_currency,
                       CASE
                           WHEN adjustment_employee.net_salary > 0
                           THEN 'Post-paid adjustment earning: carry forward to next payroll'
                           ELSE 'Post-paid adjustment recovery: carry forward to next payroll'
                       END,
                       :userId, :userId
                FROM public.payroll_employee adjustment_employee
                LEFT JOIN paid_regular
                  ON paid_regular.emp_id = adjustment_employee.emp_id
                WHERE adjustment_employee.payroll_run_id = :runId
                  AND adjustment_employee.payroll_status = 'INCLUDED'
                  AND adjustment_employee.net_salary <> 0
                """)
                .setParameter("periodId", periodId)
                .setParameter("runId", runId)
                .setParameter("batchId", batchId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int createPositiveAdjustmentCarryForwards(Long batchId, Long userId) {
        entityManager.createNativeQuery("""
                INSERT INTO public.payroll_component
                    (component_code, component_name_en, component_name_kh, component_type,
                     calculation_method, taxable, subject_to_nssf, subject_to_seniority, active, sort_order,
                     created_by, updated_by)
                VALUES
                    ('PRIOR_PAYROLL_ADJUSTMENT', 'Prior Payroll Adjustment',
                     'ប្រាក់កែតម្រូវពីខែមុន', 'EARNING', 'FORMULA',
                     FALSE, FALSE, FALSE, TRUE, 915, :userId, :userId)
                ON CONFLICT (component_code) DO NOTHING
                """)
                .setParameter("userId", userId)
                .executeUpdate();

        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee_recurring_component
                    (emp_id, payroll_component_id, amount, currency_code, recurrence_type,
                     effective_from, effective_to, active, description, remarks, created_by, updated_by)
                SELECT detail.emp_id, component.payroll_component_id, detail.carry_forward_amount,
                       detail.currency_code, 'ONE_TIME', period.period_end + 1, NULL, TRUE,
                       'Prior payroll positive adjustment',
                       'Automatic positive adjustment carry-forward; payment_detail='
                           || detail.payroll_employee_payment_id::text,
                       :userId, :userId
                FROM public.payroll_employee_payment detail
                JOIN public.payroll_payment_batch batch
                  ON batch.payroll_payment_batch_id = detail.payroll_payment_batch_id
                JOIN public.payroll_period period
                  ON period.payroll_period_id = batch.payroll_period_id
                JOIN public.payroll_component component
                  ON component.component_code = 'PRIOR_PAYROLL_ADJUSTMENT'
                WHERE batch.payroll_payment_batch_id = :batchId
                  AND batch.installment_type = 'ADJUSTMENT_SETTLEMENT'
                  AND batch.status = 'PAID'
                  AND detail.payment_amount = 0
                  AND detail.carry_forward_amount > 0
                  AND COALESCE(detail.remarks, '') LIKE 'Post-paid adjustment earning:%'
                  AND NOT EXISTS (
                      SELECT 1 FROM public.payroll_employee_recurring_component existing
                      WHERE COALESCE(existing.remarks, '') =
                            'Automatic positive adjustment carry-forward; payment_detail='
                            || detail.payroll_employee_payment_id::text
                  )
                """)
                .setParameter("batchId", batchId)
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
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static long longValue(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static Long longObject(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer integer(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static boolean bool(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static LocalDate localDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static OffsetDateTime offsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(String.valueOf(value));
    }
    /**
     * Returns true while an adjustment settlement still has a positive earning
     * or negative recovery waiting to be consumed by a paid REGULAR payroll.
     */
    public boolean hasPendingAdjustmentCarryForward(Long runId) {
        if (runId == null) {
            return false;
        }
        Object value = entityManager.createNativeQuery("""
                SELECT CASE WHEN EXISTS (
                    SELECT 1
                    FROM public.payroll_payment_batch batch
                    JOIN public.payroll_employee_payment detail
                      ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                    WHERE batch.payroll_run_id = :runId
                      AND batch.installment_type = 'ADJUSTMENT_SETTLEMENT'
                      AND batch.status = 'PAID'
                      AND detail.carry_forward_amount > 0
                      AND (
                          (COALESCE(detail.remarks, '') LIKE 'Post-paid adjustment earning:%'
                           AND (
                               NOT EXISTS (
                                   SELECT 1
                                   FROM public.payroll_employee_recurring_component recurring
                                   WHERE COALESCE(recurring.remarks, '') =
                                         'Automatic positive adjustment carry-forward; payment_detail='
                                         || detail.payroll_employee_payment_id::text
                               )
                               OR EXISTS (
                                   SELECT 1
                                   FROM public.payroll_employee_recurring_component recurring
                                   WHERE COALESCE(recurring.remarks, '') =
                                         'Automatic positive adjustment carry-forward; payment_detail='
                                         || detail.payroll_employee_payment_id::text
                                     AND recurring.active = TRUE
                               )
                           ))
                          OR
                          (COALESCE(detail.remarks, '') NOT LIKE 'Post-paid adjustment earning:%'
                           AND (
                               NOT EXISTS (
                                   SELECT 1 FROM public.payroll_recovery recovery
                                   WHERE recovery.source_payroll_employee_payment_id = detail.payroll_employee_payment_id
                               )
                               OR EXISTS (
                                   SELECT 1 FROM public.payroll_recovery recovery
                                   WHERE recovery.source_payroll_employee_payment_id = detail.payroll_employee_payment_id
                                     AND recovery.status <> 'SETTLED'
                               )
                           ))
                      )
                ) THEN 1 ELSE 0 END
                """)
                .setParameter("runId", runId)
                .getSingleResult();
        return value != null && ((Number) value).intValue() == 1;
    }

}
