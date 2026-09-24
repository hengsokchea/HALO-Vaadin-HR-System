package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.PayrollModels.TaxConfigInput;
import org.halocambodia.data.PayrollModels.TaxBracketInput;
import org.halocambodia.data.PayrollTaxBracket;
import org.halocambodia.data.PayrollTaxConfig;
import org.halocambodia.data.PayrollTaxConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PageDialogLayout-compatible service for yearly salary-tax configurations.
 *
 * The existing PayrollService remains the single owner of payroll validation,
 * permissions, bracket-completeness checks and calculated-run invalidation.
 */
@Service
@Transactional
public class PayrollTaxConfigService implements GenericService<PayrollTaxConfig> {

    private final PayrollTaxConfigRepository repository;
    private final PayrollService payrollService;

    public PayrollTaxConfigService(
            PayrollTaxConfigRepository repository,
            PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollTaxConfig> list(
            Pageable pageable,
            Specification<PayrollTaxConfig> specification) {
        return repository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollTaxConfig> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public PayrollTaxConfig update(PayrollTaxConfig entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Tax configuration is required. | ត្រូវការការកំណត់ពន្ធ។");
        }

        Long id = payrollService.saveTaxConfig(new TaxConfigInput(
                entity.getId(),
                entity.getTaxYear(),
                entity.getCurrency(),
                entity.getDependentAllowance(),
                entity.getNonResidentRate(),
                entity.isActive(),
                entity.getSourceReference(),
                entity.getNotes()));

        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved tax year could not be reloaded. | មិនអាចផ្ទុកឆ្នាំពន្ធដែលបានរក្សាទុកឡើងវិញ។"));
    }

    /**
     * Saves a Tax Year and the complete bracket list in one database
     * transaction. This is used by PayrollTaxRateView so cancelling its dialog
     * never leaves partially edited bracket rows in the database.
     */
    public PayrollTaxConfig updateWithBrackets(
            PayrollTaxConfig entity,
            List<PayrollTaxBracket> brackets) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Tax configuration is required. | ត្រូវការការកំណត់ពន្ធ។");
        }

        List<TaxBracketInput> inputs = brackets == null
                ? List.of()
                : brackets.stream()
                        .map(bracket -> new TaxBracketInput(
                                bracket.getId(),
                                entity.getId(),
                                bracket.getBracketOrder(),
                                bracket.getMinAmount(),
                                bracket.getMaxAmount(),
                                bracket.getTaxRate(),
                                bracket.getDeductionAmount()))
                        .toList();

        Long id = payrollService.saveTaxConfigWithBrackets(
                new TaxConfigInput(
                        entity.getId(),
                        entity.getTaxYear(),
                        entity.getCurrency(),
                        entity.getDependentAllowance(),
                        entity.getNonResidentRate(),
                        entity.isActive(),
                        entity.getSourceReference(),
                        entity.getNotes()),
                inputs);

        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved tax year could not be reloaded. | មិនអាចផ្ទុកឆ្នាំពន្ធដែលបានរក្សាទុកឡើងវិញ។"));
    }

    @Override
    public void delete(Set<PayrollTaxConfig> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (PayrollTaxConfig entity : entities) {
            if (entity == null || entity.getId() == null) {
                throw new IllegalArgumentException(
                        "Select a saved tax year to delete. | សូមជ្រើសឆ្នាំពន្ធដែលបានរក្សាទុកដើម្បីលុប។");
            }
            payrollService.deleteTaxConfig(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollTaxConfig> findAll(Specification<PayrollTaxConfig> specification) {
        return specification == null ? repository.findAll() : repository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollTaxConfig> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }
}
