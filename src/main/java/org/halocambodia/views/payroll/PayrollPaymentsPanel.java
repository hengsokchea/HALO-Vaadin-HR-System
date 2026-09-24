package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.*;
import static org.halocambodia.views.payroll.PayrollViewSupport.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.enums.PayrollInstallmentType;
import org.halocambodia.enums.PayrollPaymentBatchStatus;
import org.halocambodia.enums.PayrollPeriodStatus;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.PayrollActionPermissions;
import org.halocambodia.services.PayrollPaymentService;
import org.halocambodia.services.PayrollPayslipService;
import org.halocambodia.services.PayrollPayslipService.PayslipReport;
import org.halocambodia.services.PayrollService;

import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.PreviewReport;
import org.halocambodia.views.ProgressDialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.StreamRegistration;

final class PayrollPaymentsPanel extends VerticalLayout {

    private final PayrollService service;
    private final PayrollPaymentService paymentService;
    private final PayrollPayslipService payslipService;
    private final AuthenticatedUser authenticatedUser;
    private final PayrollRefreshCoordinator refreshCoordinator;

    private final ComboBox<PeriodRow> paymentPeriodFilter =new ComboBox<>("Payroll Period");
    private final Grid<PaymentBatchRow> paymentBatchGrid = new Grid<>();
    private final Grid<EmployeePaymentRow> paymentDetailGrid = new Grid<>();
    private final TextField paymentEmployeeSearch = new TextField();
    private final Button generateFirstPaymentButton = actionButton("First Payment", VaadinIcon.PLUS);
    private final Button generateFinalSettlementButton =actionButton("Final Settlement", VaadinIcon.CALC);
    private final Button generateAdjustmentSettlementButton =  actionButton("Adjustment Settlement", VaadinIcon.REFRESH);
    private final Button approvePaymentBatchButton = actionButton("Approve Batch", VaadinIcon.CHECK);
    private final Button markPaymentBatchPaidButton = actionButton("Confirm Payment", VaadinIcon.MONEY);
    private final Button exportBankFileButton = actionButton("Export Bank File", VaadinIcon.DOWNLOAD);
    private final Button exportNssfFileButton = actionButton("Export NSSF Excel", VaadinIcon.DOWNLOAD);
    private final Button printPayslipsButton = actionButton("Print Payslips", VaadinIcon.PRINT);
    private final Button cancelPaymentBatchButton = actionButton("Cancel Batch", VaadinIcon.CLOSE);
    private final Button auditHistoryButton = actionButton("Audit History", VaadinIcon.TIME_BACKWARD);
    private final Span paymentSummary = new Span("Select a payroll period and payment batch.");
    private final Div paymentSchedulePanel = new Div();
    private PaymentScheduleSummary paymentSchedule;
    private StreamRegistration downloadRegistration;

    PayrollPaymentsPanel( PayrollService service, PayrollPaymentService paymentService, PayrollPayslipService payslipService, AuthenticatedUser authenticatedUser, PayrollRefreshCoordinator refreshCoordinator) {
        this.service = service;
        this.paymentService = paymentService;
        this.payslipService = payslipService;
        this.authenticatedUser = authenticatedUser;
        this.refreshCoordinator = refreshCoordinator;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("min-height", "0");
        getStyle().set("overflow", "hidden");

        configurePaymentGrids();

        Component content = buildPage();


        Scroller pageScroller = new Scroller(content);
        pageScroller.setSizeFull();
        pageScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        pageScroller.getStyle().set("min-height", "0");

        add(pageScroller);
        setFlexGrow(1, pageScroller);
    }

    PaymentScheduleSummary paymentSchedule() {
        return paymentSchedule;
    }

    private Component buildPage() {

        //VerticalLayout page = page("Payroll Payments | ការបើកប្រាក់បៀវត្ស","First installment pays a percentage of basic salary. Final settlement pays monthly net less the PAID first installment. Adjustments are carried forward and paid in the next payroll. Approved batches can export Finance/Bank Excel and NSSF details; PAID batches can generate immutable payslip PDFs. | លើកទីមួយបើកតាមភាគរយនៃប្រាក់ខែគោល។ ការទូទាត់ចុងក្រោយដកប្រាក់លើកទីមួយដែលបាន PAID។ ការកែតម្រូវត្រូវបន្តទៅ និងទូទាត់ក្នុងបើកប្រាក់ខែបន្ទាប់។ បាច់ដែលបានអនុម័តអាចនាំចេញ Finance/Bank Excel និង NSSF; បាច់ PAID អាចបង្កើត Payslip PDF");
    	VerticalLayout page = page(null,null);

        paymentPeriodFilter.setItemLabelGenerator(PayrollViewSupport::periodLabel);
        paymentPeriodFilter.setClearButtonVisible(true);


        paymentPeriodFilter.addValueChangeListener(event -> refreshPaymentBatches());

        Button refresh = actionButton("Refresh", VaadinIcon.REFRESH);

        refresh.addClickListener(event -> refreshPaymentBatchesWithProgress());

        generateFirstPaymentButton.addClickListener(event -> openGeneratePaymentDialog(false));

        generateFinalSettlementButton.addClickListener(event -> openGeneratePaymentDialog(true));

        generateAdjustmentSettlementButton.addClickListener(event -> openGenerateAdjustmentSettlementDialog());

        approvePaymentBatchButton.addClickListener(event -> confirmPaymentBatchStatus(PayrollPaymentBatchStatus.APPROVED.code()));

        markPaymentBatchPaidButton.addClickListener(event -> {
            PaymentBatchRow batch = selectedPaymentBatch().orElse(null);
            if (batch != null && PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.matches(batch.installmentType())) {
                finalizeAdjustmentDirectly(batch);
            } else {
                openPaymentExecutionDialog();
            }
        });

        exportBankFileButton.addClickListener(event -> downloadBankPaymentFile());

        exportNssfFileButton.addClickListener(event -> downloadNssfPaymentFile());

        printPayslipsButton.addClickListener(event -> printPayslips());

        cancelPaymentBatchButton.addClickListener(event -> confirmPaymentBatchStatus(PayrollPaymentBatchStatus.CANCELLED.code()));

        auditHistoryButton.addClickListener(event -> openPaymentAuditHistory());

        HorizontalLayout filters = new HorizontalLayout(paymentPeriodFilter, generateFirstPaymentButton, generateFinalSettlementButton, generateAdjustmentSettlementButton, refresh);

        filters.setAlignItems(Alignment.END);

        filters.setWrap(true);

        filters.setWidthFull();

        HorizontalLayout batchActions = toolbar(

                approvePaymentBatchButton,

                exportBankFileButton,

                exportNssfFileButton,

                markPaymentBatchPaidButton,

                cancelPaymentBatchButton,

                auditHistoryButton);

        paymentEmployeeSearch.setPlaceholder("Search insurance number, employee or bank account... ");

        paymentEmployeeSearch.setPrefixComponent(VaadinIcon.SEARCH.create());

        paymentEmployeeSearch.setClearButtonVisible(true);

 //       paymentEmployeeSearch.setWidth("min(430px, 100%)");

        paymentEmployeeSearch.setValueChangeMode(ValueChangeMode.LAZY);

        paymentEmployeeSearch.addValueChangeListener(event -> refreshPaymentDetails());

        paymentSummary.addClassName("payroll-processing-summary");

        paymentSchedulePanel.addClassName("payroll-attendance-control");

        paymentSchedulePanel.setWidthFull();

        renderPaymentSchedule();

        HorizontalLayout detailHeader = new HorizontalLayout(sectionTitle("Employee Payments"), paymentSummary, paymentEmployeeSearch, printPayslipsButton);

        detailHeader.setWidthFull();

        detailHeader.setAlignItems(Alignment.BASELINE);

       detailHeader.setJustifyContentMode(JustifyContentMode.START);

        detailHeader.setWrap(true);

        Details paymentScheduleDetails = new Details("Detected Payment Schedule", paymentSchedulePanel);
        paymentScheduleDetails.setOpened(true);
        paymentScheduleDetails.setWidthFull();

        VerticalLayout batchSection = new VerticalLayout(paymentBatchGrid, batchActions);
        batchSection.setPadding(false);
        batchSection.setSpacing(false);
        batchSection.setWidthFull();

        Details paymentBatchDetails = new Details("Payment Batches", batchSection);
        paymentBatchDetails.setOpened(true);
        paymentBatchDetails.setWidthFull();

        page.add(filters, paymentScheduleDetails, paymentBatchDetails, detailHeader, paymentDetailGrid);

        page.setWidthFull();
        page.setHeight(null);
        page.setMinHeight("100%");
        page.setFlexGrow(0, paymentDetailGrid);

        updatePaymentActions();

        return page;

    }


