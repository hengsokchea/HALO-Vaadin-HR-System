package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.PayrollModels.NssfBandInput;
import org.halocambodia.data.PayrollNssfWageBand;
import org.halocambodia.data.PayrollNssfWageBandRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Entity service for NSSF healthcare/risk contributory-wage bands. */
@Service
@Transactional
public class PayrollNssfWageBandService implements GenericService<PayrollNssfWageBand> {

    private final PayrollNssfWageBandRepository repository;
    private final PayrollService payrollService;

    public PayrollNssfWageBandService(
            PayrollNssfWageBandRepository repository,
            PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollNssfWageBand> list(
            Pageable pageable,
            Specification<PayrollNssfWageBand> specification) {
        return repository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollNssfWageBand> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public PayrollNssfWageBand update(PayrollNssfWageBand entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "NSSF wage band is required. | ត្រូវការកម្រិតប្រាក់ឈ្នួល ប.ស.ស.។");
        }
        Long id = payrollService.saveNssfBand(new NssfBandInput(
                entity.getId(),
                entity.getPayrollNssfConfigId(),
                entity.getBandOrder(),
                entity.getMaxSalary(),
                entity.getContributoryWage()));
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved NSSF wage band could not be reloaded. "
                                + "| មិនអាចផ្ទុកកម្រិតប្រាក់ឈ្នួល ប.ស.ស. ឡើងវិញ។"));
    }

    @Override
    public void delete(Set<PayrollNssfWageBand> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (PayrollNssfWageBand entity : entities) {
            if (entity == null || entity.getId() == null) {
                throw new IllegalArgumentException(
                        "Select a saved NSSF wage band to delete. "
                                + "| សូមជ្រើសកម្រិតប្រាក់ឈ្នួល ប.ស.ស. ដែលបានរក្សាទុកដើម្បីលុប។");
            }
            payrollService.deleteNssfBand(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollNssfWageBand> findAll(Specification<PayrollNssfWageBand> specification) {
        return specification == null ? repository.findAll() : repository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollNssfWageBand> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PayrollNssfWageBand> findByConfigId(Long configId) {
        return configId == null
                ? List.of()
                : repository.findByPayrollNssfConfigIdOrderByBandOrderAsc(configId);
    }
}
