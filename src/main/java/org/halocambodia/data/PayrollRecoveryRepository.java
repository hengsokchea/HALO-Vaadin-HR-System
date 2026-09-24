package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * PostgreSQL operations for post-paid payroll recovery carry-forward.
 *
 * <p>A recovery is created from a PAID adjustment-settlement detail whose
 * carry_forward_amount is greater than zero. A regular payroll only reserves
 * recovery amounts while it is editable. The reservation becomes settled only
 * when that regular payroll's FINAL_SETTLEMENT is marked PAID.</p>
 */
@Repository
public class PayrollRecoveryRepository {

    @PersistenceContext
    private EntityManager entityManager;


    public int ensureRecoveryComponent(Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_component
                    (component_code, component_name_en, component_name_kh, component_type,
                     calculation_method, taxable, subject_to_nssf, subject_to_seniority, active, sort_order,
                     created_by, updated_by)
                VALUES
                    ('PRIOR_PAYROLL_RECOVERY', 'Prior Payroll Recovery',
                     'ការកាត់សងប្រាក់បៀវត្សមុន', 'DEDUCTION', 'FORMULA',
                     FALSE, FALSE, FALSE, TRUE, 920, :userId, :userId)
                ON CONFLICT (component_code) DO NOTHING
                """)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public boolean isEditableRegularRun(Long runId) {
        if (runId == null) {
            return false;
        }
        Object count = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_run
                WHERE payroll_run_id = :runId
                  AND run_type = 'REGULAR'
                  AND status IN ('DRAFT', 'CALCULATED')
                """)
                .setParameter("runId", runId)
                .getSingleResult();
        return count != null && ((Number) count).longValue() > 0;
    }

    public Optional<Long> findRunIdByPayrollEmployeeId(Long payrollEmployeeId) {
        if (payrollEmployeeId == null) {
            return Optional.empty();
        }
        return entityManager.createNativeQuery("""
                SELECT payroll_run_id
                FROM public.payroll_employee
                WHERE payroll_employee_id = :payrollEmployeeId
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getResultStream()
                .findFirst()
                .map(value -> ((Number) value).longValue());
    }

    public int createRecoveriesFromPaidBatch(
            Long batchId,
            Long periodId,
            Long sourceRunId,
            Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_recovery
                    (source_payroll_employee_payment_id,
                     source_payroll_period_id,
                     source_payroll_run_id,
                     source_installment_type,
                     emp_id,
                     original_amount,
                     recovered_amount,
                     currency_code,
                     status,
                     created_by,
                     updated_by)
                SELECT detail.payroll_employee_payment_id,
                       :periodId,
                       :sourceRunId,
                       batch.installment_type,
                       detail.emp_id,
                       detail.carry_forward_amount,
                       0,
                       detail.currency_code,
                       'OPEN',
                       :userId,
                       :userId
                FROM public.payroll_employee_payment detail
                JOIN public.payroll_payment_batch batch
                  ON batch.payroll_payment_batch_id = detail.payroll_payment_batch_id
                WHERE batch.payroll_payment_batch_id = :batchId
                  AND batch.payroll_period_id = :periodId
                  AND batch.payroll_run_id = :sourceRunId
                  AND batch.installment_type IN ('ADJUSTMENT_SETTLEMENT', 'FINAL_SETTLEMENT')
                  AND batch.status = 'PAID'
                  AND detail.carry_forward_amount > 0
                  AND COALESCE(detail.remarks, '') NOT LIKE 'Post-paid adjustment earning:%'
                ON CONFLICT (source_payroll_employee_payment_id) DO NOTHING
                """)
                .setParameter("batchId", batchId)
                .setParameter("periodId", periodId)
                .setParameter("sourceRunId", sourceRunId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int deleteReservedApplicationsForRun(Long runId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_recovery_application
                WHERE payroll_run_id = :runId
                  AND status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .executeUpdate();
    }

    public int deleteReservedApplicationsForEmployee(Long runId, Long payrollEmployeeId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_recovery_application
                WHERE payroll_run_id = :runId
                  AND payroll_employee_id = :payrollEmployeeId
                  AND status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int deleteReservedApplicationsForEmployees(
            Long runId, Collection<Long> payrollEmployeeIds) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_recovery_application
                WHERE payroll_run_id = :runId
                  AND payroll_employee_id IN (:payrollEmployeeIds)
                  AND status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .setParameter("payrollEmployeeIds", payrollEmployeeIds)
                .executeUpdate();
    }

    public int deleteRecoveryItemsForRun(Long runId, Long componentId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item item
                USING public.payroll_employee employee
                WHERE employee.payroll_employee_id = item.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND item.payroll_component_id = :componentId
                  AND item.source_type = 'OTHER'
                  AND COALESCE(item.remarks, '') LIKE 'Automatic prior payroll recovery;%'
                """)
                .setParameter("runId", runId)
                .setParameter("componentId", componentId)
                .executeUpdate();
    }

    public int deleteRecoveryItemsForEmployee(Long payrollEmployeeId, Long componentId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item
                WHERE payroll_employee_id = :payrollEmployeeId
                  AND payroll_component_id = :componentId
                  AND source_type = 'OTHER'
                  AND COALESCE(remarks, '') LIKE 'Automatic prior payroll recovery;%'
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .executeUpdate();
    }

    public int deleteRecoveryItemsForEmployees(
            Collection<Long> payrollEmployeeIds, Long componentId) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item
                WHERE payroll_employee_id IN (:payrollEmployeeIds)
                  AND payroll_component_id = :componentId
                  AND source_type = 'OTHER'
                  AND COALESCE(remarks, '') LIKE 'Automatic prior payroll recovery;%'
                """)
                .setParameter("payrollEmployeeIds", payrollEmployeeIds)
                .setParameter("componentId", componentId)
                .executeUpdate();
    }

    public int reserveRecoveriesForRun(Long runId, Long userId) {
        return reserveRecoveries(runId, List.of(-1L), true, userId);
    }

    public int reserveRecoveriesForEmployees(
            Long runId, Collection<Long> payrollEmployeeIds, Long userId) {
        return reserveRecoveries(runId, payrollEmployeeIds, false, userId);
    }

    public int reserveRecoveriesForEmployee(Long runId, Long payrollEmployeeId, Long userId) {
        return reserveRecoveries(runId, List.of(payrollEmployeeId), false, userId);
    }

    private int reserveRecoveries(
            Long runId,
            Collection<Long> payrollEmployeeIds,
            boolean allEmployees,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH run_ctx AS (
                    SELECT run.payroll_run_id,
                           period.period_start
                    FROM public.payroll_run run
                    JOIN public.payroll_period period
                      ON period.payroll_period_id = run.payroll_period_id
                    WHERE run.payroll_run_id = :runId
                      AND run.run_type = 'REGULAR'
                      AND run.status IN ('DRAFT', 'CALCULATED')
                ), available AS (
                    SELECT employee.payroll_employee_id,
                           employee.emp_id,
                           employee.salary_currency,
                           GREATEST(
                               COALESCE(SUM(item.payroll_amount)
                                   FILTER (WHERE component.component_type = 'EARNING'), 0)
                               - COALESCE(SUM(item.payroll_amount)
                                   FILTER (WHERE component.component_type = 'DEDUCTION'), 0),
                               0
                           ) AS available_net
                    FROM public.payroll_employee employee
                    JOIN run_ctx ctx
                      ON ctx.payroll_run_id = employee.payroll_run_id
                    LEFT JOIN public.payroll_employee_item item
                      ON item.payroll_employee_id = employee.payroll_employee_id
                    LEFT JOIN public.payroll_component component
                      ON component.payroll_component_id = item.payroll_component_id
                    WHERE employee.payroll_status = 'INCLUDED'
                      AND (:allEmployees = TRUE
                           OR employee.payroll_employee_id IN (:payrollEmployeeIds))
                    GROUP BY employee.payroll_employee_id,
                             employee.emp_id,
                             employee.salary_currency
                ), open_recovery AS (
                    SELECT recovery.payroll_recovery_id,
                           available.payroll_employee_id,
                           recovery.original_amount - recovery.recovered_amount AS outstanding_amount,
                           available.available_net,
                           source_period.period_end AS source_period_end
                    FROM public.payroll_recovery recovery
                    JOIN public.payroll_period source_period
                      ON source_period.payroll_period_id = recovery.source_payroll_period_id
                    CROSS JOIN run_ctx ctx
                    JOIN available
                      ON available.emp_id = recovery.emp_id
                     AND available.salary_currency = recovery.currency_code
                    WHERE recovery.status IN ('OPEN', 'PARTIAL')
                      AND recovery.original_amount > recovery.recovered_amount
                      AND source_period.period_end < ctx.period_start
                      AND NOT EXISTS (
                          SELECT 1
                          FROM public.payroll_recovery_application reserved_application
                          JOIN public.payroll_run reserved_run
                            ON reserved_run.payroll_run_id = reserved_application.payroll_run_id
                          JOIN public.payroll_employee reserved_employee
                            ON reserved_employee.payroll_employee_id = reserved_application.payroll_employee_id
                          WHERE reserved_application.payroll_recovery_id = recovery.payroll_recovery_id
                            AND reserved_application.status = 'RESERVED'
                            AND reserved_application.payroll_run_id <> :runId
                            AND reserved_run.status <> 'CANCELLED'
                            AND reserved_employee.payroll_status = 'INCLUDED'
                      )
                ), ordered AS (
                    SELECT open_recovery.*,
                           COALESCE(
                               SUM(open_recovery.outstanding_amount) OVER (
                                   PARTITION BY open_recovery.payroll_employee_id
                                   ORDER BY open_recovery.source_period_end,
                                            open_recovery.payroll_recovery_id
                                   ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                               ),
                               0
                           ) AS prior_outstanding
                    FROM open_recovery
                ), allocation AS (
                    SELECT payroll_recovery_id,
                           payroll_employee_id,
                           LEAST(
                               outstanding_amount,
                               GREATEST(available_net - prior_outstanding, 0)
                           ) AS applied_amount
                    FROM ordered
                )
                INSERT INTO public.payroll_recovery_application
                    (payroll_recovery_id,
                     payroll_run_id,
                     payroll_employee_id,
                     applied_amount,
                     status,
                     created_by,
                     updated_by)
                SELECT allocation.payroll_recovery_id,
                       :runId,
                       allocation.payroll_employee_id,
                       allocation.applied_amount,
                       'RESERVED',
                       :userId,
                       :userId
                FROM allocation
                WHERE allocation.applied_amount > 0
                ON CONFLICT (payroll_recovery_id, payroll_run_id) DO NOTHING
                """)
                .setParameter("runId", runId)
                .setParameter("allEmployees", allEmployees)
                .setParameter("payrollEmployeeIds", payrollEmployeeIds)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int insertRecoveryItemsForRun(Long runId, Long componentId, Long userId) {
        return insertRecoveryItems(runId, List.of(-1L), true, componentId, userId);
    }

    public int insertRecoveryItemsForEmployees(
            Long runId,
            Collection<Long> payrollEmployeeIds,
            Long componentId,
            Long userId) {
        return insertRecoveryItems(runId, payrollEmployeeIds, false, componentId, userId);
    }

    public int insertRecoveryItemsForEmployee(
            Long runId,
            Long payrollEmployeeId,
            Long componentId,
            Long userId) {
        return insertRecoveryItems(runId, List.of(payrollEmployeeId), false, componentId, userId);
    }

    private int insertRecoveryItems(
            Long runId,
            Collection<Long> payrollEmployeeIds,
            boolean allEmployees,
            Long componentId,
            Long userId) {
        return entityManager.createNativeQuery("""
                WITH reserved AS (
                    SELECT application.payroll_employee_id,
                           SUM(application.applied_amount) AS recovery_amount,
                           COUNT(*) AS recovery_count
                    FROM public.payroll_recovery_application application
                    JOIN public.payroll_employee employee
                      ON employee.payroll_employee_id = application.payroll_employee_id
                    WHERE application.payroll_run_id = :runId
                      AND application.status = 'RESERVED'
                      AND employee.payroll_status = 'INCLUDED'
                      AND (:allEmployees = TRUE
                           OR application.payroll_employee_id IN (:payrollEmployeeIds))
                    GROUP BY application.payroll_employee_id
                )
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id,
                     payroll_component_id,
                     description,
                     quantity,
                     rate,
                     amount,
                     currency_code,
                     exchange_rate,
                     payroll_amount,
                     source_type,
                     remarks,
                     created_by)
                SELECT employee.payroll_employee_id,
                       :componentId,
                       'Prior payroll recovery',
                       1,
                       reserved.recovery_amount,
                       reserved.recovery_amount,
                       employee.salary_currency,
                       1,
                       reserved.recovery_amount,
                       'OTHER',
                       'Automatic prior payroll recovery; source balance(s)=' || reserved.recovery_count::text,
                       :userId
                FROM reserved
                JOIN public.payroll_employee employee
                  ON employee.payroll_employee_id = reserved.payroll_employee_id
                WHERE reserved.recovery_amount > 0
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee_item existing
                      WHERE existing.payroll_employee_id = employee.payroll_employee_id
                        AND existing.payroll_component_id = :componentId
                        AND existing.source_type = 'OTHER'
                        AND COALESCE(existing.remarks, '') LIKE 'Automatic prior payroll recovery;%'
                  )
                """)
                .setParameter("runId", runId)
                .setParameter("allEmployees", allEmployees)
                .setParameter("payrollEmployeeIds", payrollEmployeeIds)
                .setParameter("componentId", componentId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int settleIncludedReservations(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_recovery_application application
                SET status = 'SETTLED',
                    settled_at = CURRENT_TIMESTAMP,
                    settled_by = :userId,
                    released_at = NULL,
                    released_by = NULL,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId
                FROM public.payroll_employee employee
                WHERE application.payroll_run_id = :runId
                  AND application.status = 'RESERVED'
                  AND employee.payroll_employee_id = application.payroll_employee_id
                  AND employee.payroll_run_id = :runId
                  AND employee.payroll_status = 'INCLUDED'
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int releaseUnpaidReservations(Long runId, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_recovery_application
                SET status = 'RELEASED',
                    released_at = CURRENT_TIMESTAMP,
                    released_by = :userId,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId
                WHERE payroll_run_id = :runId
                  AND status = 'RESERVED'
                """)
                .setParameter("runId", runId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int refreshRecoveryStatuses(Long userId) {
        return entityManager.createNativeQuery("""
                WITH settled AS (
                    SELECT recovery.payroll_recovery_id,
                           LEAST(
                               recovery.original_amount,
                               COALESCE(SUM(application.applied_amount)
                                   FILTER (WHERE application.status = 'SETTLED'), 0)
                           ) AS recovered_amount
                    FROM public.payroll_recovery recovery
                    LEFT JOIN public.payroll_recovery_application application
                      ON application.payroll_recovery_id = recovery.payroll_recovery_id
                    GROUP BY recovery.payroll_recovery_id,
                             recovery.original_amount
                )
                UPDATE public.payroll_recovery recovery
                SET recovered_amount = settled.recovered_amount,
                    status = CASE
                        WHEN settled.recovered_amount >= recovery.original_amount THEN 'SETTLED'
                        WHEN settled.recovered_amount > 0 THEN 'PARTIAL'
                        ELSE 'OPEN'
                    END,
                    settled_at = CASE
                        WHEN settled.recovered_amount >= recovery.original_amount
                        THEN COALESCE(recovery.settled_at, CURRENT_TIMESTAMP)
                        ELSE NULL
                    END,
                    settled_by = CASE
                        WHEN settled.recovered_amount >= recovery.original_amount
                        THEN COALESCE(recovery.settled_by, :userId)
                        ELSE NULL
                    END,
                    updated_at = CURRENT_TIMESTAMP,
                    updated_by = :userId
                FROM settled
                WHERE recovery.payroll_recovery_id = settled.payroll_recovery_id
                  AND (
                      recovery.recovered_amount IS DISTINCT FROM settled.recovered_amount
                      OR recovery.status IS DISTINCT FROM CASE
                          WHEN settled.recovered_amount >= recovery.original_amount THEN 'SETTLED'
                          WHEN settled.recovered_amount > 0 THEN 'PARTIAL'
                          ELSE 'OPEN'
                      END
                  )
                """)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
