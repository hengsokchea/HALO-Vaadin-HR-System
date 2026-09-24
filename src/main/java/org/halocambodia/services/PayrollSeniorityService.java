package org.halocambodia.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollSeniorityCalculation;
import org.halocambodia.data.PayrollSeniorityCalculationDataRepository;
import org.halocambodia.data.PayrollSeniorityCalculationDataRepository.SeniorityRunContext;
import org.halocambodia.data.PayrollSeniorityCalculationRepository;
import org.halocambodia.data.PayrollSeniorityRule;
import org.halocambodia.data.PayrollSeniorityRuleRepository;
import org.halocambodia.enums.PayrollRunType;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns seniority rule administration and the auditable June/December payroll
 * calculation. Database-intensive operations are delegated to JPA repositories;
 * this service contains no JdbcTemplate usage.
 */
@Service
@Transactional
public class PayrollSeniorityService implements GenericService<PayrollSeniorityRule> {

    private static final Set<Integer> VALID_DIVISORS = Set.of(22, 24, 26);

    private final PayrollSeniorityRuleRepository ruleRepository;
    private final PayrollSeniorityCalculationRepository calculationRepository;
    private final PayrollSeniorityCalculationDataRepository calculationDataRepository;
    private final PayrollComponentRepository componentRepository;
    private final AuthenticatedUser authenticatedUser;

