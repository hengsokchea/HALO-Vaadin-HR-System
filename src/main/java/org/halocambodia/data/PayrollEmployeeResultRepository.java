package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Secure read model for the employee's own completed payroll results.
 * The lookup is performed against the insurance number stored on the
 * authenticated application user; callers cannot supply an arbitrary employee id.
 */
@Repository
public class PayrollEmployeeResultRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<PayrollEmployeeResultRow> findPaidResults(String insuranceNo) {
        String value = insuranceNo == null ? "" : insuranceNo.trim();
        if (value.isEmpty()) {
            return List.of();
        }

        List<?> rows = entityManager.createNativeQuery("""
                SELECT detail.payroll_employee_payment_id,
                       detail.payroll_payment_batch_id,
                       detail.payroll_employee_id,
                       period.payroll_year,
                       period.payroll_month,
                       period.period_start,
                       period.period_end,
                       batch.payment_date,
                       batch.installment_type,
                       detail.currency_code,
                       detail.basic_salary,
                       detail.full_net_amount,
                       detail.previous_paid_amount,
                       detail.payment_amount,
                       detail.carry_forward_amount
                FROM public.payroll_employee_payment detail
                JOIN public.payroll_payment_batch batch
                  ON batch.payroll_payment_batch_id = detail.payroll_payment_batch_id
                JOIN public.payroll_period period
                  ON period.payroll_period_id = batch.payroll_period_id
                WHERE CAST(detail.insurance_no_snapshot AS TEXT) = :insuranceNo
                  AND batch.status = 'PAID'
                  AND batch.installment_type IN (
                      'FIRST_INSTALLMENT',
                      'FINAL_SETTLEMENT',
                      'ADJUSTMENT_SETTLEMENT'
                  )
                  AND (
                      batch.installment_type = 'FIRST_INSTALLMENT'
                      OR EXISTS (
                          SELECT 1
                          FROM public.payroll_run run
                          WHERE run.payroll_run_id = batch.payroll_run_id
                            AND run.status = 'PAID'
                      )
                  )
                ORDER BY batch.payment_date DESC NULLS LAST,
                         batch.payroll_payment_batch_id DESC,
                         detail.payroll_employee_payment_id DESC
                """)
                .setParameter("insuranceNo", value)
                .getResultList();

        return rows.stream()
                .map(row -> (Object[]) row)
                .map(PayrollEmployeeResultRepository::map)
                .toList();
    }

    private static PayrollEmployeeResultRow map(Object[] row) {
        return new PayrollEmployeeResultRow(
                longObject(row[0]),
                longObject(row[1]),
                longObject(row[2]),
                integer(row[3]),
                integer(row[4]),
                localDate(row[5]),
                localDate(row[6]),
                localDate(row[7]),
                text(row[8]),
                text(row[9]),
                decimal(row[10]),
                decimal(row[11]),
                decimal(row[12]),
                decimal(row[13]),
                decimal(row[14]));
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long longObject(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer integer(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
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
        return LocalDate.parse(value.toString());
    }
}
