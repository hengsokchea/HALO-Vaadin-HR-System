package org.halocambodia.views.attendance_management;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeAttendanceDetail;
import org.halocambodia.data.EmployeeAttendanceDetailTeam;
import org.halocambodia.data.LeaveType;
import org.halocambodia.data.LeaveTypeRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeAttendanceVerificationService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "attendance-verification", layout = MainLayout.class)
@PageTitle("QC Attendance Verification | ការផ្ទៀងផ្ទាត់វត្តមានដោយ QC")
@PermitAll
public class AttendanceVerificationView extends VerticalLayout implements BeforeEnterObserver {
    private static final DateTimeFormatter MONTH_FORMAT =     DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static volatile List<LegendDef> LEGENDS = List.of(
            new LegendDef("√", "Present | វត្តមាន", "#16A34A"),
            new LegendDef("½", "Half Day | កន្លះថ្ងៃ", "#475569"),
            new LegendDef("!", "Pending QC | រង់ចាំ QC", "#DB2777"));

    private final EmployeeAttendanceVerificationService service;
    private final AuthenticatedUser authenticatedUser;
    private final DatePicker fromDate = new DatePicker("From Date | ចាប់ពីថ្ងៃ");
    private final DatePicker toDate = new DatePicker("To Date | ដល់ថ្ងៃ");
    private final TextField search = new TextField("Search | ស្វែងរក");
    private final MultiSelectComboBox<String> locationFilter = new MultiSelectComboBox<>("Location | តំបន់");
    private final MultiSelectComboBox<String> teamFilter = new MultiSelectComboBox<>("Team | ក្រុម");
    private final Checkbox pendingOnly = new Checkbox("Pending Only");
    private final Grid<EmployeeRow> grid = new Grid<>();
    private final Span summary = new Span();
    private List<EmployeeRow> allRows = List.of();
    private HeaderRow monthHeader;
    private LocalDate configuredStart;
    private LocalDate configuredEnd;

