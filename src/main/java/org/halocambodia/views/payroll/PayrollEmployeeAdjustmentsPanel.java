package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanInput;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanRow;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanRepaymentRow;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.RecurringInput;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.RecurringRow;
import static org.halocambodia.views.payroll.PayrollViewSupport.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.halocambodia.data.Employee;
import org.halocambodia.services.PayrollEmployeeAdjustmentService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.ProgressDialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

/** Payroll-wide setup for recurring employee items plus loans and employee recoveries. */
final class PayrollEmployeeAdjustmentsPanel extends VerticalLayout {

    private final PayrollEmployeeAdjustmentService service;

    private final Grid<RecurringRow> recurringGrid = new Grid<>(RecurringRow.class, false);
    private final Grid<LoanRow> loanGrid = new Grid<>(LoanRow.class, false);
    private final TextField search = new TextField();

    PayrollEmployeeAdjustmentsPanel(PayrollEmployeeAdjustmentService service) {
        this.service = service;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("min-height", "0").set("overflow", "hidden");

        configureGrids();
        add(buildScroller());
        setFlexGrow(1, getComponentAt(0));
    }

    void refresh() {
        String q = search.getValue();
        recurringGrid.setItems(service.findRecurring(q));
        loanGrid.setItems(service.findLoans(q));
    }

    private Scroller buildScroller() {
        VerticalLayout page = page(
                "Recurring Earnings, Deductions, Loans & Recoveries | ចំណូល ការកាត់ ប្រាក់កម្ចី និងការសងសំណង",
                "Use OTHER_DEDUCTION + ONE_TIME for a simple one-time charge. Use Loans & Recoveries for company loans, asset damage/loss, or other amounts recovered across payroll months. "
                        + "Balances are reduced only when Final Settlement is PAID. "
                        + "| ប្រើការកាត់ផ្សេងៗម្តងសម្រាប់ការកាត់ម្តង។ ប្រើប្រាក់កម្ចី/ការសងសំណងសម្រាប់ការសងជាច្រើនខែ។");
        page.setHeight(null);
        page.setMinHeight("100%");

        search.setPlaceholder("Search employee / insurance / loan or recovery reference");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidth("min(420px, 100%)");
        search.addValueChangeListener(event -> refresh());

        Button newRecurring = primaryButton("New Earning / Deduction", VaadinIcon.PLUS);
        newRecurring.addClickListener(event -> openRecurringEditor(null));

        Button newLoan = primaryButton("New Loan / Recovery", VaadinIcon.PLUS);
        newLoan.addClickListener(event -> openLoanEditor(null));

        Button refresh = actionButton("Refresh", VaadinIcon.REFRESH);
        refresh.addClickListener(event -> ProgressDialog.runUiTask(
                "Loading Employee Payroll Setup | កំពុងផ្ទុកការកំណត់បុគ្គលិក",
                "Loading recurring items, loans and recoveries... | កំពុងផ្ទុកចំណូល ការកាត់ ប្រាក់កម្ចី និងការសងសំណង...",
                this::refresh,
                () -> { },
                ex -> error(databaseMessage(ex))));

        HorizontalLayout tools = toolbar(search, newRecurring, newLoan, refresh);

        VerticalLayout recurringSection = new VerticalLayout(recurringGrid);
        recurringSection.setPadding(false);
        recurringSection.setSpacing(false);
        recurringSection.setWidthFull();
        recurringGrid.setHeight("360px");

        Details recurringDetails = new Details(
                "Recurring Earnings & Deductions | ចំណូល និងការកាត់ប្រចាំ",
                recurringSection);
        recurringDetails.setOpened(true);
        recurringDetails.setWidthFull();

        VerticalLayout loanSection = new VerticalLayout(loanGrid);
        loanSection.setPadding(false);
        loanSection.setSpacing(false);
        loanSection.setWidthFull();
        loanGrid.setHeight("360px");

        Details loanDetails = new Details(
                "Loans & Employee Recoveries | ប្រាក់កម្ចី និងការសងសំណងបុគ្គលិក",
                loanSection);
        loanDetails.setOpened(true);
        loanDetails.setWidthFull();

        page.add(tools, recurringDetails, loanDetails);

        Scroller scroller = new Scroller(page);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.getStyle().set("min-height", "0");
        return scroller;
    }