    private void configurePaymentGrids() {

        configureGrid(paymentBatchGrid);
        paymentBatchGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        paymentBatchGrid.setAllRowsVisible(true);

        paymentBatchGrid.addColumn(batch -> installmentTypeLabel(batch.installmentType())) .setHeader("Installment").setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addColumn(PaymentBatchRow::paymentDate).setHeader("Payment Date").setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addComponentColumn(batch -> statusBadge(displayBatchStatus(batch))).setHeader("Status").setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addColumn(batch -> paymentMethodLabel(batch.paymentMethod())).setHeader("Payment Method").setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addColumn(PaymentBatchRow::paymentReference).setHeader("Payment Reference").setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addColumn(PaymentBatchRow::bankReference).setHeader("Bank Reference").setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addColumn(PaymentBatchRow::employeeCount).setHeader("Employees").setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addColumn(batch -> currencyMoney(batch.paymentAmount(), batch.currency())).setHeader("Payment Amount").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        paymentBatchGrid.addColumn(batch -> currencyMoney(batch.carryForwardAmount(), batch.currency())).setHeader("Recovery / Carry Forward").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);

        paymentBatchGrid.addColumn(PaymentBatchRow::notes).setHeader("Notes").setAutoWidth(true).setFlexGrow(1).setResizable(true);

        paymentBatchGrid.addSelectionListener(event -> {
            refreshPaymentDetails();
            updatePaymentActions();
        });
        
        for (Grid.Column columnPaymentBatchGrid : paymentBatchGrid.getColumns()) {
        	columnPaymentBatchGrid.setAutoWidth(true);
        	columnPaymentBatchGrid.setResizable(true);
        	columnPaymentBatchGrid.setSortable(true);
        }

        
        
        configureGrid(paymentDetailGrid);


        paymentDetailGrid.setMinHeight("360px");

        paymentDetailGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        paymentDetailGrid.addSelectionListener(event -> updatePaymentActions());
        paymentDetailGrid.addColumn(EmployeePaymentRow::insuranceNo).setHeader("Insurance") .setFrozen(true).setAutoWidth(true).setSortable(true).setResizable(true);
        paymentDetailGrid.addColumn(EmployeePaymentRow::nameEn).setHeader("Employee Name (English)").setFrozen(true).setAutoWidth(true).setSortable(true).setResizable(true);
        paymentDetailGrid.addColumn(EmployeePaymentRow::nameKh).setHeader("Employee Name (Khmer)").setAutoWidth(true).setResizable(true).setResizable(true);
        paymentDetailGrid.addColumn(EmployeePaymentRow::bankName).setHeader("Bank").setAutoWidth(true).setResizable(true).setResizable(true);
        paymentDetailGrid.addColumn(EmployeePaymentRow::bankAccount).setHeader("Bank Account").setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(row -> frequencyLabel(row.paymentFrequency())).setHeader("Frequency").setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(row -> decimal(row.firstPaymentPercent()) + "%").setHeader("First (%)").setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(row -> currencyMoney(row.basicSalary(), row.currency())).setHeader("Basic Salary").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(row -> currencyMoney(row.fullNetAmount(), row.currency())).setHeader("Net / Adjustment").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(row -> currencyMoney(row.previousPaidAmount(), row.currency())).setHeader("Previously Paid").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(row -> currencyMoney(row.paymentAmount(), row.currency())).setHeader("Pay Now").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(row -> currencyMoney(row.carryForwardAmount(), row.currency())).setHeader("Recovery / Carry Forward").setTextAlign(ColumnTextAlign.END).setAutoWidth(true).setResizable(true);
        paymentDetailGrid.addColumn(EmployeePaymentRow::remarks).setHeader("Remarks").setAutoWidth(true).setFlexGrow(1).setResizable(true);
        
