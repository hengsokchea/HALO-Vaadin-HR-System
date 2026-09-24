package org.halocambodia.data;

import static org.halocambodia.data.PayrollModels.PeriodRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class PayrollPeriodDataRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<PeriodRow> findPeriodRows() {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    p.payroll_period_id, p.payroll_year, p.payroll_month,
                    p.period_start, p.period_end, p.payment_date,
                    p.payroll_currency, p.usd_to_khr_rate, p.nssf_usd_to_khr_rate, p.status,
                    COUNT(DISTINCT r.payroll_run_id) AS run_count,
                    COUNT(DISTINCT pe.payroll_employee_id) AS employee_count,
                    COALESCE(SUM(pe.total_earnings), 0) AS total_earnings,
                    COALESCE(SUM(pe.net_salary), 0) AS net_pay
                FROM public.payroll_period p
                LEFT JOIN public.payroll_run r ON r.payroll_period_id = p.payroll_period_id
                                             AND r.status <> 'CANCELLED'
                LEFT JOIN public.payroll_employee pe
                  ON pe.payroll_run_id = r.payroll_run_id
                 AND pe.payroll_status = 'INCLUDED'
                GROUP BY p.payroll_period_id
                ORDER BY p.payroll_year DESC, p.payroll_month DESC
                """).getResultList();

        return rows.stream().map(this::toPeriodRow).toList();
    }

    private PeriodRow toPeriodRow(Object[] row) {
        return new PeriodRow(
                longValue(row[0]), intValue(row[1]), intValue(row[2]),
                localDate(row[3]), localDate(row[4]), localDate(row[5]),
                stringValue(row[6]), decimal(row[7]), decimal(row[8]), stringValue(row[9]),
                longValue(row[10]), longValue(row[11]), decimal(row[12]), decimal(row[13]));
    }

    private static Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static int intValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(value.toString());
    }

    private static LocalDate localDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
