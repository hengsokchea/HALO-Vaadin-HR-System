package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.halocambodia.enums.PayrollAttendanceControlStatus;
import org.halocambodia.enums.PayrollInstallmentType;
import org.halocambodia.enums.PayrollPaymentBatchStatus;
import org.halocambodia.enums.PayrollPeriodStatus;
import org.halocambodia.enums.PayrollRunType;

public final class PayrollModels {

    private PayrollModels() {
    }

    public static final List<String> PERIOD_STATUSES = PayrollPeriodStatus.codes();
    public static final List<String> RUN_TYPES = PayrollRunType.codes();
    public static final List<String> COMPONENT_TYPES = List.of("EARNING", "DEDUCTION", "EMPLOYER_CONTRIBUTION");
    public static final List<String> CALCULATION_METHODS =List.of("FIXED", "RATE", "FORMULA", "MANUAL");
    public static final List<String> SOURCE_TYPES =List.of("SALARY", "ATTENDANCE", "LEAVE", "OVERTIME", "TAX", "NSSF","SENIORITY", "MANUAL", "OTHER");
    public static final List<String> ATTENDANCE_CONTROL_STATUSES =PayrollAttendanceControlStatus.codes();
    public static final List<String> PAYROLL_RULE_TYPES =List.of("DEDUCTION", "LEAVE_ENTITLEMENT", "LEAVE_PAY", "OVERTIME", "ALLOWANCE");
    public static final List<String> PAYROLL_RULE_SOURCES =List.of("ATTENDANCE", "LEAVE", "MANUAL", "OTHER");
    public static final List<String> PAYROLL_RULE_UNITS =List.of("DAY", "CYCLE", "MONTH", "HOUR");
    public static final List<String> PAYMENT_FREQUENCIES = List.of("MONTHLY", "SEMI_MONTHLY");
    public static final List<String> PAYMENT_INSTALLMENT_TYPES =PayrollInstallmentType.codes();
    public static final List<String> PAYMENT_STATUSES = PayrollPaymentBatchStatus.codes();

    public record DashboardStats(
            long openPeriods,
            long activeRuns,
            long employeeCount,
            BigDecimal totalEarnings,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            String latestPeriod) {
    }

    public record PayrollReconciliationSummary(
            String period,
            BigDecimal payrollNetPay,
            BigDecimal firstPaymentPaid,
            BigDecimal finalSettlementPaid,
            BigDecimal adjustmentCarryForward,
            BigDecimal recoveryCarryForward,
            BigDecimal actualCashPaid,
            BigDecimal difference) {
    }

    public record PeriodRow(
            Long id,
            int year,
            int month,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate paymentDate,
            String currency,
            BigDecimal usdToKhrRate,
            BigDecimal nssfUsdToKhrRate,
            String status,
            long runCount,
            long employeeCount,
            BigDecimal totalEarnings,
            BigDecimal netPay) {

        public String displayName() {
            String range = startDate == null || endDate == null
                    ? ""
                    : " · %s → %s".formatted(startDate, endDate);
            return "%04d-%02d%s · %s".formatted(year, month, range, status);
        }
    }

    public record RunRow(
            Long id,
            Long periodId,
            int year,
            int month,
            int runNumber,
            String runType,
            String status,
            boolean correctionMode,
            long employeeCount,
            BigDecimal totalEarnings,
            BigDecimal totalDeductions,
            BigDecimal netPay,
            BigDecimal firstPaid,
            BigDecimal finalPaid,
            String notes) {

        public String displayName() {
            return "Run %d · %s · %s".formatted(runNumber, runType.replace('_', ' '), status);
        }
    }

    public record PayrollCandidateRow(
            Long empId,
            Integer insuranceNo,
            String nameEn,
            String nameKh,
            LocalDate lastCareerTypeDate,
            BigDecimal referenceNetPay) {

        public String displayName() {
            return insuranceNo + " · " + (nameEn == null ? "" : nameEn);
        }
    }

    public record EmployeePayrollRow(
            Long id,
            Long runId,
            Long empId,
            Integer insuranceNo,
            String nameEn,
            String nameKh,
            String gender,
            String bankName,
            String bankAccount,
            int spouseCount,
            int taxDependentCount,
            String salaryCurrency,
            BigDecimal basicSalary,
            BigDecimal calculatedBasicSalary,
            BigDecimal totalEarnings,
            BigDecimal totalDeductions,
            BigDecimal salaryTax,
            BigDecimal employeeContribution,
            BigDecimal employerContribution,
            BigDecimal netSalary,
            String payrollStatus,
            String remarks,
            boolean correctionRequired,
            String correctionReason,
            OffsetDateTime correctionReturnedAt,
            OffsetDateTime correctionResolvedAt) {

        public String displayName() {
            String name = nameEn == null || nameEn.isBlank() ? nameKh : nameEn;
            return (insuranceNo == null ? "-" : insuranceNo.toString())
                    + " · " + (name == null ? "" : name);
        }
    }