        for (Grid.Column columnPaymentDetailGrid : paymentDetailGrid.getColumns()) {
        	columnPaymentDetailGrid.setAutoWidth(true);
        	columnPaymentDetailGrid.setResizable(true);
        	columnPaymentDetailGrid.setSortable(true);
        }

    }

    void refresh() {
        refreshPaymentPeriodFilter(null);
    }

    void refreshPaymentPeriodFilter(Long preferredPeriodId) {

        try {

            Long selectedId = Optional.ofNullable(paymentPeriodFilter.getValue())

                    .map(PeriodRow::id).orElse(null);

            Long targetId = preferredPeriodId == null ? selectedId : preferredPeriodId;

            List<PeriodRow> periods = service.findPeriods();

            paymentPeriodFilter.setItems(periods);

            periods.stream().filter(period -> period.id().equals(targetId)).findFirst()

                    .or(() -> periods.stream().findFirst())

                    .ifPresentOrElse(paymentPeriodFilter::setValue, paymentPeriodFilter::clear);

            if (periods.isEmpty()) {

                paymentSchedule = null;

                paymentBatchGrid.setItems(List.of());

                paymentDetailGrid.setItems(List.of());

                renderPaymentSchedule();

                updatePaymentActions();

            } else {

                refreshPaymentBatches();

            }

        } catch (Exception ex) {

            error(databaseMessage(ex));

        }

    }


    private void refreshPaymentBatches() {

        PeriodRow period = paymentPeriodFilter.getValue();

        if (period == null) {

            paymentSchedule = null;

            paymentBatchGrid.setItems(List.of());

            paymentDetailGrid.setItems(List.of());
            paymentDetailGrid.deselectAll();

            paymentSummary.setText("Select a payroll period.");

            renderPaymentSchedule();

            updatePaymentActions();

            return;

        }

        try {

            paymentSchedule = paymentService.detectPaymentSchedule(period.id());

            renderPaymentSchedule();

            Long selectedId = paymentBatchGrid.getSelectedItems().stream()

                    .findFirst().map(PaymentBatchRow::id).orElse(null);

            List<PaymentBatchRow> batches = paymentService.findBatches(period.id());

            paymentBatchGrid.setItems(batches);

            batches.stream().filter(batch -> batch.id().equals(selectedId)).findFirst()

                    .or(() -> batches.stream().reduce((first, second) -> second))

                    .ifPresentOrElse(paymentBatchGrid::select, paymentBatchGrid::deselectAll);

            refreshPaymentDetails();

            updatePaymentActions();

        } catch (Exception ex) {

            paymentSchedule = null;

            renderPaymentSchedule();

            paymentBatchGrid.setItems(List.of());

            paymentDetailGrid.setItems(List.of());

            updatePaymentActions();

            error(databaseMessage(ex));

        }

    }


    private void refreshPaymentBatchesWithProgress() {

        PeriodRow period = paymentPeriodFilter.getValue();

        if (period == null) {
            refreshPaymentBatches();
            return;
        }

        Long periodId = period.id();
        Long selectedId = paymentBatchGrid.getSelectedItems().stream()
                .findFirst()
                .map(PaymentBatchRow::id)
                .orElse(null);

        ProgressDialog.runAsync(
                "Refresh Payments | ធ្វើបច្ចុប្បន្នភាពការបើកប្រាក់",
                "Loading payment schedule and batches... | កំពុងទាញកាលវិភាគ និងកញ្ចប់បើកប្រាក់...",
                () -> new PaymentRefreshSnapshot(
                        paymentService.detectPaymentSchedule(periodId),
                        paymentService.findBatches(periodId)),
                snapshot -> {
                    paymentSchedule = snapshot.schedule();
                    renderPaymentSchedule();

                    List<PaymentBatchRow> batches = snapshot.batches();
                    paymentBatchGrid.setItems(batches);

                    batches.stream()
                            .filter(batch -> batch.id().equals(selectedId))
                            .findFirst()
                            .or(() -> batches.stream().reduce((first, second) -> second))
                            .ifPresentOrElse(
                                    paymentBatchGrid::select,
                                    () -> {
                                        paymentBatchGrid.deselectAll();
                                        refreshPaymentDetails();
                                        updatePaymentActions();
                                    });
                },
                ex -> error(databaseMessage(ex)));

    }


    private void renderPaymentSchedule() {

        paymentSchedulePanel.removeAll();

        if (paymentSchedule == null) {

            Paragraph message = new Paragraph("Select a payroll period to detect its effective payment setting. ");

            message.addClassName("payroll-attendance-message");

            paymentSchedulePanel.add(message);

            return;

        }

        String scheduleType = !paymentSchedule.companySettingFound()

                ? "NOT_CONFIGURED"

                : paymentSchedule.mixedSchedule()

                        ? "MIXED"

                        : paymentSchedule.hasSemiMonthlyEmployees()

                                ? "SEMI_MONTHLY" : "MONTHLY";

        HorizontalLayout heading = new HorizontalLayout(statusBadge(scheduleType));

        heading.setAlignItems(Alignment.CENTER);

        heading.setWrap(true);

        String companyDefault = paymentSchedule.companySettingFound()

                ? frequencyLabel(paymentSchedule.companyFrequency())

                : "NOT CONFIGURED | មិនបានកំណត់";

        if (paymentSchedule.companySettingFound() && ("SEMI_MONTHLY".equals(paymentSchedule.companyFrequency())  || paymentSchedule.shiftOverrideCount() > 0)) {
            companyDefault += " · First " + decimal(paymentSchedule.companyFirstPercent()) + "%";
        }

        Div metrics = new Div();
        metrics.addClassName("payroll-attendance-metrics");
        metrics.add(
              //  attendanceMetric("Reference Date",String.valueOf(paymentSchedule.referenceDate())),
                attendanceMetric("Company Frequency", companyDefault),
                attendanceMetric("Employees",Long.toString(paymentSchedule.rosteredEmployeeCount())),
                attendanceMetric("Monthly", Long.toString(paymentSchedule.monthlyEmployeeCount())),
                attendanceMetric("Semi-Monthly", Long.toString(paymentSchedule.semiMonthlyEmployeeCount()))
         );


        paymentSchedulePanel.add(heading, metrics);

    }


    private void refreshPaymentDetails() {

        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);

        if (batch == null) {

            paymentDetailGrid.setItems(List.of());

            paymentSummary.setText("Select a payment batch.");

            return;

        }

        try {

            List<EmployeePaymentRow> rows = paymentService.findEmployeePayments(

                    batch.id(), paymentEmployeeSearch.getValue());

            paymentDetailGrid.setItems(rows);

            BigDecimal visibleAmount = rows.stream()

                    .map(EmployeePaymentRow::paymentAmount)

                    .filter(java.util.Objects::nonNull)

                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            paymentSummary.setText(

                    installmentTypeLabel(batch.installmentType()) + " · "

                            + statusLabel(displayBatchStatus(batch)) + " · "

                            + rows.size() + " employee(s)·"

                            + currencyMoney(visibleAmount, batch.currency()));

        } catch (Exception ex) {

            error(databaseMessage(ex));

        }

    }


    private void updatePaymentActions() {

        PeriodRow period = paymentPeriodFilter.getValue();

        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);

        boolean periodOpen = period != null && !PayrollPeriodStatus.CLOSED.matches(period.status());

        boolean canGeneratePayment = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.PAYMENT_GENERATE, AccessPageType.UPDATED_PAGE);
        boolean canApprovePayment = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.PAYMENT_APPROVE, AccessPageType.UPDATED_PAGE);
        boolean canExport = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.BANK_EXPORT, AccessPageType.UPDATED_PAGE);
        boolean canReconcile = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.PAYMENT_RECONCILE, AccessPageType.UPDATED_PAGE);
        boolean canView = authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE);

        boolean hasSemiMonthlyEmployees = paymentSchedule != null

                && paymentSchedule.hasSemiMonthlyEmployees();

        boolean paymentSettingConfigured = paymentSchedule != null

                && paymentSchedule.companySettingFound();

        boolean regularPayrollPaid = period != null
                && paymentService.hasPaidRegularPayroll(period.id());

        generateFirstPaymentButton.setEnabled(

                canGeneratePayment && periodOpen && !regularPayrollPaid
                        && paymentSettingConfigured && hasSemiMonthlyEmployees);

        generateFirstPaymentButton.setTooltipText(period == null

                ? "Select a payroll period first | សូមជ្រើសរើសរយៈពេលជាមុន"

                : regularPayrollPaid

                        ? "Regular payroll is already PAID; first installment is permanently closed "
                                + "| ប្រាក់បៀវត្សប្រចាំខែបាន PAID រួច ហើយមិនអាចបង្កើតលើកទីមួយទៀតបានទេ"

                : paymentSchedule == null

                        ? "Payment schedule is not available | មិនមានកាលវិភាគបើកប្រាក់"

                        : !paymentSettingConfigured

                                ? "Configure an active company payment rule first "

                                        + "| សូមកំណត់ច្បាប់ការបើកប្រាក់របស់ក្រុមហ៊ុនជាមុន"

                        : hasSemiMonthlyEmployees

                                ? "Generate first payment for "

                                        + paymentSchedule.semiMonthlyEmployeeCount()

                                        + " semi-monthly employee(s) "

                                        + "| បង្កើតលើកទីមួយសម្រាប់បុគ្គលិកបើកពីរដង"

                                : "No semi-monthly employee exists for this period "

                                        + "| មិនមានបុគ្គលិកបើកពីរដងសម្រាប់រយៈពេលនេះទេ");

        generateFinalSettlementButton.setEnabled(

                canGeneratePayment && periodOpen && !regularPayrollPaid && paymentSettingConfigured);

        generateFinalSettlementButton.setTooltipText(period == null

                ? "Select a payroll period first | សូមជ្រើសរើសរយៈពេលជាមុន"

                : regularPayrollPaid

                        ? "Regular payroll is already PAID; use Adjustment Settlement for post-paid corrections "
                                + "| ប្រាក់បៀវត្សប្រចាំខែបាន PAID រួច សូមប្រើការទូទាត់កែតម្រូវ"

                : paymentSettingConfigured

                        ? "Generate the final payroll settlement | បង្កើតការទូទាត់ចុងក្រោយ"

                        : "Configure an active company payment setting first "

                                + "| សូមកំណត់ការបើកប្រាក់លំនាំដើមរបស់ក្រុមហ៊ុនជាមុន");

        boolean adjustmentSettlementReady = period != null
                && periodOpen
                && paymentService.canGenerateAdjustmentSettlement(period.id());

        generateAdjustmentSettlementButton.setEnabled(canGeneratePayment && adjustmentSettlementReady);
        generateAdjustmentSettlementButton.setTooltipText(period == null
                ? "Select a payroll period first | សូមជ្រើសរើសរយៈពេលជាមុន"
                : !periodOpen
                        ? "Create and approve an ADJUSTMENT run first; creating it reopens the paid period in a controlled way "
                                + "| សូមបង្កើត និងអនុម័តដំណើរការ ADJUSTMENT ជាមុន"
                        : adjustmentSettlementReady
                                ? "Generate settlement for the approved post-paid adjustment "
                                        + "| បង្កើតការទូទាត់សម្រាប់ការកែតម្រូវដែលបានអនុម័ត"
                                : "Requires a PAID regular payroll and an APPROVED adjustment run "
                                        + "| ត្រូវការប្រាក់បៀវត្សប្រចាំខែ PAID និងការកែតម្រូវ APPROVED");

        approvePaymentBatchButton.setEnabled(

                canApprovePayment && batch != null && PayrollPaymentBatchStatus.DRAFT.matches(batch.status()));

        boolean batchApproved = batch != null && PayrollPaymentBatchStatus.APPROVED.matches(batch.status());

        exportBankFileButton.setEnabled(canExport && batch != null);
        exportBankFileButton.setTooltipText(batch == null
                ? "Select a payment batch first | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ជាមុន"
                : PayrollPaymentBatchStatus.DRAFT.matches(batch.status())
                        ? "Export a DRAFT preview. Approval invalidates this preview; export again after approval for bank transfer confirmation. "
                                + "| ទាញយកឯកសារ Draft សម្រាប់ពិនិត្យ។ បន្ទាប់ពីអនុម័ត ត្រូវទាញយកម្ដងទៀតសម្រាប់បញ្ជាក់ការផ្ទេរធនាគារ។"
                        : "Export positive Pay Now rows as the current Excel XLSX bank file "
                                + "| ទាញយកចំនួនត្រូវបើកជាឯកសារ Excel XLSX បច្ចុប្បន្ន");

        boolean nssfExportBatch = batch != null
                && batch.runId() != null
                && PayrollInstallmentType.FINAL_SETTLEMENT.matches(batch.installmentType())
                && (PayrollPaymentBatchStatus.APPROVED.matches(batch.status())
                        || PayrollPaymentBatchStatus.PAID.matches(batch.status()));

        exportNssfFileButton.setEnabled(canExport && nssfExportBatch);
        exportNssfFileButton.setTooltipText(batch == null
                ? "Select a payment batch first | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ជាមុន"
                : !PayrollInstallmentType.FINAL_SETTLEMENT.matches(batch.installmentType())
                        ? "NSSF payment Excel is available only for Final Settlement "
                                + "| Excel ប.ស.ស. មានសម្រាប់ការទូទាត់ចុងក្រោយប៉ុណ្ណោះ"
                        : !(PayrollPaymentBatchStatus.APPROVED.matches(batch.status())
                                || PayrollPaymentBatchStatus.PAID.matches(batch.status()))
                                ? "Approve the Final Settlement batch before exporting NSSF payment "
                                        + "| សូមអនុម័តកញ្ចប់ទូទាត់ចុងក្រោយមុននាំចេញ ប.ស.ស."
                                : "Export monthly NSSF employee/employer contributions in KHR "
                                        + "| នាំចេញភាគទាន ប.ស.ស. របស់និយោជិត និងនិយោជកជា KHR");

        markPaymentBatchPaidButton.setEnabled(canReconcile && batchApproved);

        boolean adjustmentBatch = batch != null
                && PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.matches(batch.installmentType());
        markPaymentBatchPaidButton.setText(adjustmentBatch
                ? "Finalize Adjustment"
                : "Confirm Payment");

        cancelPaymentBatchButton.setEnabled(

                canApprovePayment && batch != null

                        && Set.of(PayrollPaymentBatchStatus.DRAFT.code(), PayrollPaymentBatchStatus.APPROVED.code()).contains(batch.status()));

        boolean firstInstallmentPayslip = batch != null
                && PayrollInstallmentType.FIRST_INSTALLMENT.matches(batch.installmentType());
        boolean settledPayslip = batch != null
                && batch.runId() != null
                && (PayrollInstallmentType.FINAL_SETTLEMENT.matches(batch.installmentType())
                        || PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.matches(batch.installmentType()));
        boolean payslipBatch = batch != null
                && PayrollPaymentBatchStatus.PAID.matches(batch.status())
                && (firstInstallmentPayslip || settledPayslip);
        Set<EmployeePaymentRow> selectedEmployees = paymentDetailGrid.getSelectedItems();
        int selectedCount = selectedEmployees.size();
        boolean validSelection = selectedEmployees.stream()
                .allMatch(employee -> employee.id() != null);

        printPayslipsButton.setEnabled(canView && payslipBatch
                && (selectedCount == 0 || validSelection));
        printPayslipsButton.setText(selectedCount == 0
                ? "Print Payslips"
                : "Print Selected Payslips (" + selectedCount + ")");
        printPayslipsButton.setTooltipText(!payslipBatch
                ? "Payslips are available after a first/final/adjustment batch is PAID | បង្កាន់ដៃមានបន្ទាប់ពីកញ្ចប់លើកទីមួយ/ចុងក្រោយ/កែតម្រូវបាន PAID"
                : selectedCount == 0
                        ? "No employee selected: print all employees in this batch | មិនបានជ្រើសបុគ្គលិក៖ បោះពុម្ពទាំងអស់"
                        : "Print only the selected employee(s) | បោះពុម្ពតែបុគ្គលិកដែលបានជ្រើស");

        auditHistoryButton.setEnabled(batch != null);
        auditHistoryButton.setTooltipText(batch == null
                ? "Select a payment batch first | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ជាមុន"
                : "View immutable payment audit history | មើលប្រវត្តិសវនកម្មការបើកប្រាក់ដែលមិនអាចកែប្រែបាន");

    }


    private void openPaymentAuditHistory() {
        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);
        if (batch == null) {
            error("Select a payment batch first. | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ជាមុនសិន។");
            return;
        }

        Long batchId = batch.id();
        String installmentType = batch.installmentType();

        ProgressDialog.runAsync(
                "Payment Audit History | ប្រវត្តិសកម្មភាពបើកប្រាក់",
                "Loading payment batch audit history... | កំពុងទាញប្រវត្តិសកម្មភាពកញ្ចប់បើកប្រាក់...",
                () -> paymentService.findPaymentAuditHistory(batchId),
                history -> new PayrollAuditHistoryDialog(
                        "Payment Batch Audit History | ប្រវត្តិសកម្មភាពកញ្ចប់បើកប្រាក់ · "
                                + installmentTypeLabel(installmentType),
                        history)
                        .open(),
                ex -> error(message(ex)));
    }

    private Optional<PaymentBatchRow> selectedPaymentBatch() {

        return paymentBatchGrid.getSelectedItems().stream().findFirst();

    }


    private void openGeneratePaymentDialog(boolean finalSettlement) {

        PeriodRow period = paymentPeriodFilter.getValue();

        if (period == null) {

            error("Select a payroll period first. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        String title = finalSettlement

                ? "Generate Final Settlement | បង្កើតការទូទាត់ចុងក្រោយ"

                : "Generate First Payment | បង្កើតការបើកលើកទីមួយ";

        CustomDialog dialog = editorDialog(title);

        DatePicker paymentDate = new DatePicker("Payment Date | ថ្ងៃបើកប្រាក់");

        paymentDate.setRequiredIndicatorVisible(true);

        paymentDate.setMin(period.startDate());

        paymentDate.setMax(period.endDate());

        LocalDate defaultDate;

        if (finalSettlement && period.paymentDate() != null) {

            defaultDate = period.paymentDate();

        } else {

            YearMonth payrollMonth = YearMonth.of(period.year(), period.month());

            defaultDate = payrollMonth.atDay(Math.min(15, payrollMonth.lengthOfMonth()));

        }

        if (defaultDate.isBefore(period.startDate())) {

            defaultDate = period.startDate();

        } else if (defaultDate.isAfter(period.endDate())) {

            defaultDate = period.endDate();

        }

        paymentDate.setValue(defaultDate);

        TextArea notes = new TextArea("Notes | កំណត់សម្គាល់");

        notes.setWidthFull();

        notes.setMinHeight("90px");

        Paragraph explanation = new Paragraph(finalSettlement

                ? "Uses the APPROVED regular payroll. Tax, allowance, bonus, OT, NSSF and deductions are already included in full monthly net; only a PAID first installment is subtracted. "

                        + "| ប្រើប្រាក់បៀវត្សប្រចាំខែដែលបានអនុម័ត ហើយដកតែលើកទីមួយដែលបានបើករួច។"

                : "Generates the first installment only for staff resolved as SEMI_MONTHLY by the company rule (all staff for SEMI_MONTHLY, or the selected Shift exception for MONTHLY). The amount is basic salary × configured percentage; tax and NSSF are not calculated here. "

                        + "| បង្កើតតែបុគ្គលិកដែលបើកពីរដង និងមិនគណនាពន្ធ ឬ ប.ស.ស. នៅលើកនេះទេ។");

        dialog.add(new VerticalLayout(explanation, paymentDate, notes));

        Button generate = primaryButton("Generate | បង្កើត", VaadinIcon.PLUS);

        Button cancel = new Button("Cancel | បោះបង់", event -> dialog.close());

        generate.addClickListener(event -> {

            Long periodId = period.id();
            LocalDate selectedPaymentDate = paymentDate.getValue();
            String paymentNotes = notes.getValue();

            ProgressDialog.runAsync(
                    finalSettlement
                            ? "Generate Final Settlement | បង្កើតការទូទាត់ចុងក្រោយ"
                            : "Generate First Payment | បង្កើតការបើកលើកទីមួយ",
                    finalSettlement
                            ? "Preparing final settlement from the approved payroll... | កំពុងរៀបចំការទូទាត់ចុងក្រោយពីប្រាក់បៀវត្សដែលបានអនុម័ត..."
                            : "Preparing first-installment employee payments... | កំពុងរៀបចំការបើកប្រាក់លើកទីមួយសម្រាប់បុគ្គលិក...",
                    () -> finalSettlement
                            ? paymentService.generateFinalSettlement(
                                    periodId, selectedPaymentDate, paymentNotes)
                            : paymentService.generateFirstInstallment(
                                    periodId, selectedPaymentDate, paymentNotes),
                    count -> {
                        dialog.close();
                        refreshPaymentBatches();
                        success(count + " employee payment(s) generated. "
                                + "| បានបង្កើតការបើកប្រាក់បុគ្គលិកចំនួន " + count + " នាក់។");
                    },
                    ex -> error(message(ex)));

        });

        dialog.getFooter().add(cancel, generate);

        dialog.open();

    }


    private void openGenerateAdjustmentSettlementDialog() {

        PeriodRow period = paymentPeriodFilter.getValue();
        if (period == null) {
            error("Select a payroll period first. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");
            return;
        }

        CustomDialog dialog = editorDialog(
                "Generate Adjustment Carry Forward | បង្កើតការកែតម្រូវទៅខែបន្ទាប់");

        DatePicker paymentDate = new DatePicker("Settlement Date | ថ្ងៃទូទាត់កែតម្រូវ");
        paymentDate.setRequiredIndicatorVisible(true);

        LocalDate originalPaymentDate = period.paymentDate() == null
                ? period.endDate()
                : period.paymentDate();
        paymentDate.setMin(originalPaymentDate);

        LocalDate today = LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        paymentDate.setValue(today.isBefore(originalPaymentDate)
                ? originalPaymentDate
                : today);

        TextArea notes = new TextArea("Notes | កំណត់សម្គាល់");
        notes.setWidthFull();
        notes.setMinHeight("90px");

        Paragraph explanation = new Paragraph(
                "Uses the APPROVED ADJUSTMENT run after the regular payroll is PAID. "
                        + "No adjustment is paid immediately. Positive balances are added to the next REGULAR payroll "
                        + "and negative balances are deducted from the next REGULAR payroll as recovery. "
                        + "| ប្រើដំណើរការ ADJUSTMENT ដែលបានអនុម័ត បន្ទាប់ពីប្រាក់បៀវត្សប្រចាំខែបាន PAID។ "
                        + "ការកែតម្រូវមិនត្រូវបានបើកភ្លាមៗទេ។ ចំនួនវិជ្ជមានត្រូវបន្ថែមទៅប្រាក់បៀវត្ស REGULAR ខែបន្ទាប់ "
                        + "ហើយចំនួនអវិជ្ជមានត្រូវកាត់សងពីប្រាក់បៀវត្ស REGULAR ខែបន្ទាប់។");

        dialog.add(new VerticalLayout(explanation, paymentDate, notes));

        Button generate = primaryButton("Generate | បង្កើត", VaadinIcon.PLUS);
        Button cancel = new Button("Cancel | បោះបង់", event -> dialog.close());

        generate.addClickListener(event -> {
            Long periodId = period.id();
            LocalDate selectedPaymentDate = paymentDate.getValue();
            String paymentNotes = notes.getValue();

            ProgressDialog.runAsync(
                    "Generate Adjustment Carry Forward | បង្កើតការកែតម្រូវទៅខែបន្ទាប់",
                    "Preparing positive earnings and negative recoveries for the next regular payroll... "
                            + "| កំពុងរៀបចំប្រាក់បន្ថែមវិជ្ជមាន និងការកាត់សងអវិជ្ជមានសម្រាប់ប្រាក់បៀវត្សខែបន្ទាប់...",
                    () -> paymentService.generateAdjustmentSettlement(
                            periodId, selectedPaymentDate, paymentNotes),
                    count -> {
                        dialog.close();
                        refreshPaymentBatches();
                        success(count + " adjustment carry-forward record(s) generated. "
                                + "| បានបង្កើតកំណត់ត្រាកែតម្រូវទៅខែបន្ទាប់ចំនួន " + count + "។");
                    },
                    ex -> error(message(ex)));
        });

        dialog.getFooter().add(cancel, generate);
        dialog.open();
    }

    private void downloadBankPaymentFile() {
        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);
        if (batch == null) {
            error("Select a payment batch first.");
            return;
        }

        Long batchId = batch.id();

        ProgressDialog.runAsync(
                "Export Bank File | នាំចេញឯកសារធនាគារ",
                "Preparing the bank payment Excel file... | កំពុងរៀបចំឯកសារ Excel សម្រាប់ធនាគារ...",
                () -> paymentService.exportBankPaymentFile(batchId),
                export -> {
                    triggerDownload(export.fileName(), export.content(),
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    refreshPaymentBatches();
                    success("Bank payment Excel file exported and recorded in audit history. "
                            + "| បានទាញយកឯកសារបើកប្រាក់ និងកត់ត្រាក្នុងប្រវត្តិសវនកម្ម។");
                },
                ex -> error(message(ex)));
    }

    private void downloadNssfPaymentFile() {
        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);
        if (batch == null) {
            error("Select a payment batch first. | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ជាមុន។");
            return;
        }

        Long batchId = batch.id();

        ProgressDialog.runAsync(
                "Export NSSF Payment | នាំចេញការទូទាត់ ប.ស.ស.",
                "Preparing the NSSF payment Excel file... | កំពុងរៀបចំឯកសារ Excel សម្រាប់ការទូទាត់ ប.ស.ស....",
                () -> paymentService.exportNssfPaymentFile(batchId),
                export -> {
                    triggerDownload(
                            export.fileName(),
                            export.content(),
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    success("NSSF payment Excel exported successfully. "
                            + "| បាននាំចេញ Excel ការទូទាត់ ប.ស.ស. ដោយជោគជ័យ។");
                },
                ex -> error(message(ex)));
    }

    private void finalizeAdjustmentDirectly(PaymentBatchRow batch) {
        if (batch == null) {
            error("Select an adjustment batch first. | សូមជ្រើសរើសកញ្ចប់កែតម្រូវជាមុនសិន។");
            return;
        }

        Long batchId = batch.id();
        ProgressDialog.runAsync(
                "Finalize Adjustment | បញ្ចប់ការកែតម្រូវ",
                "Finalizing positive and negative carry-forward records for the next regular payroll... "
                        + "| កំពុងបញ្ចប់កំណត់ត្រាវិជ្ជមាន និងអវិជ្ជមានសម្រាប់ប្រាក់បៀវត្សខែបន្ទាប់...",
                () -> paymentService.finalizeAdjustmentSettlement(batchId),
                () -> {
                    refreshPaymentBatches();
                    refreshCoordinator.refreshPeriods();
                    refreshCoordinator.refreshProcessing();
                    refreshCoordinator.refreshDashboard();
                    success("Adjustment finalized. All positive and negative balances will be applied to the next regular payroll. "
                            + "| បានបញ្ចប់ការកែតម្រូវ។ ចំនួនវិជ្ជមាន និងអវិជ្ជមានទាំងអស់នឹងអនុវត្តនៅប្រាក់បៀវត្សខែបន្ទាប់។");
                },
                ex -> error(message(ex)));
    }

    private void openPaymentExecutionDialog() {
        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);
        if (batch == null) {
            error("Select a payment batch first. | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ជាមុនសិន។");
            return;
        }

        boolean recoveryOnly = batch.paymentAmount() == null || batch.paymentAmount().signum() == 0;
        CustomDialog dialog = editorDialog(
                PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.matches(batch.installmentType())
                        ? "Finalize Adjustment | បញ្ចប់ការកែតម្រូវ"
                        : "Confirm Payment Execution | បញ្ជាក់ការបើកប្រាក់ជាក់ស្តែង");

        ComboBox<String> method = new ComboBox<>("Payment Method | វិធីបើកប្រាក់");
        method.setItems("BANK_TRANSFER", "CASH", "CHEQUE", "OTHER", "NO_CASH_MOVEMENT");
        method.setItemLabelGenerator(PayrollPaymentsPanel::paymentMethodLabel);
        method.setRequiredIndicatorVisible(true);
        method.setWidthFull();

        TextField paymentReference = new TextField("Payment Reference | លេខយោងការបើកប្រាក់");
        paymentReference.setRequiredIndicatorVisible(true);
        paymentReference.setWidthFull();

        TextField bankReference = new TextField("Bank Reference | លេខយោងធនាគារ");
        bankReference.setWidthFull();

        if (recoveryOnly) {
            method.setValue("NO_CASH_MOVEMENT");
            method.setEnabled(false);
            paymentReference.setValue("RECOVERY-ONLY-BATCH-" + batch.id());
        } else {
            method.setValue("BANK_TRANSFER");
        }

        Runnable updateBankReference = () -> {
            boolean bankTransfer = "BANK_TRANSFER".equals(method.getValue());
            bankReference.setVisible(bankTransfer);
            bankReference.setRequiredIndicatorVisible(bankTransfer);
        };
        method.addValueChangeListener(event -> updateBankReference.run());
        updateBankReference.run();

        String exportStatus = batch.paymentFileName() == null
                ? "Bank file: not exported | ឯកសារធនាគារ៖ មិនទាន់ទាញយក"
                : "Bank file: " + batch.paymentFileName()
                        + " | បានទាញយកឯកសារធនាគារ";
        Paragraph explanation = new Paragraph(
                recoveryOnly
                        ? "This adjustment has no cash payment. Finalization records the recovery carry-forward only. "
                                + "| ការកែតម្រូវនេះមិនមានប្រាក់ត្រូវបើកទេ ហើយកត់ត្រាតែការកាត់សងខែក្រោយ។"
                        : "Record the real payment evidence before PAID. BANK_TRANSFER requires the approved bank file to be exported first and a bank reference to be entered. "
                                + "| សូមកត់ត្រាភស្តុតាងការបើកប្រាក់ជាក់ស្តែង មុនប្តូរទៅ PAID។");
        Paragraph fileStatus = new Paragraph(exportStatus);

        dialog.add(new VerticalLayout(explanation, fileStatus, method, paymentReference, bankReference));

        Button confirmPaid = primaryButton(
                recoveryOnly ? "Finalize | បញ្ចប់" : "Confirm Paid | បញ្ជាក់ថាបានបើក",
                VaadinIcon.CHECK);
        Button cancel = new Button("Cancel | បោះបង់", event -> dialog.close());

        confirmPaid.addClickListener(event -> {
            Long batchId = batch.id();
            PaymentExecutionInput input = new PaymentExecutionInput(
                    method.getValue(),
                    paymentReference.getValue(),
                    bankReference.getValue());

            ProgressDialog.runAsync(
                    recoveryOnly
                            ? "Finalize Adjustment | បញ្ចប់ការកែតម្រូវ"
                            : "Confirm Payment | បញ្ជាក់ការបើកប្រាក់",
                    recoveryOnly
                            ? "Finalizing recovery/carry-forward records... | កំពុងបញ្ចប់កំណត់ត្រាកាត់សង និងយកទៅខែក្រោយ..."
                            : "Recording payment execution and finalizing the batch... | កំពុងកត់ត្រាការបើកប្រាក់ និងបញ្ចប់កញ្ចប់...",
                    () -> paymentService.confirmBatchPaid(batchId, input),
                    () -> {
                        dialog.close();
                        refreshPaymentBatches();
                        refreshCoordinator.refreshPeriods();
                        refreshCoordinator.refreshProcessing();
                        refreshCoordinator.refreshDashboard();
                        success("Payment execution confirmed and batch finalized. "
                                + "| បានបញ្ជាក់ការបើកប្រាក់ និងបញ្ចប់កញ្ចប់ដោយជោគជ័យ។");
                    },
                    ex -> error(message(ex)));
        });

        dialog.getFooter().add(cancel, confirmPaid);
        dialog.open();
    }

    private void printPayslips() {
        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);
        if (batch == null) {
            error("Select a paid payment batch first. | សូមជ្រើសរើសកញ្ចប់ដែលបានបើកប្រាក់ជាមុនសិន។");
            return;
        }

        Set<Long> selectedPaymentIds = paymentDetailGrid.getSelectedItems().stream()
                .map(EmployeePaymentRow::id)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        try {
            PayslipReport report = payslipService.generatePayslips(batch.id(), selectedPaymentIds);
            boolean selectedOnly = !selectedPaymentIds.isEmpty();
            boolean firstInstallment = PayrollInstallmentType.FIRST_INSTALLMENT.matches(batch.installmentType());
            String titleEn = firstInstallment
                    ? (selectedOnly ? "Selected First Installment Payslips" : "First Installment Payslips")
                    : (selectedOnly ? "Selected Payslips" : "All Payslips");
            String titleKh = firstInstallment
                    ? (selectedOnly ? "បង្កាន់ដៃបើកប្រាក់លើកទីមួយដែលបានជ្រើស" : "បង្កាន់ដៃបើកប្រាក់លើកទីមួយ")
                    : (selectedOnly ? "បង្កាន់ដៃប្រាក់បៀវត្សដែលបានជ្រើស" : "បង្កាន់ដៃប្រាក់បៀវត្សទាំងអស់");
            openPayslipPreview(report, titleEn, titleKh);
        } catch (Exception ex) {
            error(message(ex));
        }
    }

    private void openPayslipPreview(PayslipReport report, String titleEn, String titleKh) {
        if (report == null || report.reportPath() == null || report.reportPath().isBlank()) {
            error("Payslip report is not available. | មិនមានរបាយការណ៍បង្កាន់ដៃប្រាក់បៀវត្សទេ។");
            return;
        }

        PreviewReport.builder(report.reportPath())
                .params(report.parameters())
                .title(titleEn, titleKh)
                .downloadFileName(report.fileName())
                .open();
    }

    private void triggerDownload(String fileName, byte[] content, String contentType) {
        StreamResource resource = new StreamResource(
                fileName, () -> new ByteArrayInputStream(content));
        resource.setContentType(contentType);
        resource.setCacheTime(0);

        if (downloadRegistration != null) {
            downloadRegistration.unregister();
        }
        downloadRegistration = VaadinSession.getCurrent()
                .getResourceRegistry()
                .registerResource(resource);
        String uri = downloadRegistration.getResourceUri().toString();
        UI.getCurrent().getPage().executeJs(
                "const a=document.createElement('a');a.href=$0;a.download=$1;"
                        + "document.body.appendChild(a);a.click();a.remove();",
                uri, fileName);
    }


    private String displayBatchStatus(PaymentBatchRow batch) {
        if (batch != null
                && PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.matches(batch.installmentType())
                && PayrollPaymentBatchStatus.PAID.matches(batch.status())
                && "NO_CASH_MOVEMENT".equals(batch.paymentMethod())
                && paymentService.hasPendingAdjustmentCarryForward(batch.runId())) {
            return "NEXT_PAYROLL";
        }
        return batch == null ? "" : batch.status();
    }

    private static String paymentMethodLabel(String method) {
        if (method == null || method.isBlank()) {
            return "-";
        }
        return switch (method) {
            case "BANK_TRANSFER" -> "Bank Transfer | ផ្ទេរធនាគារ";
            case "CASH" -> "Cash | សាច់ប្រាក់";
            case "CHEQUE" -> "Cheque | មូលប្បទានប័ត្រ";
            case "OTHER" -> "Other | ផ្សេងៗ";
            case "NO_CASH_MOVEMENT" -> "Next Payroll | ប្រាក់បៀវត្សខែបន្ទាប់";
            default -> method.replace('_', ' ');
        };
    }

    private void confirmPaymentBatchStatus(String targetStatus) {

        PaymentBatchRow batch = selectedPaymentBatch().orElse(null);

        if (batch == null) {

            error("Select a payment batch first. | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ជាមុនសិន។");

            return;

        }

        PayrollPaymentBatchStatus target = PayrollPaymentBatchStatus.from(targetStatus);
        String action = switch (target) {
            case APPROVED -> "Approve";
            case CANCELLED -> "Cancel";
            case PAID -> throw new IllegalArgumentException(
                    "Use Confirm Payment to record execution evidence before PAID. "
                            + "| សូមប្រើ Confirm Payment ដើម្បីកត់ត្រាភស្តុតាងមុន PAID។");
            case DRAFT -> throw new IllegalArgumentException(
                    "DRAFT is not a manual target from this action. "
                            + "| មិនអាចប្តូរទៅ DRAFT ពីសកម្មភាពនេះបានទេ។");
        };

        PayrollConfirmDialog confirm = confirm(

                action + " Payment Batch | " + statusLabel(targetStatus),

                action + " " + installmentTypeLabel(batch.installmentType())

                        + " dated " + batch.paymentDate() + "? "

                        + "| បញ្ជាក់ការប្តូរស្ថានភាពកញ្ចប់បើកប្រាក់នេះមែនទេ?");

        confirm.setConfirmText(action + " | បញ្ជាក់");

        if (PayrollPaymentBatchStatus.CANCELLED.matches(targetStatus)) {

            confirm.setConfirmButtonTheme("error primary");

        }

        confirm.addConfirmListener(event -> {

            Long batchId = batch.id();

            ProgressDialog.runAsync(
                    PayrollPaymentBatchStatus.APPROVED.matches(targetStatus)
                            ? "Approve Payment Batch | អនុម័តកញ្ចប់បើកប្រាក់"
                            : "Cancel Payment Batch | បោះបង់កញ្ចប់បើកប្រាក់",
                    PayrollPaymentBatchStatus.APPROVED.matches(targetStatus)
                            ? "Approving the payment batch... | កំពុងអនុម័តកញ្ចប់បើកប្រាក់..."
                            : "Cancelling the payment batch... | កំពុងបោះបង់កញ្ចប់បើកប្រាក់...",
                    () -> paymentService.moveBatchTo(batchId, targetStatus),
                    () -> {
                        refreshPaymentBatches();
                        refreshCoordinator.refreshPeriods();
                        refreshCoordinator.refreshProcessing();
                        refreshCoordinator.refreshDashboard();
                        success("Payment batch updated successfully. | បានកែប្រែកញ្ចប់បើកប្រាក់ដោយជោគជ័យ។");
                    },
                    ex -> error(message(ex)));

        });

        confirm.open();

    }


    private record PaymentRefreshSnapshot(
            PaymentScheduleSummary schedule,
            List<PaymentBatchRow> batches) {
    }

}
