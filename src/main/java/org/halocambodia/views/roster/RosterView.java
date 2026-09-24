package org.halocambodia.views.roster;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Branch;
import org.halocambodia.data.BranchRepository;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeAllocation;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.EmployeeRoster;
import org.halocambodia.data.Holiday;
import org.halocambodia.data.HolidayRepository;
import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Positions;
import org.halocambodia.data.RosterRow;
import org.halocambodia.data.Shift;
import org.halocambodia.data.ShiftCycle;
import org.halocambodia.data.ShiftRepository;
import org.halocambodia.data.Teams;
import org.halocambodia.data.TeamsRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeRosterService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.halocambodia.views.leave_management.LeaveRequestView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePicker.TimePickerI18n;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.html.Paragraph;


@PageTitle("Roster | កាលវិភាគតាមវេន")
@Route(value = "roster", layout = MainLayout.class)
@PermitAll
public class RosterView extends VerticalLayout  implements BeforeEnterObserver {
	
	protected final AuthenticatedUser authenticatedUser;

	private final HolidayRepository holidayRepository ;
    private final EmployeeRosterService rosterService;

    private final Grid<RosterRow> rosterGrid = new Grid<>(RosterRow.class, false);
    private final List<Grid.Column<RosterRow>> dateColumns = new ArrayList<>();
    private final List<RosterRow> currentRows = new ArrayList<>();
    private final List<LocalDate> visibleDates = new ArrayList<>();

    private LocalDate periodStart;
    private LocalDate periodEnd;
    Button refresh = new Button("Reload | ផ្ទុកឡើងវិញ", new Icon(VaadinIcon.REFRESH));

    private final Select<ViewMode> viewMode = new Select<>();
    private final DatePicker anchorDate = new DatePicker();
    private final Span periodSummary = new Span();
    
    private final MultiSelectComboBox<Employee> advanceFilterEmployee=new MultiSelectComboBox<Employee>("Employee");
    private final MultiSelectComboBox<Positions> advanceFilterPosition=new MultiSelectComboBox<Positions>("Position");
    private final MultiSelectComboBox<Shift> advanceFilterShift=new MultiSelectComboBox<Shift>("Shift");
    private final MultiSelectComboBox<Branch> advanceFilterBranch=new MultiSelectComboBox<Branch>("Location");
    private final MultiSelectComboBox<Teams> advanceFilterTeam=new MultiSelectComboBox<Teams>("Team");
    
    private static final DateTimeFormatter HEADER_DAY_FORMAT = DateTimeFormatter.ofPattern("d", Locale.ENGLISH);
    private static final DateTimeFormatter HEADER_MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter PERIOD_SUMMARY_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    
    private final TeamsRepository teamsRepository;
    private final BranchRepository branchRepository;
    private final ShiftRepository shiftRepository;
    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;
    
    

    public RosterView(EmployeeRosterService rosterService,HolidayRepository holidayRepository,AuthenticatedUser authenticatedUser,TeamsRepository teamsRepository,BranchRepository branchRepository,ShiftRepository shiftRepository,PositionRepository positionRepository,EmployeeRepository employeeRepository) {
    	this.authenticatedUser = authenticatedUser;
        this.rosterService = rosterService;
        this.holidayRepository=holidayRepository;
        this.teamsRepository=teamsRepository;
        this.branchRepository=branchRepository;
        this.shiftRepository=shiftRepository;
        this.positionRepository=positionRepository;
        this.employeeRepository=employeeRepository;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add( buildToolbar(), buildSummaryAndActions(), buildGrid(), buildLegend());
        expand(rosterGrid);

        // Initial period = current week
        //viewMode.setValue(ViewMode.WEEKLY);
        //anchorDate.setValue(LocalDate.now());
        //refreshPeriodAndData();
    }

    // =========================================================
    //  Layout pieces
    // =========================================================

