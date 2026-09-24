package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.AttendanceControlRow;
import static org.halocambodia.data.PayrollModels.AttendanceRow;
import static org.halocambodia.data.PayrollModels.EmployeePaymentRow;
import static org.halocambodia.data.PayrollModels.EmployeePayrollRow;
import static org.halocambodia.data.PayrollModels.ItemRow;
import static org.halocambodia.data.PayrollModels.PaymentBatchRow;
import static org.halocambodia.data.PayrollModels.PayrollAuditEventRow;
import static org.halocambodia.data.PayrollModels.PeriodRow;
import static org.halocambodia.data.PayrollModels.RunRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollAttendanceDaySnapshot;
import org.halocambodia.data.PayrollSeniorityCalculation;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollAttendanceSnapshotService;
import org.halocambodia.services.PayrollPaymentService;
import org.halocambodia.services.PayrollSeniorityService;
import org.halocambodia.services.PayrollService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

/**
 * Read-only payroll audit/data route.
 *
 * This screen intentionally uses the same current payroll services and row models
 * as the operational Payroll route. It does not maintain a second payroll
 * calculation implementation.
 */
@Route(value = "payroll-data", layout = MainLayout.class)
@PageTitle("Payroll Data | ទិន្នន័យប្រាក់បៀវត្ស")
@PermitAll
public class PayrollDataAdministrationView extends VerticalLayout implements BeforeEnterObserver {

    private final PayrollService payrollService;
    private final PayrollPaymentService paymentService;
    private final PayrollSeniorityService seniorityService;
    private final PayrollAttendanceSnapshotService attendanceSnapshotService;
    private final AuthenticatedUser authenticatedUser;

    private final ComboBox<PeriodRow> periodFilter =
            new ComboBox<>("Payroll Period | រយៈពេល");
    private final ComboBox<RunRow> runFilter =
            new ComboBox<>("Payroll Run | ដំណើរការ");

    private final Div periodInfoCard = new Div();
    private final Div runInfoCard = new Div();
    private final Div attendanceControlCard = new Div();
    private final Div attendanceSummaryCard = new Div();

    private final PayrollAdminGridSection<EmployeePayrollRow> employeeSection =
            new PayrollAdminGridSection<>(
                    "Payroll Employees | បុគ្គលិកក្នុងបញ្ជីប្រាក់បៀវត្ស",
                    "Monthly salary snapshot and calculated payroll totals. Read-only. "
                            + "| ប្រាក់ខែប្រចាំខែ និងលទ្ធផលគណនាប្រាក់បៀវត្ស។ សម្រាប់មើលតែប៉ុណ្ណោះ។",
                    EmployeePayrollRow.class,
                    false);

    private final PayrollAdminGridSection<ItemRow> itemSection =
            new PayrollAdminGridSection<>(
                    "Earnings & Deductions | ប្រាក់ចំណូល និងការកាត់កង",
                    "Select an employee to inspect every calculated component, source amount, exchange rate and payroll amount. "
                            + "| ជ្រើសបុគ្គលិកដើម្បីពិនិត្យធាតុគណនា ប្រភព អត្រាប្តូរប្រាក់ និងចំនួនបើកប្រាក់។",
                    ItemRow.class,
                    false);

    private final ComboBox<EmployeePayrollRow> attendanceEmployee =
            new ComboBox<>("Employee | បុគ្គលិក");
    private final PayrollAdminGridSection<PayrollAttendanceDaySnapshot> attendanceDaySection =
            new PayrollAdminGridSection<>(
                    "Attendance Day Snapshot | ទិន្នន័យវត្តមានប្រចាំថ្ងៃ",
                    "Frozen attendance data used by the selected payroll run. "
                            + "| ទិន្នន័យវត្តមានដែលបានរក្សាទុកសម្រាប់ដំណើរការប្រាក់បៀវត្សដែលបានជ្រើស។",
                    PayrollAttendanceDaySnapshot.class,
                    false);

    private final ComboBox<EmployeePayrollRow> seniorityEmployee =
            new ComboBox<>("Employee (optional) | បុគ្គលិក (ស្រេចចិត្ត)");
    private final PayrollAdminGridSection<PayrollSeniorityCalculation> senioritySection =
            new PayrollAdminGridSection<>(
                    "Seniority Calculation | ការគណនាប្រាក់បំណាច់អតីតភាព",
                    "Auditable seniority calculations generated for the selected payroll run. "
                            + "| លទ្ធផលគណនាអតីតភាពដែលអាចពិនិត្យប្រវត្តិបាន។",
                    PayrollSeniorityCalculation.class,
                    false);

    private final PayrollAdminGridSection<PaymentBatchRow> paymentBatchSection =
            new PayrollAdminGridSection<>(
                    "Payment Batches | កញ្ចប់បើកប្រាក់",
                    "First installment, final settlement and adjustment settlement batches for the selected period. "
                            + "| កញ្ចប់លើកទីមួយ ការទូទាត់ចុងក្រោយ និងការទូទាត់កែតម្រូវ។",
                    PaymentBatchRow.class,
                    false);
    private final PayrollAdminGridSection<EmployeePaymentRow> paymentDetailSection =
            new PayrollAdminGridSection<>(
                    "Employee Payments | ការទូទាត់បុគ្គលិក",
                    "Select a payment batch to review paid amount, previous payment and carry-forward recovery. "
                            + "| ជ្រើសកញ្ចប់បើកប្រាក់ដើម្បីពិនិត្យចំនួនបើកមុន ចំនួនបើក និងចំនួនកាត់សងបន្ត។",
                    EmployeePaymentRow.class,
                    false);

