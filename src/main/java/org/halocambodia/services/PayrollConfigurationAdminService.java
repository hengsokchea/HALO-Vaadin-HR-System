package org.halocambodia.services;

import static org.halocambodia.data.PayrollModels.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.ContractType;
import org.halocambodia.data.ContractTypeRepository;
import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollEmployeeItemRepository;
import org.halocambodia.data.PayrollNssfConfig;
import org.halocambodia.data.PayrollNssfConfigRepository;
import org.halocambodia.data.PayrollNssfWageBand;
import org.halocambodia.data.PayrollNssfWageBandRepository;
import org.halocambodia.data.PayrollPeriod;
import org.halocambodia.data.PayrollPeriodRepository;
import org.halocambodia.data.PayrollPolicyRule;
import org.halocambodia.data.PayrollPolicyRuleRepository;
import org.halocambodia.data.PayrollRunRepository;
import org.halocambodia.data.PayrollTaxBracket;
import org.halocambodia.data.PayrollTaxBracketRepository;
import org.halocambodia.data.PayrollTaxConfig;
import org.halocambodia.data.PayrollTaxConfigRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns payroll configuration administration and validation.
 *
 * <p>PayrollService delegates to this class so the main payroll workflow
 * service no longer owns configuration SQL or persistence details.</p>
 */
