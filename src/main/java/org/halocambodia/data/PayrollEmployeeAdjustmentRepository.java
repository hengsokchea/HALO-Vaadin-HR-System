package org.halocambodia.data;

import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanRow;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanRepaymentRow;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.RecurringRow;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Data access for employee recurring payroll configuration and loan/recovery balances. */
@Repository
public class PayrollEmployeeAdjustmentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public int ensureSystemComponents(Long userId) {
        return entityManager.createNativeQuery("""
                INSERT INTO public.payroll_component
                    (component_code, component_name_en, component_name_kh, component_type,
                     calculation_method, taxable, subject_to_nssf, subject_to_seniority,
                     active, sort_order, created_by, updated_by)
                VALUES
                    ('FIXED_INCOME', 'Fixed Income', 'ចំណូលថេរ', 'EARNING',
                     'FORMULA', TRUE, TRUE, FALSE, TRUE, 300, :userId, :userId),
                    ('OTHER_DEDUCTION', 'Other Deduction', 'ការកាត់ប្រាក់ផ្សេងៗ', 'DEDUCTION',
                     'FORMULA', FALSE, FALSE, FALSE, TRUE, 700, :userId, :userId),
                    ('COMPANY_LOAN_REPAYMENT', 'Employee Recovery / Loan Repayment',
                     'ការកាត់សងបំណុល / ប្រាក់កម្ចីបុគ្គលិក', 'DEDUCTION',
                     'FORMULA', FALSE, FALSE, FALSE, TRUE, 710, :userId, :userId)
                ON CONFLICT (component_code) DO NOTHING
                """)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<RecurringRow> findRecurring(String search) {
        String q = normalizeSearch(search);
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT r.payroll_employee_recurring_component_id,
                       r.emp_id,
                       e.insurance_no,
                       e.name_en,
                       e.name_kh,
                       c.component_code,
                       c.component_name_en,
                       c.component_type,
                       r.amount,
                       r.recurrence_type,
                       r.effective_from,
                       r.effective_to,
                       r.active,
                       r.description,
                       r.remarks,
                       (SELECT COUNT(*)
                          FROM public.payroll_employee_recurring_application a
                         WHERE a.payroll_employee_recurring_component_id = r.payroll_employee_recurring_component_id
                           AND a.status = 'SETTLED') AS settled_count
                FROM public.payroll_employee_recurring_component r
                JOIN public.emp_master e ON e.emp_id = r.emp_id
                JOIN public.payroll_component c
                  ON c.payroll_component_id = r.payroll_component_id
                WHERE (:q = ''
                       OR LOWER(COALESCE(e.name_en, '')) LIKE :likeQ
                       OR LOWER(COALESCE(e.name_kh, '')) LIKE :likeQ
                       OR CAST(e.insurance_no AS TEXT) LIKE :likeQ
                       OR LOWER(COALESCE(c.component_code, '')) LIKE :likeQ)
                ORDER BY r.active DESC, e.insurance_no, c.sort_order,
                         r.effective_from DESC, r.payroll_employee_recurring_component_id DESC
                """)
                .setParameter("q", q)
                .setParameter("likeQ", "%" + q + "%")
                .getResultList();
        return rows.stream().map(this::mapRecurring).toList();
    }

    @SuppressWarnings("unchecked")
    public List<LoanRow> findLoans(String search) {
        String q = normalizeSearch(search);
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT l.payroll_employee_loan_id,
                       l.emp_id,
                       e.insurance_no,
                       e.name_en,
                       e.name_kh,
                       l.recovery_type,
                       l.loan_reference,
                       l.principal_amount,
                       l.monthly_installment,
                       l.outstanding_balance,
                       l.principal_amount - l.outstanding_balance AS repaid_amount,
                       l.start_date,
                       l.end_date,
                       l.status,
                       l.remarks,
                       (SELECT COUNT(*)
                          FROM public.payroll_employee_loan_repayment rp
                         WHERE rp.payroll_employee_loan_id = l.payroll_employee_loan_id
                           AND rp.status = 'SETTLED') AS settled_count
                FROM public.payroll_employee_loan l
                JOIN public.emp_master e ON e.emp_id = l.emp_id
                WHERE (:q = ''
                       OR LOWER(COALESCE(e.name_en, '')) LIKE :likeQ
                       OR LOWER(COALESCE(e.name_kh, '')) LIKE :likeQ
                       OR CAST(e.insurance_no AS TEXT) LIKE :likeQ
                       OR LOWER(COALESCE(l.recovery_type, '')) LIKE :likeQ
                       OR LOWER(COALESCE(l.loan_reference, '')) LIKE :likeQ
                       OR LOWER(COALESCE(l.status, '')) LIKE :likeQ)
                ORDER BY CASE l.status
                           WHEN 'ACTIVE' THEN 1 WHEN 'PAUSED' THEN 2
                           WHEN 'SETTLED' THEN 3 ELSE 4 END,
                         e.insurance_no, l.start_date DESC, l.payroll_employee_loan_id DESC
                """)
                .setParameter("q", q)
                .setParameter("likeQ", "%" + q + "%")
                .getResultList();
        return rows.stream().map(this::mapLoan).toList();
    }

    @SuppressWarnings("unchecked")
    public List<LoanRepaymentRow> findLoanRepayments(Long loanId) {
        if (loanId == null) return List.of();
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT repayment.payroll_employee_loan_repayment_id,
                       repayment.payroll_employee_loan_id,
                       repayment.payroll_run_id,
                       period.payroll_year,
                       period.payroll_month,
                       run.run_number,
                       repayment.scheduled_amount,
                       repayment.deducted_amount,
                       repayment.balance_before,
                       repayment.balance_after,
                       repayment.status,
                       repayment.created_at,
                       repayment.settled_at
                FROM public.payroll_employee_loan_repayment repayment
                JOIN public.payroll_run run
                  ON run.payroll_run_id = repayment.payroll_run_id
                JOIN public.payroll_period period
                  ON period.payroll_period_id = run.payroll_period_id
                WHERE repayment.payroll_employee_loan_id = :loanId
                ORDER BY period.payroll_year DESC, period.payroll_month DESC,
                         run.run_number DESC, repayment.payroll_employee_loan_repayment_id DESC
                """)
                .setParameter("loanId", loanId)
                .getResultList();
        return rows.stream().map(row -> new LoanRepaymentRow(
                longValue(row[0]), longValue(row[1]), longValue(row[2]),
                intValue(row[3]), intValue(row[4]), intValue(row[5]),
                toBigDecimal(row[6]), toBigDecimal(row[7]),
                toBigDecimal(row[8]), toBigDecimal(row[9]),
                stringValue(row[10]), toOffsetDateTime(row[11]),
                toOffsetDateTime(row[12]))).toList();
    }

    public Optional<Long> findComponentId(String componentCode) {
        if (componentCode == null) {
            return Optional.empty();
        }
        return entityManager.createNativeQuery("""
                SELECT payroll_component_id
                FROM public.payroll_component
                WHERE component_code = :code
                  AND active = TRUE
                """)
                .setParameter("code", componentCode)
                .getResultStream()
                .findFirst()
                .map(v -> ((Number) v).longValue());
    }

    public Long insertRecurring(
            Long employeeId,
            Long componentId,
            BigDecimal amount,
            String recurrenceType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active,
            String description,
            String remarks,
            Long userId) {
        Object id = entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee_recurring_component
                    (emp_id, payroll_component_id, amount, currency_code,
                     recurrence_type, effective_from, effective_to, active,
                     description, remarks, created_by, updated_by)
                VALUES
                    (:employeeId, :componentId, :amount, 'USD',
                     :recurrenceType, :effectiveFrom, :effectiveTo, :active,
                     :description, :remarks, :userId, :userId)
                RETURNING payroll_employee_recurring_component_id
                """)
                .setParameter("employeeId", employeeId)
                .setParameter("componentId", componentId)
                .setParameter("amount", amount)
                .setParameter("recurrenceType", recurrenceType)
                .setParameter("effectiveFrom", effectiveFrom)
                .setParameter("effectiveTo", effectiveTo)
                .setParameter("active", active)
                .setParameter("description", description)
                .setParameter("remarks", remarks)
                .setParameter("userId", userId)
                .getSingleResult();
        return ((Number) id).longValue();
    }

    public int updateRecurring(
            Long id,
            Long employeeId,
            Long componentId,
            BigDecimal amount,
            String recurrenceType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active,
            String description,
            String remarks,
            Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee_recurring_component
                   SET emp_id = :employeeId,
                       payroll_component_id = :componentId,
                       amount = :amount,
                       recurrence_type = :recurrenceType,
                       effective_from = :effectiveFrom,
                       effective_to = :effectiveTo,
                       active = :active,
                       description = :description,
                       remarks = :remarks,
                       updated_at = CURRENT_TIMESTAMP,
                       updated_by = :userId,
                       version = version + 1
                 WHERE payroll_employee_recurring_component_id = :id
                """)
                .setParameter("id", id)
                .setParameter("employeeId", employeeId)
                .setParameter("componentId", componentId)
                .setParameter("amount", amount)
                .setParameter("recurrenceType", recurrenceType)
                .setParameter("effectiveFrom", effectiveFrom)
                .setParameter("effectiveTo", effectiveTo)
                .setParameter("active", active)
                .setParameter("description", description)
                .setParameter("remarks", remarks)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public long countSettledApplications(Long recurringId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee_recurring_application
                WHERE payroll_employee_recurring_component_id = :id
                  AND status = 'SETTLED'
                """)
                .setParameter("id", recurringId)
                .getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    public int deactivateRecurring(Long id, Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee_recurring_component
                   SET active = FALSE,
                       updated_at = CURRENT_TIMESTAMP,
                       updated_by = :userId,
                       version = version + 1
                 WHERE payroll_employee_recurring_component_id = :id
                """)
                .setParameter("id", id)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    public int deleteRecurring(Long id) {
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_recurring_component
                WHERE payroll_employee_recurring_component_id = :id
                  AND NOT EXISTS (
                      SELECT 1
                      FROM public.payroll_employee_recurring_application a
                      WHERE a.payroll_employee_recurring_component_id = :id
                  )
                """)
                .setParameter("id", id)
                .executeUpdate();
    }

    public boolean loanReferenceExists(String reference, Long excludingId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee_loan
                WHERE LOWER(loan_reference) = LOWER(:reference)
                  AND (CAST(:excludingId AS BIGINT) IS NULL
                       OR payroll_employee_loan_id <> :excludingId)
                """)
                .setParameter("reference", reference)
                .setParameter("excludingId", excludingId)
                .getSingleResult();
        return result != null && ((Number) result).longValue() > 0;
    }

    public long countReservedLoanRepayments(Long loanId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee_loan_repayment
                WHERE payroll_employee_loan_id = :loanId
                  AND status = 'RESERVED'
                """)
                .setParameter("loanId", loanId)
                .getSingleResult();
        return result == null ? 0L : ((Number) result).longValue();
    }

    public BigDecimal settledLoanAmount(Long loanId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(deducted_amount), 0)
                FROM public.payroll_employee_loan_repayment
                WHERE payroll_employee_loan_id = :loanId
                  AND status = 'SETTLED'
                """)
                .setParameter("loanId", loanId)
                .getSingleResult();
        return toBigDecimal(result);
    }

    public Long insertLoan(
            Long employeeId,
            String recoveryType,
            String reference,
            BigDecimal principal,
            BigDecimal monthlyInstallment,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String remarks,
            Long userId) {
        Object id = entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee_loan
                    (emp_id, recovery_type, loan_reference, principal_amount, monthly_installment,
                     outstanding_balance, start_date, end_date, status, remarks,
                     created_by, updated_by)
                VALUES
                    (:employeeId, :recoveryType, :reference, :principal, :monthlyInstallment,
                     :principal, :startDate, :endDate, :status, :remarks,
                     :userId, :userId)
                RETURNING payroll_employee_loan_id
                """)
                .setParameter("employeeId", employeeId)
                .setParameter("recoveryType", recoveryType)
                .setParameter("reference", reference)
                .setParameter("principal", principal)
                .setParameter("monthlyInstallment", monthlyInstallment)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("status", status)
                .setParameter("remarks", remarks)
                .setParameter("userId", userId)
                .getSingleResult();
        return ((Number) id).longValue();
    }

    public int updateLoan(
            Long id,
            Long employeeId,
            String recoveryType,
            String reference,
            BigDecimal principal,
            BigDecimal monthlyInstallment,
            BigDecimal outstanding,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String remarks,
            Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee_loan
                   SET emp_id = :employeeId,
                       recovery_type = :recoveryType,
                       loan_reference = :reference,
                       principal_amount = :principal,
                       monthly_installment = :monthlyInstallment,
                       outstanding_balance = :outstanding,
                       start_date = :startDate,
                       end_date = :endDate,
                       status = :status,
                       remarks = :remarks,
                       updated_at = CURRENT_TIMESTAMP,
                       updated_by = :userId,
                       version = version + 1
                 WHERE payroll_employee_loan_id = :id
                """)
                .setParameter("id", id)
                .setParameter("employeeId", employeeId)
                .setParameter("recoveryType", recoveryType)
                .setParameter("reference", reference)
                .setParameter("principal", principal)
                .setParameter("monthlyInstallment", monthlyInstallment)
                .setParameter("outstanding", outstanding)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("status", status)
                .setParameter("remarks", remarks)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private RecurringRow mapRecurring(Object[] row) {
        return new RecurringRow(
                longValue(row[0]), longValue(row[1]), intValue(row[2]),
                stringValue(row[3]), stringValue(row[4]), stringValue(row[5]),
                stringValue(row[6]), stringValue(row[7]), toBigDecimal(row[8]),
                stringValue(row[9]), toLocalDate(row[10]), toLocalDate(row[11]),
                booleanValue(row[12]), stringValue(row[13]), stringValue(row[14]),
                longValue(row[15]));
    }

    private LoanRow mapLoan(Object[] row) {
        return new LoanRow(
                longValue(row[0]), longValue(row[1]), intValue(row[2]),
                stringValue(row[3]), stringValue(row[4]), stringValue(row[5]),
                stringValue(row[6]), toBigDecimal(row[7]), toBigDecimal(row[8]),
                toBigDecimal(row[9]), toBigDecimal(row[10]), toLocalDate(row[11]),
                toLocalDate(row[12]), stringValue(row[13]), stringValue(row[14]),
                longValue(row[15]));
    }

    private static String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Integer intValue(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime;
        if (value instanceof java.time.ZonedDateTime zonedDateTime) return zonedDateTime.toOffsetDateTime();
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
        return OffsetDateTime.parse(value.toString());
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}
