package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.PayrollModels.TaxBracketInput;
import org.halocambodia.data.PayrollTaxBracket;
import org.halocambodia.data.PayrollTaxBracketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entity service for progressive tax brackets displayed inside a tax-year row.
 */
@Service
@Transactional
public class PayrollTaxBracketService implements GenericService<PayrollTaxBracket> {

    private final PayrollTaxBracketRepository repository;
    private final PayrollService payrollService;

    public PayrollTaxBracketService(
            PayrollTaxBracketRepository repository,
            PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollTaxBracket> list(
            Pageable pageable,
            Specification<PayrollTaxBracket> specification) {
        return repository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollTaxBracket> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public PayrollTaxBracket update(PayrollTaxBracket entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Tax bracket is required. | ត្រូវការថ្នាក់ពន្ធ។");
        }

        Long id = payrollService.saveTaxBracket(new TaxBracketInput(
                entity.getId(),
                entity.getPayrollTaxConfigId(),
                entity.getBracketOrder(),
                entity.getMinAmount(),
                entity.getMaxAmount(),
                entity.getTaxRate(),
                entity.getDeductionAmount()));

        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved tax bracket could not be reloaded. | មិនអាចផ្ទុកថ្នាក់ពន្ធដែលបានរក្សាទុកឡើងវិញ។"));
    }

    @Override
    public void delete(Set<PayrollTaxBracket> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (PayrollTaxBracket entity : entities) {
            if (entity == null || entity.getId() == null) {
                throw new IllegalArgumentException(
                        "Select a saved tax bracket to delete. | សូមជ្រើសថ្នាក់ពន្ធដែលបានរក្សាទុកដើម្បីលុប។");
            }
            payrollService.deleteTaxBracket(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollTaxBracket> findAll(Specification<PayrollTaxBracket> specification) {
        return specification == null ? repository.findAll() : repository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollTaxBracket> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PayrollTaxBracket> findByTaxConfigId(Long taxConfigId) {
        return taxConfigId == null
                ? List.of()
                : repository.findByPayrollTaxConfigIdOrderByBracketOrderAsc(taxConfigId);
    }

    @Transactional(readOnly = true)
    public int nextOrder(Long taxConfigId) {
        return findByTaxConfigId(taxConfigId).stream()
                .map(PayrollTaxBracket::getBracketOrder)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }
}
