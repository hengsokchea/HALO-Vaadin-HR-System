package org.halocambodia.views.payroll;

import java.math.BigDecimal;
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
import org.halocambodia.data.PayrollNssfConfig;
import org.halocambodia.data.PayrollNssfWageBand;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollNssfConfigService;
import org.halocambodia.services.PayrollNssfWageBandService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;

@Route(value = "payroll-nssf-rules", layout = MainLayout.class)
@PageTitle("NSSF Rules | ការកំណត់ភាគទាន ប.ស.ស.")
@PermitAll
public class PayrollNssfRuleView
        extends PageDialogLayout<PayrollNssfConfig, PayrollNssfConfigService> {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PayrollNssfWageBandService bandService;
    private final ContractTypeRepository contractTypeRepository;

    // NSSF rule fields. Names intentionally match entity properties.
    private final Select<ContractType> eligibleContractType = new Select<>();
    private final DatePicker effectiveFrom = new DatePicker("Effective From | ចាប់ពីថ្ងៃ");
    private final DatePicker effectiveTo = new DatePicker("Effective To | ដល់ថ្ងៃ");
    private final ComboBox<String> currency = new ComboBox<>(
            "NSSF Currency | រូបិយប័ណ្ណ ប.ស.ស.");
    private final BigDecimalField healthEmployeeRate = new BigDecimalField(
            "Health - Employee (%) | សុខភាព - និយោជិត (%)");
    private final BigDecimalField healthEmployerRate = new BigDecimalField(
            "Health - Employer (%) | សុខភាព - និយោជក (%)");
    private final BigDecimalField riskEmployeeRate = new BigDecimalField(
            "Occupational Risk - Employee (%) | ហានិភ័យការងារ - និយោជិត (%)");
    private final BigDecimalField riskEmployerRate = new BigDecimalField(
            "Occupational Risk - Employer (%) | ហានិភ័យការងារ - និយោជក (%)");
    private final BigDecimalField pensionEmployeeRate = new BigDecimalField(
            "Pension - Employee (%) | សោធន - និយោជិត (%)");
    private final BigDecimalField pensionEmployerRate = new BigDecimalField(
            "Pension - Employer (%) | សោធន - និយោជក (%)");
    private final BigDecimalField pensionMinWage = new BigDecimalField(
            "Pension Minimum Wage (KHR) | ប្រាក់ឈ្នួលសោធនអប្បបរមា (KHR)");
    private final BigDecimalField pensionMaxWage = new BigDecimalField(
            "Pension Maximum Wage (KHR) | ប្រាក់ឈ្នួលសោធនអតិបរមា (KHR)");
    private final Checkbox active = new Checkbox("Active | សកម្ម");
    private final TextField sourceReference = new TextField("Source / Reference | ប្រភពយោង");
    private final TextArea notes = new TextArea("Notes | កំណត់សម្គាល់");

    // Wage-band changes remain in this buffer until the parent dialog is saved.
    private final Grid<PayrollNssfWageBand> editorBandGrid =
            new Grid<>(PayrollNssfWageBand.class, false);
    private final List<PayrollNssfWageBand> bandBuffer = new ArrayList<>();
    private final Span bandCount = new Span();
    private Button addBandButton;

    // The parent Save / Cancel footer is shared by both editor tabs.
    private final Tabs editorTabs = new Tabs();
    private final Tab contributionSettingsTab = new Tab(
            "Contribution Settings | ការកំណត់ភាគទាន");
    private final Tab wageBandsTab = new Tab(
            "Healthcare / Risk Wage Bands | កម្រិតប្រាក់ឈ្នួល");
    private final Div contributionTabContent = new Div();
    private final Div wageBandTabContent = new Div();
    private boolean editorTabsListenerRegistered;
    private boolean repairingEditorTabs;

    public PayrollNssfRuleView(
            PayrollNssfConfigService service,
            PayrollNssfWageBandService bandService,
            ContractTypeRepository contractTypeRepository,
            UserService userService,
            AuthenticatedUser authenticatedUser) {
        super(PayrollNssfConfig.class, service, userService, authenticatedUser);
        this.bandService = bandService;
        this.contractTypeRepository = contractTypeRepository;
        addClassNames("payroll-view", "payroll-nssf-rule-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(
                PayrollNssfRuleView.class, AccessPageType.SELECTED_PAGE)
                || authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE);
        if (!allowed) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        enableToggleColumn = true;
        toggleColumnFrozen = true;
        toggleColumnWidth = 56;
        toggleColumnKey = "nssfWageBands";

        configureGrid();
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::buildReadOnlyBandDetails));
        configureEditorLayout();
        binderField();
    }

    @Override
    protected void configureEditorLayout() {
        configureRuleFields();

        FormLayout form = new FormLayout(
                eligibleContractType, effectiveFrom, effectiveTo, currency,
                healthEmployeeRate, healthEmployerRate,
                riskEmployeeRate, riskEmployerRate,
                pensionEmployeeRate, pensionEmployerRate,
                pensionMinWage, pensionMaxWage, active,
                sourceReference, notes);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("650px", 2),
                new FormLayout.ResponsiveStep("1050px", 3));
        form.setColspan(sourceReference, 3);
        form.setColspan(notes, 3);

        contributionTabContent.removeAll();
        contributionTabContent.add(form);
        contributionTabContent.setWidthFull();

        wageBandTabContent.removeAll();
        wageBandTabContent.add(buildBandEditorSection());
        wageBandTabContent.setWidthFull();

        Div contentContainer = new Div(contributionTabContent, wageBandTabContent);
        contentContainer.setWidthFull();
        contentContainer.getStyle()
                .set("padding", "1rem 0")
                .set("box-sizing", "border-box");

        configureEditorTabs();
        showEditorTab(contributionSettingsTab);

        Div wrapper = new Div(editorTabs, contentContainer);
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("padding", "0 1rem")
                .set("box-sizing", "border-box");

        editorLayout.add(wrapper);
        configureEditorFooter();
    }

    /**
     * PageDialogLayout keeps one dialog instance and reuses it for Add/Edit.
     * Keep this setup idempotent so a repeated layout setup cannot register
     * duplicate listeners or leave the stored Tab instances detached.
     */
    private void configureEditorTabs() {
        editorTabs.setWidthFull();
        ensureEditorTabsAttached();
        if (!editorTabsListenerRegistered) {
            editorTabs.addSelectedChangeListener(event -> {
                if (!repairingEditorTabs) {
                    showEditorTab(event.getSelectedTab());
                }
            });
            editorTabsListenerRegistered = true;
        }
    }

    private void ensureEditorTabsAttached() {
        boolean contributionAttached = isEditorTabChild(contributionSettingsTab);
        boolean wageBandsAttached = isEditorTabChild(wageBandsTab);
        if (contributionAttached && wageBandsAttached) {
            return;
        }

        repairingEditorTabs = true;
        try {
            editorTabs.removeAll();
            editorTabs.add(contributionSettingsTab, wageBandsTab);
        } finally {
            repairingEditorTabs = false;
        }
    }

    private boolean isEditorTabChild(Tab tab) {
        return tab != null
                && editorTabs.getChildren().anyMatch(child -> child == tab);
    }

    private void showEditorTab(Tab selectedTab) {
        ensureEditorTabsAttached();
        Tab tab = selectedTab == wageBandsTab
                ? wageBandsTab
                : contributionSettingsTab;
        if (editorTabs.getSelectedTab() != tab) {
            repairingEditorTabs = true;
            try {
                editorTabs.setSelectedTab(tab);
            } finally {
                repairingEditorTabs = false;
            }
        }
        boolean showContributionSettings = tab == contributionSettingsTab;
        contributionTabContent.setVisible(showContributionSettings);
        wageBandTabContent.setVisible(!showContributionSettings);
    }

    private void configureRuleFields() {
        eligibleContractType.setLabel(
                "Eligible Contract Type | ប្រភេទកិច្ចសន្យាមានសិទ្ធិ");
        refreshEligibleContractTypeItems(null);
        eligibleContractType.setItemLabelGenerator(
                PayrollNssfRuleView::contractTypeLabel);
        eligibleContractType.setEmptySelectionAllowed(false);
        eligibleContractType.setRequiredIndicatorVisible(true);
        eligibleContractType.setHelperText(
                "NSSF is calculated only for employees with this same Contract Type. "
                        + "Other or missing Contract Types are ignored. "
                        + "| ប.ស.ស. គណនាតែបុគ្គលិកដែលមានប្រភេទកិច្ចសន្យាដូចគ្នា។ "
                        + "ប្រភេទផ្សេង ឬមិនបានកំណត់ នឹងមិនត្រូវគណនា។");

        effectiveFrom.setRequiredIndicatorVisible(true);
        effectiveTo.setHelperText(
                "Leave empty when the rule has no planned end date. "
                        + "| ទុកទទេ ប្រសិនបើច្បាប់មិនមានកាលបរិច្ឆេទបញ្ចប់។");

        currency.setItems("KHR", "USD");
        currency.setItemLabelGenerator(PayrollNssfRuleView::currencyLabel);
        currency.setAllowCustomValue(false);
        currency.setClearButtonVisible(false);
        currency.setRequiredIndicatorVisible(true);
        currency.setHelperText(
                "KHR is the default for new NSSF rules. "
                        + "| KHR ជារូបិយប័ណ្ណលំនាំដើមសម្រាប់ច្បាប់ ប.ស.ស. ថ្មី។");

        for (BigDecimalField field : List.of(
                healthEmployeeRate, healthEmployerRate,
                riskEmployeeRate, riskEmployerRate,
                pensionEmployeeRate, pensionEmployerRate,
                pensionMinWage, pensionMaxWage)) {
            field.setRequiredIndicatorVisible(true);
        }
        pensionMaxWage.setHelperText(
                "Must be greater than or equal to the pension minimum wage. "
                        + "| ត្រូវធំជាង ឬស្មើប្រាក់ឈ្នួលសោធនអប្បបរមា។");
        sourceReference.setWidthFull();
        notes.setHeight("110px");
    }

    private void refreshEligibleContractTypeItems(PayrollNssfConfig config) {
        List<ContractType> contractTypes = new ArrayList<>(contractTypeRepository.findAll());
        contractTypes.removeIf(java.util.Objects::isNull);

        ContractType selected = config == null ? null : config.getEligibleContractType();
        ContractType matchingItem = null;
        if (selected != null && selected.getId() != null) {
            matchingItem = contractTypes.stream()
                    .filter(item -> java.util.Objects.equals(item.getId(), selected.getId()))
                    .findFirst()
                    .orElse(null);
        }

        // Keep an already-linked value visible even if it is no longer returned by
        // the normal lookup (for example, legacy/inactive reference data).
        if (selected != null && matchingItem == null) {
            contractTypes.add(selected);
            matchingItem = selected;
        }

        contractTypes.sort(Comparator.comparing(
                PayrollNssfRuleView::contractTypeLabel,
                String.CASE_INSENSITIVE_ORDER));
        eligibleContractType.setItems(contractTypes);

        if (config != null && matchingItem != null) {
            config.setEligibleContractType(matchingItem);
        }
    }

    @Override
    protected void binderField() {
        binder.forField(eligibleContractType)
                .asRequired(
                        "Eligible Contract Type is required. "
                                + "| ត្រូវជ្រើសរើសប្រភេទកិច្ចសន្យាមានសិទ្ធិ។")
                .bind(PayrollNssfConfig::getEligibleContractType,
                        PayrollNssfConfig::setEligibleContractType);
        binder.forField(effectiveFrom)
                .asRequired("Effective-from date is required. | ត្រូវបញ្ចូលកាលបរិច្ឆេទចាប់ផ្ដើម។")
                .bind(PayrollNssfConfig::getEffectiveFrom, PayrollNssfConfig::setEffectiveFrom);
        binder.forField(effectiveTo)
                .bind(PayrollNssfConfig::getEffectiveTo, PayrollNssfConfig::setEffectiveTo);
        binder.forField(currency)
                .asRequired("Currency is required. | ត្រូវបញ្ចូលរូបិយប័ណ្ណ។")
                .withValidator(
                        value -> "KHR".equals(value) || "USD".equals(value),
                        "Select KHR or USD. | សូមជ្រើសរើស KHR ឬ USD។")
                .bind(PayrollNssfConfig::getCurrency, PayrollNssfConfig::setCurrency);

        bindPercentage(
                healthEmployeeRate,
                PayrollNssfConfig::getHealthEmployeeRate,
                PayrollNssfConfig::setHealthEmployeeRate,
                "Health employee rate");
        bindPercentage(
                healthEmployerRate,
                PayrollNssfConfig::getHealthEmployerRate,
                PayrollNssfConfig::setHealthEmployerRate,
                "Health employer rate");
        bindPercentage(
                riskEmployeeRate,
                PayrollNssfConfig::getRiskEmployeeRate,
                PayrollNssfConfig::setRiskEmployeeRate,
                "Risk employee rate");
        bindPercentage(
                riskEmployerRate,
                PayrollNssfConfig::getRiskEmployerRate,
                PayrollNssfConfig::setRiskEmployerRate,
                "Risk employer rate");
        bindPercentage(
                pensionEmployeeRate,
                PayrollNssfConfig::getPensionEmployeeRate,
                PayrollNssfConfig::setPensionEmployeeRate,
                "Pension employee rate");
        bindPercentage(
                pensionEmployerRate,
                PayrollNssfConfig::getPensionEmployerRate,
                PayrollNssfConfig::setPensionEmployerRate,
                "Pension employer rate");

        binder.forField(pensionMinWage)
                .asRequired("Pension minimum wage is required. | ត្រូវបញ្ចូលប្រាក់ឈ្នួលសោធនអប្បបរមា។")
                .withValidator(
                        value -> value != null && value.signum() >= 0,
                        "Pension minimum wage cannot be negative. | ប្រាក់ឈ្នួលអប្បបរមាមិនអាចអវិជ្ជមាន។")
                .bind(PayrollNssfConfig::getPensionMinWage, PayrollNssfConfig::setPensionMinWage);
        binder.forField(pensionMaxWage)
                .asRequired("Pension maximum wage is required. | ត្រូវបញ្ចូលប្រាក់ឈ្នួលសោធនអតិបរមា។")
                .withValidator(
                        value -> value != null && value.signum() >= 0,
                        "Pension maximum wage cannot be negative. | ប្រាក់ឈ្នួលអតិបរមាមិនអាចអវិជ្ជមាន។")
                .bind(PayrollNssfConfig::getPensionMaxWage, PayrollNssfConfig::setPensionMaxWage);

        binder.bind(active, PayrollNssfConfig::isActive, PayrollNssfConfig::setActive);
        binder.bind(sourceReference,
                PayrollNssfConfig::getSourceReference,
                PayrollNssfConfig::setSourceReference);
        binder.bind(notes, PayrollNssfConfig::getNotes, PayrollNssfConfig::setNotes);
    }

    private void bindPercentage(
            BigDecimalField field,
            com.vaadin.flow.function.ValueProvider<PayrollNssfConfig, BigDecimal> getter,
            com.vaadin.flow.data.binder.Setter<PayrollNssfConfig, BigDecimal> setter,
            String label) {
        binder.forField(field)
                .asRequired(label + " is required. | ត្រូវបញ្ចូលអត្រា។")
                .withValidator(
                        PayrollNssfRuleView::isPercentage,
                        label + " must be between 0 and 100 percent. "
                                + "| អត្រាត្រូវនៅចន្លោះ 0 និង 100 ភាគរយ។")
                .bind(getter, setter);
    }

    @Override
    protected PayrollNssfConfig createNewEntity() {
        int year = LocalDate.now().getYear();
        PayrollNssfConfig value = new PayrollNssfConfig();
        value.setEffectiveFrom(LocalDate.of(year, 1, 1));
        value.setEffectiveTo(LocalDate.of(year, 12, 31));
        value.setCurrency("KHR");
        value.setHealthEmployeeRate(ZERO);
        value.setHealthEmployerRate(ZERO);
        value.setRiskEmployeeRate(ZERO);
        value.setRiskEmployerRate(ZERO);
        value.setPensionEmployeeRate(ZERO);
        value.setPensionEmployerRate(ZERO);
        value.setPensionMinWage(ZERO);
        value.setPensionMaxWage(ZERO);
        value.setActive(false);
        return value;
    }

    @Override
    protected void populateForm(PayrollNssfConfig entityValue) {
        if (entityValue != null && entityValue.getId() != null) {
            this.entity = service.findById(entityValue.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This NSSF rule no longer exists. Refresh the page. "
                                    + "| ច្បាប់ ប.ស.ស. នេះលែងមានទៀតហើយ។ សូមផ្ទុកទំព័រឡើងវិញ។"));
        } else {
            this.entity = entityValue == null ? createNewEntity() : entityValue;
        }

        // Select matches object instances unless the entity defines ID-based equality.
        // Reload the options and replace the detached ContractType on the rule with
        // the exact option instance having the same database ID before binding.
        refreshEligibleContractTypeItems(this.entity);

        loadBandBuffer(this.entity);
        binder.readBean(this.entity);
        showEditorTab(contributionSettingsTab);
        editorLayout.open();
    }

    @Override
    protected void beforeSave(PayrollNssfConfig value, boolean isNew) {
        value.setCurrency(value.getCurrency().trim().toUpperCase(Locale.ROOT));
        value.setSourceReference(blankToNull(value.getSourceReference()));
        value.setNotes(blankToNull(value.getNotes()));

        if (value.getEffectiveTo() != null
                && value.getEffectiveTo().isBefore(value.getEffectiveFrom())) {
            throw new IllegalArgumentException(
                    "Effective-to date cannot be before effective-from date. "
                            + "| កាលបរិច្ឆេទបញ្ចប់មិនអាចមុនកាលបរិច្ឆេទចាប់ផ្ដើម។");
        }
        if (value.getPensionMaxWage().compareTo(value.getPensionMinWage()) < 0) {
            throw new IllegalArgumentException(
                    "Pension maximum wage must be greater than or equal to the minimum. "
                            + "| ប្រាក់ឈ្នួលសោធនអតិបរមាត្រូវធំជាង ឬស្មើអប្បបរមា។");
        }
    }

    @Override
    protected PayrollNssfConfig save() throws Exception {
        if (entity == null) {
            throw new IllegalStateException(
                    "No NSSF rule to save. | មិនមានច្បាប់ ប.ស.ស. សម្រាប់រក្សាទុក។");
        }

        boolean isNew = entity.getId() == null || entity.getId() == 0L;
        binder.writeBean(entity);
        beforeSave(entity, isNew);
        try {
            validateBandBufferForSave(entity.isActive());
        } catch (IllegalArgumentException exception) {
            showEditorTab(wageBandsTab);
            throw exception;
        }
        entity = service.updateWithBands(entity, bandBuffer);
        afterSave(entity, isNew);
        showSuccessMessage(getSaveSuccessMessage(isNew));
        return entity;
    }

    @Override
    protected void clearForm() {
        super.clearForm();
        bandBuffer.clear();
        refreshEditorBandGrid();
    }

    @Override
    protected void focusFirstField() {
        eligibleContractType.focus();
    }

    @Override
    protected List<ColumnDef<PayrollNssfConfig>> getColumnDefs() {
        return List.of(
                col("eligibleContractType.contractTypeName",
                        "Eligible Contract | កិច្ចសន្យាមានសិទ្ធិ",
                        PayrollNssfConfig::contractTypeName,
                        value -> value.contractTypeName()),
                col("effectiveFrom", "Effective From | ចាប់ពីថ្ងៃ",
                        PayrollNssfConfig::getEffectiveFrom,
                        value -> text(value.getEffectiveFrom())),
                col("effectiveTo", "Effective To | ដល់ថ្ងៃ",
                        PayrollNssfConfig::getEffectiveTo,
                        value -> value.getEffectiveTo() == null
                                ? "No end date | មិនមានថ្ងៃបញ្ចប់"
                                : value.getEffectiveTo().toString()),
                col("currency", "Currency | រូបិយប័ណ្ណ",
                        PayrollNssfConfig::getCurrency,
                        value -> text(value.getCurrency())),
                col("healthEmployeeRate", "Health Employee | សុខភាព-និយោជិត",
                        PayrollNssfConfig::getHealthEmployeeRate,
                        value -> percent(value.getHealthEmployeeRate())),
                col("healthEmployerRate", "Health Employer | សុខភាព-និយោជក",
                        PayrollNssfConfig::getHealthEmployerRate,
                        value -> percent(value.getHealthEmployerRate())),
                col("riskEmployeeRate", "Risk Employee | ហានិភ័យ-និយោជិត",
                        PayrollNssfConfig::getRiskEmployeeRate,
                        value -> percent(value.getRiskEmployeeRate())),
                col("riskEmployerRate", "Risk Employer | ហានិភ័យ-និយោជក",
                        PayrollNssfConfig::getRiskEmployerRate,
                        value -> percent(value.getRiskEmployerRate())),
                col("pensionEmployeeRate", "Pension Employee | សោធន-និយោជិត",
                        PayrollNssfConfig::getPensionEmployeeRate,
                        value -> percent(value.getPensionEmployeeRate())),
                col("pensionEmployerRate", "Pension Employer | សោធន-និយោជក",
                        PayrollNssfConfig::getPensionEmployerRate,
                        value -> percent(value.getPensionEmployerRate())),
                col("pensionMinWage", "Pension Min (KHR) | សោធនអប្បបរមា",
                        PayrollNssfConfig::getPensionMinWage,
                        value -> money(value.getPensionMinWage())),
                col("pensionMaxWage", "Pension Max (KHR) | សោធនអតិបរមា",
                        PayrollNssfConfig::getPensionMaxWage,
                        value -> money(value.getPensionMaxWage())),
                col("active", "Active | សកម្ម",
                        PayrollNssfConfig::isActive,
                        value -> value.isActive() ? "Yes | បាទ/ចាស" : "No | ទេ"),
                col("sourceReference", "Source / Reference | ប្រភពយោង",
                        PayrollNssfConfig::getSourceReference,
                        value -> text(value.getSourceReference())),
                col("notes", "Notes | កំណត់សម្គាល់",
                        PayrollNssfConfig::getNotes,
                        value -> text(value.getNotes())));
    }

    @Override
    protected Specification<PayrollNssfConfig> buildCombinedSpecification() {
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
                        buildLikePredicate(criteriaBuilder, root.get("currency"), like),
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
                            + contractTypeLabel(contractFilter.getValue()));
                }

                DatePicker fromFilter = advPanel.getField("effectiveFrom", DatePicker.class);
                if (fromFilter != null && fromFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("effectiveFrom"), fromFilter.getValue()));
                    filterTokens.add("Effective From: " + fromFilter.getValue());
                }

                TextField currencyFilter = advPanel.getField("currency", TextField.class);
                if (currencyFilter != null
                        && currencyFilter.getValue() != null
                        && !currencyFilter.getValue().isBlank()) {
                    String value = currencyFilter.getValue().trim().toLowerCase(Locale.ROOT);
                    predicates.add(buildLikePredicate(
                            criteriaBuilder, root.get("currency"), "%" + value + "%"));
                    filterTokens.add("Currency: " + currencyFilter.getValue().trim());
                }

                Select<Boolean> activeFilter = advPanel.getField("active", Select.class);
                if (activeFilter != null && activeFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("active"), activeFilter.getValue()));
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
                        "effectiveFrom",
                        "Effective From | ចាប់ពីថ្ងៃ",
                        () -> {
                            DatePicker field = new DatePicker();
                            field.setWidthFull();
                            return field;
                        },
                        component -> ((DatePicker) component).clear()),
                new FilterDef(
                        "currency",
                        "Currency | រូបិយប័ណ្ណ",
                        () -> {
                            TextField field = new TextField();
                            field.setPlaceholder("Contains...");
                            field.setWidthFull();
                            return field;
                        },
                        component -> ((TextField) component).clear()),
                new FilterDef(
                        "active",
                        "Active | សកម្ម",
                        PayrollNssfRuleView::activeFilter,
                        component -> ((Select<?>) component).clear())));
        return advPanel;
    }

    private Select<ContractType> contractTypeFilter() {
        Select<ContractType> field = new Select<>();
        List<ContractType> rows = new ArrayList<>(contractTypeRepository.findAll());
        rows.removeIf(java.util.Objects::isNull);
        rows.sort(Comparator.comparing(
                PayrollNssfRuleView::contractTypeLabel,
                String.CASE_INSENSITIVE_ORDER));
        field.setItems(rows);
        field.setItemLabelGenerator(PayrollNssfRuleView::contractTypeLabel);
        field.setWidthFull();
        return field;
    }

    private static Select<Boolean> activeFilter() {
        Select<Boolean> field = new Select<>();
        field.setItems(true, false);
        field.setItemLabelGenerator(value -> value ? "Yes | បាទ/ចាស" : "No | ទេ");
        field.setPlaceholder("Select...");
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
        return "NSSF rule";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "NSSF rules";
    }

    @Override
    protected Sort getDefaultSort() {
        return Sort.by(
                Sort.Order.desc("effectiveFrom"),
                Sort.Order.desc("id"));
    }

    /** Expanded wage bands under the main NSSF grid are view-only. */
    private Component buildReadOnlyBandDetails(PayrollNssfConfig config) {
        Grid<PayrollNssfWageBand> viewerGrid = new Grid<>(PayrollNssfWageBand.class, false);
        configureBandGrid(viewerGrid);

        List<PayrollNssfWageBand> rows;
        try {
            rows = bandService.findByConfigId(config.getId());
        } catch (Exception exception) {
            rows = List.of();
        }
        viewerGrid.setItems(rows);

        Span count = new Span(rows.size() + " band(s) | " + rows.size() + " កម្រិត");
        count.getElement().getThemeList().add("badge contrast");

        H3 heading = new H3(
                "Healthcare / Risk Wage Bands | កម្រិតប្រាក់ឈ្នួលសុខភាព និងហានិភ័យ — "
                        + config.contractTypeName() + " — " + config.getEffectiveFrom());
        heading.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)");

        HorizontalLayout titleRow = new HorizontalLayout(heading, count);
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);



        VerticalLayout details = new VerticalLayout(titleRow,  viewerGrid);
        details.setWidthFull();
        details.setPadding(true);
        details.setSpacing(true);
        details.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-top", "1px solid var(--lumo-contrast-20pct)")
                .set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                .set("box-sizing", "border-box");
        return details;
    }

    private Component buildBandEditorSection() {
        configureBandGrid(editorBandGrid);
        addEditorBandActionColumn();

        bandCount.getElement().getThemeList().add("badge contrast");
        H3 heading = new H3(
                "Healthcare / Risk Wage Bands | កម្រិតប្រាក់ឈ្នួលសម្រាប់សុខភាព និងហានិភ័យ");
        heading.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)");

        HorizontalLayout titleRow = new HorizontalLayout(heading, bandCount);
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);

        addBandButton = primaryButton("Add | បន្ថែម", VaadinIcon.PLUS);
        addBandButton.addClickListener(event -> openBandDialog(null));

        VerticalLayout section = new VerticalLayout(titleRow, addBandButton, editorBandGrid);
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("margin", "0")
                .set("width", "100%")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-sizing", "border-box");
        refreshEditorBandGrid();
        return section;
    }

    private void configureBandGrid(Grid<PayrollNssfWageBand> bandGrid) {
        bandGrid.setWidthFull();
        bandGrid.setAllRowsVisible(true);
        bandGrid.setSelectionMode(Grid.SelectionMode.NONE);
        bandGrid.setColumnReorderingAllowed(true);
        bandGrid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_COLUMN_BORDERS);

        bandGrid.addColumn(PayrollNssfWageBand::getBandOrder)
                .setHeader(biHeader("Order", "លំដាប់"))
                .setFrozen(true)
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);
        bandGrid.addColumn(value -> value.getMaxSalary() == null
                        ? "No limit | គ្មានកំណត់"
                        : money(value.getMaxSalary()))
                .setHeader(biHeader(
                        "Maximum Monthly Salary (KHR)",
                        "ប្រាក់បៀវត្សប្រចាំខែអតិបរមា"))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        bandGrid.addColumn(value -> money(value.getContributoryWage()))
                .setHeader(biHeader(
                        "Contributory Wage (KHR)",
                        "ប្រាក់ឈ្នួលជាប់ភាគទាន"))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
    }

    private void addEditorBandActionColumn() {
        editorBandGrid.addComponentColumn(band -> {
            boolean newBand = band.getId() == null;
            boolean canEdit = authenticatedUser.hasPage(
                    PayrollView.class,
                    newBand ? AccessPageType.INSERTED_PAGE : AccessPageType.UPDATED_PAGE);
            boolean canRemove = authenticatedUser.hasPage(
                    PayrollView.class,
                    newBand ? AccessPageType.INSERTED_PAGE : AccessPageType.DELETED_PAGE);

            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            edit.setEnabled(canEdit);
            edit.getElement().setAttribute("title", "Edit wage band | កែប្រែកម្រិតប្រាក់ឈ្នួល");
            edit.getElement().setAttribute("aria-label", "Edit wage band | កែប្រែកម្រិតប្រាក់ឈ្នួល");
            edit.addClickListener(event -> openBandDialog(band));

            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addThemeVariants(
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ICON,
                    ButtonVariant.LUMO_ERROR);
            delete.setEnabled(canRemove);
            delete.getElement().setAttribute("title", "Delete wage band | លុបកម្រិតប្រាក់ឈ្នួល");
            delete.getElement().setAttribute("aria-label", "Delete wage band | លុបកម្រិតប្រាក់ឈ្នួល");
            delete.addClickListener(event -> confirmRemoveBand(band));

            HorizontalLayout actions = new HorizontalLayout(edit, delete);
            actions.setPadding(false);
            actions.setSpacing(true);
            actions.setMargin(false);
            actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            return actions;
        })
                .setHeader(biHeader("Action", "សកម្មភាព"))
                .setFrozenToEnd(true)
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
    }

    private void loadBandBuffer(PayrollNssfConfig config) {
        bandBuffer.clear();
        if (config != null && config.getId() != null) {
            bandService.findByConfigId(config.getId()).stream()
                    .map(PayrollNssfRuleView::copyBand)
                    .forEach(bandBuffer::add);
        }
        refreshEditorBandGrid();
        if (addBandButton != null) {
            addBandButton.setEnabled(authenticatedUser.hasPage(
                    PayrollView.class, AccessPageType.INSERTED_PAGE));
        }
    }

    private static PayrollNssfWageBand copyBand(PayrollNssfWageBand source) {
        PayrollNssfWageBand copy = new PayrollNssfWageBand();
        copy.setId(source.getId());
        copy.setPayrollNssfConfigId(source.getPayrollNssfConfigId());
        copy.setBandOrder(source.getBandOrder());
        copy.setMaxSalary(source.getMaxSalary());
        copy.setContributoryWage(source.getContributoryWage());
        copy.setVersion(source.getVersion());
        return copy;
    }

    private void refreshEditorBandGrid() {
        bandBuffer.sort(Comparator.comparing(
                PayrollNssfWageBand::getBandOrder,
                Comparator.nullsLast(Integer::compareTo)));
        editorBandGrid.setItems(new ArrayList<>(bandBuffer));
        bandCount.setText(bandBuffer.size() + " band(s) | " + bandBuffer.size() + " កម្រិត");
    }

    private void openBandDialog(PayrollNssfWageBand existing) {
        CustomDialog dialog = new CustomDialog(existing == null
                ? "New NSSF Wage Band | កម្រិតប្រាក់ឈ្នួលថ្មី"
                : "Edit NSSF Wage Band | កែប្រែកម្រិតប្រាក់ឈ្នួល");
        dialog.addClassName("payroll-dialog");
        dialog.setWidth("min(850px, calc(100vw - 32px))");

        IntegerField order = new IntegerField("Band Order | លំដាប់កម្រិត");
        BigDecimalField maximum = new BigDecimalField(
                "Maximum Monthly Salary (KHR) | ប្រាក់បៀវត្សប្រចាំខែអតិបរមា (KHR)");
        BigDecimalField contributoryWage = new BigDecimalField(
                "Contributory Wage (KHR) | ប្រាក់ឈ្នួលជាប់ភាគទាន (KHR)");
        order.setRequiredIndicatorVisible(true);
        order.setMin(1);
        contributoryWage.setRequiredIndicatorVisible(true);
        maximum.setHelperText(
                "Leave empty only for the final no-limit band. "
                        + "| ទុកទទេសម្រាប់តែកម្រិតចុងក្រោយដែលគ្មានដែនកំណត់។");

        if (existing == null) {
            List<PayrollNssfWageBand> currentRows = bandBuffer.stream()
                    .sorted(Comparator.comparing(
                            PayrollNssfWageBand::getBandOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .toList();
            order.setValue(currentRows.stream()
                    .map(PayrollNssfWageBand::getBandOrder)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0) + 1);
            if (!currentRows.isEmpty()) {
                contributoryWage.setValue(currentRows.getLast().getContributoryWage());
            }
        } else {
            order.setValue(existing.getBandOrder());
            maximum.setValue(existing.getMaxSalary());
            contributoryWage.setValue(existing.getContributoryWage());
        }

        FormLayout form = new FormLayout(order, maximum, contributoryWage);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("650px", 2));

        Span context = new Span(
                "Eligible Contract | កិច្ចសន្យាមានសិទ្ធិ: "
                        + (eligibleContractType.getValue() == null
                                ? "Not selected | មិនបានជ្រើសរើស"
                                : contractTypeLabel(eligibleContractType.getValue()))
                        + " • NSSF Effective From | សុពលភាពចាប់ពី: "
                        + text(effectiveFrom.getValue())
                        + " • " + text(currency.getValue()).toUpperCase(Locale.ROOT));
        context.getElement().getThemeList().add("badge contrast");

        VerticalLayout body = new VerticalLayout(context, form);
        body.setWidthFull();
        body.setPadding(true);
        body.setSpacing(true);
        dialog.add(body);

        Button cancel = new Button(
                "Cancel | បោះបង់",
                new Icon(VaadinIcon.CLOSE),
                event -> dialog.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        cancel.addClickShortcut(Key.ESCAPE);

        Button save = new Button("Save | រក្សាទុក", new Icon(VaadinIcon.CHECK));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        save.addClickShortcut(Key.ENTER);
        save.addClickListener(event -> {
            try {
                save.setEnabled(false);
                validateBandFields(order.getValue(), maximum.getValue(), contributoryWage.getValue());

                boolean duplicateOrder = bandBuffer.stream()
                        .anyMatch(item -> item != existing
                                && item.getBandOrder() != null
                                && item.getBandOrder().equals(order.getValue()));
                if (duplicateOrder) {
                    throw new IllegalArgumentException(
                            "Band order already exists. | លំដាប់កម្រិតនេះមានរួចហើយ។");
                }

                PayrollNssfWageBand value = existing == null
                        ? new PayrollNssfWageBand()
                        : existing;
                value.setPayrollNssfConfigId(entity == null ? null : entity.getId());
                value.setBandOrder(order.getValue());
                value.setMaxSalary(maximum.getValue());
                value.setContributoryWage(contributoryWage.getValue());
                if (existing == null) {
                    bandBuffer.add(value);
                }
                refreshEditorBandGrid();
                dialog.close();
            } catch (Exception exception) {
                save.setEnabled(true);
                error(rootMessage(exception));
            }
        });

        dialog.getFooter().add(cancel, save);
        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                order.focus();
            }
        });
        dialog.open();
    }

    private void confirmRemoveBand(PayrollNssfWageBand band) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.addClassName("payroll-dialog");
        confirm.setHeader("Delete NSSF Wage Band | លុបកម្រិតប្រាក់ឈ្នួល ប.ស.ស.");
        confirm.setText(
                "Remove wage band " + band.getBandOrder()
                        + " from this NSSF Rule dialog? It will be deleted from the database only "
                        + "after you click Save. | ដកកម្រិតប្រាក់ឈ្នួល " + band.getBandOrder()
                        + " ចេញពីផ្ទាំងនេះមែនទេ? វានឹងត្រូវលុបពីប្រព័ន្ធពេលចុច រក្សាទុក។");
        confirm.setCancelable(true);
        confirm.setCancelText("Cancel | បោះបង់");
        confirm.setConfirmText("Delete | លុប");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(event -> {
            bandBuffer.remove(band);
            refreshEditorBandGrid();
        });
        confirm.open();
    }

    private void validateBandBufferForSave(boolean activeRule) {
        Set<Integer> orders = new java.util.HashSet<>();
        for (PayrollNssfWageBand band : bandBuffer) {
            validateBandFields(band.getBandOrder(), band.getMaxSalary(), band.getContributoryWage());
            if (!orders.add(band.getBandOrder())) {
                throw new IllegalArgumentException(
                        "Band order " + band.getBandOrder()
                                + " is duplicated. | លំដាប់កម្រិត ប.ស.ស. ស្ទួន។");
            }
        }

        if (!activeRule) {
            return;
        }
        if (bandBuffer.isEmpty()) {
            throw new IllegalArgumentException(
                    "An active NSSF Rule must contain wage bands. "
                            + "| ច្បាប់ ប.ស.ស. សកម្មត្រូវមានកម្រិតប្រាក់ឈ្នួល។");
        }

        List<PayrollNssfWageBand> rows = bandBuffer.stream()
                .sorted(Comparator.comparing(PayrollNssfWageBand::getBandOrder))
                .toList();
        BigDecimal previousMaximum = null;
        BigDecimal previousWage = null;
        for (int index = 0; index < rows.size(); index++) {
            PayrollNssfWageBand band = rows.get(index);
            int expectedOrder = index + 1;
            if (band.getBandOrder() != expectedOrder) {
                throw new IllegalArgumentException(
                        "NSSF band order must be continuous from 1. Missing order "
                                + expectedOrder + ". | លំដាប់កម្រិត ប.ស.ស. ត្រូវបន្តគ្នាចាប់ពីលេខ 1។");
            }

            boolean finalBand = index == rows.size() - 1;
            if (!finalBand && band.getMaxSalary() == null) {
                throw new IllegalArgumentException(
                        "Only the final NSSF band may have no maximum. "
                                + "| មានតែកម្រិត ប.ស.ស. ចុងក្រោយដែលអាចគ្មានអតិបរមា។");
            }
            if (finalBand && band.getMaxSalary() != null) {
                throw new IllegalArgumentException(
                        "The final NSSF band must have no maximum. "
                                + "| កម្រិត ប.ស.ស. ចុងក្រោយត្រូវគ្មានអតិបរមា។");
            }
            if (previousMaximum != null && band.getMaxSalary() != null
                    && band.getMaxSalary().compareTo(previousMaximum) <= 0) {
                throw new IllegalArgumentException(
                        "NSSF maximum salaries must increase with each band. "
                                + "| ប្រាក់បៀវត្សអតិបរមា ប.ស.ស. ត្រូវកើនតាមលំដាប់។");
            }
            if (previousWage != null && band.getContributoryWage().compareTo(previousWage) < 0) {
                throw new IllegalArgumentException(
                        "NSSF contributory wages cannot decrease in higher bands. "
                                + "| ប្រាក់ឈ្នួលជាប់ភាគទានមិនអាចថយចុះនៅកម្រិតខ្ពស់ជាង។");
            }
            previousMaximum = band.getMaxSalary();
            previousWage = band.getContributoryWage();
        }
    }

    private static void validateBandFields(
            Integer order,
            BigDecimal maximum,
            BigDecimal contributoryWage) {
        if (order == null || order < 1) {
            throw new IllegalArgumentException(
                    "Band order must start at 1. | លំដាប់កម្រិតត្រូវចាប់ពីលេខ 1។");
        }
        if (maximum != null && maximum.signum() < 0) {
            throw new IllegalArgumentException(
                    "Maximum salary cannot be negative. | ប្រាក់បៀវត្សអតិបរមាមិនអាចអវិជ្ជមាន។");
        }
        if (contributoryWage == null || contributoryWage.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Contributory wage must be greater than zero. "
                            + "| ប្រាក់ឈ្នួលជាប់ភាគទានត្រូវធំជាងសូន្យ។");
        }
    }

    private static boolean isPercentage(BigDecimal value) {
        return value != null && value.signum() >= 0 && value.compareTo(ONE_HUNDRED) <= 0;
    }

    private static Component biHeader(String english, String khmer) {
        FlexLayout header = new FlexLayout();
        header.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        header.setWidthFull();
        header.getStyle().set("min-height", "48px");

        Span en = new Span(english);
        Span kh = new Span(khmer);
        en.getStyle().set("text-align", "center").set("width", "100%");
        kh.getStyle().set("text-align", "center").set("width", "100%");
        header.add(en, kh);
        return header;
    }

    private static Button primaryButton(String caption, VaadinIcon icon) {
        Button button = new Button(caption, new Icon(icon));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    private static String money(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String percent(BigDecimal value) {
        return value == null ? "" : money(value) + "%";
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    /**
     * Select invokes its item label generator while an optional field is being
     * cleared or rebound. The empty selection is represented by null, so the
     * formatter must never dereference the value directly.
     */
    private static String contractTypeLabel(ContractType value) {
        return value == null ? "" : text(value.getContractTypeName());
    }

    private static String currencyLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case "KHR" -> "KHR - Cambodian Riel | រៀលខ្មែរ";
            case "USD" -> "USD - US Dollar | ដុល្លារអាមេរិក";
            default -> value;
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null
                ? throwable.getClass().getSimpleName()
                : cause.getMessage();
    }

    private static void error(String message) {
        Notification notification = Notification.show(
                message, 6500, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
