package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.PAYROLL_RULE_SOURCES;
import static org.halocambodia.data.PayrollModels.PAYROLL_RULE_TYPES;
import static org.halocambodia.data.PayrollModels.PAYROLL_RULE_UNITS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollModels.ComponentRow;
import org.halocambodia.data.PayrollPolicyRule;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollPolicyRuleService;
import org.halocambodia.services.PayrollService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "payroll-rules", layout = MainLayout.class)
@PageTitle("Payroll Rules | គោលការណ៍គណនាប្រាក់បៀវត្ស")
@PermitAll
public class PayrollRuleView
        extends PageDialogLayout<PayrollPolicyRule, PayrollPolicyRuleService> {

    private final PayrollService payrollService;

    private final IntegerField ruleYear = new IntegerField("Rule Year | ឆ្នាំ");
    private final TextField ruleCode = new TextField("Rule Code | កូដ");
    private final TextField ruleName = new TextField("Rule Name | ឈ្មោះច្បាប់");
    private final Select<String> ruleType = new Select<>();
    private final Select<String> calculationSource = new Select<>();
    private final ComboBox<ComponentRow> payrollComponent = new ComboBox<>(
            "Payroll Component | ធាតុប្រាក់បៀវត្ស");
    private final Select<String> rateUnit = new Select<>();
    private final BigDecimalField rateAmount = new BigDecimalField(
            "Fixed Rate Amount | អត្រាថេរ");
    private final TextField rateCurrency = new TextField("Rate Currency | រូបិយប័ណ្ណ");
    private final BigDecimalField multiplier = new BigDecimalField(
            "Rate Multiplier | មេគុណអត្រា");
    private final Select<Integer> workdayDivisor = new Select<>();
    private final BigDecimalField payPercentage = new BigDecimalField(
            "Pay Percentage (%) | ភាគរយប្រាក់ឈ្នួល");
    private final BigDecimalField entitlementDays = new BigDecimalField(
            "Entitlement Days | ចំនួនថ្ងៃសិទ្ធិ");
    private final IntegerField serviceYearsPerExtraDay = new IntegerField(
            "Service Years / Extra Day | ឆ្នាំសេវាក្នុងមួយថ្ងៃបន្ថែម");
    private final IntegerField initialFullPayMonths = new IntegerField(
            "Initial Full-Pay Months | ខែទទួលប្រាក់ពេញដំបូង");
    private final IntegerField maxPaidMonths = new IntegerField(
            "Maximum Paid Months | ចំនួនខែទទួលប្រាក់អតិបរមា");
    private final IntegerField sortOrder = new IntegerField("Sort Order | លំដាប់");
    private final Checkbox active = new Checkbox("Active | សកម្ម");
    private final TextArea description = new TextArea("Description | ពិពណ៌នា");

    private List<ComponentRow> components = List.of();
    private Map<Long, ComponentRow> componentsById = Map.of();

    public PayrollRuleView(
            PayrollPolicyRuleService service,
            PayrollService payrollService,
            UserService userService,
            AuthenticatedUser authenticatedUser) {
        super(PayrollPolicyRule.class, service, userService, authenticatedUser);
        this.payrollService = payrollService;
        addClassNames("payroll-view", "payroll-rule-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(
                PayrollRuleView.class, AccessPageType.SELECTED_PAGE)
                || authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE);
        if (!allowed) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) {
        reloadComponents();
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    @Override
    protected void configureEditorLayout() {
        configureFields();

        Paragraph help = new Paragraph(
                "Use DAY for extra-work days such as EW. Use HOUR for recorded overtime hours such as OT. "
                        + "For automatic attendance rules, Rule Code must match the attendance/roster code. "
                        + "For example, PI uses attendance code PI. "
                        + "| ប្រើ DAY សម្រាប់ថ្ងៃធ្វើការបន្ថែម ដូចជា EW និងប្រើ HOUR សម្រាប់ម៉ោងបន្ថែម ដូចជា OT។ "
                        + "សម្រាប់ច្បាប់វត្តមានស្វ័យប្រវត្តិ កូដច្បាប់ត្រូវដូចនឹងកូដវត្តមាន ឬកូដកាលវិភាគ។ "
                        + "ឧទាហរណ៍ ច្បាប់ PI ប្រើកូដវត្តមាន PI។");
        help.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        FormLayout form = new FormLayout(
                ruleYear, ruleCode, ruleName,
                ruleType, calculationSource, payrollComponent,
                rateUnit, multiplier, workdayDivisor, rateAmount, rateCurrency,
                payPercentage, entitlementDays,
                serviceYearsPerExtraDay, initialFullPayMonths, maxPaidMonths,
                sortOrder, active, description);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("650px", 2),
                new FormLayout.ResponsiveStep("1050px", 3));
        form.setColspan(description, 3);

        Div wrapper = new Div(help, form);
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("padding", "1rem")
                .set("box-sizing", "border-box");

        editorLayout.add(wrapper);
        configureEditorFooter();
    }

    private void configureFields() {
        ruleYear.setRequiredIndicatorVisible(true);
        ruleYear.setMin(2000);
        ruleYear.setMax(2100);

        ruleCode.setRequiredIndicatorVisible(true);
        ruleCode.setMaxLength(30);
        ruleCode.setHelperText(
                "Use letters, numbers and underscores only. Automatic rules use this as the attendance trigger code. "
                        + "| ប្រើតែអក្សរ លេខ និងសញ្ញា underscore។ ច្បាប់ស្វ័យប្រវត្តិប្រើកូដនេះជាកូដវត្តមាន។");
        ruleName.setRequiredIndicatorVisible(true);
        ruleName.setMaxLength(200);

        ruleType.setLabel("Rule Type | ប្រភេទច្បាប់");
        ruleType.setItems(PAYROLL_RULE_TYPES);
        ruleType.setRequiredIndicatorVisible(true);

        calculationSource.setLabel("Calculation Source | ប្រភពគណនា");
        calculationSource.setItems(PAYROLL_RULE_SOURCES);
        calculationSource.setRequiredIndicatorVisible(true);

        payrollComponent.setItemLabelGenerator(item -> item.displayName()
                + (item.active() ? "" : " · Inactive | អសកម្ម"));
        payrollComponent.setClearButtonVisible(true);
        payrollComponent.setWidthFull();

        rateUnit.setLabel("Rate Unit | ឯកតាអត្រា");
        rateUnit.setItems(PAYROLL_RULE_UNITS);
        rateUnit.setHelperText(
                "Overtime: DAY uses attendance-code day values; HOUR uses recorded overtime hours. "
                        + "| ម៉ោងបន្ថែម៖ DAY ប្រើចំនួនថ្ងៃ និង HOUR ប្រើចំនួនម៉ោងបន្ថែម។");

        workdayDivisor.setLabel("Workday Divisor | ចំនួនថ្ងៃចែក");
        workdayDivisor.setItems(22, 24, 26);
        workdayDivisor.setRequiredIndicatorVisible(true);
        workdayDivisor.setHelperText(
                "Used by this rule when converting monthly basic salary to daily/hourly rate. "
                        + "Seniority Payment Rules keep their own divisor. "
                        + "| ប្រើសម្រាប់ច្បាប់នេះប៉ុណ្ណោះ; ច្បាប់អតីតភាពមានចំនួនថ្ងៃចែកដាច់ដោយឡែក។");

        rateCurrency.setMaxLength(3);
        serviceYearsPerExtraDay.setMin(0);
        initialFullPayMonths.setMin(0);
        maxPaidMonths.setMin(0);
        sortOrder.setMin(0);
        description.setMinHeight("100px");
        description.setWidthFull();

        ruleType.addValueChangeListener(event -> updateFieldVisibility());
        calculationSource.addValueChangeListener(event -> updateFieldVisibility());
        updateFieldVisibility();
    }

    private void updateFieldVisibility() {
        String selectedType = ruleType.getValue();
        boolean allowance = "ALLOWANCE".equals(selectedType);
        boolean deduction = "DEDUCTION".equals(selectedType);
        boolean overtime = "OVERTIME".equals(selectedType);
        boolean rateRule = deduction || overtime;
        boolean entitlement = "LEAVE_ENTITLEMENT".equals(selectedType);
        boolean leavePay = "LEAVE_PAY".equals(selectedType);

        rateAmount.setVisible(allowance);
        rateCurrency.setVisible(allowance);
        multiplier.setVisible(rateRule);
        rateUnit.setVisible(allowance || rateRule);
        workdayDivisor.setVisible(rateRule || leavePay);
        workdayDivisor.setRequiredIndicatorVisible(rateRule || leavePay);
        payPercentage.setVisible(leavePay);
        entitlementDays.setVisible(entitlement);
        serviceYearsPerExtraDay.setVisible(entitlement);
        initialFullPayMonths.setVisible(leavePay);
        maxPaidMonths.setVisible(leavePay);
        payrollComponent.setVisible(allowance || rateRule || leavePay);
        payrollComponent.setRequiredIndicatorVisible(allowance || rateRule || leavePay);

        updateRateUnitOptions(selectedType, calculationSource.getValue());

        if (overtime) {
            multiplier.setLabel("Overtime Multiplier | មេគុណម៉ោងបន្ថែម");
        } else if (deduction) {
            multiplier.setLabel("Daily Rate Multiplier | មេគុណអត្រាប្រចាំថ្ងៃ");
        } else {
            multiplier.setLabel("Rate Multiplier | មេគុណអត្រា");
        }

        String requiredComponentType = expectedComponentType(selectedType);
        ComponentRow selected = payrollComponent.getValue();
        List<ComponentRow> compatible = components.stream()
                .filter(item -> requiredComponentType == null
                        || requiredComponentType.equals(item.componentType()))
                .toList();
        payrollComponent.setItems(compatible);
        if (selected != null && compatible.stream().anyMatch(item -> item.id().equals(selected.id()))) {
            payrollComponent.setValue(selected);
        } else if (selected != null) {
            payrollComponent.clear();
        }
        if (entitlement) {
            payrollComponent.clear();
        }
    }

    private void updateRateUnitOptions(String selectedType, String selectedSource) {
        String selectedUnit = rateUnit.getValue();
        List<String> allowedUnits;
        if ("ATTENDANCE".equals(selectedSource) && "OVERTIME".equals(selectedType)) {
            allowedUnits = List.of("DAY", "HOUR");
        } else if ("ATTENDANCE".equals(selectedSource) && "ALLOWANCE".equals(selectedType)) {
            allowedUnits = List.of("DAY", "CYCLE", "MONTH");
        } else if ("DEDUCTION".equals(selectedType)) {
            allowedUnits = List.of("DAY");
        } else {
            allowedUnits = PAYROLL_RULE_UNITS;
        }
        rateUnit.setItems(allowedUnits);
        if (selectedUnit != null && allowedUnits.contains(selectedUnit)) {
            rateUnit.setValue(selectedUnit);
        } else if (rateUnit.isVisible()) {
            rateUnit.clear();
        }
    }

    @Override
    protected void binderField() {
        binder.forField(ruleYear)
                .asRequired("Rule year is required. | ត្រូវបញ្ចូលឆ្នាំច្បាប់។")
                .withValidator(value -> value != null && value >= 2000 && value <= 2100,
                        "Rule year must be between 2000 and 2100. | ឆ្នាំត្រូវនៅចន្លោះ 2000 និង 2100។")
                .bind(PayrollPolicyRule::getRuleYear, PayrollPolicyRule::setRuleYear);
        binder.forField(ruleCode)
                .asRequired("Rule code is required. | ត្រូវបញ្ចូលកូដច្បាប់។")
                .withValidator(value -> value != null
                                && value.trim().matches("[A-Za-z0-9_]+"),
                        "Use letters, numbers and underscores only. "
                                + "| ប្រើតែអក្សរ លេខ និងសញ្ញា underscore។")
                .bind(PayrollPolicyRule::getRuleCode, PayrollPolicyRule::setRuleCode);
        binder.forField(ruleName)
                .asRequired("Rule name is required. | ត្រូវបញ្ចូលឈ្មោះច្បាប់។")
                .bind(PayrollPolicyRule::getRuleName, PayrollPolicyRule::setRuleName);
        binder.forField(ruleType)
                .asRequired("Rule type is required. | ត្រូវជ្រើសប្រភេទច្បាប់។")
                .bind(PayrollPolicyRule::getRuleType, PayrollPolicyRule::setRuleType);
        binder.forField(calculationSource)
                .asRequired("Calculation source is required. | ត្រូវជ្រើសប្រភពគណនា។")
                .bind(PayrollPolicyRule::getCalculationSource,
                        PayrollPolicyRule::setCalculationSource);
        binder.forField(payrollComponent)
                .withValidator(value -> !requiresComponent(ruleType.getValue()) || value != null,
                        "Payroll Component is required for this rule. | ត្រូវជ្រើសធាតុប្រាក់បៀវត្ស។")
                .bind(this::componentForRule,
                        (value, component) -> value.setPayrollComponentId(
                                component == null ? null : component.id()));
        binder.forField(rateUnit)
                .withValidator(this::validRateUnit,
                        "Choose a supported rate unit. Overtime uses DAY or HOUR. "
                                + "| សូមជ្រើសឯកតាត្រឹមត្រូវ។ ម៉ោងបន្ថែមប្រើ DAY ឬ HOUR។")
                .bind(PayrollPolicyRule::getRateUnit, PayrollPolicyRule::setRateUnit);
        binder.forField(rateAmount)
                .withValidator(value -> !active.getValue()
                                || !"ALLOWANCE".equals(ruleType.getValue()) || positive(value),
                        "Active allowance rate must be greater than zero. | អត្រាប្រាក់ឧបត្ថម្ភត្រូវធំជាងសូន្យ។")
                .bind(PayrollPolicyRule::getRateAmount, PayrollPolicyRule::setRateAmount);
        binder.forField(rateCurrency)
                .withValidator(value -> !active.getValue()
                                || !"ALLOWANCE".equals(ruleType.getValue())
                                || value != null && value.trim().matches("[A-Za-z]{3}"),
                        "Allowance currency must contain three letters. | រូបិយប័ណ្ណត្រូវមានអក្សរ 3 តួ។")
                .bind(PayrollPolicyRule::getRateCurrency, PayrollPolicyRule::setRateCurrency);
        binder.forField(multiplier)
                .withValidator(value -> !active.getValue()
                                || !isRateRule(ruleType.getValue()) || positive(value),
                        "Multiplier must be greater than zero. | មេគុណត្រូវធំជាងសូន្យ។")
                .bind(PayrollPolicyRule::getMultiplier, PayrollPolicyRule::setMultiplier);
        binder.forField(workdayDivisor)
                .withValidator(value -> value != null && Set.of(22, 24, 26).contains(value),
                        "Workday divisor must be 22, 24, or 26. | ចំនួនថ្ងៃចែកត្រូវជា 22, 24 ឬ 26។")
                .bind(PayrollPolicyRule::getWorkdayDivisor, PayrollPolicyRule::setWorkdayDivisor);
        binder.forField(payPercentage)
                .withValidator(value -> !active.getValue()
                                || !"LEAVE_PAY".equals(ruleType.getValue())
                                || percentage(value),
                        "Pay percentage must be between 0 and 100. | ភាគរយត្រូវនៅចន្លោះ 0 និង 100។")
                .bind(PayrollPolicyRule::getPayPercentage, PayrollPolicyRule::setPayPercentage);
        binder.forField(entitlementDays)
                .withValidator(value -> !active.getValue()
                                || !"LEAVE_ENTITLEMENT".equals(ruleType.getValue())
                                || nonNegative(value),
                        "Entitlement days are required and cannot be negative. "
                                + "| ត្រូវបញ្ចូលចំនួនថ្ងៃសិទ្ធិដែលមិនអវិជ្ជមាន។")
                .bind(PayrollPolicyRule::getEntitlementDays,
                        PayrollPolicyRule::setEntitlementDays);
        binder.bind(serviceYearsPerExtraDay,
                PayrollPolicyRule::getServiceYearsPerExtraDay,
                PayrollPolicyRule::setServiceYearsPerExtraDay);
        binder.forField(initialFullPayMonths)
                .withValidator(value -> value == null || value >= 0,
                        "Initial full-pay months cannot be negative. "
                                + "| ចំនួនខែបើកប្រាក់ពេញដំបូងមិនអាចអវិជ្ជមាន។")
                .withValidator(value -> value == null || maxPaidMonths.getValue() == null
                                || value <= maxPaidMonths.getValue(),
                        "Initial Full-Pay Months cannot exceed Maximum Paid Months. "
                                + "| ចំនួនខែបើកប្រាក់ពេញដំបូងមិនអាចលើសចំនួនខែអតិបរមា។")
                .bind(PayrollPolicyRule::getInitialFullPayMonths,
                        PayrollPolicyRule::setInitialFullPayMonths);
        binder.forField(maxPaidMonths)
                .withValidator(value -> !active.getValue()
                                || !"LEAVE_PAY".equals(ruleType.getValue())
                                || value != null && value > 0,
                        "An active leave-pay rule requires Maximum Paid Months greater than zero. "
                                + "| ច្បាប់ប្រាក់ឈ្នួលពេលឈប់សម្រាកសកម្មត្រូវមានចំនួនខែអតិបរមាធំជាងសូន្យ។")
                .withValidator(value -> value == null || initialFullPayMonths.getValue() == null
                                || value >= initialFullPayMonths.getValue(),
                        "Maximum Paid Months cannot be less than Initial Full-Pay Months. "
                                + "| ចំនួនខែអតិបរមាមិនអាចតិចជាងចំនួនខែបើកប្រាក់ពេញដំបូង។")
                .bind(PayrollPolicyRule::getMaxPaidMonths,
                        PayrollPolicyRule::setMaxPaidMonths);
        binder.bind(sortOrder, PayrollPolicyRule::getSortOrder, PayrollPolicyRule::setSortOrder);
        binder.bind(active, PayrollPolicyRule::isActive, PayrollPolicyRule::setActive);
        binder.bind(description,
                PayrollPolicyRule::getDescription, PayrollPolicyRule::setDescription);
    }

    @Override
    protected PayrollPolicyRule createNewEntity() {
        PayrollPolicyRule value = new PayrollPolicyRule();
        value.setRuleYear(LocalDate.now().getYear());
        value.setRuleType("ALLOWANCE");
        value.setCalculationSource("MANUAL");
        value.setRateUnit("DAY");
        value.setRateCurrency("USD");
        value.setWorkdayDivisor(22);
        value.setSortOrder(0);
        value.setActive(true);
        return value;
    }

    @Override
    protected void populateForm(PayrollPolicyRule entityValue) {
        reloadComponents();
        if (entityValue != null && entityValue.getId() != null) {
            this.entity = service.findById(entityValue.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This payroll rule no longer exists. Refresh the page. "
                                    + "| ច្បាប់នេះលែងមានទៀតហើយ។ សូមផ្ទុកទំព័រឡើងវិញ។"));
        } else {
            this.entity = entityValue == null ? createNewEntity() : entityValue;
        }
        binder.readBean(this.entity);
        updateFieldVisibility();
        editorLayout.open();
    }

    @Override
    protected void beforeSave(PayrollPolicyRule value, boolean isNew) {
        value.setRuleCode(value.getRuleCode().trim().toUpperCase(Locale.ROOT));
        value.setRuleName(value.getRuleName().trim());
        value.setRuleType(upper(value.getRuleType()));
        value.setCalculationSource(upper(value.getCalculationSource()));
        value.setRateUnit(upperOrNull(value.getRateUnit()));
        value.setRateCurrency(upperOrNull(value.getRateCurrency()));
        value.setDescription(blankToNull(value.getDescription()));
        value.setSortOrder(value.getSortOrder() == null ? 0 : value.getSortOrder());
        value.setWorkdayDivisor(value.getWorkdayDivisor() == null ? 22 : value.getWorkdayDivisor());

        switch (value.getRuleType()) {
            case "ALLOWANCE" -> {
                value.setMultiplier(null);
                clearLeaveFields(value);
            }
            case "DEDUCTION", "OVERTIME" -> {
                value.setRateAmount(null);
                value.setRateCurrency(null);
                clearLeaveFields(value);
            }
            case "LEAVE_ENTITLEMENT" -> {
                clearRateFields(value);
                value.setPayrollComponentId(null);
                value.setPayPercentage(null);
                value.setInitialFullPayMonths(null);
                value.setMaxPaidMonths(null);
            }
            case "LEAVE_PAY" -> {
                clearRateFields(value);
                value.setEntitlementDays(null);
                value.setServiceYearsPerExtraDay(null);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported payroll rule type. | ប្រភេទច្បាប់មិនត្រឹមត្រូវ។");
        }
    }

    @Override
    protected void focusFirstField() {
        ruleYear.focus();
    }

    @Override
    protected List<ColumnDef<PayrollPolicyRule>> getColumnDefs() {
        return List.of(
                col("ruleYear", "Year | ឆ្នាំ",
                        PayrollPolicyRule::getRuleYear,
                        value -> text(value.getRuleYear())),
                col("ruleCode", "Code | កូដ",
                        PayrollPolicyRule::getRuleCode,
                        value -> text(value.getRuleCode())),
                col("ruleName", "Rule Name | ឈ្មោះច្បាប់",
                        PayrollPolicyRule::getRuleName,
                        value -> text(value.getRuleName())),
                col("ruleType", "Type | ប្រភេទ",
                        PayrollPolicyRule::getRuleType,
                        value -> text(value.getRuleType()).replace('_', ' ')),
                col("payrollComponentId", "Payroll Component | ធាតុប្រាក់បៀវត្ស",
                        PayrollPolicyRule::getPayrollComponentId,
                        this::componentLabel),
                col("rateUnit", "Configured Rule | ការកំណត់ច្បាប់",
                        PayrollPolicyRule::getRateUnit,
                        PayrollRuleView::ruleSummary),
                col("workdayDivisor", "Workday Divisor | ចំនួនថ្ងៃចែក",
                        PayrollPolicyRule::getWorkdayDivisor,
                        value -> text(value.getWorkdayDivisor())),
                col("calculationSource", "Source | ប្រភព",
                        PayrollPolicyRule::getCalculationSource,
                        value -> text(value.getCalculationSource())),
                col("active", "Active | សកម្ម",
                        PayrollPolicyRule::isActive,
                        value -> value.isActive() ? "Yes | បាទ/ចាស" : "No | ទេ"),
                col("sortOrder", "Order | លំដាប់",
                        PayrollPolicyRule::getSortOrder,
                        value -> text(value.getSortOrder())),
                col("description", "Description | ពិពណ៌នា",
                        PayrollPolicyRule::getDescription,
                        value -> text(value.getDescription())));
    }

    @Override
    protected Specification<PayrollPolicyRule> buildCombinedSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            List<String> filterTokens = new ArrayList<>();

            String quickSearch = quickSearchField == null ? null : quickSearchField.getValue();
            if (quickSearch != null && !quickSearch.isBlank()) {
                String value = quickSearch.trim();
                String like = "%" + value.toLowerCase(Locale.ROOT) + "%";
                List<jakarta.persistence.criteria.Predicate> quickPredicates = new ArrayList<>();
                quickPredicates.add(buildLikePredicate(
                        criteriaBuilder, root.get("ruleCode"), like));
                quickPredicates.add(buildLikePredicate(
                        criteriaBuilder, root.get("ruleName"), like));
                quickPredicates.add(buildLikePredicate(
                        criteriaBuilder, root.get("ruleType"), like));
                quickPredicates.add(buildLikePredicate(
                        criteriaBuilder, root.get("calculationSource"), like));
                quickPredicates.add(buildLikePredicate(
                        criteriaBuilder, root.get("description"), like));
                try {
                    quickPredicates.add(criteriaBuilder.equal(
                            root.get("ruleYear"), Integer.valueOf(value)));
                } catch (NumberFormatException ignored) {
                    // The text predicates still apply.
                }
                predicates.add(criteriaBuilder.or(
                        quickPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
                filterTokens.add("Quick Search: " + value);
            }

            if (advPanel != null) {
                IntegerField yearFilter = advPanel.getField("ruleYear", IntegerField.class);
                if (yearFilter != null && yearFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("ruleYear"), yearFilter.getValue()));
                    filterTokens.add("Year: " + yearFilter.getValue());
                }

                Select<String> typeFilter = advPanel.getField("ruleType", Select.class);
                if (typeFilter != null && typeFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("ruleType"), typeFilter.getValue()));
                    filterTokens.add("Type: " + typeFilter.getValue());
                }

                Select<Boolean> activeFilter = advPanel.getField("active", Select.class);
                if (activeFilter != null && activeFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("active"), activeFilter.getValue()));
                    filterTokens.add("Active: " + (activeFilter.getValue() ? "Yes" : "No"));
                }
            }

            showSqlFilterTokens(filterTokens);
            return criteriaBuilder.and(
                    predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
                new FilterDef(
                        "ruleYear",
                        "Rule Year | ឆ្នាំ",
                        () -> {
                            IntegerField field = new IntegerField();
                            field.setMin(2000);
                            field.setMax(2100);
                            field.setWidthFull();
                            return field;
                        },
                        component -> ((IntegerField) component).clear()),
                new FilterDef(
                        "ruleType",
                        "Rule Type | ប្រភេទ",
                        () -> {
                            Select<String> field = new Select<>();
                            field.setItems(PAYROLL_RULE_TYPES);
                            field.setWidthFull();
                            return field;
                        },
                        component -> ((Select<?>) component).clear()),
                new FilterDef(
                        "active",
                        "Active | សកម្ម",
                        PayrollRuleView::activeFilter,
                        component -> ((Select<?>) component).clear())));
        return advPanel;
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) {
            advPanel.clearAll();
        }
    }

    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of("version");
    }

    @Override
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        return action == SensitiveAction.DELETE;
    }

    @Override
    protected String getEntityLabelSingular() {
        return "payroll rule";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "payroll rules";
    }

    @Override
    protected Sort getDefaultSort() {
        return Sort.by(
                Sort.Order.desc("ruleYear"),
                Sort.Order.asc("sortOrder"),
                Sort.Order.asc("ruleCode"));
    }

    private void reloadComponents() {
        components = payrollService.findComponents(false);
        componentsById = components.stream().collect(Collectors.toMap(
                ComponentRow::id, component -> component, (first, second) -> first));
        updateFieldVisibility();
    }

    private ComponentRow componentForRule(PayrollPolicyRule value) {
        return value == null || value.getPayrollComponentId() == null
                ? null
                : componentsById.get(value.getPayrollComponentId());
    }

    private String componentLabel(PayrollPolicyRule value) {
        ComponentRow component = componentForRule(value);
        return component == null ? "-" : component.displayName();
    }

    private boolean validRateUnit(String value) {
        String selectedType = ruleType.getValue();
        if (!"ALLOWANCE".equals(selectedType) && !isRateRule(selectedType)) {
            return true;
        }
        String unit = upperOrNull(value);
        if (!active.getValue()) {
            return unit == null || PAYROLL_RULE_UNITS.contains(unit);
        }
        if ("OVERTIME".equals(selectedType)
                && "ATTENDANCE".equals(calculationSource.getValue())) {
            return Set.of("DAY", "HOUR").contains(unit);
        }
        if ("ALLOWANCE".equals(selectedType)
                && "ATTENDANCE".equals(calculationSource.getValue())) {
            return Set.of("DAY", "CYCLE", "MONTH").contains(unit);
        }
        if ("DEDUCTION".equals(selectedType)) {
            return "DAY".equals(unit);
        }
        return unit != null && PAYROLL_RULE_UNITS.contains(unit);
    }

    private static Select<Boolean> activeFilter() {
        Select<Boolean> field = new Select<>();
        field.setItems(true, false);
        field.setItemLabelGenerator(value -> value ? "Yes | បាទ/ចាស" : "No | ទេ");
        field.setWidthFull();
        return field;
    }

    private static String ruleSummary(PayrollPolicyRule value) {
        String type = upper(value.getRuleType());
        return switch (type) {
            case "ALLOWANCE" -> amount(value.getRateAmount()) + " "
                    + text(value.getRateCurrency()) + per(value.getRateUnit());
            case "DEDUCTION" -> amount(value.getMultiplier())
                    + " × daily rate" + per(value.getRateUnit());
            case "OVERTIME" -> amount(value.getMultiplier())
                    + ("HOUR".equals(upper(value.getRateUnit()))
                            ? " × hourly rate / hour"
                            : " × daily rate / day");
            case "LEAVE_ENTITLEMENT" -> {
                String result = amount(value.getEntitlementDays()) + " days/year";
                if (value.getServiceYearsPerExtraDay() != null
                        && value.getServiceYearsPerExtraDay() > 0) {
                    result += " + 1 day / " + value.getServiceYearsPerExtraDay()
                            + " service years";
                }
                yield result;
            }
            case "LEAVE_PAY" -> {
                String result = amount(value.getPayPercentage()) + "% pay";
                if (value.getInitialFullPayMonths() != null
                        && value.getInitialFullPayMonths() > 0) {
                    result = "100% first " + value.getInitialFullPayMonths()
                            + " month(s); " + result;
                }
                if (value.getMaxPaidMonths() != null && value.getMaxPaidMonths() > 0) {
                    result += " through month " + value.getMaxPaidMonths();
                }
                yield result;
            }
            default -> text(value.getDescription());
        };
    }

    private static String expectedComponentType(String type) {
        return switch (upper(type)) {
            case "DEDUCTION", "LEAVE_PAY" -> "DEDUCTION";
            case "OVERTIME", "ALLOWANCE" -> "EARNING";
            default -> null;
        };
    }

    private static boolean requiresComponent(String type) {
        return expectedComponentType(type) != null;
    }

    private static boolean isRateRule(String type) {
        return "DEDUCTION".equals(type) || "OVERTIME".equals(type);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static boolean nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }

    private static boolean percentage(BigDecimal value) {
        return value != null && value.signum() >= 0
                && value.compareTo(new BigDecimal("100")) <= 0;
    }

    private static void clearRateFields(PayrollPolicyRule value) {
        value.setRateAmount(null);
        value.setRateCurrency(null);
        value.setRateUnit(null);
        value.setMultiplier(null);
    }

    private static void clearLeaveFields(PayrollPolicyRule value) {
        value.setPayPercentage(null);
        value.setEntitlementDays(null);
        value.setServiceYearsPerExtraDay(null);
        value.setInitialFullPayMonths(null);
        value.setMaxPaidMonths(null);
    }

    private static String per(String unit) {
        return unit == null || unit.isBlank() ? "" : " / " + unit.toLowerCase(Locale.ROOT);
    }

    private static String amount(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String upperOrNull(String value) {
        String normalized = upper(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
