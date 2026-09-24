package org.halocambodia.data;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** PostgreSQL operations for recurring items and employee loan/recovery reservations. */
@Repository
public class PayrollRecurringCalculationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public int deleteGeneratedItems(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item i
                USING public.payroll_employee pe
                WHERE pe.payroll_employee_id = i.payroll_employee_id
                  AND pe.payroll_run_id = :runId
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND i.source_type IN ('RECURRING', 'LOAN')
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int deleteReservedRecurringApplications(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_recurring_application a
                WHERE a.payroll_run_id = :runId
                  AND a.status = 'RESERVED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR a.payroll_employee_id = :payrollEmployeeId)
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int deleteReservedLoanRepayments(Long runId, @Nullable Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_loan_repayment r
                WHERE r.payroll_run_id = :runId
                  AND r.status = 'RESERVED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR r.payroll_employee_id = :payrollEmployeeId)
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    /**
     * Repairs/synchronizes positive adjustment carry-forwards before a REGULAR
     * payroll is calculated. This also covers adjustment settlements finalized
     * before the carry-forward row could be created (for example, after an older
     * SQL failure). Each payment detail is imported only once.
     */
    public int syncPositiveAdjustmentCarryForwards(Long runId, Long userId) {
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
                WITH target AS (
                    SELECT p.period_end
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                      AND r.run_type = 'REGULAR'
                )
                INSERT INTO public.payroll_employee_recurring_component
                    (emp_id, payroll_component_id, amount, currency_code, recurrence_type,
                     effective_from, effective_to, active, description, remarks, created_by, updated_by)
                SELECT detail.emp_id, component.payroll_component_id, detail.carry_forward_amount,
                       detail.currency_code, 'ONE_TIME', source_period.period_end + 1, NULL, TRUE,
                       'Prior payroll positive adjustment',
                       'Automatic positive adjustment carry-forward; payment_detail='
                           || detail.payroll_employee_payment_id::text,
                       :userId, :userId
                FROM target
                JOIN public.payroll_payment_batch batch
                  ON batch.installment_type = 'ADJUSTMENT_SETTLEMENT'
                 AND batch.status = 'PAID'
                JOIN public.payroll_period source_period
                  ON source_period.payroll_period_id = batch.payroll_period_id
                 AND source_period.period_end < target.period_end
                 AND source_period.period_end + 1 <= target.period_end
                JOIN public.payroll_employee_payment detail
                  ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                JOIN public.payroll_component component
                  ON component.component_code = 'PRIOR_PAYROLL_ADJUSTMENT'
                WHERE detail.payment_amount = 0
                  AND detail.carry_forward_amount > 0
                  AND COALESCE(detail.remarks, '') LIKE 'Post-paid adjustment earning:%'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee_recurring_component existing
                      WHERE COALESCE(existing.remarks, '') =
                            'Automatic positive adjustment carry-forward; payment_detail='
                            || detail.payroll_employee_payment_id::text
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertRecurringApplications(Long runId, @Nullable Long payrollEmployeeId, Long userId) {
        return entityManager.createNativeQuery("""
                WITH context AS (
                    SELECT r.payroll_run_id, p.payment_date, p.period_end
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                      AND r.run_type = 'REGULAR'
                )
                INSERT INTO public.payroll_employee_recurring_application
                    (payroll_employee_recurring_component_id, payroll_run_id,
                     payroll_employee_id, applied_amount, status, created_by)
                SELECT cfg.payroll_employee_recurring_component_id,
                       pe.payroll_run_id,
                       pe.payroll_employee_id,
                       cfg.amount,
                       'RESERVED',
                       :userId
                FROM context x
                JOIN public.payroll_employee pe
                  ON pe.payroll_run_id = x.payroll_run_id
                 AND pe.payroll_status = 'INCLUDED'
                JOIN public.payroll_employee_recurring_component cfg
                  ON cfg.emp_id = pe.emp_id
                 AND cfg.active = TRUE
                JOIN public.payroll_component component
                  ON component.payroll_component_id = cfg.payroll_component_id
                 AND component.active = TRUE
                 AND component.component_code IN ('FIXED_INCOME', 'OTHER_DEDUCTION', 'PRIOR_PAYROLL_ADJUSTMENT')
                WHERE cfg.effective_from <= CASE
                          WHEN component.component_code = 'PRIOR_PAYROLL_ADJUSTMENT' THEN x.period_end
                          ELSE x.payment_date
                      END
                  AND (cfg.effective_to IS NULL OR cfg.effective_to >= CASE
                          WHEN component.component_code = 'PRIOR_PAYROLL_ADJUSTMENT' THEN x.period_end
                          ELSE x.payment_date
                      END)
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                  AND (
                      cfg.recurrence_type = 'MONTHLY'
                      OR (
                          cfg.recurrence_type = 'ONE_TIME'
                          AND NOT EXISTS (
                              SELECT 1
                              FROM public.payroll_employee_recurring_application settled
                              WHERE settled.payroll_employee_recurring_component_id = cfg.payroll_employee_recurring_component_id
                                AND settled.status IN ('RESERVED', 'SETTLED')
                          )
                      )
                  )
                ON CONFLICT (payroll_employee_recurring_component_id, payroll_run_id)
                DO NOTHING
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertRecurringItems(Long runId, @Nullable Long payrollEmployeeId, Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description,
                     quantity, rate, amount, currency_code, exchange_rate,
                     payroll_amount, source_type, remarks, created_by)
                SELECT a.payroll_employee_id,
                       cfg.payroll_component_id,
                       COALESCE(NULLIF(cfg.description, ''), component.component_name_en),
                       1,
                       a.applied_amount,
                       a.applied_amount,
                       'USD',
                       1,
                       a.applied_amount,
                       'RECURRING',
                       'Recurring application id=' || a.payroll_employee_recurring_application_id::text
                           || '; config=' || cfg.payroll_employee_recurring_component_id::text
                           || '; recurrence=' || cfg.recurrence_type
                           || CASE WHEN cfg.remarks IS NULL OR cfg.remarks = ''
                                   THEN '' ELSE '; ' || cfg.remarks END,
                       :userId
                FROM public.payroll_employee_recurring_application a
                JOIN public.payroll_employee_recurring_component cfg
                  ON cfg.payroll_employee_recurring_component_id = a.payroll_employee_recurring_component_id
                JOIN public.payroll_component component
                  ON component.payroll_component_id = cfg.payroll_component_id
                WHERE a.payroll_run_id = :runId
                  AND a.status = 'RESERVED'
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR a.payroll_employee_id = :payrollEmployeeId)
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertLoanReservations(Long runId, @Nullable Long payrollEmployeeId, Long userId) {
        return entityManager.createNativeQuery("""
                WITH context AS (
                    SELECT r.payroll_run_id, p.payment_date
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.payroll_run_id = :runId
                      AND r.run_type = 'REGULAR'
                )
                INSERT INTO public.payroll_employee_loan_repayment
                    (payroll_employee_loan_id, payroll_run_id, payroll_employee_id,
                     scheduled_amount, deducted_amount, balance_before, balance_after,
                     status, created_by)
                SELECT loan.payroll_employee_loan_id,
                       pe.payroll_run_id,
                       pe.payroll_employee_id,
                       loan.monthly_installment,
                       LEAST(loan.monthly_installment, loan.outstanding_balance),
                       loan.outstanding_balance,
                       GREATEST(loan.outstanding_balance - loan.monthly_installment, 0),
                       'RESERVED',
                       :userId
                FROM context x
                JOIN public.payroll_employee pe
                  ON pe.payroll_run_id = x.payroll_run_id
                 AND pe.payroll_status = 'INCLUDED'
                JOIN public.payroll_employee_loan loan
                  ON loan.emp_id = pe.emp_id
                 AND loan.status = 'ACTIVE'
                 AND loan.outstanding_balance > 0
                 AND loan.start_date <= x.payment_date
                 AND (loan.end_date IS NULL OR loan.end_date >= x.payment_date)
                WHERE (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR pe.payroll_employee_id = :payrollEmployeeId)
                ON CONFLICT (payroll_employee_loan_id, payroll_run_id)
                DO NOTHING
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertLoanItems(Long runId, @Nullable Long payrollEmployeeId, Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description,
                     quantity, rate, amount, currency_code, exchange_rate,
                     payroll_amount, source_type, remarks, created_by)
                SELECT repayment.payroll_employee_id,
                       component.payroll_component_id,
                       CASE loan.recovery_type
                           WHEN 'ASSET_DAMAGE' THEN 'Asset Damage Recovery · ' || loan.loan_reference
                           WHEN 'ASSET_LOSS' THEN 'Asset Loss Recovery · ' || loan.loan_reference
                           WHEN 'OTHER_RECOVERY' THEN 'Other Employee Recovery · ' || loan.loan_reference
                           ELSE 'Company Loan Repayment · ' || loan.loan_reference
                       END,
                       1,
                       repayment.deducted_amount,
                       repayment.deducted_amount,
                       'USD',
                       1,
                       repayment.deducted_amount,
                       'LOAN',
                       'Loan repayment reservation id=' || repayment.payroll_employee_loan_repayment_id::text
                           || '; loan=' || loan.payroll_employee_loan_id::text
                           || '; recovery type=' || loan.recovery_type
                           || '; reference=' || loan.loan_reference
                           || '; balance before=' || repayment.balance_before::text
                           || '; projected balance=' || repayment.balance_after::text,
                       :userId
                FROM public.payroll_employee_loan_repayment repayment
                JOIN public.payroll_employee_loan loan
                  ON loan.payroll_employee_loan_id = repayment.payroll_employee_loan_id
                JOIN public.payroll_component component
                  ON component.component_code = 'COMPANY_LOAN_REPAYMENT'
                 AND component.active = TRUE
                WHERE repayment.payroll_run_id = :runId
                  AND repayment.status = 'RESERVED'
                  AND repayment.deducted_amount > 0
                  AND (CAST(:payrollEmployeeId AS BIGINT) IS NULL
                       OR repayment.payroll_employee_id = :payrollEmployeeId)
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public long countLoanSettlementConflicts(Long runId) {
        Object result = entityManager.createNativeQuery("""
                WITH due AS (
                    SELECT payroll_employee_loan_id,
                           SUM(deducted_amount) AS amount,
                           MAX(balance_before) AS balance_before
                    FROM public.payroll_employee_loan_repayment
                    WHERE payroll_run_id = :runId
                      AND status = 'RESERVED'
                    GROUP BY payroll_employee_loan_id
                )
                SELECT COUNT(*)
                FROM due
                JOIN public.payroll_employee_loan loan
                  ON loan.payroll_employee_loan_id = due.payroll_employee_loan_id
                WHERE loan.outstanding_balance <> due.balance_before
                   OR loan.outstanding_balance < due.amount
                """)
                .setParameter("runId", runId)
                .getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    public int settleLoans(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                WITH due AS (
                    SELECT payroll_employee_loan_id, SUM(deducted_amount) AS amount
                    FROM public.payroll_employee_loan_repayment
                    WHERE payroll_run_id = :runId
                      AND status = 'RESERVED'
                    GROUP BY payroll_employee_loan_id
                )
                UPDATE public.payroll_employee_loan loan
                   SET outstanding_balance = GREATEST(loan.outstanding_balance - due.amount, 0),
                       status = CASE
                           WHEN loan.outstanding_balance - due.amount <= 0 THEN 'SETTLED'
                           ELSE loan.status
                       END,
                       updated_at = CURRENT_TIMESTAMP,
                       updated_by = :userId,
                       version = version + 1
                  FROM due
                 WHERE loan.payroll_employee_loan_id = due.payroll_employee_loan_id
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int settleLoanRepayments(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee_loan_repayment
                   SET status = 'SETTLED',
                       settled_at = CURRENT_TIMESTAMP,
                       settled_by = :userId
                 WHERE payroll_run_id = :runId
                   AND status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int settleRecurringApplications(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee_recurring_application
                   SET status = 'SETTLED',
                       settled_at = CURRENT_TIMESTAMP,
                       settled_by = :userId
                 WHERE payroll_run_id = :runId
                   AND status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    /**
     * A ONE_TIME recurring earning/deduction is finished after the payroll
     * application is financially settled. Keep the application history but
     * make the configuration inactive so it cannot be picked up again and the
     * setup screen immediately shows INACTIVE.
     */
    public int deactivateSettledOneTimeRecurring(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee_recurring_component recurring
                   SET active = FALSE,
                       updated_at = CURRENT_TIMESTAMP,
                       updated_by = :userId,
                       version = version + 1
                 WHERE recurring.active = TRUE
                   AND recurring.recurrence_type = 'ONE_TIME'
                   AND EXISTS (
                       SELECT 1
                       FROM public.payroll_employee_recurring_application application
                       WHERE application.payroll_employee_recurring_component_id =
                                 recurring.payroll_employee_recurring_component_id
                         AND application.payroll_run_id = :runId
                         AND application.status = 'SETTLED'
                   )
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