    private void configureGrids() {
        configureGrid(recurringGrid);
        recurringGrid.addColumn(RecurringRow::insuranceNo)
                .setHeader("Insurance No").setAutoWidth(true).setSortable(true);
        recurringGrid.addColumn(RecurringRow::employeeNameEn)
                .setHeader("Employee Name").setAutoWidth(true).setFlexGrow(1).setSortable(true);
        recurringGrid.addColumn(row -> componentLabel(row.componentCode()))
                .setHeader("Component").setAutoWidth(true).setSortable(true);
        recurringGrid.addColumn(row -> usd(row.amount()))
                .setHeader("Amount").setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END).setSortable(true);
        recurringGrid.addColumn(row -> recurrenceLabel(row.recurrenceType()))
                .setHeader("Recurrence").setAutoWidth(true).setSortable(true);
        recurringGrid.addColumn(RecurringRow::effectiveFrom)
                .setHeader("Effective From").setAutoWidth(true).setSortable(true);
        recurringGrid.addColumn(row -> row.effectiveTo() == null ? "Open" : row.effectiveTo().toString())
                .setHeader("Effective To").setAutoWidth(true).setSortable(true);
        recurringGrid.addComponentColumn(row -> statusBadge(row.active() ? "ACTIVE" : "INACTIVE"))
                .setHeader("Status").setAutoWidth(true);
        recurringGrid.addColumn(row -> row.settledApplicationCount())
                .setHeader("Paid Payrolls").setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        recurringGrid.addComponentColumn(this::recurringActions)
                .setHeader("Actions").setAutoWidth(true).setFrozenToEnd(true);