    public record AttendanceRow(
            BigDecimal scheduledDays,
            BigDecimal workedDays,
            BigDecimal holidayDays,
            BigDecimal absentDays,
            BigDecimal annualLeaveDays,
            BigDecimal specialLeaveDays,
            BigDecimal sickLeaveDays,
            BigDecimal maternityLeaveDays,
            BigDecimal otherPaidLeaveDays,
            BigDecimal unpaidLeaveDays,
            BigDecimal overtimeHours,
            BigDecimal annualLeaveRemaining,
            BigDecimal overusedAnnualLeave,
            BigDecimal specialLeaveRemaining,
            BigDecimal overusedSpecialLeave,
            LocalDate sourceStartDate,
            LocalDate sourceEndDate) {
    }

    public record AttendanceControlRow(
            Long periodId,
            String status,
            long rosterEmployeeCount,
            long rosterDayCount,
            long attendanceRecordCount,
            long rosterFallbackDayCount,
            long unverifiedAttendanceCount,
            BigDecimal overtimeHours,
            OffsetDateTime sourceLastModifiedAt,
            OffsetDateTime checkedAt,
            String checkedBy,
            OffsetDateTime approvedAt,
            String approvedBy,
            String notes,
            boolean stale) {

        public boolean approvedAndCurrent() {
            return PayrollAttendanceControlStatus.APPROVED.matches(status) && !stale;
        }

        public boolean readyForApproval() {
            return PayrollAttendanceControlStatus.CHECKED.matches(status)  && rosterEmployeeCount > 0  && rosterDayCount > 0  && unverifiedAttendanceCount == 0  && !stale;
        }
    }

    public record AttendanceDayCell(
            LocalDate date,
            String code,
            String category,
            String duration,
            BigDecimal dayValue,
            BigDecimal overtimeHours,
            String source,
            boolean hrVerified,
            int duplicateCount,
            String remarks) {

        public boolean issue() {
            return !hrVerified || duplicateCount > 1;
        }
    }

    public record AttendanceReviewRow(
            Long empId,
            Integer insuranceNo,
            String nameEn,
            String nameKh,
            String position,
            String employeeCategory,
            String team,
            String teamType,
            String location,
            String contract,
            String donor,
            BigDecimal annualLeaveRemaining,
            BigDecimal overusedAnnualLeave,
            BigDecimal specialLeaveRemaining,
            BigDecimal overusedSpecialLeave,
            Map<LocalDate, AttendanceDayCell> days,
            BigDecimal presentDays,
            BigDecimal absentDays,
            BigDecimal annualLeaveDays,
            BigDecimal unpaidLeaveDays,
            BigDecimal specialLeaveDays,
            BigDecimal sickLeaveDays,
            BigDecimal maternityLeaveDays,
            BigDecimal holidayDays,
            BigDecimal otherDays,
            Map<String, BigDecimal> policyCodeDays,
            BigDecimal overtimeHours,
            long unverifiedAttendanceCount,
            String remarks) {

        public boolean hasIssues() {
            return unverifiedAttendanceCount > 0
                    || hasDuplicateDay()
                    || positive(overusedAnnualLeave)
                    || positive(overusedSpecialLeave);
        }

        private boolean hasDuplicateDay() {
            return days != null && days.values().stream()
                    .anyMatch(day -> day != null && day.duplicateCount() > 1);
        }

        private static boolean positive(BigDecimal value) {
            return value != null && value.signum() > 0;
        }
    }

    public record ComponentRow(
            Long id,
            String code,
            String nameEn,
            String nameKh,
            String componentType,
            String calculationMethod,
            boolean allowManualEntry,
            boolean taxable,
            boolean subjectToNssf,
            boolean subjectToSeniority,
            boolean active,
            String glAccountCode,
            int sortOrder) {

        public String displayName() {
            return code + " · " + nameEn;
        }
    }

    public record ItemRow(
            Long id,
            Long payrollEmployeeId,
            Long componentId,
            String componentCode,
            String componentName,
            String componentType,
            String description,
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal amount,
            String currency,
            BigDecimal exchangeRate,
            BigDecimal payrollAmount,
            String sourceType,
            String remarks) {
    }

