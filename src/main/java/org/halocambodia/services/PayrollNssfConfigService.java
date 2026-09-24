package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.PayrollModels.NssfBandInput;
import org.halocambodia.data.PayrollModels.NssfConfigInput;
import org.halocambodia.data.PayrollNssfConfig;
import org.halocambodia.data.PayrollNssfConfigRepository;
import org.halocambodia.data.PayrollNssfWageBand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PageDialogLayout-compatible service for effective-dated NSSF rules. */
@Service
@Transactional
public class PayrollNssfConfigService implements GenericService<PayrollNssfConfig> {

    private final PayrollNssfConfigRepository repository;
    private final PayrollService payrollService;

    public PayrollNssfConfigService(
            PayrollNssfConfigRepository repository,
            PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollNssfConfig> list(
            Pageable pageable,
            Specification<PayrollNssfConfig> specification) {
        return repository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollNssfConfig> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public PayrollNssfConfig update(PayrollNssfConfig entity) {
        requireEntity(entity);
        Long id = payrollService.saveNssfConfig(toInput(entity));
        return reload(id);
    }

    public PayrollNssfConfig updateWithBands(
            PayrollNssfConfig entity,
            List<PayrollNssfWageBand> bands) {
        requireEntity(entity);
        List<NssfBandInput> inputs = bands == null
                ? List.of()
                : bands.stream()
                        .map(band -> new NssfBandInput(
                                band.getId(),
                                entity.getId(),
                                band.getBandOrder(),
                                band.getMaxSalary(),
                                band.getContributoryWage()))
                        .toList();
        Long id = payrollService.saveNssfConfigWithBands(toInput(entity), inputs);
        return reload(id);
    }

    @Override
    public void delete(Set<PayrollNssfConfig> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (PayrollNssfConfig entity : entities) {
            if (entity == null || entity.getId() == null) {
                throw new IllegalArgumentException(
                        "Select a saved NSSF rule to delete. "
                                + "| សូមជ្រើសច្បាប់ ប.ស.ស. ដែលបានរក្សាទុកដើម្បីលុប។");
            }
            payrollService.deleteNssfConfig(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollNssfConfig> findAll(Specification<PayrollNssfConfig> specification) {
        return specification == null ? repository.findAll() : repository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollNssfConfig> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    private NssfConfigInput toInput(PayrollNssfConfig entity) {
        return new NssfConfigInput(
                entity.getId(),
                entity.getEligibleContractType() == null
                        ? null
                        : entity.getEligibleContractType().getId(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getCurrency(),
                entity.getHealthEmployeeRate(),
                entity.getHealthEmployerRate(),
                entity.getRiskEmployeeRate(),
                entity.getRiskEmployerRate(),
                entity.getPensionEmployeeRate(),
                entity.getPensionEmployerRate(),
                entity.getPensionMinWage(),
                entity.getPensionMaxWage(),
                entity.isActive(),
                entity.getSourceReference(),
                entity.getNotes());
    }

    private PayrollNssfConfig reload(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved NSSF rule could not be reloaded. "
                                + "| មិនអាចផ្ទុកច្បាប់ ប.ស.ស. ដែលបានរក្សាទុកឡើងវិញ។"));
    }

    private static void requireEntity(PayrollNssfConfig entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "NSSF rule is required. | ត្រូវការច្បាប់ ប.ស.ស.។");
        }
    }
}