    private final PayrollAdminGridSection<PayrollAuditEventRow> runAuditSection =
            new PayrollAdminGridSection<>(
                    "Payroll Run Audit History | ប្រវត្តិដំណើរការប្រាក់បៀវត្ស",
                    "Calculation, correction, review, approval and paid workflow events. "
                            + "| ប្រវត្តិគណនា កែតម្រូវ ពិនិត្យ អនុម័ត និងបើកប្រាក់។",
                    PayrollAuditEventRow.class,
                    false);
    private final ComboBox<PaymentBatchRow> auditPaymentBatch =
            new ComboBox<>("Payment Batch | កញ្ចប់បើកប្រាក់");
    private final PayrollAdminGridSection<PayrollAuditEventRow> paymentAuditSection =
            new PayrollAdminGridSection<>(
                    "Payment Audit History | ប្រវត្តិការទូទាត់",
                    "Select a payment batch to review export, approval and payment-confirmation events. "
                            + "| ជ្រើសកញ្ចប់បើកប្រាក់ដើម្បីមើលប្រវត្តិនាំចេញ អនុម័ត និងបញ្ជាក់ការទូទាត់។",
                    PayrollAuditEventRow.class,
                    false);

    public PayrollDataAdministrationView(
            PayrollService payrollService,
            PayrollPaymentService paymentService,
            PayrollSeniorityService seniorityService,
            PayrollAttendanceSnapshotService attendanceSnapshotService,
            AuthenticatedUser authenticatedUser) {
        this.payrollService = payrollService;
        this.paymentService = paymentService;
        this.seniorityService = seniorityService;
        this.attendanceSnapshotService = attendanceSnapshotService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        configureContextFilters();
        configureResultGrids();
        configureAttendanceTab();
        configureSeniorityTab();
        configurePaymentsTab();
        configureAuditTab();

        styleInfoCard(periodInfoCard);
        styleInfoCard(runInfoCard);
        styleInfoCard(attendanceControlCard);
        styleInfoCard(attendanceSummaryCard);

        HorizontalLayout contextCards = new HorizontalLayout(periodInfoCard, runInfoCard);
        contextCards.setWidthFull();
        contextCards.setWrap(true);
        contextCards.setFlexGrow(1, periodInfoCard, runInfoCard);
        periodInfoCard.getStyle().set("min-width", "360px");
        runInfoCard.getStyle().set("min-width", "360px");

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.add(new Tab("Payroll Results | លទ្ធផល"), buildResultsTab());
        tabs.add(new Tab("Attendance Snapshot | វត្តមាន"), buildAttendanceTab());
        tabs.add(new Tab("Seniority | អតីតភាព"), buildSeniorityTab());
        tabs.add(new Tab("Payments | ការទូទាត់"), buildPaymentsTab());
        tabs.add(new Tab("Audit History | ប្រវត្តិ"), buildAuditTab());

        add(
                buildHeader(),
                filterCard(periodFilter, runFilter),
                contextCards,
                tabs);
        setFlexGrow(1, tabs);

        refreshReferenceData();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    private Component buildHeader() {
        H2 title = new H2("Payroll Data | ទិន្នន័យប្រាក់បៀវត្ស");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-text-color)");

        Paragraph subtitle = new Paragraph(
                "Read-only audit view using the same current payroll services as the Payroll route. "
                        + "Review calculated salary, items, attendance snapshots, seniority, payments and workflow history. "
                        + "| ទំព័រពិនិត្យទិន្នន័យសម្រាប់មើលតែប៉ុណ្ណោះ ដោយប្រើទិន្នន័យដូចគ្នានឹង Payroll។");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout header = new VerticalLayout(title, subtitle);
        header.setPadding(false);
        header.setSpacing(false);
        return header;
    }

    private void configureContextFilters() {
        periodFilter.setItemLabelGenerator(PeriodRow::displayName);
        periodFilter.setClearButtonVisible(true);
        periodFilter.setWidth("340px");

        runFilter.setItemLabelGenerator(RunRow::displayName);
        runFilter.setClearButtonVisible(true);
        runFilter.setWidth("340px");

        periodFilter.addValueChangeListener(event -> {
            PeriodRow period = event.getValue();
            refreshPeriodInfo(period);

            runFilter.clear();
            List<RunRow> runs = period == null
                    ? List.of()
                    : payrollService.findRuns(period.id());
            runFilter.setItems(runs);

            refreshAttendanceControl(period);
            refreshPaymentsForPeriod(period);
            refreshPaymentAuditBatchOptions(period);

            if (!runs.isEmpty()) {
                runFilter.setValue(runs.get(0));
            } else {
                refreshRunContext(null);
            }
        });

        runFilter.addValueChangeListener(event -> refreshRunContext(event.getValue()));
    }

    private Component buildResultsTab() {
        VerticalLayout layout = new VerticalLayout(employeeSection, itemSection);
        layout.setPadding(false);
        layout.setWidthFull();
        return layout;
    }