        configureGrid(loanGrid);
        loanGrid.addColumn(LoanRow::insuranceNo)
                .setHeader("Insurance No").setAutoWidth(true).setSortable(true);
        loanGrid.addColumn(LoanRow::employeeNameEn)
                .setHeader("Employee Name").setAutoWidth(true).setFlexGrow(1).setSortable(true);
        loanGrid.addColumn(row -> recoveryTypeLabel(row.recoveryType()))
                .setHeader("Type").setAutoWidth(true).setSortable(true);
        loanGrid.addColumn(LoanRow::loanReference)
                .setHeader("Reference").setAutoWidth(true).setSortable(true);
        loanGrid.addColumn(row -> usd(row.principalAmount()))
                .setHeader("Total Amount").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        loanGrid.addColumn(row -> usd(row.monthlyInstallment()))
                .setHeader("Monthly Deduction").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        loanGrid.addColumn(row -> usd(row.repaidAmount()))
                .setHeader("Recovered").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        loanGrid.addColumn(row -> usd(row.outstandingBalance()))
                .setHeader("Outstanding").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        loanGrid.addColumn(LoanRow::startDate)
                .setHeader("Start Date").setAutoWidth(true).setSortable(true);
        loanGrid.addComponentColumn(row -> statusBadge(loanStatusLabel(row.status())))
                .setHeader("Status").setAutoWidth(true);
        loanGrid.addColumn(LoanRow::settledRepaymentCount)
                .setHeader("Paid Months").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        loanGrid.addComponentColumn(this::loanActions)
                .setHeader("Actions").setAutoWidth(true).setFrozenToEnd(true);
    }

    private HorizontalLayout recurringActions(RecurringRow row) {
        Button edit = new Button(VaadinIcon.EDIT.create(), event -> openRecurringEditor(row));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        edit.setTooltipText("Edit");

        Button remove = new Button(VaadinIcon.TRASH.create());
        remove.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY_INLINE,
                ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_ERROR);
        remove.setTooltipText(row.settledApplicationCount() > 0 ? "Deactivate" : "Remove");
        remove.addClickListener(event -> {
            PayrollConfirmDialog confirm = confirm(
                    "Remove Recurring Item | លុបធាតុប្រចាំ",
                    row.settledApplicationCount() > 0
                            ? "This item has paid payroll history, so it will be deactivated instead of deleted. Continue?"
                            : "Remove this recurring payroll item?");
            confirm.addConfirmListener(e -> {
                try {
                    service.removeRecurring(row.id());
                    refresh();
                    success("Recurring payroll item updated. | បានធ្វើបច្ចុប្បន្នភាពធាតុប្រចាំ។");
                } catch (RuntimeException ex) {
                    error(databaseMessage(ex));
                }
            });
            confirm.open();
        });

        return toolbar(edit, remove);
    }

    private HorizontalLayout loanActions(LoanRow row) {
        Button edit = new Button(VaadinIcon.EDIT.create(), event -> openLoanEditor(row));
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        edit.setTooltipText("Edit Loan / Recovery");

        Button history = new Button(VaadinIcon.CLOCK.create(), event -> openLoanHistory(row));
        history.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        history.setTooltipText("Repayment / Recovery History");

        return toolbar(edit, history);
    }

    private void openLoanHistory(LoanRow loan) {
        CustomDialog dialog = editorDialog(
                "Repayment / Recovery History | ប្រវត្តិសងប្រាក់ / សំណង · " + loan.loanReference());
        dialog.setWidth("min(1050px, calc(100vw - 32px))");

        Grid<LoanRepaymentRow> grid = new Grid<>(LoanRepaymentRow.class, false);
        configureGrid(grid);
        grid.setAllRowsVisible(true);
        grid.addColumn(row -> "%04d-%02d".formatted(row.payrollYear(), row.payrollMonth()))
                .setHeader("Payroll Period").setAutoWidth(true);
        grid.addColumn(LoanRepaymentRow::runNumber)
                .setHeader("Run").setAutoWidth(true);
        grid.addColumn(row -> usd(row.scheduledAmount()))
                .setHeader("Scheduled").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        grid.addColumn(row -> usd(row.deductedAmount()))
                .setHeader("Deducted").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        grid.addColumn(row -> usd(row.balanceBefore()))
                .setHeader("Balance Before").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        grid.addColumn(row -> usd(row.balanceAfter()))
                .setHeader("Balance After").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        grid.addComponentColumn(row -> statusBadge(row.status()))
                .setHeader("Status").setAutoWidth(true);
        grid.addColumn(row -> row.settledAt() == null ? "-" : row.settledAt().toLocalDate().toString())
                .setHeader("Settled At").setAutoWidth(true);
        grid.setItems(service.findLoanRepayments(loan.id()));

        VerticalLayout content = new VerticalLayout(
                readOnly("Employee", loan.insuranceNo() + " · " + nvl(loan.employeeNameEn())),
                readOnly("Type", recoveryTypeLabel(loan.recoveryType())),
                readOnly("Total Amount", usd(loan.principalAmount())),
                readOnly("Outstanding", usd(loan.outstandingBalance())),
                grid);
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(true);
        dialog.add(content);

        Button close = actionButton("Close | បិទ", VaadinIcon.CLOSE_SMALL);
        close.addClickListener(event -> dialog.close());
        dialog.getFooter().add(close);
        dialog.open();
    }

    private void openRecurringEditor(RecurringRow row) {
        CustomDialog dialog = editorDialog(
                (row == null ? "New" : "Edit")
                        + " Recurring Earning / Deduction | ចំណូល / ការកាត់ប្រចាំ");

        ComboBox<Employee> employee = employeeCombo();
        ComboBox<String> component = new ComboBox<>("Component | សមាសភាគ");
        component.setItems(
                PayrollEmployeeAdjustmentService.FIXED_INCOME,
                PayrollEmployeeAdjustmentService.OTHER_DEDUCTION);
        component.setItemLabelGenerator(PayrollEmployeeAdjustmentsPanel::componentLabel);
        component.setWidthFull();

        BigDecimalField amount = new BigDecimalField("Amount (USD) | ចំនួន");
        amount.setWidthFull();
       // amount.setStep(BigDecimal.valueOf(0.01));

        ComboBox<String> recurrence = new ComboBox<>("Recurrence | ការកើតឡើងវិញ");
        recurrence.setItems("MONTHLY", "ONE_TIME");
        recurrence.setItemLabelGenerator(PayrollEmployeeAdjustmentsPanel::recurrenceLabel);
        recurrence.setWidthFull();

        DatePicker effectiveFrom = new DatePicker("Effective From | ចាប់ពី");
        DatePicker effectiveTo = new DatePicker("Effective To | ដល់");
        effectiveFrom.setWidthFull();
        effectiveTo.setWidthFull();

        TextField description = new TextField("Description | បរិយាយ");
        description.setWidthFull();

        TextArea remarks = new TextArea("Remarks | កំណត់សម្គាល់");
        remarks.setWidthFull();
        remarks.setMaxLength(2000);

        Checkbox active = new Checkbox("Active | សកម្ម", true);

        if (row == null) {
            component.setValue(PayrollEmployeeAdjustmentService.FIXED_INCOME);
            recurrence.setValue("MONTHLY");
            effectiveFrom.setValue(LocalDate.now());
        } else {
            findEmployee(row.employeeId(), employee.getListDataView().getItems().toList())
                    .ifPresent(employee::setValue);
            component.setValue(row.componentCode());
            amount.setValue(row.amount());
            recurrence.setValue(row.recurrenceType());
            effectiveFrom.setValue(row.effectiveFrom());
            effectiveTo.setValue(row.effectiveTo());
            description.setValue(nvl(row.description()));
            remarks.setValue(nvl(row.remarks()));
            active.setValue(row.active());
        }

        VerticalLayout content = new VerticalLayout(
                form(employee, component, amount, recurrence, effectiveFrom, effectiveTo),
                description, remarks, active);
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(true);
        dialog.add(content);

        Button cancel = actionButton("Cancel | បោះបង់", VaadinIcon.CLOSE_SMALL);
        cancel.addClickListener(event -> dialog.close());
        Button save = primaryButton("Save | រក្សាទុក", VaadinIcon.CHECK);
        save.addClickListener(event -> {
            try {
                Employee selectedEmployee = employee.getValue();
                Long id = service.saveRecurring(new RecurringInput(
                        row == null ? null : row.id(),
                        selectedEmployee == null ? null : selectedEmployee.getId(),
                        component.getValue(), amount.getValue(), recurrence.getValue(),
                        effectiveFrom.getValue(), effectiveTo.getValue(), active.getValue(),
                        description.getValue(), remarks.getValue()));
                dialog.close();
                refresh();
                success("Recurring payroll item saved (#" + id + "). | បានរក្សាទុកធាតុប្រចាំ។");
            } catch (RuntimeException ex) {
                error(databaseMessage(ex));
            }
        });
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openLoanEditor(LoanRow row) {
        CustomDialog dialog = editorDialog(
                (row == null ? "New" : "Edit") + " Employee Loan / Recovery | ប្រាក់កម្ចី / ការសងសំណងបុគ្គលិក");

        ComboBox<Employee> employee = employeeCombo();

        ComboBox<String> recoveryType = new ComboBox<>("Type | ប្រភេទ");
        recoveryType.setItems(
                PayrollEmployeeAdjustmentService.RECOVERY_COMPANY_LOAN,
                PayrollEmployeeAdjustmentService.RECOVERY_ASSET_DAMAGE,
                PayrollEmployeeAdjustmentService.RECOVERY_ASSET_LOSS,
                PayrollEmployeeAdjustmentService.RECOVERY_OTHER);
        recoveryType.setItemLabelGenerator(PayrollEmployeeAdjustmentsPanel::recoveryTypeLabel);
        recoveryType.setWidthFull();

        TextField reference = new TextField("Reference | លេខយោង");
        reference.setWidthFull();

        BigDecimalField principal = new BigDecimalField("Total Amount (USD) | ចំនួនសរុប");
        BigDecimalField monthly = new BigDecimalField("Monthly Deduction (USD) | ការកាត់ប្រចាំខែ");
        principal.setWidthFull();
        monthly.setWidthFull();
       // principal.setStep(BigDecimal.valueOf(0.01));
      //  monthly.setStep(BigDecimal.valueOf(0.01));

        DatePicker startDate = new DatePicker("Start Date | ថ្ងៃចាប់ផ្តើម");
        DatePicker endDate = new DatePicker("End Date | ថ្ងៃបញ្ចប់");
        startDate.setWidthFull();
        endDate.setWidthFull();

        ComboBox<String> status = new ComboBox<>("Status | ស្ថានភាព");
        status.setItems("ACTIVE", "PAUSED", "CANCELLED", "SETTLED");
        status.setItemLabelGenerator(PayrollEmployeeAdjustmentsPanel::loanStatusLabel);
        status.setWidthFull();

        Span balance = new Span();
        balance.getStyle().set("font-weight", "600");

        TextArea remarks = new TextArea("Remarks | កំណត់សម្គាល់");
        remarks.setWidthFull();
        remarks.setMaxLength(2000);

        Span guidance = new Span(
                "Tip: for a one-time phone/laptop charge, use OTHER_DEDUCTION + ONE_TIME. "
                        + "Use ASSET_DAMAGE / ASSET_LOSS here when the employee repays across multiple payroll months. "
                        + "| សម្រាប់ការកាត់ម្តង ប្រើ OTHER_DEDUCTION + ONE_TIME។");
        guidance.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        if (row == null) {
            recoveryType.setValue(PayrollEmployeeAdjustmentService.RECOVERY_COMPANY_LOAN);
            status.setValue("ACTIVE");
            startDate.setValue(LocalDate.now());
            balance.setText("Outstanding: new total amount");
        } else {
            findEmployee(row.employeeId(), employee.getListDataView().getItems().toList())
                    .ifPresent(employee::setValue);
            recoveryType.setValue(row.recoveryType());
            reference.setValue(nvl(row.loanReference()));
            principal.setValue(row.principalAmount());
            monthly.setValue(row.monthlyInstallment());
            startDate.setValue(row.startDate());
            endDate.setValue(row.endDate());
            status.setValue(row.status());
            remarks.setValue(nvl(row.remarks()));
            balance.setText("Outstanding: " + usd(row.outstandingBalance())
                    + " · Repaid: " + usd(row.repaidAmount()));
            if ("SETTLED".equals(row.status())) {
                status.setEnabled(false);
            }
        }

        VerticalLayout content = new VerticalLayout(
                form(employee, recoveryType, reference, principal, monthly, startDate, endDate, status),
                guidance, balance, remarks);
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(true);
        dialog.add(content);

        Button cancel = actionButton("Cancel | បោះបង់", VaadinIcon.CLOSE_SMALL);
        cancel.addClickListener(event -> dialog.close());
        Button save = primaryButton("Save | រក្សាទុក", VaadinIcon.CHECK);
        save.setEnabled(row == null || !"SETTLED".equals(row.status()));
        save.addClickListener(event -> {
            try {
                Employee selectedEmployee = employee.getValue();
                Long id = service.saveLoan(new LoanInput(
                        row == null ? null : row.id(),
                        selectedEmployee == null ? null : selectedEmployee.getId(),
                        recoveryType.getValue(), reference.getValue(), principal.getValue(), monthly.getValue(),
                        startDate.getValue(), endDate.getValue(), status.getValue(), remarks.getValue()));
                dialog.close();
                refresh();
                success("Loan / recovery saved (#" + id + "). | បានរក្សាទុកប្រាក់កម្ចី / ការសងសំណង។");
            } catch (RuntimeException ex) {
                error(databaseMessage(ex));
            }
        });
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private ComboBox<Employee> employeeCombo() {
        ComboBox<Employee> combo = new ComboBox<>("Employee | បុគ្គលិក");
        combo.setItems(service.findActiveEmployees());
        combo.setItemLabelGenerator(employee -> employee.getInsuranceNo()
                + " · " + nvl(employee.getNameEn())
                + (employee.getNameKh() == null ? "" : " · " + employee.getNameKh()));
        combo.setWidthFull();
        combo.setClearButtonVisible(true);
        return combo;
    }

    private static java.util.Optional<Employee> findEmployee(Long id, List<Employee> employees) {
        if (id == null) return java.util.Optional.empty();
        return employees.stream().filter(employee -> id.equals(employee.getId())).findFirst();
    }

    private static String componentLabel(String code) {
        return switch (nvl(code)) {
            case "FIXED_INCOME" -> "Fixed Income | ចំណូលថេរ";
            case "OTHER_DEDUCTION" -> "Other Deduction | ការកាត់ផ្សេងៗ";
            default -> nvl(code);
        };
    }


    private static String recoveryTypeLabel(String type) {
        return switch (nvl(type)) {
            case "COMPANY_LOAN" -> "Company Loan | ប្រាក់កម្ចីក្រុមហ៊ុន";
            case "ASSET_DAMAGE" -> "Asset Damage | ខូចទ្រព្យសម្បត្តិ";
            case "ASSET_LOSS" -> "Asset Loss | បាត់បង់ទ្រព្យសម្បត្តិ";
            case "OTHER_RECOVERY" -> "Other Recovery | ការសងសំណងផ្សេងៗ";
            default -> nvl(type).replace('_', ' ');
        };
    }


    /**
     * SETTLED is kept as the database/audit state. In the employee setup UI a
     * fully recovered loan is operationally inactive, so display it as
     * INACTIVE without changing the historical database status contract.
     */
    private static String loanStatusLabel(String status) {
        return switch (nvl(status)) {
            case "SETTLED" -> "INACTIVE";
            default -> nvl(status);
        };
    }

    private static String recurrenceLabel(String recurrence) {
        return switch (nvl(recurrence)) {
            case "MONTHLY" -> "Monthly | ប្រចាំខែ";
            case "ONE_TIME" -> "One Time | ម្តង";
            default -> nvl(recurrence);
        };
    }
}
