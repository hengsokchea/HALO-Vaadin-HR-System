package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Read-only payroll result visible to the currently authenticated employee.
 * The row is backed by PAID payroll employee-payment snapshots only.
 */
@Getter
@AllArgsConstructor
public class PayrollEmployeeResultRow {
    private final Long employeePaymentId;
    private final Long paymentBatchId;
    private final Long payrollEmployeeId;
    private final Integer payrollYear;
    private final Integer payrollMonth;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate paymentDate;
    private final String installmentType;
    private final String currency;
    private final BigDecimal basicSalary;
    private final BigDecimal fullNetAmount;
    private final BigDecimal previousPaidAmount;
    private final BigDecimal paymentAmount;
    private final BigDecimal carryForwardAmount;
}