    private void configureResultGrids() {
        Grid<EmployeePayrollRow> employeeGrid = employeeSection.grid();
        addColumn(employeeGrid, EmployeePayrollRow::insuranceNo,
                "Insurance No. | លេខធានារ៉ាប់រង", "insurance").setFrozen(true);
        addColumn(employeeGrid, EmployeePayrollRow::nameEn,
                "Name (English) | ឈ្មោះអង់គ្លេស", "nameEn").setFrozen(true);
        addColumn(employeeGrid, EmployeePayrollRow::nameKh,
                "Name (Khmer) | ឈ្មោះខ្មែរ", "nameKh");
        addColumn(employeeGrid, EmployeePayrollRow::salaryCurrency,
                "Currency | រូបិយប័ណ្ណ", "currency");
        addColumn(employeeGrid, value -> money(value.basicSalary()),
                "Monthly Basic Salary | ប្រាក់ខែគោលប្រចាំខែ", "monthlyBasic");
        addColumn(employeeGrid, value -> money(value.calculatedBasicSalary()),
                "Calculated Basic Salary | ប្រាក់ខែគោលដែលបានគណនា", "calculatedBasic");
        addColumn(employeeGrid, value -> money(value.totalEarnings()),
                "Total Earnings | ប្រាក់ចំណូលសរុប", "earnings");
        addColumn(employeeGrid, value -> money(value.totalDeductions()),
                "Total Deductions | ការកាត់កងសរុប", "deductions");
        addColumn(employeeGrid, value -> money(value.salaryTax()),
                "Salary Tax | ពន្ធប្រាក់បៀវត្ស", "tax");
        addColumn(employeeGrid, value -> money(value.employeeContribution()),
                "Employee Contribution | ភាគទាននិយោជិត", "employeeContribution");
        addColumn(employeeGrid, value -> money(value.employerContribution()),
                "Employer Contribution | ភាគទាននិយោជក", "employerContribution");
        addColumn(employeeGrid, value -> money(value.netSalary()),
                "Net Salary | ប្រាក់ខែសុទ្ធ", "net");
        addColumn(employeeGrid, EmployeePayrollRow::payrollStatus,
                "Payroll Status | ស្ថានភាពប្រាក់បៀវត្ស", "status");
        addColumn(employeeGrid, value -> value.correctionRequired() ? "Yes | បាទ/ចាស" : "No | ទេ",
                "Correction Required | ត្រូវការកែតម្រូវ", "correction");
        addColumn(employeeGrid, EmployeePayrollRow::correctionReason,
                "Correction Reason | មូលហេតុកែតម្រូវ", "correctionReason");
        addColumn(employeeGrid, EmployeePayrollRow::remarks,
                "Remarks | កំណត់សម្គាល់", "remarks");

        employeeSection.withLoader(search -> payrollService.findEmployees(currentRunId(), search));
        employeeSection.setGridHeight("360px");
        employeeGrid.addSelectionListener(event -> itemSection.refresh());

        Grid<ItemRow> itemGrid = itemSection.grid();
        addColumn(itemGrid, ItemRow::componentCode,
                "Component Code | កូដធាតុ", "componentCode").setFrozen(true);
        addColumn(itemGrid, ItemRow::componentName,
                "Component | ធាតុ", "componentName");
        addColumn(itemGrid, ItemRow::componentType,
                "Type | ប្រភេទ", "componentType");
        addColumn(itemGrid, ItemRow::description,
                "Description | បរិយាយ", "description");
        addColumn(itemGrid, value -> number(value.quantity()),
                "Quantity | បរិមាណ", "quantity");
        addColumn(itemGrid, value -> money(value.rate()),
                "Rate | អត្រា", "rate");
        addColumn(itemGrid, value -> money(value.amount()),
                "Source Amount | ចំនួនដើម", "amount");
        addColumn(itemGrid, ItemRow::currency,
                "Currency | រូបិយប័ណ្ណ", "itemCurrency");
        addColumn(itemGrid, value -> number(value.exchangeRate()),
                "Exchange Rate | អត្រាប្តូរប្រាក់", "exchangeRate");
        addColumn(itemGrid, value -> money(value.payrollAmount()),
                "Payroll Amount | ចំនួនបើកប្រាក់", "payrollAmount");
        addColumn(itemGrid, ItemRow::sourceType,
                "Source | ប្រភព", "source");
        addColumn(itemGrid, ItemRow::remarks,
                "Calculation Detail | ព័ត៌មានគណនា", "itemRemarks");

        itemSection.withLoader(search -> employeeSection.selected()
                .map(employee -> filterItems(payrollService.findItems(employee.id()), search))
                .orElse(List.of()));
        itemSection.setAllRowsVisible(true);
    }

    private Component buildAttendanceTab() {
        HorizontalLayout filters = filterCard(attendanceEmployee);
        VerticalLayout layout = new VerticalLayout(
                filters,
                attendanceControlCard,
                attendanceSummaryCard,
                attendanceDaySection);
        layout.setPadding(false);
        layout.setWidthFull();
        return layout;
    }

