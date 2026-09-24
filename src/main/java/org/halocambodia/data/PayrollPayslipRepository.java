package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Immutable PAID-payment read model for first-installment, final-settlement,
 * and adjustment-settlement payslip generation.
 */
@Repository
public class PayrollPayslipRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<PayrollPayslipRow> findPaidBatchRows(Long batchId, Long employeePaymentId) {
        String employeeFilter = employeePaymentId == null
                ? ""
                : " AND detail.payroll_employee_payment_id = :employeePaymentId";

        String sql = """
                SELECT detail.payroll_employee_payment_id,
                       detail.payroll_employee_id,
                       detail.emp_id,
                       detail.insurance_no_snapshot,
                       detail.employee_name_en_snapshot,
                       detail.employee_name_kh_snapshot,
                       detail.bank_name_snapshot,
                       detail.bank_account_snapshot,
                       period.payroll_year,
                       period.payroll_month,
                       period.period_start,
                       period.period_end,
                       batch.payment_date,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT' THEN 0
                           ELSE payroll_run.run_number
                       END AS run_number,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT' THEN 'PAYMENT'
                           ELSE payroll_run.run_type
                       END AS run_type,
                       batch.installment_type,
                       detail.currency_code,
                       detail.basic_salary,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN detail.payment_amount
                           ELSE payroll_employee.total_earnings
                       END AS total_earnings,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN 0
                           ELSE payroll_employee.total_deductions
                       END AS total_deductions,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN 0
                           ELSE payroll_employee.salary_tax
                       END AS salary_tax,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN 0
                           ELSE payroll_employee.employee_contribution
                       END AS employee_contribution,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN 0
                           ELSE payroll_employee.employer_contribution
                       END AS employer_contribution,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN detail.payment_amount
                           ELSE payroll_employee.net_salary
                       END AS net_salary,
                       detail.previous_paid_amount,
                       detail.payment_amount,
                       detail.carry_forward_amount,
                       batch.payment_method,
                       batch.payment_reference,
                       batch.bank_reference,
                       batch.paid_at,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN 'FIRST_INSTALLMENT'
                           ELSE component.component_code
                       END AS component_code,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN COALESCE(detail.remarks, 'First installment payment')
                           ELSE component.component_name_en
                       END AS component_name_en,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN NULL
                           ELSE component.component_name_kh
                       END AS component_name_kh,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN 'PAYMENT'
                           ELSE component.component_type
                       END AS component_type,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN detail.remarks
                           ELSE item.description
                       END AS description,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN 'PAYMENT'
                           ELSE item.source_type
                       END AS source_type,
                       CASE
                           WHEN batch.installment_type = 'FIRST_INSTALLMENT'
                               THEN detail.payment_amount
                           ELSE item.payroll_amount
                       END AS payroll_amount
                FROM public.payroll_payment_batch batch
                JOIN public.payroll_period period
                  ON period.payroll_period_id = batch.payroll_period_id
                JOIN public.payroll_employee_payment detail
                  ON detail.payroll_payment_batch_id = batch.payroll_payment_batch_id
                LEFT JOIN public.payroll_run payroll_run
                  ON payroll_run.payroll_run_id = batch.payroll_run_id
                LEFT JOIN public.payroll_employee payroll_employee
                  ON payroll_employee.payroll_employee_id = detail.payroll_employee_id
                 AND (payroll_run.payroll_run_id IS NULL
                      OR payroll_employee.payroll_run_id = payroll_run.payroll_run_id)
                LEFT JOIN public.payroll_employee_item item
                  ON item.payroll_employee_id = payroll_employee.payroll_employee_id
                LEFT JOIN public.payroll_component component
                  ON component.payroll_component_id = item.payroll_component_id
                WHERE batch.payroll_payment_batch_id = :batchId
                  AND batch.status = 'PAID'
                  AND batch.installment_type IN (
                      'FIRST_INSTALLMENT',
                      'FINAL_SETTLEMENT',
                      'ADJUSTMENT_SETTLEMENT'
                  )
                  AND (
                      batch.installment_type = 'FIRST_INSTALLMENT'
                      OR payroll_run.status = 'PAID'
                  )
                %s
                ORDER BY detail.insurance_no_snapshot,
                         detail.payroll_employee_payment_id,
                         COALESCE(component.sort_order, 999999),
                         item.payroll_employee_item_id
                """.formatted(employeeFilter);

        jakarta.persistence.Query query = entityManager.createNativeQuery(sql)
                .setParameter("batchId", batchId);
        if (employeePaymentId != null) {
            query.setParameter("employeePaymentId", employeePaymentId);
        }

        List<?> rows = query.getResultList();
        return rows.stream()
                .map(value -> (Object[]) value)
                .map(PayrollPayslipRepository::map)
                .toList();
    }

    private static PayrollPayslipRow map(Object[] row) {
        return new PayrollPayslipRow(
                longObject(row[0]),
                longObject(row[1]),
                longObject(row[2]),
                integer(row[3]),
                text(row[4]),
                text(row[5]),
                text(row[6]),
                text(row[7]),
                integer(row[8]),
                integer(row[9]),
                localDate(row[10]),
                localDate(row[11]),
                localDate(row[12]),
                integer(row[13]),
                text(row[14]),
                text(row[15]),
                text(row[16]),
                decimal(row[17]),
                decimal(row[18]),
                decimal(row[19]),
                decimal(row[20]),
                decimal(row[21]),
                decimal(row[22]),
                decimal(row[23]),
                decimal(row[24]),
                decimal(row[25]),
                decimal(row[26]),
                text(row[27]),
                text(row[28]),
                text(row[29]),
                offsetDateTime(row[30]),
                text(row[31]),
                text(row[32]),
                text(row[33]),
                text(row[34]),
                text(row[35]),
                text(row[36]),
                decimal(row[37]));
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
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private static OffsetDateTime offsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(value.toString());
    }
}
