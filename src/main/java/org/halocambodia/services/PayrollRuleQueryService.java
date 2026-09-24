package org.halocambodia.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollModels.PayrollRuleRow;
import org.halocambodia.data.PayrollPolicyRule;
import org.halocambodia.data.PayrollPolicyRuleRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared read model for yearly payroll policy rules without JdbcTemplate. */
@Service
public class PayrollRuleQueryService {

    private final PayrollPolicyRuleRepository payrollPolicyRuleRepository;
    private final PayrollComponentRepository payrollComponentRepository;

    public PayrollRuleQueryService(
            PayrollPolicyRuleRepository payrollPolicyRuleRepository,
            PayrollComponentRepository payrollComponentRepository) {
        this.payrollPolicyRuleRepository = payrollPolicyRuleRepository;
        this.payrollComponentRepository = payrollComponentRepository;
    }

    @Transactional(readOnly = true)
    public List<PayrollRuleRow> find(@Nullable Integer ruleYear) {
        List<PayrollPolicyRule> rules = ruleYear == null
                ? payrollPolicyRuleRepository.findAllByOrderByRuleYearDescSortOrderAscRuleCodeAsc()
                : payrollPolicyRuleRepository.findByRuleYearOrderBySortOrderAscRuleCodeAsc(ruleYear);

        Map<Long, PayrollComponent> components = loadComponents(rules);

        return rules.stream()
                .map(rule -> toRow(rule, components.get(rule.getPayrollComponentId())))
                .toList();
    }

    private Map<Long, PayrollComponent> loadComponents(Collection<PayrollPolicyRule> rules) {
        List<Long> componentIds = rules.stream()
                .map(PayrollPolicyRule::getPayrollComponentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (componentIds.isEmpty()) {
            return Map.of();
        }

        return payrollComponentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(PayrollComponent::getId, Function.identity()));
    }

    private static PayrollRuleRow toRow(
            PayrollPolicyRule rule,
            @Nullable PayrollComponent component) {
        return new PayrollRuleRow(
                rule.getId(),
                rule.getRuleYear() == null ? 0 : rule.getRuleYear(),
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getRuleType(),
                rule.getRateAmount(),
                rule.getRateCurrency(),
                rule.getRateUnit(),
                rule.getMultiplier(),
                rule.getWorkdayDivisor(),
                rule.getPayPercentage(),
                rule.getEntitlementDays(),
                rule.getServiceYearsPerExtraDay(),
                rule.getInitialFullPayMonths(),
                rule.getMaxPaidMonths(),
                rule.getPayrollComponentId(),
                component == null ? null : component.getComponentCode(),
                component == null ? null : component.getComponentNameEn(),
                rule.getCalculationSource(),
                rule.isActive(),
                rule.getSortOrder() == null ? 0 : rule.getSortOrder(),
                rule.getDescription());
    }
}