    private void configureAttendanceTab() {
        attendanceEmployee.setItemLabelGenerator(EmployeePayrollRow::displayName);
        attendanceEmployee.setClearButtonVisible(true);
        attendanceEmployee.setWidth("420px");
        attendanceEmployee.addValueChangeListener(event -> refreshAttendanceDetails(event.getValue()));

        Grid<PayrollAttendanceDaySnapshot> grid = attendanceDaySection.grid();
        addColumn(grid, PayrollAttendanceDaySnapshot::getAttendanceDate,
                "Date | កាលបរិច្ឆេទ", "date").setFrozen(true);
        addColumn(grid, PayrollAttendanceDaySnapshot::getLeaveTypeId,
                "Duty / Leave ID | លេខកាតព្វកិច្ច / ច្បាប់", "leaveTypeId");
        addColumn(grid, PayrollAttendanceDaySnapshot::getPayrollPolicyRuleId,
                "Payroll Rule ID | លេខច្បាប់ប្រាក់បៀវត្ស", "policyRuleId");
        addColumn(grid, PayrollAttendanceDaySnapshot::getHolidayId,
                "Holiday ID | លេខថ្ងៃឈប់សម្រាក", "holidayId");
        addColumn(grid, value -> number(value.getDayValue()),
                "Day Value | ចំនួនថ្ងៃ", "dayValue");
        addColumn(grid, value -> number(value.getOvertimeHours()),
                "OT Hours | ម៉ោងបន្ថែម", "ot");
        addColumn(grid, value -> number(value.getNormalWorkingHours()),
                "Working Hours | ម៉ោងធ្វើការ", "hours");
        addColumn(grid, value -> yesNo(value.isScheduledWorkday()),
                "Scheduled Workday | ថ្ងៃធ្វើការ", "workday");
        addColumn(grid, PayrollAttendanceDaySnapshot::getLeavePeriodStart,
                "Leave Start | ថ្ងៃចាប់ផ្ដើមច្បាប់", "leaveStart");
        addColumn(grid, PayrollAttendanceDaySnapshot::getSourceType,
                "Source | ប្រភព", "attendanceSource");

        attendanceDaySection.withLoader(search -> {
            EmployeePayrollRow employee = attendanceEmployee.getValue();
            if (employee == null) {
                return List.of();
            }
            return filterAttendanceDays(attendanceSnapshotService.findDays(employee.id()), search);
        });
        attendanceDaySection.setAllRowsVisible(true);
    }

    private Component buildSeniorityTab() {
        HorizontalLayout filters = filterCard(seniorityEmployee);
        VerticalLayout layout = new VerticalLayout(filters, senioritySection);
        layout.setPadding(false);
        layout.setWidthFull();
        return layout;
    }

    private void configureSeniorityTab() {
        seniorityEmployee.setItemLabelGenerator(EmployeePayrollRow::displayName);
        seniorityEmployee.setClearButtonVisible(true);
        seniorityEmployee.setPlaceholder("All employees | បុគ្គលិកទាំងអស់");
        seniorityEmployee.setWidth("420px");
        seniorityEmployee.addValueChangeListener(event -> senioritySection.refresh());

        Grid<PayrollSeniorityCalculation> grid = senioritySection.grid();
        addColumn(grid, PayrollSeniorityCalculation::insuranceNo,
                "Insurance No. | លេខធានារ៉ាប់រង", "seniorityInsurance").setFrozen(true);
        addColumn(grid, PayrollSeniorityCalculation::employeeNameEn,
                "Name (English) | ឈ្មោះអង់គ្លេស", "seniorityNameEn").setFrozen(true);
        addColumn(grid, PayrollSeniorityCalculation::employeeNameKh,
                "Name (Khmer) | ឈ្មោះខ្មែរ", "seniorityNameKh");
        addColumn(grid, value -> value.getSeniorityRule() == null
                        ? "-" : value.getSeniorityRule().contractTypeName(),
                "Eligible Contract Type | ប្រភេទកិច្ចសន្យាមានសិទ្ធិ", "contractType");
        addColumn(grid, PayrollSeniorityCalculation::getSeniorityYear,
                "Year | ឆ្នាំ", "seniorityYear");
        addColumn(grid, PayrollSeniorityCalculation::getSemesterNo,
                "Semester | ឆមាស", "semester");
        addColumn(grid, PayrollSeniorityCalculation::getSemesterStart,
                "Semester Start | ចាប់ផ្ដើមឆមាស", "semesterStart");
        addColumn(grid, PayrollSeniorityCalculation::getSemesterEnd,
                "Semester End | បញ្ចប់ឆមាស", "semesterEnd");
        addColumn(grid, PayrollSeniorityCalculation::getEligibilityStart,
                "Eligibility Start | ចាប់ផ្ដើមសិទ្ធិ", "eligibilityStart");
        addColumn(grid, PayrollSeniorityCalculation::getEligibilityEnd,
                "Eligibility End | បញ្ចប់សិទ្ធិ", "eligibilityEnd");
        addColumn(grid, PayrollSeniorityCalculation::getEligibleCalendarDays,
                "Eligible Days | ថ្ងៃមានសិទ្ធិ", "eligibleDays");
        addColumn(grid, PayrollSeniorityCalculation::getEligibleMonthCount,
                "Eligible Months | ខែមានសិទ្ធិ", "eligibleMonths");
        addColumn(grid, value -> money(value.getTotalEligibleEarnings()),
                "Eligible Earnings | ប្រាក់ចំណូលមានសិទ្ធិ", "eligibleEarnings");
        addColumn(grid, value -> money(value.getAverageMonthlyEarnings()),
                "Average Monthly | មធ្យមប្រចាំខែ", "averageMonthly");
        addColumn(grid, PayrollSeniorityCalculation::getWorkdayDivisor,
                "Workday Divisor | ចំនួនថ្ងៃចែក", "workdayDivisor");
        addColumn(grid, value -> number(value.getAverageDailyEarnings()),
                "Average Daily | មធ្យមប្រចាំថ្ងៃ", "averageDaily");
        addColumn(grid, value -> number(value.getEntitlementDays()),
                "Entitlement Days | ថ្ងៃសិទ្ធិ", "entitlementDays");
        addColumn(grid, value -> money(value.getAmount()),
                "Amount | ចំនួន", "seniorityAmount");
        addColumn(grid, PayrollSeniorityCalculation::getCurrencyCode,
                "Currency | រូបិយប័ណ្ណ", "seniorityCurrency");

        senioritySection.withLoader(search -> {
            EmployeePayrollRow employee = seniorityEmployee.getValue();
            List<PayrollSeniorityCalculation> rows = seniorityService.findCalculationsForRun(
                    currentRunId(),
                    employee == null ? null : employee.id());
            return filterSeniority(rows, search);
        });
        senioritySection.setGridHeight("520px");
    }