    public record PaymentBatchRow(
            Long id,
            Long periodId,
            Long runId,
            String installmentType,
            LocalDate paymentDate,
            String status,
            long employeeCount,
            BigDecimal paymentAmount,
            BigDecimal carryForwardAmount,
            String currency,
            String notes,
            OffsetDateTime approvedAt,
            OffsetDateTime paidAt,
            String paymentMethod,
            String paymentReference,
            String bankReference,
            String paymentFileName,
            OffsetDateTime paymentExportedAt,
            OffsetDateTime paymentConfirmedAt) {
    }

    public record PaymentExecutionInput(
            String paymentMethod,
            String paymentReference,
            String bankReference) {
    }

    public record PaymentFileExport(
            String fileName,
            byte[] content) {
    }

    public record NssfPaymentRow(
            Long payrollEmployeeId,
            Long employeeId,
            Integer insuranceNo,
            String nssfNo,
            String nationalId,
            String nameEn,
            String nameKh,
            String sex,
            String nationality,
            LocalDate dateOfBirth,
            LocalDate dateOfJoin,
            BigDecimal grossWageUsd,
            BigDecimal grossWageKhr,
            BigDecimal nssfExchangeRate,
            BigDecimal contributionWageKhr,
            BigDecimal healthEmployeeKhr,
            BigDecimal healthEmployerKhr,
            BigDecimal riskEmployeeKhr,
            BigDecimal riskEmployerKhr,
            BigDecimal pensionEmployeeKhr,
            BigDecimal pensionEmployerKhr) {

        public BigDecimal employeeTotalKhr() {
            return zero(healthEmployeeKhr)
                    .add(zero(riskEmployeeKhr))
                    .add(zero(pensionEmployeeKhr));
        }

        public BigDecimal employerTotalKhr() {
            return zero(healthEmployerKhr)
                    .add(zero(riskEmployerKhr))
                    .add(zero(pensionEmployerKhr));
        }

        public BigDecimal totalNssfKhr() {
            return employeeTotalKhr().add(employerTotalKhr());
        }

        private static BigDecimal zero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    public record EmployeePaymentRow(
            Long id,
            Long batchId,
            Long employeeId,
            Long payrollEmployeeId,
            Integer insuranceNo,
            String nameEn,
            String nameKh,
            String bankName,
            String bankAccount,
            String personalEmail,
            String paymentFrequency,
            BigDecimal firstPaymentPercent,
            BigDecimal basicSalary,
            BigDecimal fullNetAmount,
            BigDecimal previousPaidAmount,
            BigDecimal paymentAmount,
            BigDecimal carryForwardAmount,
            String currency,
            String remarks,
            String emailStatus,
            OffsetDateTime emailStatusAt) {
    }

    /** Effective payment schedule resolved for one payroll period. */
    public record PaymentScheduleSummary(
            LocalDate referenceDate,
            boolean companySettingFound,
            String companyFrequency,
            BigDecimal companyFirstPercent,
            long rosteredEmployeeCount,
            long monthlyEmployeeCount,
            long semiMonthlyEmployeeCount,
            long shiftOverrideCount,
            long unconfiguredEmployeeCount) {

        public boolean hasSemiMonthlyEmployees() {
            return semiMonthlyEmployeeCount > 0;
        }

        public boolean mixedSchedule() {
            return monthlyEmployeeCount > 0 && semiMonthlyEmployeeCount > 0;
        }
    }


    public record PayrollAuditEventRow(
            Long id,
            OffsetDateTime eventAt,
            String eventType,
            Long eventBy,
            String actorName,
            String actorUsername,
            Long payrollEmployeeId,
            Integer insuranceNo,
            String employeeNameEn,
            String employeeNameKh,
            Long payrollPaymentBatchId,
            String fromStatus,
            String toStatus,
            String reason,
            String eventDetail) {

        public String actorDisplay() {
            if (actorName != null && !actorName.isBlank()) {
                return actorUsername == null || actorUsername.isBlank()
                        ? actorName
                        : actorName + " (" + actorUsername + ")";
            }
            if (actorUsername != null && !actorUsername.isBlank()) {
                return actorUsername;
            }
            return eventBy == null ? "-" : "User #" + eventBy;
        }

        public String employeeDisplay() {
            StringBuilder value = new StringBuilder();
            if (insuranceNo != null) {
                value.append(insuranceNo);
            }
            if (employeeNameEn != null && !employeeNameEn.isBlank()) {
                if (value.length() > 0) value.append(" · ");
                value.append(employeeNameEn);
            } else if (employeeNameKh != null && !employeeNameKh.isBlank()) {
                if (value.length() > 0) value.append(" · ");
                value.append(employeeNameKh);
            }
            if (value.length() > 0) {
                return value.toString();
            }
            return payrollEmployeeId == null ? "-" : "Payroll Employee #" + payrollEmployeeId;
        }
    }

    public record TaxConfigRow(
            Long id,
            int taxYear,
            String currency,
            BigDecimal dependentAllowance,
            BigDecimal nonResidentRate,
            boolean active,
            String sourceReference,
            String notes) {
    }

    public record TaxBracketRow(
            Long id,
            Long taxConfigId,
            int bracketOrder,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal taxRate,
            BigDecimal deductionAmount) {
    }

    public record NssfConfigRow(
            Long id,
            Long eligibleContractTypeId,
            String eligibleContractTypeName,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String currency,
            BigDecimal healthEmployeeRate,
            BigDecimal healthEmployerRate,
            BigDecimal riskEmployeeRate,
            BigDecimal riskEmployerRate,
            BigDecimal pensionEmployeeRate,
            BigDecimal pensionEmployerRate,
            BigDecimal pensionMinWage,
            BigDecimal pensionMaxWage,
            boolean active,
            String sourceReference,
            String notes) {
    }

    public record NssfBandRow(
            Long id,
            Long nssfConfigId,
            int bandOrder,
            BigDecimal maxSalary,
            BigDecimal contributoryWage) {
    }

    public record PayrollRuleRow(
            Long id,
            int ruleYear,
            String code,
            String name,
            String ruleType,
            BigDecimal rateAmount,
            String rateCurrency,
            String rateUnit,
            BigDecimal multiplier,
            Integer workdayDivisor,
            BigDecimal payPercentage,
            BigDecimal entitlementDays,
            Integer serviceYearsPerExtraDay,
            Integer initialFullPayMonths,
            Integer maxPaidMonths,
            Long componentId,
            String componentCode,
            String componentName,
            String calculationSource,
            boolean active,
            int sortOrder,
            String description) {
    }

    public record PeriodInput(
            Long id,
            int year,
            int month,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate paymentDate,
            String currency,
            BigDecimal usdToKhrRate,
            BigDecimal nssfUsdToKhrRate,
            String status) {
    }

    public record ComponentInput(
            Long id,
            String code,
            String nameEn,
            String nameKh,
            String componentType,
            String calculationMethod,
            boolean allowManualEntry,
            boolean taxable,
            boolean subjectToNssf,
            boolean subjectToSeniority,
            boolean active,
            String glAccountCode,
            int sortOrder) {
    }

    public record TaxConfigInput(
            Long id,
            int taxYear,
            String currency,
            BigDecimal dependentAllowance,
            BigDecimal nonResidentRate,
            boolean active,
            String sourceReference,
            String notes) {
    }

    public record TaxBracketInput(
            Long id,
            Long taxConfigId,
            int bracketOrder,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal taxRate,
            BigDecimal deductionAmount) {
    }

    public record NssfConfigInput(
            Long id,
            Long eligibleContractTypeId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String currency,
            BigDecimal healthEmployeeRate,
            BigDecimal healthEmployerRate,
            BigDecimal riskEmployeeRate,
            BigDecimal riskEmployerRate,
            BigDecimal pensionEmployeeRate,
            BigDecimal pensionEmployerRate,
            BigDecimal pensionMinWage,
            BigDecimal pensionMaxWage,
            boolean active,
            String sourceReference,
            String notes) {
    }

    public record NssfBandInput(
            Long id,
            Long nssfConfigId,
            int bandOrder,
            BigDecimal maxSalary,
            BigDecimal contributoryWage) {
    }

    public record PayrollRuleInput(
            Long id,
            int ruleYear,
            String code,
            String name,
            String ruleType,
            BigDecimal rateAmount,
            String rateCurrency,
            String rateUnit,
            BigDecimal multiplier,
            Integer workdayDivisor,
            BigDecimal payPercentage,
            BigDecimal entitlementDays,
            Integer serviceYearsPerExtraDay,
            Integer initialFullPayMonths,
            Integer maxPaidMonths,
            Long componentId,
            String calculationSource,
            boolean active,
            int sortOrder,
            String description) {
    }

    public record ItemInput(
            Long id,
            Long payrollEmployeeId,
            Long componentId,
            String description,
            BigDecimal quantity,
            BigDecimal rate,
            BigDecimal amount,
            String currency,
            BigDecimal exchangeRate,
            String sourceType,
            String remarks) {
    }
}
