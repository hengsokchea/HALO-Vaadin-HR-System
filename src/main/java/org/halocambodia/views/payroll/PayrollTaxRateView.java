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
import org.halocambodia.data.PayrollTaxBracket;
import org.halocambodia.data.PayrollTaxConfig;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollTaxBracketService;
import org.halocambodia.services.PayrollTaxConfigService;
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
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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

@Route(value = "payroll-tax-rates", layout = MainLayout.class)
@PageTitle("Salary Tax Rules | អត្រាពន្ធលើប្រាក់បៀវត្ស")
@PermitAll
public class PayrollTaxRateView
        extends PageDialogLayout<PayrollTaxConfig, PayrollTaxConfigService> {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CURRENT_YEAR = LocalDate.now().getYear();

    private final PayrollTaxBracketService bracketService;

    // Tax-year editor fields. Names intentionally match entity properties.
    private final IntegerField taxYear = new IntegerField("Tax Year | ឆ្នាំពន្ធ");
    private final TextField currency = new TextField("Currency | រូបិយប័ណ្ណ");
    private final BigDecimalField dependentAllowance = new BigDecimalField(
            "Spouse / Dependant Allowance | ប្រាក់កាត់បន្ថយអ្នកក្នុងបន្ទុក");
    private final BigDecimalField nonResidentRate = new BigDecimalField(
            "Non-Resident Rate (%) | អត្រាអនិវាសនជន");
    private final Checkbox active = new Checkbox("Active | សកម្ម");
    private final TextArea sourceReference = new TextArea("Source / Reference | ប្រភពយោង");
    private final TextArea notes = new TextArea("Notes | កំណត់សម្គាល់");

    // Child records are edited in memory inside the Tax Year dialog and are
    // persisted together with their parent only when the user clicks Save.
    private final Grid<PayrollTaxBracket> editorBracketGrid = new Grid<>(PayrollTaxBracket.class, false);
    private final List<PayrollTaxBracket> bracketBuffer = new ArrayList<>();
    private final Span bracketCount = new Span();
    private Button addBracketButton;

    public PayrollTaxRateView(
            PayrollTaxConfigService service,
            PayrollTaxBracketService bracketService,
            UserService userService,
            AuthenticatedUser authenticatedUser) {
        super(PayrollTaxConfig.class, service, userService, authenticatedUser);
        this.bracketService = bracketService;
        addClassNames("payroll-view", "payroll-tax-rate-view");
    }

    /**
     * Keep compatibility with the payroll parent-page permission currently used
     * by PayrollService while also supporting a dedicated permission for this view.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(
                PayrollTaxRateView.class, AccessPageType.SELECTED_PAGE)
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
        toggleColumnKey = "taxBrackets";

        configureGrid();
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::buildReadOnlyBracketDetails));
        configureEditorLayout();
        binderField();
    }

    @Override
    protected void configureEditorLayout() {
        configureTaxYearFields();

        FormLayout form = new FormLayout(
                taxYear,
                currency,
                dependentAllowance,
                nonResidentRate,
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
                "Maintain the Tax Year settings and its Progressive Tax Brackets in this dialog, "
                        + "then click Save once to store them together. "
                        + "| កំណត់ឆ្នាំពន្ធ និងថ្នាក់អត្រាពន្ធក្នុងផ្ទាំងនេះ "
                        + "រួចចុច រក្សាទុក ម្តង ដើម្បីរក្សាទុកទាំងអស់ជាមួយគ្នា។");
        help.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Div wrapper = new Div(help, form);
        wrapper.setWidthFull();
        wrapper.getStyle()
                .set("padding", "1rem")
                .set("box-sizing", "border-box");

        editorLayout.add(wrapper, buildBracketEditorSection());
        configureEditorFooter();
    }

    private void configureTaxYearFields() {
        taxYear.setRequiredIndicatorVisible(true);
        taxYear.setMin(2000);
        taxYear.setMax(2100);

        currency.setRequiredIndicatorVisible(true);
        currency.setMaxLength(3);
        currency.setHelperText("Three-letter currency code, for example KHR. | កូដរូបិយប័ណ្ណ 3 អក្សរ។");

        dependentAllowance.setRequiredIndicatorVisible(true);
        nonResidentRate.setRequiredIndicatorVisible(true);
        nonResidentRate.setHelperText(
                "Value must be between 0 and 100. | តម្លៃត្រូវនៅចន្លោះ 0 និង 100។");

        sourceReference.setHeight("100px");
        notes.setHeight("120px");
    }

    @Override
    protected void binderField() {
        binder.forField(taxYear)
                .asRequired("Tax year is required. | ត្រូវបញ្ចូលឆ្នាំពន្ធ។")
                .withValidator(
                        value -> value != null && value >= 2000 && value <= 2100,
                        "Tax year must be between 2000 and 2100. | ឆ្នាំពន្ធត្រូវនៅចន្លោះ 2000 និង 2100។")
                .bind(PayrollTaxConfig::getTaxYear, PayrollTaxConfig::setTaxYear);

        binder.forField(currency)
                .asRequired("Currency is required. | ត្រូវបញ្ចូលរូបិយប័ណ្ណ។")
                .withValidator(
                        value -> value != null && value.trim().matches("[A-Za-z]{3}"),
                        "Currency must contain exactly three letters. | រូបិយប័ណ្ណត្រូវមានអក្សរ 3 តួ។")
                .bind(PayrollTaxConfig::getCurrency, PayrollTaxConfig::setCurrency);

        binder.forField(dependentAllowance)
                .asRequired("Allowance is required. | ត្រូវបញ្ចូលប្រាក់កាត់បន្ថយ។")
                .withValidator(
                        value -> value != null && value.signum() >= 0,
                        "Allowance cannot be negative. | ប្រាក់កាត់បន្ថយមិនអាចអវិជ្ជមាន។")
                .bind(PayrollTaxConfig::getDependentAllowance, PayrollTaxConfig::setDependentAllowance);

        binder.forField(nonResidentRate)
                .asRequired("Non-resident rate is required. | ត្រូវបញ្ចូលអត្រាអនិវាសនជន។")
                .withValidator(
                        PayrollTaxRateView::isPercentage,
                        "Rate must be between 0 and 100. | អត្រាត្រូវនៅចន្លោះ 0 និង 100។")
                .bind(PayrollTaxConfig::getNonResidentRate, PayrollTaxConfig::setNonResidentRate);

        binder.bind(active, PayrollTaxConfig::isActive, PayrollTaxConfig::setActive);
        binder.bind(sourceReference,
                PayrollTaxConfig::getSourceReference,
                PayrollTaxConfig::setSourceReference);
        binder.bind(notes, PayrollTaxConfig::getNotes, PayrollTaxConfig::setNotes);
    }

    @Override
    protected PayrollTaxConfig createNewEntity() {
        PayrollTaxConfig value = new PayrollTaxConfig();
        value.setTaxYear(LocalDate.now().getYear());
        value.setCurrency("KHR");
        value.setDependentAllowance(ZERO);
        value.setNonResidentRate(new BigDecimal("20"));
        value.setActive(false);
        return value;
    }

    @Override
    protected void populateForm(PayrollTaxConfig entityValue) {
        if (entityValue != null && entityValue.getId() != null) {
            this.entity = service.findById(entityValue.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This tax year no longer exists. Refresh the page. "
                                    + "| ឆ្នាំពន្ធនេះលែងមានទៀតហើយ។ សូមផ្ទុកទំព័រឡើងវិញ។"));
        } else {
            this.entity = entityValue == null ? createNewEntity() : entityValue;
        }

        loadBracketBuffer(this.entity);
        binder.readBean(this.entity);
        editorLayout.open();
    }

    @Override
    protected void beforeSave(PayrollTaxConfig value, boolean isNew) {
        value.setCurrency(value.getCurrency().trim().toUpperCase(Locale.ROOT));
        value.setSourceReference(blankToNull(value.getSourceReference()));
        value.setNotes(blankToNull(value.getNotes()));
    }

    @Override
    protected PayrollTaxConfig save() throws Exception {
        if (entity == null) {
            throw new IllegalStateException(
                    "No tax year to save. | មិនមានឆ្នាំពន្ធសម្រាប់រក្សាទុក។");
        }

        boolean isNew = entity.getId() == null || entity.getId() == 0L;
        binder.writeBean(entity);
        beforeSave(entity, isNew);
        validateBracketBufferForSave(entity.isActive());
        entity = service.updateWithBrackets(entity, bracketBuffer);
        afterSave(entity, isNew);
        showSuccessMessage(getSaveSuccessMessage(isNew));
        return entity;
    }

    @Override
    protected void clearForm() {
        super.clearForm();
        bracketBuffer.clear();
        refreshEditorBracketGrid();
    }

    @Override
    protected void focusFirstField() {
        taxYear.focus();
    }

    @Override
    protected List<ColumnDef<PayrollTaxConfig>> getColumnDefs() {
        return List.of(
                col(
                        "taxYear",
                        "Tax Year | ឆ្នាំពន្ធ",
                        PayrollTaxConfig::getTaxYear,
                        value -> text(value.getTaxYear())),
                col(
                        "currency",
                        "Currency | រូបិយប័ណ្ណ",
                        PayrollTaxConfig::getCurrency,
                        value -> text(value.getCurrency())),
                col(
                        "dependentAllowance",
                        "Spouse / Dependant Allowance | ប្រាក់កាត់បន្ថយ",
                        PayrollTaxConfig::getDependentAllowance,
                        value -> money(value.getDependentAllowance())),
                col(
                        "nonResidentRate",
                        "Non-Resident Rate | អត្រាអនិវាសនជន",
                        PayrollTaxConfig::getNonResidentRate,
                        value -> percent(value.getNonResidentRate())),
                col(
                        "active",
                        "Active | សកម្ម",
                        PayrollTaxConfig::isActive,
                        value -> value.isActive() ? "Yes | បាទ/ចាស" : "No | ទេ"),
                col(
                        "sourceReference",
                        "Source / Reference | ប្រភពយោង",
                        PayrollTaxConfig::getSourceReference,
                        value -> text(value.getSourceReference())),
                col(
                        "notes",
                        "Notes | កំណត់សម្គាល់",
                        PayrollTaxConfig::getNotes,
                        value -> text(value.getNotes())));
    }

    @Override
    protected Specification<PayrollTaxConfig> buildCombinedSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> filterTokens = new ArrayList<>();

            String quickSearch = quickSearchField == null ? null : quickSearchField.getValue();
            if (quickSearch != null && !quickSearch.isBlank()) {
                String value = quickSearch.trim();
                String like = "%" + value.toLowerCase(Locale.ROOT) + "%";
                List<Predicate> quickPredicates = new ArrayList<>();
                quickPredicates.add(buildLikePredicate(criteriaBuilder, root.get("currency"), like));
                quickPredicates.add(buildLikePredicate(criteriaBuilder, root.get("sourceReference"), like));
                quickPredicates.add(buildLikePredicate(criteriaBuilder, root.get("notes"), like));
                try {
                    quickPredicates.add(criteriaBuilder.equal(root.get("taxYear"), Integer.valueOf(value)));
                } catch (NumberFormatException ignored) {
                    // Text search is still applied to the other columns.
                }
                predicates.add(criteriaBuilder.or(quickPredicates.toArray(Predicate[]::new)));
                filterTokens.add("Quick Search: " + value);
            }

            if (advPanel == null) {
                // Apply the current year before Advanced Search is opened for
                // the first time. Once the panel exists, HR can change the
                // year or use Reset to intentionally display every year.
                predicates.add(criteriaBuilder.equal(root.get("taxYear"), CURRENT_YEAR));
                filterTokens.add("Tax Year: " + CURRENT_YEAR);
            } else {
                IntegerField yearFilter = advPanel.getField("taxYear", IntegerField.class);
                if (yearFilter != null && yearFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("taxYear"), yearFilter.getValue()));
                    filterTokens.add("Tax Year: " + yearFilter.getValue());
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
                        "taxYear",
                        "Tax Year | ឆ្នាំពន្ធ",
                        () -> {
                            IntegerField field = new IntegerField();
                            field.setMin(2000);
                            field.setMax(2100);
                            field.setValue(CURRENT_YEAR);
                            field.setWidthFull();
                            return field;
                        },
                        component -> ((IntegerField) component).clear()),
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
                        PayrollTaxRateView::activeFilter,
                        component -> ((Select<?>) component).clear())));
        // Show the same current-year filter in Advanced Search that is already
        // applied to the grid on initial page load.
        advPanel.addFilter("taxYear");
        return advPanel;
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
        return Set.of("version");
    }

    @Override
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        return action == SensitiveAction.DELETE;
    }

    @Override
    protected String getEntityLabelSingular() {
        return "tax year";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "tax years";
    }

    @Override
    protected Sort getDefaultSort() {
        return Sort.by(Sort.Direction.DESC, "taxYear");
    }

    /**
     * The expanded grid under Tax Year Settings is display-only. Brackets can
     * only be changed from the New/Edit Tax Year dialog.
     */
    private Component buildReadOnlyBracketDetails(PayrollTaxConfig config) {
        Grid<PayrollTaxBracket> viewerGrid = new Grid<>(PayrollTaxBracket.class, false);
        configureBracketGrid(viewerGrid);

        List<PayrollTaxBracket> rows;
        try {
            rows = bracketService.findByTaxConfigId(config.getId());
        } catch (Exception exception) {
            rows = List.of();
        }
        viewerGrid.setItems(rows);

        Span count = new Span(rows.size() + " bracket(s) | " + rows.size() + " ថ្នាក់");
        count.getElement().getThemeList().add("badge contrast");

        H3 heading = new H3(
                "Progressive Tax Brackets | ថ្នាក់អត្រាពន្ធ — " + config.getTaxYear());
        heading.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)");

        HorizontalLayout titleRow = new HorizontalLayout(heading, count);
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);

        Paragraph help = new Paragraph(
                "View only. Use New or Edit Tax Year to manage these brackets. "
                        + "| សម្រាប់មើលតែប៉ុណ្ណោះ។ ប្រើ បន្ថែមឆ្នាំពន្ធ ឬ កែប្រែឆ្នាំពន្ធ "
                        + "ដើម្បីគ្រប់គ្រងថ្នាក់ពន្ធ។");
        help.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout details = new VerticalLayout(titleRow, help, viewerGrid);
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

    private Component buildBracketEditorSection() {
        configureBracketGrid(editorBracketGrid);
        addEditorBracketActionColumn();

        bracketCount.getElement().getThemeList().add("badge contrast");

        H3 heading = new H3("Progressive Tax Brackets | ថ្នាក់អត្រាពន្ធ");
        heading.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)");

        HorizontalLayout titleRow = new HorizontalLayout(heading, bracketCount);
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);

        Paragraph help = new Paragraph(
                "The grid is view-only. Use Add or the row Action icons to change brackets. "
                        + "Order must start at 1, the first minimum must be 0, and only the final "
                        + "bracket may have no maximum. "
                        + "| ក្រឡាចត្រង្គសម្រាប់មើលតែប៉ុណ្ណោះ។ ប្រើ បន្ថែម ឬរូបតំណាងសកម្មភាព "
                        + "ដើម្បីកែប្រែថ្នាក់ពន្ធ។");
        help.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        addBracketButton = primaryButton("Add | បន្ថែម", VaadinIcon.PLUS);
        addBracketButton.addClickListener(event -> openBracketDialog(null));

        VerticalLayout section = new VerticalLayout(
                titleRow, help, addBracketButton, editorBracketGrid);
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("margin", "0 1rem 1rem")
                .set("width", "calc(100% - 2rem)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-sizing", "border-box");
        refreshEditorBracketGrid();
        return section;
    }

    private void configureBracketGrid(Grid<PayrollTaxBracket> bracketGrid) {
        bracketGrid.setWidthFull();
        bracketGrid.setAllRowsVisible(true);
        bracketGrid.setSelectionMode(Grid.SelectionMode.NONE);
        bracketGrid.setColumnReorderingAllowed(true);
        bracketGrid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_COLUMN_BORDERS);

        bracketGrid.addColumn(PayrollTaxBracket::getBracketOrder)
                .setHeader(biHeader("Order", "លំដាប់"))
                .setFrozen(true)
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);
        bracketGrid.addColumn(value -> money(value.getMinAmount()))
                .setHeader(biHeader("Minimum (KHR)", "អប្បបរមា"))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        bracketGrid.addColumn(value -> value.getMaxAmount() == null
                        ? "No limit | គ្មានកំណត់"
                        : money(value.getMaxAmount()))
                .setHeader(biHeader("Maximum (KHR)", "អតិបរមា"))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        bracketGrid.addColumn(value -> percent(value.getTaxRate()))
                .setHeader(biHeader("Tax Rate", "អត្រាពន្ធ"))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        bracketGrid.addColumn(value -> money(value.getDeductionAmount()))
                .setHeader(biHeader("Bracket Deduction (KHR)", "ប្រាក់លម្អៀង"))
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
    }

    private void addEditorBracketActionColumn() {
        editorBracketGrid.addComponentColumn(bracket -> {
            boolean newBracket = bracket.getId() == null;
            boolean canEdit = authenticatedUser.hasPage(
                    PayrollView.class,
                    newBracket ? AccessPageType.INSERTED_PAGE : AccessPageType.UPDATED_PAGE);
            boolean canRemove = authenticatedUser.hasPage(
                    PayrollView.class,
                    newBracket ? AccessPageType.INSERTED_PAGE : AccessPageType.DELETED_PAGE);

            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            edit.setEnabled(canEdit);
            edit.getElement().setAttribute(
                    "title", "Edit tax bracket | កែប្រែថ្នាក់ពន្ធ");
            edit.getElement().setAttribute(
                    "aria-label", "Edit tax bracket | កែប្រែថ្នាក់ពន្ធ");
            edit.addClickListener(event -> openBracketDialog(bracket));

            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.addThemeVariants(
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ICON,
                    ButtonVariant.LUMO_ERROR);
            delete.setEnabled(canRemove);
            delete.getElement().setAttribute(
                    "title", "Delete tax bracket | លុបថ្នាក់ពន្ធ");
            delete.getElement().setAttribute(
                    "aria-label", "Delete tax bracket | លុបថ្នាក់ពន្ធ");
            delete.addClickListener(event -> confirmRemoveBracket(bracket));

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

    private void loadBracketBuffer(PayrollTaxConfig config) {
        bracketBuffer.clear();
        if (config != null && config.getId() != null) {
            bracketService.findByTaxConfigId(config.getId()).stream()
                    .map(PayrollTaxRateView::copyBracket)
                    .forEach(bracketBuffer::add);
        }
        refreshEditorBracketGrid();
        if (addBracketButton != null) {
            addBracketButton.setEnabled(authenticatedUser.hasPage(
                    PayrollView.class, AccessPageType.INSERTED_PAGE));
        }
    }

    private static PayrollTaxBracket copyBracket(PayrollTaxBracket source) {
        PayrollTaxBracket copy = new PayrollTaxBracket();
        copy.setId(source.getId());
        copy.setPayrollTaxConfigId(source.getPayrollTaxConfigId());
        copy.setBracketOrder(source.getBracketOrder());
        copy.setMinAmount(source.getMinAmount());
        copy.setMaxAmount(source.getMaxAmount());
        copy.setTaxRate(source.getTaxRate());
        copy.setDeductionAmount(source.getDeductionAmount());
        copy.setVersion(source.getVersion());
        return copy;
    }

    private void refreshEditorBracketGrid() {
        bracketBuffer.sort(Comparator.comparing(
                PayrollTaxBracket::getBracketOrder,
                Comparator.nullsLast(Integer::compareTo)));
        editorBracketGrid.setItems(new ArrayList<>(bracketBuffer));
        bracketCount.setText(
                bracketBuffer.size() + " bracket(s) | " + bracketBuffer.size() + " ថ្នាក់");
    }

    private void openBracketDialog(PayrollTaxBracket existing) {
        CustomDialog dialog = new CustomDialog(existing == null
                ? "New Tax Bracket | ថ្នាក់ពន្ធថ្មី"
                : "Edit Tax Bracket | កែប្រែថ្នាក់ពន្ធ");
        dialog.addClassName("payroll-dialog");
        dialog.setWidth("min(900px, calc(100vw - 32px))");

        IntegerField order = new IntegerField("Bracket Order | លំដាប់");
        BigDecimalField minimum = new BigDecimalField("Minimum Salary (KHR) | អប្បបរមា");
        BigDecimalField maximum = new BigDecimalField("Maximum Salary (KHR) | អតិបរមា");
        BigDecimalField rate = new BigDecimalField("Tax Rate (%) | អត្រាពន្ធ");
        BigDecimalField deduction = new BigDecimalField(
                "Bracket Deduction (KHR) | ប្រាក់លម្អៀង");

        order.setRequiredIndicatorVisible(true);
        order.setMin(1);
        minimum.setRequiredIndicatorVisible(true);
        rate.setRequiredIndicatorVisible(true);
        deduction.setRequiredIndicatorVisible(true);
        maximum.setHelperText(
                "Leave empty only for the final bracket. | ទុកទទេសម្រាប់តែថ្នាក់ចុងក្រោយ។");

        if (existing == null) {
            List<PayrollTaxBracket> currentRows = bracketBuffer.stream()
                    .sorted(Comparator.comparing(
                            PayrollTaxBracket::getBracketOrder,
                            Comparator.nullsLast(Integer::compareTo)))
                    .toList();
            order.setValue(currentRows.stream()
                    .map(PayrollTaxBracket::getBracketOrder)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0) + 1);
            BigDecimal nextMinimum = currentRows.isEmpty()
                    ? ZERO
                    : currentRows.getLast().getMaxAmount() == null
                            ? ZERO
                            : currentRows.getLast().getMaxAmount().add(BigDecimal.ONE);
            minimum.setValue(nextMinimum);
            rate.setValue(ZERO);
            deduction.setValue(ZERO);
        } else {
            order.setValue(existing.getBracketOrder());
            minimum.setValue(defaultZero(existing.getMinAmount()));
            maximum.setValue(existing.getMaxAmount());
            rate.setValue(defaultZero(existing.getTaxRate()));
            deduction.setValue(defaultZero(existing.getDeductionAmount()));
        }

        FormLayout form = new FormLayout(order, minimum, maximum, rate, deduction);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("650px", 2));

        Span context = new Span(
                "Tax Year | ឆ្នាំពន្ធ: " + text(taxYear.getValue())
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
                validateBracketFields(
                        order.getValue(), minimum.getValue(), maximum.getValue(),
                        rate.getValue(), deduction.getValue());

                boolean duplicateOrder = bracketBuffer.stream()
                        .anyMatch(item -> item != existing
                                && item.getBracketOrder() != null
                                && item.getBracketOrder().equals(order.getValue()));
                if (duplicateOrder) {
                    throw new IllegalArgumentException(
                            "Bracket order already exists. | លំដាប់ថ្នាក់ពន្ធនេះមានរួចហើយ។");
                }

                PayrollTaxBracket value = existing == null
                        ? new PayrollTaxBracket()
                        : existing;
                value.setPayrollTaxConfigId(entity == null ? null : entity.getId());
                value.setBracketOrder(order.getValue());
                value.setMinAmount(minimum.getValue());
                value.setMaxAmount(maximum.getValue());
                value.setTaxRate(rate.getValue());
                value.setDeductionAmount(deduction.getValue());

                if (existing == null) {
                    bracketBuffer.add(value);
                }
                refreshEditorBracketGrid();
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

    private void confirmRemoveBracket(PayrollTaxBracket bracket) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.addClassName("payroll-dialog");
        confirm.setHeader("Delete Tax Bracket | លុបថ្នាក់ពន្ធ");
        confirm.setText(
                "Remove bracket " + bracket.getBracketOrder()
                        + " from this Tax Year dialog? It will be deleted from the database only "
                        + "after you click Save. | ដកថ្នាក់ពន្ធ " + bracket.getBracketOrder()
                        + " ចេញពីផ្ទាំងនេះមែនទេ? វានឹងត្រូវលុបពីប្រព័ន្ធពេលចុច រក្សាទុក។");
        confirm.setCancelable(true);
        confirm.setCancelText("Cancel | បោះបង់");
        confirm.setConfirmText("Delete | លុប");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(event -> {
            bracketBuffer.remove(bracket);
            refreshEditorBracketGrid();
        });
        confirm.open();
    }

    private void validateBracketBufferForSave(boolean activeTaxYear) {
        Set<Integer> orders = new java.util.HashSet<>();
        for (PayrollTaxBracket bracket : bracketBuffer) {
            validateBracketFields(
                    bracket.getBracketOrder(),
                    bracket.getMinAmount(),
                    bracket.getMaxAmount(),
                    bracket.getTaxRate(),
                    bracket.getDeductionAmount());
            if (!orders.add(bracket.getBracketOrder())) {
                throw new IllegalArgumentException(
                        "Bracket order " + bracket.getBracketOrder()
                                + " is duplicated. | លំដាប់ថ្នាក់ពន្ធស្ទួន។");
            }
        }

        // Inactive years may be saved while HR is still preparing their
        // brackets. Active years must have one complete progressive schedule.
        if (!activeTaxYear) {
            return;
        }
        if (bracketBuffer.isEmpty()) {
            throw new IllegalArgumentException(
                    "An active Tax Year must contain tax brackets. "
                            + "| ឆ្នាំពន្ធសកម្មត្រូវមានថ្នាក់អត្រាពន្ធ។");
        }

        List<PayrollTaxBracket> rows = bracketBuffer.stream()
                .sorted(Comparator.comparing(PayrollTaxBracket::getBracketOrder))
                .toList();
        if (rows.getFirst().getMinAmount().compareTo(ZERO) != 0) {
            throw new IllegalArgumentException(
                    "Bracket 1 must start at zero. | ថ្នាក់ពន្ធទី 1 ត្រូវចាប់ផ្ដើមពីសូន្យ។");
        }

        BigDecimal previousMaximum = null;
        for (int index = 0; index < rows.size(); index++) {
            PayrollTaxBracket bracket = rows.get(index);
            int expectedOrder = index + 1;
            if (bracket.getBracketOrder() != expectedOrder) {
                throw new IllegalArgumentException(
                        "Bracket order must be continuous from 1. Missing order "
                                + expectedOrder + ". | លំដាប់ថ្នាក់ពន្ធត្រូវបន្តគ្នាចាប់ពីលេខ 1។");
            }

            boolean finalBracket = index == rows.size() - 1;
            if (!finalBracket && bracket.getMaxAmount() == null) {
                throw new IllegalArgumentException(
                        "Only the final bracket may have no maximum. "
                                + "| មានតែថ្នាក់ពន្ធចុងក្រោយដែលអាចគ្មានអតិបរមា។");
            }
            if (finalBracket && bracket.getMaxAmount() != null) {
                throw new IllegalArgumentException(
                        "The final bracket must have no maximum. "
                                + "| ថ្នាក់ពន្ធចុងក្រោយត្រូវគ្មានអតិបរមា។");
            }

            if (previousMaximum != null) {
                BigDecimal difference = bracket.getMinAmount().subtract(previousMaximum);
                if (difference.signum() < 0 || difference.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalArgumentException(
                            "Tax brackets must be continuous without gaps or overlaps. "
                                    + "| ថ្នាក់ពន្ធត្រូវបន្តគ្នាដោយគ្មានចន្លោះ ឬត្រួតគ្នា។");
                }
            }
            previousMaximum = bracket.getMaxAmount();
        }
    }

    private static void validateBracketFields(
            Integer order,
            BigDecimal minimum,
            BigDecimal maximum,
            BigDecimal rate,
            BigDecimal deduction) {
        if (order == null || order < 1) {
            throw new IllegalArgumentException(
                    "Bracket order must start at 1. | លំដាប់ថ្នាក់ពន្ធត្រូវចាប់ពីលេខ 1។");
        }
        if (minimum == null || minimum.signum() < 0) {
            throw new IllegalArgumentException(
                    "Minimum salary is required and cannot be negative. "
                            + "| ប្រាក់អប្បបរមាត្រូវតែមាន និងមិនអាចអវិជ្ជមាន។");
        }
        if (maximum != null && maximum.compareTo(minimum) < 0) {
            throw new IllegalArgumentException(
                    "Maximum salary cannot be less than minimum salary. "
                            + "| ប្រាក់អតិបរមាមិនអាចតិចជាងប្រាក់អប្បបរមា។");
        }
        if (!isPercentage(rate)) {
            throw new IllegalArgumentException(
                    "Tax rate must be between 0 and 100. | អត្រាពន្ធត្រូវនៅចន្លោះ 0 និង 100។");
        }
        if (deduction == null || deduction.signum() < 0) {
            throw new IllegalArgumentException(
                    "Bracket deduction is required and cannot be negative. "
                            + "| ប្រាក់លម្អៀងត្រូវតែមាន និងមិនអាចអវិជ្ជមាន។");
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
        Button button = actionButton(caption, icon);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    private static Button actionButton(String caption, VaadinIcon icon) {
        return new Button(caption, new Icon(icon));
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
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

    private static void success(String message) {
        Notification notification = Notification.show(
                message, 3500, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private static void error(String message) {
        Notification notification = Notification.show(
                message, 6500, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