    private Component buildPaymentsTab() {
        VerticalLayout layout = new VerticalLayout(paymentBatchSection, paymentDetailSection);
        layout.setPadding(false);
        layout.setWidthFull();
        return layout;
    }

    private void configurePaymentsTab() {
        Grid<PaymentBatchRow> batchGrid = paymentBatchSection.grid();
        addColumn(batchGrid, PaymentBatchRow::id,
                "Batch ID | លេខកញ្ចប់", "batchId").setFrozen(true);
        addColumn(batchGrid, PaymentBatchRow::runId,
                "Run ID | លេខដំណើរការ", "batchRunId");
        addColumn(batchGrid, PaymentBatchRow::installmentType,
                "Transaction | ប្រតិបត្តិការ", "installmentType");
        addColumn(batchGrid, PaymentBatchRow::paymentDate,
                "Value Date | កាលបរិច្ឆេទទូទាត់", "paymentDate");
        addColumn(batchGrid, PaymentBatchRow::status,
                "Status | ស្ថានភាព", "batchStatus");
        addColumn(batchGrid, PaymentBatchRow::employeeCount,
                "Employees | បុគ្គលិក", "batchEmployees");
        addColumn(batchGrid, value -> money(value.paymentAmount()),
                "Payment Amount | ចំនួនទូទាត់", "batchAmount");
        addColumn(batchGrid, value -> money(value.carryForwardAmount()),
                "Carry Forward | ចំនួនបន្ត", "carryForward");
        addColumn(batchGrid, PaymentBatchRow::currency,
                "Currency | រូបិយប័ណ្ណ", "batchCurrency");
        addColumn(batchGrid, PaymentBatchRow::paymentMethod,
                "Payment Method | វិធីទូទាត់", "paymentMethod");
        addColumn(batchGrid, PaymentBatchRow::paymentReference,
                "Payment Reference | យោងការទូទាត់", "paymentReference");
        addColumn(batchGrid, PaymentBatchRow::bankReference,
                "Bank Reference | យោងធនាគារ", "bankReference");
        addColumn(batchGrid, PaymentBatchRow::paymentFileName,
                "Export File | ឯកសារនាំចេញ", "paymentFile");
        addColumn(batchGrid, PaymentBatchRow::approvedAt,
                "Approved At | ពេលអនុម័ត", "approvedAt");
        addColumn(batchGrid, PaymentBatchRow::paidAt,
                "Paid At | ពេលបើកប្រាក់", "paidAt");

        paymentBatchSection.withLoader(search -> filterPaymentBatches(
                paymentService.findBatches(currentPeriodId()), search));
        paymentBatchSection.setGridHeight("300px");
        batchGrid.addSelectionListener(event -> {
            paymentDetailSection.refresh();
            PaymentBatchRow selected = event.getFirstSelectedItem().orElse(null);
            if (selected == null) {
                auditPaymentBatch.clear();
            } else {
                auditPaymentBatch.setValue(selected);
            }
        });

        Grid<EmployeePaymentRow> detailGrid = paymentDetailSection.grid();
        addColumn(detailGrid, EmployeePaymentRow::insuranceNo,
                "Insurance No. | លេខធានារ៉ាប់រង", "paymentInsurance").setFrozen(true);
        addColumn(detailGrid, EmployeePaymentRow::nameEn,
                "Name (English) | ឈ្មោះអង់គ្លេស", "paymentNameEn").setFrozen(true);
        addColumn(detailGrid, EmployeePaymentRow::nameKh,
                "Name (Khmer) | ឈ្មោះខ្មែរ", "paymentNameKh");
        addColumn(detailGrid, EmployeePaymentRow::paymentFrequency,
                "Frequency | ប្រេកង់", "paymentFrequency");
        addColumn(detailGrid, value -> number(value.firstPaymentPercent()),
                "First % | ភាគរយលើកទីមួយ", "firstPercent");
        addColumn(detailGrid, value -> money(value.basicSalary()),
                "Monthly Basic Salary | ប្រាក់ខែគោលប្រចាំខែ", "paymentBasic");
        addColumn(detailGrid, value -> money(value.fullNetAmount()),
                "Full Net | ប្រាក់សុទ្ធពេញ", "fullNet");
        addColumn(detailGrid, value -> money(value.previousPaidAmount()),
                "Previous Paid | បានបើកមុន", "previousPaid");
        addColumn(detailGrid, value -> money(value.paymentAmount()),
                "Pay Now | បើកឥឡូវ", "payNow");
        addColumn(detailGrid, value -> money(value.carryForwardAmount()),
                "Carry Forward | កាត់សងបន្ត", "paymentCarryForward");
        addColumn(detailGrid, EmployeePaymentRow::currency,
                "Currency | រូបិយប័ណ្ណ", "paymentCurrency");
        addColumn(detailGrid, EmployeePaymentRow::bankName,
                "Bank | ធនាគារ", "paymentBank");
        addColumn(detailGrid, EmployeePaymentRow::bankAccount,
                "Account No. | លេខគណនី", "paymentAccount");
        addColumn(detailGrid, EmployeePaymentRow::remarks,
                "Remarks | កំណត់សម្គាល់", "paymentRemarks");

        paymentDetailSection.withLoader(search -> paymentBatchSection.selected()
                .map(batch -> paymentService.findEmployeePayments(batch.id(), search))
                .orElse(List.of()));
        paymentDetailSection.setGridHeight("420px");
    }

