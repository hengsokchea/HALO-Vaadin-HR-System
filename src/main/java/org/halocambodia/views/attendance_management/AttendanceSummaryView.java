package org.halocambodia.views.attendance_management;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeAttendanceDetail;
import org.halocambodia.data.EmployeeAttendanceDetailTeam;
import org.halocambodia.data.LeaveType;
import org.halocambodia.data.LeaveTypeRepository;
import org.halocambodia.enums.AttendanceEntryStatus;
import org.halocambodia.enums.AttendanceQcStatus;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.AttendanceSummaryService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import jakarta.annotation.security.PermitAll;

@Route(value = "attendance-summary", layout = MainLayout.class)
@PageTitle("Attendance Summary | សង្ខេបវត្តមាន")
@PermitAll
public class AttendanceSummaryView extends VerticalLayout implements BeforeEnterObserver {
    private static final DateTimeFormatter MONTH_FORMAT =DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static volatile List<LegendDef> LEGENDS = List.of(
            new LegendDef("√", "Present", "#16A34A"),
            new LegendDef("½", "Half Day", "#475569"),
            new LegendDef("!", "Pending Entry, QC or HR", "#DB2777"));

    private final AttendanceSummaryService service;
    private final AuthenticatedUser authenticatedUser;
    private final DatePicker fromDate = new DatePicker("From Date | ចាប់ពីថ្ងៃ");
    private final DatePicker toDate = new DatePicker("To Date | ដល់ថ្ងៃ");
    private final TextField search = new TextField("Search | ស្វែងរក");
    private final MultiSelectComboBox<String> locationFilter =new MultiSelectComboBox<>("Location | ទីតាំង");
    private final MultiSelectComboBox<String> teamFilter = new MultiSelectComboBox<>("Team | ក្រុម");
    private final ComboBox<WorkflowStatus> statusFilter = new ComboBox<>("Status | ស្ថានភាព");
    private final Grid<EmployeeRow> grid = new Grid<>();
    private final Button deleteSelected = new Button(
            "Delete Selected | លុបដែលបានជ្រើស", VaadinIcon.TRASH.create());
    private final Span employees = createCard("Employees | បុគ្គលិក", "#2563eb", "#eff6ff");
    private final Span records = createCard("Records | កំណត់ត្រា", "#0891b2", "#ecfeff");
    private final Span present = createCard("Present | វត្តមាន", "#16a34a", "#f0fdf4");
    private final Span leave = createCard("Leave | ច្បាប់", "#7c3aed", "#f5f3ff");
    private final Span absent = createCard("Absent | អវត្តមាន", "#dc2626", "#fef2f2");
    private final Span pendingQc = createCard("Pending QC | រង់ចាំ QC", "#ea580c", "#fff7ed");
    private final Span pendingHr = createCard("Pending HR | រង់ចាំ HR", "#ca8a04", "#fefce8");
    private final Span finalized = createCard("Finalized | បានបញ្ចប់", "#0f766e", "#f0fdfa");
    private List<EmployeeRow> allRows = List.of();
    private List<EmployeeRow> filteredRows = List.of();
    private HeaderRow monthHeader;
    private LocalDate configuredStart;
    private LocalDate configuredEnd;

