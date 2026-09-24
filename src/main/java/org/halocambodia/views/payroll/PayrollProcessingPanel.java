package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.*;
import static org.halocambodia.views.payroll.PayrollViewSupport.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.enums.PayrollAttendanceControlStatus;
import org.halocambodia.enums.PayrollEmployeeStatus;
import org.halocambodia.enums.PayrollPeriodStatus;
import org.halocambodia.enums.PayrollRunStatus;
import org.halocambodia.enums.PayrollRunType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.PayrollActionPermissions;
import org.halocambodia.services.PayrollService;

import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.ProgressDialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

final class PayrollProcessingPanel extends VerticalLayout {

    private final PayrollService service;
    private final AuthenticatedUser authenticatedUser;
    private final PayrollRefreshCoordinator refreshCoordinator;

    private final ComboBox<PeriodRow> periodFilter =new ComboBox<>("Payroll Period");
    private final ComboBox<RunRow> runFilter =new ComboBox<>("Payroll Run");
    private final TextField employeeSearch = new TextField();
    private final Grid<EmployeePayrollRow> employeeGrid = new Grid<>();

    private final Button createRunButton = actionButton("New Run", VaadinIcon.PLUS);
    private final Button generateButton = actionButton("Generate Employees", VaadinIcon.USERS);
    private final Button employeeStatusButton =actionButton("Change Status", VaadinIcon.EDIT);
    private final Button calculateButton = actionButton("Calculate", VaadinIcon.CHART);
    private final Button reviewButton = actionButton("Send for Review", VaadinIcon.CLIPBOARD_CHECK);
    private final Button returnCorrectionButton =actionButton("Return for Correction", VaadinIcon.REPLY);
    private final Button approveButton =actionButton("Approve", VaadinIcon.CHECK_CIRCLE);
    private final Button downloadButton = actionButton("Download", VaadinIcon.DOWNLOAD);

    private final Button auditHistoryButton = actionButton("Audit History", VaadinIcon.TIME_BACKWARD);
    private StreamRegistration processingDownloadRegistration;

    private final Div attendanceControlPanel = new Div();
    private final Button checkAttendanceButton = actionButton("Check Attendanceន", VaadinIcon.CLIPBOARD_TEXT);
    private final Button approveAttendanceButton =actionButton("Approve & Lock", VaadinIcon.LOCK);
    private final Button reopenAttendanceButton = actionButton("Reopen", VaadinIcon.REFRESH);
    private AttendanceControlRow attendanceControl;

    private final Span processingSummary = new Span("Select a payroll period and run. | សូមជ្រើសរើសរយៈពេល និងដំណើរការប្រាក់បៀវត្ស។");
    private final Div processingConfigurationStatus = new Div();
    private boolean processingConfigurationReady;
    private boolean processingRunTypeAvailable;
    private boolean loadingRuns;
    private int currentCorrectionCount;