    private Component buildAuditTab() {
        auditPaymentBatch.setItemLabelGenerator(this::paymentBatchLabel);
        auditPaymentBatch.setClearButtonVisible(true);
        auditPaymentBatch.setWidth("520px");
        auditPaymentBatch.addValueChangeListener(event -> paymentAuditSection.refresh());

        VerticalLayout layout = new VerticalLayout(
                runAuditSection,
                filterCard(auditPaymentBatch),
                paymentAuditSection);
        layout.setPadding(false);
        layout.setWidthFull();
        return layout;
    }

    private void configureAuditTab() {
        configureAuditGrid(runAuditSection.grid(), "runAudit");
        runAuditSection.withLoader(search -> filterAuditEvents(
                payrollService.findRunAuditHistory(currentRunId()), search));
        runAuditSection.setGridHeight("360px");

        configureAuditGrid(paymentAuditSection.grid(), "paymentAudit");
        paymentAuditSection.withLoader(search -> {
            PaymentBatchRow batch = auditPaymentBatch.getValue();
            return batch == null
                    ? List.of()
                    : filterAuditEvents(paymentService.findPaymentAuditHistory(batch.id()), search);
        });
        paymentAuditSection.setGridHeight("360px");
    }

    private void configureAuditGrid(Grid<PayrollAuditEventRow> grid, String prefix) {
        addColumn(grid, PayrollAuditEventRow::eventAt,
                "Event Time | ពេលវេលា", prefix + "At").setFrozen(true);
        addColumn(grid, PayrollAuditEventRow::eventType,
                "Event | ព្រឹត្តិការណ៍", prefix + "Type").setFrozen(true);
        addColumn(grid, PayrollAuditEventRow::actorDisplay,
                "User | អ្នកប្រើ", prefix + "Actor");
        addColumn(grid, PayrollAuditEventRow::employeeDisplay,
                "Employee | បុគ្គលិក", prefix + "Employee");
        addColumn(grid, PayrollAuditEventRow::payrollPaymentBatchId,
                "Batch ID | លេខកញ្ចប់", prefix + "Batch");
        addColumn(grid, this::statusChange,
                "Status Change | ប្តូរស្ថានភាព", prefix + "Status");
        addColumn(grid, PayrollAuditEventRow::reason,
                "Reason | មូលហេតុ", prefix + "Reason");
        addColumn(grid, PayrollAuditEventRow::eventDetail,
                "Detail | ព័ត៌មានលម្អិត", prefix + "Detail");
    }

    private void refreshReferenceData() {
        List<PeriodRow> periods = payrollService.findPeriods();
        periodFilter.setItems(periods);
        if (!periods.isEmpty()) {
            periodFilter.setValue(periods.get(0));
        } else {
            refreshPeriodInfo(null);
            refreshRunContext(null);
            refreshAttendanceControl(null);
            refreshPaymentsForPeriod(null);
            refreshPaymentAuditBatchOptions(null);
        }
    }

    private void refreshRunContext(RunRow run) {
        refreshRunInfo(run);

        employeeSection.clearSelection();
        employeeSection.refresh();
        itemSection.refresh();

        List<EmployeePayrollRow> employees = run == null
                ? List.of()
                : payrollService.findEmployees(run.id(), "");

        attendanceEmployee.clear();
        attendanceEmployee.setItems(employees);
        refreshAttendanceDetails(null);

        seniorityEmployee.clear();
        seniorityEmployee.setItems(employees);
        senioritySection.refresh();

        runAuditSection.refresh();
    }

    private void refreshPaymentsForPeriod(PeriodRow period) {
        paymentBatchSection.clearSelection();
        paymentBatchSection.refresh();
        paymentDetailSection.refresh();
    }

    private void refreshPaymentAuditBatchOptions(PeriodRow period) {
        auditPaymentBatch.clear();
        List<PaymentBatchRow> batches = period == null
                ? List.of()
                : paymentService.findBatches(period.id());
        auditPaymentBatch.setItems(batches);
        paymentAuditSection.refresh();
    }

    private void refreshPeriodInfo(PeriodRow period) {
        periodInfoCard.removeAll();
        H3 title = cardTitle("Payroll Period | រយៈពេលប្រាក់បៀវត្ស");
        if (period == null) {
            periodInfoCard.add(title, new Span("Select a payroll period. | សូមជ្រើសរយៈពេលបើកប្រាក់។"));
            return;
        }
        periodInfoCard.add(title, infoRow(
                "Period | រយៈពេល", "%04d-%02d".formatted(period.year(), period.month()),
                "Start | ចាប់ផ្ដើម", period.startDate(),
                "End | បញ្ចប់", period.endDate(),
                "Payment Date | ថ្ងៃបើកប្រាក់", period.paymentDate(),
                "Currency | រូបិយប័ណ្ណ", period.currency(),
                "Payroll/Tax FX KHR/USD | អត្រាប្តូរ", number(period.usdToKhrRate()),
                "NSSF FX KHR/USD | អត្រាប្តូរ ប.ស.ស.", number(period.nssfUsdToKhrRate()),
                "Status | ស្ថានភាព", period.status()));
    }