    private Component buildToolbar() {
        // View mode: Weekly / Monthly
        viewMode.setLabel("View | របៀបមើល");
        viewMode.setItems(ViewMode.values());
        viewMode.setItemLabelGenerator(ViewMode::getLabel);
        viewMode.addValueChangeListener(e -> refreshPeriodAndData());

        // Anchor date (any date inside the week/month you want to show)
        anchorDate.setLabel("Anchor date | កាលបរិច្ឆេទយោង");
        anchorDate.addValueChangeListener(e -> refreshPeriodAndData());

        Button previous = new Button(new Icon(VaadinIcon.ANGLE_LEFT));
        previous.setAriaLabel("Previous | មុន");
        previous.addClickListener(e -> navigatePeriod(-1));

        Button next = new Button(new Icon(VaadinIcon.ANGLE_RIGHT));
        next.setAriaLabel("Next | បន្ទាប់");
        next.addClickListener(e -> navigatePeriod(1));

        Button today = new Button("Today | ថ្ងៃនេះ", new Icon(VaadinIcon.CALENDAR_O));
        today.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        today.addClickListener(e -> {
            anchorDate.setValue(LocalDate.now());
            refreshPeriodAndData();
        });

        advanceFilterTeam.setItems(this.teamsRepository.findAll());
        advanceFilterTeam.setItemLabelGenerator(Teams::getTeamCode);
        advanceFilterTeam.addValueChangeListener(e -> this.refreshPeriodAndData());
        
        advanceFilterBranch.setItems(this.branchRepository.findAll());
        advanceFilterBranch.setItemLabelGenerator(Branch::getBranchShortName);
        advanceFilterBranch.addValueChangeListener(e -> this.refreshPeriodAndData());
        
        advanceFilterShift.setItems(this.shiftRepository.findAll());
        advanceFilterShift.setItemLabelGenerator(Shift::getShiftName);
        advanceFilterShift.addValueChangeListener(e -> this.refreshPeriodAndData());
        
        advanceFilterPosition.setItems(this.positionRepository.findAll());
        advanceFilterPosition.setItemLabelGenerator(Positions::getPosition);
        advanceFilterPosition.addValueChangeListener(e -> this.refreshPeriodAndData());
        
        advanceFilterEmployee.setItems(this.employeeRepository.findLocalStaffAllStatuses());
        advanceFilterEmployee.setItemLabelGenerator(e->e.getNameEn() + " | " + e.getNameKh()  + "-" + e.getInsuranceNo().toString() );
        advanceFilterEmployee.addValueChangeListener(e -> this.refreshPeriodAndData());
        
        HorizontalLayout toolbar = new HorizontalLayout(viewMode, anchorDate, previous, next, today,periodSummary, refresh);
        viewMode.addThemeVariants(SelectVariant.LUMO_SMALL);
        anchorDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        previous.addThemeVariants(ButtonVariant.LUMO_SMALL);
        next.addThemeVariants(ButtonVariant.LUMO_SMALL);
        today.addThemeVariants(ButtonVariant.LUMO_SMALL);
        
        refresh.addThemeVariants(ButtonVariant.LUMO_SMALL);
        
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setMargin(false);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle()
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("margin-bottom", "0");

        return toolbar;
    }