    public AttendanceSummaryView(AttendanceSummaryService service,
            AuthenticatedUser authenticatedUser,
            LeaveTypeRepository leaveTypeRepository) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
        LEGENDS = loadLegends(leaveTypeRepository);
        configurePage();
        configureFilters();
        configureGrid(null, null);
    }

    private static List<LegendDef> loadLegends(LeaveTypeRepository repository) {
        Map<String, LegendDef> definitions = new LinkedHashMap<>();
        for (LeaveType type : repository.findByObsoleteDateIsNullOrderByLeavTypeCodeAsc()) {
            String databaseCode = safe(type.getLeavTypeCode()).toUpperCase(Locale.ROOT);
            if (databaseCode.isBlank()) {
                continue;
            }
            // Present is stored as PR, but the attendance UI displays √.
            String displayCode = Set.of("P", "PR", "PRESENT").contains(databaseCode)
                    ? "√" : databaseCode;
            String label = bilingualLabel(type.getLeaveNameEn(), type.getLeaveNameKh());
            String color = validLegendColor(type.getLegendColor());
            definitions.putIfAbsent(displayCode,
                    new LegendDef(displayCode, label, color));
        }
        definitions.putIfAbsent("√", new LegendDef("√", "Present", "#16A34A"));
        definitions.put("½", new LegendDef("½", "Half Day", "#475569"));
        definitions.put("!", new LegendDef("!",
                "Pending Entry, QC or HR | រង់ចាំបញ្ចូលទិន្នន័យ QC ឬ HR", "#DB2777"));
        return List.copyOf(definitions.values());
    }

    private static String bilingualLabel(String english, String khmer) {
        String en = safe(english);
        String kh = safe(khmer);
        if (en.isBlank()) return kh;
        if (kh.isBlank()) return en;
        return en + " | " + kh;
    }

    private static String validLegendColor(String color) {
        String normalized = safe(color).toUpperCase(Locale.ROOT);
        return normalized.matches("^#[0-9A-F]{6}$") ? normalized : "#78716C";
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(AttendanceSummaryView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        } else {
            refresh();
        }
    }

    private void configurePage() {
        setSizeFull();
        setPadding(true);
        setSpacing(false);
        Button refresh = new Button("Refresh", VaadinIcon.REFRESH.create(), e -> refresh());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        boolean canDelete = canDelete();
        deleteSelected.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteSelected.setVisible(canDelete);
        deleteSelected.setEnabled(false);
        deleteSelected.addClickListener(e -> confirmDeleteSelected());

        Anchor exportExcel = createExcelExport();
        HorizontalLayout filters = new HorizontalLayout(fromDate, toDate, search,
                locationFilter, teamFilter, statusFilter, refresh, deleteSelected, exportExcel);
        filters.setWidthFull();
        filters.setWrap(true);
        // Fields have labels above their controls. Aligning to END keeps the
        // buttons level with the input boxes instead of centering them against
        // the combined label + input height.
        filters.setAlignItems(Alignment.END);
        filters.setJustifyContentMode(JustifyContentMode.START);

        Div cards = new Div();
        cards.add(employees, records, present, leave, absent, pendingQc, pendingHr, finalized);
        cards.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(155px, 1fr))")
                .set("gap", "var(--lumo-space-s)")
                .set("width", "100%")
                .set("margin", "var(--lumo-space-m) 0");
        add(filters, cards, grid, createLegend());
        setFlexGrow(1, grid);
    }

    private void configureFilters() {
        LocalDate today = LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        fromDate.setValue(today.withDayOfMonth(1));
        toDate.setValue(today);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setPlaceholder("Insurance or employee name");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        locationFilter.setPlaceholder("All allowed locations | គ្រប់ទីតាំង");
        locationFilter.setClearButtonVisible(true);
        teamFilter.setPlaceholder("All teams | គ្រប់ក្រុម");
        teamFilter.setClearButtonVisible(true);
        statusFilter.setItems(WorkflowStatus.values());
        statusFilter.setItemLabelGenerator(WorkflowStatus::label);
        statusFilter.setValue(WorkflowStatus.FINALIZED);
        fromDate.addValueChangeListener(e -> refresh());
        toDate.addValueChangeListener(e -> refresh());
        search.addValueChangeListener(e -> applyFilter());
        locationFilter.addValueChangeListener(e -> applyFilter());
        teamFilter.addValueChangeListener(e -> applyFilter());
        statusFilter.addValueChangeListener(e -> applyFilter());
    }

    private void configureGrid(LocalDate start, LocalDate end) {
        if (monthHeader != null) { grid.removeHeaderRow(monthHeader); monthHeader = null; }
        grid.removeAllColumns();
        grid.setSelectionMode(Grid.SelectionMode.MULTI).addSelectionListener(event ->
                deleteSelected.setEnabled(canDelete() && !event.getAllSelectedItems().isEmpty()));
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grid.addColumn(EmployeeRow::insuranceNo).setHeader("Ins. | លេខធានា")
                .setFrozen(true).setWidth("95px").setFlexGrow(0).setSortable(true);
        grid.addColumn(EmployeeRow::nameEn).setHeader("Name EN | ឈ្មោះអង់គ្លេស")
                .setFrozen(true).setWidth("220px").setFlexGrow(0).setSortable(true);
        grid.addColumn(EmployeeRow::nameKh).setHeader("Name KH | ឈ្មោះខ្មែរ")
                .setWidth("220px").setFlexGrow(0).setSortable(true);
        grid.addColumn(EmployeeRow::position).setHeader("Position | មុខតំណែង")
                .setWidth("190px").setFlexGrow(0);
        grid.addColumn(EmployeeRow::location).setHeader("Location | ទីតាំង")
                .setWidth("160px").setFlexGrow(0).setResizable(true);
        grid.addColumn(EmployeeRow::teams).setHeader("Team | ក្រុម")
                .setWidth("180px").setFlexGrow(0).setResizable(true);

        Map<YearMonth, List<Grid.Column<EmployeeRow>>> months = new LinkedHashMap<>();
        if (start != null && end != null) {
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                LocalDate day = d;
                Grid.Column<EmployeeRow> column = grid
                        .addColumn(new ComponentRenderer<>(row -> dayBadge(row, day)))
                        .setHeader(dayHeader(day)).setAutoWidth(true).setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER).setResizable(true);
                months.computeIfAbsent(YearMonth.from(day), x -> new ArrayList<>()).add(column);
            }
        }
        List<Grid.Column<EmployeeRow>> countColumns = new ArrayList<>();
        for (LegendDef legend : LEGENDS) {
            // Keep these indicators in the legend/day cells, but do not create
            // separate total columns for them.
            if (Set.of("½", "!").contains(legend.code())) {
                continue;
            }
            Grid.Column<EmployeeRow> column = grid
                    .addColumn(row -> formatCount(row.legendCounts()
                            .getOrDefault(legend.code(), 0.0)))
                    .setHeader(countHeader(legend))
                    .setWidth(legend.code().length() > 4 ? "95px" : "70px")
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.END)
                    .setResizable(true);
            countColumns.add(column);
        }
        Grid.Column<EmployeeRow> s = grid.addColumn(EmployeeRow::summaryStatus)
                .setHeader("Status | ស្ថានភាព").setWidth("180px").setFlexGrow(0);

        // Allow users to resize every column: employee information, daily
        // attendance, legend totals, and workflow status.
        grid.getColumns().forEach(column -> column.setResizable(true));

        if (!months.isEmpty()) {
            monthHeader = grid.prependHeaderRow();
            months.forEach((month, columns) -> {
                @SuppressWarnings("unchecked")
                Grid.Column<EmployeeRow>[] array = columns.toArray(Grid.Column[]::new);
                monthHeader.join(array).setComponent(monthHeader(month));
            });
            @SuppressWarnings("unchecked")
            Grid.Column<EmployeeRow>[] countArray =
                    countColumns.toArray(Grid.Column[]::new);
            monthHeader.join(countArray)
                    .setComponent(groupHeader("Legend Counts | ចំនួនតាមនិមិត្តសញ្ញា"));
        }
    }

    private Span dayBadge(EmployeeRow row, LocalDate date) {
        DayCell cell = row.days().get(date);
        Span badge = new Span(cell == null ? "" : cell.code());
        badge.addClassName("attendance-day-badge");
        if (cell != null) {
            badge.addClassNames(css(cell.code(), cell.issue()));
            applyLegendColor(badge, cell.code());
            badge.getElement().setAttribute("title", cell.tooltip());
            badge.getStyle().set("cursor", "pointer");
            badge.addClickListener(e -> openDetail(row, date, cell));
        }
        return badge;
    }

    private void openDetail(EmployeeRow row, LocalDate date, DayCell cell) {
        EmployeeAttendanceDetail item = cell.items().getLast();
        CustomDialog dialog = new CustomDialog("Attendance Detail | ព័ត៌មានលម្អិតវត្តមាន");
        dialog.setWidth("min(650px, 95vw)");
        VerticalLayout content = new VerticalLayout(
                new H3(row.nameEn() + " | " + row.nameKh()),
                detail("Date | កាលបរិច្ឆេទ", date), detail("Code | លេខកូដ", cell.code()),
                detail("Duration | រយៈពេល", item.getLeaveDuration() == null ? "" : item.getLeaveDuration().getLabel()),
                detail("Location | ទីតាំង", row.location()), detail("Team | ក្រុម", row.teams()),
                detail("Entry Status | ស្ថានភាពបញ្ចូល", item.getEntryStatus() == null ? "" : item.getEntryStatus().getLabel()),
                detail("QC Status | ស្ថានភាព QC", item.getQcStatus() == null ? "" : item.getQcStatus().getLabel()),
                detail("QC Date | កាលបរិច្ឆេទ QC", item.getQcDate()),
                detail("HR Final Review | ការពិនិត្យចុងក្រោយ HR", status(item).label()),
                detail("HR Review Date | កាលបរិច្ឆេទពិនិត្យ HR", item.getHrVerificationDate()),
                detail("Remark | កំណត់សម្គាល់", safe(item.getRemark())));
        content.setPadding(false);
        dialog.add(content);

        if (canDelete()) {
            Button deleteDay = new Button("Delete Day | លុបថ្ងៃនេះ", VaadinIcon.TRASH.create());
            deleteDay.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteDay.addClickListener(e -> confirmDeleteDay(dialog, row, date, cell));
            dialog.getFooter().add(deleteDay);
        }
        dialog.open();
    }

    private boolean canDelete() {
        return authenticatedUser.hasPage(
                AttendanceSummaryView.class, AccessPageType.DELETED_PAGE);
    }

    private void confirmDeleteDay(CustomDialog detailDialog, EmployeeRow row,
            LocalDate date, DayCell cell) {
        Set<Long> ids = attendanceIds(cell);
        if (ids.isEmpty()) {
            Notification.show(
                    "No attendance record found for this day. | មិនមានកំណត់ត្រាវត្តមានសម្រាប់ថ្ងៃនេះទេ។",
                    3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String employee = safe(row.nameEn());
        String message = "Delete " + ids.size() + " attendance record(s) for "
                + employee + " on " + date + "? This cannot be undone. "
                + "| លុបកំណត់ត្រាវត្តមាន " + ids.size() + " សម្រាប់ "
                + employee + " នៅថ្ងៃទី " + date + "? មិនអាចត្រឡប់វិញបានទេ។";
        openDeleteConfirmation(
                "Delete Attendance Day | លុបវត្តមានប្រចាំថ្ងៃ",
                message,
                ids,
                detailDialog);
    }

    private void confirmDeleteSelected() {
        Set<EmployeeRow> selectedRows = grid.getSelectedItems();
        if (selectedRows.isEmpty()) {
            Notification.show(
                    "Please select at least one row. | សូមជ្រើសរើសយ៉ាងហោចណាស់មួយជួរ។",
                    3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Set<Long> ids = new LinkedHashSet<>();
        selectedRows.forEach(row -> row.days().values().forEach(cell ->
                ids.addAll(attendanceIds(cell))));

        if (ids.isEmpty()) {
            Notification.show(
                    "The selected rows do not contain attendance records. "
                            + "| ជួរដែលបានជ្រើសមិនមានកំណត់ត្រាវត្តមានទេ។",
                    3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String message = "Delete " + ids.size() + " attendance record(s) from "
                + selectedRows.size() + " selected employee row(s)? This cannot be undone. "
                + "Only attendance days currently visible under the selected Status filter are included. "
                + "| លុបកំណត់ត្រាវត្តមាន " + ids.size() + " ពីជួរបុគ្គលិកដែលបានជ្រើស "
                + selectedRows.size() + "? មិនអាចត្រឡប់វិញបានទេ។";
        openDeleteConfirmation(
                "Delete Selected Attendance | លុបវត្តមានដែលបានជ្រើស",
                message,
                ids,
                null);
    }

    private Set<Long> attendanceIds(DayCell cell) {
        Set<Long> ids = new LinkedHashSet<>();
        if (cell != null && cell.items() != null) {
            cell.items().stream()
                    .map(EmployeeAttendanceDetail::getId)
                    .filter(Objects::nonNull)
                    .forEach(ids::add);
        }
        return ids;
    }

    private void openDeleteConfirmation(String title, String message,
            Set<Long> attendanceIds, CustomDialog parentDialog) {
        CustomDialog confirm = new CustomDialog(title);
        confirm.setWidth("min(560px, 95vw)");

        Div text = new Div();
        text.setText(message);
        text.getStyle().set("padding", "var(--lumo-space-m)");
        confirm.add(text);

        Button cancel = new Button("Cancel | បោះបង់", e -> confirm.close());
        Button delete = new Button("Delete | លុប", VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        delete.addClickListener(e -> {
            try {
                int deleted = service.deleteAttendanceRecords(attendanceIds);
                confirm.close();
                if (parentDialog != null) {
                    parentDialog.close();
                }
                grid.deselectAll();
                refresh();
                Notification.show(
                        "Deleted " + deleted + " attendance record(s). "
                                + "| បានលុបកំណត់ត្រាវត្តមាន " + deleted + "។",
                        4000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show(
                        ex.getMessage() == null ? "Unable to delete attendance record(s)." : ex.getMessage(),
                        6000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirm.getFooter().add(cancel, delete);
        confirm.open();
    }

    private static Component detail(String label, Object value) {
        Div row = new Div();
        Span title = new Span(label + ": "); title.getStyle().set("font-weight", "600");
        row.add(title, new Span(value == null ? "" : String.valueOf(value)));
        return row;
    }

    private void refresh() {
        try {
            allRows = group(service.findSummary(fromDate.getValue(), toDate.getValue()));
            updateFilterItems();
            LocalDate start = fromDate.getValue();
            LocalDate end = toDate.getValue();
            if (!Objects.equals(configuredStart, start) || !Objects.equals(configuredEnd, end)) {
                configureGrid(start, end);
                configuredStart = start;
                configuredEnd = end;
            }
            applyFilter();
        } catch (Exception e) {
            allRows = List.of(); filteredRows = List.of();
            grid.setItems(List.of());
            updateCards(List.of());
            Notification.show(e.getMessage() == null ? "Unexpected error" : e.getMessage(),
                    4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void applyFilter() {
        String term = safe(search.getValue()).toLowerCase(Locale.ROOT);
        Set<String> locations = locationFilter.getValue(), teams = teamFilter.getValue();
        WorkflowStatus selected = statusFilter.getValue() == null ? WorkflowStatus.ALL : statusFilter.getValue();
        List<EmployeeRow> rows = allRows.stream().map(row -> row.filtered(selected))
                .filter(row -> !row.days().isEmpty())
                .filter(row -> locations.isEmpty() || any(row.location(), locations))
                .filter(row -> teams.isEmpty() || any(row.teams(), teams))
                .filter(row -> term.isBlank() || String.valueOf(row.insuranceNo()).contains(term)
                        || row.nameEn().toLowerCase(Locale.ROOT).contains(term)
                        || row.nameKh().toLowerCase(Locale.ROOT).contains(term)
                        || row.position().toLowerCase(Locale.ROOT).contains(term)
                        || row.location().toLowerCase(Locale.ROOT).contains(term)
                        || row.teams().toLowerCase(Locale.ROOT).contains(term)).toList();
        filteredRows = rows;
        grid.deselectAll();
        deleteSelected.setEnabled(false);
        grid.setItems(rows);
        updateCards(rows);
    }

    private Anchor createExcelExport() {
        Button exportButton = new Button(
                "Export Excel",
                VaadinIcon.DOWNLOAD.create());
        exportButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SUCCESS);

        Anchor anchor = new Anchor(
                DownloadHandler.fromInputStream(event -> {
                    byte[] excel = buildExcelWorkbook();
                    return new DownloadResponse(
                            new ByteArrayInputStream(excel),
                            "Attendance_Summary.xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            excel.length);
                }, "Attendance_Summary.xlsx"),
                "");
        anchor.add(exportButton);
        anchor.getStyle().set("text-decoration", "none");
        return anchor;
    }

    private byte[] buildExcelWorkbook() {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance Summary");
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            CellStyle weekendHeaderStyle = createExcelWeekendHeaderStyle(workbook);
            CellStyle legendStyle = createExcelLegendStyle(workbook);

            List<LocalDate> dates = new ArrayList<>();
            List<LegendDef> excelCountLegends = LEGENDS.stream()
                    .filter(legend -> !Set.of("½", "!").contains(legend.code()))
                    .toList();
            if (fromDate.getValue() != null && toDate.getValue() != null) {
                for (LocalDate date = fromDate.getValue(); !date.isAfter(toDate.getValue());
                        date = date.plusDays(1)) {
                    dates.add(date);
                }
            }

            Row groupHeader = sheet.createRow(0);
            Row header = sheet.createRow(1);
            int column = 0;
            column = verticalExcelHeader(sheet, groupHeader, header, column, "No.", headerStyle);
            column = verticalExcelHeader(sheet, groupHeader, header, column, "Insurance No.", headerStyle);
            column = verticalExcelHeader(sheet, groupHeader, header, column,
                    "Employee Name (English)", headerStyle);
            column = verticalExcelHeader(sheet, groupHeader, header, column,
                    "Employee Name (Khmer)", headerStyle);
            column = verticalExcelHeader(sheet, groupHeader, header, column, "Position", headerStyle);
            column = verticalExcelHeader(sheet, groupHeader, header, column, "Location", headerStyle);
            column = verticalExcelHeader(sheet, groupHeader, header, column, "Team", headerStyle);

            int dateIndex = 0;
            while (dateIndex < dates.size()) {
                YearMonth month = YearMonth.from(dates.get(dateIndex));
                int monthStartColumn = column;
                while (dateIndex < dates.size()
                        && YearMonth.from(dates.get(dateIndex)).equals(month)) {
                    LocalDate date = dates.get(dateIndex++);
                    String dayLabel = date.getDayOfMonth() + "\n"
                            + date.getDayOfWeek().name().substring(0, 3)
                            + " | " + khDay(date);
                    CellStyle dayStyle = date.getDayOfWeek().getValue() >= 6
                            ? weekendHeaderStyle : headerStyle;
                    column = excelHeader(header, column, dayLabel, dayStyle);
                }
                mergedExcelHeader(sheet, groupHeader, monthStartColumn, column - 1,
                        month.format(MONTH_FORMAT) + " | "
                                + khMonth(month.getMonthValue()) + " " + month.getYear(),
                        headerStyle);
            }

            int legendStartColumn = column;
            for (LegendDef legend : excelCountLegends) {
                column = excelHeader(header, column, legend.code(), headerStyle);
            }
            if (!excelCountLegends.isEmpty()) {
                mergedExcelHeader(sheet, groupHeader, legendStartColumn, column - 1,
                        "Legend Counts | ចំនួនតាមនិមិត្តសញ្ញា", headerStyle);
            }
            column = verticalExcelHeader(sheet, groupHeader, header, column,
                    "Workflow Status | ស្ថានភាព", headerStyle);

            int rowNumber = 2;
            for (EmployeeRow employeeRow : filteredRows) {
                Row row = sheet.createRow(rowNumber);
                int cell = 0;
                row.createCell(cell++).setCellValue(rowNumber - 1);
                rowNumber++;
                Cell insuranceCell = row.createCell(cell++);
                if (employeeRow.insuranceNo() != null) {
                    insuranceCell.setCellValue(employeeRow.insuranceNo().doubleValue());
                }
                row.createCell(cell++).setCellValue(employeeRow.nameEn());
                row.createCell(cell++).setCellValue(employeeRow.nameKh());
                row.createCell(cell++).setCellValue(employeeRow.position());
                row.createCell(cell++).setCellValue(employeeRow.location());
                row.createCell(cell++).setCellValue(employeeRow.teams());

                for (LocalDate date : dates) {
                    DayCell day = employeeRow.days().get(date);
                    String value = day == null ? "" : day.code()
                            + (isPendingWorkflow(day.status()) ? " !" : "");
                    row.createCell(cell++).setCellValue(value);
                }
                for (LegendDef legend : excelCountLegends) {
                    row.createCell(cell++).setCellValue(employeeRow.legendCounts()
                            .getOrDefault(legend.code(), 0.0));
                }
                row.createCell(cell).setCellValue(employeeRow.summaryStatus());
            }

            // Display the legend below the last attendance record, one item per row.
            rowNumber++;
            Row legendTitle = sheet.createRow(rowNumber++);
            mergedExcelHeader(sheet, legendTitle, 0, 1,
                    "Attendance Legend | និមិត្តសញ្ញាវត្តមាន", headerStyle);
            legendTitle.setHeightInPoints(24);

            for (LegendDef legend : LEGENDS) {
                Row legendRow = sheet.createRow(rowNumber++);
                Cell codeCell = legendRow.createCell(0);
                codeCell.setCellValue(legend.code());
                codeCell.setCellStyle(headerStyle);
                Cell descriptionCell = legendRow.createCell(1);
                descriptionCell.setCellValue(englishLabel(legend.label()));
                descriptionCell.setCellStyle(legendStyle);
            }

            sheet.createFreezePane(7, 2);
            groupHeader.setHeightInPoints(24);
            header.setHeightInPoints(38);
            for (int i = 0; i < column; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i), 35 * 256));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to export attendance summary to Excel", exception);
        }
    }

    private static int excelHeader(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return column + 1;
    }

    private static int verticalExcelHeader(Sheet sheet, Row groupHeader, Row header,
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

    private static void mergedExcelHeader(Sheet sheet, Row row, int fromColumn,
            int toColumn, String value, CellStyle style) {
        for (int column = fromColumn; column <= toColumn; column++) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(style);
            if (column == fromColumn) {
                cell.setCellValue(value);
            }
        }
        if (toColumn > fromColumn) {
            sheet.addMergedRegion(new CellRangeAddress(
                    row.getRowNum(), row.getRowNum(), fromColumn, toColumn));
        }
    }

    private static CellStyle createExcelHeaderStyle(Workbook workbook) {
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

    private static CellStyle createExcelWeekendHeaderStyle(Workbook workbook) {
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

    private static CellStyle createExcelLegendStyle(Workbook workbook) {
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

    private void updateFilterItems() {
        Set<String> oldLocations = new LinkedHashSet<>(locationFilter.getValue());
        Set<String> oldTeams = new LinkedHashSet<>(teamFilter.getValue());
        List<String> locations = allRows.stream().flatMap(r -> values(r.location()).stream())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<String> teams = allRows.stream().flatMap(r -> values(r.teams()).stream())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        locationFilter.setItems(locations); teamFilter.setItems(teams);
        oldLocations.retainAll(locations); oldTeams.retainAll(teams);
        locationFilter.setValue(oldLocations); teamFilter.setValue(oldTeams);
    }

    private List<EmployeeRow> group(List<EmployeeAttendanceDetail> items) {
        Map<Long, Builder> grouped = new LinkedHashMap<>();
        items.forEach(item -> grouped.computeIfAbsent(item.getEmployee().getId(),
                id -> new Builder(item.getEmployee())).add(item));
        return grouped.values().stream().map(Builder::build).toList();
    }

    private void updateCards(List<EmployeeRow> rows) {
        setCard(employees, rows.size());
        setCard(records, rows.stream().mapToLong(r -> r.days().size()).sum());
        setCard(present, rows.stream().mapToLong(EmployeeRow::presentCount).sum());
        setCard(leave, rows.stream().mapToLong(EmployeeRow::leaveCount).sum());
        setCard(absent, rows.stream().mapToLong(EmployeeRow::absentCount).sum());
        setCard(pendingQc, count(rows, WorkflowStatus.PENDING_QC));
        setCard(pendingHr, count(rows, WorkflowStatus.PENDING_HR));
        setCard(finalized, count(rows, WorkflowStatus.FINALIZED));
    }

    private static long count(List<EmployeeRow> rows, WorkflowStatus status) {
        return rows.stream().flatMap(r -> r.days().values().stream()).filter(c -> c.status() == status).count();
    }

    private static Span createCard(String label, String accentColor, String backgroundColor) {
        Span card = new Span();
        card.addClassName("attendance-summary-card");
        card.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between")
                .set("gap", "var(--lumo-space-xs)")
                .set("min-width", "0")
                .set("min-height", "76px")
                .set("padding", "var(--lumo-space-m)")
                .set("background", backgroundColor)
                .set("border", "1px solid color-mix(in srgb, " + accentColor + " 22%, transparent)")
                .set("border-top", "4px solid " + accentColor)
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "0 2px 8px rgba(15, 23, 42, 0.08)")
                .set("box-sizing", "border-box");
        Span title = new Span(label);
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("line-height", "1.25");
        Span value = new Span("0"); value.addClassName("card-value");
        value.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "800")
                .set("line-height", "1")
                .set("color", accentColor);
        card.add(title, value); return card;
    }

    private static void setCard(Span card, long value) {
        card.getChildren().filter(c -> c.hasClassName("card-value")).findFirst()
                .ifPresent(c -> c.getElement().setText(String.valueOf(value)));
    }

    private static Div dayHeader(LocalDate date) {
        Div header = new Div(); header.getStyle().set("display", "flex")
                .set("flex-direction", "column").set("align-items", "center").set("font-weight", "700");
        if (date.getDayOfWeek().getValue() >= 6) header.getStyle().set("color", "var(--lumo-error-text-color)");
        String en = date.getDayOfWeek().name().substring(0, 3);
        Span weekday = new Span(en + " | " + khDay(date));
        weekday.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        header.add(new Span(String.valueOf(date.getDayOfMonth())), weekday);
        header.getElement().setAttribute("title", date + " · " + en + " | " + khDay(date)); return header;
    }

    private static Div monthHeader(YearMonth month) {
        return groupHeader(month.format(MONTH_FORMAT) + " | " + khMonth(month.getMonthValue()) + " " + month.getYear());
    }

    private static Div groupHeader(String text) {
        Div header = new Div();
        header.setText(text);
        header.getStyle().set("font-weight", "700").set("text-align", "center");
        return header;
    }

    private static Div countHeader(LegendDef legend) {
        Div header = new Div();
        header.setText(legend.code());
        header.getStyle().set("font-weight", "700").set("text-align", "center");
        header.getElement().setAttribute("title",
                legend.code() + " - " + legend.label());
        return header;
    }

    private static String formatCount(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String khDay(LocalDate d) { return switch (d.getDayOfWeek()) {
        case MONDAY -> "ច"; case TUESDAY -> "អ"; case WEDNESDAY -> "ព"; case THURSDAY -> "ព្រ";
        case FRIDAY -> "សុ"; case SATURDAY -> "ស"; case SUNDAY -> "អា"; }; }
    private static String khMonth(int m) { return switch (m) {
        case 1 -> "មករា"; case 2 -> "កុម្ភៈ"; case 3 -> "មីនា"; case 4 -> "មេសា";
        case 5 -> "ឧសភា"; case 6 -> "មិថុនា"; case 7 -> "កក្កដា"; case 8 -> "សីហា";
        case 9 -> "កញ្ញា"; case 10 -> "តុលា"; case 11 -> "វិច្ឆិកា"; case 12 -> "ធ្នូ"; default -> ""; }; }

    private static Div createLegend() {
        Div legend = new Div();
        legend.addClassName("payroll-attendance-legend");
        LEGENDS.forEach(item -> legend.add(
                legend(item.code(), englishLabel(item.label()), item.color())));
        return legend;
    }

    private static String englishLabel(String bilingualLabel) {
        String label = safe(bilingualLabel);
        int separator = label.indexOf('|');
        return separator < 0 ? label : label.substring(0, separator).trim();
    }

    private static Div legend(String code, String label, String color) {
        Div item = new Div(); item.addClassName("payroll-attendance-legend-item");
        item.getElement().setAttribute("title", code + " - " + label);
        Span badge = new Span(code);
        badge.addClassName("attendance-day-badge");
        applyColor(badge, color);
        item.add(badge, new Span(label)); return item;
    }

    private static void applyLegendColor(Span badge, String displayedCode) {
        String normalized = safe(displayedCode).replace("½", "").toUpperCase(Locale.ROOT);
        if (Set.of("P", "PR", "PRESENT").contains(normalized)) normalized = "√";
        String code = normalized;
        LEGENDS.stream()
                .filter(item -> item.code().equalsIgnoreCase(code))
                .findFirst()
                .ifPresent(item -> applyColor(badge, item.color()));
    }

    private static void applyColor(Span badge, String color) {
        String valid = validLegendColor(color);
        badge.getStyle()
                .set("background-color", "color-mix(in srgb, " + valid + " 16%, transparent)")
                .set("border-color", valid)
                .set("color", valid);
    }

    private static String[] css(String code, boolean issue) {
        String n = safe(code).toUpperCase().replace("½", "");
        String base = switch (n) { case "√", "P", "PR", "PRESENT" -> "attendance-code-present";
            case "AL" -> "attendance-code-annual"; case "SL" -> "attendance-code-special";
            case "S" -> "attendance-code-sick"; case "UL" -> "attendance-code-unpaid";
            case "A" -> "attendance-code-absent"; case "H", "OFF", "OFF/PMM" -> "attendance-code-holiday";
            default -> "attendance-code-other"; };
        List<String> result = new ArrayList<>(List.of(base));
        if (code.contains("½")) result.add("attendance-code-half"); if (issue) result.add("attendance-day-issue");
        return result.toArray(String[]::new);
    }

    private static WorkflowStatus status(EmployeeAttendanceDetail item) {
        if (item.getEntryStatus() == AttendanceEntryStatus.DRAFT)
            return item.getQcStatus() == AttendanceQcStatus.RETURNED ? WorkflowStatus.RETURNED : WorkflowStatus.DRAFT;
        if (item.getQcStatus() == AttendanceQcStatus.RETURNED) return WorkflowStatus.RETURNED;
        if (item.getQcStatus() != AttendanceQcStatus.VERIFIED || item.getQcByEmployee() == null) return WorkflowStatus.PENDING_QC;
        return item.getHrVerificationBy() != null || item.getHrVerificationDate() != null
                ? WorkflowStatus.FINALIZED : WorkflowStatus.PENDING_HR;
    }

    private static boolean isPendingWorkflow(WorkflowStatus status) {
        return status == WorkflowStatus.DRAFT
                || status == WorkflowStatus.RETURNED
                || status == WorkflowStatus.PENDING_QC
                || status == WorkflowStatus.PENDING_HR;
    }

    private static String code(EmployeeAttendanceDetail item) {
        LeaveType type = item.getLeaveType(); String code = type == null ? "" : safe(type.getLeavTypeCode()).toUpperCase();
        if (code.isBlank() || Set.of("P", "PR", "PRESENT").contains(code)) code = "√";
        return item.getNumberOfDay() != null && Float.compare(item.getNumberOfDay(), .5f) == 0 ? "½" + code : code;
    }

    private static boolean any(String csv, Set<String> selected) { return values(csv).stream().anyMatch(selected::contains); }
    private static List<String> values(String csv) { return csv == null || csv.isBlank() ? List.of() : List.of(csv.split(",\\s*")); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static boolean present(String c) { return Set.of("√", "P", "PR", "PRESENT").contains(c.replace("½", "").toUpperCase()); }
    private static boolean absent(String c) { return "A".equals(c.replace("½", "").toUpperCase()); }
    private static boolean leave(String c) { return Set.of("AL", "SL", "S", "UL", "ML").contains(c.replace("½", "").toUpperCase()); }

    private enum WorkflowStatus {
        ALL("All Statuses | គ្រប់ស្ថានភាព"), DRAFT("Draft | ព្រាង"), RETURNED("Returned | បានបញ្ជូនត្រឡប់"),
        PENDING_QC("Pending QC | រង់ចាំ QC"), PENDING_HR("Pending HR | រង់ចាំ HR"), FINALIZED("Finalized | បានបញ្ចប់");
        private final String label; WorkflowStatus(String label) { this.label = label; } private String label() { return label; }
    }

    private record LegendDef(String code, String label, String color) { }

    private record DayCell(String code, WorkflowStatus status, boolean issue, String tooltip,
            List<EmployeeAttendanceDetail> items) { }
    private record EmployeeRow(Long employeeId, Integer insuranceNo, String nameEn, String nameKh,
            String position, String location, String teams, Map<LocalDate, DayCell> days,
            long presentCount, long leaveCount, long absentCount,
            Map<String, Double> legendCounts, String summaryStatus) {
        private EmployeeRow filtered(WorkflowStatus selected) {
            Map<LocalDate, DayCell> map = new LinkedHashMap<>();
            days.forEach((d, c) -> { if (selected == WorkflowStatus.ALL || c.status() == selected) map.put(d, c); });
            long p = map.values().stream().filter(c -> present(c.code())).count();
            long l = map.values().stream().filter(c -> leave(c.code())).count();
            long a = map.values().stream().filter(c -> absent(c.code())).count();
            Map<String, Double> counts = calculateLegendCounts(map.values());
            String s = map.values().stream().map(DayCell::status).distinct().count() == 1 && !map.isEmpty()
                    ? map.values().iterator().next().status().label() : WorkflowStatus.ALL.label();
            return new EmployeeRow(employeeId, insuranceNo, nameEn, nameKh, position, location, teams,
                    Map.copyOf(map), p, l, a, counts, s);
        }
    }

    private static Map<String, Double> calculateLegendCounts(
            java.util.Collection<DayCell> cells) {
        Map<String, Double> counts = new LinkedHashMap<>();
        LEGENDS.forEach(item -> counts.put(item.code(), 0.0));
        for (DayCell cell : cells) {
            String displayCode = safe(cell.code());
            boolean halfDay = displayCode.contains("½");
            String normalized = displayCode.replace("½", "").toUpperCase(Locale.ROOT);
            String legendCode = switch (normalized) {
                case "P", "PR", "PRESENT", "√" -> "√";
                case "OFF" -> "Off";
                case "OFF/PMM" -> "Off/PMM";
                default -> normalized;
            };
            if (counts.containsKey(legendCode)) {
                counts.merge(legendCode, halfDay ? 0.5 : 1.0, Double::sum);
            }
            if (halfDay) {
                counts.merge("½", 1.0, Double::sum);
            }
            if (isPendingWorkflow(cell.status())) {
                counts.merge("!", 1.0, Double::sum);
            }
        }
        return Map.copyOf(counts);
    }

    private static final class Builder {
        private final Employee employee;
        private final Map<LocalDate, List<EmployeeAttendanceDetail>> days = new LinkedHashMap<>();
        private Builder(Employee employee) { this.employee = employee; }
        private void add(EmployeeAttendanceDetail item) { days.computeIfAbsent(item.getAttendanceDate(), d -> new ArrayList<>()).add(item); }
        private EmployeeRow build() {
            Map<LocalDate, DayCell> cells = new LinkedHashMap<>(); Set<String> locations = new LinkedHashSet<>(), teams = new LinkedHashSet<>();
            days.forEach((date, items) -> {
                EmployeeAttendanceDetail display = items.getLast(); String code = code(display); WorkflowStatus status = status(display);
                boolean duplicate = items.size() > 1;
                boolean issue = duplicate || isPendingWorkflow(status);
                cells.put(date, new DayCell(code, status, issue,
                        date + " · " + code + " · " + status.label()
                                + (duplicate ? " · Duplicate: " + items.size() : ""),
                        List.copyOf(items)));
                items.forEach(item -> {
                    if (item.getReportLocation() != null) locations.add(safe(item.getReportLocation().getBranchShortName()));
                    if (item.getAttendanceDetailTeams() != null) for (EmployeeAttendanceDetailTeam row : item.getAttendanceDetailTeams())
                        if (row != null && row.getTeam() != null) teams.add(safe(row.getTeam().getTeamCode()));
                });
            });
            locations.remove(""); teams.remove("");
            String position = employee.getPositions() == null ? "" : safe(employee.getPositions().getPosition()) + " | " + safe(employee.getPositions().getPositionKh());
            return new EmployeeRow(employee.getId(), employee.getInsuranceNo(), safe(employee.getNameEn()), safe(employee.getNameKh()),
                    position, String.join(", ", locations), String.join(", ", teams),
                    Map.copyOf(cells), 0, 0, 0, Map.of(), "")
                    .filtered(WorkflowStatus.ALL);
        }
    }
}