    private void refreshRunInfo(RunRow run) {
        runInfoCard.removeAll();
        H3 title = cardTitle("Payroll Run | ដំណើរការប្រាក់បៀវត្ស");
        if (run == null) {
            runInfoCard.add(title, new Span("Select a payroll run. | សូមជ្រើសដំណើរការប្រាក់បៀវត្ស។"));
            return;
        }
        runInfoCard.add(title, infoRow(
                "Run | ដំណើរការ", run.runNumber(),
                "Type | ប្រភេទ", run.runType(),
                "Status | ស្ថានភាព", run.status(),
                "Correction Mode | របៀបកែតម្រូវ", yesNo(run.correctionMode()),
                "Employees | បុគ្គលិក", run.employeeCount(),
                "Earnings | ប្រាក់ចំណូល", money(run.totalEarnings()),
                "Deductions | ការកាត់កង", money(run.totalDeductions()),
                "Net Pay | ប្រាក់សុទ្ធ", money(run.netPay()),
                "Notes | កំណត់សម្គាល់", nullToDash(run.notes())));
    }

    private void refreshAttendanceControl(PeriodRow period) {
        attendanceControlCard.removeAll();
        H3 title = cardTitle("Attendance Approval | ការអនុម័តវត្តមាន");
        if (period == null) {
            attendanceControlCard.add(title, new Span("Select a payroll period. | សូមជ្រើសរយៈពេលបើកប្រាក់។"));
            return;
        }

        AttendanceControlRow control = payrollService.findAttendanceControl(period.id());
        attendanceControlCard.add(title, infoRow(
                "Status | ស្ថានភាព", control.status(),
                "Stale | ទិន្នន័យប្រែប្រួល", yesNo(control.stale()),
                "Employees | បុគ្គលិក", control.rosterEmployeeCount(),
                "Roster Days | ថ្ងៃវេន", control.rosterDayCount(),
                "Attendance Records | កំណត់ត្រាវត្តមាន", control.attendanceRecordCount(),
                "Roster Fallback | ទិន្នន័យជំនួស", control.rosterFallbackDayCount(),
                "Unverified | មិនទាន់ផ្ទៀងផ្ទាត់", control.unverifiedAttendanceCount(),
                "OT Hours | ម៉ោងបន្ថែម", number(control.overtimeHours()),
                "Checked At | ពេលពិនិត្យ", control.checkedAt(),
                "Checked By | អ្នកពិនិត្យ", nullToDash(control.checkedBy()),
                "Approved At | ពេលអនុម័ត", control.approvedAt(),
                "Approved By | អ្នកអនុម័ត", nullToDash(control.approvedBy())));
    }

    private void refreshAttendanceDetails(EmployeePayrollRow employee) {
        attendanceSummaryCard.removeAll();
        H3 title = cardTitle("Attendance Summary | សង្ខេបវត្តមាន");

        if (employee == null) {
            attendanceSummaryCard.add(title, new Span("Select an employee. | សូមជ្រើសបុគ្គលិក។"));
            attendanceDaySection.refresh();
            return;
        }

        AttendanceRow summary = payrollService.findAttendance(employee.id()).orElse(null);
        if (summary == null) {
            attendanceSummaryCard.add(title, new Span("No attendance snapshot. | មិនមានទិន្នន័យវត្តមាន។"));
        } else {
            attendanceSummaryCard.add(title, infoRow(
                    "Source Start | ថ្ងៃចាប់ផ្ដើមប្រភព", summary.sourceStartDate(),
                    "Source End | ថ្ងៃបញ្ចប់ប្រភព", summary.sourceEndDate(),
                    "Scheduled | ថ្ងៃកំណត់", number(summary.scheduledDays()),
                    "Worked | បានធ្វើការ", number(summary.workedDays()),
                    "Holiday | ថ្ងៃឈប់", number(summary.holidayDays()),
                    "Absent | អវត្តមាន", number(summary.absentDays()),
                    "AL | ច្បាប់ប្រចាំឆ្នាំ", number(summary.annualLeaveDays()),
                    "SL | ច្បាប់ពិសេស", number(summary.specialLeaveDays()),
                    "Sick | ច្បាប់ឈឺ", number(summary.sickLeaveDays()),
                    "Maternity | មាតុភាព", number(summary.maternityLeaveDays()),
                    "Other Paid Leave | ច្បាប់មានប្រាក់ផ្សេង", number(summary.otherPaidLeaveDays()),
                    "Unpaid | គ្មានប្រាក់", number(summary.unpaidLeaveDays()),
                    "OT Hours | ម៉ោងបន្ថែម", number(summary.overtimeHours()),
                    "AL Remaining | AL នៅសល់", number(summary.annualLeaveRemaining()),
                    "AL Overused | AL ប្រើលើស", number(summary.overusedAnnualLeave()),
                    "SL Remaining | SL នៅសល់", number(summary.specialLeaveRemaining()),
                    "SL Overused | SL ប្រើលើស", number(summary.overusedSpecialLeave())));
        }
        attendanceDaySection.refresh();
    }

    private Long currentPeriodId() {
        PeriodRow period = periodFilter.getValue();
        return period == null ? null : period.id();
    }

    private Long currentRunId() {
        RunRow run = runFilter.getValue();
        return run == null ? null : run.id();
    }

    private String paymentBatchLabel(PaymentBatchRow batch) {
        if (batch == null) {
            return "-";
        }
        return "#%d · %s · %s · %s".formatted(
                batch.id(),
                nullToDash(batch.installmentType()),
                nullToDash(batch.paymentDate()),
                nullToDash(batch.status()));
    }

