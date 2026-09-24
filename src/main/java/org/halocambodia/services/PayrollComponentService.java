package org.halocambodia.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollModels.ComponentInput;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PageDialogLayout-compatible service for payroll components. */
@Service
@Transactional
public class PayrollComponentService implements GenericService<PayrollComponent> {

    private final PayrollComponentRepository repository;
    private final PayrollService payrollService;

    public PayrollComponentService(
            PayrollComponentRepository repository,
            PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollComponent> list(
            Pageable pageable,
            Specification<PayrollComponent> specification) {
        Page<PayrollComponent> page = repository.findAll(specification, pageable);
        populateRuleReferences(page.getContent());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollComponent> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public PayrollComponent update(PayrollComponent entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Payroll component is required. | ត្រូវការធាតុប្រាក់បៀវត្ស។");
        }

        Long id = payrollService.saveComponent(new ComponentInput(
                entity.getId(),
                entity.getComponentCode(),
                entity.getComponentNameEn(),
                entity.getComponentNameKh(),
                entity.getComponentType(),
                entity.getCalculationMethod(),
                entity.isAllowManualEntry(),
                entity.isTaxable(),
                entity.isSubjectToNssf(),
                entity.isSubjectToSeniority(),
                entity.isActive(),
                entity.getGlAccountCode(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder()));
        return reload(id);
    }

    @Override
    public void delete(Set<PayrollComponent> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (PayrollComponent entity : entities) {
            if (entity == null || entity.getId() == null) {
                throw new IllegalArgumentException(
                        "Select a saved payroll component to delete. "
                                + "| សូមជ្រើសធាតុប្រាក់បៀវត្សដែលបានរក្សាទុកដើម្បីលុប។");
            }
            payrollService.deleteComponent(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollComponent> findAll(Specification<PayrollComponent> specification) {
        List<PayrollComponent> rows = specification == null
                ? repository.findAll()
                : repository.findAll(specification);
        populateRuleReferences(rows);
        return rows;
    }

    @Transactional(readOnly = true)
    public Optional<PayrollComponent> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<PayrollComponent> result = repository.findById(id);
        result.ifPresent(component -> populateRuleReferences(List.of(component)));
        return result;
    }

    private PayrollComponent reload(Long id) {
        PayrollComponent component = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved payroll component could not be reloaded. "
                                + "| មិនអាចផ្ទុកធាតុប្រាក់បៀវត្សដែលបានរក្សាទុកឡើងវិញ។"));
        populateRuleReferences(List.of(component));
        return component;
    }

    private void populateRuleReferences(List<PayrollComponent> components) {
        if (components == null || components.isEmpty()) {
            return;
        }

        Map<Long, String> references = new HashMap<>();
        payrollService.findPayrollRules(null).stream()
                .filter(rule -> rule.componentId() != null)
                .forEach(rule -> references.merge(
                        rule.componentId(),
                        rule.ruleYear() + " · " + rule.code(),
                        (existing, next) -> existing + ", " + next));

        for (PayrollComponent component : components) {
            component.setUsedByPayrollRules(
                    references.getOrDefault(component.getId(), "-"));
        }
    }
}