@Service
public class PayrollConfigurationAdminService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Set<String> SYSTEM_COMPONENT_CODES = Set.of(
            "BASIC_SALARY", "SALARY_TAX",
            "NSSF_EMPLOYEE", "NSSF_EMPLOYER",
            "NSSF_HEALTH_EMPLOYEE", "NSSF_HEALTH_EMPLOYER",
            "NSSF_RISK_EMPLOYEE", "NSSF_RISK_EMPLOYER",
            "NSSF_PENSION_EMPLOYEE", "NSSF_PENSION_EMPLOYER",
            "ABSENCE_DEDUCTION", "SICK_LEAVE_DEDUCTION", "MATERNITY_DEDUCTION", "OVERTIME_PAY",
            "SENIORITY_PAYMENT",
            "PAYROLL_ADJUSTMENT_EARNING", "PAYROLL_ADJUSTMENT_DEDUCTION",
            "PRIOR_PAYROLL_RECOVERY");

    private final AuthenticatedUser authenticatedUser;
    private final PayrollComponentRepository componentRepository;
    private final PayrollEmployeeItemRepository employeeItemRepository;
    private final PayrollTaxConfigRepository taxConfigRepository;
    private final PayrollTaxBracketRepository taxBracketRepository;
    private final PayrollNssfConfigRepository nssfConfigRepository;
    private final PayrollNssfWageBandRepository nssfWageBandRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final PayrollPolicyRuleRepository policyRuleRepository;
    private final PayrollRunRepository runRepository;
    private final PayrollPeriodRepository periodRepository;
    private final PayrollRuleQueryService payrollRuleQueryService;

    public PayrollConfigurationAdminService(
            AuthenticatedUser authenticatedUser,
            PayrollComponentRepository componentRepository,
            PayrollEmployeeItemRepository employeeItemRepository,
            PayrollTaxConfigRepository taxConfigRepository,
            PayrollTaxBracketRepository taxBracketRepository,
            PayrollNssfConfigRepository nssfConfigRepository,
            PayrollNssfWageBandRepository nssfWageBandRepository,
            ContractTypeRepository contractTypeRepository,
            PayrollPolicyRuleRepository policyRuleRepository,
            PayrollRunRepository runRepository,
            PayrollPeriodRepository periodRepository,
            PayrollRuleQueryService payrollRuleQueryService) {
        this.authenticatedUser = authenticatedUser;
        this.componentRepository = componentRepository;
        this.employeeItemRepository = employeeItemRepository;
        this.taxConfigRepository = taxConfigRepository;
        this.taxBracketRepository = taxBracketRepository;
        this.nssfConfigRepository = nssfConfigRepository;
        this.nssfWageBandRepository = nssfWageBandRepository;
        this.contractTypeRepository = contractTypeRepository;
        this.policyRuleRepository = policyRuleRepository;
        this.runRepository = runRepository;
        this.periodRepository = periodRepository;
        this.payrollRuleQueryService = payrollRuleQueryService;
    }

    // ---------------------------------------------------------------------
    // Payroll Components
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ComponentRow> findComponents(boolean activeOnly) {
        List<PayrollComponent> rows = activeOnly
                ? componentRepository.findByActiveTrueOrderBySortOrderAscComponentCodeAsc()
                : componentRepository.findAllByOrderBySortOrderAscComponentCodeAsc();
        return rows.stream().map(this::toComponentRow).toList();
    }

    @Transactional
    public Long saveComponent(ComponentInput input) {
        validateComponent(input);
        User user = currentUser();

        PayrollComponent component;
        if (input.id() == null) {
            requirePermission(AccessPageType.INSERTED_PAGE);
            component = new PayrollComponent();
            component.setUserCreated(user);
        } else {
            requirePermission(AccessPageType.UPDATED_PAGE);
            component = componentRepository.findById(input.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payroll component does not exist. | មិនមានធាតុប្រាក់បៀវត្សនេះទេ។"));

            String existingCode = component.getComponentCode();
            String existingType = component.getComponentType();
            boolean protectedIdentity = SYSTEM_COMPONENT_CODES.contains(existingCode)
                    || employeeItemRepository.existsByPayrollComponentId(component.getId());
            if (protectedIdentity
                    && (!existingCode.equalsIgnoreCase(input.code().trim())
                        || !existingType.equals(input.componentType()))) {
                throw new IllegalStateException(
                        "Component code and type cannot change after the component is used by payroll. "
                                + "Create a new component or deactivate this one instead.");
            }
        }

        component.setComponentCode(input.code().trim().toUpperCase(Locale.ROOT));
        component.setComponentNameEn(input.nameEn().trim());
        component.setComponentNameKh(blankToNull(input.nameKh()));
        component.setComponentType(input.componentType());
        component.setCalculationMethod(input.calculationMethod());
        component.setAllowManualEntry(input.allowManualEntry());
        component.setTaxable(input.taxable());
        component.setSubjectToNssf(input.subjectToNssf());
        component.setSubjectToSeniority(input.subjectToSeniority());
        component.setActive(input.active());
        component.setGlAccountCode(blankToNull(input.glAccountCode()));
        component.setSortOrder(input.sortOrder());
        component.setUserUpdated(user);

        PayrollComponent saved = componentRepository.saveAndFlush(component);
        if (input.id() != null) {
            runRepository.resetAllCalculatedRuns();
        }
        return saved.getId();
    }

    @Transactional
    public void deleteComponent(Long componentId) {
        requirePermission(AccessPageType.DELETED_PAGE);
        PayrollComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll component does not exist. | មិនមានធាតុប្រាក់បៀវត្សនេះទេ។"));

        String componentCode = component.getComponentCode();
        if (SYSTEM_COMPONENT_CODES.contains(componentCode)) {
            throw new IllegalStateException(
                    componentCode + " is a system payroll component and cannot be deleted. "
                            + "Deactivate it only when the related automatic calculation is not required. "
                            + "| " + componentCode + " ជាធាតុប្រព័ន្ធ និងមិនអាចលុបបានទេ។");
        }
        if (policyRuleRepository.existsByPayrollComponentId(componentId)) {
            throw new IllegalStateException(
                    "This component is referenced by a Payroll Rule. Change the rule mapping or deactivate the component instead.");
        }
        if (employeeItemRepository.existsByPayrollComponentId(componentId)) {
            throw new IllegalStateException(
                    "This component is already used in payroll. Deactivate it instead.");
        }
        componentRepository.delete(component);
        componentRepository.flush();
    }

    // ---------------------------------------------------------------------
    // Salary Tax
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TaxConfigRow> findTaxConfigs() {
        return taxConfigRepository.findAllByOrderByTaxYearDesc().stream()
                .map(config -> new TaxConfigRow(
                        config.getId(), config.getTaxYear(), config.getCurrency(),
                        zero(config.getDependentAllowance()), zero(config.getNonResidentRate()),
                        config.isActive(), config.getSourceReference(), config.getNotes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaxBracketRow> findTaxBrackets(Long taxConfigId) {
        if (taxConfigId == null) {
            return List.of();
        }
        return taxBracketRepository.findByPayrollTaxConfigIdOrderByBracketOrderAsc(taxConfigId).stream()
                .map(bracket -> new TaxBracketRow(
                        bracket.getId(), bracket.getPayrollTaxConfigId(), bracket.getBracketOrder(),
                        zero(bracket.getMinAmount()), bracket.getMaxAmount(),
                        zero(bracket.getTaxRate()), zero(bracket.getDeductionAmount())))
                .toList();
    }

    @Transactional
    public Long saveTaxConfig(TaxConfigInput input) {
        return saveTaxConfigInternal(input, null);
    }

    @Transactional
    public Long saveTaxConfigWithBrackets(
            TaxConfigInput input,
            List<TaxBracketInput> bracketInputs) {
        return saveTaxConfigInternal(
                input,
                bracketInputs == null ? List.of() : List.copyOf(bracketInputs));
    }

    private Long saveTaxConfigInternal(
            TaxConfigInput input,
            List<TaxBracketInput> bracketInputs) {
        validateTaxConfig(input);
        User user = currentUser();
        boolean replaceBrackets = bracketInputs != null;

        PayrollTaxConfig config;
        if (input.id() == null) {
            requirePermission(AccessPageType.INSERTED_PAGE);
            if (input.active() && !replaceBrackets) {
                throw new IllegalStateException(
                        "Create the tax year as inactive, add all brackets, then activate it. "
                                + "| សូមបង្កើតឆ្នាំពន្ធជាមិនសកម្ម បន្ថែមថ្នាក់ទាំងអស់ រួចទើបបើកសកម្មភាព។");
            }
            config = new PayrollTaxConfig();
            config.setUserCreated(user);
        } else {
            requirePermission(AccessPageType.UPDATED_PAGE);
            config = taxConfigRepository.findById(input.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Salary-tax configuration does not exist. | មិនមានការកំណត់ពន្ធនេះទេ។"));
            if (input.active() && !replaceBrackets) {
                validateTaxBracketConfiguration(config.getId(), input.taxYear());
            }
            Integer currentTaxYear = config.getTaxYear();
            if (currentTaxYear != null && currentTaxYear.intValue() != input.taxYear()
                    && periodRepository.countByPayrollYear(currentTaxYear) > 0) {
                throw new IllegalStateException(
                        "Tax year cannot be changed because that year is already used by payroll periods.");
            }
            runRepository.resetCalculatedRunsForTaxConfig(config.getId());
        }

        config.setTaxYear(input.taxYear());
        config.setCurrency(input.currency().trim().toUpperCase(Locale.ROOT));
        config.setDependentAllowance(input.dependentAllowance());
        config.setNonResidentRate(input.nonResidentRate());
        config.setActive(input.active());
        config.setSourceReference(blankToNull(input.sourceReference()));
        config.setNotes(blankToNull(input.notes()));
        config.setUserUpdated(user);
        config = taxConfigRepository.saveAndFlush(config);

        if (replaceBrackets) {
            replaceTaxBrackets(config.getId(), bracketInputs, user);
            if (input.active()) {
                validateTaxBracketConfiguration(config.getId(), input.taxYear());
            }
        }

        runRepository.resetCalculatedRunsForTaxConfig(config.getId());
        return config.getId();
    }

    private void replaceTaxBrackets(
            Long taxConfigId,
            List<TaxBracketInput> bracketInputs,
            User user) {
        List<PayrollTaxBracket> existing =
                taxBracketRepository.findByPayrollTaxConfigIdOrderByBracketOrderAsc(taxConfigId);
        Set<Long> remainingIds = new LinkedHashSet<>();
        existing.forEach(row -> remainingIds.add(row.getId()));

        if (!remainingIds.isEmpty()) {
            taxBracketRepository.shiftOrdersForReplace(taxConfigId);
        }

        for (TaxBracketInput input : bracketInputs) {
            TaxBracketInput normalized = new TaxBracketInput(
                    input.id(), taxConfigId, input.bracketOrder(), input.minAmount(),
                    input.maxAmount(), input.taxRate(), input.deductionAmount());
            validateTaxBracket(normalized);

            PayrollTaxBracket bracket;
            if (normalized.id() == null) {
                requirePermission(AccessPageType.INSERTED_PAGE);
                bracket = new PayrollTaxBracket();
                bracket.setUserCreated(user);
                bracket.setPayrollTaxConfigId(taxConfigId);
            } else {
                if (!remainingIds.remove(normalized.id())) {
                    throw new IllegalArgumentException(
                            "A tax bracket does not belong to the selected Tax Year. "
                                    + "| ថ្នាក់ពន្ធមួយមិនមែនជារបស់ឆ្នាំពន្ធដែលបានជ្រើសទេ។");
                }
                requirePermission(AccessPageType.UPDATED_PAGE);
                bracket = taxBracketRepository.findById(normalized.id())
                        .filter(row -> taxConfigId.equals(row.getPayrollTaxConfigId()))
                        .orElseThrow(() -> new IllegalStateException(
                                "A tax bracket changed or was deleted by another user. Refresh and try again. "
                                        + "| ថ្នាក់ពន្ធត្រូវបានកែប្រែ ឬលុបដោយអ្នកប្រើផ្សេង។"));
            }

            bracket.setBracketOrder(normalized.bracketOrder());
            bracket.setMinAmount(normalized.minAmount());
            bracket.setMaxAmount(normalized.maxAmount());
            bracket.setTaxRate(normalized.taxRate());
            bracket.setDeductionAmount(normalized.deductionAmount());
            bracket.setUserUpdated(user);
            taxBracketRepository.save(bracket);
        }

        taxBracketRepository.flush();
        if (!remainingIds.isEmpty()) {
            requirePermission(AccessPageType.DELETED_PAGE);
            remainingIds.forEach(taxBracketRepository::deleteById);
            taxBracketRepository.flush();
        }
    }

    @Transactional
    public void deleteTaxConfig(Long taxConfigId) {
        requirePermission(AccessPageType.DELETED_PAGE);
        PayrollTaxConfig config = taxConfigRepository.findById(taxConfigId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Salary-tax configuration does not exist. | មិនមានការកំណត់ពន្ធនេះទេ។"));
        if (periodRepository.countByPayrollYear(config.getTaxYear()) > 0) {
            throw new IllegalStateException(
                    "This tax year is already referenced by a payroll period. Deactivate it instead.");
        }
        taxConfigRepository.delete(config);
        taxConfigRepository.flush();
    }

    @Transactional
    public Long saveTaxBracket(TaxBracketInput input) {
        validateTaxBracket(input);
        User user = currentUser();
        PayrollTaxBracket bracket;

        if (input.id() == null) {
            requirePermission(AccessPageType.INSERTED_PAGE);
            bracket = new PayrollTaxBracket();
            bracket.setUserCreated(user);
            bracket.setPayrollTaxConfigId(input.taxConfigId());
        } else {
            requirePermission(AccessPageType.UPDATED_PAGE);
            bracket = taxBracketRepository.findById(input.id())
                    .filter(row -> input.taxConfigId().equals(row.getPayrollTaxConfigId()))
                    .orElseThrow(() -> new IllegalStateException(
                            "Tax bracket changed or was deleted by another user. Refresh and try again."));
        }

        bracket.setBracketOrder(input.bracketOrder());
        bracket.setMinAmount(input.minAmount());
        bracket.setMaxAmount(input.maxAmount());
        bracket.setTaxRate(input.taxRate());
        bracket.setDeductionAmount(input.deductionAmount());
        bracket.setUserUpdated(user);
        bracket = taxBracketRepository.saveAndFlush(bracket);

        runRepository.resetCalculatedRunsForTaxConfig(input.taxConfigId());
        validateTaxBracketConfigurationIfActive(input.taxConfigId());
        return bracket.getId();
    }

    @Transactional
    public void deleteTaxBracket(Long taxBracketId) {
        requirePermission(AccessPageType.DELETED_PAGE);
        PayrollTaxBracket bracket = taxBracketRepository.findById(taxBracketId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tax bracket does not exist. | មិនមានថ្នាក់ពន្ធនេះទេ។"));
        Long taxConfigId = bracket.getPayrollTaxConfigId();
        taxBracketRepository.delete(bracket);
        taxBracketRepository.flush();
        validateTaxBracketConfigurationIfActive(taxConfigId);
        runRepository.resetCalculatedRunsForTaxConfig(taxConfigId);
    }

    // ---------------------------------------------------------------------
    // NSSF
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NssfConfigRow> findNssfConfigs() {
        return nssfConfigRepository.findAllForAdministration().stream()
                .map(config -> new NssfConfigRow(
                        config.getId(),
                        config.getEligibleContractType() == null
                                ? null : config.getEligibleContractType().getId(),
                        config.getEligibleContractType() == null
                                ? null : config.getEligibleContractType().getContractTypeName(),
                        config.getEffectiveFrom(), config.getEffectiveTo(), config.getCurrency(),
                        zero(config.getHealthEmployeeRate()), zero(config.getHealthEmployerRate()),
                        zero(config.getRiskEmployeeRate()), zero(config.getRiskEmployerRate()),
                        zero(config.getPensionEmployeeRate()), zero(config.getPensionEmployerRate()),
                        zero(config.getPensionMinWage()), config.getPensionMaxWage(),
                        config.isActive(), config.getSourceReference(), config.getNotes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NssfBandRow> findNssfBands(Long nssfConfigId) {
        if (nssfConfigId == null) {
            return List.of();
        }
        return nssfWageBandRepository.findByPayrollNssfConfigIdOrderByBandOrderAsc(nssfConfigId).stream()
                .map(band -> new NssfBandRow(
                        band.getId(), band.getPayrollNssfConfigId(), band.getBandOrder(),
                        band.getMaxSalary(), zero(band.getContributoryWage())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findNssfConfigurationIssues(Long payrollPeriodId) {
        if (payrollPeriodId == null) {
            return List.of("Select a payroll period");
        }
        PayrollPeriod period = periodRepository.findById(payrollPeriodId).orElse(null);
        if (period == null || period.getPaymentDate() == null) {
            return List.of("Payment date is required for NSSF");
        }
        return nssfConfigRepository.findConfigurationIssues(payrollPeriodId);
    }

    @Transactional
    public Long saveNssfConfig(NssfConfigInput input) {
        return saveNssfConfigInternal(input, null);
    }

    @Transactional
    public Long saveNssfConfigWithBands(
            NssfConfigInput input,
            List<NssfBandInput> bandInputs) {
        return saveNssfConfigInternal(
                input,
                bandInputs == null ? List.of() : List.copyOf(bandInputs));
    }

    private Long saveNssfConfigInternal(
            NssfConfigInput input,
            List<NssfBandInput> bandInputs) {
        validateNssfConfig(input);
        User user = currentUser();
        boolean replaceBands = bandInputs != null;

        if (input.active()
                && nssfConfigRepository.countActiveOverlaps(
                        input.id(), input.eligibleContractTypeId(), input.effectiveTo(), input.effectiveFrom()) > 0) {
            throw new IllegalStateException(
                    "Active NSSF dates cannot overlap another active rule for the same eligible contract scope. "
                            + "| កាលបរិច្ឆេទច្បាប់ ប.ស.ស. សកម្មមិនអាចត្រួតគ្នា "
                            + "សម្រាប់ប្រភេទកិច្ចសន្យាមានសិទ្ធិដូចគ្នាបានទេ។");
        }

        PayrollNssfConfig config;
        if (input.id() == null) {
            requirePermission(AccessPageType.INSERTED_PAGE);
            if (input.active() && !replaceBands) {
                throw new IllegalStateException(
                        "Create the NSSF rule as inactive, add all wage bands, then activate it. "
                                + "| សូមបង្កើតច្បាប់ ប.ស.ស. ជាមិនសកម្ម បន្ថែមកម្រិតទាំងអស់ រួចទើបបើកសកម្មភាព។");
            }
            config = new PayrollNssfConfig();
            config.setUserCreated(user);
        } else {
            requirePermission(AccessPageType.UPDATED_PAGE);
            config = nssfConfigRepository.findById(input.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "NSSF rule does not exist. | មិនមានច្បាប់ ប.ស.ស. នេះទេ។"));
            if (input.active() && !replaceBands) {
                validateNssfBandConfiguration(config.getId());
            }
            runRepository.resetCalculatedRunsForNssfConfig(config.getId());
        }

        ContractType contractType = contractTypeRepository.findById(input.eligibleContractTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Selected eligible contract type does not exist. "
                                + "| ប្រភេទកិច្ចសន្យាមានសិទ្ធិដែលបានជ្រើសមិនមានទេ។"));

        config.setEligibleContractType(contractType);
        config.setEffectiveFrom(input.effectiveFrom());
        config.setEffectiveTo(input.effectiveTo());
        config.setCurrency(input.currency().trim().toUpperCase(Locale.ROOT));
        config.setHealthEmployeeRate(input.healthEmployeeRate());
        config.setHealthEmployerRate(input.healthEmployerRate());
        config.setRiskEmployeeRate(input.riskEmployeeRate());
        config.setRiskEmployerRate(input.riskEmployerRate());
        config.setPensionEmployeeRate(input.pensionEmployeeRate());
        config.setPensionEmployerRate(input.pensionEmployerRate());
        config.setPensionMinWage(input.pensionMinWage());
        config.setPensionMaxWage(input.pensionMaxWage());
        config.setActive(input.active());
        config.setSourceReference(blankToNull(input.sourceReference()));
        config.setNotes(blankToNull(input.notes()));
        config.setUserUpdated(user);
        config = nssfConfigRepository.saveAndFlush(config);

        if (replaceBands) {
            replaceNssfBands(config.getId(), bandInputs, user);
            if (input.active()) {
                validateNssfBandConfiguration(config.getId());
            }
        }

        runRepository.resetCalculatedRunsForNssfConfig(config.getId());
        return config.getId();
    }

    private void replaceNssfBands(
            Long nssfConfigId,
            List<NssfBandInput> bandInputs,
            User user) {
        List<PayrollNssfWageBand> existing =
                nssfWageBandRepository.findByPayrollNssfConfigIdOrderByBandOrderAsc(nssfConfigId);
        Set<Long> remainingIds = new LinkedHashSet<>();
        existing.forEach(row -> remainingIds.add(row.getId()));

        if (!remainingIds.isEmpty()) {
            nssfWageBandRepository.shiftOrdersForReplace(nssfConfigId);
        }

        for (NssfBandInput input : bandInputs) {
            NssfBandInput normalized = new NssfBandInput(
                    input.id(), nssfConfigId, input.bandOrder(),
                    input.maxSalary(), input.contributoryWage());
            validateNssfBand(normalized);

            PayrollNssfWageBand band;
            if (normalized.id() == null) {
                requirePermission(AccessPageType.INSERTED_PAGE);
                band = new PayrollNssfWageBand();
                band.setUserCreated(user);
                band.setPayrollNssfConfigId(nssfConfigId);
            } else {
                if (!remainingIds.remove(normalized.id())) {
                    throw new IllegalArgumentException(
                            "An NSSF wage band does not belong to the selected rule. "
                                    + "| កម្រិតប្រាក់ឈ្នួល ប.ស.ស. មួយមិនមែនជារបស់ច្បាប់ដែលបានជ្រើសទេ។");
                }
                requirePermission(AccessPageType.UPDATED_PAGE);
                band = nssfWageBandRepository.findById(normalized.id())
                        .filter(row -> nssfConfigId.equals(row.getPayrollNssfConfigId()))
                        .orElseThrow(() -> new IllegalStateException(
                                "An NSSF wage band changed or was deleted by another user. Refresh and try again. "
                                        + "| កម្រិតប្រាក់ឈ្នួល ប.ស.ស. ត្រូវបានកែប្រែ ឬលុបដោយអ្នកប្រើផ្សេង។"));
            }

            band.setBandOrder(normalized.bandOrder());
            band.setMaxSalary(normalized.maxSalary());
            band.setContributoryWage(normalized.contributoryWage());
            band.setUserUpdated(user);
            nssfWageBandRepository.save(band);
        }

        nssfWageBandRepository.flush();
        if (!remainingIds.isEmpty()) {
            requirePermission(AccessPageType.DELETED_PAGE);
            remainingIds.forEach(nssfWageBandRepository::deleteById);
            nssfWageBandRepository.flush();
        }
    }

    @Transactional
    public void deleteNssfConfig(Long nssfConfigId) {
        requirePermission(AccessPageType.DELETED_PAGE);
        PayrollNssfConfig config = nssfConfigRepository.findById(nssfConfigId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "NSSF rule does not exist. | មិនមានច្បាប់ ប.ស.ស. នេះទេ។"));
        if (nssfConfigRepository.countCoveredPayrollPeriods(nssfConfigId) > 0) {
            throw new IllegalStateException(
                    "This NSSF rule covers an existing payroll period. Deactivate it instead. "
                            + "| ច្បាប់ ប.ស.ស. នេះគ្របដណ្ដប់លើរយៈពេលបើកប្រាក់បៀវត្សដែលមានស្រាប់។ "
                            + "សូមបិទសកម្មភាពជំនួសការលុប។");
        }
        nssfConfigRepository.delete(config);
        nssfConfigRepository.flush();
    }

    @Transactional
    public Long saveNssfBand(NssfBandInput input) {
        validateNssfBand(input);
        User user = currentUser();
        PayrollNssfWageBand band;

        if (input.id() == null) {
            requirePermission(AccessPageType.INSERTED_PAGE);
            band = new PayrollNssfWageBand();
            band.setUserCreated(user);
            band.setPayrollNssfConfigId(input.nssfConfigId());
        } else {
            requirePermission(AccessPageType.UPDATED_PAGE);
            band = nssfWageBandRepository.findById(input.id())
                    .filter(row -> input.nssfConfigId().equals(row.getPayrollNssfConfigId()))
                    .orElseThrow(() -> new IllegalStateException(
                            "NSSF wage band changed or was deleted by another user. Refresh and try again."));
        }

        band.setBandOrder(input.bandOrder());
        band.setMaxSalary(input.maxSalary());
        band.setContributoryWage(input.contributoryWage());
        band.setUserUpdated(user);
        band = nssfWageBandRepository.saveAndFlush(band);

        runRepository.resetCalculatedRunsForNssfConfig(input.nssfConfigId());
        validateNssfBandConfigurationIfActive(input.nssfConfigId());
        return band.getId();
    }

    @Transactional
    public void deleteNssfBand(Long nssfBandId) {
        requirePermission(AccessPageType.DELETED_PAGE);
        PayrollNssfWageBand band = nssfWageBandRepository.findById(nssfBandId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "NSSF wage band does not exist. | មិនមានកម្រិតប្រាក់ឈ្នួល ប.ស.ស. នេះទេ។"));
        Long configId = band.getPayrollNssfConfigId();
        nssfWageBandRepository.delete(band);
        nssfWageBandRepository.flush();
        validateNssfBandConfigurationIfActive(configId);
        runRepository.resetCalculatedRunsForNssfConfig(configId);
    }

    // ---------------------------------------------------------------------
    // Payroll Policy Rules
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Integer> findPayrollRuleYears() {
        return policyRuleRepository.findDistinctRuleYears();
    }

    @Transactional(readOnly = true)
    public List<PayrollRuleRow> findPayrollRules(Integer ruleYear) {
        return payrollRuleQueryService.find(ruleYear);
    }

    @Transactional
    public Long savePayrollRule(PayrollRuleInput input) {
        validatePayrollRule(input);
        validateAutomaticOvertimeRuleUniqueness(input);
        validateAutomaticLeavePayTriggerUniqueness(input);
        User user = currentUser();

        PayrollPolicyRule rule;
        Integer previousYear = null;
        if (input.id() == null) {
            requirePermission(AccessPageType.INSERTED_PAGE);
            rule = new PayrollPolicyRule();
            rule.setUserCreated(user);
        } else {
            requirePermission(AccessPageType.UPDATED_PAGE);
            rule = policyRuleRepository.findById(input.id())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payroll rule does not exist. | មិនមានច្បាប់ប្រាក់បៀវត្សនេះទេ។"));
            previousYear = rule.getRuleYear();
        }

        rule.setRuleYear(input.ruleYear());
        rule.setRuleCode(input.code().trim().toUpperCase(Locale.ROOT));
        rule.setRuleName(input.name().trim());
        rule.setRuleType(input.ruleType());
        rule.setRateAmount(input.rateAmount());
        rule.setRateCurrency(upperOrNull(input.rateCurrency()));
        rule.setRateUnit(upperOrNull(input.rateUnit()));
        rule.setMultiplier(input.multiplier());
        rule.setWorkdayDivisor(input.workdayDivisor() == null ? 22 : input.workdayDivisor());
        rule.setPayPercentage(input.payPercentage());
        rule.setEntitlementDays(input.entitlementDays());
        rule.setServiceYearsPerExtraDay(input.serviceYearsPerExtraDay());
        rule.setInitialFullPayMonths(input.initialFullPayMonths());
        rule.setMaxPaidMonths(input.maxPaidMonths());
        rule.setPayrollComponentId(input.componentId());
        rule.setCalculationSource(input.calculationSource());
        rule.setActive(input.active());
        rule.setSortOrder(input.sortOrder());
        rule.setDescription(blankToNull(input.description()));
        rule.setUserUpdated(user);
        rule = policyRuleRepository.saveAndFlush(rule);

        if (previousYear != null) {
            runRepository.resetCalculatedRunsForRuleYear(previousYear);
        }
        if (previousYear == null || previousYear.intValue() != input.ruleYear()) {
            runRepository.resetCalculatedRunsForRuleYear(input.ruleYear());
        }
        return rule.getId();
    }

    @Transactional
    public void deletePayrollRule(Long ruleId) {
        requirePermission(AccessPageType.DELETED_PAGE);
        PayrollPolicyRule rule = policyRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll rule does not exist. | មិនមានច្បាប់ប្រាក់បៀវត្សនេះទេ។"));
        Integer ruleYear = rule.getRuleYear();
        policyRuleRepository.delete(rule);
        policyRuleRepository.flush();
        if (ruleYear != null) {
            runRepository.resetCalculatedRunsForRuleYear(ruleYear);
        }
    }

    // ---------------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------------

    private void validateComponent(ComponentInput input) {
        if (input == null || input.code() == null || input.code().isBlank()
                || input.nameEn() == null || input.nameEn().isBlank()) {
            throw new IllegalArgumentException("Component code and English name are required.");
        }
        if (!COMPONENT_TYPES.contains(input.componentType())
                || !CALCULATION_METHODS.contains(input.calculationMethod())) {
            throw new IllegalArgumentException("Invalid component type or calculation method.");
        }
        if (input.subjectToSeniority() && !"EARNING".equals(input.componentType())) {
            throw new IllegalArgumentException(
                    "Only EARNING components can be included in the seniority average. "
                            + "| មានតែធាតុចំណូលប៉ុណ្ណោះដែលអាចបញ្ចូលក្នុងមូលដ្ឋានអតីតភាព។");
        }
        if ("SENIORITY_PAYMENT".equalsIgnoreCase(input.code().trim())
                && input.subjectToSeniority()) {
            throw new IllegalArgumentException(
                    "SENIORITY_PAYMENT cannot be included in its own calculation base. "
                            + "| SENIORITY_PAYMENT មិនអាចបញ្ចូលក្នុងមូលដ្ឋានគណនារបស់ខ្លួនបានទេ។");
        }
    }

    private void validateTaxConfig(TaxConfigInput input) {
        if (input == null || input.taxYear() < 2000 || input.taxYear() > 2100) {
            throw new IllegalArgumentException("Enter a valid tax year.");
        }
        if (input.currency() == null || input.currency().trim().length() != 3) {
            throw new IllegalArgumentException(
                    "Tax currency must contain three characters, for example KHR.");
        }
        if (input.dependentAllowance() == null || input.dependentAllowance().signum() < 0) {
            throw new IllegalArgumentException("Dependant allowance cannot be negative.");
        }
        if (input.nonResidentRate() == null
                || input.nonResidentRate().signum() < 0
                || input.nonResidentRate().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Non-resident rate must be between 0 and 100 percent.");
        }
    }

    private void validateTaxBracket(TaxBracketInput input) {
        if (input == null || input.taxConfigId() == null) {
            throw new IllegalArgumentException("Tax year is required.");
        }
        if (input.bracketOrder() < 1) {
            throw new IllegalArgumentException("Bracket order must be at least 1.");
        }
        if (input.minAmount() == null || input.minAmount().signum() < 0) {
            throw new IllegalArgumentException("Minimum amount cannot be negative.");
        }
        if (input.maxAmount() != null && input.maxAmount().compareTo(input.minAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Maximum amount must be greater than or equal to minimum amount.");
        }
        if (input.taxRate() == null || input.taxRate().signum() < 0
                || input.taxRate().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 100 percent.");
        }
        if (input.deductionAmount() == null || input.deductionAmount().signum() < 0) {
            throw new IllegalArgumentException("Tax deduction cannot be negative.");
        }
    }

    private void validateTaxBracketConfiguration(Long taxConfigId, int taxYear) {
        List<TaxBracketRow> brackets = findTaxBrackets(taxConfigId);
        if (brackets.isEmpty()) {
            throw new IllegalStateException(
                    "No salary-tax brackets are configured for " + taxYear + ".");
        }
        if (brackets.getFirst().minAmount().compareTo(ZERO) != 0) {
            throw new IllegalStateException(
                    "Salary-tax bracket 1 must start at zero for " + taxYear
                            + ". | ថ្នាក់ពន្ធទី 1 ត្រូវចាប់ផ្ដើមពីសូន្យ។");
        }

        BigDecimal previousMaximum = null;
        for (int index = 0; index < brackets.size(); index++) {
            TaxBracketRow bracket = brackets.get(index);
            int expectedOrder = index + 1;
            if (bracket.bracketOrder() != expectedOrder) {
                throw new IllegalStateException(
                        "Salary-tax bracket order must be continuous from 1. Missing order "
                                + expectedOrder + ". | លំដាប់ថ្នាក់ពន្ធត្រូវបន្តគ្នាចាប់ពីលេខ 1។");
            }
            boolean finalBracket = index == brackets.size() - 1;
            if (bracket.maxAmount() == null && !finalBracket) {
                throw new IllegalStateException(
                        "Only the final salary-tax bracket may have no maximum. "
                                + "| មានតែថ្នាក់ពន្ធចុងក្រោយប៉ុណ្ណោះដែលអាចគ្មានអតិបរមា។");
            }
            if (finalBracket && bracket.maxAmount() != null) {
                throw new IllegalStateException(
                        "The final salary-tax bracket must have no maximum. "
                                + "| ថ្នាក់ពន្ធចុងក្រោយត្រូវគ្មានចំនួនអតិបរមា។");
            }
            if (previousMaximum != null) {
                BigDecimal boundaryDifference = bracket.minAmount().subtract(previousMaximum);
                if (boundaryDifference.signum() < 0
                        || boundaryDifference.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalStateException(
                            "Salary-tax brackets must be continuous without gaps or overlaps at order "
                                    + bracket.bracketOrder()
                                    + ". Use the previous maximum or previous maximum + 1 KHR as the next minimum. "
                                    + "| ថ្នាក់ពន្ធត្រូវបន្តគ្នាដោយគ្មានចន្លោះ ឬត្រួតគ្នា។");
                }
            }
            previousMaximum = bracket.maxAmount();
        }
    }

    private void validateTaxBracketConfigurationIfActive(Long taxConfigId) {
        taxConfigRepository.findById(taxConfigId)
                .filter(PayrollTaxConfig::isActive)
                .ifPresent(config -> validateTaxBracketConfiguration(
                        config.getId(), config.getTaxYear()));
    }

    private void validateNssfConfig(NssfConfigInput input) {
        if (input == null || input.effectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "NSSF effective-from date is required. | ត្រូវបញ្ចូលកាលបរិច្ឆេទចាប់ផ្ដើមសុពលភាព ប.ស.ស.។");
        }
        if (input.effectiveTo() != null && input.effectiveTo().isBefore(input.effectiveFrom())) {
            throw new IllegalArgumentException(
                    "NSSF effective-to date cannot be before effective-from date. "
                            + "| កាលបរិច្ឆេទបញ្ចប់សុពលភាព ប.ស.ស. មិនអាចមុនកាលបរិច្ឆេទចាប់ផ្ដើមបានទេ។");
        }
        if (input.currency() == null || input.currency().trim().length() != 3) {
            throw new IllegalArgumentException(
                    "NSSF currency must contain three characters, for example KHR. "
                            + "| រូបិយប័ណ្ណ ប.ស.ស. ត្រូវមានបីតួអក្សរ ឧទាហរណ៍ KHR។");
        }
        if (input.eligibleContractTypeId() == null) {
            throw new IllegalArgumentException(
                    "Eligible Contract Type is required for an NSSF rule. "
                            + "Employees with another or missing Contract Type are ignored. "
                            + "| ត្រូវជ្រើសរើសប្រភេទកិច្ចសន្យាសម្រាប់ច្បាប់ ប.ស.ស.។ "
                            + "បុគ្គលិកដែលមានប្រភេទកិច្ចសន្យាផ្សេង ឬមិនបានកំណត់ នឹងមិនត្រូវគណនា។");
        }
        if (!contractTypeRepository.existsById(input.eligibleContractTypeId())) {
            throw new IllegalArgumentException(
                    "Selected eligible contract type does not exist. "
                            + "| ប្រភេទកិច្ចសន្យាមានសិទ្ធិដែលបានជ្រើសមិនមានទេ។");
        }
        validatePercentage(input.healthEmployeeRate(), "Health employee rate", "អត្រាសុខភាពនិយោជិត");
        validatePercentage(input.healthEmployerRate(), "Health employer rate", "អត្រាសុខភាពនិយោជក");
        validatePercentage(input.riskEmployeeRate(), "Occupational-risk employee rate", "អត្រាហានិភ័យការងារនិយោជិត");
        validatePercentage(input.riskEmployerRate(), "Occupational-risk employer rate", "អត្រាហានិភ័យការងារនិយោជក");
        validatePercentage(input.pensionEmployeeRate(), "Pension employee rate", "អត្រាសោធននិយោជិត");
        validatePercentage(input.pensionEmployerRate(), "Pension employer rate", "អត្រាសោធននិយោជក");
        if (input.pensionMinWage() == null || input.pensionMinWage().signum() < 0) {
            throw new IllegalArgumentException(
                    "Pension minimum contributory wage cannot be negative. "
                            + "| ប្រាក់ឈ្នួលជាប់ភាគទានសោធនអប្បបរមាមិនអាចជាចំនួនអវិជ្ជមានបានទេ។");
        }
        if (input.pensionMaxWage() == null
                || input.pensionMaxWage().compareTo(input.pensionMinWage()) < 0) {
            throw new IllegalArgumentException(
                    "Pension maximum contributory wage must be greater than or equal to the minimum. "
                            + "| ប្រាក់ឈ្នួលជាប់ភាគទានសោធនអតិបរមាត្រូវតែធំជាង ឬស្មើអប្បបរមា។");
        }
    }

    private void validateNssfBand(NssfBandInput input) {
        if (input == null || input.nssfConfigId() == null) {
            throw new IllegalArgumentException(
                    "NSSF rule is required. | ត្រូវជ្រើសរើសច្បាប់ ប.ស.ស.។");
        }
        if (input.bandOrder() < 1) {
            throw new IllegalArgumentException(
                    "NSSF band order must be at least 1. | លំដាប់កម្រិត ប.ស.ស. ត្រូវចាប់ពីលេខ 1។");
        }
        if (input.maxSalary() != null && input.maxSalary().signum() < 0) {
            throw new IllegalArgumentException(
                    "NSSF band maximum salary cannot be negative. "
                            + "| ប្រាក់បៀវត្សអតិបរមារបស់កម្រិត ប.ស.ស. មិនអាចជាចំនួនអវិជ្ជមានបានទេ។");
        }
        if (input.contributoryWage() == null || input.contributoryWage().signum() <= 0) {
            throw new IllegalArgumentException(
                    "NSSF contributory wage must be greater than zero. "
                            + "| ប្រាក់ឈ្នួលជាប់ភាគទាន ប.ស.ស. ត្រូវតែធំជាងសូន្យ។");
        }
    }

    private void validateNssfBandConfiguration(Long configId) {
        List<NssfBandRow> bands = findNssfBands(configId);
        if (bands.isEmpty()) {
            throw new IllegalStateException(
                    "NSSF Rules require wage bands. | ច្បាប់ ប.ស.ស. ត្រូវមានកម្រិតប្រាក់ឈ្នួល។");
        }

        BigDecimal previousMaximum = null;
        BigDecimal previousWage = null;
        for (int index = 0; index < bands.size(); index++) {
            NssfBandRow band = bands.get(index);
            int expectedOrder = index + 1;
            if (band.bandOrder() != expectedOrder) {
                throw new IllegalStateException(
                        "NSSF band order must be continuous from 1. Missing order " + expectedOrder
                                + ". | លំដាប់កម្រិត ប.ស.ស. ត្រូវបន្តគ្នាចាប់ពីលេខ 1។");
            }
            boolean finalBand = index == bands.size() - 1;
            if (band.maxSalary() == null && !finalBand) {
                throw new IllegalStateException(
                        "Only the final NSSF band may have no maximum. "
                                + "| មានតែកម្រិត ប.ស.ស. ចុងក្រោយប៉ុណ្ណោះដែលអាចគ្មានអតិបរមា។");
            }
            if (finalBand && band.maxSalary() != null) {
                throw new IllegalStateException(
                        "The final NSSF band must have no maximum. "
                                + "| កម្រិត ប.ស.ស. ចុងក្រោយត្រូវគ្មានប្រាក់បៀវត្សអតិបរមា។");
            }
            if (previousMaximum != null && band.maxSalary() != null
                    && band.maxSalary().compareTo(previousMaximum) <= 0) {
                throw new IllegalStateException(
                        "NSSF maximum salaries must increase with each band. "
                                + "| ប្រាក់បៀវត្សអតិបរមា ប.ស.ស. ត្រូវកើនតាមលំដាប់កម្រិត។");
            }
            if (previousWage != null && band.contributoryWage().compareTo(previousWage) < 0) {
                throw new IllegalStateException(
                        "NSSF contributory wages cannot decrease in higher bands. "
                                + "| ប្រាក់ឈ្នួលជាប់ភាគទាន ប.ស.ស. មិនអាចថយចុះនៅកម្រិតខ្ពស់ជាងបានទេ។");
            }
            previousMaximum = band.maxSalary();
            previousWage = band.contributoryWage();
        }
    }

    private void validateNssfBandConfigurationIfActive(Long configId) {
        nssfConfigRepository.findById(configId)
                .filter(PayrollNssfConfig::isActive)
                .ifPresent(config -> validateNssfBandConfiguration(config.getId()));
    }

    private static void validatePercentage(
            BigDecimal value,
            String englishLabel,
            String khmerLabel) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    englishLabel + " must be between 0 and 100 percent. | "
                            + khmerLabel + " ត្រូវស្ថិតនៅចន្លោះ 0 និង 100 ភាគរយ។");
        }
    }

    private void validatePayrollRule(PayrollRuleInput input) {
        if (input == null || input.ruleYear() < 2000 || input.ruleYear() > 2100) {
            throw new IllegalArgumentException("Enter a valid payroll-rule year.");
        }
        if (input.code() == null || input.code().isBlank()
                || input.name() == null || input.name().isBlank()) {
            throw new IllegalArgumentException("Rule code and name are required.");
        }
        if (!input.code().trim().matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Rule code may contain only letters, numbers and underscores. "
                            + "| កូដច្បាប់អាចមានតែអក្សរ លេខ និងសញ្ញា underscore។");
        }
        if (!PAYROLL_RULE_TYPES.contains(input.ruleType())) {
            throw new IllegalArgumentException("Invalid payroll-rule type.");
        }
        if (!PAYROLL_RULE_SOURCES.contains(input.calculationSource())) {
            throw new IllegalArgumentException("Invalid payroll-rule source.");
        }
        if (input.rateUnit() != null && !input.rateUnit().isBlank()
                && !PAYROLL_RULE_UNITS.contains(input.rateUnit().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Invalid payroll-rule unit.");
        }
        if (input.rateCurrency() != null && !input.rateCurrency().isBlank()
                && input.rateCurrency().trim().length() != 3) {
            throw new IllegalArgumentException(
                    "Payroll-rule currency must contain three characters.");
        }
        if (input.workdayDivisor() == null
                || !Set.of(22, 24, 26).contains(input.workdayDivisor())) {
            throw new IllegalArgumentException(
                    "Workday divisor must be 22, 24, or 26. "
                            + "| ចំនួនថ្ងៃចែកត្រូវជា 22, 24 ឬ 26។");
        }
        requireNonNegative(input.rateAmount(), "Rate amount");
        requireNonNegative(input.multiplier(), "Multiplier");
        requireNonNegative(input.payPercentage(), "Pay percentage");
        requireNonNegative(input.entitlementDays(), "Entitlement days");
        if (input.payPercentage() != null
                && input.payPercentage().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Pay percentage cannot exceed 100 percent.");
        }
        if (input.serviceYearsPerExtraDay() != null && input.serviceYearsPerExtraDay() < 0
                || input.initialFullPayMonths() != null && input.initialFullPayMonths() < 0
                || input.maxPaidMonths() != null && input.maxPaidMonths() < 0) {
            throw new IllegalArgumentException("Rule month/year values cannot be negative.");
        }
        if (input.initialFullPayMonths() != null && input.maxPaidMonths() != null
                && input.initialFullPayMonths() > input.maxPaidMonths()) {
            throw new IllegalArgumentException(
                    "Initial Full-Pay Months cannot exceed Maximum Paid Months. "
                            + "| ចំនួនខែបើកប្រាក់ពេញដំបូងមិនអាចលើសចំនួនខែអតិបរមា។");
        }

        if (input.active()) {
            switch (input.ruleType()) {
                case "ALLOWANCE" -> {
                    if (input.rateAmount() == null || input.rateAmount().signum() <= 0
                            || input.rateCurrency() == null || input.rateCurrency().isBlank()
                            || input.rateUnit() == null || input.rateUnit().isBlank()) {
                        throw new IllegalArgumentException(
                                "An active allowance rule requires a positive rate, currency, and unit. "
                                        + "| ច្បាប់ប្រាក់ឧបត្ថម្ភសកម្មត្រូវមានអត្រាវិជ្ជមាន រូបិយប័ណ្ណ និងឯកតា។");
                    }
                    if ("ATTENDANCE".equals(input.calculationSource())
                            && !Set.of("DAY", "CYCLE", "MONTH")
                                    .contains(upperOrNull(input.rateUnit()))) {
                        throw new IllegalArgumentException(
                                "Automatic attendance allowance rules must use DAY, CYCLE or MONTH. "
                                        + "| ច្បាប់ប្រាក់ឧបត្ថម្ភតាមវត្តមានត្រូវប្រើ DAY, CYCLE ឬ MONTH។");
                    }
                }
                case "DEDUCTION", "OVERTIME" -> {
                    if (input.multiplier() == null || input.multiplier().signum() <= 0) {
                        throw new IllegalArgumentException(
                                "An active " + input.ruleType() + " rule requires a positive multiplier. "
                                        + "| ច្បាប់សកម្មត្រូវមានមេគុណវិជ្ជមាន។");
                    }
                    String unit = upperOrNull(input.rateUnit());
                    if ("DEDUCTION".equals(input.ruleType()) && !"DAY".equals(unit)) {
                        throw new IllegalArgumentException(
                                "Automatic attendance deduction rules must use DAY. "
                                        + "| ច្បាប់កាត់ប្រាក់តាមវត្តមានត្រូវប្រើឯកតា DAY។");
                    }
                    if ("OVERTIME".equals(input.ruleType())
                            && "ATTENDANCE".equals(input.calculationSource())
                            && !Set.of("DAY", "HOUR").contains(unit)) {
                        throw new IllegalArgumentException(
                                "Automatic overtime rules must use DAY or HOUR. "
                                        + "Use DAY for extra-work days and HOUR for recorded overtime hours. "
                                        + "| ច្បាប់ម៉ោងបន្ថែមត្រូវប្រើ DAY ឬ HOUR។ "
                                        + "ប្រើ DAY សម្រាប់ថ្ងៃធ្វើការបន្ថែម និង HOUR សម្រាប់ម៉ោងបន្ថែម។");
                    }
                }
                case "LEAVE_ENTITLEMENT" -> {
                    if (input.entitlementDays() == null) {
                        throw new IllegalArgumentException(
                                "An active leave-entitlement rule requires entitlement days. "
                                        + "| ច្បាប់សិទ្ធិឈប់សម្រាកសកម្មត្រូវមានចំនួនថ្ងៃសិទ្ធិ។");
                    }
                }
                case "LEAVE_PAY" -> {
                    if (input.payPercentage() == null
                            || input.maxPaidMonths() == null || input.maxPaidMonths() <= 0) {
                        throw new IllegalArgumentException(
                                "An active leave-pay rule requires pay percentage and paid-month limit. "
                                        + "| ច្បាប់ប្រាក់ឈ្នួលពេលឈប់សម្រាកសកម្មត្រូវមានភាគរយ និងកំណត់ខែ។");
                    }
                    if (Set.of("S", "PI").contains(input.code().trim().toUpperCase(Locale.ROOT))
                            && input.initialFullPayMonths() == null) {
                        throw new IllegalArgumentException(
                                "The active sick/prolonged-illness rule requires Initial Full-Pay Months. "
                                        + "| ច្បាប់ឈប់សម្រាកឈឺ ឬជំងឺរ៉ាំរ៉ៃត្រូវមានចំនួនខែដំបូងដែលបើកប្រាក់ពេញ។");
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported payroll-rule type.");
            }
        }

        String expectedComponentType = switch (input.ruleType()) {
            case "DEDUCTION", "LEAVE_PAY" -> "DEDUCTION";
            case "OVERTIME", "ALLOWANCE" -> "EARNING";
            default -> null;
        };
        if (expectedComponentType != null && input.componentId() == null) {
            throw new IllegalArgumentException(
                    "Payroll Component is required for " + input.ruleType() + " rules.");
        }
        if (input.componentId() != null) {
            PayrollComponent component = componentRepository.findById(input.componentId()).orElse(null);
            boolean matches = component != null
                    && (expectedComponentType == null
                        || expectedComponentType.equals(component.getComponentType()))
                    && (!input.active() || component.isActive());
            if (!matches) {
                String requirement = expectedComponentType == null
                        ? "existing Payroll Component"
                        : "active " + expectedComponentType + " Payroll Component";
                throw new IllegalArgumentException(
                        "Selected component is invalid. Choose an " + requirement + ".");
            }
        }
    }

    private void validateAutomaticOvertimeRuleUniqueness(PayrollRuleInput input) {
        if (input == null || !input.active()
                || !"OVERTIME".equals(input.ruleType())
                || !"ATTENDANCE".equals(input.calculationSource())
                || !"HOUR".equals(upperOrNull(input.rateUnit()))) {
            return;
        }

        if (policyRuleRepository.countOtherActiveHourlyOvertimeRules(
                input.ruleYear(), input.id()) > 0) {
            throw new IllegalArgumentException(
                    "Only one active HOUR overtime rule is allowed per year because all recorded "
                            + "overtime hours feed that rule. Keep EW as DAY and OT as HOUR. "
                            + "| ក្នុងមួយឆ្នាំអាចមានច្បាប់ម៉ោងបន្ថែមឯកតា HOUR សកម្មតែមួយប៉ុណ្ណោះ។ "
                            + "សូមរក្សា EW ជា DAY និង OT ជា HOUR។");
        }
    }

    private void validateAutomaticLeavePayTriggerUniqueness(PayrollRuleInput input) {
        if (input == null || !input.active()
                || !"LEAVE_PAY".equals(input.ruleType())
                || !"LEAVE".equals(input.calculationSource())) {
            return;
        }

        String triggerCode = attendanceTriggerCode(input.code());
        boolean duplicateTrigger = policyRuleRepository
                .findOtherActiveLeavePayCodes(input.ruleYear(), input.id()).stream()
                .map(PayrollConfigurationAdminService::attendanceTriggerCode)
                .anyMatch(triggerCode::equals);
        if (duplicateTrigger) {
            throw new IllegalArgumentException(
                    "Only one active leave-pay rule may use attendance trigger " + triggerCode
                            + " in " + input.ruleYear() + ". "
                            + "| ក្នុងឆ្នាំ " + input.ruleYear()
                            + " អាចមានច្បាប់ប្រាក់ឈ្នួលពេលឈប់សម្រាកសកម្មតែមួយប៉ុណ្ណោះ "
                            + "សម្រាប់កូដវត្តមាន " + triggerCode + "។");
        }
    }

    private static void requireNonNegative(BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(label + " cannot be negative.");
        }
    }

    private ComponentRow toComponentRow(PayrollComponent component) {
        return new ComponentRow(
                component.getId(), component.getComponentCode(), component.getComponentNameEn(),
                component.getComponentNameKh(), component.getComponentType(),
                component.getCalculationMethod(), component.isAllowManualEntry(), component.isTaxable(), component.isSubjectToNssf(),
                component.isSubjectToSeniority(), component.isActive(), component.getGlAccountCode(),
                component.getSortOrder() == null ? 0 : component.getSortOrder());
    }

    private User currentUser() {
        return authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in."));
    }

    private void requirePermission(AccessPageType type) {
        if (!authenticatedUser.hasPage(PayrollView.class, type)) {
            throw new IllegalArgumentException(
                    "You don't have permission to perform this payroll operation.");
        }
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String attendanceTriggerCode(String ruleCode) {
        return upperOrNull(ruleCode);
    }
}