    PayrollProcessingPanel( PayrollService service,AuthenticatedUser authenticatedUser, PayrollRefreshCoordinator refreshCoordinator) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
        this.refreshCoordinator = refreshCoordinator;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        configureEmployeeGrid();
        addDetachListener(event -> releaseProcessingDownloadResource());
        Component content = buildPage();
        add(content);
        setFlexGrow(1, content);
    }

    Long selectedPeriodId() {
        PeriodRow period = periodFilter.getValue();
        return period == null ? null : period.id();
    }

    private Component buildPage() {

      // VerticalLayout page = page("Payroll Processing | ដំណើរការប្រាក់បៀវត្ស","Check and approve attendance before generating monthly payroll snapshots. "  + "| ពិនិត្យ និងអនុម័តវត្តមានមុនបង្កើតទិន្នន័យប្រាក់បៀវត្សប្រចាំខែ។");
    	 VerticalLayout page = page(null,null);

        periodFilter.setItemLabelGenerator(PayrollViewSupport::periodLabel);

        periodFilter.setClearButtonVisible(true);
       // periodFilter.setWidth("390px");
        periodFilter.addValueChangeListener(e -> loadRuns(e.getValue(), null));
        runFilter.setItemLabelGenerator(PayrollViewSupport::runLabel);
        runFilter.setClearButtonVisible(true);
        runFilter.setPlaceholder("No run yet - click New Run");
       // runFilter.setWidth("300px");
        runFilter.addValueChangeListener(e -> {
            if (!loadingRuns) {
                refreshProcessing();
            }
        });
        createRunButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createRunButton.addClickListener(e -> openCreateRunDialog());
        generateButton.addClickListener(e -> confirmGenerate());
        employeeStatusButton.addClickListener(e -> openEmployeeStatusDialog());
        calculateButton.addClickListener(e -> calculateSelectedOrAll());
        reviewButton.addClickListener(e -> advanceReviewWorkflow());
        returnCorrectionButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        returnCorrectionButton.addClickListener(e -> openReturnForCorrectionDialog());
        approveButton.addClickListener(e -> {
            RunRow run = runFilter.getValue();
            if (run == null) {
                error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");
                return;
            }
            executeRunAction(
                    () -> service.moveRunTo(run.id(), PayrollRunStatus.APPROVED.code()),
                    "Payroll approved successfully. | បានអនុម័តប្រាក់បៀវត្សដោយជោគជ័យ។");
        });
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS,ButtonVariant.LUMO_PRIMARY);
        downloadButton.addClickListener(e -> downloadProcessingExcel());
        auditHistoryButton.addClickListener(e -> openRunAuditHistory());
        HorizontalLayout filters = new HorizontalLayout(periodFilter, runFilter, createRunButton);

        filters.setAlignItems(Alignment.END);

        filters.setWrap(true);

        filters.setWidthFull();

        HorizontalLayout actions = toolbar( generateButton, employeeStatusButton, calculateButton, reviewButton, returnCorrectionButton, approveButton, downloadButton, auditHistoryButton);
        employeeSearch.setPlaceholder("Search insurance number or employee name...");
        employeeSearch.setClearButtonVisible(true);
       // employeeSearch.setWidth("min(420px, 100%)");

        employeeSearch.setValueChangeMode(ValueChangeMode.LAZY);
        employeeSearch.addValueChangeListener(e -> refreshEmployees());
        processingSummary.addClassName("payroll-processing-summary");
        HorizontalLayout searchBar = new HorizontalLayout(processingSummary, employeeSearch);
        searchBar.setWidthFull();
        searchBar.setAlignItems(Alignment.CENTER);
        searchBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        searchBar.setWrap(true);

        // Configuration and Attendance Readiness are still evaluated in
        // refreshProcessing() for button/workflow safety, but the duplicate
        // status sections are not shown on the Processing tab.
        page.add(filters, actions, searchBar, employeeGrid);

        page.setFlexGrow(1, employeeGrid);

        return page;

    }


    private Component buildAttendanceControlPanel() {

        attendanceControlPanel.addClassName("payroll-attendance-control");
        attendanceControlPanel.setWidthFull();
        checkAttendanceButton.addClickListener(e -> {
            PeriodRow period = periodFilter.getValue();
            if (period == null) {
                error("Select a payroll period first. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");
                return;
            }

            executeAttendanceAction(

                    () -> service.checkAttendance(period.id(), null),

                    "Attendance checked. Review the results before approval. "

                            + "| បានពិនិត្យវត្តមាន។ សូមពិនិត្យលទ្ធផលមុនអនុម័ត។");

        });

        approveAttendanceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        approveAttendanceButton.addClickListener(e -> confirmApproveAttendance());
        reopenAttendanceButton.addClickListener(e -> confirmReopenAttendance());
        renderAttendanceControl();

        return attendanceControlPanel;

    }


    private void configureEmployeeGrid() {

        configureGrid(employeeGrid);

        employeeGrid.setSelectionMode(Grid.SelectionMode.MULTI);

        employeeGrid.addColumn(EmployeePayrollRow::insuranceNo).setHeader("Insurance").setFrozen(true).setAutoWidth(true).setSortable(true);
        employeeGrid.addColumn(EmployeePayrollRow::nameEn).setHeader("Employee Name (English)").setFrozen(true).setAutoWidth(true).setSortable(true);
        employeeGrid.addColumn(EmployeePayrollRow::nameKh).setHeader("Employee Name (Khmer)").setAutoWidth(true);
        employeeGrid.addColumn(EmployeePayrollRow::gender).setHeader("Gender").setAutoWidth(true);
        employeeGrid.addColumn(EmployeePayrollRow::bankName).setHeader("Bank").setAutoWidth(true);
        employeeGrid.addColumn(EmployeePayrollRow::bankAccount).setHeader("Bank Account").setAutoWidth(true);
        employeeGrid.addColumn(EmployeePayrollRow::spouseCount).setHeader("Spouse").setAutoWidth(true);
        employeeGrid.addColumn(EmployeePayrollRow::taxDependentCount).setHeader("Tax-Eligible Dependants").setAutoWidth(true);
        moneyColumn(employeeGrid, "Basic Salary", EmployeePayrollRow::basicSalary);
        moneyColumn(employeeGrid, "Gross Pay", EmployeePayrollRow::totalEarnings);
        moneyColumn(employeeGrid, "Deductions", EmployeePayrollRow::totalDeductions);
        moneyColumn(employeeGrid, "Salary Tax", EmployeePayrollRow::salaryTax);
        moneyColumn(employeeGrid, "Net Pay", EmployeePayrollRow::netSalary);
        employeeGrid.addComponentColumn(emp -> statusBadge(emp.payrollStatus())).setHeader("Payroll Status").setAutoWidth(true);
        employeeGrid.addComponentColumn(emp -> correctionBadge(emp)).setHeader("Correction").setAutoWidth(true);

        employeeGrid.addComponentColumn(emp -> {
            Button details = new Button(VaadinIcon.EYE.create(), e -> openEmployeeDetails(emp));
            details.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            details.setTooltipText("View payroll details | មើលព័ត៌មានលម្អិតប្រាក់បៀវត្ស");
            return details;
        }).setHeader("Details").setFrozenToEnd(true).setAutoWidth(true);

        employeeGrid.addItemDoubleClickListener(e -> openEmployeeDetails(e.getItem()));
        employeeGrid.addSelectionListener(e -> {
            updateProcessingActions();
            updateCalculateButtonLabel();
        });
        updateCalculateButtonLabel();

    }


    void refresh() {

        try {

            Long selectedId = Optional.ofNullable(periodFilter.getValue()).map(PeriodRow::id).orElse(null);

            List<PeriodRow> periods = service.findPeriods();

            periodFilter.setItems(periods);

            periods.stream().filter(p -> p.id().equals(selectedId)).findFirst()

                    .or(() -> periods.stream().findFirst())

                    .ifPresent(periodFilter::setValue);

            if (periods.isEmpty()) {

                periodFilter.clear();

                runFilter.clear();

                employeeGrid.setItems(List.of());

            }

        } catch (Exception ex) {

            error(databaseMessage(ex));

        }

    }

    private void loadRuns(PeriodRow period, Long preferredRunId) {

        loadingRuns = true;
        try {
            if (period == null) {
                runFilter.clear();
                runFilter.setItems(List.of());
            } else {
                List<RunRow> runs = service.findRuns(period.id());
                runFilter.setItems(runs);
                runs.stream().filter(r -> r.id().equals(preferredRunId)).findFirst()
                        .or(() -> runs.stream().findFirst())
                        .ifPresentOrElse(runFilter::setValue, runFilter::clear);
            }
        } finally {
            loadingRuns = false;
        }

        // Refresh exactly once. Previously setValue()/clear() could trigger a
        // value-change refresh and this explicit refresh immediately triggered
        // the same expensive payroll queries a second time.
        refreshProcessing();

    }

    private void refreshProcessing() {

        refreshAttendanceControl();

        renderProcessingConfigurationStatus();

        PeriodRow period = periodFilter.getValue();

        processingRunTypeAvailable = period != null

                && !service.findAllowedRunTypes(period.id()).isEmpty();

        RunRow run = runFilter.getValue();
        currentCorrectionCount = run == null
                ? 0
                : service.countCorrectionRequiredEmployees(run.id());

        refreshEmployees();

        updateProcessingActions();

        updateGenerateButtonLabel();

        updateCalculateButtonLabel();

    }

    private void renderProcessingConfigurationStatus() {

        processingConfigurationStatus.removeAll();

        PeriodRow period = periodFilter.getValue();

        if (period == null) {

            processingConfigurationReady = false;

            processingConfigurationStatus.add(new Span(

                    "Payroll configuration: select a payroll period to see the rules and components used by Calculate. "

                            + "| ការកំណត់ប្រាក់បៀវត្ស៖ សូមជ្រើសរើសរយៈពេលដើម្បីមើលច្បាប់ "

                            + "និងសមាសភាគដែលប្រើក្នុងការគណនា។"));

            return;

        }

        try {

            List<ComponentRow> components = service.findComponents(true);

            List<PayrollRuleRow> rules = service.findPayrollRules(period.year()).stream()

                    .filter(PayrollRuleRow::active)

                    .toList();

            Set<String> componentCodes = components.stream()

                    .map(ComponentRow::code)

                    .collect(java.util.stream.Collectors.toSet());

            List<String> issues = new ArrayList<>();

            for (String requiredCode : List.of(

                    "BASIC_SALARY", "SALARY_TAX",

                    "NSSF_HEALTH_EMPLOYEE", "NSSF_HEALTH_EMPLOYER",

                    "NSSF_RISK_EMPLOYEE", "NSSF_RISK_EMPLOYER",

                    "NSSF_PENSION_EMPLOYEE", "NSSF_PENSION_EMPLOYER")) {

                if (!componentCodes.contains(requiredCode)) {

                    issues.add("Missing active component " + requiredCode);

                }

            }

            if (rules.isEmpty()) {

                issues.add("No active Payroll Rules for " + period.year());

            }

            rules.stream()

                    .filter(rule -> !"LEAVE_ENTITLEMENT".equals(rule.ruleType()))

                    .filter(rule -> rule.componentId() == null)

                    .forEach(rule -> issues.add("Rule " + rule.code() + " has no Payroll Component"));

            rules.stream()

                    .filter(rule -> rule.componentCode() != null)

                    .filter(rule -> !componentCodes.contains(rule.componentCode()))

                    .forEach(rule -> issues.add(

                            "Rule " + rule.code() + " uses an inactive Payroll Component"));

            List<TaxConfigRow> taxConfigs = service.findTaxConfigs().stream()

                    .filter(TaxConfigRow::active)

                    .filter(config -> config.taxYear() == period.year())

                    .toList();

            if (taxConfigs.size() != 1) {

                issues.add("Exactly one active Salary Tax year is required for " + period.year());

            } else if (service.findTaxBrackets(taxConfigs.getFirst().id()).isEmpty()) {

                issues.add("Salary Tax year " + period.year() + " has no brackets");

            }

            issues.addAll(service.findNssfConfigurationIssues(period.id()));

            Span status = new Span(

                    ("Configuration | ការកំណត់ · Rule Year | ឆ្នាំច្បាប់ %d "

                            + "· %d active rule(s) | ច្បាប់សកម្ម · %d active component(s) | សមាសភាគសកម្ម")

                                    .formatted(period.year(), rules.size(), components.size()));

            status.getStyle().set("font-weight", "600");

            Span readiness = new Span(issues.isEmpty()

                    ? "Ready | រួចរាល់"

                    : "Needs Setup | ត្រូវកំណត់បន្ថែម");

            readiness.getElement().getThemeList().add(

                    issues.isEmpty() ? "badge success" : "badge error");

            HorizontalLayout row = new HorizontalLayout(status, readiness);

            row.setWidthFull();

            row.setAlignItems(Alignment.CENTER);

            row.setJustifyContentMode(JustifyContentMode.BETWEEN);

            row.setWrap(true);

            processingConfigurationStatus.add(row);

            processingConfigurationReady = issues.isEmpty();

            if (!issues.isEmpty()) {

                Paragraph details = new Paragraph(

                        String.join(" · ", issues)

                                + " | សូមបំពេញការកំណត់ខាងលើមុនចុចគណនា។");

                details.getStyle().set("color", "var(--lumo-error-text-color)");

                processingConfigurationStatus.add(details);

            }

        } catch (Exception ex) {

            processingConfigurationReady = false;

            Span warning = new Span(

                    "Payroll configuration could not be loaded | មិនអាចផ្ទុកការកំណត់ប្រាក់បៀវត្សបាន៖ "

                            + message(ex));

            warning.getStyle().set("color", "var(--lumo-error-text-color)");

            processingConfigurationStatus.add(warning);

        }

    }

    private void refreshAttendanceControl() {

        PeriodRow period = periodFilter.getValue();

        if (period == null) {

            attendanceControl = null;

            renderAttendanceControl();

            return;

        }

        try {

            attendanceControl = service.findAttendanceControl(period.id());

            renderAttendanceControl();

        } catch (Exception ex) {

            attendanceControl = null;

            renderAttendanceControl();

            error(databaseMessage(ex));

        }

    }

    private void renderAttendanceControl() {

        attendanceControlPanel.removeAll();

        H3 title = sectionTitle("Step 1: Attendance Readiness | ជំហានទី១៖ ការត្រៀមវត្តមាន");

        title.getStyle().set("margin", "0");

        if (periodFilter.getValue() == null) {

            Paragraph message = new Paragraph(

                    "Select a payroll period to check attendance. "

                            + "| សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សដើម្បីពិនិត្យវត្តមាន។");

            message.addClassName("payroll-attendance-message");

            attendanceControlPanel.add(title, message,

                    toolbar(checkAttendanceButton, approveAttendanceButton, reopenAttendanceButton));

            return;

        }

        if (attendanceControl == null) {

            Paragraph message = new Paragraph(

                    "Attendance control is unavailable. Run V8__create_payroll_attendance_control.sql first. "

                            + "| មិនមានការគ្រប់គ្រងវត្តមានទេ។ សូមដំណើរការ "

                            + "V8__create_payroll_attendance_control.sql ជាមុនសិន។");

            message.addClassName("payroll-attendance-message");

            attendanceControlPanel.add(title, message,

                    toolbar(checkAttendanceButton, approveAttendanceButton, reopenAttendanceButton));

            return;

        }

        String displayedStatus = attendanceControl.stale() ? "STALE" : attendanceControl.status();

        HorizontalLayout heading = new HorizontalLayout(title, statusBadge(displayedStatus));

        heading.setAlignItems(Alignment.CENTER);

        heading.setWrap(true);

        Div metrics = new Div();

        metrics.addClassName("payroll-attendance-metrics");

        metrics.add(

                attendanceMetric("Roster Employees | បុគ្គលិកក្នុងតារាងវេន",

                        Long.toString(attendanceControl.rosterEmployeeCount())),

                attendanceMetric("Roster Days | ថ្ងៃក្នុងតារាងវេន",

                        Long.toString(attendanceControl.rosterDayCount())),

                attendanceMetric("Attendance Records | កំណត់ត្រាវត្តមាន",

                        Long.toString(attendanceControl.attendanceRecordCount())),

                attendanceMetric("Roster Fallback Days | ថ្ងៃប្រើតារាងវេនជំនួស",

                        Long.toString(attendanceControl.rosterFallbackDayCount())),

                attendanceMetric("Waiting HR Verification | រង់ចាំ HR ផ្ទៀងផ្ទាត់",

                        Long.toString(attendanceControl.unverifiedAttendanceCount())),

                attendanceMetric("Overtime Hours | ម៉ោងបន្ថែម", decimal(attendanceControl.overtimeHours())));

        Paragraph message = new Paragraph(attendanceControlMessage(attendanceControl));

        message.addClassName("payroll-attendance-message");

        attendanceControlPanel.add(heading, metrics, message,

                toolbar(checkAttendanceButton, approveAttendanceButton, reopenAttendanceButton));

    }

    private void refreshEmployees() {

        RunRow run = runFilter.getValue();

        if (run == null) {
            employeeGrid.setItems(List.of());
            processingSummary.setText("Select a payroll period and run. | សូមជ្រើសរើសរយៈពេល និងដំណើរការប្រាក់បៀវត្ស។");
            return;
        }

        List<EmployeePayrollRow> employees = service.findEmployees(run.id(), employeeSearch.getValue());

        employeeGrid.setItems(employees);

        long included = employees.stream().filter(e -> PayrollEmployeeStatus.INCLUDED.matches(e.payrollStatus())).count();
        long onHold = employees.stream().filter(e -> PayrollEmployeeStatus.ON_HOLD.matches(e.payrollStatus())).count();
        long excluded = employees.stream().filter(e -> PayrollEmployeeStatus.EXCLUDED.matches(e.payrollStatus())).count();
        processingSummary.setText(("%04d-%02d · Run  %d · %s · %s · Displayed %d " + "· Included %d · On Hold %d · Excluded %d " + "· Correction %d · Net Pay %s").formatted(run.year(), run.month(), run.runNumber(), runTypeLabel(run.runType()), statusLabel(displayRunStatus(run)),  employees.size(), included,  onHold, excluded, currentCorrectionCount, usd(run.netPay())));

    }

    private String displayRunStatus(RunRow run) {
        if (run != null && PayrollRunType.ADJUSTMENT.matches(run.runType())  && PayrollRunStatus.PAID.matches(run.status())  && service.hasPendingAdjustmentCarryForward(run.id())) {
            return "NEXT_PAYROLL";
        }
        return run == null ? "" : run.status();
    }

    private void updateProcessingActions() {

        PeriodRow period = periodFilter.getValue();

        RunRow run = runFilter.getValue();

        String status = run == null ? "" : run.status();

        int correctionCount = run == null ? 0 : currentCorrectionCount;

        boolean correctionMode = run != null && run.correctionMode();

        boolean hasSelection = !employeeGrid.getSelectedItems().isEmpty();

        boolean selectedEmployeesEditable = !correctionMode || employeeGrid.getSelectedItems().stream() .allMatch(EmployeePayrollRow::correctionRequired);

        boolean periodEditable = period != null && !PayrollPeriodStatus.CLOSED.matches(period.status());

        boolean attendanceReady = attendanceControl != null && attendanceControl.approvedAndCurrent();

        // Post-paid ADJUSTMENT runs reconcile an already-paid regular payroll.
        // Their review/approval workflow must not be blocked by the period attendance gate.
        boolean adjustmentRun = run != null && PayrollRunType.ADJUSTMENT.matches(run.runType());
        boolean workflowAttendanceReady = adjustmentRun || attendanceReady;

        boolean canRunManage = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.RUN_MANAGEMENT, AccessPageType.UPDATED_PAGE);
        boolean canEmployeeStatus = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.EMPLOYEE_STATUS, AccessPageType.UPDATED_PAGE);
        boolean canCalculate = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.CALCULATE, AccessPageType.UPDATED_PAGE);
        boolean canSendReview = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.SEND_REVIEW, AccessPageType.UPDATED_PAGE);
        boolean canReview = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.REVIEW, AccessPageType.UPDATED_PAGE);
        boolean canReturnCorrection = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.RETURN_CORRECTION, AccessPageType.UPDATED_PAGE);
        boolean canApprove = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.APPROVE, AccessPageType.UPDATED_PAGE);
        boolean canAttendanceLock = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.ATTENDANCE_LOCK, AccessPageType.UPDATED_PAGE);

        createRunButton.setEnabled(canRunManage && attendanceReady && processingRunTypeAvailable);

        createRunButton.setTooltipText(!attendanceReady

                ? "Check and approve attendance before creating a payroll run "

                        + "| ពិនិត្យ និងអនុម័តវត្តមានមុនបង្កើតដំណើរការប្រាក់បៀវត្ស"

                : processingRunTypeAvailable

                        ? "Create a payroll run | បង្កើតដំណើរការប្រាក់បៀវត្ស"

                        : "No additional run type is available for this period "

                                + "| មិនមានប្រភេទដំណើរការបន្ថែមសម្រាប់រយៈពេលនេះទេ");

        checkAttendanceButton.setEnabled(canAttendanceLock && periodEditable

                && (attendanceControl == null || !attendanceControl.approvedAndCurrent()));

        approveAttendanceButton.setEnabled(canAttendanceLock && periodEditable

                && attendanceControl != null

                && PayrollAttendanceControlStatus.CHECKED.matches(attendanceControl.status()));

        approveAttendanceButton.setTooltipText(

                attendanceControl != null && PayrollAttendanceControlStatus.CHECKED.matches(attendanceControl.status())

                        ? "Approve and lock the checked attendance. Validation runs again when clicked. "

                                + "| អនុម័ត និងចាក់សោវត្តមានដែលបានពិនិត្យ។ ប្រព័ន្ធនឹងផ្ទៀងផ្ទាត់ម្តងទៀត។"

                        : "Click Check Attendance first. | សូមចុចពិនិត្យវត្តមានជាមុនសិន។");

        reopenAttendanceButton.setEnabled(canAttendanceLock && periodEditable

                && attendanceControl != null && PayrollAttendanceControlStatus.APPROVED.matches(attendanceControl.status()));

        generateButton.setEnabled(canRunManage && attendanceReady && !correctionMode

                && Set.of(PayrollRunStatus.DRAFT.code(), PayrollRunStatus.CALCULATED.code()).contains(status));

        generateButton.setTooltipText(correctionMode

                ? "Generate Employees is locked while returned employees still require correction "

                        + "| មិនអាចបង្កើតបុគ្គលិក ខណៈពេលនៅមានបុគ្គលិកត្រូវកែតម្រូវ"

                : "Generate payroll employees | បង្កើតបុគ្គលិកប្រាក់បៀវត្ស");

        employeeStatusButton.setEnabled(canEmployeeStatus

                && Set.of(PayrollRunStatus.DRAFT.code(), PayrollRunStatus.CALCULATED.code()).contains(status)

                && hasSelection

                && selectedEmployeesEditable);

        employeeStatusButton.setTooltipText(!hasSelection

                ? "Select employee(s) first | សូមជ្រើសរើសបុគ្គលិកជាមុនសិន"

                : !selectedEmployeesEditable

                        ? "Only employees currently returned for correction can have their payroll status changed "

                                + "| អាចប្តូរស្ថានភាពបានតែបុគ្គលិកដែលកំពុងត្រូវបានបញ្ជូនត្រឡប់មកកែតម្រូវប៉ុណ្ណោះ"

                        : correctionMode

                                ? "Change the returned employee payroll status; INCLUDED refreshes and recalculates, "

                                        + "while ON HOLD or EXCLUDED resolves the correction without recalculation "

                                        + "| ប្តូរស្ថានភាពបុគ្គលិកដែលត្រឡប់មកកែតម្រូវ"

                                : "Change payroll participation for the selected employee(s) "

                                        + "| ប្តូរការចូលរួមប្រាក់បៀវត្សរបស់បុគ្គលិកដែលបានជ្រើសរើស");

        // ADJUSTMENT runs reconcile an already-paid regular payroll. Do not disable
        // Calculate All Differences because the current period attendance/configuration
        // readiness gate is intended for normal payroll calculation. Any real calculation
        // validation is still enforced by PayrollService when the action executes.
        boolean calculationPrerequisitesReady = adjustmentRun
                || (attendanceReady && processingConfigurationReady);

        calculateButton.setEnabled(canCalculate && calculationPrerequisitesReady

                && Set.of(PayrollRunStatus.DRAFT.code(), PayrollRunStatus.CALCULATED.code()).contains(status)

                && (!correctionMode
                        || (hasSelection && selectedEmployeesEditable)
                        || (!hasSelection && correctionCount > 0)));

        calculateButton.setTooltipText(!adjustmentRun && !processingConfigurationReady

                ? "Complete Payroll Components, Payroll Rules, Salary Tax, and NSSF setup first "

                        + "| សូមបំពេញការកំណត់ធាតុ ច្បាប់ ពន្ធ និង ប.ស.ស. ជាមុនសិន"

                : correctionMode && hasSelection && !selectedEmployeesEditable

                        ? "Your selection contains employee(s) that were not returned for correction "

                                + "| ការជ្រើសរើសមានបុគ្គលិកដែលមិនត្រូវបានបញ្ជូនត្រឡប់មកកែតម្រូវ"

                        : correctionMode

                                ? "Recalculate only employee(s) returned for correction "

                                        + "| គណនាឡើងវិញតែបុគ្គលិកដែលត្រូវកែតម្រូវ"

                                : "Calculate payroll | គណនាប្រាក់បៀវត្ស");

        boolean calculated = PayrollRunStatus.CALCULATED.matches(status);
        boolean pendingReview = PayrollRunStatus.PENDING_REVIEW.matches(status);
        boolean reviewed = PayrollRunStatus.REVIEWED.matches(status);

        reviewButton.setText(pendingReview  ? "Mark Reviewed"  : "Send for Review");

        reviewButton.setEnabled((calculated ? canSendReview : canReview) && workflowAttendanceReady   && (calculated || pendingReview)  && correctionCount == 0);

        reviewButton.setTooltipText(!(calculated ? canSendReview : canReview)

                ? "You do not have update permission | អ្នកមិនមានសិទ្ធិកែប្រែ"

                : !workflowAttendanceReady

                        ? "Attendance must remain approved and current "

                                + "| វត្តមានត្រូវតែបានអនុម័ត និងទាន់សម័យ"

                        : correctionCount > 0

                                ? correctionCount + " employee(s) still require correction. Recalculate them first. "

                                        + "| នៅមានបុគ្គលិក " + correctionCount

                                        + " នាក់ត្រូវកែតម្រូវ។ សូមគណនាពួកគេឡើងវិញជាមុនសិន។"

                                : calculated

                                        ? "Send this calculated run for review "
                                                + "| ផ្ញើដំណើរការដែលបានគណនាទៅពិនិត្យ"

                                        : pendingReview

                                                ? "Complete the payroll review "
                                                        + "| បញ្ចប់ការពិនិត្យប្រាក់បៀវត្ស"

                                                : "Calculate all INCLUDED employees before review. Current status: "

                                                        + statusLabel(status)

                                                        + " | សូមគណនាបុគ្គលិក INCLUDED ទាំងអស់មុនផ្ញើពិនិត្យ");

        returnCorrectionButton.setEnabled(canReturnCorrection && (pendingReview || reviewed));

        returnCorrectionButton.setTooltipText(!canReturnCorrection

                ? "You do not have update permission | អ្នកមិនមានសិទ្ធិកែប្រែ"

                : (pendingReview || reviewed)

                        ? hasSelection

                                ? "Return only the selected employee(s) for correction "

                                        + "| បញ្ជូនត្រឡប់ទៅកែតម្រូវតែបុគ្គលិកដែលបានជ្រើសរើស"

                                : "No employee selected: return all INCLUDED employees for correction "

                                        + "| មិនបានជ្រើសបុគ្គលិក៖ បញ្ជូនបុគ្គលិក INCLUDED ទាំងអស់ទៅកែតម្រូវ"

                        : "Return for Correction is available while payroll is pending review or reviewed "

                                + "| អាចបញ្ជូនត្រឡប់ទៅកែតម្រូវនៅពេលរង់ចាំពិនិត្យ ឬបានពិនិត្យរួច");

        approveButton.setEnabled(canApprove && workflowAttendanceReady && reviewed);

        approveButton.setTooltipText(!canApprove

                ? "You do not have update permission | អ្នកមិនមានសិទ្ធិកែប្រែ"

                : !workflowAttendanceReady

                        ? "Attendance must remain approved and current "

                                + "| វត្តមានត្រូវតែបានអនុម័ត និងទាន់សម័យ"

                        : reviewed

                                ? "Approve this reviewed payroll run | អនុម័តដំណើរការដែលបានពិនិត្យ"

                                : "Send the calculated run for review before approval. Current status: "

                                        + statusLabel(status)

                                        + " | សូមផ្ញើដំណើរការទៅពិនិត្យមុនអនុម័ត");

        downloadButton.setEnabled(run != null);
        downloadButton.setTooltipText(run == null
                ? "Select a payroll run first | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុន"
                : "Download the currently displayed Processing employees as Excel | ទាញយកបុគ្គលិកដែលកំពុងបង្ហាញជាឯកសារ Excel");

        auditHistoryButton.setEnabled(run != null);
        auditHistoryButton.setTooltipText(run == null
                ? "Select a payroll run first | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុន"
                : "View immutable payroll audit history | មើលប្រវត្តិសវនកម្មប្រាក់បៀវត្សដែលមិនអាចកែប្រែបាន");

    }

    private void downloadProcessingExcel() {
        RunRow run = runFilter.getValue();
        if (run == null) {
            error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");
            return;
        }

        Long runId = run.id();
        String search = employeeSearch.getValue();
        String fileName = "Payroll_Processing_%04d_%02d_Run_%d_%s.xlsx"
                .formatted(run.year(), run.month(), run.runNumber(), run.runType());

        ProgressDialog.runAsync(
                "Download Payroll Excel | ទាញយក Excel ប្រាក់បៀវត្ស",
                "Preparing payroll Processing Excel... | កំពុងរៀបចំឯកសារ Excel ប្រាក់បៀវត្ស...",
                () -> {
                    List<EmployeePayrollRow> rows = service.findEmployees(runId, search);
                    return buildProcessingExcelWorkbook(run, rows);
                },
                excel -> {
                    triggerProcessingExcelDownload(fileName, excel);
                    success("Payroll Excel is ready. Download started. | ឯកសារ Excel ប្រាក់បៀវត្សរួចរាល់។ ការទាញយកបានចាប់ផ្តើម។");
                },
                ex -> error(message(ex)));
    }

    private byte[] buildProcessingExcelWorkbook(RunRow run, List<EmployeePayrollRow> employees) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Payroll Processing");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue(
                    "Payroll Processing %04d-%02d · Run %d · %s · %s"
                            .formatted(
                                    run.year(),
                                    run.month(),
                                    run.runNumber(),
                                    runTypeLabel(run.runType()),
                                    statusLabel(displayRunStatus(run))));
            title.getCell(0).setCellStyle(titleStyle);

            String[] headers = {
                    "Insurance",
                    "Employee Name (English)",
                    "Employee Name (Khmer)",
                    "Gender",
                    "Bank",
                    "Bank Account",
                    "Spouse",
                    "Tax-Eligible Dependants",
                    "Basic Salary",
                    "Gross Pay",
                    "Deductions",
                    "Salary Tax",
                    "Net Pay",
                    "Payroll Status",
                    "Correction"
            };

            Row header = sheet.createRow(2);
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
                header.getCell(column).setCellStyle(headerStyle);
            }

            int rowIndex = 3;
            for (EmployeePayrollRow employee : employees) {
                Row row = sheet.createRow(rowIndex++);
                setLongCell(row, 0, employee.insuranceNo());
                setTextCell(row, 1, employee.nameEn());
                setTextCell(row, 2, employee.nameKh());
                setTextCell(row, 3, employee.gender());
                setTextCell(row, 4, employee.bankName());
                setTextCell(row, 5, employee.bankAccount());
                row.createCell(6).setCellValue(employee.spouseCount());
                row.createCell(7).setCellValue(employee.taxDependentCount());
                setMoneyCell(row, 8, employee.basicSalary(), moneyStyle);
                setMoneyCell(row, 9, employee.totalEarnings(), moneyStyle);
                setMoneyCell(row, 10, employee.totalDeductions(), moneyStyle);
                setMoneyCell(row, 11, employee.salaryTax(), moneyStyle);
                setMoneyCell(row, 12, employee.netSalary(), moneyStyle);
                setTextCell(row, 13, statusLabel(employee.payrollStatus()));
                setTextCell(row, 14, employee.correctionRequired() ? "Correction Required" : "");
            }

            sheet.createFreezePane(0, 3);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    2,
                    Math.max(2, rowIndex - 1),
                    0,
                    headers.length - 1));

            for (int column = 0; column < headers.length; column++) {
                sheet.autoSizeColumn(column);
                int width = Math.min(sheet.getColumnWidth(column) + 512, 12000);
                sheet.setColumnWidth(column, width);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to create payroll Processing Excel file. | មិនអាចបង្កើតឯកសារ Excel ប្រាក់បៀវត្សបានទេ។",
                    ex);
        }
    }

    private static void setTextCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private static void setLongCell(Row row, int column, Number value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.longValue());
        } else {
            row.createCell(column);
        }
    }

    private static void setMoneyCell(Row row, int column, BigDecimal value, CellStyle style) {
        var cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void triggerProcessingExcelDownload(String fileName, byte[] content) {
        releaseProcessingDownloadResource();

        StreamResource resource = new StreamResource(
                fileName,
                () -> new ByteArrayInputStream(content));
        resource.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resource.setCacheTime(0);

        processingDownloadRegistration = VaadinSession.getCurrent()
                .getResourceRegistry()
                .registerResource(resource);

        String uri = processingDownloadRegistration.getResourceUri().toString();
        UI.getCurrent().getPage().executeJs(
                "const a=document.createElement('a');"
                        + "a.href=$0;a.download=$1;document.body.appendChild(a);"
                        + "a.click();a.remove();",
                uri,
                fileName);
    }

    private void releaseProcessingDownloadResource() {
        if (processingDownloadRegistration != null) {
            try {
                processingDownloadRegistration.unregister();
            } catch (Exception ignore) {
                // Resource may already be detached with the Vaadin session.
            } finally {
                processingDownloadRegistration = null;
            }
        }
    }

    private void openRunAuditHistory() {
        RunRow run = runFilter.getValue();
        if (run == null) {
            error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");
            return;
        }

        Long runId = run.id();
        int runNumber = run.runNumber();

        ProgressDialog.runAsync(
                "Payroll Audit History | ប្រវត្តិសកម្មភាពប្រាក់បៀវត្ស",
                "Loading payroll audit history... | កំពុងទាញប្រវត្តិសកម្មភាពប្រាក់បៀវត្ស...",
                () -> service.findRunAuditHistory(runId),
                history -> new PayrollAuditHistoryDialog(
                        "Payroll Run Audit History | ប្រវត្តិសកម្មភាពដំណើរការប្រាក់បៀវត្ស · Run "
                                + runNumber,
                        history)
                        .open(),
                ex -> error(message(ex)));
    }

    private void confirmApproveAttendance() {

        PeriodRow period = periodFilter.getValue();

        if (period == null || attendanceControl == null) {

            error("Check attendance for a payroll period first. "

                    + "| សូមពិនិត្យវត្តមានសម្រាប់រយៈពេលប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        LocalDate cutoffDate = LocalDate.now(ZoneId.of("Asia/Phnom_Penh"));

        if (cutoffDate.isAfter(period.endDate())) cutoffDate = period.endDate();

        PayrollConfirmDialog confirm = confirm("Approve and Lock Attendance | អនុម័ត និងចាក់សោវត្តមាន",

                ("Save the complete attendance snapshot for %04d-%02d and lock attendance through %s? "

                        + "Attendance after the cutoff date remains editable. "

                        + "| រក្សាទុកទិន្នន័យវត្តមានពេញលេញសម្រាប់ %04d-%02d និងចាក់សោវត្តមានរហូតដល់ %s មែនទេ? "

                        + "វត្តមានក្រោយថ្ងៃកំណត់នៅតែអាចកែប្រែបាន។")

                                .formatted(period.year(), period.month(), cutoffDate,

                                        period.year(), period.month(), cutoffDate));

        confirm.setConfirmText("Approve & Lock | អនុម័ត និងចាក់សោ");

        confirm.addConfirmListener(e -> executeAttendanceAction(

                () -> service.approveAttendance(period.id(), null),

                "Attendance snapshot saved. Attendance through the cutoff date is locked. "

                        + "| បានរក្សាទុកទិន្នន័យវត្តមាន។ វត្តមានរហូតដល់ថ្ងៃកំណត់ត្រូវបានចាក់សោ។"));

        confirm.open();

    }

    private void confirmReopenAttendance() {

        PeriodRow period = periodFilter.getValue();

        if (period == null) {

            error("Select a payroll period first. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        PayrollConfirmDialog confirm = confirm("Reopen Attendance | បើកវត្តមានឡើងវិញ",

                "Reopen attendance for correction? Payroll generation and calculation will be blocked until it is checked and approved again. "

                        + "| បើកវត្តមានឡើងវិញដើម្បីកែតម្រូវមែនទេ? ការបង្កើត និងគណនាប្រាក់បៀវត្សនឹងត្រូវបានរារាំង "

                        + "រហូតដល់វត្តមានត្រូវបានពិនិត្យ និងអនុម័តម្ដងទៀត។");

        confirm.setConfirmText("Reopen | បើកឡើងវិញ");

        confirm.addConfirmListener(e -> executeAttendanceAction(

                () -> service.reopenAttendance(period.id(),

                        "Reopened for attendance correction | បើកឡើងវិញដើម្បីកែតម្រូវវត្តមាន"),

                "Attendance reopened for correction. | បានបើកវត្តមានឡើងវិញដើម្បីកែតម្រូវ។"));

        confirm.open();

    }

    private void executeAttendanceAction(Runnable action, String successMessage) {

        ProgressDialog.runAsync(
                "Attendance Processing | ដំណើរការវត្តមាន",
                "Please wait while attendance is processed... | សូមរង់ចាំ ខណៈពេលប្រព័ន្ធកំពុងដំណើរការវត្តមាន...",
                action,
                () -> {
                    refreshAttendanceControl();
                    updateProcessingActions();
                    success(successMessage);
                },
                ex -> error(message(ex)));

    }


    private void openCreateRunDialog() {

        PeriodRow period = periodFilter.getValue();

        if (period == null) {

            error("Select a payroll period first. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        List<String> allowedTypes;

        try {

            allowedTypes = service.findAllowedRunTypes(period.id());

        } catch (Exception ex) {

            error(message(ex));

            return;

        }

        if (allowedTypes.isEmpty()) {

            error("No additional payroll run type is available for this period. "

                    + "| មិនមានប្រភេទដំណើរការប្រាក់បៀវត្សបន្ថែមសម្រាប់រយៈពេលនេះទេ។");

            return;

        }

        CustomDialog dialog = editorDialog("New Payroll Run | បង្កើតដំណើរការថ្មី");

        Select<String> type = new Select<>();

        type.setLabel("Run Type | ប្រភេទ");

        type.setItems(allowedTypes);

        type.setItemLabelGenerator(PayrollViewSupport::runTypeLabel);

        type.setValue(allowedTypes.contains(PayrollRunType.REGULAR.code()) ? PayrollRunType.REGULAR.code() : allowedTypes.getFirst());

        Span behavior = new Span(runTypeDescription(type.getValue()));

        behavior.addClassName("payroll-help-text");

        type.addValueChangeListener(event -> behavior.setText(runTypeDescription(event.getValue())));

        TextArea notes = new TextArea("Notes | កំណត់សម្គាល់");

        notes.setWidthFull();

        notes.setMinHeight("100px");

        dialog.add(new VerticalLayout(type, behavior, notes));

        Button create = primaryButton("Create Run", VaadinIcon.PLUS);

        //Button cancel = new Button("Cancel", e -> dialog.close());
        Button cancel = errorButton("Cancel", VaadinIcon.ESC) ;
        cancel.addClickListener(e -> dialog.close());

        create.addClickListener(e -> {

            try {

                Long runId = service.createRun(period.id(), type.getValue(), notes.getValue());

                dialog.close();

                refresh();

                loadRuns(periodFilter.getValue(), runId);

                refreshCoordinator.refreshPeriods();

                refreshCoordinator.refreshDashboard();

                success("Payroll run created. You can now generate employees. "

                        + "| បានបង្កើតដំណើរការប្រាក់បៀវត្ស។ ឥឡូវអ្នកអាចបង្កើតបញ្ជីបុគ្គលិកបាន។");

            } catch (Exception ex) {

                error(message(ex));

            }

        });

        dialog.getFooter().add(cancel, create);

        dialog.open();

    }

    private void confirmGenerate() {
        RunRow run = runFilter.getValue();
        if (run == null) {
            error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");
            return;
        }
        if (PayrollRunType.FINAL_PAYMENT.matches(run.runType())) {
            openRunEmployeeSelectionDialog(run);
            return;
        }

        boolean adjustment = PayrollRunType.ADJUSTMENT.matches(run.runType());
        PayrollConfirmDialog confirm = confirm(
                adjustment ? "Reconcile Attendance & Generate Adjustment" : "Generate Payroll Employees",

                adjustment
                        ? "Keep attendance through the payment date frozen from the original Regular payroll, "
                                + "then compare the latest actual attendance against the roster for dates after the payment date. "
                                + "Only financial differences will remain in this Adjustment. "
                                + "| រក្សាវត្តមានរហូតដល់ថ្ងៃបើកប្រាក់តាម Regular ដើម ហើយផ្ទៀងផ្ទាត់វត្តមានពិតក្រោយថ្ងៃបើកប្រាក់ជាមួយតារាងវេន។"
                        : "Use the approved attendance and import rostered employees, salary, bank, dependant, and leave data? "
                                + "Existing employees will not be duplicated. | ប្រើវត្តមានដែលបានអនុម័ត និងនាំចូលបុគ្គលិកក្នុងតារាងវេន "
                                + "ប្រាក់បៀវត្ស ធនាគារ អ្នកក្នុងបន្ទុក និងទិន្នន័យច្បាប់មែនទេ? បុគ្គលិកដែលមានស្រាប់នឹងមិនត្រូវបានបង្កើតស្ទួនទេ។");

        confirm.setConfirmText(adjustment ? "Reconcile & Generate" : "Generate");
        confirm.addConfirmListener(e -> {
            Long runId = run.id();

            ProgressDialog.runAsync(
                    adjustment ? "Reconcile Attendance | ផ្ទៀងផ្ទាត់វត្តមាន" : "Generate Payroll Employees | បង្កើតបុគ្គលិកប្រាក់បៀវត្ស",
                    adjustment
                            ? "Comparing frozen payroll attendance with final post-payment attendance... | កំពុងផ្ទៀងផ្ទាត់វត្តមានក្រោយថ្ងៃបើកប្រាក់..."
                            : "Loading approved attendance and employee payroll data... | កំពុងទាញវត្តមានដែលបានអនុម័ត និងទិន្នន័យប្រាក់បៀវត្សបុគ្គលិក...",
                    () -> service.generatePayrollEmployees(runId),
                    inserted -> {
                        reloadSelectedRun();
                        refreshCoordinator.refreshDashboard();
                        success(adjustment
                                ? inserted + " employee(s) with a financial difference generated. "
                                        + "Employees with no difference were excluded automatically. "
                                        + "| បានបង្កើតតែបុគ្គលិកដែលមានភាពខុសគ្នាផ្នែកហិរញ្ញវត្ថុចំនួន " + inserted + " នាក់។"
                                : inserted + " employee(s) added to payroll. | បានបន្ថែមបុគ្គលិកចំនួន "
                                        + inserted + " នាក់ទៅក្នុងប្រាក់បៀវត្ស។");
                    },
                    ex -> error(message(ex)));
        });

        confirm.open();

    }

    private void openRunEmployeeSelectionDialog(RunRow run) {

        List<PayrollCandidateRow> candidates;

        try {

            candidates = service.findPayrollCandidates(run.id());

        } catch (Exception ex) {

            error(message(ex));

            return;

        }

        if (candidates.isEmpty()) {

            error("No eligible employees remain for this " + runTypeLabel(run.runType()) + " run. "

                    + "| មិនមានបុគ្គលិកដែលមានសិទ្ធិនៅសល់សម្រាប់ដំណើរការប្រភេទនេះទេ។");

            return;

        }

        String title = PayrollRunType.ADJUSTMENT.matches(run.runType())

                ? "Select Employees for Adjustment | ជ្រើសរើសបុគ្គលិកសម្រាប់កែតម្រូវ"

                : "Select Departing Employees | ជ្រើសរើសបុគ្គលិកដែលចាកចេញ";

        CustomDialog dialog = editorDialog(title);

        dialog.setWidth("min(1000px, 96vw)");

        Paragraph explanation = new Paragraph(PayrollRunType.ADJUSTMENT.matches(run.runType())

                ? "Select only affected employees. Calculate will compare their current payroll with the approved regular run and create only the difference. "

                        + "| ជ្រើសរើសតែបុគ្គលិកដែលត្រូវកែតម្រូវ។ ការគណនានឹងប្រៀបធៀបជាមួយដំណើរការប្រចាំខែដែលបានអនុម័ត ហើយបង្កើតតែចំនួនខុសគ្នា។"

                : "Select departing employees. Basic salary and attendance will stop at the contract end date; add any other final benefits as manual items before Calculate. "

                        + "| ជ្រើសរើសបុគ្គលិកដែលចាកចេញ។ ប្រាក់បៀវត្សមូលដ្ឋាន និងវត្តមាននឹងគិតត្រឹមថ្ងៃបញ្ចប់កិច្ចសន្យា។");

        Grid<PayrollCandidateRow> candidateGrid = new Grid<>();

        configureGrid(candidateGrid);

        candidateGrid.setSelectionMode(Grid.SelectionMode.MULTI);

        candidateGrid.setItems(candidates);

        candidateGrid.setHeight("420px");

        candidateGrid.addColumn(PayrollCandidateRow::insuranceNo)

                .setHeader("Insurance Number | លេខធានារ៉ាប់រង").setAutoWidth(true).setFrozen(true);

        candidateGrid.addColumn(PayrollCandidateRow::nameEn)

                .setHeader("Employee Name (English) | ឈ្មោះជាអង់គ្លេស").setAutoWidth(true);

        candidateGrid.addColumn(PayrollCandidateRow::nameKh)

                .setHeader("Employee Name (Khmer) | ឈ្មោះជាខ្មែរ").setAutoWidth(true);

        if (PayrollRunType.ADJUSTMENT.matches(run.runType())) {

            candidateGrid.addColumn(candidate -> usd(candidate.referenceNetPay()))

                    .setHeader("Regular Net Pay | ប្រាក់សុទ្ធប្រចាំខែ")

                    .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);

        } else {

            candidateGrid.addColumn(candidate -> candidate.lastCareerTypeDate() == null

                    ? "-" : candidate.lastCareerTypeDate().toString())

                    .setHeader("Leaving Date | កាលបរិច្ឆេទចាកចេញ").setAutoWidth(true);

        }

        TextField candidateSearch = new TextField("Filter Employees | ស្វែងរកបុគ្គលិក");

        candidateSearch.setPlaceholder(

                "Insurance number, English/Khmer name, career date | លេខធានារ៉ាប់រង ឈ្មោះ ឬកាលបរិច្ឆេទអាជីព");

        candidateSearch.setPrefixComponent(VaadinIcon.SEARCH.create());

        candidateSearch.setClearButtonVisible(true);

        candidateSearch.setValueChangeMode(ValueChangeMode.EAGER);

        candidateSearch.setWidthFull();

        Set<Long> selectedCandidateIds = new java.util.LinkedHashSet<>();

        Set<Long> visibleCandidateIds = candidates.stream()

                .map(PayrollCandidateRow::empId)

                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        boolean[] restoringSelection = { false };

        Span selectedCount = new Span("0 employee(s) selected | បានជ្រើសរើស 0 នាក់");

        Span filteredCount = new Span(

                "Showing " + candidates.size() + " of " + candidates.size()

                        + " employee(s) | បង្ហាញ " + candidates.size() + " នាក់ក្នុងចំណោម "

                        + candidates.size() + " នាក់");

        Button generate = primaryButton("Generate Selected | បង្កើតអ្នកបានជ្រើស", VaadinIcon.USERS);

        generate.setEnabled(false);

        candidateGrid.addSelectionListener(event -> {

            if (restoringSelection[0]) {

                return;

            }

            selectedCandidateIds.removeAll(visibleCandidateIds);

            event.getAllSelectedItems().stream()

                    .map(PayrollCandidateRow::empId)

                    .forEach(selectedCandidateIds::add);

            int count = selectedCandidateIds.size();

            selectedCount.setText(count + " employee(s) selected | បានជ្រើសរើស " + count + " នាក់");

            generate.setEnabled(count > 0);

        });

        candidateSearch.addValueChangeListener(event -> {

            List<PayrollCandidateRow> filtered = filterPayrollCandidates(

                    candidates, event.getValue());

            visibleCandidateIds.clear();

            filtered.stream().map(PayrollCandidateRow::empId).forEach(visibleCandidateIds::add);

            restoringSelection[0] = true;

            candidateGrid.setItems(filtered);

            filtered.stream()

                    .filter(candidate -> selectedCandidateIds.contains(candidate.empId()))

                    .forEach(candidateGrid::select);

            restoringSelection[0] = false;

            filteredCount.setText(

                    "Showing " + filtered.size() + " of " + candidates.size()

                            + " employee(s) | បង្ហាញ " + filtered.size() + " នាក់ក្នុងចំណោម "

                            + candidates.size() + " នាក់");

        });

        Button cancel = new Button("Cancel", event -> dialog.close());

        generate.addClickListener(event -> {

            Set<Long> selectedIds = Set.copyOf(selectedCandidateIds);
            Long runId = run.id();

            ProgressDialog.runAsync(
                    "Generate Selected Employees | បង្កើតបុគ្គលិកដែលបានជ្រើស",
                    "Loading payroll data for the selected employees... | កំពុងទាញទិន្នន័យប្រាក់បៀវត្សសម្រាប់បុគ្គលិកដែលបានជ្រើស...",
                    () -> service.generatePayrollEmployees(runId, selectedIds),
                    inserted -> {
                        dialog.close();
                        reloadSelectedRun();
                        refreshCoordinator.refreshDashboard();
                        success(inserted + " selected employee(s) added to payroll. "
                                + "| បានបន្ថែមបុគ្គលិកដែលបានជ្រើសរើសចំនួន " + inserted + " នាក់។");
                    },
                    ex -> error(message(ex)));

        });

        HorizontalLayout selectionSummary = new HorizontalLayout(selectedCount, filteredCount);

        selectionSummary.setWidthFull();

        selectionSummary.setJustifyContentMode(JustifyContentMode.BETWEEN);

        selectionSummary.setWrap(true);

        VerticalLayout content = new VerticalLayout(

                explanation, candidateSearch, selectionSummary, candidateGrid);

        content.setPadding(false);

        content.setSizeFull();

        dialog.add(content);

        dialog.getFooter().add(cancel, generate);

        dialog.open();

    }

    private void advanceReviewWorkflow() {

        RunRow run = runFilter.getValue();

        if (run == null) {
            error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");
            return;
        }

        if (PayrollRunStatus.CALCULATED.matches(run.status())) {
            executeRunAction(
                    () -> service.moveRunTo(run.id(), PayrollRunStatus.PENDING_REVIEW.code()),
                    "Payroll sent for review. | បានផ្ញើប្រាក់បៀវត្សទៅពិនិត្យ។");
            return;
        }

        if (PayrollRunStatus.PENDING_REVIEW.matches(run.status())) {
            executeRunAction(
                    () -> service.moveRunTo(run.id(), PayrollRunStatus.REVIEWED.code()),
                    "Payroll review completed. | បានបញ្ចប់ការពិនិត្យប្រាក់បៀវត្ស។");
            return;
        }

        error("This payroll run is not ready for a review action. Current status: "
                + statusLabel(run.status())
                + " | ដំណើរការប្រាក់បៀវត្សនេះមិនទាន់អាចធ្វើសកម្មភាពពិនិត្យបានទេ។");
    }


    private void openReturnForCorrectionDialog() {

        RunRow run = runFilter.getValue();

        if (run == null) {

            error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        if (!Set.of(PayrollRunStatus.PENDING_REVIEW.code(), PayrollRunStatus.REVIEWED.code())
                .contains(run.status())) {

            error("Only a payroll run pending review or already reviewed can be returned for correction. "
                    + "| អាចបញ្ជូនត្រឡប់បានតែដំណើរការដែលរង់ចាំពិនិត្យ ឬបានពិនិត្យរួចប៉ុណ្ណោះ។");

            return;

        }

        List<EmployeePayrollRow> selectedEmployees = new ArrayList<>(employeeGrid.getSelectedItems());

        long invalidSelected = selectedEmployees.stream()
                .filter(employee -> !PayrollEmployeeStatus.INCLUDED.matches(employee.payrollStatus()))
                .count();

        if (invalidSelected > 0) {
            error("Only INCLUDED employees can be returned for correction. "
                    + "Clear ON_HOLD/EXCLUDED employees from the selection. "
                    + "| អាចបញ្ជូនត្រឡប់ទៅកែតម្រូវបានតែបុគ្គលិក INCLUDED ប៉ុណ្ណោះ។");
            return;
        }

        Set<Long> selectedEmployeeIds = selectedEmployees.stream()
                .map(EmployeePayrollRow::id)
                .collect(java.util.stream.Collectors.toSet());

        boolean returnAll = selectedEmployeeIds.isEmpty();

        CustomDialog dialog = editorDialog(
                "Return Payroll for Correction | បញ្ជូនប្រាក់បៀវត្សត្រឡប់ទៅកែតម្រូវ");

        dialog.setWidth("min(720px, 94vw)");

        Paragraph scope = new Paragraph();

        if (returnAll) {
            scope.setText(
                    "Scope: ALL INCLUDED employees. No employee is selected, so the whole included payroll will be returned. "
                            + "| វិសាលភាព៖ បុគ្គលិក INCLUDED ទាំងអស់។ មិនបានជ្រើសបុគ្គលិក ដូច្នេះបុគ្គលិកទាំងអស់នឹងត្រូវកែតម្រូវ។");
        } else {
            String selectedNames = selectedEmployees.stream()
                    .limit(8)
                    .map(employee -> employee.insuranceNo() + " · "
                            + (employee.nameEn() == null ? "" : employee.nameEn()))
                    .collect(java.util.stream.Collectors.joining(", "));
            if (selectedEmployees.size() > 8) {
                selectedNames += ", ...";
            }
            scope.setText(
                    "Scope: SELECTED employees only (" + selectedEmployees.size() + "): " + selectedNames
                            + " | វិសាលភាព៖ តែបុគ្គលិកដែលបានជ្រើសរើស (" + selectedEmployees.size() + ")។");
        }

        scope.getElement().getThemeList().add("badge contrast");

        Paragraph help = new Paragraph(
                "The run moves back to CALCULATED, but only returned employees are editable. "
                        + "If no employee was selected, all INCLUDED employees are returned. "
                        + "After correction, recalculate the returned employee(s), then Send for Review again. "
                        + "| ដំណើរការនឹងត្រឡប់ទៅ CALCULATED ប៉ុន្តែអាចកែបានតែបុគ្គលិកដែលត្រូវបានបញ្ជូនត្រឡប់។ "
                        + "បើមិនបានជ្រើសបុគ្គលិក នឹងកែតម្រូវបុគ្គលិក INCLUDED ទាំងអស់។");

        help.addClassName("payroll-dialog-help");

        TextArea reason = new TextArea("Reason / Notes * | មូលហេតុ / កំណត់សម្គាល់ *");

        reason.setPlaceholder(
                "Example: Employee 00123 overtime hours are incorrect. "
                        + "| ឧទាហរណ៍៖ ម៉ោងបន្ថែមរបស់បុគ្គលិក 00123 មិនត្រឹមត្រូវ។");

        reason.setWidthFull();

        reason.setMinHeight("130px");

        reason.setMaxLength(2000);

        VerticalLayout content = new VerticalLayout(scope, help, reason);

        content.setPadding(false);

        content.setSpacing(true);

        dialog.add(content);

        Button cancel = new Button("Cancel", e -> dialog.close());

        Button returnButton = new Button(
                returnAll
                        ? "Return All for Correction | បញ្ជូនទាំងអស់ទៅកែតម្រូវ"
                        : "Return Selected (" + selectedEmployees.size() + ") | បញ្ជូនអ្នកបានជ្រើស",
                VaadinIcon.REPLY.create());

        returnButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        returnButton.addClickListener(e -> {

            String correctionReason = reason.getValue() == null ? "" : reason.getValue().trim();

            if (correctionReason.isEmpty()) {

                error("Enter the reason for correction. | សូមបញ្ចូលមូលហេតុនៃការកែតម្រូវ។");

                reason.focus();

                return;

            }

            Long runId = run.id();

            ProgressDialog.runAsync(
                    "Return for Correction | បញ្ជូនត្រឡប់ទៅកែតម្រូវ",
                    "Updating the payroll correction scope... | កំពុងកំណត់វិសាលភាពកែតម្រូវប្រាក់បៀវត្ស...",
                    () -> service.returnRunForCorrection(
                            runId, selectedEmployeeIds, correctionReason),
                    returnedCount -> {
                        dialog.close();
                        reloadSelectedRun();
                        refreshCoordinator.refreshDashboard();
                        refreshCoordinator.refreshPeriods();

                        success(returnAll
                                ? returnedCount + " INCLUDED employee(s) returned for correction. "
                                        + "Recalculate returned employees, then send the run for review again. "
                                        + "| បានបញ្ជូនបុគ្គលិក INCLUDED ចំនួន " + returnedCount
                                        + " នាក់ទៅកែតម្រូវ។"
                                : returnedCount + " selected employee(s) returned for correction. "
                                        + "Other employees remain locked. Recalculate only the returned employee(s), "
                                        + "then send the run for review again. "
                                        + "| បានបញ្ជូនបុគ្គលិកដែលបានជ្រើសចំនួន " + returnedCount
                                        + " នាក់ទៅកែតម្រូវ ហើយបុគ្គលិកផ្សេងទៀតនៅតែចាក់សោ។");
                    },
                    ex -> error(message(ex)));

        });

        dialog.getFooter().add(cancel, returnButton);

        dialog.open();

        reason.focus();

    }


    private void executeRunAction(Runnable action, String successMessage) {

        ProgressDialog.runAsync(
                "Payroll Processing | ដំណើរការប្រាក់បៀវត្ស",
                "Please wait while payroll processing is completed... | សូមរង់ចាំ ខណៈពេលប្រព័ន្ធកំពុងដំណើរការប្រាក់បៀវត្ស...",
                action,
                () -> {
                    reloadSelectedRun();
                    refreshCoordinator.refreshDashboard();
                    refreshCoordinator.refreshPeriods();
                    success(successMessage);
                },
                ex -> error(message(ex)));

    }

    private void openEmployeeStatusDialog() {

        RunRow run = runFilter.getValue();

        List<EmployeePayrollRow> selectedEmployees = new ArrayList<>(employeeGrid.getSelectedItems());

        if (run == null) {

            error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        if (selectedEmployees.isEmpty()) {

            error("Select employee(s) first. | សូមជ្រើសរើសបុគ្គលិកជាមុនសិន។");

            return;

        }

        Set<Long> employeeIds = selectedEmployees.stream()

                .map(EmployeePayrollRow::id)

                .collect(java.util.stream.Collectors.toSet());

        Set<String> currentStatuses = selectedEmployees.stream()

                .map(EmployeePayrollRow::payrollStatus)

                .collect(java.util.stream.Collectors.toSet());

        CustomDialog dialog = editorDialog("Change Payroll Status | ប្តូរស្ថានភាពប្រាក់បៀវត្ស");

        dialog.setWidth("min(650px, 94vw)");

        Span selection = new Span(selectedEmployees.size()

                + " employee(s) selected | បានជ្រើសរើសបុគ្គលិក "

                + selectedEmployees.size() + " នាក់");

        selection.getElement().getThemeList().add("badge contrast");

        Select<String> status = new Select<>();

        status.setLabel("New Payroll Status | ស្ថានភាពប្រាក់បៀវត្សថ្មី");

        status.setItems(PayrollEmployeeStatus.codes());

        status.setItemLabelGenerator(PayrollViewSupport::statusLabel);

        status.setWidthFull();

        if (currentStatuses.size() == 1) {

            status.setValue(currentStatuses.iterator().next());

        }

        TextArea notes = new TextArea("Reason / Notes | មូលហេតុ / កំណត់សម្គាល់");

        notes.setPlaceholder("Optional status-change note | កំណត់សម្គាល់បន្ថែម (មិនចាំបាច់)");

        notes.setWidthFull();

        notes.setMinHeight("90px");

        Paragraph behavior = new Paragraph();

        behavior.addClassName("payroll-dialog-help");

        Runnable updateBehavior = () -> {
            if (status.getValue() == null || status.getValue().isBlank()) {
                behavior.setText("Choose the status to apply to every selected employee. "
                        + "| សូមជ្រើសរើសស្ថានភាពសម្រាប់បុគ្គលិកដែលបានជ្រើសទាំងអស់។");
                return;
            }
            PayrollEmployeeStatus selectedStatus = PayrollEmployeeStatus.from(status.getValue());
            if (run.correctionMode()) {
                behavior.setText(switch (selectedStatus) {
                    case INCLUDED -> "This returned employee stays INCLUDED. The current employee-master snapshot "
                            + "will be refreshed and payroll will be recalculated immediately; the correction is then resolved. "
                            + "| បុគ្គលិកដែលត្រឡប់មកកែតម្រូវនេះនៅតែ INCLUDED។ ប្រព័ន្ធនឹងទាញទិន្នន័យថ្មីពី Employee Master "
                            + "ហើយគណនាប្រាក់បៀវត្សឡើងវិញភ្លាមៗ បន្ទាប់មកបញ្ចប់ការកែតម្រូវ។";
                    case ON_HOLD -> "The returned employee will be placed ON HOLD, omitted from payroll totals, "
                            + "and this correction will be resolved without recalculation. Existing payroll items are preserved. "
                            + "| បុគ្គលិកនឹងត្រូវដាក់ ON HOLD មិនបញ្ចូលក្នុងចំនួនសរុបប្រាក់បៀវត្ស "
                            + "ហើយបញ្ចប់ការកែតម្រូវដោយមិនគណនាឡើងវិញ។";
                    case EXCLUDED -> "The returned employee will be EXCLUDED. Generated payroll items are cleared, "
                            + "calculated totals are reset to zero, and this correction will be resolved. "
                            + "| បុគ្គលិកនឹងត្រូវ EXCLUDED។ ធាតុដែលប្រព័ន្ធបានគណនានឹងត្រូវលុប "
                            + "ចំនួនសរុបកំណត់ជាសូន្យ ហើយបញ្ចប់ការកែតម្រូវ។";
                });
                return;
            }
            behavior.setText(switch (selectedStatus) {
                case INCLUDED -> "INCLUDED participates in payroll calculation. If this run is already calculated, "
                        + "the selected employees are recalculated now. | INCLUDED ចូលរួមក្នុងការគណនាប្រាក់បៀវត្ស។ "
                        + "បើដំណើរការនេះបានគណនារួច បុគ្គលិកដែលបានជ្រើសនឹងត្រូវគណនាឡើងវិញភ្លាម។";
                case ON_HOLD -> "ON HOLD preserves existing payroll items, but omits the employee from Calculate All "
                        + "and run totals. | ON HOLD រក្សាទុកធាតុប្រាក់បៀវត្សបច្ចុប្បន្ន ប៉ុន្តែមិនបញ្ចូលបុគ្គលិកក្នុង "
                        + "គណនាទាំងអស់ និងចំនួនសរុបនៃដំណើរការ។";
                case EXCLUDED -> "EXCLUDED deletes generated Basic Salary, attendance, leave, overtime, allowance, "
                        + "Tax and NSSF items, then resets calculated totals to zero. Attendance snapshots and manual "
                        + "items are retained. | EXCLUDED លុបធាតុដែលប្រព័ន្ធបានគណនា ហើយកំណត់ចំនួនសរុបជាសូន្យ។ "
                        + "ទិន្នន័យវត្តមាន និងធាតុបញ្ចូលដោយដៃនៅតែរក្សាទុក។";
            });
        };

        status.addValueChangeListener(e -> updateBehavior.run());

        updateBehavior.run();

        VerticalLayout content = new VerticalLayout(selection, status, behavior, notes);

        content.setPadding(false);

        content.setSpacing(true);

        dialog.add(content);

        Button cancel = new Button("Cancel", e -> dialog.close());

        Button save = primaryButton("Apply Status | អនុវត្តស្ថានភាព", VaadinIcon.CHECK);

        Runnable applyStatus = () -> {

            String targetStatus = status.getValue();
            String statusNotes = notes.getValue();
            Long runId = run.id();

            ProgressDialog.runAsync(
                    "Change Payroll Status | ប្តូរស្ថានភាពប្រាក់បៀវត្ស",
                    "Updating selected employee payroll status... | កំពុងកែប្រែស្ថានភាពប្រាក់បៀវត្សរបស់បុគ្គលិកដែលបានជ្រើស...",
                    () -> service.changePayrollEmployeeStatus(
                            runId, employeeIds, targetStatus, statusNotes),
                    updated -> {
                        dialog.close();
                        reloadSelectedRun();
                        refreshCoordinator.refreshDashboard();
                        refreshCoordinator.refreshPeriods();
                        success(updated + " employee status(es) updated successfully. "
                                + "| បានប្តូរស្ថានភាពបុគ្គលិកចំនួន " + updated + " នាក់ដោយជោគជ័យ។");
                    },
                    ex -> error(message(ex)));

        };

        save.addClickListener(e -> {

            if (status.getValue() == null) {

                error("Select the new payroll status. | សូមជ្រើសរើសស្ថានភាពប្រាក់បៀវត្សថ្មី។");

                return;

            }

            if (!PayrollEmployeeStatus.EXCLUDED.matches(status.getValue())) {

                applyStatus.run();

                return;

            }

            PayrollConfirmDialog confirm = confirm(

                    "Exclude Selected Employees | មិនរួមបញ្ចូលបុគ្គលិកដែលបានជ្រើស",

                    "Exclude the selected employee(s) and permanently clear their generated payroll items for this run? "

                            + "| មិនរួមបញ្ចូលបុគ្គលិកដែលបានជ្រើស និងលុបធាតុប្រាក់បៀវត្សដែលប្រព័ន្ធបានគណនា "

                            + "សម្រាប់ដំណើរការនេះមែនទេ?");

            confirm.setConfirmText("Exclude & Clear | មិនរួមបញ្ចូល និងលុប");

            confirm.addConfirmListener(event -> applyStatus.run());

            confirm.open();

        });

        dialog.getFooter().add(cancel, save);

        dialog.open();

    }

    private void calculateSelectedOrAll() {

        RunRow run = runFilter.getValue();
        if (run == null) {
            error("Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");
            return;
        }
        Set<Long> selectedEmployeeIds = employeeGrid.getSelectedItems().stream()
                .map(EmployeePayrollRow::id)
                .collect(java.util.stream.Collectors.toSet());
        long ineligibleSelectionCount = employeeGrid.getSelectedItems().stream()
                .filter(employee -> !PayrollEmployeeStatus.INCLUDED.matches(employee.payrollStatus()))
                .count();

        if (ineligibleSelectionCount > 0) {
            error("Only INCLUDED employees can be recalculated. Change payroll status first. "  + "| អាចគណនាឡើងវិញបានតែបុគ្គលិក INCLUDED ប៉ុណ្ណោះ។ "  + "សូមប្តូរស្ថានភាពប្រាក់បៀវត្សជាមុនសិន។");
            return;
        }

        int correctionCount = service.countCorrectionRequiredEmployees(run.id());
        boolean correctionMode = run.correctionMode();
        long lockedSelectionCount = correctionMode   ? employeeGrid.getSelectedItems().stream() .filter(employee -> !employee.correctionRequired()) .count() : 0;
        if (lockedSelectionCount > 0) {
            error("Your selection contains employee(s) that do not currently require correction. "  + "Only employees with Correction Required status can be refreshed and recalculated during this correction cycle. "+ "| ការជ្រើសរើសមានបុគ្គលិកដែលមិនត្រូវបានបញ្ជូនត្រឡប់មកកែតម្រូវ។");
            return;
        }

        if (selectedEmployeeIds.isEmpty()) {
            executeRunAction(
                    () -> service.recalculateRun(run.id()),
                    correctionMode
                            ? correctionCount + " returned employee(s) refreshed from Employee Master and recalculated successfully. "
                                    + "The other reviewed employees were not refreshed or recalculated. "
                                    + "| បានគណនាបុគ្គលិកដែលត្រូវកែតម្រូវចំនួន " + correctionCount
                                    + " នាក់ឡើងវិញដោយជោគជ័យ។"
                            : "Payroll calculated for all INCLUDED employees successfully. "
                                    + "| បានគណនាប្រាក់បៀវត្សសម្រាប់បុគ្គលិក INCLUDED ទាំងអស់ដោយជោគជ័យ។");

            return;

        }

        String draftNote = PayrollRunStatus.DRAFT.matches(run.status())

                ? PayrollRunType.ADJUSTMENT.matches(run.runType())

                        ? " If every INCLUDED adjustment employee was selected, the run is now CALCULATED; "

                                + "otherwise use Calculate All Differences. "

                                + "| ប្រសិនបើបានជ្រើសបុគ្គលិក INCLUDED ទាំងអស់ក្នុងការកែតម្រូវ "

                                + "ដំណើរការនឹងទៅជា CALCULATED។ បើមិនដូច្នោះទេ សូមចុច គណនាចំនួនខុសគ្នាទាំងអស់។"

                        : " The run remains DRAFT until Calculate All is completed. "

                                + "| ដំណើរការនៅតែជា ព្រាង រហូតដល់ការគណនាទាំងអស់បានបញ្ចប់។"

                : "";

        executeRunAction(

                () -> service.recalculateEmployees(run.id(), selectedEmployeeIds),

                selectedEmployeeIds.size() + (correctionMode
                        ? " returned employee(s) refreshed from Employee Master and recalculated successfully. "
                                + "Non-returned employees were left unchanged. "
                                + "| បានគណនាបុគ្គលិកដែលត្រូវកែតម្រូវចំនួន "
                                + selectedEmployeeIds.size() + " នាក់ឡើងវិញ។"
                        : " selected employee(s) recalculated successfully. "
                                + "| បានគណនាបុគ្គលិកដែលបានជ្រើសរើសចំនួន "
                                + selectedEmployeeIds.size() + " នាក់ឡើងវិញដោយជោគជ័យ។")
                        + draftNote);

    }

    private void updateCalculateButtonLabel() {

        int selected = employeeGrid.getSelectedItems().size();

        RunRow run = runFilter.getValue();

        int correctionCount = run == null ? 0 : currentCorrectionCount;

        if (run != null && run.correctionMode()) {

            if (selected > 0) {
                calculateButton.setText("Refresh & Recalculate Returned Selected (" + selected + ")");
                calculateButton.setTooltipText("Refresh selected returned employees from Employee Master, then recalculate them only " + "| ធ្វើបច្ចុប្បន្នភាពពី Employee Master ហើយគណនាឡើងវិញតែបុគ្គលិកដែលបានបញ្ជូនត្រឡប់មកកែតម្រូវ");
            } else if (correctionCount > 0) {
                calculateButton.setText("Refresh & Recalculate All Returned (" + correctionCount + ")");
                calculateButton.setTooltipText("No employee selected: refresh all returned employees from Employee Master, then recalculate them only "   + "| មិនបានជ្រើសបុគ្គលិក៖ ធ្វើបច្ចុប្បន្នភាពពី Employee Master ហើយគណនាឡើងវិញតែបុគ្គលិកដែលត្រូវកែតម្រូវទាំងអស់");
            } else {
                calculateButton.setText("Returned Employees Corrected");
                calculateButton.setTooltipText("All returned employees are recalculated. Send the payroll for review again. " + "| បុគ្គលិកដែលត្រូវកែតម្រូវបានគណនាឡើងវិញរួច។ សូមផ្ញើទៅពិនិត្យម្តងទៀត។");
            }

        } else if (selected > 0) {
            calculateButton.setText("Recalculate Selected (" + selected + ")");
            calculateButton.setTooltipText( "Recalculate only the selected payroll employee(s) | គណនាឡើងវិញតែបុគ្គលិកប្រាក់បៀវត្សដែលបានជ្រើសរើស");

        } else if (run != null && PayrollRunType.ADJUSTMENT.matches(run.runType())) {
            calculateButton.setText("Calculate All Differences");
            calculateButton.setTooltipText("Compare every affected employee with the approved regular run and calculate only the differences " + "| ប្រៀបធៀបបុគ្គលិកដែលត្រូវកែតម្រូវជាមួយដំណើរការប្រចាំខែ ហើយគណនាតែចំនួនខុសគ្នា");

        } else if (run != null && PayrollRunType.FINAL_PAYMENT.matches(run.runType())) {
            calculateButton.setText("Calculate All Final Payments");
            calculateButton.setTooltipText("Calculate final pay for all selected departing employees " + "| គណនាប្រាក់ចុងក្រោយសម្រាប់បុគ្គលិកដែលចាកចេញទាំងអស់");

        } else {

            calculateButton.setText("Calculate All");
            calculateButton.setTooltipText("No employee selected: calculate the full payroll run | មិនបានជ្រើសរើសបុគ្គលិក៖ គណនាដំណើរការប្រាក់បៀវត្សទាំងមូល");

        }

    }

    private void updateGenerateButtonLabel() {

        RunRow run = runFilter.getValue();

        if (run == null || PayrollRunType.REGULAR.matches(run.runType())) {
            generateButton.setText("Generate All Employees");
            generateButton.setTooltipText("Generate every rostered employee for the regular monthly payroll " + "| បង្កើតបុគ្គលិកក្នុងតារាងវេនទាំងអស់សម្រាប់ប្រាក់បៀវត្សប្រចាំខែ");
        } else if (PayrollRunType.ADJUSTMENT.matches(run.runType())) {
            generateButton.setText("Select Affected Employees");
            generateButton.setTooltipText("Choose only employees whose approved regular payroll must be corrected "  + "| ជ្រើសតែបុគ្គលិកដែលត្រូវកែតម្រូវប្រាក់បៀវត្សដែលបានអនុម័ត");
        } else {
            generateButton.setText("Select Departing Employees");
            generateButton.setTooltipText("Choose employees whose contract ends during this payroll period "  + "| ជ្រើសបុគ្គលិកដែលកិច្ចសន្យាបញ្ចប់ក្នុងរយៈពេលប្រាក់បៀវត្សនេះ");

        }

    }

    private void reloadSelectedRun() {

        PeriodRow period = periodFilter.getValue();

        Long runId = selectedRunId();

        if (period != null) {

            loadRuns(period, runId);

        }

    }

    private void openEmployeeDetails(EmployeePayrollRow employee) {

        RunRow run = runFilter.getValue();

        boolean editable = run != null

                && Set.of(PayrollRunStatus.DRAFT.code(), PayrollRunStatus.CALCULATED.code()).contains(run.status())

                && PayrollEmployeeStatus.INCLUDED.matches(employee.payrollStatus())

                && (!run.correctionMode() || employee.correctionRequired());

        CustomDialog dialog = editorDialog("Payroll Details | ព័ត៌មានលម្អិតប្រាក់បៀវត្ស · "

                + employee.insuranceNo() + " · " + employee.nameEn());

        dialog.setWidth("min(1200px, 96vw)");

        dialog.setHeight("min(820px, 94vh)");

        Div content = new Div();

        content.setSizeFull();

        content.addClassName("payroll-detail-content");

        // Payroll Details now focuses only on Earnings & Deductions.
        // Summary and Attendance & Leave remain available elsewhere in the
        // Payroll workflow and are intentionally not shown in this dialog.
        content.add(employeeItems(employee, editable));

        VerticalLayout body = new VerticalLayout(content);

        body.setSizeFull();

        body.setPadding(false);

        body.setFlexGrow(1, content);

        dialog.add(body);

        Button close = new Button("Close | បិទ", e -> {

            dialog.close();

            reloadSelectedRun();

        });

        dialog.getFooter().add(close);

        dialog.open();

    }

    private Component employeeSummary(EmployeePayrollRow e) {

        FormLayout layout = new FormLayout();

        layout.add(

                readOnly("Insurance Number | លេខធានារ៉ាប់រង", Integer.toString(e.insuranceNo())),

                readOnly("Employee Name (English) | ឈ្មោះបុគ្គលិក (អង់គ្លេស)", e.nameEn()),

                readOnly("Employee Name (Khmer) | ឈ្មោះបុគ្គលិក (ខ្មែរ)", e.nameKh()),

                readOnly("Gender | ភេទ", e.gender()),

                readOnly("Bank Name | ឈ្មោះធនាគារ", e.bankName()),

                readOnly("Bank Account | គណនីធនាគារ", e.bankAccount()),

                readOnly("Spouse | សហព័ទ្ធ", Integer.toString(e.spouseCount())),

                readOnly("Tax-Eligible Dependants | អ្នកក្នុងបន្ទុកដែលអាចកាត់បន្ថយពន្ធ",

                        Integer.toString(e.taxDependentCount())),

                readOnly("Basic Salary | ប្រាក់បៀវត្សមូលដ្ឋាន", usd(e.basicSalary())),

                readOnly("Gross Pay | ប្រាក់សរុប", usd(e.totalEarnings())),

                readOnly("Total Deductions | ការកាត់សរុប", usd(e.totalDeductions())),

                readOnly("Salary Tax | ពន្ធលើប្រាក់បៀវត្ស", usd(e.salaryTax())),

                readOnly("Employee Contribution | ភាគទាននិយោជិត", usd(e.employeeContribution())),

                readOnly("Employer Contribution | ភាគទាននិយោជក", usd(e.employerContribution())),

                readOnly("Net Pay | ប្រាក់សុទ្ធ", usd(e.netSalary())),

                readOnly("Payroll Status | ស្ថានភាពប្រាក់បៀវត្ស", statusLabel(e.payrollStatus())));

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),

                new FormLayout.ResponsiveStep("650px", 2), new FormLayout.ResponsiveStep("950px", 3));

        return layout;

    }

    private Component attendanceSummary(Long payrollEmployeeId) {

        Optional<AttendanceRow> result = service.findAttendance(payrollEmployeeId);

        if (result.isEmpty()) {
            return new Paragraph("No attendance snapshot is available for this employee. "  + "| មិនមានទិន្នន័យវត្តមានសម្រាប់បុគ្គលិកនេះទេ។");
        }

        AttendanceRow a = result.get();

        FormLayout layout = new FormLayout();

        layout.add(

                readOnly("Source Period | រយៈពេលប្រភព",

                        a.sourceStartDate() + " to | ដល់ " + a.sourceEndDate()),

                readOnly("Scheduled Days | ថ្ងៃតាមកាលវិភាគ", decimal(a.scheduledDays())),

                readOnly("Worked Days | ថ្ងៃធ្វើការ", decimal(a.workedDays())),

                readOnly("Holiday Days | ថ្ងៃឈប់សម្រាក", decimal(a.holidayDays())),

                readOnly("Absent Days | ថ្ងៃអវត្តមាន", decimal(a.absentDays())),

                readOnly("Annual Leave Days | ថ្ងៃច្បាប់ប្រចាំឆ្នាំ", decimal(a.annualLeaveDays())),

                readOnly("Special Leave Days | ថ្ងៃច្បាប់ពិសេស", decimal(a.specialLeaveDays())),

                readOnly("Sick Leave Days | ថ្ងៃច្បាប់ឈឺ", decimal(a.sickLeaveDays())),

                readOnly("Maternity Leave Days | ថ្ងៃច្បាប់មាតុភាព", decimal(a.maternityLeaveDays())),

                readOnly("Other Paid Leave | ច្បាប់មានប្រាក់ផ្សេងៗ", decimal(a.otherPaidLeaveDays())),

                readOnly("Unpaid Leave | ច្បាប់គ្មានប្រាក់", decimal(a.unpaidLeaveDays())),

                readOnly("Overtime Hours | ម៉ោងបន្ថែម", decimal(a.overtimeHours())),

                readOnly("AL Remaining | ច្បាប់ប្រចាំឆ្នាំនៅសល់", decimal(a.annualLeaveRemaining())),

                readOnly("Overused AL | ច្បាប់ប្រចាំឆ្នាំប្រើលើស", decimal(a.overusedAnnualLeave())),

                readOnly("Special Leave Remaining | ច្បាប់ពិសេសនៅសល់", decimal(a.specialLeaveRemaining())),

                readOnly("Overused Special Leave | ច្បាប់ពិសេសប្រើលើស",

                        decimal(a.overusedSpecialLeave())));

        Map<String, BigDecimal> policyQuantities = service.findAttendancePolicyQuantities(payrollEmployeeId);

        for (String code : List.of("OT", "ADP", "ATC", "STC", "ASO", "CHS", "CLT", "HST", "STR", "SUC")) {

            layout.add(readOnly( code + " Attendance Qty | បរិមាណវត្តមាន", decimal(policyQuantities.getOrDefault(code, BigDecimal.ZERO))));

        }

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("650px", 2), new FormLayout.ResponsiveStep("950px", 3));

        return layout;

    }

    private Component employeeItems(EmployeePayrollRow employee, boolean editable) {

        VerticalLayout page = new VerticalLayout();

        page.setSizeFull();

        page.setPadding(false);

        Grid<ItemRow> grid = new Grid<>();

        configureGrid(grid);

        grid.getStyle().remove("height");

        grid.setAllRowsVisible(true);

        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(ItemRow::componentCode).setHeader("Code | កូដ").setAutoWidth(true);

        grid.addColumn(ItemRow::componentName).setHeader("Component | សមាសភាគ").setAutoWidth(true);

        grid.addComponentColumn(i -> typeBadge(i.componentType()))

                .setHeader("Type | ប្រភេទ").setAutoWidth(true);

        grid.addColumn(ItemRow::description).setHeader("Description | បរិយាយ").setAutoWidth(true);

        grid.addColumn(ItemRow::quantity).setHeader("Quantity | បរិមាណ").setAutoWidth(true);

        grid.addColumn(ItemRow::rate).setHeader("Rate | អត្រា").setAutoWidth(true);

        moneyColumn(grid, "Amount | ចំនួនទឹកប្រាក់", ItemRow::payrollAmount);

        grid.addColumn(i -> sourceTypeLabel(i.sourceType()))

                .setHeader("Source | ប្រភព").setAutoWidth(true);

        Runnable refresh = () -> grid.setItems(service.findItems(employee.id()));

        Button add = primaryButton("Add Item | បន្ថែមធាតុ", VaadinIcon.PLUS);

        Button edit = actionButton("Edit Item | កែប្រែធាតុ", VaadinIcon.EDIT);

        Button delete = actionButton("Delete Item | លុបធាតុ", VaadinIcon.TRASH);

        boolean canInsert = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.MANUAL_ITEM, AccessPageType.INSERTED_PAGE);

        boolean canUpdate = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.MANUAL_ITEM, AccessPageType.UPDATED_PAGE);

        boolean canDelete = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.MANUAL_ITEM, AccessPageType.DELETED_PAGE);

        add.setEnabled(editable && canInsert);

        edit.setEnabled(false);

        delete.setEnabled(false);

        grid.addSelectionListener(event -> {

            ItemRow selected = event.getFirstSelectedItem().orElse(null);

            boolean manualItem = selected != null && !isSystemCalculatedItem(selected);

            edit.setEnabled(editable && canUpdate && manualItem);

            delete.setEnabled(editable && canDelete && manualItem);

        });

        add.addClickListener(e -> openItemDialog(employee, null, refresh));

        edit.addClickListener(e -> grid.getSelectedItems().stream().findFirst().ifPresentOrElse(

                item -> openItemDialog(employee, item, refresh),

                () -> error("Select one payroll item to edit. | សូមជ្រើសរើសធាតុប្រាក់បៀវត្សមួយដើម្បីកែប្រែ។")));

        delete.addClickListener(e -> grid.getSelectedItems().stream().findFirst().ifPresentOrElse(item -> {

            PayrollConfirmDialog confirm = confirm("Delete Payroll Item | លុបធាតុប្រាក់បៀវត្ស",

                    "Delete " + item.componentName() + "? | លុប " + item.componentName() + " មែនទេ?");

            confirm.addConfirmListener(event -> {

                try {

                    service.deleteItem(item);

                    refresh.run();

                    success("Payroll item deleted. | បានលុបធាតុប្រាក់បៀវត្ស។");

                } catch (Exception ex) {

                    error(message(ex));

                }

            });

            confirm.open();

        }, () -> error("Select one payroll item to delete. | សូមជ្រើសរើសធាតុប្រាក់បៀវត្សមួយដើម្បីលុប។")));

        page.add(toolbar(add, edit, delete), grid);

        page.setFlexGrow(1, grid);

        refresh.run();

        return page;

    }

    private void openItemDialog(EmployeePayrollRow employee, ItemRow current, Runnable afterSave) {

        CustomDialog dialog = editorDialog(current == null

                ? "Add Payroll Item | បន្ថែមធាតុប្រាក់បៀវត្ស"

                : "Edit Payroll Item | កែប្រែធាតុប្រាក់បៀវត្ស");

        ComboBox<ComponentRow> component = new ComboBox<>("Payroll Component | ធាតុប្រាក់ខែ");

        List<ComponentRow> manualComponents = service.findManualEntryComponents();

        component.setItems(manualComponents);

        component.setItemLabelGenerator(ComponentRow::displayName);

        component.setWidthFull();

        TextField description = new TextField("Description | បរិយាយ");

        BigDecimalField quantity = new BigDecimalField("Quantity | បរិមាណ");

        BigDecimalField rate = new BigDecimalField("Rate | អត្រា");

        BigDecimalField amount = new BigDecimalField("Amount | ចំនួនទឹកប្រាក់");

        ComboBox<String> currency = new ComboBox<>("Currency | រូបិយប័ណ្ណ");
        currency.setItems("USD", "KHR");
        currency.setAllowCustomValue(false);
        currency.setWidthFull();

        BigDecimalField exchange = new BigDecimalField("Exchange Rate | អត្រាប្តូរ");
        exchange.setReadOnly(true);
        exchange.setWidthFull();

        Select<String> source = new Select<>();

        source.setLabel("Source | ប្រភព");

        source.setItems(SOURCE_TYPES);

        source.setItemLabelGenerator(PayrollViewSupport::sourceTypeLabel);

        TextArea remarks = new TextArea("Remarks | កំណត់សម្គាល់");

        quantity.setValue(BigDecimal.ONE);

        PeriodRow payrollPeriod = periodFilter.getValue();
        String payrollCurrency = payrollPeriod == null || payrollPeriod.currency() == null
                || payrollPeriod.currency().isBlank()
                        ? "USD"
                        : payrollPeriod.currency().trim().toUpperCase();
        currency.setValue(payrollCurrency);
        exchange.setValue(BigDecimal.ONE);

        currency.addValueChangeListener(event ->
                exchange.setValue(exchangeRateForCurrency(
                        event.getValue(), payrollCurrency, payrollPeriod)));

        source.setValue("MANUAL");

        source.setEnabled(false);

        component.addValueChangeListener(event -> {
            if (current == null && event.getValue() != null
                    && (description.getValue() == null || description.getValue().isBlank())) {
                description.setValue(event.getValue().nameEn());
            }
        });

        if (current != null) {

            manualComponents.stream().filter(c -> c.id().equals(current.componentId()))

                    .findFirst().ifPresent(component::setValue);

            description.setValue(nvl(current.description()));

            quantity.setValue(current.quantity());

            if (current.rate() != null) {

                rate.setValue(current.rate());

            }

            amount.setValue(current.amount());

            currency.setValue(current.currency());

            exchange.setValue(current.exchangeRate() == null ? BigDecimal.ONE : current.exchangeRate());

            source.setValue(current.sourceType());

            remarks.setValue(nvl(current.remarks()));

        }

        FormLayout form = form(component, description, quantity, rate, amount, currency, exchange, source, remarks);

        form.setColspan(remarks, 2);

        dialog.add(form);

        Button save = primaryButton("Save", VaadinIcon.CHECK);

        Button cancel = new Button("Cancel", e -> dialog.close());

        save.addClickListener(e -> {

            try {

                if (component.getValue() == null) {

                    throw new IllegalArgumentException(

                            "Payroll component is required. | ត្រូវជ្រើសរើសសមាសភាគប្រាក់បៀវត្ស។");

                }

                service.saveItem(new ItemInput(

                        current == null ? null : current.id(), employee.id(), component.getValue().id(),

                        description.getValue(), quantity.getValue(), rate.getValue(), amount.getValue(),

                        currency.getValue() == null ? payrollCurrency : currency.getValue().trim().toUpperCase(),
                        exchange.getValue(),

                        source.getValue(), remarks.getValue()));

                dialog.close();

                afterSave.run();

                success("Payroll item saved successfully. | បានរក្សាទុកធាតុប្រាក់បៀវត្សដោយជោគជ័យ។");

            } catch (Exception ex) {

                error(message(ex));

            }

        });

        dialog.getFooter().add(cancel, save);

        dialog.open();

    }

    private BigDecimal exchangeRateForCurrency(String itemCurrency, String payrollCurrency, PeriodRow period) {
        String item = itemCurrency == null ? "" : itemCurrency.trim().toUpperCase();
        String payroll = payrollCurrency == null ? "" : payrollCurrency.trim().toUpperCase();

        if (item.isBlank() || payroll.isBlank() || item.equals(payroll)) {
            return BigDecimal.ONE;
        }

        BigDecimal usdToKhr = period == null ? null : period.usdToKhrRate();
        if (usdToKhr == null || usdToKhr.signum() <= 0) {
            return BigDecimal.ONE;
        }

        if ("USD".equals(item) && "KHR".equals(payroll)) {
            return usdToKhr;
        }
        if ("KHR".equals(item) && "USD".equals(payroll)) {
            return BigDecimal.ONE.divide(usdToKhr, 6, RoundingMode.HALF_UP);
        }

        return BigDecimal.ONE;
    }


    private Long selectedRunId() {

        RunRow run = runFilter.getValue();

        if (run == null) {

            throw new IllegalArgumentException(

                    "Select a payroll run first. | សូមជ្រើសរើសដំណើរការប្រាក់បៀវត្សជាមុនសិន។");

        }

        return run.id();

    }


    private Span correctionBadge(EmployeePayrollRow employee) {

        RunRow run = runFilter.getValue();
        if (run == null || !run.correctionMode()) {
            return new Span("-");
        }

        if (employee != null && employee.correctionRequired()) {
            Span badge = new Span("Correction Required");
            badge.getElement().getThemeList().add("badge error pill");
            if (employee.correctionReason() != null && !employee.correctionReason().isBlank()) {
                badge.getElement().setAttribute("title", employee.correctionReason());
            }
            return badge;
        }

        if (employee != null
                && employee.correctionReturnedAt() != null
                && employee.correctionResolvedAt() != null) {
            Span badge = new Span("Corrected / Ready");
            badge.getElement().getThemeList().add("badge success pill");
            return badge;
        }

        Span badge = new Span("Locked / Reviewed");
        badge.getElement().getThemeList().add("badge contrast pill");
        return badge;

    }

}
