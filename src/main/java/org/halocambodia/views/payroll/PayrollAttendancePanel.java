package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.*;
import static org.halocambodia.views.payroll.PayrollViewSupport.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.enums.PayrollAttendanceControlStatus;
import org.halocambodia.enums.PayrollPeriodStatus;
import org.halocambodia.data.LeaveType;
import org.halocambodia.data.LeaveTypeRepository;
import org.halocambodia.data.LeaveTypeSubType;
import org.halocambodia.data.LeaveTypeSubTypeRepository;
import org.halocambodia.data.enums.LeaveDuration;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.PayrollActionPermissions;
import org.halocambodia.services.PayrollAttendanceDayEditService;
import org.halocambodia.services.PayrollService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.ProgressDialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

final class PayrollAttendancePanel extends VerticalLayout {


    private static final Set<String> ATTENDANCE_SUMMARY_EXCLUDED_CODES = Set.of("√", "H", "OFF", "OFF/PM", "OFF/PMM");

    private final PayrollService service;
    private final AuthenticatedUser authenticatedUser;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeSubTypeRepository leaveTypeSubTypeRepository;
    private final PayrollAttendanceDayEditService attendanceDayEditService;
    private final PayrollRefreshCoordinator refreshCoordinator;

    private final List<LegendDef> attendanceLegends;
    private final Map<String, LegendDef> attendanceLegendByCode;

    private final Grid<AttendanceReviewRow> attendanceReviewGrid = new Grid<>();
    private HeaderRow attendanceMonthHeaderRow;
    private final Div attendanceReviewLegend = new Div();
    private final ComboBox<PeriodRow> attendancePeriodFilter = new ComboBox<>("Payroll Period");
    private final DatePicker attendanceSummaryStartDate = new DatePicker("From Date");
    private final DatePicker attendanceSummaryEndDate = new DatePicker("To Date");
    private final TextField attendanceReviewSearch = new TextField();
    private final Checkbox attendanceIssuesOnly = new Checkbox("Issues Only");
    private final Span attendanceReviewSummary = new Span( "Select a payroll period.");
    private final Button attendanceReviewApproveButton = actionButton("Approve & Lock", VaadinIcon.LOCK);
    private final Button attendanceReviewReopenButton =  actionButton("Reopen", VaadinIcon.REFRESH);

    private AttendanceControlRow attendanceReviewControl;
    private List<AttendanceReviewRow> attendanceReviewRows = List.of();
    private StreamRegistration attendanceExportRegistration;

    PayrollAttendancePanel( PayrollService service, AuthenticatedUser authenticatedUser,LeaveTypeRepository leaveTypeRepository, LeaveTypeSubTypeRepository leaveTypeSubTypeRepository, PayrollAttendanceDayEditService attendanceDayEditService, PayrollRefreshCoordinator refreshCoordinator) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveTypeSubTypeRepository = leaveTypeSubTypeRepository;
        this.attendanceDayEditService = attendanceDayEditService;
        this.refreshCoordinator = refreshCoordinator;
        this.attendanceLegends = loadAttendanceLegends(leaveTypeRepository);
        this.attendanceLegendByCode = indexAttendanceLegends(attendanceLegends);

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        addDetachListener(event -> releaseAttendanceExportResource());