    public PayrollSeniorityService(
            PayrollSeniorityRuleRepository ruleRepository,
            PayrollSeniorityCalculationRepository calculationRepository,
            PayrollSeniorityCalculationDataRepository calculationDataRepository,
            PayrollComponentRepository componentRepository,
            AuthenticatedUser authenticatedUser) {
        this.ruleRepository = ruleRepository;
        this.calculationRepository = calculationRepository;
        this.calculationDataRepository = calculationDataRepository;
        this.componentRepository = componentRepository;
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollSeniorityRule> list(
            Pageable pageable,
            Specification<PayrollSeniorityRule> specification) {
        return specification == null
                ? ruleRepository.findAll(pageable)
                : ruleRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<PayrollSeniorityRule> specification) {
        return specification == null
                ? ruleRepository.count()
                : ruleRepository.count(specification);
    }

    @Override
    public PayrollSeniorityRule update(PayrollSeniorityRule rule) {
        requirePermission(rule != null && rule.getId() == null
                ? AccessPageType.INSERTED_PAGE
                : AccessPageType.UPDATED_PAGE);
        validateRule(rule);
        validateNoOverlap(rule);

        User user = currentUser();
        if (rule.getId() == null) {
            rule.setUserCreated(user);
        }
        rule.setUserUpdated(user);
        rule.setSourceReference(blankToNull(rule.getSourceReference()));
        rule.setNotes(blankToNull(rule.getNotes()));

        try {
            PayrollSeniorityRule saved = ruleRepository.saveAndFlush(rule);
            invalidateCalculatedRuns();
            return reload(saved.getId());
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException(
                    "This seniority rule was changed by another user. Refresh and try again. "
                            + "| ច្បាប់អតីតភាពនេះត្រូវបានកែដោយអ្នកប្រើផ្សេង។ សូមផ្ទុកឡើងវិញ។",
                    ex);
        }
    }

    @Override
    public void delete(Set<PayrollSeniorityRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        requirePermission(AccessPageType.DELETED_PAGE);

        for (PayrollSeniorityRule rule : rules) {
            if (rule == null || rule.getId() == null) {
                throw new IllegalArgumentException(
                        "Select a saved seniority rule. | សូមជ្រើសច្បាប់អតីតភាពដែលបានរក្សាទុក។");
            }
            if (calculationRepository.countBySeniorityRuleId(rule.getId()) > 0) {
                throw new IllegalStateException(
                        "This rule already has payroll calculation history. Deactivate it instead. "
                                + "| ច្បាប់នេះមានប្រវត្តិគណនាប្រាក់បៀវត្សរួចហើយ។ សូមបិទសកម្មភាពជំនួសឱ្យការលុប។");
            }
            ruleRepository.deleteById(rule.getId());
        }

        ruleRepository.flush();
        invalidateCalculatedRuns();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollSeniorityRule> findAll(
            Specification<PayrollSeniorityRule> specification) {
        return specification == null
                ? ruleRepository.findAll()
                : ruleRepository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<PayrollSeniorityRule> findById(Long id) {
        return id == null ? Optional.empty() : ruleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PayrollSeniorityCalculation> findCalculations(Long ruleId) {
        return ruleId == null
                ? List.of()
                : calculationRepository.findResults(ruleId);
    }

    @Transactional(readOnly = true)
    public List<PayrollSeniorityCalculation> findCalculationsForRun(
            Long runId, Long payrollEmployeeId) {
        return runId == null
                ? List.of()
                : calculationRepository.findByPayrollRun(runId, payrollEmployeeId);
    }

    /**
     * Replaces seniority calculations for all included employees in a run, or
     * for one payroll employee when selective recalculation is used.
     */
    public int calculate(Long runId, Long payrollEmployeeId) {
        SeniorityRunContext context = loadRunContext(runId);
        clearCalculatedSeniority(runId, payrollEmployeeId);

        // Termination seniority has different legal conditions and needs a
        // termination-reason source. It is deliberately not guessed here.
        if (PayrollRunType.FINAL_PAYMENT.matches(context.runType())) {
            return 0;
        }

        if (context.payrollMonth() != 6 && context.payrollMonth() != 12) {
            return 0;
        }

        LocalDate semesterStart = context.payrollMonth() == 6
                ? LocalDate.of(context.payrollYear(), 1, 1)
                : LocalDate.of(context.payrollYear(), 7, 1);
        LocalDate semesterEnd = context.payrollMonth() == 6
                ? LocalDate.of(context.payrollYear(), 6, 30)
                : LocalDate.of(context.payrollYear(), 12, 31);

        long ruleCount = calculationDataRepository.countActiveRules(
                semesterEnd,
                context.payrollMonth());
        if (ruleCount == 0) {
            throw new IllegalStateException(
                    "No active Seniority Payment Rule covers payroll month "
                            + context.payrollYear() + "-"
                            + String.format("%02d", context.payrollMonth()) + ". "
                            + "Configure the eligible UDC contract type and workday divisor first. "
                            + "| មិនមានច្បាប់ប្រាក់បំណាច់អតីតភាពសកម្មសម្រាប់ខែនេះទេ។ "
                            + "សូមកំណត់ប្រភេទកិច្ចសន្យា UDC និងចំនួនថ្ងៃធ្វើការមុនគណនា។");
        }

        long overlapping = calculationDataRepository.countOverlappingActiveRules(
                semesterEnd,
                context.payrollMonth());
        if (overlapping > 0) {
            throw new IllegalStateException(
                    "More than one active Seniority Payment Rule covers the same contract type. "
                            + "Fix the overlapping effective dates before calculating payroll. "
                            + "| មានច្បាប់អតីតភាពសកម្មច្រើនជាងមួយសម្រាប់ប្រភេទកិច្ចសន្យាដូចគ្នា។");
        }

        Long componentId = requireSeniorityComponent();
        requireAtLeastOneEligibleEarningComponent();
        Long userId = currentUserId();

        int inserted = calculationDataRepository.insertSeniorityCalculations(
                runId,
                payrollEmployeeId,
                semesterStart,
                semesterEnd,
                userId);

        calculationDataRepository.insertSeniorityPayrollItems(
                runId,
                payrollEmployeeId,
                componentId,
                userId);

        return inserted;
    }

    private void clearCalculatedSeniority(Long runId, Long payrollEmployeeId) {
        calculationDataRepository.deleteSeniorityItems(runId, payrollEmployeeId);
        calculationDataRepository.deleteSeniorityCalculations(runId, payrollEmployeeId);
    }

    private SeniorityRunContext loadRunContext(Long runId) {
        if (runId == null) {
            throw new IllegalArgumentException(
                    "Payroll run is required. | ត្រូវការដំណើរការប្រាក់បៀវត្ស។");
        }
        SeniorityRunContext context = calculationDataRepository.findRunContext(runId);
        if (context == null) {
            throw new IllegalArgumentException(
                    "Payroll run not found. | រកមិនឃើញដំណើរការប្រាក់បៀវត្ស។");
        }
        return context;
    }

    private Long requireSeniorityComponent() {
        PayrollComponent component = componentRepository
                .findByComponentCodeIgnoreCase("SENIORITY_PAYMENT")
                .orElseThrow(() -> new IllegalStateException(
                        "Active SENIORITY_PAYMENT EARNING/FORMULA component is required. "
                                + "Run payroll_seniority_payment.sql first. "
                                + "| ត្រូវមានធាតុ SENIORITY_PAYMENT ដែលសកម្ម។"));

        if (!component.isActive()
                || !"EARNING".equalsIgnoreCase(component.getComponentType())
                || !"FORMULA".equalsIgnoreCase(component.getCalculationMethod())) {
            throw new IllegalStateException(
                    "Active SENIORITY_PAYMENT EARNING/FORMULA component is required. "
                            + "Run payroll_seniority_payment.sql first. "
                            + "| ត្រូវមានធាតុ SENIORITY_PAYMENT ដែលសកម្ម។");
        }
        return component.getId();
    }

    private void requireAtLeastOneEligibleEarningComponent() {
        if (calculationDataRepository.countEligibleEarningComponents() == 0) {
            throw new IllegalStateException(
                    "No earning component is included in the seniority average. "
                            + "Enable 'Seniority Base' on BASIC_SALARY and any eligible benefits. "
                            + "| មិនមានធាតុចំណូលសម្រាប់មូលដ្ឋានគណនាអតីតភាពទេ។");
        }
    }

    private void validateRule(PayrollSeniorityRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException(
                    "Seniority rule is required. | ត្រូវការច្បាប់អតីតភាព។");
        }
        if (rule.getEligibleContractType() == null
                || rule.getEligibleContractType().getId() == null) {
            throw new IllegalArgumentException(
                    "Eligible contract type is required. | ត្រូវជ្រើសប្រភេទកិច្ចសន្យាដែលមានសិទ្ធិ។");
        }
        if (rule.getEffectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "Effective-from date is required. | ត្រូវបញ្ចូលថ្ងៃចាប់ផ្តើមអនុវត្ត។");
        }
        if (rule.getEffectiveTo() != null
                && rule.getEffectiveTo().isBefore(rule.getEffectiveFrom())) {
            throw new IllegalArgumentException(
                    "Effective-to date cannot be before effective-from date. "
                            + "| ថ្ងៃបញ្ចប់មិនអាចមុនថ្ងៃចាប់ផ្តើម។");
        }
        if (!Integer.valueOf(6).equals(rule.getFirstPaymentMonth())
                || !Integer.valueOf(12).equals(rule.getSecondPaymentMonth())) {
            throw new IllegalArgumentException(
                    "Current Cambodia seniority payment uses June and December. "
                            + "| ប្រាក់បំណាច់អតីតភាពបច្ចុប្បន្នបើកនៅខែមិថុនា និងធ្នូ។");
        }
        if (rule.getDaysPerPayment() == null
                || rule.getDaysPerPayment().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Days per payment must be greater than zero. | ចំនួនថ្ងៃត្រូវធំជាងសូន្យ។");
        }
        if (rule.getMinimumEligibleDays() == null
                || rule.getMinimumEligibleDays() <= 0) {
            throw new IllegalArgumentException(
                    "Minimum eligible days must be greater than zero. | ចំនួនថ្ងៃអប្បបរមាត្រូវធំជាងសូន្យ។");
        }
        if (rule.getWorkdayDivisor() == null
                || !VALID_DIVISORS.contains(rule.getWorkdayDivisor())) {
            throw new IllegalArgumentException(
                    "Workday divisor must be 22, 24, or 26. | ចំនួនថ្ងៃធ្វើការត្រូវជា 22, 24 ឬ 26។");
        }
    }

    private void validateNoOverlap(PayrollSeniorityRule rule) {
        if (!rule.isActive()) {
            return;
        }

        long overlaps = calculationDataRepository.countRuleOverlap(
                rule.getEligibleContractType().getId(),
                rule.getId(),
                rule.getEffectiveTo(),
                rule.getEffectiveFrom());
        if (overlaps > 0) {
            throw new IllegalArgumentException(
                    "An active seniority rule already overlaps this contract type and date range. "
                            + "| មានច្បាប់អតីតភាពសកម្មត្រួតគ្នាសម្រាប់ប្រភេទកិច្ចសន្យា និងកាលបរិច្ឆេទនេះ។");
        }
    }

    private PayrollSeniorityRule reload(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Saved seniority rule could not be reloaded. "
                                + "| មិនអាចផ្ទុកច្បាប់អតីតភាពដែលបានរក្សាទុកឡើងវិញ។"));
    }

    private void invalidateCalculatedRuns() {
        calculationDataRepository.invalidateCalculatedRuns();
    }

    private void requirePermission(AccessPageType type) {
        if (!authenticatedUser.hasPage(PayrollView.class, type)) {
            throw new IllegalArgumentException(
                    "You do not have permission for this payroll operation. "
                            + "| អ្នកមិនមានសិទ្ធិសម្រាប់ប្រតិបត្តិការប្រាក់បៀវត្សនេះទេ។");
        }
    }

    private User currentUser() {
        return authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException(
                        "User is not logged in. | អ្នកប្រើមិនបានចូលប្រព័ន្ធ។"));
    }

    private Long currentUserId() {
        return currentUser().getId();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
