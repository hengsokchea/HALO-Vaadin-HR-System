package org.halocambodia.views.roster;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.dom.Style.AlignItems;
import com.vaadin.flow.dom.Style.FontWeight;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.PermitAll;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.component.SignaturePad;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Attachment;
import org.halocambodia.data.AttachmentRepository;
import org.halocambodia.data.Branch;
import org.halocambodia.data.BranchRepository;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.GoalSetting;
import org.halocambodia.data.GoalSettingDetail;
import org.halocambodia.data.HREmployeeData;
import org.halocambodia.data.HREmployeeRepository;
import org.halocambodia.data.HREmployeeWithSupervisor;
import org.halocambodia.data.Holiday;
import org.halocambodia.data.HolidayRepository;
import org.halocambodia.data.Positions;
import org.halocambodia.data.Shift;
import org.halocambodia.data.ShiftCycle;
import org.halocambodia.data.ShiftCycleDetail;
import org.halocambodia.data.ShiftDayRow;
import org.halocambodia.data.ShiftRepository;
import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.EmployeeLeaveDetail;
import org.halocambodia.data.FileSizeFormatter;
import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.data.Unit;
import org.halocambodia.data.UnitRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.GoalSettingEntryService;
import org.halocambodia.services.GoalSettingService;
import org.halocambodia.services.ShiftCycleService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.MasterDetailLayout;
import org.halocambodia.views.MasterPageDialogLayout;
import org.halocambodia.views.PreviewReport;
import org.halocambodia.views.access_denied.AccessDeniedView;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.grid.FooterRow;


