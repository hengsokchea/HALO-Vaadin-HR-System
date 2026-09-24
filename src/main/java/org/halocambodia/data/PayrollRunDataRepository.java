package org.halocambodia.data;

import static org.halocambodia.data.PayrollModels.DashboardStats;
import static org.halocambodia.data.PayrollModels.PayrollReconciliationSummary;
import static org.halocambodia.data.PayrollModels.RunRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Read/update operations for payroll-run workflow data. */
@Repository
public class PayrollRunDataRepository {

    @PersistenceContext
    private EntityManager entityManager;


    public DashboardStats getDashboardStats() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                WITH latest AS (
                    SELECT r.payroll_run_id, p.payroll_year, p.payroll_month
                    FROM public.payroll_run r
                    JOIN public.payroll_period p
                      ON p.payroll_period_id = r.payroll_period_id
                    WHERE r.status <> 'CANCELLED'
                    ORDER BY p.payroll_year DESC, p.payroll_month DESC, r.run_number DESC
                    LIMIT 1
                )
                SELECT
                    (SELECT COUNT(*) FROM public.payroll_period WHERE status <> 'CLOSED') AS open_periods,
                    (SELECT COUNT(*) FROM public.payroll_run WHERE status NOT IN ('PAID','CANCELLED')) AS active_runs,
                    COUNT(pe.payroll_employee_id) AS employee_count,
                    COALESCE(SUM(pe.total_earnings), 0) AS total_earnings,
                    COALESCE(SUM(pe.total_deductions), 0) AS total_deductions,
                    COALESCE(SUM(pe.net_salary), 0) AS net_pay,
                    COALESCE(MAX(l.payroll_year::text || '-' || LPAD(l.payroll_month::text, 2, '0')), '-') AS latest_period
                FROM latest l
                LEFT JOIN public.payroll_employee pe
                  ON pe.payroll_run_id = l.payroll_run_id
                 AND pe.payroll_status = 'INCLUDED'
                """).getSingleResult();

        return new DashboardStats(
                longValue(row[0]),
                longValue(row[1]),
                longValue(row[2]),
                decimal(row[3]),
                decimal(row[4]),
                decimal(row[5]),
                text(row[6]));
    }

    public PayrollReconciliationSummary getLatestReconciliation() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                WITH latest_period AS (
                    SELECT p.payroll_period_id, p.payroll_year, p.payroll_month
                    FROM public.payroll_period p
                    WHERE EXISTS (
                        SELECT 1
                        FROM public.payroll_run r
                        WHERE r.payroll_period_id = p.payroll_period_id
                          AND r.run_type = 'REGULAR'
                          AND r.status <> 'CANCELLED'
                    )
                    ORDER BY p.payroll_year DESC, p.payroll_month DESC, p.payroll_period_id DESC
                    LIMIT 1
                ), regular_run AS (
                    SELECT r.payroll_run_id
                    FROM public.payroll_run r
                    JOIN latest_period lp
                      ON lp.payroll_period_id = r.payroll_period_id
                    WHERE r.run_type = 'REGULAR'
                      AND r.status <> 'CANCELLED'
                    ORDER BY r.run_number DESC, r.payroll_run_id DESC
                    LIMIT 1
                ), regular_total AS (
                    SELECT COALESCE(SUM(pe.net_salary), 0) AS payroll_net_pay
                    FROM regular_run rr
                    LEFT JOIN public.payroll_employee pe
                      ON pe.payroll_run_id = rr.payroll_run_id
                     AND pe.payroll_status = 'INCLUDED'
                ), payment_total AS (
                    SELECT
                        COALESCE(SUM(CASE
                            WHEN batch.status = 'PAID'
                             AND batch.installment_type = 'FIRST_INSTALLMENT'
                            THEN detail.payment_amount ELSE 0 END), 0) AS first_paid,
                        COALESCE(SUM(CASE
                            WHEN batch.status = 'PAID'
                             AND batch.installment_type = 'FINAL_SETTLEMENT'
                            THEN detail.payment_amount ELSE 0 END), 0) AS final_paid,
                        COALESCE(SUM(CASE
                            WHEN batch.status = 'PAID'
                             AND batch.installment_type = 'ADJUSTMENT_SETTLEMENT'
                             AND COALESCE(pe.net_salary, 0) > 0
                            THEN detail.carry_forward_amount ELSE 0 END), 0) AS adjustment_carry,
                        COALESCE(SUM(CASE
                            WHEN batch.status = 'PAID'
                             AND batch.installment_type = 'ADJUSTMENT_SETTLEMENT'
                             AND COALESCE(pe.net_salary, 0) < 0
                            THEN detail.carry_forward_amount
                            WHEN batch.status = 'PAID'
                             AND batch.installment_type = 'FINAL_SETTLEMENT'
                            THEN detail.carry_forward_amount
                            ELSE 0 END), 0) AS recovery_carry,
                        COALESCE(SUM(CASE
                            WHEN batch.status = 'PAID'
                            THEN detail.payment_amount ELSE 0 END), 0) AS cash_paid,
                        COALESCE(SUM(CASE
                            WHEN batch.status = 'PAID'
                             AND batch.installment_type = 'FINAL_SETTLEMENT'
                            THEN detail.carry_forward_amount ELSE 0 END), 0) AS regular_recovery
                    FROM latest_period lp
                    LEFT JOIN public.payroll_payment_batch batch
                      ON batch.payroll_period_id = lp.payroll_period_id
                    LEFT JOIN public.payroll_employee_payment detail
                      ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                    LEFT JOIN public.payroll_employee pe
                      ON pe.payroll_employee_id = detail.payroll_employee_id
                )
                SELECT COALESCE(lp.payroll_year::text || '-' || LPAD(lp.payroll_month::text, 2, '0'), '-'),
                       COALESCE(rt.payroll_net_pay, 0),
                       COALESCE(pt.first_paid, 0),
                       COALESCE(pt.final_paid, 0),
                       COALESCE(pt.adjustment_carry, 0),
                       COALESCE(pt.recovery_carry, 0),
                       COALESCE(pt.cash_paid, 0),
                       COALESCE(rt.payroll_net_pay, 0)
                           - COALESCE(pt.first_paid, 0)
                           - COALESCE(pt.final_paid, 0)
                           + COALESCE(pt.regular_recovery, 0) AS difference
                FROM latest_period lp
                CROSS JOIN regular_total rt
                CROSS JOIN payment_total pt
                """).getResultStream().findFirst().orElse(null);

        if (row == null) {
            return new PayrollReconciliationSummary(
                    "-", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return new PayrollReconciliationSummary(
                text(row[0]),
                decimal(row[1]),
                decimal(row[2]),
                decimal(row[3]),
                decimal(row[4]),
                decimal(row[5]),
                decimal(row[6]),
                decimal(row[7]));
    }

    public List<RunRow> findByPeriodId(Long periodId) {
        if (periodId == null) {
            return List.of();
        }
        return rows("""
                SELECT r.payroll_run_id,
                       r.payroll_period_id,
                       p.payroll_year,
                       p.payroll_month,
                       r.run_number,
                       r.run_type,
                       r.status,
                       r.correction_mode,
                       COUNT(pe.payroll_employee_id) AS employee_count,
                       COALESCE(SUM(pe.total_earnings), 0) AS total_earnings,
                       COALESCE(SUM(pe.total_deductions), 0) AS total_deductions,
                       COALESCE(SUM(pe.net_salary), 0) AS net_pay,
                       COALESCE((
                           SELECT SUM(detail.payment_amount)
                           FROM public.payroll_payment_batch batch
                           JOIN public.payroll_employee_payment detail
                             ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                           WHERE batch.payroll_period_id = r.payroll_period_id
                             AND batch.installment_type = 'FIRST_INSTALLMENT'
                             AND batch.status = 'PAID'
                       ), 0) AS first_paid,
                       COALESCE((
                           SELECT SUM(detail.payment_amount)
                           FROM public.payroll_payment_batch batch
                           JOIN public.payroll_employee_payment detail
                             ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                           WHERE batch.payroll_run_id = r.payroll_run_id
                             AND batch.installment_type = 'FINAL_SETTLEMENT'
                             AND batch.status = 'PAID'
                       ), 0) AS final_paid,
                       r.notes
                FROM public.payroll_run r
                JOIN public.payroll_period p
                  ON p.payroll_period_id = r.payroll_period_id
                LEFT JOIN public.payroll_employee pe
                  ON pe.payroll_run_id = r.payroll_run_id
                 AND pe.payroll_status = 'INCLUDED'
                WHERE r.payroll_period_id = :periodId
                GROUP BY r.payroll_run_id, p.payroll_year, p.payroll_month
                ORDER BY r.run_number DESC
                """, "periodId", periodId);
    }

    public List<RunRow> findRecent(int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> result = entityManager.createNativeQuery("""
                SELECT r.payroll_run_id,
                       r.payroll_period_id,
                       p.payroll_year,
                       p.payroll_month,
                       r.run_number,
                       r.run_type,
                       r.status,
                       r.correction_mode,
                       COUNT(pe.payroll_employee_id) AS employee_count,
                       COALESCE(SUM(pe.total_earnings), 0) AS total_earnings,
                       COALESCE(SUM(pe.total_deductions), 0) AS total_deductions,
                       COALESCE(SUM(pe.net_salary), 0) AS net_pay,
                       COALESCE((
                           SELECT SUM(detail.payment_amount)
                           FROM public.payroll_payment_batch batch
                           JOIN public.payroll_employee_payment detail
                             ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                           WHERE batch.payroll_period_id = r.payroll_period_id
                             AND batch.installment_type = 'FIRST_INSTALLMENT'
                             AND batch.status = 'PAID'
                       ), 0) AS first_paid,
                       COALESCE((
                           SELECT SUM(detail.payment_amount)
                           FROM public.payroll_payment_batch batch
                           JOIN public.payroll_employee_payment detail
                             ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                           WHERE batch.payroll_run_id = r.payroll_run_id
                             AND batch.installment_type = 'FINAL_SETTLEMENT'
                             AND batch.status = 'PAID'
                       ), 0) AS final_paid,
                       r.notes
                FROM public.payroll_run r
                JOIN public.payroll_period p
                  ON p.payroll_period_id = r.payroll_period_id
                LEFT JOIN public.payroll_employee pe
                  ON pe.payroll_run_id = r.payroll_run_id
                 AND pe.payroll_status = 'INCLUDED'
                GROUP BY r.payroll_run_id, p.payroll_year, p.payroll_month
                ORDER BY p.payroll_year DESC, p.payroll_month DESC, r.run_number DESC
                """)
                .setMaxResults(Math.max(1, limit))
                .getResultList();
        return result.stream().map(PayrollRunDataRepository::mapRun).toList();
    }

    public Optional<RunRow> findRow(Long runId) {
        if (runId == null) {
            return Optional.empty();
        }
        List<RunRow> result = rows("""
                SELECT r.payroll_run_id,
                       r.payroll_period_id,
                       p.payroll_year,
                       p.payroll_month,
                       r.run_number,
                       r.run_type,
                       r.status,
                       r.correction_mode,
                       COUNT(pe.payroll_employee_id) AS employee_count,
                       COALESCE(SUM(pe.total_earnings), 0) AS total_earnings,
                       COALESCE(SUM(pe.total_deductions), 0) AS total_deductions,
                       COALESCE(SUM(pe.net_salary), 0) AS net_pay,
                       COALESCE((
                           SELECT SUM(detail.payment_amount)
                           FROM public.payroll_payment_batch batch
                           JOIN public.payroll_employee_payment detail
                             ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                           WHERE batch.payroll_period_id = r.payroll_period_id
                             AND batch.installment_type = 'FIRST_INSTALLMENT'
                             AND batch.status = 'PAID'
                       ), 0) AS first_paid,
                       COALESCE((
                           SELECT SUM(detail.payment_amount)
                           FROM public.payroll_payment_batch batch
                           JOIN public.payroll_employee_payment detail
                             ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                           WHERE batch.payroll_run_id = r.payroll_run_id
                             AND batch.installment_type = 'FINAL_SETTLEMENT'
                             AND batch.status = 'PAID'
                       ), 0) AS final_paid,
                       r.notes
                FROM public.payroll_run r
                JOIN public.payroll_period p
                  ON p.payroll_period_id = r.payroll_period_id
                LEFT JOIN public.payroll_employee pe
                  ON pe.payroll_run_id = r.payroll_run_id
                 AND pe.payroll_status = 'INCLUDED'
                WHERE r.payroll_run_id = :runId
                GROUP BY r.payroll_run_id, p.payroll_year, p.payroll_month
                """, "runId", runId);
        return result.stream().findFirst();
    }

    public boolean isCorrectionMode(Long runId) {
        Object value = entityManager.createNativeQuery("""
                SELECT correction_mode
                FROM public.payroll_run
                WHERE payroll_run_id = :runId
                """)
                .setParameter("runId", runId)
                .getResultStream()
                .findFirst()
                .orElse(Boolean.FALSE);
        return Boolean.TRUE.equals(value);
    }

    @SuppressWarnings("unchecked")
    private List<RunRow> rows(String sql, String parameterName, Object parameterValue) {
        List<Object[]> result = entityManager.createNativeQuery(sql)
                .setParameter(parameterName, parameterValue)
                .getResultList();
        return result.stream().map(PayrollRunDataRepository::mapRun).toList();
    }

    private static RunRow mapRun(Object[] row) {
        return new RunRow(
                longValue(row[0]),
                longValue(row[1]),
                intValue(row[2]),
                intValue(row[3]),
                intValue(row[4]),
                text(row[5]),
                text(row[6]),
                booleanValue(row[7]),
                longValue(row[8]),
                decimal(row[9]),
                decimal(row[10]),
                decimal(row[11]),
                decimal(row[12]),
                decimal(row[13]),
                text(row[14]));
    }

    private static Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static int intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean b ? b : value != null && Boolean.parseBoolean(value.toString());
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
