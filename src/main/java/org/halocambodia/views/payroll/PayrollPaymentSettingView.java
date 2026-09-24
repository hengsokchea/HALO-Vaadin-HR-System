package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.PAYMENT_FREQUENCIES;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollPaymentSetting;
import org.halocambodia.data.Shift;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollPaymentSettingService;
import org.halocambodia.services.ShiftService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "payroll-payment-settings", layout = MainLayout.class)
@PageTitle("Payroll Payment Settings | ការកំណត់ការបើកប្រាក់")
@PermitAll
public class PayrollPaymentSettingView
        extends PageDialogLayout<PayrollPaymentSetting, PayrollPaymentSettingService> {

    private final ShiftService shiftService;

    private final ComboBox<Shift> shift = new ComboBox<>("Shift | វេន");
    private final Select<String> paymentFrequency = new Select<>();
    private final BigDecimalField firstPaymentPercent = new BigDecimalField(
            "First Payment (%) | ភាគរយបើកលើកទីមួយ");
    private final DatePicker effectiveFrom = new DatePicker(
            "Effective From | ចាប់ផ្ដើមអនុវត្ត");
    private final DatePicker effectiveTo = new DatePicker(
            "Effective To | បញ្ចប់អនុវត្ត");
    private final Checkbox active = new Checkbox("Active | សកម្ម");

    public PayrollPaymentSettingView(
            PayrollPaymentSettingService service,
            ShiftService shiftService,
            UserService userService,
            AuthenticatedUser authenticatedUser) {
        super(PayrollPaymentSetting.class, service, userService, authenticatedUser);
        this.shiftService = shiftService;
        addClassNames("payroll-view", "payroll-payment-setting-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(
                PayrollPaymentSettingView.class, AccessPageType.SELECTED_PAGE)
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
    protected void configureEditorLayout() {
        List<Shift> shifts = new ArrayList<>(shiftService.findAll(null));
        shifts.sort(Comparator.comparing(
                value -> value.getShiftName() == null ? "" : value.getShiftName(),
                String.CASE_INSENSITIVE_ORDER));

        shift.setItems(shifts);
        shift.setItemLabelGenerator(value -> value == null ? "" : value.getShiftName());
        shift.setClearButtonVisible(true);
        shift.setHelperText(
                "MONTHLY only: leave blank to pay all staff monthly, or choose the one Shift that should be SEMI_MONTHLY. "
                        + "SEMI_MONTHLY disables Shift and applies to all staff. "
                        + "| សម្រាប់ MONTHLY៖ ទុកវេនទទេ = បើកប្រចាំខែទាំងអស់; ជ្រើសវេន = វេននោះបើកពីរដង។");

        paymentFrequency.setLabel("Payment Frequency | ចំនួនដងបើកប្រាក់");
        paymentFrequency.setItems(PAYMENT_FREQUENCIES);
        paymentFrequency.setItemLabelGenerator(PayrollPaymentSettingView::frequencyLabel);
        paymentFrequency.setRequiredIndicatorVisible(true);

        firstPaymentPercent.setRequiredIndicatorVisible(true);
        firstPaymentPercent.setHelperText(
                "Used when any staff are SEMI_MONTHLY; normally 50%. "
                        + "| ប្រើនៅពេលមានបុគ្គលិកបើកពីរដង; ជាទូទៅ 50%។");

        effectiveFrom.setRequiredIndicatorVisible(true);
        active.setValue(true);

        paymentFrequency.addValueChangeListener(event -> updateRuleFields());
        shift.addValueChangeListener(event -> updateRuleFields());

        FormLayout form = new FormLayout(
                shift,
                paymentFrequency,
                firstPaymentPercent,
                effectiveFrom,
                effectiveTo,
                active);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2));

        Paragraph help = new Paragraph(
                "Rule behavior: MONTHLY + blank Shift = all staff MONTHLY. "
                        + "MONTHLY + Support = Support SEMI_MONTHLY and all other shifts MONTHLY. "
                        + "MONTHLY + Operation = Operation SEMI_MONTHLY and all other shifts MONTHLY. "
                        + "SEMI_MONTHLY = Shift disabled and all staff SEMI_MONTHLY. "
                        + "The selected Shift is an exception to a MONTHLY company rule, not a separate payment rule. "
                        + "| MONTHLY + វេនទទេ = បុគ្គលិកទាំងអស់បើកប្រចាំខែ; ជ្រើសវេន = វេននោះបើកពីរដង។");
        help.getStyle().set("margin", "0").set("color", "var(--lumo-primary-text-color)");

        Div wrapper = new Div(help, form);
        wrapper.setWidthFull();
        wrapper.getStyle().set("padding", "1rem").set("box-sizing", "border-box");
        editorLayout.add(wrapper);
        configureEditorFooter();
    }

    @Override
    protected void binderField() {
        binder.bind(shift,
                PayrollPaymentSetting::getShift,
                PayrollPaymentSetting::setShift);
        binder.forField(paymentFrequency)
                .asRequired("Select payment frequency. | សូមជ្រើសចំនួនដងបើកប្រាក់។")
                .bind(PayrollPaymentSetting::getPaymentFrequency,
                        PayrollPaymentSetting::setPaymentFrequency);
        binder.forField(firstPaymentPercent)
                .asRequired("First payment percent is required. | ត្រូវបញ្ចូលភាគរយលើកទីមួយ។")
                .withValidator(value -> value != null
                                && value.signum() >= 0
                                && value.compareTo(new BigDecimal("100")) < 0,
                        "Percent must be from 0 to less than 100. | ភាគរយត្រូវចាប់ពី 0 ដល់តិចជាង 100។")
                .bind(PayrollPaymentSetting::getFirstPaymentPercent,
                        PayrollPaymentSetting::setFirstPaymentPercent);
        binder.forField(effectiveFrom)
                .asRequired("Effective From is required. | ត្រូវបញ្ចូលថ្ងៃចាប់ផ្ដើម។")
                .bind(PayrollPaymentSetting::getEffectiveFrom,
                        PayrollPaymentSetting::setEffectiveFrom);
        binder.bind(effectiveTo,
                PayrollPaymentSetting::getEffectiveTo,
                PayrollPaymentSetting::setEffectiveTo);
        binder.bind(active,
                PayrollPaymentSetting::isActive,
                PayrollPaymentSetting::setActive);
    }

    @Override
    protected PayrollPaymentSetting createNewEntity() {
        PayrollPaymentSetting setting = new PayrollPaymentSetting();
        setting.setPaymentFrequency("MONTHLY");
        setting.setFirstPaymentPercent(BigDecimal.ZERO);
        setting.setEffectiveFrom(LocalDate.of(LocalDate.now().getYear(), 1, 1));
        setting.setActive(true);
        return setting;
    }

    @Override
    protected void populateForm(PayrollPaymentSetting value) {
        if (value != null && value.getId() != null) {
            this.entity = service.findById(value.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payment setting no longer exists. Refresh the page. "
                                    + "| ការកំណត់នេះលែងមានទៀតហើយ។"));
        } else {
            this.entity = value == null ? createNewEntity() : value;
        }

        binder.readBean(this.entity);
        updateRuleFields();
        editorLayout.open();
    }

    @Override
    protected void beforeSave(PayrollPaymentSetting value, boolean isNew) {
        // Employee overrides are legacy-only. New maintenance is one company rule
        // with an optional Shift exception when frequency is MONTHLY.
        value.setEmployee(null);
        if ("SEMI_MONTHLY".equals(value.getPaymentFrequency())) {
            value.setShift(null);
        }
        if (value.getEffectiveTo() != null
                && value.getEffectiveTo().isBefore(value.getEffectiveFrom())) {
            throw new IllegalArgumentException(
                    "Effective To cannot be before Effective From. "
                            + "| ថ្ងៃបញ្ចប់មិនអាចមុនថ្ងៃចាប់ផ្ដើមបានទេ។");
        }
    }

    @Override
    protected void focusFirstField() {
        shift.focus();
    }

    @Override
    protected List<ColumnDef<PayrollPaymentSetting>> getColumnDefs() {
        return List.of(
                col("shift", "Semi-Monthly Shift Exception | វេនបើកពីរដង",
                        PayrollPaymentSetting::getShift,
                        value -> {
                            if ("SEMI_MONTHLY".equals(value.getPaymentFrequency())) {
                                return "Disabled · All Staff Semi-Monthly | បុគ្គលិកទាំងអស់បើកពីរដង";
                            }
                            if (value.getShift() == null) {
                                return "Blank · All Staff Monthly | បុគ្គលិកទាំងអស់បើកប្រចាំខែ";
                            }
                            return value.getShift().getShiftName() + " → SEMI-MONTHLY";
                        }),
                col("paymentFrequency", "Frequency | ចំនួនដង",
                        PayrollPaymentSetting::getPaymentFrequency,
                        value -> frequencyLabel(value.getPaymentFrequency())),
                col("firstPaymentPercent", "First Payment (%) | ភាគរយលើកទីមួយ",
                        PayrollPaymentSetting::getFirstPaymentPercent,
                        value -> decimal(value.getFirstPaymentPercent())),
                col("effectiveFrom", "Effective From | ចាប់ផ្ដើម",
                        PayrollPaymentSetting::getEffectiveFrom,
                        value -> text(value.getEffectiveFrom())),
                col("effectiveTo", "Effective To | បញ្ចប់",
                        PayrollPaymentSetting::getEffectiveTo,
                        value -> value.getEffectiveTo() == null ? "Open-ended" : value.getEffectiveTo().toString()),
                col("active", "Active | សកម្ម",
                        PayrollPaymentSetting::isActive,
                        value -> value.isActive() ? "Active | សកម្ម" : "Inactive | អសកម្ម"));
    }

    @Override
    protected Specification<PayrollPaymentSetting> buildCombinedSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> tokens = new ArrayList<>();
            String search = quickSearchField == null ? "" : quickSearchField.getValue();
            if (search != null && !search.isBlank()) {
                String value = search.trim();
                String like = "%" + value.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        buildLikePredicate(criteriaBuilder, root.get("paymentFrequency"), like),
                        buildLikePredicate(criteriaBuilder,
                                root.join("shift", JoinType.LEFT).get("shiftName"), like)));
                tokens.add("Quick Search: " + value);
            }
            // Historical employee overrides are intentionally hidden.
            predicates.add(criteriaBuilder.isNull(root.get("employee")));
            showSqlFilterTokens(tokens);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Override
    protected String getSortProperty(String columnKey) {
        if ("shift".equals(columnKey)) {
            return "shift.shiftName";
        }
        return super.getSortProperty(columnKey);
    }

    @Override
    protected Sort getDefaultSort() {
        return Sort.by(
                Sort.Order.desc("effectiveFrom"),
                Sort.Order.desc("id"));
    }

    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of("version");
    }

    @Override
    protected String getEntityLabelSingular() {
        return "payment setting";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "payment settings";
    }

    private void updateRuleFields() {
        String frequency = paymentFrequency.getValue();
        boolean companySemiMonthly = "SEMI_MONTHLY".equals(frequency);

        if (companySemiMonthly) {
            shift.setEnabled(false);
            if (shift.getValue() != null) {
                shift.clear();
            }
            firstPaymentPercent.setEnabled(true);
            firstPaymentPercent.setRequiredIndicatorVisible(true);
            if (firstPaymentPercent.getValue() == null
                    || firstPaymentPercent.getValue().signum() == 0) {
                firstPaymentPercent.setValue(new BigDecimal("50.00"));
            }
            return;
        }

        shift.setEnabled(true);
        boolean shiftException = "MONTHLY".equals(frequency) && shift.getValue() != null;
        firstPaymentPercent.setEnabled(shiftException);
        firstPaymentPercent.setRequiredIndicatorVisible(shiftException);

        if (shiftException) {
            if (firstPaymentPercent.getValue() == null
                    || firstPaymentPercent.getValue().signum() == 0) {
                firstPaymentPercent.setValue(new BigDecimal("50.00"));
            }
        } else {
            firstPaymentPercent.setValue(BigDecimal.ZERO);
        }
    }

    private static String frequencyLabel(String value) {
        return switch (value == null ? "" : value) {
            case "SEMI_MONTHLY" -> "SEMI-MONTHLY | ពីរដងក្នុងមួយខែ";
            case "MONTHLY" -> "MONTHLY | ម្ដងក្នុងមួយខែ";
            default -> value == null ? "" : value;
        };
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