    private String statusChange(PayrollAuditEventRow event) {
        if (event == null) {
            return "-";
        }
        String from = nullToDash(event.fromStatus());
        String to = nullToDash(event.toStatus());
        return "-".equals(from) && "-".equals(to) ? "-" : from + " → " + to;
    }

    private static List<ItemRow> filterItems(List<ItemRow> rows, String search) {
        String term = normalize(search);
        if (term.isEmpty()) {
            return rows;
        }
        return rows.stream()
                .filter(row -> contains(row.componentCode(), term)
                        || contains(row.componentName(), term)
                        || contains(row.componentType(), term)
                        || contains(row.description(), term)
                        || contains(row.sourceType(), term)
                        || contains(row.remarks(), term)
                        || contains(row.currency(), term))
                .toList();
    }

    private static List<PayrollAttendanceDaySnapshot> filterAttendanceDays(
            List<PayrollAttendanceDaySnapshot> rows, String search) {
        String term = normalize(search);
        if (term.isEmpty()) {
            return rows;
        }
        return rows.stream()
                .filter(row -> contains(row.getAttendanceDate(), term)
                        || contains(row.getLeaveTypeId(), term)
                        || contains(row.getPayrollPolicyRuleId(), term)
                        || contains(row.getHolidayId(), term)
                        || contains(row.getSourceType(), term))
                .toList();
    }

    private static List<PayrollSeniorityCalculation> filterSeniority(
            List<PayrollSeniorityCalculation> rows, String search) {
        String term = normalize(search);
        if (term.isEmpty()) {
            return rows;
        }
        return rows.stream()
                .filter(row -> contains(row.insuranceNo(), term)
                        || contains(row.employeeNameEn(), term)
                        || contains(row.employeeNameKh(), term)
                        || contains(row.getSeniorityYear(), term)
                        || contains(row.getSemesterNo(), term)
                        || contains(row.getEligibilityStart(), term)
                        || contains(row.getEligibilityEnd(), term)
                        || contains(row.getCurrencyCode(), term)
                        || (row.getSeniorityRule() != null
                            && contains(row.getSeniorityRule().contractTypeName(), term)))
                .toList();
    }

    private static List<PaymentBatchRow> filterPaymentBatches(
            List<PaymentBatchRow> rows, String search) {
        String term = normalize(search);
        if (term.isEmpty()) {
            return rows;
        }
        return rows.stream()
                .filter(row -> contains(row.id(), term)
                        || contains(row.runId(), term)
                        || contains(row.installmentType(), term)
                        || contains(row.paymentDate(), term)
                        || contains(row.status(), term)
                        || contains(row.paymentMethod(), term)
                        || contains(row.paymentReference(), term)
                        || contains(row.bankReference(), term)
                        || contains(row.paymentFileName(), term)
                        || contains(row.notes(), term))
                .toList();
    }

    private static List<PayrollAuditEventRow> filterAuditEvents(
            List<PayrollAuditEventRow> rows, String search) {
        String term = normalize(search);
        if (term.isEmpty()) {
            return rows;
        }
        return rows.stream()
                .filter(row -> contains(row.eventAt(), term)
                        || contains(row.eventType(), term)
                        || contains(row.actorDisplay(), term)
                        || contains(row.employeeDisplay(), term)
                        || contains(row.payrollPaymentBatchId(), term)
                        || contains(row.fromStatus(), term)
                        || contains(row.toStatus(), term)
                        || contains(row.reason(), term)
                        || contains(row.eventDetail(), term))
                .toList();
    }


    private static HorizontalLayout filterCard(Component... fields) {
        HorizontalLayout layout = new HorizontalLayout(fields);
        layout.setAlignItems(Alignment.END);
        layout.setWrap(true);
        layout.setPadding(true);
        layout.setWidthFull();
        layout.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");
        return layout;
    }

    private static <T, V> Grid.Column<T> addColumn(
            Grid<T> grid,
            com.vaadin.flow.function.ValueProvider<T, V> provider,
            String header,
            String key) {
        return grid.addColumn(provider)
                .setHeader(header)
                .setKey(key)
                .setAutoWidth(true)
                .setResizable(true)
                .setSortable(true);
    }

    private static void styleInfoCard(Div card) {
        card.setWidthFull();
        card.getStyle()
                .set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");
    }

    private static H3 cardTitle(String text) {
        H3 title = new H3(text);
        title.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");
        return title;
    }

    private static Component infoRow(Object... labelAndValues) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWrap(true);
        row.setWidthFull();

        for (int index = 0; index + 1 < labelAndValues.length; index += 2) {
            Div metric = new Div();
            Span label = new Span(String.valueOf(labelAndValues[index]));
            label.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)");

            Span value = new Span(nullToDash(labelAndValues[index + 1]));
            value.getStyle()
                    .set("font-weight", "700")
                    .set("font-size", "var(--lumo-font-size-m)");

            metric.add(label, new Div(value));
            metric.getStyle().set("min-width", "150px");
            row.add(metric);
        }
        return row;
    }

    private static String money(BigDecimal value) {
        return value == null
                ? "-"
                : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String number(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private static String yesNo(boolean value) {
        return value ? "Yes | បាទ/ចាស" : "No | ទេ";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(Object value, String normalizedTerm) {
        return value != null
                && String.valueOf(value).toLowerCase(Locale.ROOT).contains(normalizedTerm);
    }

    private static String nullToDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        return text.isBlank() ? "-" : text;
    }
}
