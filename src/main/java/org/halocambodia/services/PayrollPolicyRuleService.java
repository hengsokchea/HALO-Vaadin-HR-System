package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.PayrollModels.PayrollRuleInput;
import org.halocambodia.data.PayrollPolicyRule;
import org.halocambodia.data.PayrollPolicyRuleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PageDialogLayout-compatible service for yearly payroll policy rules. */
@Service
@Transactional
public class PayrollPolicyRuleService implements GenericService<PayrollPolicyRule> {

    private final PayrollPolicyRuleRepository repository;
    private final PayrollService payrollService;

    public PayrollPolicyRuleService(
            PayrollPolicyRuleRepository repository,
            PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollPolicyRule> list(
            Pageable pageable,
            Specification<PayrollPolicyRule> specification) {
        return repository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollPolicyRule> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public PayrollPolicyRule update(PayrollPolicyRule entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Payroll rule is required. | ត្រូវការច្បាប់ប្រាក់បៀវត្ស។");
        }

        Long id = payrollService.savePayrollRule(new PayrollRuleInput(
                entity.getId(),
                entity.getRuleYear(),
                entity.getRuleCode(),
                entity.getRuleName(),
                entity.getRuleType(),
                entity.getRateAmount(),
                entity.getRateCurrency(),
                entity.getRateUnit(),
                entity.getMultiplier(),
                entity.getWorkdayDivisor(),
                entity.getPayPercentage(),
                entity.getEntitlementDays(),
                entity.getServiceYearsPerExtraDay(),
                entity.getInitialFullPayMonths(),
                entity.getMaxPaidMonths(),
                entity.getPayrollComponentId(),
                entity.getCalculationSource(),
                entity.isActive(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                entity.getDescription()));

        return reload(id);
    }

    @Override
    public void delete(Set<PayrollPolicyRule> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (PayrollPolicyRule entity : entities) {
            if (entity == null || entity.getId() == null) {
                throw new IllegalArgumentException(
                        "Select a saved payroll rule to delete. "
                                + "| សូមជ្រើសច្បាប់ប្រាក់បៀវត្សដែលបានរក្សាទុកដើម្បីលុប។");
            }
            payrollService.deletePayrollRule(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollPolicyRule> findAll(Specification<PayrollPolicyRule> specification) {
        return specification == null ? repository.findAll() : repository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollPolicyRule> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    private PayrollPolicyRule reload(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved payroll rule could not be reloaded. "
                                + "| មិនអាចផ្ទុកច្បាប់ប្រាក់បៀវត្សដែលបានរក្សាទុកឡើងវិញ។"));
    }
}