@PageTitle("Calendar")
@Route(value = "calendar", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class CalendarView  extends MasterPageDialogLayout<ShiftCycle, ShiftCycleService> implements BeforeEnterObserver{
	
    private ComboBox<Shift> shift=new ComboBox<Shift>();
    private IntegerField year =new IntegerField();
    private Checkbox includeWeekends= new Checkbox("Include Weekends");
    
    Grid<ShiftDayRow> gridShiftDetail = new Grid<>(ShiftDayRow.class, false); 
    private FooterRow gridDetailFooter;
    Button btnGenerateShift=new Button("Generate Shift Calendar | បង្កើតប្រតិទិនតាមវេន", new Icon(VaadinIcon.AUTOMATION),e->{
    	ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Generate Shift Calendar | បង្កើតប្រតិទិនវេន");
        Span enLine1 = new Span("Before generating, please make sure you have set the public holidays.");
        Span enLine2 = new Span("This will create a shift calendar for the selected shift and year.");
        Span khLine1 = new Span("មុនពេលបង្កើត សូមប្រាកដថា អ្នកបានកំណត់ថ្ងៃឈប់សម្រាកសាធារណៈរួចរាល់ហើយ។");
        Span khLine2 = new Span("ប្រតិទិនវេននឹងត្រូវបានបង្កើតសម្រាប់វេន និងឆ្នាំដែលអ្នកបានជ្រើសរើស។");
        
        enLine1.getStyle().set("font-weight", "600");
        khLine1.getStyle().set("font-weight", "600");
        enLine2.getStyle().set("font-weight", "600");
        khLine2.getStyle().set("font-weight", "600");
        
        enLine1.getStyle().set("color", "var(--lumo-error-text-color)");
        khLine1.getStyle().set("color", "var(--lumo-error-text-color)");
        enLine2.getStyle().set("color", "var(--lumo-error-text-color)");
        khLine2.getStyle().set("color", "var(--lumo-error-text-color)");

        VerticalLayout content = new VerticalLayout(enLine1, enLine2, khLine1, khLine2);
        content.setPadding(false);
        content.setSpacing(false);
        content.setMargin(false);

        dialog.add(content);       // use styled components instead
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel | បោះបង់");
        dialog.setConfirmText("Generate | បង្កើត");
        
        dialog.setConfirmButtonTheme("primary");
        dialog.setCancelButtonTheme("primary error");

        dialog.addConfirmListener(event -> generateShift());

        dialog.open();
    });
    
    //share data 
    private final ShiftRepository shiftRepository;
    private final HolidayRepository holidayRepository;
    private final Optional<User> currentUserLogin;   
    private final BeanValidationBinder<ShiftCycle> binder= new BeanValidationBinder<>(ShiftCycle.class);    
    
    //Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");    
    
    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");
    
 
	 public CalendarView(ShiftCycleService service,UserService userService,AuthenticatedUser authenticatedUser,ShiftRepository shiftRepository,HolidayRepository holidayRepository) { 
		 super(service,userService,authenticatedUser);
		 this.currentUserLogin=authenticatedUser.get();
		 this.shiftRepository=shiftRepository;
		 this.holidayRepository=holidayRepository;
	 }
	 
	 @Override
	    protected void onAttach(AttachEvent attachEvent) {
	        super.onAttach(attachEvent);
	        try {
		        UI.getCurrent().access(() -> {
		        	try {
						configureGrid();
					
						configureEditorLayout();    	        
						binderField();
						createAdvanceFilterLayout(); 
						configureGridDetail();
		        	} catch (Exception e) {
		        		showError("Error during UI setup: " + e.getMessage());
						e.printStackTrace();
					}
		        	
		        });
		       
		        this.shift.setItems(shiftRepository.findAll());
		        
		    } catch (Exception ex) {
		    	showError("Error initializing view: " + ex.getMessage());
		        ex.printStackTrace();
		    }

	}
	 	
	private void binderField() {
		binder.bindInstanceFields(this);
	}
	private void configureGridDetail() {

	   gridShiftDetail.removeAllColumns();
	    
	    gridShiftDetail.addColumn(ShiftDayRow::getDayOfMonth)
	        .setHeader(createHeader("Day"))
	        .setKey("dayOfMonth")
	        .setWidth("80px")          
	        .setFlexGrow(0)           
	        .setFrozen(true);

	    for (int m = 1; m <= 12; m++) {
	        final int month = m;
	        String monthName = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

	        // Use ComponentRenderer to add a clickable component to each cell
	        gridShiftDetail.addColumn(new ComponentRenderer<>(row -> {
	            ShiftCycleDetail detail = getShiftCycleDetailForMonth(row, Month.of(month));
	            
	            // Create a clickable container for the cell
	            Div cellContent = new Div();
	            cellContent.setSizeFull(); // 🔹 fill whole cell (width + height)
	            cellContent.getStyle()
	            .set("height", "100%")
	            .set("flex", "1 0 auto")     // stretch inside vaadin-grid-cell-content
	            .set("align-self", "stretch")
	            .set("display", "flex")
	            .set("alignItems", "center")
	            .set("justifyContent", "center")
	            .set("padding", "0")
	            .set("margin", "0")
	            .set("box-sizing", "border-box");
	            
	         
	            if (detail != null) {
	                String dayOfWeek = detail.getCycleDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
	                String holidayStatus = (detail.getHoliday() != null) ? " - " + detail.getHoliday().getHolidayCode()  : "";
	                
	                holidayStatus += (detail.getHoliday().getHolidayDate() != null) ? " ( " + detail.getHoliday().getHolidayDate() +" )"  : "";
	                

	                Tooltip tooltip = Tooltip.forComponent(cellContent);
	                tooltip.setText(dayOfWeek + holidayStatus);
	                cellContent.add(new Span(dayOfWeek + holidayStatus));
	                
	             
	                 
	                if(detail.getHoliday().getHolidayGroup().getHolidayBackgroundColor()!=null) {
	                	cellContent.getStyle().setBackgroundColor(detail.getHoliday().getHolidayGroup().getHolidayBackgroundColor());	
	                }	                	               
	                // Add a double-click listener to this specific cell component
	                cellContent.addDoubleClickListener(event -> {
	                    // This is the key: pass both the row and the specific detail to the dialog
	                    showHolidayChangeDialog(detail);
	                });
	            }
	            return cellContent;
	        }))
	        .setHeader(createHeader(monthName))
	        
	        .setFlexGrow(0);
	    }
	    
	    //gridShiftDetail.addThemeVariants(GridVariant.LUMO_COMPACT);
	    gridShiftDetail.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS,GridVariant.LUMO_WRAP_CELL_CONTENT);
	    gridShiftDetail.setAllRowsVisible(true);
	    
	    gridShiftDetail.getColumns().forEach(column -> {
            column.setResizable(true);   // Enable resizing for all columns
            column.setSortable(false);    // Enable sorting for all columns
            column.setTextAlign(ColumnTextAlign.CENTER);
            if(column.getKey()!=null && !column.getKey().equalsIgnoreCase("dayOfMonth")) {
            	column.setAutoWidth(true);
            }
            
        });
	 // Add the footer row and store it
	    gridDetailFooter = gridShiftDetail.appendFooterRow();
        Span footerColumnTotal=new Span("Totals:");
        footerColumnTotal.getStyle().setFontWeight(FontWeight.BOLD);
	    gridDetailFooter.getCell(gridShiftDetail.getColumnByKey("dayOfMonth")).setComponent(footerColumnTotal);
	    
	    // 🔹 Remove padding inside cells
	    gridShiftDetail.getStyle().set("--vaadin-grid-cell-padding", "0px");

	    // Optional: also tighten header & footer cells
	    gridShiftDetail.getStyle().set("--vaadin-grid-header-cell-padding", "0px");
	    gridShiftDetail.getStyle().set("--vaadin-grid-footer-cell-padding", "0px");
	    

	}
	private void updateGridDetailFooter(List<ShiftDayRow> dayRows) {
	    if (gridDetailFooter == null || dayRows == null) {
	        return;
	    }

	    // Get the year from the entity
	    int year = this.entity.getYear();

	    // Iterate through each month's column
	    for (int m = 1; m <= 12; m++) {
	        final int month = m;
	        
	        // Calculate the actual number of days for the specific month in the given year
	        int totalDaysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
	        
	        Set<Long> workingGroupIds = Set.of(1L, 7L, 8L);

	        long workingDays = dayRows.stream()
	            .map(row -> getShiftCycleDetailForMonth(row, Month.of(month)))
	            .filter(Objects::nonNull)
	            .filter(detail -> detail.getHoliday() != null && workingGroupIds.contains(detail.getHoliday().getHolidayGroup().getId()))
	            .count();
	            
	        // Find the correct column using its index or by looping
	        Grid.Column<ShiftDayRow> monthColumn = gridShiftDetail.getColumns().get(month); // Assuming "Day" is at index 0
	        Span footerColumn=new Span(workingDays + "/" + totalDaysInMonth);
	        footerColumn.getStyle().setFontWeight(FontWeight.BOLD);
	        gridDetailFooter.getCell(monthColumn).setComponent(footerColumn);
	        
	    }
	    updateEditorFooter(entity.getShiftCycleDetails());
	}
	private void updateEditorFooter(List<ShiftCycleDetail> details) {
	    if (editorLayout == null) {
	        return;
	    }

	    // Clear existing footer content to prevent duplicates
	    editorLayout.getFooter().removeAll();

	    // Group the details by holiday group ID and count them
	    Map<Long, Long> countsByGroup = details.stream()
	            .filter(detail -> detail.getHoliday() != null)
	            .collect(Collectors.groupingBy(
	                detail -> detail.getHoliday().getHolidayGroup().getId(),
	                Collectors.counting()
	            ));

	    long workingDays = countsByGroup.getOrDefault(1L, 0L) + countsByGroup.getOrDefault(7L, 0L) + countsByGroup.getOrDefault(8L, 0L);
	    long publicHolidays = countsByGroup.getOrDefault(2L, 0L);
	    long holidaysInLieu = countsByGroup.getOrDefault(3L, 0L);
	    long offDays = countsByGroup.getOrDefault(6L, 0L);
	    long pmm = countsByGroup.getOrDefault(4L, 0L);
	    long annualLeave = countsByGroup.getOrDefault(5L, 0L);
	    long totalDays = details.size();

	    HorizontalLayout footerLayout = new HorizontalLayout();
	    footerLayout.setJustifyContentMode(JustifyContentMode.CENTER);
	    footerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
	    footerLayout.setWidthFull();
	    footerLayout.getStyle().setFontWeight(FontWeight.BOLD);

	    // Create a label for each count
	    footerLayout.add(new Span("Working Days: " + workingDays));
	    footerLayout.add(new Span("Public Holidays: " + publicHolidays));
	    footerLayout.add(new Span("Holidays in Lieu: " + holidaysInLieu));
	    footerLayout.add(new Span("Off Days: " + offDays));
	    footerLayout.add(new Span("PMM: " + pmm));
	    footerLayout.add(new Span("Annual Leave: " + annualLeave));
	    footerLayout.add(new Span("Total days in " + year.getValue() +": "+ totalDays));

	    // Add some styling for better readability
	    //footerLayout.getStyle().set("padding", "0.5em 1em").set("gap", "1em");
	    

	    editorLayout.getFooter().add(footerLayout);
	}
	private Div createHeader(String text) {
	    Span headerSpan = new Span(text);
	    headerSpan.getStyle().set("fontWeight", "bold");
	    
	    Div headerDiv = new Div(headerSpan);
	    //headerDiv.getStyle().set("backgroundColor", "red");
	    //headerDiv.getStyle().set("color", "white");
	    headerDiv.getStyle().set("display", "flex");
	    headerDiv.getStyle().set("justifyContent", "center");
	    headerDiv.getStyle().set("alignItems", "center");
	    headerDiv.setWidthFull();
	    headerDiv.setHeightFull();
	    
	    return headerDiv;
	}
	private void showHolidayChangeDialog(ShiftCycleDetail detail) {
	    Dialog dialog = new Dialog();
	    dialog.setHeaderTitle("Change Holiday for " + detail.getCycleDate());

	    // Create the ComboBox to select a new holiday
	    ComboBox<Holiday> holidayComboBox = new ComboBox<>("Select Holiday");

	    // Get the year from the selected detail's date
	    int selectedYear = detail.getCycleDate().getYear();

	    // Get a list of Holiday IDs that are already assigned in this shift cycle
	    List<Long> usedHolidayIds = this.entity.getShiftCycleDetails().stream()
	            .map(ShiftCycleDetail::getHoliday)
	            .filter(Objects::nonNull)
	            .map(Holiday::getId)
	            .collect(Collectors.toList());

	    // Fetch holidays for the year that are not in the 'usedHolidayIds' list
	    holidayComboBox.setItems(holidayRepository.findByHolidayDateYearAndNotInIds(selectedYear, usedHolidayIds));

	    holidayComboBox.setWidthFull();
	    holidayComboBox.setItemLabelGenerator(holiday -> {
	        String dateString = (holiday.getHolidayDate() != null) ? holiday.getHolidayDate().toString() : "";
	        return holiday.getHolidayName() + " " + dateString;
	    });

	    Button saveButton = new Button("Save", e -> {
	        Holiday selectedHoliday = holidayComboBox.getValue();
	        if (selectedHoliday == null) {
	            showError("Please select a holiday.");
	            return;
	        }

	        try {
	            // Update the holiday using the specific detail ID
	            service.updateHolidayForDetailId(detail.getId(), selectedHoliday);

	            // Refresh the grid to show the updated data
	            this.entity = service.findCycle(this.entity.getShift().getId().intValue(), this.entity.getYear())
	                .orElse(null);

	            if (this.entity != null) {
	                List<ShiftDayRow> dayRows = ShiftDayRow.buildDayRows(this.entity.getShiftCycleDetails());
	                this.gridShiftDetail.setItems(dayRows);
	                updateGridDetailFooter(dayRows);
	                refreshGrid();
	            } else {
	                showError("The shift cycle was not found after saving.");
	            }

	            showSuccess("Holiday updated successfully!");
	        } catch (Exception ex) {
	            showError("Failed to update holiday: " + ex.getMessage());
	            ex.printStackTrace();
	        }
	        dialog.close();
	    });
	    saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
	    saveButton.addClickShortcut(Key.ENTER);

	    Button cancelButton = new Button("Cancel", e -> dialog.close());

	    dialog.add(holidayComboBox);
	    dialog.getFooter().add(saveButton, cancelButton);
	    dialog.open();
	}
	/**
	 * A helper method to get the correct ShiftCycleDetail object from the ShiftDayRow.
	 * You can add this helper method inside your ShiftView class.
	 */
	private ShiftCycleDetail getShiftCycleDetailForMonth(ShiftDayRow row, Month month) {
	    switch (month) {
	        case JANUARY: return row.getMonthJanuary();
	        case FEBRUARY: return row.getMonthFebruary();
	        case MARCH: return row.getMonthMarch();
	        case APRIL: return row.getMonthApril();
	        case MAY: return row.getMonthMay();
	        case JUNE: return row.getMonthJune();
	        case JULY: return row.getMonthJuly();
	        case AUGUST: return row.getMonthAugust();
	        case SEPTEMBER: return row.getMonthSeptember();
	        case OCTOBER: return row.getMonthOctober();
	        case NOVEMBER: return row.getMonthNovember();
	        case DECEMBER: return row.getMonthDecember();
	        default: return null;
	    }
	}
	


	@Override
	 protected void configureGrid() throws Exception {
	    	//grid.addColumn(ShiftCycle::getId).setHeader("ID").setFooter("Total Records:").setKey("id");
	   		grid.addColumn(
        	    new ComponentRenderer<>(selectedRow -> {
        	     
        	        Button btnGenerateReportForm = new Button(new Icon(VaadinIcon.PRINT),e->this.generateReportForm(selectedRow.getId()));    
        	        btnGenerateReportForm.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE);
        	        btnGenerateReportForm.setTooltipText("Click to generate the Calender.");

        	        // Create a layout to hold both buttons
        	        HorizontalLayout actionLayout = new HorizontalLayout(btnGenerateReportForm,new Span(selectedRow.getId().toString()));
        	        actionLayout.setSpacing(false);  // Remove spacing between buttons
        	        actionLayout.setPadding(false);  // Remove padding in layout
        	        actionLayout.setMargin(false);   // Remove margin in layout
        	        actionLayout.getStyle().set("padding", "0");  // Ensure no extra padding
        	        actionLayout.getStyle().set("margin", "0");   // Ensure no extra margin
        	        //actionLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        	        
        	        return actionLayout;
        	    })
        	).setHeader("ID").setTextAlign(ColumnTextAlign.START).setAutoWidth(true).setSortProperty("id").setKey("id");
	   		
	        grid.addColumn(ShiftCycle::getYear).setHeader("Year").setKey("year");
	        
	        grid.addColumn(entityRowSelect -> {
	            return entityRowSelect.getShift() != null ? entityRowSelect.getShift().getShiftName() : ""; // Display the username or a default value
	        }).setHeader("Shift")
	        .setSortProperty("shift.shiftName").setKey("shift.shiftName");
	        
	        grid.addColumn(ShiftCycle::getIncludeWeekends).setHeader(this.includeWeekends.getLabel()).setKey("includeWeekends");
	        


	        grid.addColumn(entityRowUserCreated -> {
	            User userCreated = entityRowUserCreated.getUserCreated(); // Get the related User object
	            return userCreated != null ? userCreated.getName() : ""; // Display the username or a default value
	        }).setHeader("Created By")
	        .setSortProperty("userCreated.name").setKey("userCreated.name");
	        
	        grid.addColumn(entityRowUserCreatedAt -> {        	
	        	return entityRowUserCreatedAt.getCreatedAt() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRowUserCreatedAt.getCreatedAt()) : "";
	        }).setHeader("Created At") 
	        .setSortProperty("createdAt").setKey("createdAt");
	        
	        grid.addColumn(entityRowUserUpdated -> {
	            User userUpdated = entityRowUserUpdated.getUserUpdated(); // Get the related User object
	            return userUpdated != null ? userUpdated.getName() : ""; // Display the username or a default value
	        }).setHeader("Updated By")
	        .setSortProperty("userUpdated.name").setKey("userUpdated.name");
	        
	        grid.addColumn(entityRowUserUpdateddAt -> {        	
	        	return entityRowUserUpdateddAt.getCreatedAt() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRowUserUpdateddAt.getUpdatedAt()) : "";
	        }).setHeader("Updated At") 
	        .setSortProperty("updatedAt").setKey("updatedAt");

	        grid.getColumns().forEach(column -> {
	            column.setResizable(true);   // Enable resizing for all columns
	            column.setSortable(true);    // Enable sorting for all columns
	            column.setTextAlign(ColumnTextAlign.CENTER);
	            if(column.getHeaderText() !="ID") {
	            	 column.setAutoWidth(true);
	            }	           
	        });

	        createShowHideColumnGridToolBar();   
	        
	        grid.setAllRowsVisible(true);
	        
	        grid.setItemDetailsRenderer(this.createTabRenderer());
	        
	 }
    private void generateReportForm(Long id) {
        if (id == null) {
            Notification.show("ID cannot be null or empty.",  9000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("shift_cycle_id", id); 

        PreviewReport previewReport =  new PreviewReport("report_embed/calender.jasper", parameters);
        previewReport.open();
    }
    
	   private ComponentRenderer<Component, ShiftCycle> createTabRenderer() {
	        return new ComponentRenderer<>(entityRecord -> {
	            TabSheet tabSheet = new TabSheet();
	            tabSheet.setSizeFull();
	            
	        	Grid<ShiftDayRow> gridDetailTab =new Grid <>(ShiftDayRow.class, false);
	        	gridDetailTab.setSizeFull();
	        	
	            tabSheet.add("Calender Detail | ព័ត៌មានលម្អិត", gridDetailTab);
	            	           
	            gridDetailTab.addColumn(ShiftDayRow::getDayOfMonth)
		        .setHeader(createHeader("Day"))
		        .setKey("dayOfMonth")
		        .setWidth("80px")          
		        .setFlexGrow(0)           
		        .setFrozen(true);

		    for (int m = 1; m <= 12; m++) {
		        final int month = m;
		        String monthName = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

		        // Use ComponentRenderer to add a clickable component to each cell
		        gridDetailTab.addColumn(new ComponentRenderer<>(row -> {
		            ShiftCycleDetail detail = getShiftCycleDetailForMonth(row, Month.of(month));
		            
		            // Create a clickable container for the cell
		            Div cellContent = new Div();
		            cellContent.setSizeFull(); // 🔹 fill whole cell (width + height)
		            cellContent.getStyle()
		            .set("height", "100%")
		            .set("flex", "1 0 auto")     // stretch inside vaadin-grid-cell-content
		            .set("align-self", "stretch")
		            .set("display", "flex")
		            .set("alignItems", "center")
		            .set("justifyContent", "center")
		            .set("padding", "0")
		            .set("margin", "0")
		            .set("box-sizing", "border-box");
		            
		         
		            if (detail != null) {
		                String dayOfWeek = detail.getCycleDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
		                String holidayStatus = (detail.getHoliday() != null) ? " - " + detail.getHoliday().getHolidayCode()  : "";
		                
		                holidayStatus += (detail.getHoliday().getHolidayDate() != null) ? " ( " + detail.getHoliday().getHolidayDate() +" )"  : "";
		                

		                Tooltip tooltip = Tooltip.forComponent(cellContent);
		                tooltip.setText(dayOfWeek + holidayStatus);
		                cellContent.add(new Span(dayOfWeek + holidayStatus));		                		            
		                 
		                if(detail.getHoliday().getHolidayGroup().getHolidayBackgroundColor()!=null) {
		                	cellContent.getStyle().setBackgroundColor(detail.getHoliday().getHolidayGroup().getHolidayBackgroundColor());	
		                }	                	               
		            }
		            return cellContent;
		        }))
		        .setHeader(createHeader(monthName))
		        
		        .setFlexGrow(0);
		    }
		     
		    gridDetailTab.setItems(ShiftDayRow.buildDayRows(entityRecord.getShiftCycleDetails()));
	        	 
	        gridDetailTab.setAllRowsVisible(true);
	        
	        gridDetailTab.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
	        gridDetailTab.setAllRowsVisible(true);
		    
	        gridDetailTab.getColumns().forEach(column -> {
	            column.setResizable(true);   // Enable resizing for all columns
	            column.setSortable(false);    // Enable sorting for all columns
	            column.setTextAlign(ColumnTextAlign.CENTER);
	            if(column.getKey()!=null && !column.getKey().equalsIgnoreCase("dayOfMonth")) {
	            	column.setAutoWidth(true);
	            }
	            
	        });
		    
	        return tabSheet;
	    });
	}

	   

	 @Override
	 protected void configureEditorLayout() throws Exception{
		 btnGenerateShift.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		 HorizontalLayout shiftToolBar = new HorizontalLayout(this.shift, this.year,this.includeWeekends, btnGenerateShift);
		 shiftToolBar.setWidthFull();
		 shiftToolBar.setJustifyContentMode(JustifyContentMode.CENTER);
		 // Applies BASELINE alignment to all children
		 shiftToolBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
		 shiftToolBar.setMargin(false);
		 shiftToolBar.setPadding(false);	
	        
	     gridShiftDetail.setSizeFull();
	     editorLayout.setDialogTitle("Generate Shift Calendar | បង្កើតប្រតិទិនតាមវេន"); 
	     editorLayout.getHeader().addComponentAsFirst(btnGenerateShift);
	     editorLayout.getHeader().addComponentAsFirst(includeWeekends);
	     editorLayout.getHeader().addComponentAsFirst(year);
	     editorLayout.getHeader().addComponentAsFirst(shift);
	     
	     
		 editorLayout.add(gridShiftDetail);
	     //editorLayout.getFooter().add(buttonLayout); 
	     
	     shift.setItemLabelGenerator(Shift::getShiftName);	
	     
	     shift.setPlaceholder("e.g. Operation/Support");
	     this.year.setPlaceholder(String.valueOf(java.time.Year.now().getValue()));
	     this.year.setStep(1);
	     year.setClearButtonVisible(true);
	     year.setStepButtonsVisible(true);
	 }

	 private void save() {
		 if (entity == null) {
			 showError("No entity to save.");
		     return;
		 }
		 try {
			 binder.writeBean(entity);
			 entity = service.update(entity); // This gives you an ID			 
			 Notification.show("Data saved successfully | ទិន្នន័យបានរក្សាទុកដោយជោគជ័យ", 1000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			 clearForm();
		     closeForm();
		     refreshGrid();
		 } catch (ObjectOptimisticLockingFailureException ex) {
		        showError("Another user has modified this record.");
		 } catch (ValidationException ex) {
		        showError("Validation failed. Please check your input.");
		 } catch (DataAccessException ex) {
		        showError("Database error: " + ex.getMessage());
		 } catch (Exception ex) {
		        showError("Unexpected error: " + ex.getMessage());
		 }
	}

	 
	 @Override
	 protected void populateForm(ShiftCycle entity) throws Exception {
	     binder.readBean(entity); // Populate the form using the binder
	     editorLayout.open();    

	     boolean isEdit=(entity.getId()!=null ?true:false);
	     
	     if (!isEdit) {
	         // Correctly reference the IntegerField component
	         year.setMin(java.time.Year.now().getValue());
	         year.setMax(java.time.Year.now().getValue() + 1);
	         year.setValue(java.time.Year.now().getValue()); 

	     }
	     
	     // Build the data rows
	     List<ShiftDayRow> dayRows = ShiftDayRow.buildDayRows(entity.getShiftCycleDetails());
	     this.gridShiftDetail.setItems(dayRows);

	     // Update the footer with the calculated totals
	     updateGridDetailFooter(dayRows);
	     
	     shift.setReadOnly(isEdit);
	     year.setReadOnly(isEdit);
	     includeWeekends.setReadOnly(isEdit);
	     btnGenerateShift.setEnabled(!isEdit);
	     
	     
	     //updateEditorFooter(entity.getShiftCycleDetails());
	     
	 }

	 private void clearForm() {
		 binder.readBean(null);
		 this.entity = null;
	 }
	 private void closeForm() {
		 editorLayout.close();
	 }
	    
	    
	    @Override
	    protected Specification<ShiftCycle> buildCombinedSpecification() {
	        return (root, query, criteriaBuilder) -> {
	        	try {
		            List<Predicate> predicates = new ArrayList<>();
		            List<String> sqlFilter = new ArrayList<>();
		                     	            
		            Join<ShiftCycle, User> userCreatedJoin = root.join("userCreated", JoinType.LEFT);
		            Join<ShiftCycle, User> userUpdatedJoin = root.join("userUpdated", JoinType.LEFT);	 
		            

      
		            // Quick Search Filter
		            String quickSearchValue = txtQuick.getValue();
		            if (quickSearchValue != null && !quickSearchValue.isEmpty()) {
		                String likePattern = "%" + quickSearchValue.toLowerCase().trim() + "%";	          	               
	
		                predicates.add(criteriaBuilder.or(
		                	
		                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(root.get("id"), criteriaBuilder.literal(""))), likePattern),
		                    
		                    	                    	              
		                    criteriaBuilder.like(criteriaBuilder.lower(userCreatedJoin.get("name")), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower( DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(criteriaBuilder, root.get("createdAt"))), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(userUpdatedJoin.get("name")), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(criteriaBuilder, root.get("updatedAt"))), likePattern)
		                ));
		            }
	
		            // Advanced Filters
		            //ID Filter
		            if (advanceFilterID.getValue() != null) {
		                predicates.add(criteriaBuilder.equal(root.get("id"), advanceFilterID.getValue()));
		                sqlFilter.add("ID = " + advanceFilterID.getValue());
		            }
	
			        
		           
		            // Created By Filter
			        if (advanceFilterCreatedBy.getValue() != null && !advanceFilterCreatedBy.getValue().isEmpty()) {
			            Set<String> userNames = advanceFilterCreatedBy.getValue().stream()
			                .map(User::getName)
			                .collect(Collectors.toSet());
			            predicates.add(userCreatedJoin.get("name").in(userNames));
			            sqlFilter.add("CreatedBy IN (" + String.join(", ", userNames) + ")"); // Add to list
			           
			        }
			        // Created At Date Filter
			        if (advanceFilterCreatedDateFrom.getValue() != null && advanceFilterCreatedDateTo.getValue() != null) {
			        	Expression<LocalDate> truncatedCreatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("createdAt"));
			        	predicates.add(criteriaBuilder.between(truncatedCreatedAt, advanceFilterCreatedDateFrom.getValue(), advanceFilterCreatedDateTo.getValue()));
			            sqlFilter.add("CreatedAt BETWEEN " + advanceFilterCreatedDateFrom.getValue() + " AND " + advanceFilterCreatedDateTo.getValue());
			        }
			        
			        // Updated By Filter
			        if (advanceFilterUpdatedBy.getValue() != null && !advanceFilterUpdatedBy.getValue().isEmpty()) {		                
			                Set<String> userNamesUpdated = advanceFilterUpdatedBy.getValue().stream()
			                    .map(User::getName)
			                    .collect(Collectors.toSet());
			                predicates.add(userUpdatedJoin.get("name").in(userNamesUpdated));
			                sqlFilter.add("UpdatedBy IN (" + String.join(", ", userNamesUpdated) + ")"); 
			        }
			        
			        // Updated At Date Filter
			        if (advanceFilterUpdaedDateFrom.getValue() != null && advanceFilterUpdaedDateTo.getValue() != null) {
			            Expression<LocalDate> truncatedUpdatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("updatedAt"));
			            predicates.add(criteriaBuilder.between(truncatedUpdatedAt, advanceFilterUpdaedDateFrom.getValue(), advanceFilterUpdaedDateTo.getValue()));
			            sqlFilter.add("UpdatedAt BETWEEN " + advanceFilterUpdaedDateFrom.getValue() + " AND " + advanceFilterUpdaedDateTo.getValue());
			        }
			        	            		       
		            // Show/Hide Advanced Filters
		            this.showHideAdvanceFilter(sqlFilter.isEmpty() ? null : String.join(" AND ", sqlFilter));
	
		            // Combine predicates
		            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
	        	} catch (Exception ex) {
	                showError("Error in filter: " + ex.getMessage());
	                ex.printStackTrace();
	                return criteriaBuilder.conjunction(); // Return a "true" predicate fallback
	            }
	        };
	    }

	    
	    @Override
		protected void createAdvanceFilterLayout() {
	
	    	advanceFilterID.addThemeVariants(TextFieldVariant.LUMO_SMALL);
	 
	    	
	    	advanceFilterCreatedBy.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
	    	advanceFilterCreatedBy.setWidthFull();
	    	advanceFilterCreatedBy.setClearButtonVisible(true);
	    	advanceFilterCreatedBy.setItems(userService.getAllUser());
	    	advanceFilterCreatedBy.setItemLabelGenerator(User::getName);
	    	advanceFilterCreatedBy.setAutoExpand(AutoExpandMode.BOTH);
	    	
	    	
	    	advanceFilterCreatedDateFrom.addThemeVariants(DatePickerVariant.LUMO_SMALL);
	    	advanceFilterCreatedDateFrom.setClearButtonVisible(true);
	    	advanceFilterCreatedDateTo.addThemeVariants(DatePickerVariant.LUMO_SMALL);
	    	advanceFilterCreatedDateTo.setClearButtonVisible(true);
	    	advanceFilterCreatedDateFrom.addValueChangeListener(e -> advanceFilterCreatedDateTo.setMin(e.getValue()));
	    	advanceFilterCreatedDateTo.addValueChangeListener(e -> advanceFilterCreatedDateFrom.setMax(e.getValue()));
	    	
	    	advanceFilterUpdatedBy.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
	    	advanceFilterUpdatedBy.setWidthFull();
	    	advanceFilterUpdatedBy.setClearButtonVisible(true);
	    	advanceFilterUpdatedBy.setItems(userService.getAllUser());
	    	advanceFilterUpdatedBy.setItemLabelGenerator(User::getName);
	    	advanceFilterUpdatedBy.setAutoExpand(AutoExpandMode.BOTH);
			 
	    	
	    	advanceFilterUpdaedDateFrom.addThemeVariants(DatePickerVariant.LUMO_SMALL);
	    	advanceFilterUpdaedDateFrom.setClearButtonVisible(true);
	    	advanceFilterUpdaedDateTo.addThemeVariants(DatePickerVariant.LUMO_SMALL);
	    	advanceFilterUpdaedDateTo.setClearButtonVisible(true);
	    	advanceFilterUpdaedDateFrom.addValueChangeListener(e -> advanceFilterUpdaedDateTo.setMin(e.getValue()));
	    	advanceFilterUpdaedDateTo.addValueChangeListener(e -> advanceFilterUpdaedDateFrom.setMax(e.getValue()));
	    	
	    	
	    	 this.advanceSearchLayout.add(advanceFilterID,
	  
	    			 advanceFilterCreatedBy,
	    			 advanceFilterCreatedDateFrom,advanceFilterCreatedDateTo,
	    			 advanceFilterUpdatedBy,
	    			 advanceFilterUpdaedDateFrom,advanceFilterUpdaedDateTo);
	    	//return formLayout;
	    }
		


	    @Override
	    protected void focusFirstField() {
	        //this.fromDate.focus(); // Focus the "name" field
	    }
	    
	    
	    @Override
	    protected ShiftCycle createNewEntity() throws Exception {
	        ShiftCycle newCycle = new ShiftCycle();
	        newCycle.setYear(java.time.Year.now().getValue()); // ✅ default year
	        return newCycle;
	    }


	
	    @Override
	    public void beforeEnter(BeforeEnterEvent event) {
	    	if(!authenticatedUser.hasPage(CalendarView.class,AccessPageType.SELECTED_PAGE)) {
	    		 event.rerouteTo(AccessDeniedView.class);
	    	}

	    }
	    
	    @Override
	    protected HorizontalLayout exportToExcelFile() {
	        // Create a hidden anchor for the download
	        Anchor downloadLink = new Anchor();
	        downloadLink.getElement().setAttribute("download", true);
	        downloadLink.getElement().getStyle().set("display", "none");

	        // Create the export button
	        Button exportButton = new Button("Export to Excel", new Icon(VaadinIcon.DOWNLOAD));
	        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_WARNING);

	        exportButton.addClickListener(event -> {
	            // Create StreamResource dynamically upon button click
	            StreamResource resource = new StreamResource("Shift.xlsx", () -> {
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	                try (Workbook workbook = new XSSFWorkbook()) {
	                    Sheet sheet = workbook.createSheet("Shift");

	                    // Write headers dynamically from grid columns
	                    Row headerRow = sheet.createRow(0);
	                    List<Grid.Column<ShiftCycle>> columns = grid.getColumns();
	                    for (int i = 0; i < columns.size(); i++) {
	                        headerRow.createCell(i).setCellValue(columns.get(i).getHeaderText());
	                    }

	                    // Write data
	                    List<ShiftCycle> itemsToExport = grid.getSelectedItems().isEmpty() ?
	                            grid.getGenericDataView().getItems().toList() : new ArrayList<>(grid.getSelectedItems());
	                    AtomicInteger rowIndex = new AtomicInteger(1); // Use AtomicInteger to keep track of row index

	                    itemsToExport.forEach(row2bExport -> {
	                        Row dataRow = sheet.createRow(rowIndex.getAndIncrement());

	                        // Loop over columns dynamically
	                        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
	                            String columnKey = columns.get(columnIndex).getKey(); // Get column key
	                            switch (columnKey) {	                                             
                                	case "userCreated.name":
	                                    dataRow.createCell(columnIndex).setCellValue(row2bExport.getUserCreated()!= null ? row2bExport.getUserCreated().getName():"");
	                                    break;
                                	case "userUpdated.name":
	                                    dataRow.createCell(columnIndex).setCellValue(row2bExport.getUserUpdated()!= null ? row2bExport.getUserUpdated().getName():"");
	                                    break;	                                    
	                                // Add other specific cases as needed
	                                default:
	                                    try {
	                                        // Use reflection to check fields in Asset and its superclasses
	                                        Field field = getFieldFromClassHierarchy(ShiftCycle.class, columnKey);
	                                        if (field != null) {
	                                            field.setAccessible(true); // Make sure the field is accessible
	                                            
	                                            // Get the value of the field and write it to the Excel cell
	                                            Object value = field.get(row2bExport); // Retrieve value from the Asset entity
	                                            if (value instanceof java.util.Date) {
	                                                dataRow.createCell(columnIndex).setCellValue(((java.util.Date) value).toString());
	                                            } else if (value instanceof java.time.LocalDate) {
	                                                dataRow.createCell(columnIndex).setCellValue(DateTimeUtilFormart.DATE_FORMATTER.format((java.time.LocalDate) value));
	                                            } else if (value instanceof java.time.LocalDateTime) {
	                                                dataRow.createCell(columnIndex).setCellValue(((java.time.LocalDateTime) value).toString());
	                                            } else if (value instanceof java.time.ZonedDateTime) {
	                                                dataRow.createCell(columnIndex).setCellValue(DateTimeUtilFormart.DATE_TIME_FORMATTER.format((java.time.ZonedDateTime) value));
	                                            } else if(value instanceof Number) {
	                                                dataRow.createCell(columnIndex).setCellValue(((Number) value).doubleValue());	                                                
	                                        	}else {
	                                                dataRow.createCell(columnIndex).setCellValue(value != null ? value.toString() : "");
	                                            }

	                                        } else {
	                                            dataRow.createCell(columnIndex).setCellValue(""); // Field not found
	                                        }
	                                    } catch (IllegalAccessException e) {
	                                        dataRow.createCell(columnIndex).setCellValue("Error"); // Handle error
	                                    }
	                                    break;
	                            }
	                        }
	                    });

	                    workbook.write(outputStream);
	                } catch (IOException e) {
	                    throw new UncheckedIOException(e);
	                }
	                return new ByteArrayInputStream(outputStream.toByteArray());
	            });

	            downloadLink.setHref(resource);
	            downloadLink.getElement().callJsFunction("click");
	        });

	        return new HorizontalLayout(exportButton, downloadLink);
	}
	    
	    private void generateShift() {
	        try {
	            if (shift.getValue() == null) {
	                showError("Please select a Shift.");
	                return;
	            }
	            if (year.getValue() == null) {
	                showError("Please enter a Year.");
	                return;
	            }

	            int shiftId = shift.getValue().getId().intValue();
	            int yearNo  = year.getValue();
	            
	            // 🔴 NEW: Do not allow generate if shiftId + yearNo already exist
	            if (service.findCycle(shiftId, yearNo).isPresent()) {
	                showError("A shift calendar for this Shift and Year already exists. Please open and edit the existing calendar instead of generating a new one. | ប្រតិទិនវេនសម្រាប់វេននេះ និងឆ្នាំនេះមានរួចហើយ។ សូមបើក និងកែសម្រួលប្រតិទិនដែលមានស្រាប់ ជំនួសឱ្យបង្កើតថ្មីម្តងទៀត។");
	                return;
	            }
	            

	            int inserted = service.generateShiftCycleRaw(shiftId, yearNo,this.includeWeekends.getValue());
	            showSuccess("Generated " + inserted + " day(s).");
	            
	         // 1) Load the parent cycle (so the dialog/binder can reflect the current entity)
	            service.findCycle(shiftId, yearNo).ifPresentOrElse(cycle -> {
	                this.entity = cycle;           // keep current context entity
	                binder.readBean(cycle);        // refresh header fields if any
	                this.gridShiftDetail.setItems(ShiftDayRow.buildDayRows(entity.getShiftCycleDetails()));
	            }, () -> {
	                // Shouldn’t happen if insert/upsert succeeded, but handle gracefully
	                gridShiftDetail.setItems(Collections.emptyList());
	                showError("Cycle not found after generation.");
	            });

	            // Refresh the main grid (list of cycles)
	            refreshGrid();
	            
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	            showError("Failed to generate: " + e.getMessage());
	        }
	    }


}
