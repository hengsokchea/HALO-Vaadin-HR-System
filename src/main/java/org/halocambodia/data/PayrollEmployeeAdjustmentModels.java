package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTOs for recurring payroll items and company loans. */
public final class PayrollEmployeeAdjustmentModels {

    private PayrollEmployeeAdjustmentModels() {
    }

    public record RecurringRow(
            Long id,
            Long employeeId,
            Integer insuranceNo,
            String employeeNameEn,
            String employeeNameKh,
            String componentCode,
            String componentNameEn,
            String componentType,
            BigDecimal amount,
            String recurrenceType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active,
            String description,
            String remarks,
            long settledApplicationCount) {
    }

    public record RecurringInput(
            Long id,
            Long employeeId,
            String componentCode,
            BigDecimal amount,
            String recurrenceType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean active,
            String description,
            String remarks) {
    }

    public record LoanRow(
            Long id,
            Long employeeId,
            Integer insuranceNo,
            String employeeNameEn,
            String employeeNameKh,
            String recoveryType,
            String loanReference,
            BigDecimal principalAmount,
            BigDecimal monthlyInstallment,
            BigDecimal outstandingBalance,
            BigDecimal repaidAmount,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String remarks,
            long settledRepaymentCount) {
    }

    public record LoanInput(
            Long id,
            Long employeeId,
            String recoveryType,
            String loanReference,
            BigDecimal principalAmount,
            BigDecimal monthlyInstallment,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String remarks) {
    }
    public record LoanRepaymentRow(
            Long id,
            Long loanId,
            Long payrollRunId,
            int payrollYear,
            int payrollMonth,
            int runNumber,
            BigDecimal scheduledAmount,
            BigDecimal deductedAmount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String status,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime settledAt) {
    }

}
