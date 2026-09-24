package org.halocambodia.views.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.ContractType;
import org.halocambodia.data.ContractTypeRepository;
import org.halocambodia.data.PayrollSeniorityCalculation;
import org.halocambodia.data.PayrollSeniorityRule;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollSeniorityService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "payroll-seniority-rules", layout = MainLayout.class)
@PageTitle("Seniority Payment Rules | ច្បាប់ប្រាក់បំណាច់អតីតភាព")
@PermitAll
public class PayrollSeniorityRuleView
        extends PageDialogLayout<PayrollSeniorityRule, PayrollSeniorityService> {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ContractTypeRepository contractTypeRepository;

    private final Select<ContractType> eligibleContractType = new Select<>();
    private final DatePicker effectiveFrom = new DatePicker("Effective From | ចាប់ពីថ្ងៃ");
    private final DatePicker effectiveTo = new DatePicker("Effective To | ដល់ថ្ងៃ");
    private final Select<Integer> firstPaymentMonth = new Select<>();
    private final Select<Integer> secondPaymentMonth = new Select<>();
    private final BigDecimalField daysPerPayment = new BigDecimalField(
            "Days per Payment | ចំនួនថ្ងៃក្នុងមួយលើក");
    private final Select<Integer> workdayDivisor = new Select<>();
    private final Checkbox requireEmployedAtSemesterEnd = new Checkbox(
            "Must be employed at semester end | ត្រូវនៅបម្រើការងារនៅចុងឆមាស");
    private final Checkbox active = new Checkbox("Active | សកម្ម");
    private final TextField sourceReference = new TextField("Source / Reference | ប្រភពយោង");
    private final TextArea notes = new TextArea("Notes | កំណត់សម្គាល់");

    public PayrollSeniorityRuleView(
            PayrollSeniorityService service,
            ContractTypeRepository contractTypeRepository,
            UserService userService,
            AuthenticatedUser authenticatedUser) {
        super(PayrollSeniorityRule.class, service, userService, authenticatedUser);
        this.contractTypeRepository = contractTypeRepository;
        addClassNames("payroll-view", "payroll-seniority-rule-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(
                PayrollSeniorityRuleView.class, AccessPageType.SELECTED_PAGE)
                || authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE);
        if (!allowed) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) {
        enableToggleColumn = true;
        toggleColumnFrozen = true;
        toggleColumnWidth = 56;
        toggleColumnKey = "seniorityResults";

        configureGrid();
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::buildCalculationDetails));
        configureEditorLayout();
        binderField();
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        if (grid.getColumnByKey("paymentMonths") != null) {
            grid.getColumnByKey("paymentMonths").setSortable(false);
        }
    }

    @Override
    protected void configureEditorLayout() {
        configureFields();

        FormLayout form = new FormLayout(
                eligibleContractType,
                effectiveFrom,
                effectiveTo,
                firstPaymentMonth,
                secondPaymentMonth,
                daysPerPayment,
                workdayDivisor,
                requireEmployedAtSemesterEnd,
                active,
                sourceReference,
                notes);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("650px", 2),
                new FormLayout.ResponsiveStep("1050px", 3));
        form.setColspan(sourceReference, 3);
        form.setColspan(notes, 3);

        Paragraph help = new Paragraph(
                "Configure one effective-dated rule for each eligible UDC contract type. "
                        + "The result expander shows the complete calculation audit. "
                        + "| កំណត់ច្បាប់មានសុពលភាពមួយសម្រាប់ប្រភេទកិច្ចសន្យា UDC នីមួយៗ។ "
                        + "ពង្រីកជួរដើម្បីមើលលម្អិតការគណនា។");
        help.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Div wrapper = new Div(help, form);
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("padding", "1rem")
                .set("box-sizing", "border-box");

        editorLayout.add(wrapper);
        configureEditorFooter();
    }

    private void configureFields() {
        eligibleContractType.setLabel("Eligible Contract Type | ប្រភេទកិច្ចសន្យាមានសិទ្ធិ");
        refreshEligibleContractTypeItems(null);
        eligibleContractType.setItemLabelGenerator(PayrollSeniorityRuleView::contractTypeLabel);
        eligibleContractType.setEmptySelectionAllowed(false);
        eligibleContractType.setRequiredIndicatorVisible(true);

        effectiveFrom.setRequiredIndicatorVisible(true);
        effectiveTo.setHelperText(
                "Leave empty when there is no planned end date. | ទុកទទេ ប្រសិនបើគ្មានថ្ងៃបញ្ចប់។");

        firstPaymentMonth.setLabel("First Payment Month | ខែបើកប្រាក់លើកទី១");
        firstPaymentMonth.setItems(6);
        firstPaymentMonth.setItemLabelGenerator(value -> "06 - June | មិថុនា");
        firstPaymentMonth.setRequiredIndicatorVisible(true);

        secondPaymentMonth.setLabel("Second Payment Month | ខែបើកប្រាក់លើកទី២");
        secondPaymentMonth.setItems(12);
        secondPaymentMonth.setItemLabelGenerator(value -> "12 - December | ធ្នូ");
        secondPaymentMonth.setRequiredIndicatorVisible(true);

        daysPerPayment.setRequiredIndicatorVisible(true);
        daysPerPayment.setHelperText("Current rule: 7.5 days. | ច្បាប់បច្ចុប្បន្ន៖ 7.5 ថ្ងៃ។");

        workdayDivisor.setLabel("Monthly Workday Divisor | ចំនួនថ្ងៃធ្វើការប្រចាំខែ");
        workdayDivisor.setItems(22, 24, 26);
        workdayDivisor.setRequiredIndicatorVisible(true);
        workdayDivisor.setHelperText(
                "Choose the company's actual monthly schedule. | ជ្រើសតាមកាលវិភាគការងាររបស់ក្រុមហ៊ុន។");

        sourceReference.setWidthFull();
        sourceReference.setMaxLength(1000);
        notes.setHeight("110px");
    }

    private void refreshEligibleContractTypeItems(PayrollSeniorityRule rule) {
        List<ContractType> contractTypes = new ArrayList<>(contractTypeRepository.findAll());
        contractTypes.removeIf(java.util.Objects::isNull);

        ContractType selected = rule == null ? null : rule.getEligibleContractType();
        ContractType matchingItem = null;
        if (selected != null && selected.getId() != null) {
            matchingItem = contractTypes.stream()
                    .filter(item -> java.util.Objects.equals(item.getId(), selected.getId()))
                    .findFirst()
                    .orElse(null);
        }

        // Vaadin Select matches the current value against the item instances.
        // The rule loaded for editing can contain a different ContractType entity
        // instance with the same database ID, so remap it to the exact Select item.
        // Keep a legacy linked value visible if it is no longer returned normally.
        if (selected != null && matchingItem == null) {
            contractTypes.add(selected);
            matchingItem = selected;
        }

        contractTypes.sort(Comparator.comparing(
                PayrollSeniorityRuleView::contractTypeLabel,
                String.CASE_INSENSITIVE_ORDER));
        eligibleContractType.setItems(contractTypes);

        if (rule != null && matchingItem != null) {
            rule.setEligibleContractType(matchingItem);
        }
    }

    @Override
    protected void binderField() {
        binder.forField(eligibleContractType)
                .asRequired("Contract type is required. | ត្រូវជ្រើសប្រភេទកិច្ចសន្យា។")
                .bind(PayrollSeniorityRule::getEligibleContractType,
                        PayrollSeniorityRule::setEligibleContractType);
        binder.forField(effectiveFrom)
                .asRequired("Effective-from date is required. | ត្រូវបញ្ចូលថ្ងៃចាប់ផ្តើម។")
                .bind(PayrollSeniorityRule::getEffectiveFrom,
                        PayrollSeniorityRule::setEffectiveFrom);
        binder.bind(effectiveTo,
                PayrollSeniorityRule::getEffectiveTo,
                PayrollSeniorityRule::setEffectiveTo);
        binder.forField(firstPaymentMonth)
                .asRequired("First payment month is required. | ត្រូវជ្រើសខែទី១។")
                .bind(PayrollSeniorityRule::getFirstPaymentMonth,
                        PayrollSeniorityRule::setFirstPaymentMonth);
        binder.forField(secondPaymentMonth)
                .asRequired("Second payment month is required. | ត្រូវជ្រើសខែទី២។")
                .bind(PayrollSeniorityRule::getSecondPaymentMonth,
                        PayrollSeniorityRule::setSecondPaymentMonth);
        binder.forField(daysPerPayment)
                .asRequired("Days per payment is required. | ត្រូវបញ្ចូលចំនួនថ្ងៃ។")
                .withValidator(
                        value -> value != null && value.compareTo(ZERO) > 0,
                        "Days must be greater than zero. | ចំនួនថ្ងៃត្រូវធំជាងសូន្យ។")
                .bind(PayrollSeniorityRule::getDaysPerPayment,
                        PayrollSeniorityRule::setDaysPerPayment);
        binder.forField(workdayDivisor)
                .asRequired("Workday divisor is required. | ត្រូវជ្រើសចំនួនថ្ងៃធ្វើការ។")
                .bind(PayrollSeniorityRule::getWorkdayDivisor,
                        PayrollSeniorityRule::setWorkdayDivisor);
        binder.bind(requireEmployedAtSemesterEnd,
                PayrollSeniorityRule::isRequireEmployedAtSemesterEnd,
                PayrollSeniorityRule::setRequireEmployedAtSemesterEnd);
        binder.bind(active,
                PayrollSeniorityRule::isActive,
                PayrollSeniorityRule::setActive);
        binder.bind(sourceReference,
                PayrollSeniorityRule::getSourceReference,
                PayrollSeniorityRule::setSourceReference);
        binder.bind(notes,
                PayrollSeniorityRule::getNotes,
                PayrollSeniorityRule::setNotes);
    }

    @Override
    protected PayrollSeniorityRule createNewEntity() {
        PayrollSeniorityRule value = new PayrollSeniorityRule();
        value.setEffectiveFrom(LocalDate.of(LocalDate.now().getYear(), 1, 1));
        value.setFirstPaymentMonth(6);
        value.setSecondPaymentMonth(12);
        value.setDaysPerPayment(new BigDecimal("7.50"));
        value.setMinimumEligibleDays(21);
        value.setWorkdayDivisor(26);
        value.setRequireEmployedAtSemesterEnd(true);
        value.setActive(false);
        value.setSourceReference("MLVT Prakas No. 443 and Instruction No. 58/19");
        return value;
    }

    @Override
    protected void populateForm(PayrollSeniorityRule entityValue) {
        if (entityValue != null && entityValue.getId() != null) {
            this.entity = service.findById(entityValue.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This seniority rule no longer exists. Refresh the page. "
                                    + "| ច្បាប់អតីតភាពនេះលែងមានទៀតហើយ។ សូមផ្ទុកឡើងវិញ។"));
        } else {
            this.entity = entityValue == null ? createNewEntity() : entityValue;
        }

        // Reload the options and replace the ContractType reference with the exact
        // item instance having the same database ID before Binder reads the bean.
        // This makes the saved Eligible Contract Type visible when editing.
        refreshEligibleContractTypeItems(this.entity);

        binder.readBean(this.entity);
        editorLayout.open();
    }

    @Override
    protected void beforeSave(PayrollSeniorityRule value, boolean isNew) {
        value.setSourceReference(blankToNull(value.getSourceReference()));
        value.setNotes(blankToNull(value.getNotes()));
    }

    @Override
    protected void focusFirstField() {
        eligibleContractType.focus();
    }

    @Override
    protected List<ColumnDef<PayrollSeniorityRule>> getColumnDefs() {
        return List.of(
                col("eligibleContractType.contractTypeName", "Eligible Contract | កិច្ចសន្យាមានសិទ្ធិ",
                        PayrollSeniorityRule::contractTypeName,
                        value -> value.contractTypeName()),
                col("effectiveFrom", "Effective From | ចាប់ពីថ្ងៃ",
                        PayrollSeniorityRule::getEffectiveFrom,
                        value -> text(value.getEffectiveFrom())),
                col("effectiveTo", "Effective To | ដល់ថ្ងៃ",
                        PayrollSeniorityRule::getEffectiveTo,
                        value -> text(value.getEffectiveTo())),
                col("paymentMonths", "Payment Months | ខែបើកប្រាក់",
                        value -> value.getFirstPaymentMonth() + ", " + value.getSecondPaymentMonth(),
                        value -> monthName(value.getFirstPaymentMonth()) + " / "
                                + monthName(value.getSecondPaymentMonth())),
                col("daysPerPayment", "Days / Payment | ថ្ងៃក្នុងមួយលើក",
                        PayrollSeniorityRule::getDaysPerPayment,
                        value -> decimal(value.getDaysPerPayment(), 2)),
                col("workdayDivisor", "Workday Divisor | ចំនួនថ្ងៃធ្វើការ",
                        PayrollSeniorityRule::getWorkdayDivisor,
                        value -> text(value.getWorkdayDivisor())),
                col("requireEmployedAtSemesterEnd", "Employed at Semester End | នៅធ្វើការចុងឆមាស",
                        PayrollSeniorityRule::isRequireEmployedAtSemesterEnd,
                        value -> yesNo(value.isRequireEmployedAtSemesterEnd())),
                col("active", "Active | សកម្ម",
                        PayrollSeniorityRule::isActive,
                        value -> yesNo(value.isActive())),
                col("sourceReference", "Source / Reference | ប្រភពយោង",
                        PayrollSeniorityRule::getSourceReference,
                        value -> text(value.getSourceReference())),
                col("notes", "Notes | កំណត់សម្គាល់",
                        PayrollSeniorityRule::getNotes,
                        value -> text(value.getNotes())));
    }

    private Component buildCalculationDetails(PayrollSeniorityRule rule) {
        List<PayrollSeniorityCalculation> rows = service.findCalculations(rule.getId());
        if (rows.isEmpty()) {
            Span empty = new Span(
                    "No calculated seniority results use this rule yet. "
                            + "| មិនទាន់មានលទ្ធផលអតីតភាពដែលប្រើច្បាប់នេះទេ។");
            empty.getStyle().set("padding", "0.75rem");
            return empty;
        }

        Grid<PayrollSeniorityCalculation> resultGrid =
                new Grid<>(PayrollSeniorityCalculation.class, false);
        resultGrid.addThemeVariants(
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COLUMN_BORDERS);
        resultGrid.setColumnReorderingAllowed(true);
        resultGrid.getColumns().forEach(column -> column.setResizable(true));
        resultGrid.addColumn(PayrollSeniorityCalculation::insuranceNo)
                .setHeader("Insurance No. | លេខធានារ៉ាប់រង").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::employeeNameEn)
                .setHeader("Employee (English) | ឈ្មោះអង់គ្លេស").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::employeeNameKh)
                .setHeader("Employee (Khmer) | ឈ្មោះខ្មែរ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::payrollRunId)
                .setHeader("Payroll Run ID | លេខដំណើរការ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::getSeniorityYear)
                .setHeader("Year | ឆ្នាំ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::getSemesterNo)
                .setHeader("Semester | ឆមាស").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(value -> value.getEligibilityStart() + " → " + value.getEligibilityEnd())
                .setHeader("Eligibility Dates | កាលបរិច្ឆេទមានសិទ្ធិ")
                .setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::getEligibleCalendarDays)
                .setHeader("Eligible Days | ថ្ងៃមានសិទ្ធិ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::getEligibleMonthCount)
                .setHeader("Months Used | ខែដែលបានប្រើ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(value -> money(value.getTotalEligibleEarnings()))
                .setHeader("Semester Earnings | ចំណូលក្នុងឆមាស").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(value -> money(value.getAverageMonthlyEarnings()))
                .setHeader("Monthly Average | មធ្យមប្រចាំខែ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(PayrollSeniorityCalculation::getWorkdayDivisor)
                .setHeader("Divisor | ចំនួនថ្ងៃ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(value -> decimal(value.getAverageDailyEarnings(), 6))
                .setHeader("Daily Average | មធ្យមប្រចាំថ្ងៃ").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(value -> decimal(value.getEntitlementDays(), 2))
                .setHeader("Entitlement Days | ថ្ងៃទទួលបាន").setResizable(true).setAutoWidth(true);
        resultGrid.addColumn(value -> money(value.getAmount()) + " " + value.getCurrencyCode())
                .setHeader("Seniority Amount | ប្រាក់អតីតភាព").setResizable(true).setAutoWidth(true);
        resultGrid.setItems(rows);
        resultGrid.setHeight("420px");

        H4 heading = new H4("Calculation Results | លទ្ធផលគណនា");
        heading.getStyle().set("margin", "0");
        VerticalLayout layout = new VerticalLayout(heading, resultGrid);
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.setWidthFull();
        return layout;
    }

    @Override
    protected Specification<PayrollSeniorityRule> buildCombinedSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> filterTokens = new ArrayList<>();

            String quickSearch = quickSearchField == null ? null : quickSearchField.getValue();
            if (quickSearch != null && !quickSearch.isBlank()) {
                String value = quickSearch.trim();
                String like = "%" + value.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        buildLikePredicate(criteriaBuilder,
                                root.join("eligibleContractType", JoinType.LEFT)
                                        .get("contractTypeName"), like),
                        buildLikePredicate(criteriaBuilder, root.get("sourceReference"), like),
                        buildLikePredicate(criteriaBuilder, root.get("notes"), like)));
                filterTokens.add("Quick Search: " + value);
            }

            if (advPanel != null) {
                Select<ContractType> contractFilter =
                        advPanel.getField("eligibleContractType", Select.class);
                if (contractFilter != null && contractFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("eligibleContractType"), contractFilter.getValue()));
                    filterTokens.add("Contract: "
                            + contractFilter.getValue().getContractTypeName());
                }

                Select<Boolean> activeFilter = advPanel.getField("active", Select.class);
                if (activeFilter != null && activeFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("active"), activeFilter.getValue()));
                    filterTokens.add("Active: " + (activeFilter.getValue() ? "Yes" : "No"));
                }
            }

            showSqlFilterTokens(filterTokens);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
                new FilterDef(
                        "eligibleContractType",
                        "Eligible Contract Type | ប្រភេទកិច្ចសន្យា",
                        this::contractTypeFilter,
                        component -> ((Select<?>) component).clear()),
                new FilterDef(
                        "active",
                        "Active | សកម្ម",
                        PayrollSeniorityRuleView::activeFilter,
                        component -> ((Select<?>) component).clear())));
        return advPanel;
    }

    private Select<ContractType> contractTypeFilter() {
        Select<ContractType> field = new Select<>();
        List<ContractType> rows = new ArrayList<>(contractTypeRepository.findAll());
        rows.sort(Comparator.comparing(
                value -> text(value.getContractTypeName()),
                String.CASE_INSENSITIVE_ORDER));
        field.setItems(rows);
        field.setItemLabelGenerator(ContractType::getContractTypeName);
        field.setWidthFull();
        return field;
    }

    private static Select<Boolean> activeFilter() {
        Select<Boolean> field = new Select<>();
        field.setItems(true, false);
        field.setItemLabelGenerator(value -> value ? "Active | សកម្ម" : "Inactive | អសកម្ម");
        field.setWidthFull();
        return field;
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) {
            advPanel.clearAll();
        }
    }

    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of("version", "eligibleContractType.version");
    }

    @Override
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        return action == SensitiveAction.DELETE;
    }

    @Override
    protected String getEntityLabelSingular() {
        return "seniority payment rule";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "seniority payment rules";
    }

    @Override
    protected Sort getDefaultSort() {
        return Sort.by(
                Sort.Order.desc("effectiveFrom"),
                Sort.Order.asc("eligibleContractType.contractTypeName"));
    }

    private static String monthName(Integer month) {
        if (month == null) {
            return "";
        }
        return switch (month) {
            case 6 -> "June | មិថុនា";
            case 12 -> "December | ធ្នូ";
            default -> String.valueOf(month);
        };
    }

    private static String yesNo(boolean value) {
        return value ? "Yes | បាទ/ចាស" : "No | ទេ";
    }

    private static String money(BigDecimal value) {
        return decimal(value, 2);
    }

    private static String decimal(BigDecimal value, int scale) {
        return value == null ? "0" : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String contractTypeLabel(ContractType value) {
        return value == null ? "" : text(value.getContractTypeName());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
