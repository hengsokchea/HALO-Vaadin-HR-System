package org.halocambodia.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollPaymentSetting;
import org.halocambodia.data.PayrollPaymentSettingRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.payroll.PayrollPaymentSettingView;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PayrollPaymentSettingService implements GenericService<PayrollPaymentSetting> {

    private static final Set<String> FREQUENCIES = Set.of("MONTHLY", "SEMI_MONTHLY");
    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

    private final PayrollPaymentSettingRepository repository;
    private final AuthenticatedUser authenticatedUser;

    public PayrollPaymentSettingService(
            PayrollPaymentSettingRepository repository,
            AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollPaymentSetting> list(
            Pageable pageable, Specification<PayrollPaymentSetting> specification) {
        return specification == null
                ? repository.findAll(pageable)
                : repository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollPaymentSetting> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public PayrollPaymentSetting update(PayrollPaymentSetting setting) {
        if (setting == null) {
            throw new IllegalArgumentException(
                    "Payment setting is required. | ត្រូវការការកំណត់ការបើកប្រាក់។");
        }

        boolean isNew = setting.getId() == null;
        requirePermission(isNew ? AccessPageType.INSERTED_PAGE : AccessPageType.UPDATED_PAGE);
        // Current maintenance uses one company rule with an optional Shift exception.
        // Legacy employee-level settings are not used by payroll resolution.
        setting.setEmployee(null);
        normalizeAndValidate(setting);

        if (setting.isActive()) {
            Long settingId = setting.getId() == null ? -1L : setting.getId();
            LocalDate effectiveTo = setting.getEffectiveTo() == null
                    ? OPEN_ENDED_DATE : setting.getEffectiveTo();

            long overlaps = repository.countOverlappingActiveSettings(
                    settingId, setting.getEffectiveFrom(), effectiveTo);

            if (overlaps > 0) {
                throw new IllegalArgumentException(
                        "An active company payment rule already covers part of this date range. "
                                + "Only one active rule may apply for a date. "
                                + "| មានច្បាប់បើកប្រាក់សកម្មរួចហើយសម្រាប់រយៈពេលនេះ។");
            }
        }

        User currentUser = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in."));
        if (isNew) {
            setting.setUserCreated(currentUser);
        }
        setting.setUserUpdated(currentUser);
        return repository.save(setting);
    }

    @Override
    public void delete(Set<PayrollPaymentSetting> settings) {
        requirePermission(AccessPageType.DELETED_PAGE);
        if (settings == null || settings.isEmpty()) {
            return;
        }
        repository.deleteAll(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollPaymentSetting> findAll(
            Specification<PayrollPaymentSetting> specification) {
        return specification == null ? repository.findAll() : repository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollPaymentSetting> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    private void normalizeAndValidate(PayrollPaymentSetting setting) {
        String frequency = setting.getPaymentFrequency() == null
                ? "" : setting.getPaymentFrequency().trim().toUpperCase(Locale.ROOT);
        if (!FREQUENCIES.contains(frequency)) {
            throw new IllegalArgumentException(
                    "Payment frequency must be MONTHLY or SEMI_MONTHLY. "
                            + "| ប្រេកង់បើកប្រាក់ត្រូវជា MONTHLY ឬ SEMI_MONTHLY។");
        }
        if (setting.getEffectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "Effective From is required. | ត្រូវបញ្ចូលថ្ងៃចាប់ផ្ដើមអនុវត្ត។");
        }
        if (setting.getEffectiveTo() != null
                && setting.getEffectiveTo().isBefore(setting.getEffectiveFrom())) {
            throw new IllegalArgumentException(
                    "Effective To cannot be before Effective From. "
                            + "| ថ្ងៃបញ្ចប់អនុវត្តមិនអាចមុនថ្ងៃចាប់ផ្ដើមបានទេ។");
        }

        BigDecimal percent = setting.getFirstPaymentPercent() == null
                ? BigDecimal.ZERO : setting.getFirstPaymentPercent();

        if ("SEMI_MONTHLY".equals(frequency)) {
            // SEMI_MONTHLY is company-wide. Shift is intentionally disabled/blank.
            setting.setShift(null);
            validateFirstPaymentPercent(percent);
        } else if (setting.getShift() == null) {
            // MONTHLY + blank Shift = every staff member is paid monthly.
            percent = BigDecimal.ZERO;
        } else {
            // MONTHLY + Shift = company remains monthly, selected Shift is the
            // SEMI_MONTHLY exception and therefore needs a first-installment percent.
            validateFirstPaymentPercent(percent);
        }

        setting.setPaymentFrequency(frequency);
        setting.setFirstPaymentPercent(percent.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private static void validateFirstPaymentPercent(BigDecimal percent) {
        if (percent == null || percent.signum() <= 0
                || percent.compareTo(new BigDecimal("100")) >= 0) {
            throw new IllegalArgumentException(
                    "First payment percent must be greater than 0 and less than 100 when semi-monthly payment applies. "
                            + "| ភាគរយបើកលើកទីមួយត្រូវធំជាង 0 និងតូចជាង 100 នៅពេលបើកពីរដង។");
        }
    }

    private void requirePermission(AccessPageType action) {
        boolean allowed = authenticatedUser.hasPage(PayrollView.class, action);
        if (!allowed) {
            // Allow installations that register a dedicated permission for the settings page.
            allowed = authenticatedUser.hasPage(PayrollPaymentSettingView.class, action);
        }
        if (!allowed) {
            throw new IllegalArgumentException(
                    "You do not have permission for this payroll payment operation. "
                            + "| អ្នកមិនមានសិទ្ធិសម្រាប់ប្រតិបត្តិការនេះទេ។");
        }
    }
}
