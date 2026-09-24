package org.halocambodia.services;

import java.math.BigDecimal;
import java.util.List;

import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollTaxBracket;
import org.halocambodia.data.PayrollTaxBracketRepository;
import org.halocambodia.data.PayrollTaxCalculationRepository;
import org.halocambodia.data.PayrollTaxCalculationRepository.TaxContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Salary-tax calculation orchestration without direct JDBC access. */
@Service
public class PayrollSalaryTaxCalculationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final PayrollTaxCalculationRepository calculationRepository;
    private final PayrollTaxBracketRepository bracketRepository;
    private final PayrollComponentRepository componentRepository;

    public PayrollSalaryTaxCalculationService(
            PayrollTaxCalculationRepository calculationRepository,
            PayrollTaxBracketRepository bracketRepository,
            PayrollComponentRepository componentRepository) {
        this.calculationRepository = calculationRepository;
        this.bracketRepository = bracketRepository;
        this.componentRepository = componentRepository;
    }

    @Transactional
    public void calculate(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        TaxContext context = calculationRepository.findContext(runId);
        if (context == null) {
            throw new IllegalArgumentException("Payroll run not found.");
        }
        if (context.taxConfigId() == null) {
            throw new IllegalStateException(
                    "No active Salary Tax Rules exist for " + context.taxYear()
                            + ". Configure the tax year before calculating payroll.");
        }
        if (!"KHR".equals(context.taxCurrency())) {
            throw new IllegalStateException(
                    "Salary tax rules for " + context.taxYear() + " must use KHR currency.");
        }
        if (!"USD".equals(context.payrollCurrency())) {
            throw new IllegalStateException(
                    "Automatic salary tax currently requires payroll currency USD because payroll earnings are stored in USD.");
        }
        if (context.usdToKhrRate() == null || context.usdToKhrRate().signum() <= 0) {
            throw new IllegalStateException(
                    "USD to KHR exchange rate must be greater than zero.");
        }

        validateTaxBracketConfiguration(context.taxConfigId(), context.taxYear());

        Long salaryTaxComponentId = requireSalaryTaxComponent();
        long expectedEmployees = calculationRepository.countIncludedEmployees(
                runId,
                payrollEmployeeId);
        if (expectedEmployees == 0) {
            return;
        }

        calculationRepository.deleteExistingTaxItems(
                salaryTaxComponentId,
                runId,
                payrollEmployeeId);

        int inserted = calculationRepository.insertCalculatedTaxItems(
                runId,
                payrollEmployeeId,
                salaryTaxComponentId,
                userId);

        if (inserted != expectedEmployees) {
            throw new IllegalStateException(
                    "Salary-tax brackets do not cover every employee. Review the brackets for "
                            + context.taxYear() + ".");
        }
    }

    private Long requireSalaryTaxComponent() {
        PayrollComponent component = componentRepository
                .findByComponentCodeIgnoreCase("SALARY_TAX")
                .orElseThrow(() -> new IllegalStateException(
                        "Active SALARY_TAX deduction component is required before calculating payroll."));
        if (!component.isActive()
                || !"DEDUCTION".equalsIgnoreCase(component.getComponentType())) {
            throw new IllegalStateException(
                    "Active SALARY_TAX deduction component is required before calculating payroll.");
        }
        return component.getId();
    }

    private void validateTaxBracketConfiguration(Long taxConfigId, int taxYear) {
        List<PayrollTaxBracket> brackets = bracketRepository
                .findByPayrollTaxConfigIdOrderByBracketOrderAsc(taxConfigId);
        if (brackets.isEmpty()) {
            throw new IllegalStateException(
                    "No salary-tax brackets are configured for " + taxYear + ".");
        }
        if (brackets.getFirst().getMinAmount().compareTo(ZERO) != 0) {
            throw new IllegalStateException(
                    "Salary-tax bracket 1 must start at zero for " + taxYear
                            + ". | ថ្នាក់ពន្ធទី 1 ត្រូវចាប់ផ្ដើមពីសូន្យ។");
        }

        BigDecimal previousMaximum = null;
        for (int index = 0; index < brackets.size(); index++) {
            PayrollTaxBracket bracket = brackets.get(index);
            int expectedOrder = index + 1;
            if (!Integer.valueOf(expectedOrder).equals(bracket.getBracketOrder())) {
                throw new IllegalStateException(
                        "Salary-tax bracket order must be continuous from 1. Missing order "
                                + expectedOrder
                                + ". | លំដាប់ថ្នាក់ពន្ធត្រូវបន្តគ្នាចាប់ពីលេខ 1។");
            }

            boolean finalBracket = index == brackets.size() - 1;
            if (bracket.getMaxAmount() == null && !finalBracket) {
                throw new IllegalStateException(
                        "Only the final salary-tax bracket may have no maximum. "
                                + "| មានតែថ្នាក់ពន្ធចុងក្រោយប៉ុណ្ណោះដែលអាចគ្មានអតិបរមា។");
            }
            if (finalBracket && bracket.getMaxAmount() != null) {
                throw new IllegalStateException(
                        "The final salary-tax bracket must have no maximum. "
                                + "| ថ្នាក់ពន្ធចុងក្រោយត្រូវគ្មានចំនួនអតិបរមា។");
            }

            if (previousMaximum != null) {
                BigDecimal boundaryDifference = bracket.getMinAmount().subtract(previousMaximum);
                if (boundaryDifference.signum() < 0
                        || boundaryDifference.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalStateException(
                            "Salary-tax brackets must be continuous without gaps or overlaps at order "
                                    + bracket.getBracketOrder()
                                    + ". Use the previous maximum or previous maximum + 1 KHR as the next minimum. "
                                    + "| ថ្នាក់ពន្ធត្រូវបន្តគ្នាដោយគ្មានចន្លោះ ឬត្រួតគ្នា។");
                }
            }
            previousMaximum = bracket.getMaxAmount();
        }
    }
}
