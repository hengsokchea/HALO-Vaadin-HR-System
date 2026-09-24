package org.halocambodia.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollModels.PayrollRuleRow;
import org.halocambodia.data.PayrollPolicyCalculationRepository;
import org.halocambodia.data.PayrollPolicyCalculationRepository.PolicyContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes automatic attendance/leave/overtime/allowance payroll rules.
 * Workflow decisions stay in {@link PayrollService}; set-based SQL is owned by
 * {@link PayrollPolicyCalculationRepository}. No JdbcTemplate is used here.
 */
@Service
public class PayrollPolicyCalculationService {

    private final PayrollPolicyCalculationRepository calculationRepository;
    private final PayrollRuleQueryService payrollRuleQueryService;
    private final PayrollComponentRepository payrollComponentRepository;

    public PayrollPolicyCalculationService(
            PayrollPolicyCalculationRepository calculationRepository,
            PayrollRuleQueryService payrollRuleQueryService,
            PayrollComponentRepository payrollComponentRepository) {
        this.calculationRepository = calculationRepository;
        this.payrollRuleQueryService = payrollRuleQueryService;
        this.payrollComponentRepository = payrollComponentRepository;
    }

    @Transactional
    public void calculate(Long runId, @Nullable Long payrollEmployeeId, Long userId) {
        PolicyContext context = calculationRepository.findContext(runId);
        if (context == null) {
            throw new IllegalArgumentException("Payroll run not found.");
        }

        LocalDate monthStart = LocalDate.of(context.year(), context.month(), 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        List<PayrollRuleRow> yearlyRules = payrollRuleQueryService.find(context.year());
        List<PayrollRuleRow> deductionRules = automaticRules(
                yearlyRules, "ATTENDANCE", "DEDUCTION");
        List<PayrollRuleRow> leavePayRules = automaticRules(
                yearlyRules, "LEAVE", "LEAVE_PAY");
        List<PayrollRuleRow> overtimeRules = yearlyRules.stream()
                .filter(PayrollRuleRow::active)
                .filter(rule -> "ATTENDANCE".equals(rule.calculationSource()))
                .filter(rule -> "OVERTIME".equals(rule.ruleType()))
                .toList();
        List<PayrollRuleRow> allowanceRules = automaticRules(
                yearlyRules, "ATTENDANCE", "ALLOWANCE");

        calculationRepository.deleteAutomaticPolicyItems(runId, payrollEmployeeId);

        if (deductionRules.isEmpty()
                && leavePayRules.isEmpty()
                && overtimeRules.isEmpty()
                && allowanceRules.isEmpty()) {
            return;
        }

        if (context.periodStart().isAfter(monthStart)
                || context.periodEnd().isBefore(monthEnd)) {
            throw new IllegalStateException(
                    "Payroll period " + context.periodStart() + " to " + context.periodEnd()
                            + " must include the complete payroll month " + monthStart + " to " + monthEnd
                            + " so the daily-rate divisor can be calculated from the roster.");
        }

        long currencyMismatch = calculationRepository.countCurrencyMismatch(
                runId, payrollEmployeeId, context.payrollCurrency());
        if (currencyMismatch > 0) {
            throw new IllegalStateException(
                    "Automatic attendance payroll rules require employee salary currency to match payroll currency "
                            + context.payrollCurrency() + ".");
        }

        validateDailyRateBasis(runId, payrollEmployeeId, monthStart, monthEnd);

        for (PayrollRuleRow leavePayRule : leavePayRules) {
            String triggerCode = attendanceTriggerCode(leavePayRule);
            validateLeavePeriodSnapshot(
                    runId,
                    payrollEmployeeId,
                    triggerCode,
                    leavePayRule.code() + " - " + leavePayRule.name());
        }

        for (PayrollRuleRow deductionRule : deductionRules) {
            requireNonNull(
                    deductionRule.multiplier(),
                    "Rule " + deductionRule.code() + " multiplier");
            Long componentId = requireRuleComponent(deductionRule, "DEDUCTION");
            calculationRepository.insertAttendanceDeduction(
                    runId,
                    payrollEmployeeId,
                    componentId,
                    upperOrNull(deductionRule.code()),
                    deductionRule.multiplier(),
                    requireWorkdayDivisor(deductionRule),
                    userId);
        }

        for (PayrollRuleRow leavePayRule : leavePayRules) {
            boolean requireInitialFullPayMonths = Set.of("S", "PI")
                    .contains(upperOrNull(leavePayRule.code()));
            validateLeavePayRule(leavePayRule, requireInitialFullPayMonths);
            Long componentId = requireRuleComponent(leavePayRule, "DEDUCTION");
            calculationRepository.insertLeavePayAdjustment(
                    runId,
                    payrollEmployeeId,
                    componentId,
                    attendanceTriggerCode(leavePayRule),
                    "Automatic " + leavePayRule.code() + " leave pay adjustment",
                    leavePayRule.code(),
                    leavePayRule.initialFullPayMonths() == null
                            ? 0
                            : leavePayRule.initialFullPayMonths(),
                    leavePayRule.maxPaidMonths(),
                    leavePayRule.payPercentage(),
                    requireWorkdayDivisor(leavePayRule),
                    userId);
        }

        for (PayrollRuleRow overtimeRule : overtimeRules) {
            requireNonNull(
                    overtimeRule.multiplier(),
                    "Rule " + overtimeRule.code() + " multiplier");

            String rateUnit = upperOrNull(overtimeRule.rateUnit());
            if (!Set.of("DAY", "HOUR").contains(rateUnit)) {
                throw new IllegalStateException(
                        "Overtime rule " + overtimeRule.code() + " must use DAY or HOUR. "
                                + "| ច្បាប់ម៉ោងបន្ថែម " + overtimeRule.code()
                                + " ត្រូវប្រើឯកតា DAY ឬ HOUR។");
            }

            if ("HOUR".equals(rateUnit)) {
                validateHourlyRateBasis(
                        runId,
                        payrollEmployeeId,
                        monthStart,
                        monthEnd,
                        overtimeRule.code());
            }

            Long componentId = requireRuleComponent(overtimeRule, "EARNING");
            calculationRepository.insertOvertimePay(
                    runId,
                    payrollEmployeeId,
                    componentId,
                    upperOrNull(overtimeRule.code()),
                    rateUnit,
                    overtimeRule.multiplier(),
                    requireWorkdayDivisor(overtimeRule),
                    userId);
        }

        for (PayrollRuleRow allowanceRule : allowanceRules) {
            insertAttendanceAllowance(
                    runId,
                    payrollEmployeeId,
                    allowanceRule,
                    context.payrollCurrency(),
                    userId);
        }
    }

    private static int requireWorkdayDivisor(PayrollRuleRow rule) {
        Integer divisor = rule == null ? null : rule.workdayDivisor();
        if (divisor == null || !Set.of(22, 24, 26).contains(divisor)) {
            throw new IllegalStateException(
                    "Payroll rule " + (rule == null ? "" : rule.code())
                            + " must have Workday Divisor 22, 24, or 26. "
                            + "| ច្បាប់ប្រាក់បៀវត្សត្រូវមានចំនួនថ្ងៃចែក 22, 24 ឬ 26។");
        }
        return divisor;
    }

    private static List<PayrollRuleRow> automaticRules(
            List<PayrollRuleRow> rules,
            String source,
            String type) {
        return rules.stream()
                .filter(PayrollRuleRow::active)
                .filter(rule -> source.equals(rule.calculationSource()))
                .filter(rule -> type.equals(rule.ruleType()))
                .toList();
    }

    private static String attendanceTriggerCode(PayrollRuleRow rule) {
        return attendanceTriggerCode(rule == null ? null : rule.code());
    }

    private static String attendanceTriggerCode(String ruleCode) {
        return upperOrNull(ruleCode);
    }

    private void validateDailyRateBasis(
            Long runId,
            @Nullable Long payrollEmployeeId,
            LocalDate monthStart,
            LocalDate monthEnd) {
        long missingWorkdays = calculationRepository.countMissingWorkdays(
                runId,
                payrollEmployeeId,
                monthStart,
                monthEnd);
        if (missingWorkdays > 0) {
            throw new IllegalStateException(
                    missingWorkdays
                            + " payroll employee(s) have no scheduled workdays in the payroll month. "
                            + "Review the roster before calculating payroll.");
        }
    }

    private void validateHourlyRateBasis(
            Long runId,
            @Nullable Long payrollEmployeeId,
            LocalDate monthStart,
            LocalDate monthEnd,
            String ruleCode) {
        long missingHours = calculationRepository.countMissingHourlyBasis(
                runId,
                payrollEmployeeId,
                monthStart,
                monthEnd);
        if (missingHours > 0) {
            throw new IllegalStateException(
                    missingHours
                            + " payroll employee(s) have overtime hours but no normal working hours "
                            + "for hourly overtime rule " + ruleCode + ". Review the roster first. "
                            + "| បុគ្គលិក " + missingHours
                            + " នាក់មានម៉ោងបន្ថែម ប៉ុន្តែមិនមានម៉ោងធ្វើការធម្មតា សម្រាប់ច្បាប់ "
                            + ruleCode + "។ សូមពិនិត្យកាលវិភាគការងារជាមុន។");
        }
    }

    private void validateLeavePeriodSnapshot(
            Long runId,
            @Nullable Long payrollEmployeeId,
            String triggerCode,
            String label) {
        long missing = calculationRepository.countMissingLeavePeriodSnapshot(
                runId,
                payrollEmployeeId,
                triggerCode);
        if (missing > 0) {
            throw new IllegalStateException(
                    missing + " " + label
                            + " attendance day(s) do not have an HR-verified leave period. "
                            + "Verify the leave request (HR status 6), then regenerate/check attendance before payroll.");
        }
    }

    private void validateLeavePayRule(PayrollRuleRow rule, boolean requireFullPayMonths) {
        requireNonNull(
                rule.payPercentage(),
                "Rule " + rule.code() + " pay percentage");
        if (requireFullPayMonths && rule.initialFullPayMonths() == null) {
            throw new IllegalStateException(
                    "Rule " + rule.code() + " requires Initial Full Pay Months.");
        }
        if (rule.maxPaidMonths() == null || rule.maxPaidMonths() <= 0) {
            throw new IllegalStateException(
                    "Rule " + rule.code() + " requires Max Paid Months greater than zero.");
        }
        int initialMonths = rule.initialFullPayMonths() == null
                ? 0
                : rule.initialFullPayMonths();
        if (rule.maxPaidMonths() < initialMonths) {
            throw new IllegalStateException(
                    "Rule " + rule.code()
                            + " Max Paid Months cannot be less than Initial Full Pay Months.");
        }
    }

    private static void requireNonNull(Object value, String label) {
        if (value == null) {
            throw new IllegalStateException(
                    label + " is required for automatic payroll calculation.");
        }
    }

    private Long requireRuleComponent(PayrollRuleRow rule, String expectedType) {
        if (rule.componentId() == null) {
            throw new IllegalStateException(
                    "Payroll rule " + rule.code()
                            + " is not linked to a Payroll Component. "
                            + "Edit the yearly rule and choose its Payroll Component.");
        }

        PayrollComponent component = payrollComponentRepository.findById(rule.componentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payroll rule " + rule.code()
                                + " must reference an active " + expectedType
                                + " Payroll Component."));

        if (!component.isActive()
                || !expectedType.equalsIgnoreCase(component.getComponentType())) {
            throw new IllegalStateException(
                    "Payroll rule " + rule.code()
                            + " must reference an active " + expectedType
                            + " Payroll Component.");
        }

        return component.getId();
    }