    public AttendanceVerificationView(EmployeeAttendanceVerificationService service,
            AuthenticatedUser authenticatedUser,
            LeaveTypeRepository leaveTypeRepository) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
        LEGENDS = loadLegends(leaveTypeRepository);
        configurePage();
        configureFilters();
        configureGrid(null, null);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(AttendanceVerificationView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        } else {
            refresh();
        }
    }

    private void configurePage() {
        setSizeFull();
        setPadding(true);
        setSpacing(false);


        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create(), e -> refresh());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_SUCCESS);
        Button verifyButton = new Button("Verify Selected", VaadinIcon.CHECK_CIRCLE.create(), e -> confirmVerification());
        verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyButton.setEnabled(authenticatedUser.hasPage(AttendanceVerificationView.class, AccessPageType.UPDATED_PAGE));

        HorizontalLayout filters = new HorizontalLayout(fromDate, toDate, search, locationFilter, teamFilter, pendingOnly, refreshButton);
        filters.setWidthFull();
        filters.setWrap(true);
        filters.setAlignItems(Alignment.CENTER);
        filters.setJustifyContentMode(JustifyContentMode.START);

        summary.getStyle().set("font-weight", "600");

        HorizontalLayout verificationSummary = new HorizontalLayout(verifyButton, summary);
        verificationSummary.setWidthFull();
        verificationSummary.setAlignItems(Alignment.CENTER);
        verificationSummary.setJustifyContentMode(JustifyContentMode.START);

        add(filters, verificationSummary, grid, createLegend());
        setFlexGrow(1, grid);
    }

    private void configureFilters() {
        LocalDate today = LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        fromDate.setValue(today.withDayOfMonth(1));
        toDate.setValue(today);
        fromDate.setWidth("180px");
        toDate.setWidth("180px");
        search.setPlaceholder("Insurance or  name");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
       //search.setWidth("min(380px, 100%)");
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> applyFilter());
        locationFilter.setPlaceholder("All locations | គ្រប់តំបន់");
        locationFilter.setClearButtonVisible(true);
        //locationFilter.setWidth("220px");
        locationFilter.addValueChangeListener(e -> applyFilter());
        teamFilter.setPlaceholder("All teams | គ្រប់ក្រុម");
        teamFilter.setClearButtonVisible(true);
       // teamFilter.setWidth("220px");
        teamFilter.addValueChangeListener(e -> applyFilter());
        pendingOnly.setValue(true);
        pendingOnly.addValueChangeListener(e -> applyFilter());
    }

    private void configureGrid(LocalDate start, LocalDate end) {
        if (monthHeader != null) {
            grid.removeHeaderRow(monthHeader);
            monthHeader = null;
        }
        // Recreate the selection model every time the dynamic date columns are
        // rebuilt. Otherwise removeAllColumns() can remove the checkbox column
        // while the Grid still keeps the old MULTI selection model.
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.removeAllColumns();
        grid.setSizeFull();
        GridMultiSelectionModel<EmployeeRow> selectionModel =
                (GridMultiSelectionModel<EmployeeRow>) grid.setSelectionMode(
                        Grid.SelectionMode.MULTI);
        selectionModel.setSelectionColumnFrozen(true);
        selectionModel.setDragSelect(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        Grid.Column<EmployeeRow> insuranceColumn = grid.addColumn(EmployeeRow::insuranceNo)
                .setHeader("Ins. | លេខធានា")
                .setFrozen(true).setWidth("95px").setFlexGrow(0).setSortable(true);
        Grid.Column<EmployeeRow> nameEnColumn = grid.addColumn(EmployeeRow::nameEn)
                .setHeader("Name EN | ឈ្មោះអង់គ្លេស")
                .setFrozen(true).setWidth("220px").setFlexGrow(0).setSortable(true);
        Grid.Column<EmployeeRow> nameKhColumn = grid.addColumn(EmployeeRow::nameKh)
                .setHeader("Name KH | ឈ្មោះខ្មែរ")
                .setWidth("220px").setFlexGrow(0).setSortable(true);
        Grid.Column<EmployeeRow> positionColumn = grid.addColumn(EmployeeRow::position)
                .setHeader("Position | មុខតំណែង")
                .setWidth("190px").setFlexGrow(0);
        grid.addColumn(EmployeeRow::location)
                .setHeader("Location | តំបន់")
                .setWidth("160px").setFlexGrow(0).setResizable(true);
        grid.addColumn(EmployeeRow::teams)
                .setHeader("Team | ក្រុម")
                .setWidth("180px").setFlexGrow(0).setResizable(true);

        Map<YearMonth, List<Grid.Column<EmployeeRow>>> monthColumns = new LinkedHashMap<>();
        if (start != null && end != null) {
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                LocalDate day = date;
                Grid.Column<EmployeeRow> column = grid.addColumn(new ComponentRenderer<>(row -> dayBadge(row, day)))
                        .setHeader(dayHeader(day))
                        .setAutoWidth(true).setFlexGrow(0).setTextAlign(ColumnTextAlign.CENTER)
                        .setResizable(true);
                monthColumns.computeIfAbsent(YearMonth.from(day), ignored -> new ArrayList<>()).add(column);
            }
        }
        Grid.Column<EmployeeRow> pendingColumn = grid.addColumn(EmployeeRow::pendingCount)
                .setHeader("Pending | មិនទាន់ផ្ទៀងផ្ទាត់")
                .setWidth("145px").setFlexGrow(0).setTextAlign(ColumnTextAlign.END);
        Grid.Column<EmployeeRow> verifiedColumn = grid.addColumn(EmployeeRow::verifiedCount)
                .setHeader("Verified | បានផ្ទៀងផ្ទាត់")
                .setWidth("135px").setFlexGrow(0).setTextAlign(ColumnTextAlign.END);
        if (!monthColumns.isEmpty()) {
            monthHeader = grid.prependHeaderRow();
            monthColumns.forEach((month, columns) -> {
                @SuppressWarnings("unchecked")
                Grid.Column<EmployeeRow>[] array = columns.toArray(Grid.Column[]::new);
                monthHeader.join(array).setComponent(monthHeader(month));
            });
            monthHeader.join(pendingColumn, verifiedColumn)
                    .setComponent(qcSummaryHeader());
        }
    }

    private static Div qcSummaryHeader() {
        Div header = new Div();
        header.setText("QC Summary | សង្ខេប QC");
        header.getStyle()
                .set("font-weight", "700")
                .set("text-align", "center");
        return header;
    }

    private static Div dayHeader(LocalDate date) {
        boolean weekend = date.getDayOfWeek().getValue() >= 6;
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("line-height", "1.2")
                .set("font-weight", "700");
        if (weekend) {
            header.getStyle().set("color", "var(--lumo-error-text-color)");
        }
        Span dayNumber = new Span(String.valueOf(date.getDayOfMonth()));
        Span weekday = new Span(englishWeekday(date) + " | " + khmerWeekday(date));
        weekday.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        header.add(dayNumber, weekday);
        header.getElement().setAttribute("title",
                date + " · " + englishWeekday(date) + " | " + khmerWeekday(date));
        return header;
    }

    private static Div monthHeader(YearMonth month) {
        Div header = new Div();
        header.setText(month.format(MONTH_FORMAT) + " | "
                + khmerMonth(month.getMonthValue()) + " " + month.getYear());
        header.getStyle().set("font-weight", "700").set("text-align", "center");
        return header;
    }

    private static String englishWeekday(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Mon";
            case TUESDAY -> "Tue";
            case WEDNESDAY -> "Wed";
            case THURSDAY -> "Thu";
            case FRIDAY -> "Fri";
            case SATURDAY -> "Sat";
            case SUNDAY -> "Sun";
        };
    }

    private static String khmerWeekday(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "ច";
            case TUESDAY -> "អ";
            case WEDNESDAY -> "ព";
            case THURSDAY -> "ព្រ";
            case FRIDAY -> "សុ";
            case SATURDAY -> "ស";
            case SUNDAY -> "អា";
        };
    }

    private static String khmerMonth(int month) {
        return switch (month) {
            case 1 -> "មករា";
            case 2 -> "កុម្ភៈ";
            case 3 -> "មីនា";
            case 4 -> "មេសា";
            case 5 -> "ឧសភា";
            case 6 -> "មិថុនា";
            case 7 -> "កក្កដា";
            case 8 -> "សីហា";
            case 9 -> "កញ្ញា";
            case 10 -> "តុលា";
            case 11 -> "វិច្ឆិកា";
            case 12 -> "ធ្នូ";
            default -> "";
        };
    }

    private Span dayBadge(EmployeeRow row, LocalDate date) {
        DayCell cell = row.days().get(date);
        Span badge = new Span(cell == null ? "" : cell.code());
        badge.addClassName("attendance-day-badge");
        if (cell != null) {
            badge.getElement().setAttribute("title", cell.tooltip());
            badge.addClassNames(attendanceCss(
                    cell.code(), cell.duplicate() || !cell.verified()));
            applyLegendColor(badge, cell.code());
            badge.getStyle().set("cursor", "pointer");
            badge.addClickListener(event -> openDayQcDialog(row, date, cell));
        }
        return badge;
    }

    private void openDayQcDialog(EmployeeRow row, LocalDate date, DayCell cell) {
        CustomDialog dialog = new CustomDialog(
                "Daily QC Review | ការពិនិត្យ QC ប្រចាំថ្ងៃ");
        dialog.setWidth("min(620px, 95vw)");
        EmployeeAttendanceDetail record = cell.records().getLast();
        VerticalLayout content = new VerticalLayout(
                new H3(row.nameEn() + " | " + row.nameKh()),
                new Span("Attendance Date | កាលបរិច្ឆេទវត្តមាន: " + date),
                new Span("Attendance | វត្តមាន: " + cell.code()),
                new Span("Duration | រយៈពេល: "
                        + (record.getLeaveDuration() == null ? "" : record.getLeaveDuration().getLabel())),
                new Span("Data Entry By | អ្នកបញ្ចូលទិន្នន័យ: "
                        + (record.getDataEntryBy() == null ? "" : safe(record.getDataEntryBy().getNameEn()))),
                new Span("Data Entry Date | កាលបរិច្ឆេទបញ្ចូល: "
                        + (record.getDataEntryDate() == null ? "" : record.getDataEntryDate())),
                new Span("Remark | កំណត់សម្គាល់: " + safe(record.getRemark())));
        content.setPadding(false);
        dialog.add(content);

        Button verify = new Button("Verify Day | ផ្ទៀងផ្ទាត់ថ្ងៃនេះ", event -> {
            try {
                EmployeeAttendanceVerificationService.VerifyResult result =
                        service.verifyDay(record.getId());
                if (result.verified() == 0) {
                    showError(result.skipped() > 0
                            ? "This day has a duplicate or is not eligible for QC verification."
                            : "This day cannot be verified.");
                    return;
                }
                dialog.close();
                refresh();
                Notification.show("Day verified | បានផ្ទៀងផ្ទាត់ថ្ងៃ", 2500,
                        Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
        verify.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button returnButton = new Button("Return | បញ្ជូនត្រឡប់",
                event -> openReturnDialog(dialog, cell));
        returnButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(returnButton, verify);
        dialog.open();
    }

    private void openReturnDialog(CustomDialog parent, DayCell cell) {
        CustomDialog dialog = new CustomDialog(
                "Return Attendance | បញ្ជូនវត្តមានត្រឡប់");
        dialog.setWidth("min(560px, 95vw)");
        TextArea reason = new TextArea("Return Reason | មូលហេតុនៃការបញ្ជូនត្រឡប់");
        reason.setWidthFull();
        reason.setMinLength(3);
        reason.setRequired(true);
        dialog.add(reason);
        Button confirm = new Button("Return | បញ្ជូនត្រឡប់", event -> {
            try {
                List<Long> ids = cell.records().stream().map(EmployeeAttendanceDetail::getId).toList();
                int returned = service.returnDay(ids, reason.getValue());
                parent.close();
                dialog.close();
                refresh();
                Notification.show(returned + " record(s) returned to Daily Attendance.",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) { showError(ex.getMessage()); }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(new Button("Cancel | បោះបង់", e -> dialog.close()), confirm);
        dialog.open();
    }

    private static String[] attendanceCss(String displayCode, boolean duplicate) {
        String normalized = safe(displayCode).toUpperCase().replace("½", "");
        String css = switch (normalized) {
            case "√", "P", "PR", "PRESENT" -> "attendance-code-present";
            case "AL" -> "attendance-code-annual";
            case "SL" -> "attendance-code-special";
            case "S" -> "attendance-code-sick";
            case "UL" -> "attendance-code-unpaid";
            case "A" -> "attendance-code-absent";
            case "ML" -> "attendance-code-maternity";
            case "H", "OFF", "OFF/PMM" -> "attendance-code-holiday";
            default -> normalized.isBlank() ? "attendance-code-empty" : "attendance-code-other";
        };
        List<String> classes = new ArrayList<>();
        classes.add(css);
        if (safe(displayCode).contains("½")) classes.add("attendance-code-half");
        if (duplicate) classes.add("attendance-day-issue");
        return classes.toArray(String[]::new);
    }

    private void refresh() {
        try {
            LocalDate start = fromDate.getValue();
            LocalDate end = toDate.getValue();
            allRows = group(service.findForVerification(start, end));
            updateLocationAndTeamFilters();
            if (!Objects.equals(configuredStart, start) || !Objects.equals(configuredEnd, end)) {
                configureGrid(start, end);
                configuredStart = start;
                configuredEnd = end;
            }
            applyFilter();
        } catch (Exception ex) {
            allRows = List.of();
            grid.setItems(List.of());
            showError(ex.getMessage());
        }
    }

    private void applyFilter() {
        String term = search.getValue() == null ? "" : search.getValue().trim().toLowerCase();
        Set<String> selectedLocations = locationFilter.getValue();
        Set<String> selectedTeams = teamFilter.getValue();
        List<EmployeeRow> rows = allRows.stream()
                .filter(row -> !pendingOnly.getValue() || row.pendingCount() > 0)
                .filter(row -> selectedLocations.isEmpty()
                        || containsAnyValue(row.location(), selectedLocations))
                .filter(row -> selectedTeams.isEmpty()
                        || containsAnyValue(row.teams(), selectedTeams))
                .filter(row -> term.isBlank() || String.valueOf(row.insuranceNo()).contains(term)
                        || row.nameEn().toLowerCase().contains(term)
                        || row.nameKh().toLowerCase().contains(term)
                        || row.position().toLowerCase().contains(term)
                        || row.location().toLowerCase().contains(term)
                        || row.teams().toLowerCase().contains(term)).toList();
        grid.setItems(rows);
        summary.setText(rows.size() + " employee(s) | បុគ្គលិក · "
                + rows.stream().mapToLong(EmployeeRow::pendingCount).sum()
                + " pending | មិនទាន់ផ្ទៀងផ្ទាត់ · "
                + rows.stream().mapToLong(EmployeeRow::verifiedCount).sum()
                + " verified | បានផ្ទៀងផ្ទាត់");
    }

    private void updateLocationAndTeamFilters() {
        Set<String> currentLocations = new LinkedHashSet<>(locationFilter.getValue());
        Set<String> currentTeams = new LinkedHashSet<>(teamFilter.getValue());

        List<String> locations = allRows.stream()
                .flatMap(row -> csvValues(row.location()).stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> teams = allRows.stream()
                .flatMap(row -> csvValues(row.teams()).stream())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        locationFilter.setItems(locations);
        teamFilter.setItems(teams);

        currentLocations.retainAll(locations);
        currentTeams.retainAll(teams);
        locationFilter.setValue(currentLocations);
        teamFilter.setValue(currentTeams);
    }

    private static boolean containsAnyValue(String csv, Set<String> selectedValues) {
        return csvValues(csv).stream().anyMatch(selectedValues::contains);
    }

    private static List<String> csvValues(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return List.of(csv.split(",\\s*"));
    }

    private List<EmployeeRow> group(List<EmployeeAttendanceDetail> records) {
        Map<Long, EmployeeRowBuilder> grouped = new LinkedHashMap<>();
        for (EmployeeAttendanceDetail record : records) {
            grouped.computeIfAbsent(record.getEmployee().getId(), ignored ->
                    new EmployeeRowBuilder(record.getEmployee())).add(record);
        }
        return grouped.values().stream().map(EmployeeRowBuilder::build).toList();
    }

    private void confirmVerification() {
        Set<EmployeeRow> selected = grid.getSelectedItems();
        if (selected.isEmpty()) {
            showError("Please select at least one employee. | សូមជ្រើសរើសបុគ្គលិកយ៉ាងហោចណាស់ម្នាក់។");
            return;
        }
        List<Long> ids = selected.stream().flatMap(row -> row.pendingIds().stream()).toList();
        if (ids.isEmpty()) {
            showError("The selected employees have no pending attendance. "
                    + "| បុគ្គលិកដែលបានជ្រើសមិនមានវត្តមានរង់ចាំផ្ទៀងផ្ទាត់ទេ។");
            return;
        }
        CustomDialog dialog = new CustomDialog(
                "Confirm Attendance Verification | បញ្ជាក់ការផ្ទៀងផ្ទាត់វត្តមាន");
        dialog.setWidth("min(560px, 95vw)");

        Paragraph confirmation = new Paragraph(
                "Verify " + ids.size() + " attendance record(s) for " + selected.size()
                        + " employee(s)?\n\nផ្ទៀងផ្ទាត់កំណត់ត្រាវត្តមាន " + ids.size()
                        + " សម្រាប់បុគ្គលិក " + selected.size() + " នាក់?");
        confirmation.getStyle().set("white-space", "pre-line");
        dialog.add(confirmation);

        Button cancel = new Button("Cancel | បោះបង់", e -> dialog.close());
        Button confirm = new Button("Verify | ផ្ទៀងផ្ទាត់", e -> {
            try {
                EmployeeAttendanceVerificationService.VerifyResult result = service.verify(ids);
                dialog.close();
                Notification.show(result.verified() + " verified; " + result.skipped()
                        + " skipped because of issues. | បានផ្ទៀងផ្ទាត់ " + result.verified()
                        + "; រំលង " + result.skipped() + " ដោយសារបញ្ហា។",
                        3500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refresh();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private static Div createLegend() {
        Div legend = new Div();
        legend.addClassName("payroll-attendance-legend");
        LEGENDS.forEach(item -> legend.add(legendItem(
                item.code(), englishLabel(item.label()), item.color())));
        return legend;
    }

    private static Div legendItem(String code, String label, String color) {
        Div item = new Div();
        item.addClassName("payroll-attendance-legend-item");
        item.getElement().setAttribute("title", code + " - " + label);
        Span badge = new Span(code);
        badge.addClassName("attendance-day-badge");
        applyColor(badge, color);
        item.add(badge, new Span(label));
        return item;
    }

    private static List<LegendDef> loadLegends(LeaveTypeRepository repository) {
        Map<String, LegendDef> definitions = new LinkedHashMap<>();
        for (LeaveType type : repository.findByObsoleteDateIsNullOrderByLeavTypeCodeAsc()) {
            String databaseCode = safe(type.getLeavTypeCode()).toUpperCase(Locale.ROOT);
            if (databaseCode.isBlank()) continue;
            String displayCode = Set.of("P", "PR", "PRESENT").contains(databaseCode)
                    ? "√" : databaseCode;
            definitions.putIfAbsent(displayCode, new LegendDef(displayCode,
                    bilingualLabel(type.getLeaveNameEn(), type.getLeaveNameKh()),
                    validLegendColor(type.getLegendColor())));
        }
        definitions.putIfAbsent("√", new LegendDef("√", "Present | វត្តមាន", "#16A34A"));
        definitions.put("½", new LegendDef("½", "Half Day | កន្លះថ្ងៃ", "#475569"));
        definitions.put("!", new LegendDef("!", "Pending QC | រង់ចាំ QC", "#DB2777"));
        return List.copyOf(definitions.values());
    }

    private static String bilingualLabel(String english, String khmer) {
        String en = safe(english), kh = safe(khmer);
        if (en.isBlank()) return kh;
        if (kh.isBlank()) return en;
        return en + " | " + kh;
    }

    private static String englishLabel(String label) {
        int separator = safe(label).indexOf('|');
        return separator < 0 ? safe(label) : safe(label).substring(0, separator).trim();
    }

    private static String validLegendColor(String color) {
        String normalized = safe(color).toUpperCase(Locale.ROOT);
        return normalized.matches("^#[0-9A-F]{6}$") ? normalized : "#78716C";
    }

    private static void applyLegendColor(Span badge, String displayedCode) {
        String normalized = safe(displayedCode).replace("½", "").toUpperCase(Locale.ROOT);
        if (Set.of("P", "PR", "PRESENT", "√").contains(normalized)) normalized = "√";
        String code = normalized;
        LEGENDS.stream().filter(item -> item.code().equalsIgnoreCase(code)).findFirst()
                .ifPresent(item -> applyColor(badge, item.color()));
    }

    private static void applyColor(Span badge, String color) {
        String valid = validLegendColor(color);
        badge.getStyle()
                .set("background-color", "color-mix(in srgb, " + valid + " 16%, transparent)")
                .set("border-color", valid)
                .set("color", valid);
    }

    private void showError(String message) {
        Notification.show(message == null ? "Unexpected error" : message,
                4000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private static String code(EmployeeAttendanceDetail record) {
        LeaveType type = record.getLeaveType();
        String value = type == null ? "" : safe(type.getLeavTypeCode()).toUpperCase();
        if (value.isBlank() || Set.of("P", "PR", "PRESENT").contains(value)) value = "√";
        if (record.getNumberOfDay() != null && record.getNumberOfDay() == 0.5f) value = "½" + value;
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record DayCell(String code, boolean verified, boolean duplicate, String tooltip,
            List<EmployeeAttendanceDetail> records) { }
    private record LegendDef(String code, String label, String color) { }
    private record EmployeeRow(Long employeeId, Integer insuranceNo, String nameEn,
            String nameKh, String position, String location, String teams,
            Map<LocalDate, DayCell> days, List<Long> pendingIds,
            long pendingCount, long verifiedCount, long issues) { }

    private static final class EmployeeRowBuilder {
        private final Employee employee;
        private final Map<LocalDate, List<EmployeeAttendanceDetail>> byDay = new LinkedHashMap<>();
        private EmployeeRowBuilder(Employee employee) { this.employee = employee; }
        private void add(EmployeeAttendanceDetail record) {
            byDay.computeIfAbsent(record.getAttendanceDate(), ignored -> new ArrayList<>()).add(record);
        }
        private EmployeeRow build() {
            Map<LocalDate, DayCell> days = new LinkedHashMap<>();
            List<Long> pendingIds = new ArrayList<>();
            long verified = 0, issues = 0;
            for (Map.Entry<LocalDate, List<EmployeeAttendanceDetail>> entry : byDay.entrySet()) {
                List<EmployeeAttendanceDetail> records = entry.getValue();
                EmployeeAttendanceDetail display = records.get(records.size() - 1);
                List<Long> pending = records.stream().filter(r -> r.getQcByEmployee() == null)
                        .map(EmployeeAttendanceDetail::getId).toList();
                pendingIds.addAll(pending);
                verified += records.size() - pending.size();
                if (records.size() > 1) issues += records.size() - 1;
                boolean dayVerified = pending.isEmpty();
                String dayCode = code(display);
                days.put(entry.getKey(), new DayCell(dayCode, dayVerified, records.size() > 1,
                        entry.getKey() + " · " + dayCode + " · "
                        + (dayVerified ? "QC Verified | QC បានផ្ទៀងផ្ទាត់"
                                : "Pending QC Verification | រង់ចាំ QC ផ្ទៀងផ្ទាត់")
                        + (records.size() > 1 ? " · Duplicate: " + records.size() : ""),
                        List.copyOf(records)));
            }
            String nameEn = safe(employee.getNameEn());
            String nameKh = safe(employee.getNameKh());
            String position = employee.getPositions() == null ? ""
                    : safe(employee.getPositions().getPosition()) + " | "
                            + safe(employee.getPositions().getPositionKh());
            Set<String> locations = new LinkedHashSet<>();
            Set<String> teams = new LinkedHashSet<>();
            for (List<EmployeeAttendanceDetail> records : byDay.values()) {
                for (EmployeeAttendanceDetail record : records) {
                    if (record.getReportLocation() != null) {
                        String location = safe(
                                record.getReportLocation().getBranchShortName());
                        if (!location.isBlank()) locations.add(location);
                    }
                    if (record.getAttendanceDetailTeams() == null) continue;
                    for (EmployeeAttendanceDetailTeam teamRow
                            : record.getAttendanceDetailTeams()) {
                        if (teamRow == null || teamRow.getTeam() == null) continue;
                        String teamCode = safe(teamRow.getTeam().getTeamCode());
                        if (!teamCode.isBlank()) teams.add(teamCode);
                    }
                }
            }
            return new EmployeeRow(employee.getId(), employee.getInsuranceNo(), nameEn, nameKh,
                    position, String.join(", ", locations), String.join(", ", teams),
                    Map.copyOf(days), List.copyOf(pendingIds), pendingIds.size(), verified, issues);
        }
    }
}
