package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.CALCULATION_METHODS;
import static org.halocambodia.data.PayrollModels.COMPONENT_TYPES;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollComponent;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollComponentService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;

@Route(value = "payroll-components", layout = MainLayout.class)
@PageTitle("Payroll Components | ធាតុប្រាក់បៀវត្ស")
@PermitAll
public class PayrollComponentView
        extends PageDialogLayout<PayrollComponent, PayrollComponentService> {

    private final TextField componentCode = new TextField("Component Code | កូដធាតុ");
    private final TextField componentNameEn = new TextField(
            "Name (English) | ឈ្មោះអង់គ្លេស");
    private final TextField componentNameKh = new TextField(
            "Name (Khmer) | ឈ្មោះខ្មែរ");
    private final Select<String> componentType = new Select<>();
    private final Select<String> calculationMethod = new Select<>();
    private final Checkbox allowManualEntry = new Checkbox("Manual Entry | បញ្ចូលដោយដៃ");
    private final Checkbox taxable = new Checkbox("Taxable | ជាប់ពន្ធ");
    private final Checkbox subjectToNssf = new Checkbox("Subject to NSSF | ជាប់ ប.ស.ស.");
    private final Checkbox subjectToSeniority = new Checkbox(
            "Seniority Base | មូលដ្ឋានគណនាអតីតភាព");
    private final Checkbox active = new Checkbox("Active | សកម្ម");
    private final TextField glAccountCode = new TextField(
            "General Ledger Account | គណនីបញ្ជីទូទៅ");
    private final IntegerField sortOrder = new IntegerField("Sort Order | លំដាប់");

    public PayrollComponentView(
            PayrollComponentService service,
            UserService userService,
            AuthenticatedUser authenticatedUser) {
        super(PayrollComponent.class, service, userService, authenticatedUser);
        addClassNames("payroll-view", "payroll-component-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(
                PayrollComponentView.class, AccessPageType.SELECTED_PAGE)
                || authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE);
        if (!allowed) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) {
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        if (grid.getColumnByKey("usedByPayrollRules") != null) {
            grid.getColumnByKey("usedByPayrollRules").setSortable(false);
        }
    }

    @Override
    protected void configureEditorLayout() {
        configureFields();

        FormLayout form = new FormLayout(
                componentCode,
                componentNameEn,
                componentNameKh,
                componentType,
                calculationMethod,
                allowManualEntry,
                glAccountCode,
                sortOrder,
                taxable,
                subjectToNssf,
                subjectToSeniority,
                active);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("650px", 2),
                new FormLayout.ResponsiveStep("1050px", 3));

        Paragraph help = new Paragraph(
                "Define the payroll line item here. Payroll Rules that use this component "
                        + "are displayed in the read-only grid column. "
                        + "| កំណត់ធាតុប្រាក់បៀវត្សនៅទីនេះ។ ច្បាប់ដែលប្រើធាតុនេះ "
                        + "ត្រូវបានបង្ហាញក្នុងជួរឈរសម្រាប់មើលតែប៉ុណ្ណោះ។");
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
        componentCode.setRequiredIndicatorVisible(true);
        componentCode.setMaxLength(50);
        componentCode.setHelperText(
                "Use letters, numbers and underscores, for example OVERTIME_PAY. "
                        + "| ប្រើអក្សរ លេខ និងសញ្ញា underscore។");

        componentNameEn.setRequiredIndicatorVisible(true);
        componentNameEn.setMaxLength(200);
        componentNameKh.setMaxLength(200);

        componentType.setLabel("Component Type | ប្រភេទធាតុ");
        componentType.setItems(COMPONENT_TYPES);
        componentType.setRequiredIndicatorVisible(true);
        componentType.addValueChangeListener(event -> {
            boolean earning = "EARNING".equals(event.getValue());
            subjectToSeniority.setEnabled(earning);
            if (!earning) {
                subjectToSeniority.setValue(false);
            }
        });

        calculationMethod.setLabel("Calculation Method | វិធីគណនា");
        calculationMethod.setItems(CALCULATION_METHODS);
        calculationMethod.setRequiredIndicatorVisible(true);
        allowManualEntry.setHelperText(
                "Allow direct one-time entry in Payroll > Add Payroll Item, independent of calculation method. "
                        + "Automatically generated items remain protected. "
                        + "| អនុញ្ញាតឱ្យបញ្ចូលធាតុម្តងដោយផ្ទាល់នៅ Add Payroll Item។");

        glAccountCode.setMaxLength(50);
        sortOrder.setMin(0);
        sortOrder.setRequiredIndicatorVisible(true);
    }

    @Override
    protected void binderField() {
        binder.forField(componentCode)
                .asRequired("Component code is required. | ត្រូវបញ្ចូលកូដធាតុ។")
                .withValidator(
                        value -> value != null
                                && value.trim().matches("[A-Za-z0-9_]+"),
                        "Use letters, numbers and underscores only. "
                                + "| ប្រើតែអក្សរ លេខ និងសញ្ញា underscore។")
                .bind(PayrollComponent::getComponentCode,
                        PayrollComponent::setComponentCode);
        binder.forField(componentNameEn)
                .asRequired("English name is required. | ត្រូវបញ្ចូលឈ្មោះអង់គ្លេស។")
                .bind(PayrollComponent::getComponentNameEn,
                        PayrollComponent::setComponentNameEn);
        binder.bind(componentNameKh,
                PayrollComponent::getComponentNameKh,
                PayrollComponent::setComponentNameKh);
        binder.forField(componentType)
                .asRequired("Component type is required. | ត្រូវជ្រើសប្រភេទធាតុ។")
                .bind(PayrollComponent::getComponentType,
                        PayrollComponent::setComponentType);
        binder.forField(calculationMethod)
                .asRequired("Calculation method is required. | ត្រូវជ្រើសវិធីគណនា។")
                .bind(PayrollComponent::getCalculationMethod,
                        PayrollComponent::setCalculationMethod);
        binder.bind(allowManualEntry, PayrollComponent::isAllowManualEntry, PayrollComponent::setAllowManualEntry);
        binder.bind(taxable, PayrollComponent::isTaxable, PayrollComponent::setTaxable);
        binder.bind(subjectToNssf,
                PayrollComponent::isSubjectToNssf,
                PayrollComponent::setSubjectToNssf);
        binder.bind(subjectToSeniority,
                PayrollComponent::isSubjectToSeniority,
                PayrollComponent::setSubjectToSeniority);
        binder.bind(active, PayrollComponent::isActive, PayrollComponent::setActive);
        binder.bind(glAccountCode,
                PayrollComponent::getGlAccountCode,
                PayrollComponent::setGlAccountCode);
        binder.forField(sortOrder)
                .asRequired("Sort order is required. | ត្រូវបញ្ចូលលំដាប់។")
                .withValidator(
                        value -> value != null && value >= 0,
                        "Sort order cannot be negative. | លំដាប់មិនអាចអវិជ្ជមាន។")
                .bind(PayrollComponent::getSortOrder, PayrollComponent::setSortOrder);
    }

    @Override
    protected PayrollComponent createNewEntity() {
        PayrollComponent value = new PayrollComponent();
        value.setComponentType("EARNING");
        value.setCalculationMethod("MANUAL");
        value.setTaxable(false);
        value.setSubjectToNssf(false);
        value.setSubjectToSeniority(false);
        value.setActive(true);
        value.setSortOrder(0);
        return value;
    }

    @Override
    protected void populateForm(PayrollComponent entityValue) {
        if (entityValue != null && entityValue.getId() != null) {
            this.entity = service.findById(entityValue.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This payroll component no longer exists. Refresh the page. "
                                    + "| ធាតុប្រាក់បៀវត្សនេះលែងមានទៀតហើយ។ សូមផ្ទុកទំព័រឡើងវិញ។"));
        } else {
            this.entity = entityValue == null ? createNewEntity() : entityValue;
        }

        componentCode.setReadOnly(false);
        componentType.setReadOnly(false);
        componentCode.setHelperText(
                "Use letters, numbers and underscores, for example OVERTIME_PAY. "
                        + "| ប្រើអក្សរ លេខ និងសញ្ញា underscore។");

        binder.readBean(this.entity);
        editorLayout.open();
    }

    @Override
    protected void beforeSave(PayrollComponent value, boolean isNew) {
        value.setComponentCode(upper(value.getComponentCode()));
        value.setComponentNameEn(value.getComponentNameEn().trim());
        value.setComponentNameKh(blankToNull(value.getComponentNameKh()));
        value.setComponentType(upper(value.getComponentType()));
        value.setCalculationMethod(upper(value.getCalculationMethod()));
        value.setGlAccountCode(blankToNull(value.getGlAccountCode()));
        value.setSortOrder(value.getSortOrder() == null ? 0 : value.getSortOrder());
    }

    @Override
    protected void focusFirstField() {
        componentCode.focus();
    }

    @Override
    protected List<ColumnDef<PayrollComponent>> getColumnDefs() {
        return List.of(
                col("componentCode", "Code | កូដ",
                        PayrollComponent::getComponentCode,
                        value -> text(value.getComponentCode())),
                col("componentNameEn", "Name (English) | ឈ្មោះអង់គ្លេស",
                        PayrollComponent::getComponentNameEn,
                        value -> text(value.getComponentNameEn())),
                col("componentNameKh", "Name (Khmer) | ឈ្មោះខ្មែរ",
                        PayrollComponent::getComponentNameKh,
                        value -> text(value.getComponentNameKh())),
                col("componentType", "Type | ប្រភេទ",
                        PayrollComponent::getComponentType,
                        value -> text(value.getComponentType()).replace('_', ' ')),
                col("calculationMethod", "Calculation | ការគណនា",
                        PayrollComponent::getCalculationMethod,
                        value -> text(value.getCalculationMethod())),
                col("allowManualEntry", "Manual Entry | បញ្ចូលដោយដៃ",
                        PayrollComponent::isAllowManualEntry,
                        value -> yesNo(value.isAllowManualEntry())),
                col("usedByPayrollRules", "Used By Payroll Rules | ប្រើដោយច្បាប់",
                        PayrollComponent::getUsedByPayrollRules,
                        value -> text(value.getUsedByPayrollRules())),
                col("taxable", "Taxable | ជាប់ពន្ធ",
                        PayrollComponent::isTaxable,
                        value -> yesNo(value.isTaxable())),
                col("subjectToNssf", "NSSF | ប.ស.ស.",
                        PayrollComponent::isSubjectToNssf,
                        value -> yesNo(value.isSubjectToNssf())),
                col("subjectToSeniority", "Seniority Base | មូលដ្ឋានអតីតភាព",
                        PayrollComponent::isSubjectToSeniority,
                        value -> yesNo(value.isSubjectToSeniority())),
                col("active", "Active | សកម្ម",
                        PayrollComponent::isActive,
                        value -> value.isActive() ? "Active | សកម្ម" : "Inactive | អសកម្ម"),
                col("glAccountCode", "General Ledger Account | គណនីបញ្ជីទូទៅ",
                        PayrollComponent::getGlAccountCode,
                        value -> text(value.getGlAccountCode())),
                col("sortOrder", "Order | លំដាប់",
                        PayrollComponent::getSortOrder,
                        value -> text(value.getSortOrder())));
    }

    @Override
    protected Specification<PayrollComponent> buildCombinedSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> filterTokens = new ArrayList<>();

            String quickSearch = quickSearchField == null ? null : quickSearchField.getValue();
            if (quickSearch != null && !quickSearch.isBlank()) {
                String value = quickSearch.trim();
                String like = "%" + value.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        buildLikePredicate(criteriaBuilder, root.get("componentCode"), like),
                        buildLikePredicate(criteriaBuilder, root.get("componentNameEn"), like),
                        buildLikePredicate(criteriaBuilder, root.get("componentNameKh"), like),
                        buildLikePredicate(criteriaBuilder, root.get("componentType"), like),
                        buildLikePredicate(criteriaBuilder, root.get("calculationMethod"), like),
                        buildLikePredicate(criteriaBuilder, root.get("glAccountCode"), like)));
                filterTokens.add("Quick Search: " + value);
            }

            if (advPanel != null) {
                TextField codeFilter = advPanel.getField("componentCode", TextField.class);
                if (hasText(codeFilter)) {
                    String value = codeFilter.getValue().trim();
                    predicates.add(buildLikePredicate(
                            criteriaBuilder,
                            root.get("componentCode"),
                            "%" + value.toLowerCase(Locale.ROOT) + "%"));
                    filterTokens.add("Code: " + value);
                }

                TextField nameFilter = advPanel.getField("componentName", TextField.class);
                if (hasText(nameFilter)) {
                    String value = nameFilter.getValue().trim();
                    String like = "%" + value.toLowerCase(Locale.ROOT) + "%";
                    predicates.add(criteriaBuilder.or(
                            buildLikePredicate(criteriaBuilder, root.get("componentNameEn"), like),
                            buildLikePredicate(criteriaBuilder, root.get("componentNameKh"), like)));
                    filterTokens.add("Name: " + value);
                }

                Select<String> typeFilter = advPanel.getField("componentType", Select.class);
                if (typeFilter != null && typeFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("componentType"), typeFilter.getValue()));
                    filterTokens.add("Type: " + typeFilter.getValue());
                }

                Select<String> methodFilter = advPanel.getField("calculationMethod", Select.class);
                if (methodFilter != null && methodFilter.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(
                            root.get("calculationMethod"), methodFilter.getValue()));
                    filterTokens.add("Calculation: " + methodFilter.getValue());
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
                textFilter("componentCode", "Component Code | កូដធាតុ"),
                textFilter("componentName", "Component Name | ឈ្មោះធាតុ"),
                new FilterDef(
                        "componentType",
                        "Component Type | ប្រភេទធាតុ",
                        () -> selectFilter(COMPONENT_TYPES),
                        component -> ((Select<?>) component).clear()),
                new FilterDef(
                        "calculationMethod",
                        "Calculation Method | វិធីគណនា",
                        () -> selectFilter(CALCULATION_METHODS),
                        component -> ((Select<?>) component).clear()),
                new FilterDef(
                        "active",
                        "Active | សកម្ម",
                        PayrollComponentView::activeFilter,
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
        return "payroll component";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "payroll components";
    }

    @Override
    protected Sort getDefaultSort() {
        return Sort.by(
                Sort.Order.asc("sortOrder"),
                Sort.Order.asc("componentCode"));
    }

    private static FilterDef textFilter(String key, String label) {
        return new FilterDef(
                key,
                label,
                () -> {
                    TextField field = new TextField();
                    field.setPlaceholder("Contains...");
                    field.setWidthFull();
                    return field;
                },
                component -> ((TextField) component).clear());
    }

    private static Select<String> selectFilter(List<String> values) {
        Select<String> field = new Select<>();
        field.setItems(values);
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

    private static boolean hasText(TextField field) {
        return field != null && field.getValue() != null && !field.getValue().isBlank();
    }

    private static String yesNo(boolean value) {
        return value ? "Yes | បាទ/ចាស" : "No | ទេ";
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