    private void insertAttendanceAllowance(
            Long runId,
            @Nullable Long payrollEmployeeId,
            PayrollRuleRow rule,
            String payrollCurrency,
            Long userId) {
        String ruleCode = upperOrNull(rule.code());
        long occurrences = calculationRepository.countAttendanceCodeOccurrences(
                runId,
                payrollEmployeeId,
                ruleCode);
        if (occurrences == 0) {
            return;
        }

        requireNonNull(
                rule.rateAmount(),
                "Rule " + rule.code() + " rate amount");

        String rateCurrency = upperOrNull(rule.rateCurrency());
        if (rateCurrency == null) {
            throw new IllegalStateException(
                    "Rule " + rule.code()
                            + " rate currency is required for automatic allowance calculation.");
        }
        if (!rateCurrency.equals(payrollCurrency)) {
            throw new IllegalStateException(
                    "Rule " + rule.code() + " currency " + rateCurrency
                            + " must match payroll currency " + payrollCurrency + ".");
        }

        String rateUnit = upperOrNull(rule.rateUnit());
        if (rateUnit == null || !Set.of("DAY", "CYCLE", "MONTH").contains(rateUnit)) {
            throw new IllegalStateException(
                    "Rule " + rule.code()
                            + " must use DAY, CYCLE or MONTH for automatic attendance allowance calculation.");
        }

        Long componentId = requireRuleComponent(rule, "EARNING");
        calculationRepository.insertAttendanceAllowance(
                runId,
                payrollEmployeeId,
                componentId,
                ruleCode,
                rateUnit,
                rule.rateAmount(),
                rateCurrency,
                userId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