    private Component buildSummaryAndActions() {
        periodSummary.getStyle()
                .set("font-weight", "500")
                .set("font-size", "var(--lumo-font-size-m)");


        refresh.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refresh.addClickListener(e -> refreshPeriodAndData());

        HorizontalLayout layout = new HorizontalLayout(advanceFilterEmployee,advanceFilterPosition,advanceFilterShift,advanceFilterBranch,advanceFilterTeam);
        layout.setWidthFull();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setMargin(false);
        advanceFilterEmployee.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL.LUMO_SMALL);
        advanceFilterPosition.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL.LUMO_SMALL);
        advanceFilterBranch.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL.LUMO_SMALL);
        advanceFilterTeam.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL.LUMO_SMALL);
        advanceFilterShift.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL.LUMO_SMALL);
       // layout.setAlignItems(FlexComponent.Alignment.CENTER);
        //layout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        //layout.getStyle()
        //        .set("margin-top", "0")
        //        .set("margin-bottom", "0");

        return layout;
    }

    private Component buildGrid() {
        rosterGrid.setSizeFull();
        rosterGrid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT
        );
        rosterGrid.addClassName("roster-grid");

        // First column: Employee name (change to use your real fields)
        rosterGrid.addColumn(row -> {return row.getEmployee() != null ? this.safeJoin("-",this.safeJoin(" | ",row.getEmployee().getNameEn(),  row.getEmployee().getNameKh()) ,row.getEmployee().getInsuranceNo().toString()) : "";
        }).setHeader("Employee").setAutoWidth(true).setFlexGrow(0).setFrozen(true);
        
        rosterGrid.addColumn(row -> {
            EmployeeAllocation alloc = row.getEmployeeAllocation();
            if (alloc == null) {
                return "";
            }
            Positions pos = alloc.getPositions();
            return pos != null ? pos.getPosition() : "";
        }).setHeader("Position");
        
        rosterGrid.addColumn(row -> {
            EmployeeAllocation alloc = row.getEmployeeAllocation();
            if (alloc == null) {
                return "";
            }
             Shift shift = alloc.getShiftCycle().getShift();
            return shift != null ? shift.getShiftName() : "";
        }).setHeader("Shift");
        
        rosterGrid.addColumn(row -> {
            EmployeeAllocation alloc = row.getEmployeeAllocation();
            if (alloc == null) {
                return "";
            }
             Branch branch = alloc.getBranch();
            return branch != null ? branch.getBranchShortName() : "";
        }).setHeader("Location");
        
        rosterGrid.addColumn(row -> {
            EmployeeAllocation alloc = row.getEmployeeAllocation();
            if (alloc == null) {
                return "";
            }
            Teams team = alloc.getTeams();
            return team != null ? team.getTeamCode() : "";
        }).setHeader("Team");
        
        rosterGrid.getColumns().forEach(column -> {
            column.setResizable(true);   // Enable resizing for all columns
            column.setSortable(true);    // Enable sorting for all columns
            column.setTextAlign(ColumnTextAlign.CENTER);
            column.setAutoWidth(true);
            
        });
        
        return rosterGrid;
    }

    
    private String safeJoin(String separator, String... parts) {
        return Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(separator));
    }
    private Component buildLegend() {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setPadding(true);
        legend.setSpacing(true);
        legend.setAlignItems(FlexComponent.Alignment.CENTER);
        legend.getStyle()
                .set("border-top", "1px solid var(--lumo-contrast-10pct)");

        legend.add(
                legendItem("#42a5f5", "Working Days | ថ្ងៃធ្វើការ"),
                legendItem("#FCE8E6", "Off Days | ថ្ងៃឈប់សម្រាក"),
                legendItem("#4caf50", "Public Holidays | ថ្ងៃឈប់សម្រាកបុណ្យ"),
                legendItem("#e53935", "Leave (Full day) | ឈប់ពេញថ្ងៃ"),
                legendItem("#ffb300", "Leave (Morning only) | ឈប់ព្រឹក"),
                legendItem("#8e24aa", "Leave (Afternoon only) | ឈប់រសៀល")
        );

        return legend;
    }


    private Component legendItem(String color, String label) {
        Div box = new Div();
        box.getStyle()
                .set("width", "14px")
                .set("height", "14px")
                .set("border-radius", "4px")
                .set("background-color", color);

        Span text = new Span(label);

        HorizontalLayout item = new HorizontalLayout(box, text);
        //item.setSpacing(5, Unit.REM);
        item.setAlignItems(FlexComponent.Alignment.CENTER);
        return item;
    }

    // =========================================================
    //  Period + data loading
    // =========================================================

    private void navigatePeriod(int step) {
        if (anchorDate.getValue() == null) {
            anchorDate.setValue(LocalDate.now());
        }

        LocalDate anchor = anchorDate.getValue();
        ViewMode mode = viewMode.getValue() != null ? viewMode.getValue() : ViewMode.WEEKLY;

        switch (mode) {
            case WEEKLY -> anchorDate.setValue(anchor.plusWeeks(step));
            case MONTHLY -> anchorDate.setValue(anchor.plusMonths(step));
        }

        refreshPeriodAndData();
    }

    private void refreshPeriodAndData() {
        LocalDate anchor = anchorDate.getValue();
        if (anchor == null) {
            anchor = LocalDate.now();
            anchorDate.setValue(anchor);
        }

        ViewMode mode = viewMode.getValue() != null ? viewMode.getValue() : ViewMode.WEEKLY;

        if (mode == ViewMode.MONTHLY) {
            periodStart = anchor.withDayOfMonth(1);
            periodEnd = anchor.withDayOfMonth(anchor.lengthOfMonth());
        } else {
            LocalDate monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            periodStart = monday;
            periodEnd = monday.plusDays(6);
        }

        visibleDates.clear();
        LocalDate d = periodStart;
        while (!d.isAfter(periodEnd)) {
            visibleDates.add(d);
            d = d.plusDays(1);
        }

        buildDateColumns();
        loadRosterData();
        updatePeriodSummary();
    }

    private void updatePeriodSummary() {
        long days = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        String text = PERIOD_SUMMARY_FORMAT.format(periodStart)
                + " – "
                + PERIOD_SUMMARY_FORMAT.format(periodEnd)
                + " (" + days + " days | " + days + " ថ្ងៃ)";
        periodSummary.setText(text);
    }

    private void buildDateColumns() {
        // Remove old date columns
        for (Grid.Column<RosterRow> col : dateColumns) {
            rosterGrid.removeColumn(col);
        }
        dateColumns.clear();

        // Add new date columns
        for (LocalDate date : visibleDates) {
            Grid.Column<RosterRow> col = rosterGrid
                    .addComponentColumn(row -> createShiftCell(row, date))
                    .setHeader(createDateHeader(date))
                    .setAutoWidth(false)
                    .setWidth("96px")
                    .setFlexGrow(0);
            dateColumns.add(col);
        }
    }

    private Component createDateHeader(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;

        Span dayName = new Span(dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        dayName.getStyle().set("font-size", "11px");

        Span dayNumber = new Span(HEADER_DAY_FORMAT.format(date));
        dayNumber.getStyle()
                .set("font-weight", "600")
                .set("font-size", "14px");

        Span month = new Span(HEADER_MONTH_FORMAT.format(date));
        month.getStyle().set("font-size", "10px");

        VerticalLayout header = new VerticalLayout(dayName, dayNumber, month);
        header.setPadding(false);
        header.setSpacing(false);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("border-radius", "8px")
                .set("padding", "4px")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("background-color", weekend ? "var(--lumo-error-color-10pct)" : "var(--lumo-base-color)");

        return header;
    }

    private Component createShiftCell(RosterRow row, LocalDate date) {
        EmployeeRoster roster = row.getRosterFor(date);

        Div cell = new Div();
        cell.addClassName("shift-cell");
        cell.getStyle()
                .set("border-radius", "10px")
                .set("padding", "2px 4px")
                .set("min-height", "32px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "12px")
                .set("cursor", "pointer");

        if (roster == null) {
            cell.setText("+");
            cell.getStyle()
                    .set("border", "1px dashed var(--lumo-contrast-20pct)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("background-color", "var(--lumo-base-color)");
        } else {
            cell.removeAll();

            Icon icon = buildShiftIcon(roster);
            Span label = new Span(buildShiftLabel(roster));
            label.getStyle()
                    .set("font-size", "11px")
                    .set("white-space", "nowrap");

            if (icon != null) {
                icon.setSize("14px");
                icon.getStyle().set("margin-right", "4px");
                cell.add(icon);
            }
            cell.add(label);

            String color = determineColor(roster);
            cell.getStyle()
                    .set("background-color", color)
                    .set("color", "white");

            // Tooltip with richer info
            Tooltip tip = Tooltip.forComponent(cell);
            tip.setPosition(TooltipPosition.TOP_START);
            tip.setText(buildTooltipText(roster, date));
        }

        cell.getElement().addEventListener("click", e -> openEditDialog(row, date));
        return cell;
    }


    private String buildShiftLabel(EmployeeRoster roster) {
        if (roster == null) return "";

        String returnValue = "";

        Holiday h = roster.getHoliday();
        LocalTime start = roster.getStartTime();
        LocalTime end = roster.getEndTime();

        String workingTime = (start != null && end != null)
                ? TIME_FORMAT.format(start) + "-" + TIME_FORMAT.format(end)
                : "";

        if (h == null || h.getHolidayGroup() == null || h.getHolidayGroup().getId() == null) {
            // fallback: if no holiday/group, treat as working day
            return !workingTime.isBlank() ? workingTime : "Working Day";
        }

        Long groupId = h.getHolidayGroup().getId();

        if (groupId.equals(1L) || groupId.equals(7L) || groupId.equals(8L)) {
            returnValue = !workingTime.isBlank() ? workingTime : "Working Day";

        } else if (groupId.equals(4L) || groupId.equals(6L)) {
            returnValue = "Off";

        } else if (groupId.equals(2L) || groupId.equals(3L)) {
            returnValue = "Holiday";

        } else if (groupId.equals(5L) || groupId.equals(9L)) {
            // ✅ FIX: leaveDuration can be null
            var duration = roster.getLeaveDuration();
            if (duration == null) {
                returnValue = "Leave"; // fallback
            } else {
                // ✅ better: switch on enum, not toString()
                returnValue = switch (duration) {
                    case FULL_DAY -> "Leave";
                    case MORNING_ONLY -> "AM Leave";
                    case AFTERNOON_ONLY -> "PM Leave";
                };
            }
        }

        return returnValue;
    }



    private String determineColor(EmployeeRoster roster) {

        Holiday h = roster.getHoliday();
        if (h != null) {
        	//System.out.println("Day:"+roster.getRosterDate() +" Group ID:"+h.getHolidayGroup().getId());
        	if(h.getHolidayGroup().getId().equals(1L) || h.getHolidayGroup().getId().equals(7L) || h.getHolidayGroup().getId().equals(8L)) {  
        		return "#42a5f5";  //working day
        	}else if (h.getHolidayGroup().getId().equals(4L) ||h.getHolidayGroup().getId().equals(6L)){
        		return "#FCE8E6"; //Day Off
        	}else if (h.getHolidayGroup().getId().equals(2L) || h.getHolidayGroup().getId().equals(3L)){
        		return "#4caf50"; //Public Holidays
        	}else if (h.getHolidayGroup().getId().equals(5L) || h.getHolidayGroup().getId().equals(9L)){
        		if (roster.getLeaveDuration() != null) {
                    return switch (roster.getLeaveDuration().toString()) {
                        case "FULL_DAY" -> "#e53935";   // red
                        case "MORNING_ONLY" -> "#ffb300"; // amber
                        case "AFTERNOON_ONLY" -> "#8e24aa"; // purple
                        default -> "#e53935"; // Full Day Leave
                    };
                }
        		
        		return "#e53935"; // Full Day Leave
        	}else {        	
        		return "#fb8c00"; //working day
        	}
            
        }else {
        	return "#42a5f5";  //working day
        }

    }
    private Icon buildShiftIcon(EmployeeRoster roster) {
        Holiday h = roster.getHoliday();

        // No holiday → normal working day
        if (h == null || h.getHolidayGroup() == null) {
            return VaadinIcon.CLOCK.create(); // working day
        }

        Long groupId = h.getHolidayGroup().getId();

        if (groupId == 1L || groupId == 7L || groupId == 8L) {
            // Working day
            return VaadinIcon.CLOCK.create();
        }

        if (groupId == 4L || groupId == 6L) {
            // Day off (OFF)
            return VaadinIcon.UMBRELLA.create();
        }

        if (groupId == 2L || groupId == 3L) {
            // Public holiday
            return VaadinIcon.GIFT.create();
        }

        if (groupId == 5L || groupId == 9L) {
            // Leave (personal / annual etc.)
            var duration = roster.getLeaveDuration();
            if (duration == null) {
                return VaadinIcon.SUITCASE.create(); // fallback
            }

            return switch (duration) {
                case FULL_DAY      -> VaadinIcon.SUITCASE.create(); // full-day leave
                case MORNING_ONLY  -> VaadinIcon.SUN_O.create();    // morning only
                case AFTERNOON_ONLY-> VaadinIcon.MOON_O.create();   // afternoon only
            };
        }

        // Default fallback
        return VaadinIcon.CLOCK.create();
    }

    
    private String buildTooltipText(EmployeeRoster roster, LocalDate date) {
        if (roster == null) return "";

        StringBuilder sb = new StringBuilder();

        sb.append(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
          .append(" ")
          .append(PERIOD_SUMMARY_FORMAT.format(date))
          .append("\n");

        Holiday holiday = roster.getHoliday();
        Long groupId = (holiday != null && holiday.getHolidayGroup() != null)
                ? holiday.getHolidayGroup().getId()
                : null;

        boolean isLeaveGroup = groupId != null && (groupId == 5L || groupId == 9L);

        if (isLeaveGroup) {
            sb.append("Leave: ");

            var duration = roster.getLeaveDuration(); // ✅ can be null
            if (duration == null) {
                sb.append("Full day"); // fallback
            } else {
                sb.append(switch (duration) {
                    case FULL_DAY -> "Full day";
                    case MORNING_ONLY -> "Morning only";
                    case AFTERNOON_ONLY -> "Afternoon only";
                });
            }

            if (roster.getLeaveType() != null) {
                sb.append(" (")
                  .append(roster.getLeaveType().getLeaveNameEn());
                sb.append(" | ").append(roster.getLeaveType().getLeaveNameKh());
                sb.append(")");
            }

            if (roster.getLeaveTypeSubType() != null) {
                sb.append("\nSubtype: ")
                  .append(roster.getLeaveTypeSubType().getLeaveSubTypeNameEn());
            }

            sb.append("\n");
        } else {
            if (holiday != null) {
                sb.append("Day: ")
                  .append(safeJoin(" | ", holiday.getHolidayName(), holiday.getHolidayNameKh()))
                  .append("\n");
            }
        }

        if (roster.getStartTime() != null && roster.getEndTime() != null) {
            sb.append("Time Work: ")
              .append(TIME_FORMAT.format(roster.getStartTime()))
              .append(" - ")
              .append(TIME_FORMAT.format(roster.getEndTime()))
              .append("\n");
        }

        if (roster.getRemark() != null && !roster.getRemark().isBlank()) {
            sb.append("Remark: ").append(roster.getRemark()).append("\n");
        }

        return sb.toString().trim();
    }


    private void loadRosterData() {
        currentRows.clear();

        // Filter by current period [periodStart, periodEnd]
        Specification<EmployeeRoster> spec = (root, query, cb) -> {
            query.distinct(true);
            return cb.between(root.get("rosterDate"), periodStart, periodEnd);
        };
      
        // ✅ Employee filter
        if (advanceFilterEmployee.getValue() != null && !advanceFilterEmployee.getValue().isEmpty()) {
        	List<Long> Ids = advanceFilterEmployee.getValue().stream()
                            .map(Employee::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            spec = spec.and((root, query, cb) -> root.get("employee").get("id").in(Ids));
        }
        
        // ✅ Position filter
        if (advanceFilterPosition.getValue() != null && !advanceFilterPosition.getValue().isEmpty()) {
        	List<Long> Ids = advanceFilterPosition.getValue().stream()
                            .map(Positions::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            spec = spec.and((root, query, cb) -> root.get("employeeAllocation").get("positions").get("id").in(Ids));
        }
        
        // ✅ Location filter
        if (advanceFilterBranch.getValue() != null && !advanceFilterBranch.getValue().isEmpty()) {
        	List<Long> Ids = advanceFilterBranch.getValue().stream()
                            .map(Branch::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            spec = spec.and((root, query, cb) -> root.get("employeeAllocation").get("branch").get("id").in(Ids));
        }
        
        // ✅ Team filter
        if (advanceFilterTeam.getValue() != null && !advanceFilterTeam.getValue().isEmpty()) {
        	List<Long> Ids = advanceFilterTeam.getValue().stream()
                            .map(Teams::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            spec = spec.and((root, query, cb) -> root.get("employeeAllocation").get("teams").get("id").in(Ids));
        }
        
        // ✅ Shift filter
        if (advanceFilterShift.getValue() != null && !advanceFilterShift.getValue().isEmpty()) {
        	List<Long> Ids = advanceFilterShift.getValue().stream()
                            .map(Shift::getId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            spec = spec.and((root, query, cb) -> root.get("employeeAllocation").get("shiftCycle").get("shift").get("id").in(Ids));
        }
        
                
        List<EmployeeRoster> entries = rosterService.findAll(spec);

        // Group by employee
        Map<Employee, RosterRow> rowMap = new LinkedHashMap<>();

        for (EmployeeRoster er : entries) {
            Employee emp = er.getEmployee();
            if (emp == null) {
                continue;
            }
            RosterRow row = rowMap.computeIfAbsent(emp, RosterRow::new);
            row.putRoster(er.getRosterDate(), er);
        }

        currentRows.addAll(rowMap.values());
        rosterGrid.setItems(currentRows);
        
        
    }

    // =========================================================
    //  Edit dialog
    // =========================================================

    private void openEditDialog(RosterRow row, LocalDate date) {
        EmployeeRoster existing = row.getRosterFor(date);
        
        

        boolean isNew = existing == null;
        EmployeeRoster working = isNew ? new EmployeeRoster() : existing;
        
        if(working.getHoliday().getHolidayGroup().getId()==9L) {
        	Notification.show("You can’t edit this leave here. If you need to make changes, please do it in the Leave module. \n មិនអាចកែសម្រួលការឈប់សម្រាកនេះបានទេ។ ប្រសិនបើត្រូវការកែប្រែ សូមធ្វើនៅក្នុងម៉ូឌុលការឈប់សម្រាក។", 9000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);;
        	return;
        }

        if (isNew) {
            working.setEmployee(row.getEmployee());
            working.setRosterDate(date);
            working.setSourceType("MANUAL_OVERRIDE");

            // Try to reuse any existing shift cycle for that employee in this period
            ShiftCycle fallbackCycle = row.findFallbackShiftCycle();
            if (fallbackCycle != null) {
                working.setShiftCycle(fallbackCycle);
            }
        }

        CustomDialog dialog = new CustomDialog("Edit Roster for Employee | កែប្រែកាលវិភាគវេនសម្រាប់បុគ្គលិក");
        //dialog.setHeaderTitle("Edit shift | កែប្រែវេន");
        ComboBox<Holiday> holidayField=new ComboBox<Holiday>("Day Status | ស្ថានភាព");
        //holidayField.setItems(this.holidayRepository.findByHolidayDateYearOrNull(this.anchorDate.getValue().getYear()));
        holidayField.setItems(this.holidayRepository.findByHolidayDateYearAndNotInIds(Integer.valueOf( this.anchorDate.getValue().getYear()),List.of(39L) ));
        holidayField.setItemLabelGenerator(holiday -> {
            String names = this.safeJoin(" | ", holiday.getHolidayName(), holiday.getHolidayNameKh());
            if (holiday.getHolidayDate() == null) {
                return names; 
            }
            String formattedDate = holiday.getHolidayDate().format( DateTimeUtilFormart.DATE_FORMATTER);            
            return this.safeJoin(" - ", names, formattedDate);
        });
        
        holidayField.setValue(working.getHoliday());
        
      
        
        TimePicker startField = new TimePicker("Start | ចាប់ផ្តើម");
        startField.setStep(Duration.ofMinutes(15));

        startField.setValue(working.getStartTime());

        TimePicker endField = new TimePicker("End | បញ្ចប់");
        endField.setStep(Duration.ofMinutes(15));
        endField.setValue(working.getEndTime());

        IntegerField breakField = new IntegerField("Break (min) | ពេលសម្រាក (នាទី)");       
        breakField.setMin(0);
        if (working.getBreakMinutes() != null) {
            breakField.setValue(working.getBreakMinutes());
        }

        TextArea remarkField = new TextArea("Remark | ចំណាំ");
        remarkField.setWidthFull();
        remarkField.setMaxLength(255);
        remarkField.setValue(working.getRemark() != null ? working.getRemark() : "");

        remarkField.setHelperText("e.g. Rest Day, Leave, Night shift...");

        FormLayout form = new FormLayout(holidayField,startField, endField, breakField, remarkField);
        form.setColspan(holidayField,2);
        form.setColspan(remarkField,2);
        form.setWidthFull();

        Button cancel = new Button("Cancel | បោះបង់", e -> dialog.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY,ButtonVariant.LUMO_ERROR);

        Button save = new Button("Save | រក្សាទុក", new Icon(VaadinIcon.CHECK));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(click -> {
            try {
            	working.setHoliday(holidayField.getValue());
                working.setStartTime(startField.getValue());
                working.setEndTime(endField.getValue());
                working.setBreakMinutes(breakField.getValue());
                working.setRemark(remarkField.getValue());
                working.setSourceType("MANUAL_OVERRIDE");

                rosterService.update(working);

                Notification.show("Roster saved successfully", 3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                dialog.close();
                refreshPeriodAndData();
            } catch (Exception ex) {
                Notification.show("Roster to save shift: " + ex.getMessage(),5000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout footerLayout = new HorizontalLayout(cancel, save);
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        dialog.add(form);
        dialog.getFooter().add(footerLayout);
        //dialog.setWidth("420px");
        dialog.open();
        
        // 🔹 INITIAL visibility based on current value
        updateShiftFieldsVisibility(holidayField.getValue(), startField, endField, breakField);

        // 🔹 React when user changes Day Status
        holidayField.addValueChangeListener(e -> {
            Holiday selected = e.getValue();
            updateShiftFieldsVisibility(selected, startField, endField, breakField);
        });
    }
    private void updateShiftFieldsVisibility(
            Holiday holiday,
            TimePicker startField,
            TimePicker endField,
            IntegerField breakField
    ) {
        boolean isWorkingDayGroup = false;

        if (holiday != null && holiday.getHolidayGroup() != null) {
            Long groupId = holiday.getHolidayGroup().getId();
            if (groupId != null && (groupId == 1L || groupId == 7L || groupId == 8L)) {
                isWorkingDayGroup = true;
            }
        }

        // Show time fields only for working-day groups 1,7,8
        startField.setVisible(isWorkingDayGroup);
        endField.setVisible(isWorkingDayGroup);
        breakField.setVisible(isWorkingDayGroup);

        // Optional: clear values when not working day
        if (!isWorkingDayGroup) {
            startField.clear();
            endField.clear();
            breakField.clear();
        }
    }

    
    // =========================================================
    //  Helper classes
    // =========================================================

    private enum ViewMode {
        WEEKLY("Weekly | តាមសប្តាហ៍"),
        MONTHLY("Monthly | តាមខែ");

        private final String label;

        ViewMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var current = authenticatedUser.get();
        //System.out.println(">>> RosterView user = " + current.map(User::getName).orElse("anonymous"));

        boolean allowed = authenticatedUser.hasPage(RosterView.class, AccessPageType.SELECTED_PAGE);
        //System.out.println(">>> hasPage(RosterView, SELECTED_PAGE) = " + allowed);

        if (!allowed) {
            //System.out.println(">>> Rerouting to AccessDeniedView");
            event.rerouteTo(AccessDeniedView.class);
            return;
        }

        //System.out.println(">>> Access granted to RosterView");

        // ✅ Only load data AFTER permission is confirmed
        if (anchorDate.getValue() == null) {
            anchorDate.setValue(LocalDate.now());
        }
        if (viewMode.getValue() == null) {
            viewMode.setValue(ViewMode.MONTHLY);
        }
        refreshPeriodAndData();
    }
    
  

}
