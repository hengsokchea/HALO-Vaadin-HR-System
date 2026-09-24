package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Immutable read model used only for paid-payroll payslip generation.
 * Every row represents one payroll component line while repeating the final
 * paid employee and payment snapshots needed by the report group header/footer.
 */
@Getter
@AllArgsConstructor
public class PayrollPayslipRow {

    private final Long payrollEmployeePaymentId;
    private final Long payrollEmployeeId;
    private final Long employeeId;
    private final Integer insuranceNo;
    private final String employeeNameEn;
    private final String employeeNameKh;
    private final String bankName;
    private final String bankAccount;

    private final Integer payrollYear;
    private final Integer payrollMonth;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate paymentDate;
    private final Integer runNumber;
    private final String runType;
    private final String installmentType;
    private final String currency;

    private final BigDecimal basicSalary;
    private final BigDecimal totalEarnings;
    private final BigDecimal totalDeductions;
    private final BigDecimal salaryTax;
    private final BigDecimal employeeContribution;
    private final BigDecimal employerContribution;
    private final BigDecimal netSalary;

    private final BigDecimal previousPaidAmount;
    private final BigDecimal paymentAmount;
    private final BigDecimal carryForwardAmount;

    private final String paymentMethod;
    private final String paymentReference;
    private final String bankReference;
    private final OffsetDateTime paidAt;

    private final String componentCode;
    private final String componentNameEn;
    private final String componentNameKh;
    private final String componentType;
    private final String description;
    private final String sourceType;
    private final BigDecimal lineAmount;
}