        configureAttendanceReviewGrid((PeriodRow) null, List.of());
        attendanceReviewGrid.addItemClickListener(event ->
                handleAttendanceDayClick(event.getItem(), event.getColumn().getKey()));
        Component content = buildPage();
        add(content);
        setFlexGrow(1, content);
    }

    private Component buildPage() {

       // VerticalLayout page = page("Payroll Attendance Check & Lock | ពិនិត្យ និងចាក់សោវត្តមានប្រាក់បៀវត្ស","Review HR-finalized attendance by employee and day, then approve and lock the payroll snapshot. | ពិនិត្យវត្តមានដែល HR បានបញ្ចប់តាមបុគ្គលិក និងតាមថ្ងៃ បន្ទាប់មកអនុម័ត និងចាក់សោទិន្នន័យប្រាក់បៀវត្ស។");
    	 VerticalLayout page = page(null,null);

        attendancePeriodFilter.setItemLabelGenerator(PayrollViewSupport::periodLabel);
        attendancePeriodFilter.setClearButtonVisible(true);

        attendancePeriodFilter.setWidth("390px");
        attendancePeriodFilter.addValueChangeListener(e -> refreshAttendanceReview());

        attendanceReviewSearch.setPlaceholder("Search insurance, employee, team, location...");
        attendanceReviewSearch.setPrefixComponent(VaadinIcon.SEARCH.create());
        attendanceReviewSearch.setClearButtonVisible(true);
        
        attendanceReviewSearch.setWidth("min(400px, 100%)");
        attendanceReviewSearch.setValueChangeMode(ValueChangeMode.LAZY);
        attendanceReviewSearch.addValueChangeListener(e -> refreshAttendanceReviewRows());
        attendanceIssuesOnly.addValueChangeListener(e ->refreshAttendanceReviewRowsWithProgress("Filtering Attendance | កំពុងត្រងវត្តមាន","Applying the Issues Only filter... | កំពុងអនុវត្តតម្រងបញ្ហា..."));

        Button refresh = actionButton("",VaadinIcon.REFRESH);
        refresh.setTooltipText("Refresh");
        
        refresh.addClickListener(e -> refreshAttendanceReviewWithProgress());

        Button exportExcel = createAttendanceReviewExcelExport();
        attendanceReviewApproveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        attendanceReviewApproveButton.addClickListener(e -> confirmAttendanceReviewApproval());

        attendanceReviewReopenButton.addClickListener(e -> confirmAttendanceReviewReopen());

        HorizontalLayout filters = new HorizontalLayout(attendancePeriodFilter, attendanceReviewSearch, attendanceIssuesOnly, refresh, exportExcel, attendanceReviewApproveButton, attendanceReviewReopenButton);
        filters.setAlignItems(Alignment.END);
        filters.setWrap(true);
        filters.setWidthFull();


      //  HorizontalLayout actions = toolbar(  attendanceReviewApproveButton, attendanceReviewReopenButton);

        attendanceReviewSummary.addClassName("payroll-processing-summary");

        attendanceReviewLegend.addClassName("payroll-attendance-legend");

        renderAttendanceLegend(List.of());

        page.add(filters, attendanceReviewSummary, attendanceReviewGrid, attendanceReviewLegend);

        page.setFlexGrow(1, attendanceReviewGrid);

        return page;

    }

    Component buildSummaryPage() {

        VerticalLayout page = page("Attendance Summary | សង្ខេបវត្តមាន", "View employee attendance for a selected date range. "  + "| មើលសង្ខេបវត្តមានបុគ្គលិកតាមចន្លោះកាលបរិច្ឆេទដែលបានជ្រើសរើស។");

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Phnom_Penh"));

        attendanceSummaryStartDate.setValue(today.withDayOfMonth(1));
        attendanceSummaryEndDate.setValue(today);
        attendanceSummaryStartDate.setWidth("180px");
        attendanceSummaryEndDate.setWidth("180px");
        attendanceSummaryStartDate.addValueChangeListener(e -> refreshAttendanceSummary());
        attendanceSummaryEndDate.addValueChangeListener(e -> refreshAttendanceSummary());
        attendanceReviewSearch.setPlaceholder("Search insurance number, employee, team, location...");
        attendanceReviewSearch.setPrefixComponent(VaadinIcon.SEARCH.create());
        attendanceReviewSearch.setClearButtonVisible(true);
        attendanceReviewSearch.setWidth("min(420px, 100%)");
        attendanceReviewSearch.setValueChangeMode(ValueChangeMode.LAZY);
        attendanceReviewSearch.addValueChangeListener(e -> refreshAttendanceSummaryRows());
        attendanceIssuesOnly.addValueChangeListener(e -> refreshAttendanceSummaryRows());
        Button refresh = actionButton("Refresh | ផ្ទុកឡើងវិញ", VaadinIcon.REFRESH);
        refresh.addClickListener(e -> refreshAttendanceSummary());
        HorizontalLayout filters = new HorizontalLayout( attendanceSummaryStartDate, attendanceSummaryEndDate, attendanceReviewSearch, attendanceIssuesOnly, refresh);

        filters.setAlignItems(Alignment.END);
        filters.setWrap(true);
        filters.setWidthFull();

        attendanceReviewSummary.addClassName("payroll-processing-summary");
        attendanceReviewLegend.addClassName("payroll-attendance-legend");
        renderAttendanceLegend(List.of());
        page.add(filters, attendanceReviewSummary, attendanceReviewGrid, attendanceReviewLegend);
        page.setFlexGrow(1, attendanceReviewGrid);
        return page;

    }


    private void configureAttendanceReviewGrid(  PeriodRow period, List<PayrollRuleRow> activeRules) {
        configureAttendanceReviewGrid( period == null ? null : period.startDate(),  period == null ? null : period.endDate(),  activeRules);
    }

    private void configureAttendanceReviewGrid( LocalDate startDate, LocalDate endDate, List<PayrollRuleRow> activeRules) {

        configureGrid(attendanceReviewGrid);

        if (attendanceMonthHeaderRow != null) {
            attendanceReviewGrid.removeHeaderRow(attendanceMonthHeaderRow);
            attendanceMonthHeaderRow = null;
        }

        attendanceReviewGrid.removeAllColumns();
        attendanceReviewGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        attendanceReviewGrid.addColumn(AttendanceReviewRow::insuranceNo).setHeader("Insurance").setKey("insuranceNo").setFrozen(true).setWidth("90px").setFlexGrow(0).setSortable(true);
        attendanceReviewGrid.addColumn(AttendanceReviewRow::nameEn).setHeader("Employee Name").setKey("nameEn").setFrozen(true).setWidth("190px").setFlexGrow(0).setSortable(true);
        attendanceReviewGrid.addColumn(AttendanceReviewRow::position).setHeader("Position").setWidth("190px").setFlexGrow(0);
        attendanceReviewGrid.addColumn(AttendanceReviewRow::employeeCategory).setHeader("Category").setWidth("150px").setFlexGrow(0);
        attendanceReviewGrid.addColumn(AttendanceReviewRow::team).setHeader("Team ").setWidth("130px").setFlexGrow(0);
        attendanceReviewGrid.addColumn(AttendanceReviewRow::location).setHeader("Location").setWidth("130px").setFlexGrow(0);
        attendanceDecimalColumn("AL Re", AttendanceReviewRow::annualLeaveRemaining, "118px");
        attendanceDecimalColumn("O.AL", AttendanceReviewRow::overusedAnnualLeave, "128px");
        attendanceDecimalColumn("SL Re", AttendanceReviewRow::specialLeaveRemaining, "118px");
        attendanceDecimalColumn("O.SL", AttendanceReviewRow::overusedSpecialLeave, "128px");

        Map<YearMonth, List<Grid.Column<AttendanceReviewRow>>> monthColumns =new LinkedHashMap<>();

        if (startDate != null && endDate != null) {

            for (LocalDate date = startDate;

                    !date.isAfter(endDate);

                    date = date.plusDays(1)) {

                LocalDate columnDate = date;

                Grid.Column<AttendanceReviewRow> dayColumn = attendanceReviewGrid

                        .addColumn(attendanceDayRenderer(columnDate))

                        .setHeader(attendanceDayHeader(columnDate))

                        .setKey("date-" + columnDate)

                        .setWidth("58px").setFlexGrow(0)

                        .setTextAlign(ColumnTextAlign.CENTER)

                        .setResizable(true);

                monthColumns.computeIfAbsent(

                        YearMonth.from(columnDate), ignored -> new java.util.ArrayList<>())

                        .add(dayColumn);

            }

        }

        attendanceDecimalColumn("P", "Present", AttendanceReviewRow::presentDays, "62px");

        attendanceDecimalColumn("A", "Absent", AttendanceReviewRow::absentDays, "62px");

        attendanceDecimalColumn("AL", "Annual Leave", AttendanceReviewRow::annualLeaveDays, "62px");

        attendanceDecimalColumn("UL", "Unpaid Leave", AttendanceReviewRow::unpaidLeaveDays, "62px");

        attendanceDecimalColumn("SL", "Special Leave", AttendanceReviewRow::specialLeaveDays, "62px");

        attendanceDecimalColumn("S", "Sick Leave", AttendanceReviewRow::sickLeaveDays, "62px");

        attendanceDecimalColumn("ML", "Maternity Leave", AttendanceReviewRow::maternityLeaveDays, "62px");

        attendanceDecimalColumn("H", "Holiday (excluding Off and Off/PMM)",

                AttendanceReviewRow::holidayDays, "62px");

        attendanceDecimalColumn("Off", "Day Off",

                row -> holidayCodeDays(row, "OFF"), "72px");

        attendanceDecimalColumn("Off/PMM", "Day Off / PMM",

                row -> holidayCodeDays(row, "OFF/PMM"), "98px");

        attendanceDecimalColumn("Other", "Other Attendance",

                AttendanceReviewRow::otherDays, "105px");

        // Dynamic payroll-rule columns are shared with Excel export so the
        // Attendance Check & Lock grid and workbook always have the same columns.

        for (PayrollRuleRow rule : attendanceReviewDynamicRules(activeRules)) {

            String policyCode = normalizedRuleCode(rule.code());

            boolean hourlyOvertime = isHourlyOvertimeAttendanceRule(rule);

            attendanceDecimalColumn(

                    hourlyOvertime ? policyCode + " (hrs)" : policyCode,

                    attendanceRuleLegendLabel(rule)

                            + (hourlyOvertime ? " - Overtime Hours" : ""),

                    hourlyOvertime

                            ? AttendanceReviewRow::overtimeHours

                            : row -> attendancePolicyQuantity(row, policyCode),

                    hourlyOvertime ? "96px" : "78px");

        }

        attendanceReviewGrid.addColumn(AttendanceReviewRow::unverifiedAttendanceCount)

                .setHeader("Unverified | មិនទាន់ផ្ទៀងផ្ទាត់").setWidth("155px").setFlexGrow(0)

                .setTextAlign(ColumnTextAlign.END);

        attendanceReviewGrid.addColumn(this::attendanceLegendSummary)

                .setHeader("Payroll Summary | សង្ខេបប្រាក់បៀវត្ស")

                .setWidth("320px")

                .setFlexGrow(1)

                .setResizable(true);

        // Apply resizing to employee, balance, day and summary columns.

        attendanceReviewGrid.getColumns()

                .forEach(column -> column.setResizable(true));

        // Header row 1 groups calendar-day columns by their actual month.

        // Vaadin's normal column header remains row 2 (day number + weekday).

        if (!monthColumns.isEmpty()) {

            attendanceMonthHeaderRow = attendanceReviewGrid.prependHeaderRow();

            monthColumns.forEach((month, columns) -> {

                @SuppressWarnings("unchecked")

                Grid.Column<AttendanceReviewRow>[] columnsForMonth =

                        columns.toArray(Grid.Column[]::new);

                attendanceMonthHeaderRow.join(columnsForMonth)

                        .setComponent(attendanceMonthHeader(month));

            });

        }

    }

    private void attendanceDecimalColumn(

            String header,

            com.vaadin.flow.function.ValueProvider<AttendanceReviewRow, BigDecimal> provider,

            String width) {

        attendanceDecimalColumn(header, null, provider, width);

    }

    private void attendanceDecimalColumn(

            String header, String tooltip,

            com.vaadin.flow.function.ValueProvider<AttendanceReviewRow, BigDecimal> provider,

            String width) {

        Grid.Column<AttendanceReviewRow> column = attendanceReviewGrid

                .addColumn(row -> decimal(provider.apply(row)))

                .setWidth(width).setFlexGrow(0)

                .setTextAlign(ColumnTextAlign.END).setResizable(true);

        column.setHeader(tooltip == null || tooltip.isBlank()

                ? new Span(header)

                : attendanceSummaryHeader(header, tooltip));

    }

    private static Span attendanceSummaryHeader(String code, String tooltip) {

        Span header = new Span(code);

        header.getElement().setAttribute("title", code + " - " + tooltip);

        header.getElement().setAttribute("aria-label", code + " - " + tooltip);

        return header;

    }

    private static BigDecimal holidayCodeDays(AttendanceReviewRow row, String code) {

        if (row == null || row.days() == null || code == null) {

            return BigDecimal.ZERO;

        }

        String expectedCode = normalizedRuleCode(code);

        return row.days().values().stream()

                .filter(cell -> cell != null

                        && expectedCode.equals(normalizedRuleCode(cell.code()))

                        && "HOLIDAY".equals(normalizedRuleCode(cell.source())))

                .map(AttendanceDayCell::dayValue)

                .filter(java.util.Objects::nonNull)

                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    private LitRenderer<AttendanceReviewRow> attendanceDayRenderer(LocalDate date) {

        return LitRenderer.<AttendanceReviewRow>of("""

                <span class="attendance-day-badge ${item.cssClass}"

                      style="--attendance-custom-accent: ${item.customAccent};"

                      title="${item.tooltip}">${item.code}</span>

                """)
                .withProperty("code", row -> attendanceDayCode(row.days().get(date)))
                .withProperty("cssClass", row -> attendanceDayCss(row.days().get(date)))
                .withProperty("customAccent", row -> attendanceDayCustomAccent(row.days().get(date)))
                .withProperty("tooltip", row -> attendanceDayTooltip(row.days().get(date)));
    }


    void refresh() {

        try {
            Long selectedId = Optional.ofNullable(attendancePeriodFilter.getValue()) .map(PeriodRow::id).orElse(null);
            List<PeriodRow> periods = service.findPeriods();
            attendancePeriodFilter.setItems(periods);

            periods.stream().filter(p -> p.id().equals(selectedId)).findFirst().or(() -> periods.stream().findFirst()).ifPresentOrElse(attendancePeriodFilter::setValue, attendancePeriodFilter::clear);

            if (periods.isEmpty()) {
                attendanceReviewControl = null;
                attendanceReviewGrid.setItems(List.of());
                configureAttendanceReviewGrid(null, List.of());
                renderAttendanceLegend(List.of());
                updateAttendanceReviewActions();
            } else {
                refreshAttendanceReview();
            }
        } catch (Exception ex) {
            error(databaseMessage(ex));
        }
    }

    private void refreshAttendanceSummary() {
        LocalDate startDate = attendanceSummaryStartDate.getValue();
        LocalDate endDate = attendanceSummaryEndDate.getValue();
        if (startDate == null || endDate == null) {
            configureAttendanceReviewGrid((PeriodRow) null, List.of());
            renderAttendanceLegend(List.of());
            attendanceReviewGrid.setItems(List.of());
            attendanceReviewSummary.setText("Select From Date and To Date. | សូមជ្រើសរើសថ្ងៃចាប់ផ្ដើម និងថ្ងៃបញ្ចប់។");
            return;
        }

        if (endDate.isBefore(startDate)) {
            attendanceReviewGrid.setItems(List.of());
            attendanceReviewSummary.setText("End Date must be on or after Start Date. "  + "| ថ្ងៃបញ្ចប់ត្រូវតែស្មើ ឬក្រោយថ្ងៃចាប់ផ្ដើម។");
            return;
        }

        try {

            List<PayrollRuleRow> activeRules = new ArrayList<>();

            for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {

                activeRules.addAll(service.findPayrollRules(year).stream()
                        .filter(PayrollRuleRow::active)
                        .toList());
            }

            configureAttendanceReviewGrid(startDate, endDate, activeRules);
            renderAttendanceLegend(activeRules);
            refreshAttendanceSummaryRows();
        } catch (Exception ex) {
            configureAttendanceReviewGrid(startDate, endDate, List.of());
            renderAttendanceLegend(List.of());
            attendanceReviewGrid.setItems(List.of());
            error(databaseMessage(ex));
        }
    }

    private void refreshAttendanceSummaryRows() {
        LocalDate startDate = attendanceSummaryStartDate.getValue();
        LocalDate endDate = attendanceSummaryEndDate.getValue();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            attendanceReviewGrid.setItems(List.of());
            return;
        }

        try {

            List<AttendanceReviewRow> rows = service.findAttendanceSummary(startDate, endDate, attendanceReviewSearch.getValue(), attendanceIssuesOnly.getValue());
            attendanceReviewGrid.setItems(rows);
            long issueEmployees = rows.stream().filter(AttendanceReviewRow::hasIssues).count();
            long unverified = rows.stream()
                    .mapToLong(AttendanceReviewRow::unverifiedAttendanceCount).sum();

            attendanceReviewSummary.setText(("%s → %s · %d employee(s) | បុគ្គលិក · %d with issues | មានបញ្ហា " + "· %d unverified | មិនទាន់ផ្ទៀងផ្ទាត់") .formatted(startDate, endDate, rows.size(), issueEmployees, unverified));
        } catch (Exception ex) {
            attendanceReviewGrid.setItems(List.of());
            error(databaseMessage(ex));
        }
    }

    private void refreshAttendanceReview() {
        PeriodRow period = attendancePeriodFilter.getValue();
        if (period == null) {
            configureAttendanceReviewGrid(null, List.of());
            renderAttendanceLegend(List.of());
            attendanceReviewControl = null;
            attendanceReviewGrid.setItems(List.of());
            attendanceReviewSummary.setText("Select a payroll period.");
            updateAttendanceReviewActions();
            return;
        }

        try {

            List<PayrollRuleRow> activeRules = service.findPayrollRules(period.year()).stream()
                    .filter(PayrollRuleRow::active)
                    .toList();
            configureAttendanceReviewGrid(period, activeRules);
            renderAttendanceLegend(activeRules);
            attendanceReviewControl = service.findAttendanceControl(period.id());
            refreshAttendanceReviewRows();
            updateAttendanceReviewActions();
        } catch (Exception ex) {
            configureAttendanceReviewGrid(period, List.of());
            renderAttendanceLegend(List.of());
            attendanceReviewControl = null;
            attendanceReviewGrid.setItems(List.of());
            updateAttendanceReviewActions();
            error(databaseMessage(ex));
        }

    }

    private void refreshAttendanceReviewRows() {

        PeriodRow period = attendancePeriodFilter.getValue();

        if (period == null) {
            attendanceReviewRows = List.of();
            attendanceReviewGrid.setItems(List.of());
            attendanceReviewSummary.setText("Select a payroll period.");
            return;

        }

        try {
            List<AttendanceReviewRow> rows = service.findAttendanceReview( period.id(), attendanceReviewSearch.getValue(), attendanceIssuesOnly.getValue());
            applyAttendanceReviewRows(period, rows);
        } catch (Exception ex) {
            attendanceReviewRows = List.of();
            attendanceReviewGrid.setItems(List.of());
            error(databaseMessage(ex));
        }

    }


    private Button createAttendanceReviewExcelExport() {

        Button exportButton = new Button( VaadinIcon.DOWNLOAD.create());
        exportButton.setTooltipText("Export Excel");
        exportButton.addThemeVariants( ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        exportButton.addClickListener(event -> exportAttendanceReviewWithProgress());
        return exportButton;
    }

    private void exportAttendanceReviewWithProgress() {

        PeriodRow period = attendancePeriodFilter.getValue();
        if (period == null) {
            error("Select a payroll period before exporting. " );
            return;
        }

        List<AttendanceReviewRow> rows = List.copyOf(attendanceReviewRows);

        String filename = "Payroll_Attendance_%04d_%02d.xlsx".formatted(period.year(), period.month());

        runAttendanceBackgroundTask("Export Attendance | នាំចេញវត្តមាន","Preparing Excel file... | កំពុងរៀបចំឯកសារ Excel...",

                () -> buildAttendanceReviewExcelWorkbook(period, rows),
                excel -> {
                    triggerAttendanceExcelDownload(excel, filename);
                    success("Attendance Excel is ready. Download started. " + "| ឯកសារ Excel វត្តមានរួចរាល់។ ការទាញយកបានចាប់ផ្តើម។");
                });
    }

    private void triggerAttendanceExcelDownload(byte[] excel, String filename) {
        releaseAttendanceExportResource();

        StreamResource resource = new StreamResource(filename, () -> new ByteArrayInputStream(excel));
        resource.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resource.setCacheTime(0);
        attendanceExportRegistration = VaadinSession.getCurrent()
                .getResourceRegistry()
                .registerResource(resource);
        String url = attendanceExportRegistration.getResourceUri().toString();
        UI.getCurrent().getPage().executeJs(
                "const a=document.createElement('a');"
                        + "a.href=$0;a.download=$1;document.body.appendChild(a);"
                        + "a.click();a.remove();",
                url, filename);
    }

    private void releaseAttendanceExportResource() {

        if (attendanceExportRegistration != null) {
            try {
                attendanceExportRegistration.unregister();

            } catch (Exception ignore) {
                // Resource may already be detached with the Vaadin session.

            } finally {
                attendanceExportRegistration = null;

            }

        }

    }

    private byte[] buildAttendanceReviewExcelWorkbook(

            PeriodRow period, List<AttendanceReviewRow> rows) {

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll Attendance");
            CellStyle headerStyle = createAttendanceExcelHeaderStyle(workbook);
            CellStyle weekendStyle = createAttendanceExcelWeekendHeaderStyle(workbook);
            CellStyle legendStyle = createAttendanceExcelLegendStyle(workbook);
            CellStyle decimalStyle = workbook.createCellStyle();
            decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            List<LocalDate> dates = period.startDate().datesUntil(period.endDate().plusDays(1)).toList();

            List<PayrollRuleRow> activeRules = service.findPayrollRules(period.year()).stream()
                    .filter(PayrollRuleRow::active)
                    .toList();

            List<PayrollRuleRow> dynamicRules = attendanceReviewDynamicRules(activeRules);
            Row groupHeader = sheet.createRow(0);
            Row header = sheet.createRow(1);
            int column = 0;

            // Exact same fixed columns and order as configureAttendanceReviewGrid().
            for (String label : List.of(
                    "Insurance",
                    "Employee Name",
                    "Position",
                    "Category",
                    "Team",
                    "Location",
                    "AL Re",
                    "O.AL",
                    "SL Re",
                    "O.SL")) {

                column = attendanceVerticalExcelHeader(sheet, groupHeader, header, column, label, headerStyle);

            }

            int dateIndex = 0;

            while (dateIndex < dates.size()) {
                YearMonth month = YearMonth.from(dates.get(dateIndex));
                int monthStart = column;
                while (dateIndex < dates.size() && YearMonth.from(dates.get(dateIndex)).equals(month)) {
                    LocalDate date = dates.get(dateIndex++);
                    String dayLabel = date.getDayOfMonth() + "\n"

                            + date.getDayOfWeek().name().substring(0, 3)

                            + "/" + khmerWeekdayShort(date);

                    CellStyle style = date.getDayOfWeek().getValue() >= 6

                            ? weekendStyle : headerStyle;

                    column = attendanceExcelHeader(header, column, dayLabel, style);

                }

                attendanceMergedExcelHeader(

                        sheet, groupHeader, monthStart, column - 1,

                        month.format(DateTimeFormatter.ofPattern(

                                "MMMM yyyy", Locale.ENGLISH))

                                + " | " + khmerMonth(month.getMonthValue())

                                + " " + month.getYear(),

                        headerStyle);

            }

            // Exact same fixed attendance totals as the grid.
            for (String code : List.of(

                    "P", "A", "AL", "UL", "SL", "S", "ML", "H",

                    "Off", "Off/PMM", "Other")) {

                column = attendanceVerticalExcelHeader(

                        sheet, groupHeader, header, column, code, headerStyle);

            }

            // Exact same dynamic payroll-rule columns as the grid.
            for (PayrollRuleRow rule : dynamicRules) {
                String policyCode = normalizedRuleCode(rule.code());
                boolean hourlyOvertime = isHourlyOvertimeAttendanceRule(rule);
                column = attendanceVerticalExcelHeader( sheet, groupHeader, header, column,  hourlyOvertime ? policyCode + " (hrs)" : policyCode,  headerStyle);
            }

            column = attendanceVerticalExcelHeader(sheet, groupHeader, header, column,"Unverified", headerStyle);
            column = attendanceVerticalExcelHeader(sheet, groupHeader, header, column,"Payroll Summary",headerStyle);

            int rowNumber = 2;

            for (AttendanceReviewRow employee : rows) {
                Row row = sheet.createRow(rowNumber++);
                int cellIndex = 0;
                if (employee.insuranceNo() != null) {
                    row.createCell(cellIndex).setCellValue(employee.insuranceNo());
                }

                cellIndex++;
                row.createCell(cellIndex++).setCellValue(nvl(employee.nameEn()));
                row.createCell(cellIndex++).setCellValue(nvl(employee.position()));
                row.createCell(cellIndex++).setCellValue(nvl(employee.employeeCategory()));
                row.createCell(cellIndex++).setCellValue(nvl(employee.team()));
                row.createCell(cellIndex++).setCellValue(nvl(employee.location()));
                cellIndex = attendanceDecimalExcelCell(row, cellIndex, employee.annualLeaveRemaining(), decimalStyle);
                cellIndex = attendanceDecimalExcelCell( row, cellIndex, employee.overusedAnnualLeave(), decimalStyle);
                cellIndex = attendanceDecimalExcelCell( row, cellIndex, employee.specialLeaveRemaining(), decimalStyle);

                cellIndex = attendanceDecimalExcelCell( row, cellIndex, employee.overusedSpecialLeave(), decimalStyle);

                for (LocalDate date : dates) {

                    AttendanceDayCell day = employee.days() == null

                            ? null : employee.days().get(date);

                    // Use the same display code as the Vaadin grid (including ½).
                    row.createCell(cellIndex++).setCellValue(

                            day == null ? "" : attendanceDayCode(day));

                }

                for (BigDecimal value : List.of(

                        zero(employee.presentDays()),

                        zero(employee.absentDays()),

                        zero(employee.annualLeaveDays()),

                        zero(employee.unpaidLeaveDays()),

                        zero(employee.specialLeaveDays()),

                        zero(employee.sickLeaveDays()),

                        zero(employee.maternityLeaveDays()),

                        zero(employee.holidayDays()),

                        zero(holidayCodeDays(employee, "OFF")),

                        zero(holidayCodeDays(employee, "OFF/PMM")),

                        zero(employee.otherDays()))) {

                    cellIndex = attendanceDecimalExcelCell(

                            row, cellIndex, value, decimalStyle);

                }

                for (PayrollRuleRow rule : dynamicRules) {
                    String policyCode = normalizedRuleCode(rule.code());
                    BigDecimal quantity = isHourlyOvertimeAttendanceRule(rule) ? zero(employee.overtimeHours())  : attendancePolicyQuantity(employee, policyCode);
                    cellIndex = attendanceDecimalExcelCell( row, cellIndex, quantity, decimalStyle);

                }
                row.createCell(cellIndex++).setCellValue(employee.unverifiedAttendanceCount());
                row.createCell(cellIndex).setCellValue(attendanceLegendSummary(employee));
            }

            rowNumber++;

            Row legendTitle = sheet.createRow(rowNumber++);

            attendanceMergedExcelHeader(sheet, legendTitle, 0, 1,"Attendance Legend", headerStyle);

            legendTitle.setHeightInPoints(24);

            for (LegendDef legend : attendanceLegends) {

                Row legendRow = sheet.createRow(rowNumber++);
                Cell code = legendRow.createCell(0);
                code.setCellValue(attendanceExcelCode(legend.code()));
                code.setCellStyle(headerStyle);
                Cell description = legendRow.createCell(1);
                description.setCellValue(legend.label());
                description.setCellStyle(legendStyle);

            }

            // Grid freezes only Insurance No. and Employee Name.
            sheet.createFreezePane(2, 2);

            groupHeader.setHeightInPoints(24);

            header.setHeightInPoints(38);

            // Grid has 10 fixed columns before the date columns.
            int attendanceDateStartColumn = 10;

            int attendanceDateEndColumn = attendanceDateStartColumn + dates.size();

            for (int i = 0; i < column; i++) {

                boolean attendanceDateColumn =

                        i >= attendanceDateStartColumn && i < attendanceDateEndColumn;

                if (attendanceDateColumn) {

                    sheet.setColumnWidth(i, 8 * 256);

                    continue;

                }

                sheet.autoSizeColumn(i);

                int maximum = i == column - 1 ? 55 * 256 : 35 * 256;

                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i), maximum));

            }

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException exception) {

            throw new IllegalStateException(

                    "Unable to export payroll attendance to Excel", exception);

        }

    }

    private static int attendanceExcelHeader(

            Row row, int column, String value, CellStyle style) {

        Cell cell = row.createCell(column);

        cell.setCellValue(value);

        cell.setCellStyle(style);

        return column + 1;

    }

    private static int attendanceVerticalExcelHeader(

            Sheet sheet, Row groupHeader, Row header,

            int column, String value, CellStyle style) {

        Cell top = groupHeader.createCell(column);

        top.setCellValue(value);

        top.setCellStyle(style);

        Cell bottom = header.createCell(column);

        bottom.setCellStyle(style);

        sheet.addMergedRegion(new CellRangeAddress(

                groupHeader.getRowNum(), header.getRowNum(), column, column));

        return column + 1;

    }

    private static void attendanceMergedExcelHeader(

            Sheet sheet, Row row, int fromColumn, int toColumn,

            String value, CellStyle style) {

        for (int column = fromColumn; column <= toColumn; column++) {

            Cell cell = row.createCell(column);

            cell.setCellStyle(style);

            if (column == fromColumn) cell.setCellValue(value);

        }

        if (toColumn > fromColumn) {

            sheet.addMergedRegion(new CellRangeAddress(

                    row.getRowNum(), row.getRowNum(), fromColumn, toColumn));

        }

    }

    private static int attendanceDecimalExcelCell(

            Row row, int column, BigDecimal value, CellStyle style) {

        Cell cell = row.createCell(column);

        cell.setCellValue(zero(value).doubleValue());

        cell.setCellStyle(style);

        return column + 1;

    }

    private static BigDecimal zero(BigDecimal value) {

        return value == null ? BigDecimal.ZERO : value;

    }

    private static String attendanceExcelCode(String code) {

        String normalized = normalizedRuleCode(code);

        return normalized != null

                && Set.of("√", "P", "PRESENT", "PR").contains(normalized)

                        ? "√"

                        : nvl(code);

    }

    private static BigDecimal attendancePolicyQuantity(

            AttendanceReviewRow row, String requestedCode) {

        if (row == null || row.policyCodeDays() == null) {

            return BigDecimal.ZERO;

        }

        String expectedCode = normalizedRuleCode(requestedCode);

        if (expectedCode == null) {

            return BigDecimal.ZERO;

        }

        return row.policyCodeDays().entrySet().stream()

                .filter(entry -> expectedCode.equals(

                        normalizedRuleCode(entry.getKey())))

                .map(Map.Entry::getValue)

                .filter(java.util.Objects::nonNull)

                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    private String attendanceLegendSummary(AttendanceReviewRow employee) {

        if (employee == null) {

            return "";

        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();

        if (employee.days() != null) {

            for (AttendanceDayCell day : employee.days().values()) {

                if (day == null) {

                    continue;

                }

                String code = attendanceExcelCode(day.code());

                String normalized = normalizedRuleCode(code);

                if (normalized == null

                        || ATTENDANCE_SUMMARY_EXCLUDED_CODES.contains(normalized)) {

                    continue;

                }

                BigDecimal value = zero(day.dayValue());

                if (value.signum() != 0) {

                    totals.merge(code, value, BigDecimal::add);

                }

            }

        }

        StringBuilder summary = new StringBuilder();

        totals.forEach((code, value) -> {

            if (summary.length() > 0) {

                summary.append(" · ");

            }

            String name = attendanceLegendEnglishName(code);

            summary.append(code);

            if (!name.isBlank()) {

                summary.append(" – ").append(name);

            }

            summary.append(": ").append(decimal(value));

        });

        BigDecimal overtime = zero(employee.overtimeHours());

        if (overtime.signum() > 0) {

            if (summary.length() > 0) {

                summary.append(" · ");

            }

            summary.append("OT – Overtime Hours: ")

                    .append(decimal(overtime))

                    .append(" hrs");

        }

        return summary.toString();

    }

    private String attendanceLegendEnglishName(String code) {

        LegendDef legend = attendanceLegendByCode.get(

                normalizedAttendanceDisplayCode(attendanceExcelCode(code)));

        return legend == null ? "" : englishLabel(legend.label());

    }

    private static CellStyle createAttendanceExcelHeaderStyle(Workbook workbook) {

        Font font = workbook.createFont();

        font.setBold(true);

        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();

        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);

        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setWrapText(true);

        return style;

    }

    private static CellStyle createAttendanceExcelWeekendHeaderStyle(Workbook workbook) {

        Font font = workbook.createFont();

        font.setBold(true);

        font.setColor(IndexedColors.DARK_RED.getIndex());

        CellStyle style = workbook.createCellStyle();

        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);

        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setWrapText(true);

        return style;

    }

    private static CellStyle createAttendanceExcelLegendStyle(Workbook workbook) {

        Font font = workbook.createFont();

        font.setBold(true);

        font.setColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle style = workbook.createCellStyle();

        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.LEFT);

        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setWrapText(true);

        return style;

    }


    private void handleAttendanceDayClick(

            AttendanceReviewRow row, String columnKey) {

        if (row == null || columnKey == null || !columnKey.startsWith("date-")) {

            return;

        }

        try {

            LocalDate attendanceDate = LocalDate.parse(columnKey.substring("date-".length()));

            if (attendanceDayEditService.isLocked(attendanceDate)) {

                showLockedAttendanceMessage(attendanceDate);

            } else {

                openAttendanceDayEditor(row, attendanceDate);

            }

        } catch (Exception ex) {

            error(message(ex));

        }

    }

    private void showLockedAttendanceMessage(LocalDate attendanceDate) {

        Notification notification = Notification.show(

                "Attendance on " + attendanceDate

                        + " cannot be edited here because it is today or an earlier date. "

                        + "If you need to edit it, please use Daily Attendance. "

                        + "| មិនអាចកែប្រែវត្តមាននៅថ្ងៃទី " + attendanceDate

                        + " នៅទីនេះបានទេ ព្រោះវាជាថ្ងៃនេះ ឬថ្ងៃមុន។ "

                        + "ប្រសិនបើត្រូវការកែប្រែ សូមប្រើទំព័រវត្តមានប្រចាំថ្ងៃ។",

                9000,

                Notification.Position.MIDDLE);

        notification.addThemeVariants(NotificationVariant.LUMO_WARNING);

    }

    private void openAttendanceDayEditor(

            AttendanceReviewRow row, LocalDate attendanceDate) {

        Optional<PayrollAttendanceDayEditService.EditData> existing =

                attendanceDayEditService.find(row.empId(), attendanceDate);

        Optional<PayrollAttendanceDayEditService.ShiftDefaults> shiftDefaults =

                attendanceDayEditService.findShiftDefaults(row.empId(), attendanceDate);

        AttendanceDayCell cell = row.days().get(attendanceDate);

        CustomDialog dialog = new CustomDialog(

                "Edit Attendance | កែប្រែវត្តមាន");

        dialog.addClassName("payroll-dialog");

        dialog.setWidth("760px");

        Paragraph employee = new Paragraph(

                "%s · %s · %s".formatted(

                        row.insuranceNo(), nvl(row.nameEn()), attendanceDate));

        employee.getStyle().set("font-weight", "600");

        ComboBox<LeaveType> leaveType =

                new ComboBox<>("Duty / Leave | កាតព្វកិច្ច / ច្បាប់");

        List<LeaveType> leaveTypes =

                leaveTypeRepository.findByObsoleteDateIsNullOrderByLeavTypeCodeAsc();

        leaveType.setItems(leaveTypes);

        leaveType.setItemLabelGenerator(type ->

                "%s · %s · %s".formatted(

                        nvl(type.getLeavTypeCode()),

                        nvl(type.getLeaveNameEn()),

                        nvl(type.getLeaveNameKh())));

        leaveType.setRequired(true);

        leaveType.setWidthFull();

        ComboBox<LeaveTypeSubType> subType =

                new ComboBox<>("Sub Leave | ប្រភេទរង");

        subType.setItemLabelGenerator(type ->

                nvl(type.getLeaveSubTypeNameEn()) + " · "

                        + nvl(type.getLeaveSubTypeNameKh()));

        subType.setWidthFull();

        ComboBox<LeaveDuration> duration =

                new ComboBox<>("Duration | រយៈពេល");

        duration.setItems(LeaveDuration.values());

        duration.setItemLabelGenerator(LeaveDuration::getLabel);

        duration.setValue(LeaveDuration.FULL_DAY);

        duration.setRequired(true);

        TimePicker firstIn = new TimePicker("First In | ចូលដំបូង");

        TimePicker lastOut = new TimePicker("Last Out | ចេញចុងក្រោយ");

        IntegerField breakMinutes = new IntegerField("Break (minutes) | សម្រាក (នាទី)");

        breakMinutes.setMin(0);

        breakMinutes.setStepButtonsVisible(true);

        BigDecimalField normalHours = new BigDecimalField("Normal Hours | ម៉ោងធម្មតា");

        BigDecimalField overtimeHours =

                new BigDecimalField("Overtime Hours | ម៉ោងបន្ថែម");

        overtimeHours.setReadOnly(true);

        TextArea remark = new TextArea("Remarks | កំណត់សម្គាល់");

        remark.setWidthFull();

        Runnable recalculateOvertime = () -> {

            if (firstIn.getValue() == null || lastOut.getValue() == null) {

                overtimeHours.clear();

                return;

            }

            long workedMinutes = Duration.between(

                    firstIn.getValue(), lastOut.getValue()).toMinutes();

            if (workedMinutes < 0) workedMinutes += 24L * 60L;

            if (breakMinutes.getValue() != null) {

                workedMinutes -= Math.max(breakMinutes.getValue(), 0);

            }

            BigDecimal workedHours = BigDecimal.valueOf(Math.max(workedMinutes, 0))

                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            BigDecimal normal = normalHours.getValue() == null

                    ? BigDecimal.ZERO : normalHours.getValue();

            overtimeHours.setValue(workedHours.subtract(normal).max(BigDecimal.ZERO));

        };

        firstIn.addValueChangeListener(event -> recalculateOvertime.run());

        lastOut.addValueChangeListener(event -> recalculateOvertime.run());

        breakMinutes.addValueChangeListener(event -> recalculateOvertime.run());

        normalHours.addValueChangeListener(event -> recalculateOvertime.run());

        Runnable applyShiftByTypeAndDuration = () -> {

            LeaveType selected = leaveType.getValue();

            LeaveDuration selectedDuration = duration.getValue();

            Long groupId = selected == null || selected.getLeaveTypeGroup() == null

                    ? null : selected.getLeaveTypeGroup().getId();

            if (selectedDuration == null || groupId == null) {

                return;

            }

            firstIn.clear();

            lastOut.clear();

            breakMinutes.clear();

            normalHours.clear();

            // Only group 1 is present/working. Every other group is non-working

            // for a full day.

            if (isFullDayNonWorking(selected, selectedDuration)) {

                return;

            }

            shiftDefaults.ifPresent(defaults -> {

                LocalTime selectedFirstIn;

                LocalTime selectedLastOut;

                Integer selectedBreak;

                BigDecimal selectedNormalHours;

                if (!Long.valueOf(1L).equals(groupId)) {

                    // Morning leave means the employee works the afternoon shift.

                    // Afternoon leave means the employee works the morning shift.

                    boolean worksAfternoon =

                            selectedDuration == LeaveDuration.MORNING_ONLY;

                    selectedFirstIn = worksAfternoon

                            ? defaults.afternoonFirstIn() : defaults.morningFirstIn();

                    selectedLastOut = worksAfternoon

                            ? defaults.afternoonLastOut() : defaults.morningLastOut();

                    selectedBreak = worksAfternoon

                            ? defaults.afternoonBreakMinutes() : defaults.morningBreakMinutes();

                    selectedNormalHours = worksAfternoon

                            ? defaults.afternoonNormalHours() : defaults.morningNormalHours();

                } else {

                    // Group 1 means working duty/present.

                    switch (selectedDuration) {

                        case MORNING_ONLY -> {

                            selectedFirstIn = defaults.morningFirstIn();

                            selectedLastOut = defaults.morningLastOut();

                            selectedBreak = defaults.morningBreakMinutes();

                            selectedNormalHours = defaults.morningNormalHours();

                        }

                        case AFTERNOON_ONLY -> {

                            selectedFirstIn = defaults.afternoonFirstIn();

                            selectedLastOut = defaults.afternoonLastOut();

                            selectedBreak = defaults.afternoonBreakMinutes();

                            selectedNormalHours = defaults.afternoonNormalHours();

                        }

                        case FULL_DAY -> {

                            selectedFirstIn = defaults.firstIn();

                            selectedLastOut = defaults.lastOut();

                            selectedBreak = defaults.breakMinutes();

                            selectedNormalHours = defaults.normalHours();

                        }

                        default -> throw new IllegalStateException(

                                "Unsupported attendance duration: " + selectedDuration);

                    }

                }

                if (selectedFirstIn != null) firstIn.setValue(selectedFirstIn);

                if (selectedLastOut != null) lastOut.setValue(selectedLastOut);

                if (selectedBreak != null) breakMinutes.setValue(selectedBreak);

                if (selectedNormalHours != null) normalHours.setValue(selectedNormalHours);

            });

        };

        leaveType.addValueChangeListener(event -> {

            LeaveType selected = event.getValue();

            if (selected == null) {

                subType.clear();

                subType.setItems(List.of());

                return;

            }

            List<LeaveTypeSubType> subTypes = leaveTypeSubTypeRepository

                    .findByLeaveTypeIdOrderByLeaveSubTypeNameEnAsc(selected.getId())

                    .stream()

                    .filter(item -> item.getObsoleteDate() == null)

                    .toList();

            subType.setItems(subTypes);

            if (subType.getValue() != null

                    && subTypes.stream().noneMatch(item ->

                            item.getId().equals(subType.getValue().getId()))) {

                subType.clear();

            }

            applyShiftByTypeAndDuration.run();

        });

        duration.addValueChangeListener(event -> applyShiftByTypeAndDuration.run());

        Long selectedLeaveTypeId = existing

                .map(PayrollAttendanceDayEditService.EditData::leaveTypeId)

                .orElse(null);

        if (selectedLeaveTypeId == null && cell != null) {

            String cellCode = normalizedRuleCode(cell.code());

            if (cellCode != null && Set.of("P", "PRESENT", "√").contains(cellCode)) {

                cellCode = "PR";

            } else if ("W".equals(cellCode)) {

                cellCode = "WV";

            }

            final String desiredCode = cellCode;

            selectedLeaveTypeId = leaveTypes.stream()

                    .filter(type -> desiredCode != null

                            && desiredCode.equals(normalizedRuleCode(type.getLeavTypeCode())))

                    .map(LeaveType::getId)

                    .findFirst().orElse(null);

        }

        final Long initialLeaveTypeId = selectedLeaveTypeId;

        leaveTypes.stream()

                .filter(type -> type.getId().equals(initialLeaveTypeId))

                .findFirst().ifPresent(leaveType::setValue);

        existing.ifPresent(data -> {

            duration.setValue(data.leaveDuration() == null

                    ? LeaveDuration.FULL_DAY : data.leaveDuration());

            if (!isFullDayNonWorking(leaveType.getValue(), duration.getValue())) {

                if (data.firstIn() != null) {

                    firstIn.setValue(data.firstIn());

                }

                if (data.lastOut() != null) {

                    lastOut.setValue(data.lastOut());

                }

                if (data.breakMinutes() != null) {

                    breakMinutes.setValue(data.breakMinutes());

                }

                if (data.normalHours() != null) {

                    normalHours.setValue(data.normalHours());

                }

            }

            remark.setValue(nvl(data.remark()));

            if (data.leaveSubTypeId() != null) {

                leaveTypeSubTypeRepository.findById(data.leaveSubTypeId())

                        .ifPresent(subType::setValue);

            }

        });

        FormLayout form = new FormLayout(

                leaveType, subType, duration, firstIn, lastOut,

                breakMinutes, normalHours, overtimeHours, remark);

        form.setColspan(leaveType, 2);

        form.setColspan(subType, 2);

        form.setColspan(remark, 2);

        dialog.add(employee, form);

        Button cancel = new Button("Cancel | បោះបង់", event -> dialog.close());

        Button save = new Button("Save | រក្សាទុក", event -> {

            if (leaveType.getValue() == null) {

                leaveType.setInvalid(true);

                leaveType.setErrorMessage("Duty / Leave is required.");

                return;

            }

            try {

                attendanceDayEditService.save(

                        row.empId(), attendanceDate,

                        new PayrollAttendanceDayEditService.EditCommand(

                                leaveType.getValue().getId(),

                                subType.getValue() == null ? null : subType.getValue().getId(),

                                duration.getValue(), firstIn.getValue(), lastOut.getValue(),

                                breakMinutes.getValue(), normalHours.getValue(), remark.getValue()));

                dialog.close();

                refreshAttendanceReviewRows();

                success("Attendance saved. Approved payroll snapshot remains frozen. "

                        + "| បានរក្សាទុកវត្តមាន។ ទិន្នន័យប្រាក់បៀវត្សដែលបានអនុម័តមិនផ្លាស់ប្តូរ។");

            } catch (Exception ex) {

                error(message(ex));

            }

        });

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancel, save);

        dialog.open();

    }

    private void updateAttendanceReviewActions() {

        PeriodRow period = attendancePeriodFilter.getValue();

        boolean editable = authenticatedUser.hasPermissionRoute(PayrollActionPermissions.ATTENDANCE_LOCK, AccessPageType.UPDATED_PAGE)

                && period != null && !PayrollPeriodStatus.CLOSED.matches(period.status());

        attendanceReviewApproveButton.setEnabled(editable

                && (attendanceReviewControl == null || !attendanceReviewControl.approvedAndCurrent()));

        attendanceReviewApproveButton.setTooltipText(

                "Check attendance, validate all payroll attendance requirements, then approve and lock in one action. "
                        + "| ពិនិត្យវត្តមាន ផ្ទៀងផ្ទាត់លក្ខខណ្ឌទាំងអស់ បន្ទាប់មកអនុម័ត និងចាក់សោក្នុងសកម្មភាពតែមួយ។");

        attendanceReviewReopenButton.setEnabled(editable

                && attendanceReviewControl != null

                && PayrollAttendanceControlStatus.APPROVED.matches(attendanceReviewControl.status()));

    }


    private void renderAttendanceLegend(List<PayrollRuleRow> activeRules) {

        attendanceReviewLegend.removeAll();

        attendanceLegends.forEach(item -> attendanceReviewLegend.add(

                attendanceLegendItem(item.code(), englishLabel(item.label()), item.color())));

        Set<String> baseCodes = new java.util.HashSet<>();

        attendanceLegends.forEach(item -> baseCodes.add(

                normalizedAttendanceDisplayCode(item.code())));

        Map<String, PayrollRuleRow> rulesByCode = new LinkedHashMap<>();

        if (activeRules != null) {

            activeRules.stream()

                    .filter(PayrollRuleRow::active)

                    .filter(PayrollAttendancePanel::displayInAttendanceLegend)

                    .forEach(rule -> {

                        String code = normalizedRuleCode(rule.code());

                        if (code != null && !baseCodes.contains(

                                normalizedAttendanceDisplayCode(code))) {

                            rulesByCode.putIfAbsent(code, rule);

                        }

                    });

        }

        rulesByCode.forEach((code, rule) -> attendanceReviewLegend.add(

                attendanceLegendItem(

                        code, attendanceRuleLegendLabel(rule), null)));

    }

    private static List<PayrollRuleRow> attendanceReviewDynamicRules(

            List<PayrollRuleRow> activeRules) {

        Set<String> categoryColumns = Set.of(

                "P", "A", "AL", "UL", "SL", "S", "ML", "H");

        return attendancePayrollRules(activeRules).stream()

                .filter(rule -> {

                    String policyCode = normalizedRuleCode(rule.code());

                    return policyCode != null

                            && !categoryColumns.contains(policyCode);

                })

                .toList();

    }

    private static boolean isHourlyOvertimeAttendanceRule(PayrollRuleRow rule) {

        return rule != null

                && "OVERTIME".equals(rule.ruleType())

                && "HOUR".equals(normalizedRuleCode(rule.rateUnit()));

    }

    private static List<PayrollRuleRow> attendancePayrollRules(List<PayrollRuleRow> rules) {

        if (rules == null) {

            return List.of();

        }

        Map<String, PayrollRuleRow> rulesByCode = new LinkedHashMap<>();

        rules.stream()

                .filter(PayrollRuleRow::active)

                .filter(rule -> "ATTENDANCE".equals(normalizedRuleCode(

                        rule.calculationSource())))

                .filter(rule -> Set.of("DEDUCTION", "OVERTIME", "ALLOWANCE")

                        .contains(normalizedRuleCode(rule.ruleType())))

                .forEach(rule -> rulesByCode.put(normalizedRuleCode(rule.code()), rule));

        return new ArrayList<>(rulesByCode.values());

    }

    private static boolean displayInAttendanceLegend(PayrollRuleRow rule) {

        String source = normalizedRuleCode(rule.calculationSource());

        String type = normalizedRuleCode(rule.ruleType());

        return "ATTENDANCE".equals(source)

                && Set.of("DEDUCTION", "OVERTIME", "ALLOWANCE", "LEAVE_ENTITLEMENT")

                        .contains(type)

                || "LEAVE".equals(source)

                && Set.of("LEAVE_ENTITLEMENT", "LEAVE_PAY").contains(type);

    }

    private static String attendanceRuleLegendLabel(PayrollRuleRow rule) {

        String code = normalizedRuleCode(rule.code());

        return rule.name() == null || rule.name().isBlank()

                ? code

                : rule.name().trim();

    }

    private static String normalizedRuleCode(String value) {

        if (value == null || value.isBlank()) {

            return null;

        }

        return value.trim().toUpperCase(Locale.ROOT);

    }

    private static boolean isFullDayNonWorking(

            LeaveType leaveType, LeaveDuration duration) {

        if (leaveType == null || duration != LeaveDuration.FULL_DAY) {

            return false;

        }

        Long groupId = leaveType.getLeaveTypeGroup() == null

                ? null : leaveType.getLeaveTypeGroup().getId();

        return !Long.valueOf(1L).equals(groupId);

    }

    private static Div attendanceLegendItem(String code, String label, String color) {

        Div item = new Div();

        item.addClassName("payroll-attendance-legend-item");

        item.getElement().setAttribute("title", code + " - " + label);

        Span badge = new Span(code);

        badge.addClassName("attendance-day-badge");

        for (String cssClass : attendanceBadgeCss(code).split("\\s+")) {

            if (!cssClass.isBlank()) {

                badge.addClassName(cssClass);

            }

        }

        badge.getStyle().set("--attendance-custom-accent", validAttendanceAccent(color));

        item.add(badge, new Span(label));

        return item;

    }

    private static List<LegendDef> loadAttendanceLegends(LeaveTypeRepository repository) {

        Map<String, LegendDef> definitions = new LinkedHashMap<>();

        for (LeaveType type : repository.findByObsoleteDateIsNullOrderByLeavTypeCodeAsc()) {

            String databaseCode = normalizedRuleCode(type.getLeavTypeCode());

            if (databaseCode == null) continue;

            String displayCode = Set.of("P", "PR", "PRESENT").contains(databaseCode)

                    ? "√" : databaseCode;

            definitions.putIfAbsent(displayCode, new LegendDef(displayCode,

                    bilingualLabel(type.getLeaveNameEn(), type.getLeaveNameKh()),

                    validAttendanceAccent(type.getLegendColor())));

        }

        definitions.putIfAbsent("√", new LegendDef("√", "Present | វត្តមាន", null));

        definitions.put("½", new LegendDef("½", "Half Day | កន្លះថ្ងៃ", null));

        definitions.put("!", new LegendDef("!", "Issue | បញ្ហា", null));

        return List.copyOf(definitions.values());

    }

    private static Map<String, LegendDef> indexAttendanceLegends(

            List<LegendDef> legends) {

        Map<String, LegendDef> index = new LinkedHashMap<>();

        if (legends != null) {

            for (LegendDef legend : legends) {

                if (legend == null) {

                    continue;

                }

                index.putIfAbsent(

                        normalizedAttendanceDisplayCode(legend.code()), legend);

            }

        }

        return Map.copyOf(index);

    }

    private static String bilingualLabel(String english, String khmer) {

        String en = nvl(english).trim();

        String kh = nvl(khmer).trim();

        if (en.isBlank()) return kh;

        if (kh.isBlank()) return en;

        return en + " | " + kh;

    }

    private static String englishLabel(String label) {

        String value = nvl(label).trim();

        int separator = value.indexOf('|');

        return separator < 0 ? value : value.substring(0, separator).trim();

    }

    private static String validAttendanceAccent(String color) {

        String value = nvl(color).trim().toUpperCase(Locale.ROOT);

        return value.matches("^#[0-9A-F]{6}$")

                ? value

                : "var(--lumo-contrast-50pct)";

    }

    private static String normalizedAttendanceDisplayCode(String code) {

        String value = nvl(code).replace("½", "").trim().toUpperCase(Locale.ROOT);

        return Set.of("P", "PR", "PRESENT", "√").contains(value) ? "√" : value;

    }

    private static String attendanceBadgeCss(String code) {

        String raw = nvl(code).trim();

        if (raw.contains("½")) {

            return "attendance-code-other attendance-code-half";

        }

        String normalized = normalizedAttendanceDisplayCode(raw);

        return switch (normalized) {

            case "√" -> "attendance-code-present";

            case "AL" -> "attendance-code-annual";

            case "SL" -> "attendance-code-special";

            case "S" -> "attendance-code-sick";

            case "UL" -> "attendance-code-unpaid";

            case "A" -> "attendance-code-absent";

            case "ML" -> "attendance-code-maternity";

            case "H", "OFF", "OFF/PM", "OFF/PMM" -> "attendance-code-holiday";

            case "!" -> "attendance-code-issue";

            default -> "attendance-code-other";

        };

    }

    private String attendanceAccent(String code) {

        LegendDef legend = attendanceLegendByCode.get(

                normalizedAttendanceDisplayCode(code));

        return legend == null

                ? "var(--lumo-contrast-50pct)"

                : validAttendanceAccent(legend.color());

    }

    private static Div attendanceDayHeader(LocalDate date) {

        Div header = new Div();

        header.addClassName("payroll-attendance-day-header");

        if (date.getDayOfWeek().getValue() >= 6) {

            header.addClassName("weekend");

        }

        Span day = new Span(Integer.toString(date.getDayOfMonth()));

        day.addClassName("payroll-attendance-day-number");

        Span weekday = new Span(

                date.getDayOfWeek().name().substring(0, 3) + "/" + khmerWeekdayShort(date));

        weekday.addClassName("payroll-attendance-weekday");

        header.add(day, weekday);

        return header;

    }

    private static Div attendanceMonthHeader(YearMonth month) {

        Div header = new Div();

        header.addClassName("payroll-attendance-month-header");

        header.setText(month.format(

                DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH))

                + " | " + khmerMonth(month.getMonthValue()) + " " + month.getYear());

        return header;

    }

    private static String attendanceDayCode(AttendanceDayCell cell) {

        if (cell == null) {

            return "";

        }

        // Only the database attendance code PR is rendered as the present symbol.

        // Holiday codes such as Off and Off/PMM must remain visible as themselves.

        String rawCode = nvl(cell.code()).trim();

        boolean present = "PR".equalsIgnoreCase(rawCode);

        String code = present ? "√" : rawCode;

        if (cell.dayValue() != null && cell.dayValue().compareTo(new BigDecimal("0.5")) == 0) {

            return code + "½";

        }

        return code;

    }

    private static String attendanceDayCss(AttendanceDayCell cell) {

        if (cell == null) {

            return "attendance-code-empty";

        }

        String rawCode = normalizedRuleCode(cell.code());

        String css = rawCode != null && Set.of("OFF", "OFF/PMM").contains(rawCode)

                ? "attendance-code-holiday"

                : switch (nvl(cell.category())) {

            case "P" -> "attendance-code-present";

            case "AL" -> "attendance-code-annual";

            case "SL" -> "attendance-code-special";

            case "S" -> "attendance-code-sick";

            case "UL" -> "attendance-code-unpaid";

            case "A" -> "attendance-code-absent";

            case "ML" -> "attendance-code-maternity";

            case "H" -> "attendance-code-holiday";

            default -> "attendance-code-other";

        };

        if (cell.dayValue() != null && cell.dayValue().compareTo(new BigDecimal("0.5")) == 0) {

            css += " attendance-code-half";

        }

        if (cell.issue()) {

            css += " attendance-day-issue";

        }

        return css;

    }

    private String attendanceDayCustomAccent(AttendanceDayCell cell) {

        if (cell == null) {

            return "var(--lumo-contrast-50pct)";

        }

        return attendanceAccent(cell.code());

    }

    private static String attendanceDayTooltip(AttendanceDayCell cell) {

        if (cell == null) {

            return "No roster record | មិនមានកំណត់ត្រាតារាងវេន";

        }

        StringBuilder text = new StringBuilder()

                .append(cell.date()).append(" · ").append(nvl(cell.code()))

                .append(" · ").append(nvl(cell.duration()))

                .append(" · Source | ប្រភព: ").append(nvl(cell.source()));

        if (cell.overtimeHours() != null && cell.overtimeHours().signum() > 0) {

            text.append(" · OT | ម៉ោងបន្ថែម: ").append(decimal(cell.overtimeHours()));

        }

        if (!cell.hrVerified()) {

            text.append(" · Waiting HR verification | រង់ចាំ HR ផ្ទៀងផ្ទាត់");

        }

        if (cell.duplicateCount() > 1) {

            text.append(" · ").append(cell.duplicateCount())

                    .append(" duplicate records | កំណត់ត្រាស្ទួន");

        }

        if (cell.remarks() != null && !cell.remarks().isBlank()) {

            text.append(" · ").append(cell.remarks());

        }

        return text.toString();

    }

    private void confirmAttendanceReviewApproval() {

        PeriodRow period = attendancePeriodFilter.getValue();

        if (period == null) {

            error("Select a payroll period first. "

                    + "| សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        LocalDate cutoffDate = LocalDate.now(ZoneId.of("Asia/Phnom_Penh"));

        if (cutoffDate.isAfter(period.endDate())) cutoffDate = period.endDate();

        PayrollConfirmDialog confirm = confirm("Approve and Lock Attendance | អនុម័ត និងចាក់សោវត្តមាន",

                ("Save the complete employee-by-day attendance snapshot for %04d-%02d and lock attendance through %s? "

                        + "Attendance after the cutoff date remains editable. "

                        + "| រក្សាទុកទិន្នន័យវត្តមានតាមបុគ្គលិក និងតាមថ្ងៃសម្រាប់ %04d-%02d "

                        + "និងចាក់សោរហូតដល់ %s មែនទេ? វត្តមានក្រោយថ្ងៃកំណត់នៅតែអាចកែប្រែបាន។")

                                .formatted(period.year(), period.month(), cutoffDate,

                                        period.year(), period.month(), cutoffDate));

        confirm.setConfirmText("Approve & Lock | អនុម័ត និងចាក់សោ");

        confirm.addConfirmListener(e -> executeAttendanceReviewAction(

                () -> service.approveAttendance(period.id(), null),

                "Attendance snapshot saved. Attendance through the cutoff date is locked. "

                        + "| បានរក្សាទុកទិន្នន័យវត្តមាន។ វត្តមានរហូតដល់ថ្ងៃកំណត់ត្រូវបានចាក់សោ។",

                "Approve & Lock Attendance | កំពុងអនុម័ត និងចាក់សោវត្តមាន",

                "Checking attendance, saving the payroll snapshot, and applying the lock... "

                        + "| កំពុងរក្សាទុកទិន្នន័យវត្តមាន និងអនុវត្តការចាក់សោ..."));

        confirm.open();

    }

    private void confirmAttendanceReviewReopen() {

        PeriodRow period = attendancePeriodFilter.getValue();

        if (period == null) {

            error("Select a payroll period first. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        PayrollConfirmDialog confirm = confirm("Reopen Attendance | បើកវត្តមានឡើងវិញ",

                "Reopen this attendance period for correction? Payroll processing will be blocked until attendance is checked and approved again. "

                        + "| បើករយៈពេលវត្តមាននេះឡើងវិញដើម្បីកែតម្រូវមែនទេ? ដំណើរការប្រាក់បៀវត្សនឹងត្រូវបានរារាំង "

                        + "រហូតដល់វត្តមានត្រូវបានពិនិត្យ និងអនុម័តម្ដងទៀត។");

        confirm.setConfirmText("Reopen | បើកឡើងវិញ");

        confirm.addConfirmListener(e -> executeAttendanceReviewAction(

                () -> service.reopenAttendance(period.id(),

                        "Reopened from attendance review | បើកឡើងវិញពីការពិនិត្យវត្តមាន"),

                "Attendance reopened for correction. | បានបើកវត្តមានឡើងវិញដើម្បីកែតម្រូវ។",

                "Reopen Attendance | កំពុងបើកវត្តមានឡើងវិញ",

                "Reopening attendance for correction... | កំពុងបើកវត្តមានឡើងវិញសម្រាប់កែតម្រូវ..."));

        confirm.open();

    }

    private void executeAttendanceReviewAction(

            Runnable action,

            String successMessage,

            String progressTitle,

            String progressMessage) {

        PeriodRow period = attendancePeriodFilter.getValue();

        if (period == null) {

            error("Select a payroll period first. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សជាមុនសិន។");

            return;

        }

        String search = attendanceReviewSearch.getValue();

        boolean issuesOnly = attendanceIssuesOnly.getValue();

        runAttendanceBackgroundTask(

                progressTitle,

                progressMessage,

                () -> {

                    action.run();

                    return loadAttendanceReviewData(period, search, issuesOnly);

                },

                data -> {

                    applyAttendanceReviewData(data);

                    Long processingPeriodId = refreshCoordinator.processingPeriodId();

                    if (processingPeriodId != null

                            && period.id().equals(processingPeriodId)) {

                        refreshCoordinator.refreshProcessing();

                    }

                    success(successMessage);

                });

    }

    private void refreshAttendanceReviewWithProgress() {

        PeriodRow period = attendancePeriodFilter.getValue();

        if (period == null) {

            refreshAttendanceReview();

            return;

        }

        String search = attendanceReviewSearch.getValue();

        boolean issuesOnly = attendanceIssuesOnly.getValue();

        runAttendanceBackgroundTask(

                "Refresh Attendance | កំពុងផ្ទុកវត្តមានឡើងវិញ",

                "Loading attendance, payroll rules, and control status... "

                        + "| កំពុងផ្ទុកវត្តមាន ច្បាប់ប្រាក់បៀវត្ស និងស្ថានភាពគ្រប់គ្រង...",

                () -> loadAttendanceReviewData(period, search, issuesOnly),

                this::applyAttendanceReviewData);

    }

    private void refreshAttendanceReviewRowsWithProgress(

            String progressTitle, String progressMessage) {

        PeriodRow period = attendancePeriodFilter.getValue();

        if (period == null) {

            refreshAttendanceReviewRows();

            return;

        }

        String search = attendanceReviewSearch.getValue();

        boolean issuesOnly = attendanceIssuesOnly.getValue();

        runAttendanceBackgroundTask(

                progressTitle,

                progressMessage,

                () -> service.findAttendanceReview(

                        period.id(), search, issuesOnly),

                rows -> {

                    if (!sameAttendanceRequest(period, search, issuesOnly)) {

                        return;

                    }

                    applyAttendanceReviewRows(period, rows);

                });

    }

    private AttendanceReviewData loadAttendanceReviewData(

            PeriodRow period, String search, boolean issuesOnly) {

        List<PayrollRuleRow> activeRules = service.findPayrollRules(period.year()).stream()

                .filter(PayrollRuleRow::active)

                .toList();

        AttendanceControlRow control = service.findAttendanceControl(period.id());

        List<AttendanceReviewRow> rows = service.findAttendanceReview(

                period.id(), search, issuesOnly);

        return new AttendanceReviewData(

                period, search, issuesOnly, activeRules, control, List.copyOf(rows));

    }

    private void applyAttendanceReviewData(AttendanceReviewData data) {

        if (!sameAttendanceRequest( data.period(), data.search(), data.issuesOnly())) {
            return;
        }

        configureAttendanceReviewGrid(data.period(), data.activeRules());

        renderAttendanceLegend(data.activeRules());

        attendanceReviewControl = data.control();

        applyAttendanceReviewRows(data.period(), data.rows());

        updateAttendanceReviewActions();

    }

    private void applyAttendanceReviewRows(
        PeriodRow period, List<AttendanceReviewRow> rows) {
        attendanceReviewRows = List.copyOf(rows);
        attendanceReviewGrid.setItems(rows);
        long issueEmployees = rows.stream().filter(AttendanceReviewRow::hasIssues).count();
        long unverified = rows.stream().mapToLong(AttendanceReviewRow::unverifiedAttendanceCount).sum();
        attendanceReviewSummary.setText(("%s → %s , %d employee(s). %d employee(s) with issues "+ ", %d unverified ") .formatted(period.startDate(), period.endDate(), rows.size(),  issueEmployees, unverified));
    }

    private boolean sameAttendanceRequest(
        PeriodRow period, String search, boolean issuesOnly) {
        PeriodRow selected = attendancePeriodFilter.getValue();
        return selected != null  && selected.id().equals(period.id()) && Objects.equals(attendanceReviewSearch.getValue(), search)  && attendanceIssuesOnly.getValue() == issuesOnly;
    }

    private <T> void runAttendanceBackgroundTask(String progressTitle,String progressMessage, Supplier<T> task, Consumer<T> onSuccess) {
        ProgressDialog.runAsync(progressTitle, progressMessage,task, onSuccess, ex -> error(message(ex)));
    }

    private record AttendanceReviewData( PeriodRow period, String search,boolean issuesOnly, List<PayrollRuleRow> activeRules, AttendanceControlRow control, List<AttendanceReviewRow> rows) { }
    private record LegendDef(String code, String label, String color) { }
}
