package org.halocambodia.data;

import static org.halocambodia.data.PayrollModels.ItemRow;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Focused data-access operations for payroll employee items.
 *
 * Business rules (manual vs system-calculated items, permissions, correction
 * locking and recalculation) remain in PayrollService. This repository owns only
 * item persistence/read SQL.
 */
@Repository
public class PayrollEmployeeItemDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ItemRow> findItems(Long payrollEmployeeId) {
        if (payrollEmployeeId == null) {
            return List.of();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT i.payroll_employee_item_id,
                       i.payroll_employee_id,
                       i.payroll_component_id,
                       c.component_code,
                       c.component_name_en,
                       c.component_type,
                       i.description,
                       i.quantity,
                       i.rate,
                       i.amount,
                       i.currency_code,
                       i.exchange_rate,
                       i.payroll_amount,
                       i.source_type,
                       i.remarks
                FROM public.payroll_employee_item i
                JOIN public.payroll_component c
                  ON c.payroll_component_id = i.payroll_component_id
                WHERE i.payroll_employee_id = :payrollEmployeeId
                ORDER BY c.sort_order, i.payroll_employee_item_id
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getResultList();
        return rows.stream().map(PayrollEmployeeItemDataRepository::mapItem).toList();
    }

    public Optional<String> findComponentCode(Long componentId) {
        if (componentId == null) {
            return Optional.empty();
        }
        return entityManager.createNativeQuery("""
                SELECT component_code
                FROM public.payroll_component
                WHERE payroll_component_id = :componentId
                """)
                .setParameter("componentId", componentId)
                .getResultStream()
                .findFirst()
                .map(Object::toString);
    }

    public Optional<String> findExistingItemComponentCode(
            Long itemId, Long payrollEmployeeId) {
        if (itemId == null || payrollEmployeeId == null) {
            return Optional.empty();
        }
        return entityManager.createNativeQuery("""
                SELECT c.component_code
                FROM public.payroll_employee_item i
                JOIN public.payroll_component c
                  ON c.payroll_component_id = i.payroll_component_id
                WHERE i.payroll_employee_item_id = :itemId
                  AND i.payroll_employee_id = :payrollEmployeeId
                """)
                .setParameter("itemId", itemId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getResultStream()
                .findFirst()
                .map(Object::toString);
    }

    public boolean isRecurringOrLoanItem(Long itemId, Long payrollEmployeeId) {
        if (itemId == null || payrollEmployeeId == null) {
            return false;
        }
        Object count = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee_item i
                WHERE i.payroll_employee_item_id = :itemId
                  AND i.payroll_employee_id = :payrollEmployeeId
                  AND i.source_type IN ('RECURRING','LOAN')
                """)
                .setParameter("itemId", itemId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .getSingleResult();
        return count instanceof Number number && number.longValue() > 0;
    }

    public boolean isAutomaticPolicyItem(Long itemId, Long payrollEmployeeId) {
        if (itemId == null || payrollEmployeeId == null) {
            return false;
        }
        Object count = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM public.payroll_employee_item i
                WHERE i.payroll_employee_item_id = :itemId
                  AND i.payroll_employee_id = :payrollEmployeeId
                  AND i.source_type IN ('ATTENDANCE','LEAVE','OVERTIME')
                  AND COALESCE(i.remarks, '') ~ '^Rule [A-Z0-9_]+;'
                """)
                .setParameter("itemId", itemId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
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

    public Optional<Long> findRunIdByItemId(Long itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return entityManager.createNativeQuery("""
                SELECT pe.payroll_run_id
                FROM public.payroll_employee_item i
                JOIN public.payroll_employee pe
                  ON pe.payroll_employee_id = i.payroll_employee_id
                WHERE i.payroll_employee_item_id = :itemId
                """)
                .setParameter("itemId", itemId)
                .getResultStream()
                .findFirst()
                .map(value -> ((Number) value).longValue());
    }

    public Long insert(
            Long payrollEmployeeId,
            Long componentId,
            String description,
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal amount,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal payrollAmount,
            String sourceType,
            String remarks,
            Long userId) {
        Object id = entityManager.createNativeQuery("""
                INSERT INTO public.payroll_employee_item
                    (payroll_employee_id, payroll_component_id, description,
                     quantity, rate, amount, currency_code, exchange_rate,
                     payroll_amount, source_type, remarks, created_by)
                VALUES
                    (:payrollEmployeeId, :componentId, :description,
                     :quantity, :rate, :amount, :currency, :exchangeRate,
                     :payrollAmount, :sourceType, :remarks, :userId)
                RETURNING payroll_employee_item_id
                """)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .setParameter("componentId", componentId)
                .setParameter("description", description)
                .setParameter("quantity", quantity)
                .setParameter("rate", rate)
                .setParameter("amount", amount)
                .setParameter("currency", currency)
                .setParameter("exchangeRate", exchangeRate)
                .setParameter("payrollAmount", payrollAmount)
                .setParameter("sourceType", sourceType)
                .setParameter("remarks", remarks)
                .setParameter("userId", userId)
                .getSingleResult();
        return ((Number) id).longValue();
    }

    public int update(
            Long itemId,
            Long payrollEmployeeId,
            Long componentId,
            String description,
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal amount,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal payrollAmount,
            String sourceType,
            String remarks,
            Long userId) {
        return entityManager.createNativeQuery("""
                UPDATE public.payroll_employee_item
                SET payroll_component_id = :componentId,
                    description = :description,
                    quantity = :quantity,
                    rate = :rate,
                    amount = :amount,
                    currency_code = :currency,
                    exchange_rate = :exchangeRate,
                    payroll_amount = :payrollAmount,
                    source_type = :sourceType,
                    remarks = :remarks,
                    updated_by = :userId,
                    updated_at = CURRENT_TIMESTAMP
                WHERE payroll_employee_item_id = :itemId
                  AND payroll_employee_id = :payrollEmployeeId
                """)
                .setParameter("componentId", componentId)
                .setParameter("description", description)
                .setParameter("quantity", quantity)
                .setParameter("rate", rate)
                .setParameter("amount", amount)
                .setParameter("currency", currency)
                .setParameter("exchangeRate", exchangeRate)
                .setParameter("payrollAmount", payrollAmount)
                .setParameter("sourceType", sourceType)
                .setParameter("remarks", remarks)
                .setParameter("userId", userId)
                .setParameter("itemId", itemId)
                .setParameter("payrollEmployeeId", payrollEmployeeId)
                .executeUpdate();
    }

    public int delete(Long itemId) {
        if (itemId == null) {
            return 0;
        }
        return entityManager.createNativeQuery("""
                DELETE FROM public.payroll_employee_item
                WHERE payroll_employee_item_id = :itemId
                """)
                .setParameter("itemId", itemId)
                .executeUpdate();
    }

    private static ItemRow mapItem(Object[] row) {
        return new ItemRow(
                longValue(row[0]),
                longValue(row[1]),
                longValue(row[2]),
                text(row[3]),
                text(row[4]),
                text(row[5]),
                text(row[6]),
                decimal(row[7]),
                decimalNullable(row[8]),
                decimal(row[9]),
                text(row[10]),
                decimal(row[11]),
                decimal(row[12]),
                text(row[13]),
                text(row[14]));
    }

    private static Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
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

    private static BigDecimal decimalNullable(Object value) {
        return value == null ? null : decimal(value);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
