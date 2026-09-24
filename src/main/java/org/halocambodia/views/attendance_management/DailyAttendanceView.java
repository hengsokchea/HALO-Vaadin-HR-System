package org.halocambodia.views.attendance_management;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.timepicker.TimePickerVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Null;
//import kotlin.reflect.jvm.internal.impl.types.Variance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.component.SignaturePad;
import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeAttendanceDetailService;
import org.halocambodia.services.EmployeeAttendanceService;
import org.halocambodia.services.EmployeeLeaveRequestService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.PreviewReport;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.halocambodia.views.employee_allocate.EmployeeAllocationCloneView;
import org.halocambodia.views.employee_allocate.EmployeeAllocationImportView;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.halocambodia.data.enums.*;
import org.halocambodia.enums.AttendanceEntryStatus;
import org.halocambodia.enums.EmployeeTypeEnum;

@Route(value = "attendance", layout = MainLayout.class)
@PageTitle("Daily Attendance | វត្តមានប្រចាំថ្ងៃ")
@PermitAll
@Uses(Icon.class)
public class DailyAttendanceView extends PageDialogLayout<EmployeeAttendanceDetail, EmployeeAttendanceDetailService> {

    private static final int EMPLOYEE_LOOKUP_PAGE_SIZE = 50;
    private static final List<EmployeeTypeEnum> ACTIVE_STAFF_TYPES =
            List.of(EmployeeTypeEnum.National, EmployeeTypeEnum.International);
    private static final List<EmployeeTypeEnum> ACTIVE_LOCAL_STAFF_TYPES =
            List.of(EmployeeTypeEnum.National);

    // Form Fields for Attendance
	private ComboBox<Employee> reportedByEmployee = new ComboBox<>("Reported By");
	private ComboBox<Positions> reportedByPosition = new ComboBox<>("Reported Position");
    private DatePicker reportedDate = new DatePicker("Reported Date");
    private ComboBox<Branch> reportLocation = new ComboBox<>("Reported Location");    
    
    private ComboBox<Employee> employee =    new ComboBox<>("Attendance Name | ឈ្មោះវត្តមាន");
    private DatePicker attendanceDate =new DatePicker("Attendance Date | កាលបរិច្ឆេទវត្តមាន");	 
	private TimePicker firstIn =new TimePicker("First In | ចូលដំបូង");
	private TimePicker lastOut =new TimePicker("Last Out | ចេញចុងក្រោយ");	
	private IntegerField breakMinutes =new IntegerField("Break(minuts) | រយះពេលសម្រាក(នាទី)");	
	private BigDecimalField totalHours =new BigDecimalField("Total Hours | ម៉ោងសរុប");
	private BigDecimalField normalHours = new BigDecimalField("Normal Hours | ម៉ោងធម្មតា");
	private BigDecimalField overtimeHours =new BigDecimalField("Overtime Hours | ម៉ោងលើសម៉ោងធម្មតា");
	private ComboBox<LeaveType> leaveType =new ComboBox<>("Duty/Leave");	
	
	private ComboBox<LeaveTypeGroup> leaveTypeGroup =new ComboBox<>("Status");
	
	
	private ComboBox<LeaveTypeSubType> leaveTypeSubType =new ComboBox<>("Leave Sub Type | ប្រភេទច្បាប់សម្រាករង");	
	private ComboBox<LeaveDuration> leaveDuration = new ComboBox<>("Duration | រយៈពេល");	 
	private TextArea remark = new TextArea("Remark | កំណត់សម្គាល់");
  
	private ComboBox<Employee> qcByEmployee = new ComboBox<>("Supervisor");
	private ComboBox<Positions> qcByPosition = new ComboBox<>("Supervisor Position");
    private DatePicker qcDate = new DatePicker("Supervisor Date");
    private ComboBox<Branch> qcLocation = new ComboBox<>("Supervisor Location");

	private ComboBox<Employee> dataEntryBy = new ComboBox<>("Data Entry By | អ្នកបញ្ចូលទិន្នន័យ");
	private ComboBox<Positions> dataEntryPosition = new ComboBox<>("Data Entry Position | មុខតំណែងអ្នកបញ្ចូល");
	private ComboBox<Branch> dataEntryLocation = new ComboBox<>("Data Entry Location | ទីតាំងបញ្ចូលទិន្នន័យ");
	private DatePicker dataEntryDate = new DatePicker("Data Entry Date | កាលបរិច្ឆេទបញ្ចូល");
	private ComboBox<AttendanceEntryStatus> entryStatus = new ComboBox<>("Entry Status | ស្ថានភាពបញ្ចូល");
    

	 
	//Form Fields for Attendance detail Team & task
	 CustomDialog attendanceDetailTeamDialog = new CustomDialog("Team & Minefiled Assign");
	 private ComboBox<Teams> teamTeam =new ComboBox<Teams>("Team | ក្រុម");
	 private MultiSelectComboBox<Tasks> teamTask =new MultiSelectComboBox<Tasks>("Minefield | ចម្ការមីន");	
	 private Grid<EmployeeAttendanceDetailTeam> gridAttendanceDetailTeam = new Grid<>(EmployeeAttendanceDetailTeam.class,false);
	 private final BeanValidationBinder<EmployeeAttendanceDetailTeam> detailTeamBinder =new BeanValidationBinder<>(EmployeeAttendanceDetailTeam.class);
	 private EmployeeAttendanceDetailTeam currentTeam;

    // Advance Filter Components
    private NumberField advanceFilterID = new NumberField("ID");
    
    private MultiSelectComboBox<Employee> advanceFilterEmployee=new MultiSelectComboBox<Employee>(this.employee.getLabel());
    private DateRangePicker advanceFilterAttendanceDate = new DateRangePicker();
    private MultiSelectComboBox<Teams> advanceFilterTeams=new MultiSelectComboBox<Teams>(this.teamTeam.getLabel());
    private MultiSelectComboBox<Tasks> advanceFilterTasks=new MultiSelectComboBox<Tasks>(this.teamTask.getLabel());
    private MultiSelectComboBox<LeaveType> advanceFilterLeaveType=new MultiSelectComboBox<LeaveType>(this.leaveType.getLabel());
    private MultiSelectComboBox<LeaveTypeSubType> advanceFilterLeaveTypeSubType=new MultiSelectComboBox<LeaveTypeSubType>(this.leaveTypeSubType.getLabel());
    private MultiSelectComboBox<LeaveDuration> advanceFilterLeaveDuration=new MultiSelectComboBox<LeaveDuration>(this.leaveDuration.getLabel());
    private TimePicker advanceFilterFirstInFrom =new TimePicker(this.firstIn.getLabel() +" From");
    private TimePicker advanceFilterFirstInTo =new TimePicker(this.firstIn.getLabel() +" To");
    
    private TimePicker advanceFilterlastOutFrom =new TimePicker(this.lastOut.getLabel() +" From");
    private TimePicker advanceFilterlastOutTo =new TimePicker(this.lastOut.getLabel() +" To");
    private IntegerField advanceFilterBreakMinutes =new IntegerField(this.breakMinutes.getLabel());	
    private BigDecimalField advanceFilterTotalHours =new BigDecimalField(this.totalHours.getLabel());
	private BigDecimalField advanceFilterNormalHours = new BigDecimalField(this.normalHours.getLabel());
	private BigDecimalField advanceFilterOvertimeHours =new BigDecimalField(this.overtimeHours.getLabel());
	private MultiSelectComboBox<Employee> advanceFilterReportedBy=new MultiSelectComboBox<Employee>(this.reportedByEmployee.getLabel());
	private MultiSelectComboBox<Positions> advanceFilterReportedByPosition = new MultiSelectComboBox<Positions>(this.reportedByPosition.getLabel());
    private DateRangePicker advanceFilterReportedDate = new DateRangePicker();
    private MultiSelectComboBox<Branch> advanceFilterReportLocation = new MultiSelectComboBox<>(this.reportLocation.getLabel());
    
	private MultiSelectComboBox<Employee> advanceFilterQcByEmployee=new MultiSelectComboBox<Employee>(this.qcByEmployee.getLabel());
	private MultiSelectComboBox<Positions> advanceFilterQcByPosition = new MultiSelectComboBox<Positions>(this.qcByPosition.getLabel());
    private DateRangePicker advanceFilterQcDate = new DateRangePicker();
    private MultiSelectComboBox<Branch> advanceFilterQcLocation = new MultiSelectComboBox<>(this.qcLocation.getLabel());
    private TextField advanceFilterRemark = new TextField(this.remark.getLabel());

    private MultiSelectComboBox<Employee> advanceFilterDataEntryBy =
            new MultiSelectComboBox<>(this.dataEntryBy.getLabel());
    private MultiSelectComboBox<Positions> advanceFilterDataEntryPosition =
            new MultiSelectComboBox<>(this.dataEntryPosition.getLabel());
    private MultiSelectComboBox<Branch> advanceFilterDataEntryLocation =
            new MultiSelectComboBox<>(this.dataEntryLocation.getLabel());
    private DateRangePicker advanceFilterDataEntryDate = new DateRangePicker();
    private MultiSelectComboBox<AttendanceEntryStatus> advanceFilterEntryStatus =
            new MultiSelectComboBox<>(this.entryStatus.getLabel());
    

    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    private DateRangePicker advanceFilterCreatedDate = new DateRangePicker();
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    private DateRangePicker advanceFilterUpdatedDate = new DateRangePicker();
    
    

    private final Optional<User> currentUserLogin;
    private String quickSearch = "";

    // Repositories
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final BranchRepository branchRepository;
    private final TeamsRepository teamsRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeSubTypeRepository leaveTypeSubTypeRepository;
    private final EmployeeRosterRepository employeeRosterRepository;
    private final TasksRepository tasksRepository;
    private final EmployeeAllocationRepository employeeAllocationRepository;
    private final LeaveTypeGroupRepository leaveTypeGroupRepository;
    private boolean editorLookupDataLoaded;
    
    

    // In-app clipboard (copy object per row)
    private CopiedAttendance clipboard;   // null = nothing copied
    private Long clipboardSourceId;
    private enum PasteMode { TEAM, MINEFIELD, TEAM_AND_MINEFIELD }
    private EmployeeAttendanceDetail rangeAnchor;
    
    private static final class CopiedAttendance {
        // map Team -> Tasks (this supports Team only / Minefield only / both)
        private final Map<Long, CopiedTeam> teamsById = new java.util.LinkedHashMap<>();

        static CopiedAttendance from(EmployeeAttendanceDetail src) {
            CopiedAttendance c = new CopiedAttendance();

            if (src == null || src.getAttendanceDetailTeams() == null) return c;

            for (EmployeeAttendanceDetailTeam tr : src.getAttendanceDetailTeams()) {
                if (tr == null || tr.getTeam() == null || tr.getTeam().getId() == null) continue;

                Long teamId = tr.getTeam().getId();
                CopiedTeam ct = c.teamsById.computeIfAbsent(teamId, id -> new CopiedTeam(tr.getTeam()));

                if (tr.getTeamTasks() != null) {
                    for (EmployeeAttendanceDetailTeamTask j : tr.getTeamTasks()) {
                        if (j != null && j.getTask() != null) {
                            ct.tasks.add(j.getTask());
                        }
                    }
                }
            }
            return c;
        }

        boolean isEmpty() {
            return teamsById.isEmpty();
        }

        private static final class CopiedTeam {
            final Teams team;
            final Set<Tasks> tasks = new java.util.LinkedHashSet<>();
            CopiedTeam(Teams team) { this.team = team; }
        }
        
    }
    
       

    public DailyAttendanceView(EmployeeAttendanceDetailService service, UserService userService, AuthenticatedUser authenticatedUser,EmployeeRepository employeeRepository,PositionRepository positionRepository,BranchRepository branchRepository,TeamsRepository teamsRepository,LeaveTypeRepository leaveTypeRepository,LeaveTypeSubTypeRepository leaveTypeSubTypeRepository,EmployeeRosterRepository employeeRosterRepository,TasksRepository tasksRepository,EmployeeAllocationRepository employeeAllocationRepository,LeaveTypeGroupRepository leaveTypeGroupRepository) {
        super(EmployeeAttendanceDetail.class, service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
        this.employeeRepository=employeeRepository;
        this.positionRepository=positionRepository;
        this.branchRepository=branchRepository;
        this.teamsRepository=teamsRepository;
        this.leaveTypeRepository=leaveTypeRepository;
        this.leaveTypeSubTypeRepository=leaveTypeSubTypeRepository;
        this.employeeRosterRepository=employeeRosterRepository;
        this.tasksRepository=tasksRepository;
        this.employeeAllocationRepository=employeeAllocationRepository;
        this.leaveTypeGroupRepository=leaveTypeGroupRepository;
        
    }

	@Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        setDefaultAttendanceDateRange();
        configureGrid();
        configureEditorLayout();
        binderField();
        configureEditorFooter();
        leftMenu.addComponentItem(
                buildSubmitSelectedButton(),
                "Submit Selected",
                "Submit Selected | បញ្ជូនជួរដែលបានជ្រើស"
        );
    }

    private void setDefaultAttendanceDateRange() {
        LocalDate today = LocalDate.now();
        advanceFilterAttendanceDate.getFromPicker().setValue(today);
        advanceFilterAttendanceDate.getToPicker().setValue(today);
    }

    private Button buildSubmitSelectedButton() {
        Button button = new Button(
                "Submit Selected | បញ្ជូនជួរដែលបានជ្រើស",
                VaadinIcon.PAPERPLANE.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        button.setEnabled(authenticatedUser.hasPage(
                DailyAttendanceView.class, AccessPageType.UPDATED_PAGE));

        button.addClickListener(event -> {
            Set<EmployeeAttendanceDetail> selected = grid.getSelectedItems();
            if (selected.isEmpty()) {
                Notification.show(
                        "Please select at least one attendance row. "
                        + "| សូមជ្រើសរើសជួរវត្តមានយ៉ាងហោចណាស់មួយ។",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }

            List<Long> ids = selected.stream()
                    .filter(row -> row.getEntryStatus() != AttendanceEntryStatus.SUBMITTED)
                    .map(EmployeeAttendanceDetail::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (ids.isEmpty()) {
                Notification.show(
                        "All selected rows are already submitted. "
                        + "| ជួរដែលបានជ្រើសទាំងអស់បានបញ្ជូនរួចហើយ។",
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                return;
            }

            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Confirm Attendance Submission | បញ្ជាក់ការបញ្ជូនវត្តមាន");
            dialog.setText("Submit " + ids.size() + " selected attendance record(s) to QC?\n\n"
                    + "បញ្ជូនកំណត់ត្រាវត្តមានដែលបានជ្រើស " + ids.size() + " ទៅ QC?");
            dialog.setConfirmText("Submit | បញ្ជូន");
            dialog.setConfirmButtonTheme("primary success");
            dialog.setCancelText("Cancel | បោះបង់");
            dialog.setCancelable(true);
            dialog.addConfirmListener(confirm -> {
                try {
                    int submitted = service.submitSelected(ids);
                    refreshGrid();
                    grid.deselectAll();
                    Notification.show(
                            submitted + " attendance record(s) submitted to QC. "
                            + "| បានបញ្ជូនកំណត់ត្រាវត្តមាន " + submitted + " ទៅ QC។",
                            3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            dialog.open();
        });
        return button;
    }

    private void loadDataToForm() {
		configureEmployeeComboBox(this.reportedByEmployee, ACTIVE_STAFF_TYPES);
		this.reportedByPosition.setItems(this.positionRepository.findByObsolateDateIsNull());
		this.reportLocation.setItems(this.branchRepository.findByIsActiveTrue());    	  	
		
		configureEmployeeComboBox(this.employee, ACTIVE_STAFF_TYPES);
    	
    	// Leave Type & Group - IMPORTANT ORDER
        List<LeaveType> allLeaveTypes = this.leaveTypeRepository.findAll();
        this.leaveType.setItems(allLeaveTypes);
        
        // Load LeaveTypeGroup (derived from LeaveType)
        List<LeaveTypeGroup> allGroups = allLeaveTypes.stream()
                .map(LeaveType::getLeaveTypeGroup)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        this.leaveTypeGroup.setItems(allGroups);
        this.leaveTypeGroup.setReadOnly(true);                    // Usually not editable
        this.leaveTypeGroup.setItemLabelGenerator(LeaveTypeGroup::getLeaveTypeGroup);
        
    	

    	this.leaveTypeSubType.setItems(this.leaveTypeSubTypeRepository.findAll());  
    	
    	
    	this.leaveDuration.setItems(LeaveDuration.values());
		configureEmployeeComboBox(this.qcByEmployee, ACTIVE_STAFF_TYPES);
    	this.qcByPosition.setItems(this.positionRepository.findByObsolateDateIsNull());
    	this.qcLocation.setItems(this.branchRepository.findByIsActiveTrue());
		configureEmployeeComboBox(this.dataEntryBy, ACTIVE_STAFF_TYPES);
		this.dataEntryPosition.setItems(this.positionRepository.findByObsolateDateIsNull());
		this.dataEntryLocation.setItems(this.branchRepository.findByIsActiveTrue());
		this.entryStatus.setItems(AttendanceEntryStatus.values());
		this.entryStatus.setItemLabelGenerator(AttendanceEntryStatus::getLabel);
		this.dataEntryBy.setItemLabelGenerator(this::formatAttendanceEmployeeLabel);
		this.dataEntryPosition.setItemLabelGenerator(this::formatPosition);
		this.dataEntryLocation.setItemLabelGenerator(Branch::getBranchShortName);
		this.dataEntryBy.setReadOnly(false);
		this.dataEntryPosition.setReadOnly(false);
		this.dataEntryLocation.setReadOnly(false);
		this.dataEntryDate.setReadOnly(false);
		this.entryStatus.setReadOnly(false);
		this.dataEntryBy.addValueChangeListener(event -> {
		    Employee selected = event.getValue();
		    if (selected != null) {
		        this.dataEntryPosition.setValue(selected.getPositions());
		        this.dataEntryLocation.setValue(selected.getBranch());
		    }
		});
    	
        this.teamTeam.setItems(this.teamsRepository.findAll());
        this.teamTask.setItems(this.tasksRepository.findAll());
        
        //this.leaveTypeGroup.setItems(leaveTypeGroupRepository.findAll()); // or whatever your repo is
       // this.leaveTypeGroup.setItemLabelGenerator(LeaveTypeGroup::getLeaveTypeGroup); // adjust as needed
       // this.leaveTypeGroup.setReadOnly(true);
  
    }

    /**
     * The grid does not need the editor's complete lookup lists. Loading them only
     * when Add/Edit is opened keeps the initial attendance page lightweight.
     */
    private void ensureEditorLookupDataLoaded() {
        if (editorLookupDataLoaded) {
            return;
        }
        loadDataToForm();
        editorLookupDataLoaded = true;
    }

    @Override
    protected Sort getDefaultSort() {
        return Sort.by(Sort.Direction.DESC, "attendanceDate")
                .and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private CallbackDataProvider<Employee, String> createEmployeeDataProvider(
            List<EmployeeTypeEnum> employeeTypes) {
        return new CallbackDataProvider<>(
                query -> {
                    String filter = query.getFilter().orElse("").trim();
                    int limit = Math.max(1, query.getLimit());
                    int page = query.getOffset() / limit;
                    return employeeRepository.searchActiveEmployees(
                            employeeTypes,
                            "Active",
                            filter,
                            PageRequest.of(page, limit)
                    ).stream();
                },
                query -> {
                    String filter = query.getFilter().orElse("").trim();
                    long total = employeeRepository.countActiveEmployees(
                            employeeTypes,
                            "Active",
                            filter
                    );
                    return (int) Math.min(Integer.MAX_VALUE, total);
                }
        );
    }

    private void configureEmployeeComboBox(
            ComboBox<Employee> comboBox,
            List<EmployeeTypeEnum> employeeTypes) {
        comboBox.setPageSize(EMPLOYEE_LOOKUP_PAGE_SIZE);
        comboBox.getElement().setProperty("filterDebounceTimeout", 350);
        comboBox.setDataProvider(
                createEmployeeDataProvider(employeeTypes),
                filter -> filter == null ? "" : filter.trim()
        );
    }

    private void configureEmployeeMultiSelect(
            MultiSelectComboBox<Employee> comboBox,
            List<EmployeeTypeEnum> employeeTypes) {
        comboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        comboBox.setWidthFull();
        comboBox.setClearButtonVisible(true);
        comboBox.setAutoExpand(AutoExpandMode.BOTH);
        comboBox.setPageSize(EMPLOYEE_LOOKUP_PAGE_SIZE);
        comboBox.setItemLabelGenerator(this::formatAttendanceEmployeeLabel);
        comboBox.getElement().setProperty("filterDebounceTimeout", 350);
        comboBox.setDataProvider(
                createEmployeeDataProvider(employeeTypes),
                filter -> filter == null ? "" : filter.trim()
        );
    }

    @Override
    protected void binderField() {
        binder.bindInstanceFields(this);

     
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        
        grid.getColumns().size();
        
        // freeze main columns
        freezeMainColumns();

        // freeze selection checkbox column (if MULTI)
        if (grid.getSelectionModel() instanceof GridMultiSelectionModel<?> sm) {
            @SuppressWarnings("unchecked")
            GridMultiSelectionModel<EmployeeAttendanceDetail> sel = (GridMultiSelectionModel<EmployeeAttendanceDetail>) sm;
            sel.setSelectionColumnFrozen(true);
        }        
        grid.setItemDetailsRenderer(this.createTabRenderer());
        configureAttendanceColumnRenderers();
        configureAttendanceColumnWidths();
        
        grid.setAllRowsVisible(false);
        
     GridContextMenu<EmployeeAttendanceDetail> menu = grid.addContextMenu();

     // ===== Copy group =====
     GridMenuItem<EmployeeAttendanceDetail> menuCopy = menu.addItem("Copy | ចម្លង", e -> {
    	    EmployeeAttendanceDetail src = e.getItem().orElseGet(() -> grid.getSelectedItems().stream().findFirst().orElse(null));
    	    if (src == null) {
    	        Notification.show("No row selected | មិនមានជួរត្រូវបានជ្រើស", 2000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_CONTRAST);
    	        return;
    	    }

    	    clipboard = CopiedAttendance.from(src);
    	    clipboardSourceId = src.getId();

    	    Notification.show("Copied | បានចម្លង ✅", 1500, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    	});
    	menuCopy.addComponentAsFirst(createIcon(VaadinIcon.COPY));

    	// ===== Paste =====
    	GridMenuItem<EmployeeAttendanceDetail> menuPaste = menu.addItem("Paste | បិទភ្ជាប់");
    	menuPaste.addComponentAsFirst(createIcon(VaadinIcon.PASTE));
    	GridSubMenu<EmployeeAttendanceDetail> subMenuPaste = menuPaste.getSubMenu();

    	subMenuPaste.addItem("Team | ក្រុម", e -> pasteToTarget(e, PasteMode.TEAM)).addComponentAsFirst(createIcon(VaadinIcon.GROUP));
    	subMenuPaste.addItem("Minefield | ចម្ការមីន", e -> pasteToTarget(e, PasteMode.MINEFIELD)).addComponentAsFirst(createIcon(VaadinIcon.MAP_MARKER));
    	subMenuPaste.addItem("Team & Minefield | ក្រុម & ចម្ការមីន", e -> pasteToTarget(e, PasteMode.TEAM_AND_MINEFIELD)).addComponentAsFirst(createIcon(VaadinIcon.CONNECT));

    	enableRowRangeSelection(grid);

    	 GridMultiSelectionModel<EmployeeAttendanceDetail> sm =
    	         (GridMultiSelectionModel<EmployeeAttendanceDetail>) grid.getSelectionModel();

    	 sm.setSelectionColumnFrozen(true);
    	 sm.setDragSelect(true);
    }

    @Override
    protected void afterDataProviderConfigured() {
        installLazyItemIndexProvider();
    }

    @Override
    protected String getSortProperty(String columnKey) {
        return switch (columnKey) {
            case "team", "tasks", "reviewStatus" -> null;
            case "reportLocation" -> "reportLocation.branchShortName";
            case "leaveType" -> "leaveType.leaveNameEn";
            case "leaveTypeSubType" -> "leaveTypeSubType.leaveSubTypeNameEn";
            case "reportedByEmployee" -> "reportedByEmployee.nameEn";
            case "reportedByPosition" -> "reportedByPosition.position";
            case "qcByEmployee" -> "qcByEmployee.nameEn";
            case "qcByPosition" -> "qcByPosition.position";
            case "qcLocation" -> "qcLocation.branchShortName";
            default -> columnKey;
        };
    }

    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of(
                "id",
                "leaveType.leaveTypeGroup",
                "leaveTypeSubType",
                "breakMinutes",
                "normalHours",
                "reportedByEmployee",
                "reportedByPosition",
                "reportedDate",
                "qcByEmployee",
                "qcByPosition",
                "qcDate",
                "qcLocation",
                "remark",
                "survey123Id"
        );
    }

    private void configureAttendanceColumnRenderers() {
        Optional.ofNullable(grid.getColumnByKey("leaveType"))
                .ifPresent(column -> column.setRenderer(
                        new ComponentRenderer<>(this::createAttendanceStatusBadge)));
        Optional.ofNullable(grid.getColumnByKey("reviewStatus"))
                .ifPresent(column -> column.setRenderer(
                        new ComponentRenderer<>(this::createReviewStatusBadge)));
    }

    private void configureAttendanceColumnWidths() {
        grid.getColumns().stream()
                .filter(column -> column.getKey() != null)
                .filter(column -> !"toggleDetails".equals(column.getKey()))
                .forEach(column -> {
                    column.setAutoWidth(false);
                    column.setFlexGrow(0);
                    column.setWidth("140px");
                });

        setColumnWidth("id", "80px");
        setColumnWidth("employee.insuranceNo", "105px");
        setColumnWidth("employee.nameEn", "190px");
        setColumnWidth("employee.nameKh", "190px");
        setColumnWidth("attendanceDate", "130px");
        setColumnWidth("reportLocation", "125px");
        setColumnWidth("team", "125px");
        setColumnWidth("tasks", "180px");
        setColumnWidth("leaveType.leaveTypeGroup", "150px");
        setColumnWidth("leaveType", "190px");
        setColumnWidth("leaveTypeSubType", "190px");
        setColumnWidth("leaveDuration", "145px");
        setColumnWidth("firstIn", "105px");
        setColumnWidth("lastOut", "105px");
        setColumnWidth("breakMinutes", "110px");
        setColumnWidth("totalHours", "115px");
        setColumnWidth("normalHours", "120px");
        setColumnWidth("overtimeHours", "120px");
        setColumnWidth("reviewStatus", "155px");
        setColumnWidth("reportedByEmployee", "210px");
        setColumnWidth("qcByEmployee", "210px");
        setColumnWidth("remark", "260px");
        setColumnWidth("survey123Id", "260px");
    }

    private void setColumnWidth(String key, String width) {
        Optional.ofNullable(grid.getColumnByKey(key)).ifPresent(column -> column.setWidth(width));
    }

    private Component createAttendanceStatusBadge(EmployeeAttendanceDetail attendance) {
        LeaveType type = attendance == null ? null : attendance.getLeaveType();
        String code = type == null || type.getLeavTypeCode() == null
                ? ""
                : type.getLeavTypeCode().trim().toUpperCase();
        String name = type == null || type.getLeaveNameEn() == null
                ? ""
                : type.getLeaveNameEn().trim();
        String text = code.isBlank() ? name : code + (name.isBlank() ? "" : " · " + name);
        if (text.isBlank()) {
            text = "Not set | មិនបានកំណត់";
        }

        Span badge = createBadge(text);
        if ("PR".equals(code) || "P".equals(code)) {
            applyBadgeColors(badge, "#E6F4EA", "#137333", "#A8DAB5");
        } else if (Set.of("A", "UL").contains(code)) {
            applyBadgeColors(badge, "#FCE8E6", "#B3261E", "#F3B8B5");
        } else if (Set.of("AL", "SL", "S", "ML", "PL", "PI").contains(code)) {
            applyBadgeColors(badge, "#E8F0FE", "#174EA6", "#AECBFA");
        } else if ("H".equals(code)) {
            applyBadgeColors(badge, "#F1F3F4", "#3C4043", "#DADCE0");
        } else {
            applyBadgeColors(badge, "#E0F2F1", "#00695C", "#80CBC4");
        }
        badge.getElement().setProperty("title", text);
        return badge;
    }

    private Component createReviewStatusBadge(EmployeeAttendanceDetail attendance) {
        boolean reviewed = attendance != null && attendance.getQcByEmployee() != null;
        Span badge = createBadge(reviewed
                ? "Reviewed | បានត្រួតពិនិត្យ"
                : "Pending | រង់ចាំត្រួតពិនិត្យ");
        if (reviewed) {
            applyBadgeColors(badge, "#E6F4EA", "#137333", "#A8DAB5");
        } else {
            applyBadgeColors(badge, "#FEF7E0", "#B06000", "#F7CB73");
        }
        return badge;
    }

    private Span createBadge(String text) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("display", "inline-block")
                .set("max-width", "100%")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap")
                .set("border-radius", "999px")
                .set("border", "1px solid")
                .set("padding", "0.12rem 0.55rem")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "600");
        return badge;
    }

    private void applyBadgeColors(Span badge, String background, String color, String border) {
        badge.getStyle()
                .set("background", background)
                .set("color", color)
                .set("border-color", border);
    }

    private void freezeMainColumns() {
        Optional.ofNullable(grid.getColumnByKey("id")).ifPresent(c -> {
            c.setFrozen(true);
            c.setFlexGrow(0);
        });
        Optional.ofNullable(grid.getColumnByKey("employee.insuranceNo")).ifPresent(c -> {
            c.setFrozen(true);
            c.setFlexGrow(0);
        });
        Optional.ofNullable(grid.getColumnByKey("employee.nameEn")).ifPresent(c -> {
            c.setFrozen(true);
            c.setFlexGrow(0);
        });
        Optional.ofNullable(grid.getColumnByKey("employee.nameKh")).ifPresent(c -> {
            c.setFrozen(true);
            c.setFlexGrow(0);
        });
        Optional.ofNullable(grid.getColumnByKey("attendanceDate")).ifPresent(c -> {
            c.setFrozen(true);
            c.setFlexGrow(0);
        });
    }
    
    private void enableRowRangeSelection(Grid<EmployeeAttendanceDetail> grid) {
        GridMultiSelectionModel<EmployeeAttendanceDetail> sm =
                (GridMultiSelectionModel<EmployeeAttendanceDetail>) grid.getSelectionModel();

        sm.setSelectionColumnFrozen(true);

        sm.addClientItemToggleListener(event -> {
            EmployeeAttendanceDetail clicked = event.getItem();
            if (clicked == null) return;

            // first click sets anchor
            if (rangeAnchor == null) {
                rangeAnchor = clicked;
                return;
            }

            if (!event.isShiftKey()) {
                rangeAnchor = clicked;
                return;
            }

            var lazy = grid.getLazyDataView();

            int a = lazy.getItemIndex(rangeAnchor).orElse(-1);
            int b = lazy.getItemIndex(clicked).orElse(-1);
            if (a < 0 || b < 0) {
                rangeAnchor = clicked;
                return;
            }

            int from = Math.min(a, b);
            int to   = Math.max(a, b);

            int maxRange = 500;
            if ((to - from) > maxRange) {
                Notification.show("Range too large (" + (to - from + 1) + ")",
                        2500, Notification.Position.TOP_CENTER);
                return;
            }

            boolean select = event.isSelected(); // clicked row new state

            for (int i = from; i <= to; i++) {
            	Optional.ofNullable(lazy.getItem(i)).ifPresent(item -> {
            	    if (select) sm.select(item);
            	    else sm.deselect(item);
            	});

            }

            rangeAnchor = clicked;
        });
    }
    
    private void installLazyItemIndexProvider() {
        GridLazyDataView<EmployeeAttendanceDetail> lazy = grid.getLazyDataView();

        lazy.setItemIndexProvider((item, query) -> findIndexById(item, query));
    }

    // generic helper that scans in chunks using the same sort/filter query
    private <F> int findIndexById(EmployeeAttendanceDetail item, Query<EmployeeAttendanceDetail, F> baseQuery) {
        if (item == null || item.getId() == null) return -1;

        final Long targetId = item.getId();
        final int pageSize = 200;
        final int hardLimit = 20000;

        @SuppressWarnings("unchecked")
        DataProvider<EmployeeAttendanceDetail, F> dp =       (DataProvider<EmployeeAttendanceDetail, F>) grid.getDataProvider();

        int offset = 0;
        while (offset <= hardLimit) {

            Query<EmployeeAttendanceDetail, F> q = new Query<>(
                    offset,
                    pageSize,
                    baseQuery.getSortOrders(),
                    baseQuery.getInMemorySorting(),
                    baseQuery.getFilter().orElse(null)
            );

            List<EmployeeAttendanceDetail> batch = dp.fetch(q).toList();
            if (batch.isEmpty()) return -1;

            for (int i = 0; i < batch.size(); i++) {
                EmployeeAttendanceDetail r = batch.get(i);
                if (r != null && Objects.equals(r.getId(), targetId)) {
                    return offset + i;
                }
            }

            offset += batch.size();
            if (batch.size() < pageSize) return -1;
        }

        return -1;
    }
    
    private void pasteToTarget(
            GridContextMenu.GridContextMenuItemClickEvent<EmployeeAttendanceDetail> e,
            PasteMode mode
    ) {
        if (clipboard == null || clipboard.isEmpty()) {
            Notification.show("Nothing copied yet | មិនទាន់មានអ្វីត្រូវបានចម្លង", 2500, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        // targets = selected rows if any, otherwise context row
        List<EmployeeAttendanceDetail> targets = new ArrayList<>(grid.getSelectedItems());
        if (targets.isEmpty()) {
            e.getItem().ifPresent(targets::add);
        }

        // remove copied/source row
        List<EmployeeAttendanceDetail> filteredTargets = targets.stream()
                .filter(t -> clipboardSourceId == null || !Objects.equals(t.getId(), clipboardSourceId))
                .collect(Collectors.toList());

        if (filteredTargets.isEmpty()) {
            Notification.show("Nothing to paste (source row skipped) | មិនមានជួរដើម្បីបិទភ្ជាប់", 2000, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        confirmPaste(mode, filteredTargets);
    }

    private void confirmPaste(PasteMode mode, List<EmployeeAttendanceDetail> targets) {

        if (clipboard == null || clipboard.isEmpty()) {
            Notification.show("Nothing copied yet | មិនទាន់មានអ្វីត្រូវបានចម្លង", 2500, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        final List<EmployeeAttendanceDetail> finalTargets = List.copyOf(targets); // ✅ final for lambda

        String modeLabel = switch (mode) {
            case TEAM -> "Team";
            case MINEFIELD -> "Minefield";
            case TEAM_AND_MINEFIELD -> "Team & Minefield";
        };

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Paste | បញ្ជាក់ការបិទភ្ជាប់");
        dialog.setText("Paste " + modeLabel + " to " + finalTargets.size()
                + " row(s)?\n\nបិទភ្ជាប់ " + modeLabel + " ទៅ " + finalTargets.size() + " ជួរ?");
        dialog.setConfirmText("Paste | បិទភ្ជាប់");
        dialog.setConfirmButtonTheme("primary success");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel | បោះបង់");
        dialog.setCancelButtonTheme("primary error");
        
        //dialog.addCancelListener(ev -> dialog.close());
        dialog.addConfirmListener(ev -> pasteToTargets(finalTargets, mode)); // ✅ no error now
        dialog.open();
    }
    private void pasteToTargets(List<EmployeeAttendanceDetail> targets, PasteMode mode) {

        User auditUser = currentUserLogin.orElse(null);

        try {
            for (EmployeeAttendanceDetail target : targets) {
                if (target == null) continue;

                if (target.getAttendanceDetailTeams() == null) {
                    target.setAttendanceDetailTeams(new ArrayList<>());
                }

                switch (mode) {
                    case TEAM -> pasteTeamsOnly(target, auditUser);
                    case MINEFIELD -> pasteMinefieldsOnly(target, auditUser);
                    case TEAM_AND_MINEFIELD -> pasteTeamsAndMinefields(target, auditUser);
                }

                service.update(target);
            }

            refreshGrid();
            Notification.show("Pasted to " + targets.size() + " row(s) ✅", 1800, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception ex) {
            Notification.show("Paste failed: " + ex.getMessage(), 3500, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            ex.printStackTrace();
        }
    }

    private void pasteTeamsOnly(EmployeeAttendanceDetail target, User auditUser) {
        // Replace teams; clear tasks
        target.getAttendanceDetailTeams().clear();

        for (var ct : clipboard.teamsById.values()) {
            EmployeeAttendanceDetailTeam tr = new EmployeeAttendanceDetailTeam();
            tr.setAttendanceDetail(target);
            tr.setTeam(ct.team);

            if (auditUser != null) {
                tr.setUserCreated(auditUser);
                tr.setUserUpdated(auditUser);
            }

            tr.setTeamTasks(new ArrayList<>()); // no tasks
            target.getAttendanceDetailTeams().add(tr);
        }
    }

    private void pasteMinefieldsOnly(EmployeeAttendanceDetail target, User auditUser) {
        // Only set tasks for matching teams; if team doesn't exist -> skip
        Map<Long, EmployeeAttendanceDetailTeam> targetTeams = target.getAttendanceDetailTeams().stream()
                .filter(t -> t != null && t.getTeam() != null && t.getTeam().getId() != null)
                .collect(Collectors.toMap(t -> t.getTeam().getId(), t -> t, (a,b) -> a));

        for (var entry : clipboard.teamsById.entrySet()) {
            Long teamId = entry.getKey();
            var ct = entry.getValue();

            EmployeeAttendanceDetailTeam tr = targetTeams.get(teamId);
            if (tr == null) {
                // No matching team row in target
                continue;
            }

            setTasks(tr, ct.tasks, auditUser);
        }
    }

    private void pasteTeamsAndMinefields(EmployeeAttendanceDetail target, User auditUser) {
        // Replace everything (teams + their tasks)
        target.getAttendanceDetailTeams().clear();

        for (var ct : clipboard.teamsById.values()) {
            EmployeeAttendanceDetailTeam tr = new EmployeeAttendanceDetailTeam();
            tr.setAttendanceDetail(target);
            tr.setTeam(ct.team);

            if (auditUser != null) {
                tr.setUserCreated(auditUser);
                tr.setUserUpdated(auditUser);
            }

            tr.setTeamTasks(new ArrayList<>());
            setTasks(tr, ct.tasks, auditUser);

            target.getAttendanceDetailTeams().add(tr);
        }
    }

    private void setTasks(EmployeeAttendanceDetailTeam teamRow, Set<Tasks> tasks, User auditUser) {
        if (teamRow.getTeamTasks() == null) {
            teamRow.setTeamTasks(new ArrayList<>());
        }

        List<EmployeeAttendanceDetailTeamTask> joinList = teamRow.getTeamTasks();
        Set<Tasks> selected = new java.util.HashSet<>(tasks);

        // remove unselected
        joinList.removeIf(j -> j.getTask() != null && !selected.contains(j.getTask()));

        // add missing
        for (Tasks t : selected) {
            boolean exists = joinList.stream().anyMatch(j -> Objects.equals(j.getTask(), t));
            if (!exists) {
                EmployeeAttendanceDetailTeamTask j = new EmployeeAttendanceDetailTeamTask();
                j.setAttendanceDetailTeam(teamRow);
                j.setTask(t);

                if (auditUser != null) {
                    j.setUserCreated(auditUser);
                    j.setUserUpdated(auditUser);
                }
                joinList.add(j);
            }
        }

        // bump updatedBy
        if (auditUser != null) {
            for (EmployeeAttendanceDetailTeamTask j : joinList) {
                if (j.getUserCreated() == null) j.setUserCreated(auditUser);
                j.setUserUpdated(auditUser);
            }
        }
    }

    
    private Component createIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("margin-inline-end", "var(--lumo-space-s")
                .set("padding", "var(--lumo-space-xs)");
        return icon;
    } 
    
    private ComponentRenderer<Component, EmployeeAttendanceDetail> createTabRenderer() {
        return new ComponentRenderer<>(entityRecord -> {
            TabSheet tabSheet = new TabSheet();

            Grid<EmployeeAttendanceDetailTeam> gridDetailTab =new Grid<>(EmployeeAttendanceDetailTeam.class, false);

            gridDetailTab.addColumn(EmployeeAttendanceDetailTeam::getId).setHeader("ID").setKey("id");
            gridDetailTab.addColumn(detailTeam -> detailTeam.getTeam()!=null? detailTeam.getTeam().getTeamCode() :"").setHeader(this.teamTeam.getLabel());

            gridDetailTab.addColumn(detailTask -> {
                if (detailTask.getTeamTasks() == null || detailTask.getTeamTasks().isEmpty()) {
                    return "";
                }

                return detailTask.getTeamTasks().stream()
                        .map(EmployeeAttendanceDetailTeamTask::getTask)
                        .filter(Objects::nonNull)
                        .map(Tasks::getTaskCode)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));
            }).setHeader(this.teamTask.getLabel());
            
        	gridDetailTab.addColumn(entitySelected -> {
    	        return entitySelected.getUserCreated() != null ? entitySelected.getUserCreated().getName() : "";
    	    }).setHeader("Created by")
    	      .setSortProperty("userCreated.name")
    	      .setKey("userCreated.name");

        	gridDetailTab.addColumn(entitySelected -> {
    	        return entitySelected.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getCreatedAt()) : "";
    	    }).setHeader("Created At")
    	      .setSortProperty("createdAt")
    	      .setKey("createdAt");

        	gridDetailTab.addColumn(entitySelected -> {
    	        return entitySelected.getUserUpdated() != null ? entitySelected.getUserUpdated().getName() : "";
    	    }).setHeader("Updated by")
    	      .setSortProperty("userUpdated.name")
    	      .setKey("userUpdated.name");

        	gridDetailTab.addColumn(entitySelected -> {
    	        return entitySelected.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getUpdatedAt()) : "";
    	    }).setHeader("Updated At")
    	      .setSortProperty("updatedAt")
    	      .setKey("updatedAt");
        	
        	gridDetailTab.getColumns().forEach(column -> {
                column.setResizable(true);   // Enable resizing for all columns
                column.setSortable(true);    // Enable sorting for all columns
                column.setTextAlign(ColumnTextAlign.CENTER);
                if(column.getHeaderText() !="ID") {
                	 column.setAutoWidth(true);
                }
               
            });
        	gridDetailTab.setAllRowsVisible(true);
        	Tab teamTab = tabSheet.add("Team & Tasks | ព័ត៌មានលម្អិត",gridDetailTab);        	
        	  List<Tab> allTabs = List.of(teamTab);
              styleTab(teamTab, true);             
              tabSheet.addSelectedChangeListener(event -> {
                  Tab selected = event.getSelectedTab();
                  allTabs.forEach(tab -> styleTab(tab, tab == selected));
              });
            tabSheet.setSizeFull();
            if (entityRecord != null && entityRecord.getAttendanceDetailTeams() != null) {
                gridDetailTab.setItems(entityRecord.getAttendanceDetailTeams());
            } else {
                gridDetailTab.setItems(Collections.emptyList());
            }
            return tabSheet;
        });
    }
    private void styleTab(Tab tab, boolean selected) {
        // base small pill style
        tab.getStyle().set("border-radius", "999px").set("padding", "0.05rem 0.7rem").set("font-size", "var(--lumo-font-size-s)").set("line-height", "1");
        if (selected) {
            tab.getStyle().set("background","linear-gradient(90deg, rgb(11,60,97), rgb(0,141,168))").set("color", "white").set("border", "none");
        } else {
            tab.getStyle().remove("background").remove("color").remove("border");
        }
    }


    @Override
    protected void configureEditorLayout() throws Exception {
        editorLayout.setDialogTitle("Daily Attendance | វត្តមានប្រចាំថ្ងៃ");

        // ============================================================
        // 🧍‍♂️ Report INFORMATION
        // ============================================================


        FormLayout reportInfoForm = new FormLayout(reportedByEmployee,reportedByPosition,reportedDate,reportLocation);
        reportInfoForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3)
        );
        reportInfoForm.setWidthFull();

        Details reportInfoSection = new Details(
                "📝 Reported Information | ព័ត៌មានអ្នករាយការណ៍",
                reportInfoForm
        );
        reportInfoSection.addThemeVariants(DetailsVariant.FILLED);
        reportInfoSection.setOpened(true);
        reportInfoSection.setWidthFull();
        reportInfoSection.getStyle().set("border", "3px solid rgb(232, 125, 30)").set("border-radius", "15px");
        //reportInfoSection.getStyle().set("border", "3px solid transparent").set("border-radius", "15px").set("border-image", "linear-gradient(90deg, #E87D1E, #FFC857) 1"); 
        
        // ============================================================
        // 🧍‍♂️ Attendance  INFORMATION
        // ============================================================
        FormLayout attendanceForm = new FormLayout(employee,attendanceDate,this.leaveTypeGroup, leaveType,leaveTypeSubType,leaveDuration,firstIn, lastOut,breakMinutes,totalHours,normalHours,overtimeHours);
        attendanceForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3)
        );
        attendanceForm.setColspan(leaveTypeSubType, 3);        
        attendanceForm.setWidthFull();
        
        Details attendanceInfoSection = new Details(
                "📅 Attendance Information | ព័ត៌មានវត្តមាន",
                attendanceForm
        );
        attendanceInfoSection.addThemeVariants(DetailsVariant.FILLED);
        attendanceInfoSection.setOpened(true);
        attendanceInfoSection.setWidthFull();
        attendanceInfoSection.getStyle().set("border", "3px solid rgb(232, 125, 30)").set("border-radius", "15px");

        
        
        // ============================================================
        // 🧍‍♂️ Team Task INFORMATION
        // ============================================================
       	gridAttendanceDetailTeam.setWidthFull();
    	gridAttendanceDetailTeam.addThemeVariants( GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
    	gridAttendanceDetailTeam.setAllRowsVisible(true);
    	gridAttendanceDetailTeam.addColumn(team -> team.getTeam() != null ? team.getTeam().getTeamCode()  : "").setHeader(this.teamTeam.getLabel()).setKey("team") .setAutoWidth(true);

    	gridAttendanceDetailTeam.addColumn(team -> {
    	    if (team.getTeamTasks() == null || team.getTeamTasks().isEmpty()) {
    	        return "";
    	    }
    	    return team.getTeamTasks().stream()
    	            .map(EmployeeAttendanceDetailTeamTask::getTask) // join → Task
    	            .filter(Objects::nonNull)
    	            .map(Tasks::getTaskCode)                        // Task → code
    	            .sorted()
    	            .collect(Collectors.joining(", "));
    	}).setHeader(this.teamTask.getLabel()).setKey("taskss").setAutoWidth(true);

    	
    	gridAttendanceDetailTeam.addComponentColumn(team -> {
    	    Button edit = new Button(new Icon(VaadinIcon.EDIT));
    	    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);    	   
    	    edit.getElement().setProperty("title", "Edit | កែប្រែ");
    	    edit.addClickListener(e -> openTeamEditDialog(team));

    	    Button delete = new Button(new Icon(VaadinIcon.TRASH));
    	    delete.addThemeVariants(ButtonVariant.LUMO_ERROR,
    	            ButtonVariant.LUMO_TERTIARY,
    	            ButtonVariant.LUMO_ICON);
    	    delete.getElement().setProperty("title", "Delete | លុប");
    	    delete.addClickListener(e2 -> removeTeamFromCurrentDetail(team));

    	    HorizontalLayout layout = new HorizontalLayout(edit, delete);
    	    layout.setSpacing(false);
    	    layout.setPadding(false);
    	    layout.setMargin(false);
    	    layout.setWidthFull();
    	    layout.setJustifyContentMode(JustifyContentMode.CENTER);
    	    layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    	    return layout;
    	    
	    	}).setHeader("Actions | សកម្មភាព").setTextAlign(ColumnTextAlign.CENTER).setAutoWidth(true);
    	
    	
	    	Button assignTeamButton =new Button("Assign Team & Tasks | កំណត់ក្រុម និងភារកិច្ច", e -> {
	        if (this.entity == null) {
	            Notification.show(
	                "Please select an attendance detail first | សូមជ្រើសរើសវត្តមានជាមុនសិន",
	                3000, Position.TOP_CENTER
	            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
	            return;
	        }

	        // 🔹 New team row
	        currentTeam = new EmployeeAttendanceDetailTeam();
	        currentTeam.setAttendanceDetail(this.entity);

	        // Attach to parent if not yet
	        if (this.entity.getAttendanceDetailTeams() == null) {
	        	this.entity.setAttendanceDetailTeams(new ArrayList<>());
	        }
	        if (!this.entity.getAttendanceDetailTeams().contains(currentTeam)) {
	        	this.entity.getAttendanceDetailTeams().add(currentTeam);
	        }

	        // Bind new bean → fields become empty
	        detailTeamBinder.setBean(currentTeam);

	        attendanceDetailTeamDialog.open();
	    });
    	assignTeamButton.setIcon(new Icon(VaadinIcon.GROUP));
    	assignTeamButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_SMALL);
    	assignTeamButton.getStyle().set("background", "linear-gradient(90deg, rgb(11,60,97), rgb(0,141,168))").set("color", "white").set("border-radius", "999px").set("border", "none").set("padding-inline", "1.4rem");

    	VerticalLayout layuoutTeam = new VerticalLayout(assignTeamButton, this.gridAttendanceDetailTeam);
    	layuoutTeam.setSpacing(false);
    	layuoutTeam.setMargin(false);
    	layuoutTeam.setSpacing(false);
        this.configureAttendanceTeamDialog();

        Details teamTaskSection = new Details(
                "👥 Team & Tasks Information | ព័ត៌មានក្រុម និងភារកិច្ច",
                layuoutTeam);
        teamTaskSection.addThemeVariants(DetailsVariant.FILLED);
        teamTaskSection.setOpened(true);
        teamTaskSection.setWidthFull();               
        teamTaskSection.getStyle().set("border", "3px solid rgb(232, 125, 30)").set("border-radius", "15px");

        
   
        
        // ============================================================
        // DATA ENTRY INFORMATION
        // ============================================================
        FormLayout dataEntryForm = new FormLayout(
                dataEntryBy, dataEntryPosition, dataEntryLocation, dataEntryDate, entryStatus);
        dataEntryForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3)
        );
        dataEntryForm.setWidthFull();
        

        Details dataEntrySection = new Details(
                "⌨ Data Entry Information | ព័ត៌មានការបញ្ចូលទិន្នន័យ",
                dataEntryForm
        );
        dataEntrySection.addThemeVariants(DetailsVariant.FILLED);
        dataEntrySection.setOpened(true);
        dataEntrySection.setWidthFull();
        dataEntrySection.getStyle().set("border", "3px solid rgb(232, 125, 30)").set("border-radius", "15px");
        
        
        // ============================================================
        // MAIN FORM
        // ============================================================
        FormLayout mainForm = new FormLayout(reportInfoSection,attendanceInfoSection,teamTaskSection,dataEntrySection,remark);
        mainForm.setColspan(reportInfoSection, 2);
        mainForm.setColspan(attendanceInfoSection, 2);
        mainForm.setColspan(dataEntrySection, 2);
        mainForm.setColspan(remark, 2);
        mainForm.setColspan(teamTaskSection, 2);
        mainForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2)
        );
        mainForm.setWidthFull();

        Stream.of(reportInfoSection,attendanceInfoSection,teamTaskSection,dataEntrySection)
        .forEach(details -> {
            details.getStyle()
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "0 2px 8px var(--lumo-contrast-20pct)")
                .set("background-color", "var(--lumo-base-color)")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("padding", "0")
                .set("width", "100%");
            
            details.getElement().executeJs(
                "this.shadowRoot.querySelector('[part=\"content\"]')?.style.setProperty('padding', 'var(--lumo-space-m)')"
            );
        });


        // ============================================================
        // FIXED: Compact layout (no large side margins)
        // ============================================================
        HorizontalLayout layout = new HorizontalLayout(mainForm);
        layout.setPadding(false);
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle()
            .set("padding-left", "var(--lumo-space-m)")
            .set("padding-right", "var(--lumo-space-m)")
            .set("margin", "0");
        editorLayout.add(layout);

        
        this.reportedByEmployee.setItemLabelGenerator(this::formatAttendanceEmployeeLabel);
        this.reportedByEmployee.addValueChangeListener(e->{
        	if(e.getValue()!=null) {
        		this.reportedByPosition.setValue(e.getValue().getPositions());
        		this.reportLocation.setValue(e.getValue().getBranch());
        	}else {
        		this.reportedByPosition.clear();
        		this.reportLocation.clear();
        	}
        });

        
        this.reportedByPosition.setItemLabelGenerator(e ->this.formatPosition(e));        
        this.reportLocation.setItemLabelGenerator(Branch::getBranchShortName);
        
        
        
        this.employee.setItemLabelGenerator(this::formatAttendanceEmployeeLabel);
        this.employee.addValueChangeListener(e->{
        	this.findRoster(this.employee.getValue(),this.attendanceDate.getValue());
        });        
        this.attendanceDate.addValueChangeListener(e->{
        	this.findRoster(this.employee.getValue(),this.attendanceDate.getValue());
        });
        
        
        this.leaveType.setItemLabelGenerator(e -> this.formatLeaveType(e));
        this.leaveTypeSubType.setItemLabelGenerator(e -> this.formatLeaveSubType(e));
        
        this.leaveType.addValueChangeListener(e -> {
        	LeaveType selectedLeave = e.getValue();
        	
            this.leaveTypeSubType.clear();
            this.leaveTypeSubType.setItems(Collections.emptyList());

            if (selectedLeave != null) {
                this.leaveTypeGroup.setValue(selectedLeave.getLeaveTypeGroup());
                
                List<LeaveTypeSubType> subTypes = e.getValue().getLeaveTypeSubTypes();
                if (subTypes != null && !subTypes.isEmpty()) {
                    this.leaveTypeSubType.setItems(subTypes);
                    // 🔹 Only show required indicator when there ARE sub types
                    this.leaveTypeSubType.setRequiredIndicatorVisible(true);
                    this.leaveTypeSubType.setVisible(true);
                } else {
                    // 🔹 No sub types -> not required
                    this.leaveTypeSubType.setRequiredIndicatorVisible(false);
                    this.leaveTypeSubType.setVisible(false);
                }
                

            } else {
                this.leaveTypeSubType.setRequiredIndicatorVisible(false);
            }
            checkLeaveFullDay();
        });
        
        this.leaveDuration.addValueChangeListener(e->checkLeaveFullDay());
        
        
        
        this.qcByEmployee.setItemLabelGenerator(this::formatAttendanceEmployeeLabel);
        this.qcByEmployee.addValueChangeListener(e->{
        	if(e.getValue()!=null) {
        		this.qcByPosition.setValue(e.getValue().getPositions());
        		this.qcLocation.setValue(e.getValue().getBranch());
        	}else {
        		this.qcByPosition.clear();
        		this.qcLocation.clear();
        	}
        });
        
        
        this.qcByPosition.setItemLabelGenerator(e -> this.formatPosition(e));        
        this.qcLocation.setItemLabelGenerator(Branch::getBranchShortName);
        
        this.totalHours.setReadOnly(true);
        this.overtimeHours.setReadOnly(true);
        
        this.firstIn.addValueChangeListener(e->{
        	this.recalcHours();
        });        
        this.lastOut.addValueChangeListener(e->{
        	this.recalcHours();
        });
        this.breakMinutes.addValueChangeListener(e->{
        	this.recalcHours();
        });
        this.normalHours.addValueChangeListener(e->{
        	this.recalcHours();
        });
        
        
        
    }
    

    private void findRoster(Employee emp, LocalDate rosterDay) {
        // Guard clause for nulls from UI
        if (emp == null || rosterDay == null) {
            this.leaveType.clear();
            this.leaveTypeSubType.clear();
            this.leaveDuration.clear();
            this.firstIn.clear();
            this.lastOut.clear();
            this.breakMinutes.clear();
            this.normalHours.clear();
            this.totalHours.clear();
            this.overtimeHours.clear();
            return;
        }

        employeeRosterRepository.findByEmployeeAndRosterDate(emp, rosterDay)
            .ifPresentOrElse(roster -> {
                System.out.println("Roster found: " + roster);

                LeaveType rosterLeave = null;
                LeaveDuration rosterLeaveDuration = null;

                // 🔹 Defensive: holiday & holiday.leaveType may be null
                var holiday = roster.getHoliday();
                if (holiday != null && holiday.getLeaveType() != null) {
                    rosterLeave = holiday.getLeaveType();
                    rosterLeaveDuration = LeaveDuration.FULL_DAY;
                } else {
                    rosterLeave = roster.getLeaveType();
                    rosterLeaveDuration = roster.getLeaveDuration();
                }

                this.leaveType.setValue(rosterLeave);
                this.leaveTypeSubType.setValue(roster.getLeaveTypeSubType());
                this.leaveDuration.setValue(rosterLeaveDuration);

                this.firstIn.setValue(roster.getStartTime());
                this.lastOut.setValue(roster.getEndTime());
                this.breakMinutes.setValue(roster.getBreakMinutes());
                this.normalHours.setValue(roster.getTotalWorkingHour());

                // Recalculate totals based on the loaded values
                //recalcHours();

            }, () -> {
                System.out.println("Roster not found");

                this.leaveType.clear();
                this.leaveTypeSubType.clear();
                this.leaveDuration.clear();
                this.firstIn.clear();
                this.lastOut.clear();
                this.breakMinutes.clear();
                this.normalHours.clear();
                this.totalHours.clear();
                this.overtimeHours.clear();
            });
        recalcHours();
    }

    private void recalcHours() {
        if (this.entity == null) {
            return;
        }

        // entity already has latest values because of setBean()
        this.entity.recalculateHours();

        var total = this.entity.getTotalHours();
        var overtime = this.entity.getOvertimeHours();

        System.out.println("Total Hour: " + (total != null ? total : "0"));

        if (total != null) {
            totalHours.setValue(total);
        } else {
            totalHours.clear();
        }

        if (overtime != null) {
            overtimeHours.setValue(overtime);
        } else {
            overtimeHours.clear();
        }
    }


    private void checkLeaveFullDay() {
        LeaveDuration duration = this.leaveDuration.getValue();
        LeaveType leave = this.leaveType.getValue();

        // Nothing selected → just recalc and exit
        if (leave == null || duration == null) {
            recalcHours();
            return;
        }

        var group = leave.getLeaveTypeGroup();
        Long groupId = (group != null ? group.getId() : null);

        // Get current employee + date once
        Employee emp = (this.entity != null) ? this.entity.getEmployee() : null;
        LocalDate attDate = this.attendanceDate.getValue();

        Optional<Shift> shiftOpt = Optional.empty();
        if (emp != null && attDate != null) {
            shiftOpt = this.employeeAllocationRepository.findShiftByEmployeeAndDate(emp, attDate);
        }

        // ===== Group 2: Leave =====
        if (Objects.equals(groupId, 2L)) {

            switch (duration) {
                case FULL_DAY:
                    // Full day leave → no work time
                    clearTimeFields();
                    this.totalHours.clear();
                    this.overtimeHours.clear();
                    break;

                case MORNING_ONLY:
                    // Morning leave → work only AFTERNOON
                    shiftOpt.ifPresentOrElse(shift -> {
                        if (shift != null) {
                            this.firstIn.setValue(shift.getAfternoonStartTime());
                            this.lastOut.setValue(shift.getAfternoonEndTime());
                            this.breakMinutes.setValue(shift.getAfternoonBreakMinutes());
                        } else {
                            clearTimeFields();
                        }
                    }, this::clearTimeFields);
                    break;

                case AFTERNOON_ONLY:
                    // Afternoon leave → work only MORNING
                    shiftOpt.ifPresentOrElse(shift -> {
                        if (shift != null) {
                            this.firstIn.setValue(shift.getMorningStartTime());
                            this.lastOut.setValue(shift.getMorningEndTime());
                            this.breakMinutes.setValue(shift.getMorningBreakMinutes());
                        } else {
                            clearTimeFields();
                        }
                    }, this::clearTimeFields);
                    break;

                default:
                    break;
            }
        }

        // ===== Group 1: Present (if you want to auto-fill from shift) =====
        else if (Objects.equals(groupId, 1L)) {

            switch (duration) {
                case FULL_DAY:
                    // Full day present → full shift
                    shiftOpt.ifPresentOrElse(shift -> {
                        if (shift != null) {
                            this.firstIn.setValue(shift.getStartTime());
                            this.lastOut.setValue(shift.getEndTime());
                            this.breakMinutes.setValue(shift.getBreakMinutes());
                        } else {
                            clearTimeFields();
                        }
                    }, this::clearTimeFields);
                    break;

                case MORNING_ONLY:
                    // Present only in morning
                    shiftOpt.ifPresentOrElse(shift -> {
                        if (shift != null) {
                            this.firstIn.setValue(shift.getMorningStartTime());
                            this.lastOut.setValue(shift.getMorningEndTime());
                            this.breakMinutes.setValue(shift.getMorningBreakMinutes());
                        } else {
                            clearTimeFields();
                        }
                    }, this::clearTimeFields);
                    break;

                case AFTERNOON_ONLY:
                    // Present only in afternoon
                    shiftOpt.ifPresentOrElse(shift -> {
                        if (shift != null) {
                            this.firstIn.setValue(shift.getAfternoonStartTime());
                            this.lastOut.setValue(shift.getAfternoonEndTime());
                            this.breakMinutes.setValue(shift.getAfternoonBreakMinutes());
                        } else {
                            clearTimeFields();
                        }
                    }, this::clearTimeFields);
                    break;

                default:
                    break;
            }
        }

        // Other groups → no special auto logic (keep current values)

        // Recalculate based on whatever we set above
        recalcHours();
    }

    private void clearTimeFields() {
        this.firstIn.clear();
        this.lastOut.clear();
        this.breakMinutes.clear();
    }

    @Override
    protected void beforeSave(EmployeeAttendanceDetail attendance, boolean isNew) {
        if (attendance.getAttendanceDetailTeams() == null) {
            attendance.setAttendanceDetailTeams(new ArrayList<>());
        }
    }

    @Override
    protected String getSaveSuccessMessage(boolean isNew) {
        return "Attendance saved successfully | ការរក្សាទុកវត្តមានបានជោគជ័យ";
    }


    @Override
    protected void populateForm(EmployeeAttendanceDetail record) throws Exception {
        ensureEditorLookupDataLoaded();
        this.entity = record;
		findCurrentEmployee().ifPresent(currentEmployee -> {
		    this.entity.setDataEntryBy(currentEmployee);
		    this.entity.setDataEntryPosition(currentEmployee.getPositions());
		    this.entity.setDataEntryLocation(currentEmployee.getBranch());
		    this.entity.setDataEntryDate(LocalDate.now());
		    if (this.entity.getEntryStatus() == null) {
		        this.entity.setEntryStatus(AttendanceEntryStatus.DRAFT);
		    }
		});
        binder.readBean(entity);
        refreshTeamGridForCurrentDetail();
        editorLayout.open();
        
    	/*

        // Make sure we always have an instance
        if (record == null) {
        	record = new EmployeeAttendanceDetail();
        	record.setSource("MANUAL");
        }
        this.entity = record;
        binder.setBean(this.entity);
        
        boolean isNewEntity = (entity.getId() == null || entity.getId() == 0);

        if (isNewEntity && currentUserLogin.isPresent()) {
            String insuranceStr = currentUserLogin.get().getInsurance();

            if (insuranceStr != null && !insuranceStr.isBlank()) {
                try {
                    int insuranceNo = Integer.parseInt(insuranceStr);

                    employeeRepository.findByInsuranceNo(insuranceNo)
                        .ifPresent(emp -> {
                            this.entity.setReportedByEmployee(emp);
                            this.entity.setReportedByPosition(emp.getPositions());
                            this.entity.setReportLocation(emp.getBranch());
                            this.entity.setReportedDate(LocalDate.now()); // ✅ instead of Now()
                            
                            this.entity.setAttendanceDate(LocalDate.now());
                        });

                } catch (NumberFormatException ex) {
                    // optional: log or ignore if insurance is not numeric
                    System.out.println("Invalid insurance number: " + insuranceStr);
                }
            }
        }

        // Bind entity → fields
        binder.setBean(this.entity);
        
        if (this.entity.getLeaveType() != null) {
            this.leaveTypeGroup.setValue(this.entity.getLeaveType().getLeaveTypeGroup());
            
            // Trigger the listener manually to load sub-types correctly
            // (binder.setBean may not always fire value change listeners)
            this.leaveTypeSubType.setItems(this.entity.getLeaveType().getLeaveTypeSubTypes());
            if (this.entity.getLeaveTypeSubType() != null) {
                this.leaveTypeSubType.setValue(this.entity.getLeaveTypeSubType());
            }
        }
        
        refreshTeamGridForCurrentDetail();
        editorLayout.open();
        */
    }




    @Override
    protected Specification<EmployeeAttendanceDetail> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {

                
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                String quick = quickSearch;
                boolean hasQuickSearch = quick != null && !quick.isBlank();
                boolean needsTeamJoin = hasQuickSearch
                        || (advanceFilterTeams.getValue() != null && !advanceFilterTeams.getValue().isEmpty())
                        || (advanceFilterTasks.getValue() != null && !advanceFilterTasks.getValue().isEmpty());

                // === JOINS ===
                // To-one joins are needed only by the broad quick search. Advanced
                // filters compare the association path directly, so the normal page
                // and count queries remain small.
                Join<EmployeeAttendanceDetail, Employee> empJoin = null;
                Join<EmployeeAttendanceDetailTeam, Teams> joinTeam = null;
                Join<EmployeeAttendanceDetailTeamTask, Tasks> joinTasks = null;
                if (needsTeamJoin) {
                    Join<EmployeeAttendanceDetail, EmployeeAttendanceDetailTeam> joinDetailTeam =
                            root.join("attendanceDetailTeams", JoinType.LEFT);
                    joinTeam = joinDetailTeam.join("team", JoinType.LEFT);
                    Join<EmployeeAttendanceDetailTeam, EmployeeAttendanceDetailTeamTask> joinDetailTasks =
                            joinDetailTeam.join("teamTasks", JoinType.LEFT);
                    joinTasks = joinDetailTasks.join("task", JoinType.LEFT);
                }
                Join<EmployeeAttendanceDetail, LeaveType> leaveTypeJoin = null;
                Join<EmployeeAttendanceDetail, LeaveTypeSubType> leaveTypeSubTypeJoin = null;
                Join<EmployeeAttendanceDetail, Employee> empReportedJoin = null;
                Join<EmployeeAttendanceDetail, Positions> empReportedPositionJoin = null;
                Join<EmployeeAttendanceDetail, Branch> empReportedBranchJoin = null;
                Join<EmployeeAttendanceDetail, Employee> empQCJoin = null;
                Join<EmployeeAttendanceDetail, Positions> empQCPositionJoin = null;
                Join<EmployeeAttendanceDetail, Branch> empQCBranchJoin = null;
                Join<EmployeeAttendanceDetail, User> userCreated = null;
                Join<EmployeeAttendanceDetail, User> userUpdated = null;

                if (hasQuickSearch) {
                    empJoin = root.join("employee", JoinType.LEFT);
                    leaveTypeJoin = root.join("leaveType", JoinType.LEFT);
                    leaveTypeSubTypeJoin = root.join("leaveTypeSubType", JoinType.LEFT);
                    empReportedJoin = root.join("reportedByEmployee", JoinType.LEFT);
                    empReportedPositionJoin = root.join("reportedByPosition", JoinType.LEFT);
                    empReportedBranchJoin = root.join("reportLocation", JoinType.LEFT);
                    empQCJoin = root.join("qcByEmployee", JoinType.LEFT);
                    empQCPositionJoin = root.join("qcByPosition", JoinType.LEFT);
                    empQCBranchJoin = root.join("qcLocation", JoinType.LEFT);
                    userCreated = root.join("userCreated", JoinType.LEFT);
                    userUpdated = root.join("userUpdated", JoinType.LEFT);
                }
                
                // Only the Team/Task one-to-many joins can duplicate attendance rows.
                query.distinct(needsTeamJoin);

                
                // Keep DRAFT as the default Daily Attendance work queue, but let
                // the advanced Entry Status filter explicitly display other states.
                Set<AttendanceEntryStatus> selectedEntryStatuses = advanceFilterEntryStatus.getValue();
                if (selectedEntryStatuses == null || selectedEntryStatuses.isEmpty()) {
                    predicates.add(cb.equal(root.get("entryStatus"), AttendanceEntryStatus.DRAFT));
                }
                
             // === DEFAULT BRANCH FILTER based on user's roles ===
                if (currentUserLogin.isPresent()) {
                    User user = currentUserLogin.get();
                    
                    List<Long> allowedBranchIds = user.getRoles().stream()
                            .flatMap(role -> {
                                Collection<Branch> branches = role.getBranchs(); // adjust if method name is different
                                return branches != null ? branches.stream() : Stream.empty();
                            })
                            .map(Branch::getId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

                    if (!allowedBranchIds.isEmpty()) {
                        predicates.add(root.get("reportLocation").get("id").in(allowedBranchIds));
                    } else {
                        // Security: user with no branch access sees nothing
                        predicates.add(cb.disjunction()); // always false
                    }
                }
                


                // === QUICK SEARCH ===
                if (hasQuickSearch) {
                    String like = "%" + quick.toLowerCase().trim() + "%";

                    predicates.add(cb.or(
                        buildLikePredicate(cb, root.get("id"), like),
                        
                        buildLikePredicate(cb, empJoin.get("nameEn"), like),
                        buildLikePredicate(cb, empJoin.get("nameKh"), like),
                        buildLikePredicate(cb, empJoin.get("insuranceNo"), like),
                        buildLikePredicate(cb, root.get("attendanceDate"), like),
                        buildLikePredicate(cb, joinTeam.get("teamCode"), like),
                        buildLikePredicate(cb, joinTasks.get("taskCode"), like),
                        buildLikePredicate(cb, leaveTypeJoin.get("leaveNameEn"), like),
                        buildLikePredicate(cb, leaveTypeJoin.get("leaveNameKh"), like),
                        buildLikePredicate(cb, leaveTypeSubTypeJoin.get("leaveSubTypeNameEn"), like),
                        buildLikePredicate(cb, leaveTypeSubTypeJoin.get("leaveSubTypeNameKh"), like),
                        buildLikePredicate(cb, root.get("leaveDuration"), like),
                        buildLikePredicate(cb, root.get("firstIn"), like),
                        buildLikePredicate(cb, root.get("lastOut"), like),
                        buildLikePredicate(cb, root.get("breakMinutes"), like),
                        buildLikePredicate(cb, root.get("totalHours"), like),
                        buildLikePredicate(cb, root.get("normalHours"), like),
                        buildLikePredicate(cb, root.get("overtimeHours"), like),
                        
                        buildLikePredicate(cb, empReportedJoin.get("nameEn"), like),
                        buildLikePredicate(cb, empReportedJoin.get("nameKh"), like),
                        buildLikePredicate(cb, empReportedJoin.get("insuranceNo"), like),
                        buildLikePredicate(cb, empReportedPositionJoin.get("position"), like),
                        buildLikePredicate(cb, empReportedPositionJoin.get("positionKh"), like),
                        buildLikePredicate(cb, root.get("reportedDate"), like),
                        buildLikePredicate(cb, empReportedBranchJoin.get("branchShortName"), like),
                        
                        buildLikePredicate(cb, empQCJoin.get("nameEn"), like),
                        buildLikePredicate(cb, empQCJoin.get("nameKh"), like),
                        buildLikePredicate(cb, empQCJoin.get("insuranceNo"), like),
                        buildLikePredicate(cb, empQCPositionJoin.get("position"), like),
                        buildLikePredicate(cb, empQCPositionJoin.get("positionKh"), like),
                        buildLikePredicate(cb, root.get("qcDate"), like),
                        buildLikePredicate(cb, empQCBranchJoin.get("branchShortName"), like),
                        
                        buildLikePredicate(cb, root.get("remark"), like),
                        buildLikePredicate(cb, root.get("survey123Id"), like),
                        
                        
                        buildLikePredicate(cb, userCreated.get("name"), like),
                        buildLikePredicate(cb, userUpdated.get("name"), like),
                        buildLikePredicate(cb, root.get("createdAt"), like),
                        buildLikePredicate(cb, root.get("updatedAt"), like)
                    ));
                }

                // === ID FILTER ===
                if (advanceFilterID.getValue() != null) {
                    predicates.add(cb.equal(root.get("id"), advanceFilterID.getValue().longValue()));
                    sqlFilter.add("ID = " + advanceFilterID.getValue().longValue());
                }
                
                buildInPredicate(cb, root.<Employee>get("employee"), advanceFilterEmployee.getValue(), Employee::getNameEn,
                        employee.getLabel(), predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("attendanceDate"),
                        advanceFilterAttendanceDate.getFrom(), advanceFilterAttendanceDate.getTo(),
                        attendanceDate.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, joinTeam, advanceFilterTeams.getValue(), Teams::getTeamCode,
                        teamTeam.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, joinTasks, advanceFilterTasks.getValue(), Tasks::getTaskCode,
                        teamTask.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<LeaveType>get("leaveType"), advanceFilterLeaveType.getValue(), LeaveType::getLeaveNameEn,
                        leaveType.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<LeaveTypeSubType>get("leaveTypeSubType"), advanceFilterLeaveTypeSubType.getValue(),
                        LeaveTypeSubType::getLeaveSubTypeNameEn, leaveTypeSubType.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.get("leaveDuration"), advanceFilterLeaveDuration.getValue(),
                        LeaveDuration::getLabel, leaveDuration.getLabel(), predicates, sqlFilter);

                buildBetweenPredicate(cb, root.get("firstIn"), advanceFilterFirstInFrom.getValue(),
                        advanceFilterFirstInTo.getValue(), firstIn.getLabel(), predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("lastOut"), advanceFilterlastOutFrom.getValue(),
                        advanceFilterlastOutTo.getValue(), lastOut.getLabel(), predicates, sqlFilter);
                addExactFilter(cb, root.get("breakMinutes"), advanceFilterBreakMinutes.getValue(),
                        breakMinutes.getLabel(), predicates, sqlFilter);
                addExactFilter(cb, root.get("totalHours"), advanceFilterTotalHours.getValue(),
                        totalHours.getLabel(), predicates, sqlFilter);
                addExactFilter(cb, root.get("normalHours"), advanceFilterNormalHours.getValue(),
                        normalHours.getLabel(), predicates, sqlFilter);
                addExactFilter(cb, root.get("overtimeHours"), advanceFilterOvertimeHours.getValue(),
                        overtimeHours.getLabel(), predicates, sqlFilter);

                buildInPredicate(cb, root.<Employee>get("reportedByEmployee"), advanceFilterReportedBy.getValue(), Employee::getNameEn,
                        reportedByEmployee.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<Positions>get("reportedByPosition"), advanceFilterReportedByPosition.getValue(),
                        Positions::getPosition, reportedByPosition.getLabel(), predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("reportedDate"), advanceFilterReportedDate.getFrom(),
                        advanceFilterReportedDate.getTo(), reportedDate.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<Branch>get("reportLocation"), advanceFilterReportLocation.getValue(),
                        Branch::getBranchShortName, reportLocation.getLabel(), predicates, sqlFilter);

                buildInPredicate(cb, root.<Employee>get("qcByEmployee"), advanceFilterQcByEmployee.getValue(), Employee::getNameEn,
                        qcByEmployee.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<Positions>get("qcByPosition"), advanceFilterQcByPosition.getValue(),
                        Positions::getPosition, qcByPosition.getLabel(), predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("qcDate"), advanceFilterQcDate.getFrom(),
                        advanceFilterQcDate.getTo(), qcDate.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<Branch>get("qcLocation"), advanceFilterQcLocation.getValue(),
                        Branch::getBranchShortName, qcLocation.getLabel(), predicates, sqlFilter);

                buildInPredicate(cb, root.<Employee>get("dataEntryBy"), advanceFilterDataEntryBy.getValue(),
                        Employee::getNameEn, dataEntryBy.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<Positions>get("dataEntryPosition"), advanceFilterDataEntryPosition.getValue(),
                        Positions::getPosition, dataEntryPosition.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.<Branch>get("dataEntryLocation"), advanceFilterDataEntryLocation.getValue(),
                        Branch::getBranchShortName, dataEntryLocation.getLabel(), predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("dataEntryDate"), advanceFilterDataEntryDate.getFrom(),
                        advanceFilterDataEntryDate.getTo(), dataEntryDate.getLabel(), predicates, sqlFilter);
                buildInPredicate(cb, root.get("entryStatus"), advanceFilterEntryStatus.getValue(),
                        AttendanceEntryStatus::getLabel, entryStatus.getLabel(), predicates, sqlFilter);

                advanceFilterBuildLikePredicate(cb, root.get("remark"), advanceFilterRemark.getValue(),
                        remark.getLabel(), predicates, sqlFilter);

                buildInPredicate(cb, root.<User>get("userCreated"), advanceFilterCreatedBy.getValue(), User::getName,
                        "Created By", predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("createdAt"), advanceFilterCreatedDate.getFrom(),
                        advanceFilterCreatedDate.getTo(), "Created At", predicates, sqlFilter);
                buildInPredicate(cb, root.<User>get("userUpdated"), advanceFilterUpdatedBy.getValue(), User::getName,
                        "Updated By", predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("updatedAt"), advanceFilterUpdatedDate.getFrom(),
                        advanceFilterUpdatedDate.getTo(), "Updated At", predicates, sqlFilter);

                // === FINAL DISPLAY ===
                showSqlFilterTokens(sqlFilter);

                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception ex) {
                showErrorMessage("Error in filter: " + ex.getMessage());
                ex.printStackTrace();
                return cb.conjunction();
            }
        };
    }

    @Override
    protected Predicate buildLikePredicate(CriteriaBuilder cb, Expression<?> expression, String likePattern) {
        Class<?> type = expression.getJavaType();
        Expression<String> stringExpr;

        // String fields directly
        if (String.class.equals(type)) {
            stringExpr = cb.lower(expression.as(String.class));
        }
        // Numbers -> text
        else if (Number.class.isAssignableFrom(type)) {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        }
        // ✅ LocalTime (TIME) -> cast to text (avoid timezone/to_char datetime)
        else if (java.time.LocalTime.class.isAssignableFrom(type)
                || java.sql.Time.class.isAssignableFrom(type)) {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
            // or: cb.lower(cb.function("to_char", String.class, expression, cb.literal("HH24:MI:SS")))
            // (only if you KNOW your DB supports to_char(time,...); cast-to-text is safest)
        }
        // ✅ LocalDate -> date formatting (optional, but nice)
        else if (java.time.LocalDate.class.isAssignableFrom(type)
                || java.sql.Date.class.isAssignableFrom(type)) {
            // If your DATE_TIME_FORMATTER_DB handles date fine, you can keep using it.
            // Otherwise:
            stringExpr = cb.lower(cb.function("to_char", String.class, expression, cb.literal("DD-MM-YYYY")));
        }
        // DateTime / timestamp types -> your existing formatter
        else if (java.time.LocalDateTime.class.isAssignableFrom(type)
                || java.time.OffsetDateTime.class.isAssignableFrom(type)
                || java.time.ZonedDateTime.class.isAssignableFrom(type)
                || java.time.Instant.class.isAssignableFrom(type)
                || java.util.Date.class.isAssignableFrom(type)) {
            stringExpr = cb.lower(DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(cb, expression));
        }
        // fallback
        else {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        }

        return cb.like(stringExpr, likePattern);
    }

    
    /**
     * 🔹 Universal LIKE predicate builder
     * Handles String, numeric, and temporal (date/time) fields.
     */
 /*  private Predicate buildLikePredicate(CriteriaBuilder cb, Expression<?> expression, String likePattern) {
        Expression<String> stringExpr;

        // Handle String fields directly
        if (String.class.equals(expression.getJavaType())) {
            stringExpr = cb.lower((Expression<String>) expression);
        }
        // Handle numeric fields (Integer, Long, BigDecimal, etc.)
        else if (Number.class.isAssignableFrom(expression.getJavaType())) {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        }
        // Handle date/time fields (LocalDate, LocalDateTime, Date, Timestamp)
        else if (java.time.temporal.Temporal.class.isAssignableFrom(expression.getJavaType())
                || java.util.Date.class.isAssignableFrom(expression.getJavaType())) {
            stringExpr = cb.lower(DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(cb, expression));
        }
        // Fallback for any other type (safe cast to text)
        else {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        }

        return cb.like(stringExpr, likePattern);
    }
   */ 

    /**
     * 🔹 IN predicate builder (for multi-select filters).
     */
    private <E> void buildInPredicate(
            CriteriaBuilder cb,
            Expression<E> expression,
            Collection<E> values,
            Function<E, String> labelMapper,
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter) {

        if (values != null && !values.isEmpty()) {
            predicates.add(expression.in(values));
            if (sqlFilter != null) { // ✅ Only build SQL string if needed
                String joined = values.stream()
                        .map(labelMapper)
                        .filter(Objects::nonNull)
                        .map(v -> "'" + v.replace("'", "''") + "'")
                        .collect(Collectors.joining(", "));
                sqlFilter.add(label + " IN (" + joined + ")");
            }
        }
    }


/*
    private <T extends Comparable<? super T>> void buildBetweenPredicate(
            CriteriaBuilder cb,
            Expression<T> expression,
            T from,
            T to,
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter) {

        if (from != null && to != null) {
            predicates.add(cb.between(expression, from, to));
            sqlFilter.add(String.format("%s BETWEEN '%s' AND '%s'", label, from, to));
        }
    }
*/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void buildBetweenPredicate(
            CriteriaBuilder cb,
            Expression<?> expression,
            Object from,
            Object to,
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter) {

        if (from == null && to == null) return;

        Class<?> exprType = expression.getJavaType();

        // 1) Special case: DatePicker(LocalDate) filtering a datetime/timestamp column
        boolean fromIsDate = from instanceof java.time.LocalDate;
        boolean toIsDate   = to   instanceof java.time.LocalDate;

        boolean exprIsDateTime =
                java.time.LocalDateTime.class.isAssignableFrom(exprType)
                || java.sql.Timestamp.class.isAssignableFrom(exprType)
                || java.util.Date.class.isAssignableFrom(exprType);

        if ((fromIsDate || toIsDate) && exprIsDateTime) {
            java.time.LocalDate fromDate = (java.time.LocalDate) from;
            java.time.LocalDate toDate   = (java.time.LocalDate) to;

            // Start of day / End of day (inclusive)
            java.time.LocalDateTime start = (fromDate != null) ? fromDate.atStartOfDay() : null;
            java.time.LocalDateTime endOfDay = (toDate != null) ? toDate.atTime(java.time.LocalTime.MAX) : null;

            // LocalDateTime column
            if (java.time.LocalDateTime.class.isAssignableFrom(exprType)) {
                Expression<java.time.LocalDateTime> exp = (Expression<java.time.LocalDateTime>) expression;

                if (start != null) {
                    predicates.add(cb.greaterThanOrEqualTo(exp, start));
                    sqlFilter.add(String.format("%s >= '%s'", label, from));
                }
                if (endOfDay != null) {
                    predicates.add(cb.lessThanOrEqualTo(exp, endOfDay));
                    sqlFilter.add(String.format("%s <= '%s'", label, to));
                }
                return;
            }

            // Timestamp column
            if (java.sql.Timestamp.class.isAssignableFrom(exprType)) {
                Expression<java.sql.Timestamp> exp = (Expression<java.sql.Timestamp>) expression;

                if (start != null) {
                    java.sql.Timestamp s = java.sql.Timestamp.valueOf(start);
                    predicates.add(cb.greaterThanOrEqualTo(exp, s));
                    sqlFilter.add(String.format("%s >= '%s'", label, from));
                }
                if (endOfDay != null) {
                    java.sql.Timestamp e = java.sql.Timestamp.valueOf(endOfDay);
                    predicates.add(cb.lessThanOrEqualTo(exp, e));      // ✅ was lessThan
                    sqlFilter.add(String.format("%s <= '%s'", label, to)); // ✅ was <
                }
                return;
            }

            // java.util.Date column
            if (java.util.Date.class.isAssignableFrom(exprType)) {
                Expression<java.util.Date> exp = (Expression<java.util.Date>) expression;

                if (start != null) {
                    java.util.Date s = java.sql.Timestamp.valueOf(start);
                    predicates.add(cb.greaterThanOrEqualTo(exp, s));
                    sqlFilter.add(String.format("%s >= '%s'", label, from));
                }
                if (endOfDay != null) {
                    java.util.Date e = java.sql.Timestamp.valueOf(endOfDay);
                    predicates.add(cb.lessThanOrEqualTo(exp, e));      // ✅ was lessThan
                    sqlFilter.add(String.format("%s <= '%s'", label, to)); // ✅ was <
                }
                return;
            }
        }

        // 2) Default: normal comparable between / >= / <=
        if (from != null && !(from instanceof Comparable)) return;
        if (to != null && !(to instanceof Comparable)) return;

        Expression<? extends Comparable> exp = (Expression<? extends Comparable>) expression;

        if (from != null && to != null) {
            predicates.add(cb.between((Expression) exp, (Comparable) from, (Comparable) to));
            sqlFilter.add(String.format("%s BETWEEN '%s' AND '%s'", label, from, to));
        } else if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo((Expression) exp, (Comparable) from));
            sqlFilter.add(String.format("%s >= '%s'", label, from));
        } else { // to != null
            predicates.add(cb.lessThanOrEqualTo((Expression) exp, (Comparable) to));
            sqlFilter.add(String.format("%s <= '%s'", label, to));
        }
    }

    private void addExactFilter(
            CriteriaBuilder cb,
            Expression<?> expression,
            Object value,
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter) {

        if (value == null) {
            return;
        }

        predicates.add(cb.equal(expression, value));
        sqlFilter.add(label + " = " + value);
    }



    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
                new AdvancedSearchPanel.FilterDef(
                        "id",
                        "ID",
                        () -> {
                            advanceFilterID.addThemeVariants(TextFieldVariant.LUMO_SMALL);
                            advanceFilterID.setClearButtonVisible(true);
                            advanceFilterID.setWidthFull();
                            return advanceFilterID;
                        },
                        component -> advanceFilterID.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "employee",
                        employee.getLabel(),
                        () -> {
                            configureEmployeeMultiSelect(advanceFilterEmployee, ACTIVE_LOCAL_STAFF_TYPES);
                            return advanceFilterEmployee;
                        },
                        component -> advanceFilterEmployee.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "attendanceDate",
                        attendanceDate.getLabel(),
                        () -> {
                            advanceFilterAttendanceDate.setWidthFull();
                            return advanceFilterAttendanceDate;
                        },
                        component -> advanceFilterAttendanceDate.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "team",
                        teamTeam.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterTeams,
                                    teamsRepository.findAll(Sort.by("teamCode")), Teams::getTeamCode);
                            return advanceFilterTeams;
                        },
                        component -> advanceFilterTeams.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "tasks",
                        teamTask.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterTasks,
                                    tasksRepository.findAll(Sort.by("taskCode")), Tasks::getTaskCode);
                            return advanceFilterTasks;
                        },
                        component -> advanceFilterTasks.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "leaveType",
                        leaveType.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterLeaveType,
                                    leaveTypeRepository.findAll(), this::formatLeaveType);
                            return advanceFilterLeaveType;
                        },
                        component -> advanceFilterLeaveType.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "leaveSubType",
                        leaveTypeSubType.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterLeaveTypeSubType,
                                    leaveTypeSubTypeRepository.findAll(), this::formatLeaveSubType);
                            return advanceFilterLeaveTypeSubType;
                        },
                        component -> advanceFilterLeaveTypeSubType.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "duration",
                        leaveDuration.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterLeaveDuration,
                                    List.of(LeaveDuration.values()), LeaveDuration::getLabel);
                            return advanceFilterLeaveDuration;
                        },
                        component -> advanceFilterLeaveDuration.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "firstIn",
                        firstIn.getLabel(),
                        () -> buildTimeRangeFilter(advanceFilterFirstInFrom, advanceFilterFirstInTo),
                        component -> {
                            advanceFilterFirstInFrom.clear();
                            advanceFilterFirstInTo.clear();
                        }
                ),
                new AdvancedSearchPanel.FilterDef(
                        "lastOut",
                        lastOut.getLabel(),
                        () -> buildTimeRangeFilter(advanceFilterlastOutFrom, advanceFilterlastOutTo),
                        component -> {
                            advanceFilterlastOutFrom.clear();
                            advanceFilterlastOutTo.clear();
                        }
                ),
                new AdvancedSearchPanel.FilterDef(
                        "breakMinutes",
                        breakMinutes.getLabel(),
                        () -> configureIntegerFilter(advanceFilterBreakMinutes),
                        component -> advanceFilterBreakMinutes.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "totalHours",
                        totalHours.getLabel(),
                        () -> configureDecimalFilter(advanceFilterTotalHours),
                        component -> advanceFilterTotalHours.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "normalHours",
                        normalHours.getLabel(),
                        () -> configureDecimalFilter(advanceFilterNormalHours),
                        component -> advanceFilterNormalHours.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "overtimeHours",
                        overtimeHours.getLabel(),
                        () -> configureDecimalFilter(advanceFilterOvertimeHours),
                        component -> advanceFilterOvertimeHours.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "reportedBy",
                        reportedByEmployee.getLabel(),
                        () -> {
                            configureEmployeeMultiSelect(advanceFilterReportedBy, ACTIVE_LOCAL_STAFF_TYPES);
                            return advanceFilterReportedBy;
                        },
                        component -> advanceFilterReportedBy.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "reportedPosition",
                        reportedByPosition.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterReportedByPosition,
                                    positionRepository.findByObsolateDateIsNull(), this::formatPosition);
                            return advanceFilterReportedByPosition;
                        },
                        component -> advanceFilterReportedByPosition.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "reportedDate",
                        reportedDate.getLabel(),
                        () -> {
                            advanceFilterReportedDate.setWidthFull();
                            return advanceFilterReportedDate;
                        },
                        component -> advanceFilterReportedDate.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "reportLocation",
                        reportLocation.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterReportLocation,
                                    branchRepository.findByIsActiveTrue(), Branch::getBranchShortName);
                            return advanceFilterReportLocation;
                        },
                        component -> advanceFilterReportLocation.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "qcBy",
                        qcByEmployee.getLabel(),
                        () -> {
                            configureEmployeeMultiSelect(advanceFilterQcByEmployee, ACTIVE_LOCAL_STAFF_TYPES);
                            return advanceFilterQcByEmployee;
                        },
                        component -> advanceFilterQcByEmployee.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "qcPosition",
                        qcByPosition.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterQcByPosition,
                                    positionRepository.findByObsolateDateIsNull(), this::formatPosition);
                            return advanceFilterQcByPosition;
                        },
                        component -> advanceFilterQcByPosition.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "qcDate",
                        qcDate.getLabel(),
                        () -> {
                            advanceFilterQcDate.setWidthFull();
                            return advanceFilterQcDate;
                        },
                        component -> advanceFilterQcDate.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "qcLocation",
                        qcLocation.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterQcLocation,
                                    branchRepository.findByIsActiveTrue(), Branch::getBranchShortName);
                            return advanceFilterQcLocation;
                        },
                        component -> advanceFilterQcLocation.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "dataEntryBy",
                        dataEntryBy.getLabel(),
                        () -> {
                            configureEmployeeMultiSelect(advanceFilterDataEntryBy, ACTIVE_LOCAL_STAFF_TYPES);
                            return advanceFilterDataEntryBy;
                        },
                        component -> advanceFilterDataEntryBy.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "dataEntryPosition",
                        dataEntryPosition.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterDataEntryPosition,
                                    positionRepository.findByObsolateDateIsNull(), this::formatPosition);
                            return advanceFilterDataEntryPosition;
                        },
                        component -> advanceFilterDataEntryPosition.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "dataEntryLocation",
                        dataEntryLocation.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterDataEntryLocation,
                                    branchRepository.findByIsActiveTrue(), Branch::getBranchShortName);
                            return advanceFilterDataEntryLocation;
                        },
                        component -> advanceFilterDataEntryLocation.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "dataEntryDate",
                        dataEntryDate.getLabel(),
                        () -> {
                            advanceFilterDataEntryDate.setWidthFull();
                            return advanceFilterDataEntryDate;
                        },
                        component -> advanceFilterDataEntryDate.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "entryStatus",
                        entryStatus.getLabel(),
                        () -> {
                            setupMultiSelectComboBox(advanceFilterEntryStatus,
                                    List.of(AttendanceEntryStatus.values()), AttendanceEntryStatus::getLabel);
                            return advanceFilterEntryStatus;
                        },
                        component -> advanceFilterEntryStatus.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "remark",
                        remark.getLabel(),
                        () -> configureTextFilter(advanceFilterRemark),
                        component -> advanceFilterRemark.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "createdBy",
                        "Created By | បង្កើតដោយ",
                        () -> {
                            setupMultiSelectComboBox(advanceFilterCreatedBy,
                                    userService.getAllUser(), User::getName);
                            return advanceFilterCreatedBy;
                        },
                        component -> advanceFilterCreatedBy.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "createdAt",
                        "Created At | ថ្ងៃបង្កើត",
                        () -> {
                            advanceFilterCreatedDate.setWidthFull();
                            return advanceFilterCreatedDate;
                        },
                        component -> advanceFilterCreatedDate.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "updatedBy",
                        "Updated By | កែប្រែដោយ",
                        () -> {
                            setupMultiSelectComboBox(advanceFilterUpdatedBy,
                                    userService.getAllUser(), User::getName);
                            return advanceFilterUpdatedBy;
                        },
                        component -> advanceFilterUpdatedBy.clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "updatedAt",
                        "Updated At | ថ្ងៃកែប្រែ",
                        () -> {
                            advanceFilterUpdatedDate.setWidthFull();
                            return advanceFilterUpdatedDate;
                        },
                        component -> advanceFilterUpdatedDate.clear()
                )
        ));

        // Same default behavior as NationalStaffActiveView: start with one
        // useful filter, and let the user add only the filters they need.
        advPanel.addFilter("employee");
        return advPanel;
    }

    private Component buildTimeRangeFilter(TimePicker from, TimePicker to) {
        from.addThemeVariants(TimePickerVariant.LUMO_SMALL);
        from.setPlaceholder("From | ចាប់ពី");
        from.setWidthFull();

        to.addThemeVariants(TimePickerVariant.LUMO_SMALL);
        to.setPlaceholder("To | ដល់");
        to.setWidthFull();

        from.addValueChangeListener(event -> {
            if (event.getValue() != null
                    && to.getValue() != null
                    && to.getValue().isBefore(event.getValue())) {
                to.clear();
            }
        });

        Span separator = new Span("–");
        separator.getStyle().set("white-space", "nowrap");

        HorizontalLayout range = new HorizontalLayout(from, separator, to);
        range.setPadding(false);
        range.setSpacing(true);
        range.setAlignItems(Alignment.BASELINE);
        range.setWidthFull();
        range.setFlexGrow(1, from, to);
        return range;
    }

    private IntegerField configureIntegerFilter(IntegerField field) {
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        field.setClearButtonVisible(true);
        field.setWidthFull();
        return field;
    }

    private BigDecimalField configureDecimalFilter(BigDecimalField field) {
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        field.setClearButtonVisible(true);
        field.setWidthFull();
        return field;
    }

    private TextField configureTextFilter(TextField field) {
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        field.setClearButtonVisible(true);
        field.setPlaceholder("Contains... | មានពាក្យ...");
        field.setWidthFull();
        return field;
    }

    @Override
    protected void onQuickSearch(String text) {
        quickSearch = text == null ? "" : text.trim();
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) {
            advPanel.clearAll();
        }
        setDefaultAttendanceDateRange();
        showSqlFilterTokens(List.of());
    }

   @Override
    protected void focusFirstField() {
	   reportedByEmployee.focus();
    }

    @Override
    protected EmployeeAttendanceDetail createNewEntity() throws Exception {
	    ensureEditorLookupDataLoaded();
	    	EmployeeAttendanceDetail entity = new EmployeeAttendanceDetail();


        if ( currentUserLogin.isPresent()) {
            String insuranceStr = currentUserLogin.get().getInsurance();

            if (insuranceStr != null && !insuranceStr.isBlank()) {
                try {
                    int insuranceNo = Integer.parseInt(insuranceStr);

                    employeeRepository.findByInsuranceNo(insuranceNo)
                        .ifPresent(emp -> {
                            entity.setReportedByEmployee(emp);
                            entity.setReportedByPosition(emp.getPositions());
                            entity.setReportLocation(emp.getBranch());
                            entity.setReportedDate(LocalDate.now()); // ✅ instead of Now()
                            
                            entity.setAttendanceDate(LocalDate.now());
							entity.setDataEntryBy(emp);
							entity.setDataEntryPosition(emp.getPositions());
							entity.setDataEntryLocation(emp.getBranch());
							entity.setDataEntryDate(LocalDate.now());
							entity.setEntryStatus(AttendanceEntryStatus.DRAFT);
                        });

                } catch (NumberFormatException ex) {
                    // optional: log or ignore if insurance is not numeric
                    System.out.println("Invalid insurance number: " + insuranceStr);
                }
            }
        }
        
        return entity;
    }

    @Override
    protected List<ColumnDef<EmployeeAttendanceDetail>> getColumnDefs() {
        return List.of(
            col("id", "ID",EmployeeAttendanceDetail::getId, e -> e.getId() != null ? e.getId().toString() : ""),
            
            col("employee.insuranceNo","Insurance",e -> e.getEmployee().getInsuranceNo(), e -> e.getEmployee().getInsuranceNo() != null ? e.getEmployee().getInsuranceNo().toString() : ""),
            col("employee.nameEn","Employee(EN)",e -> e.getEmployee().getNameEn(), e -> e.getEmployee().getNameEn() != null ? e.getEmployee().getNameEn().toString() : ""),
            col("employee.nameKh","Employee(KH)",e -> e.getEmployee().getNameKh(), e -> e.getEmployee().getNameKh() != null ? e.getEmployee().getNameKh().toString() : ""),
            
           // col("employee", employee.getLabel(),e->this.formatEmployeeLabel(e.getEmployee()), e->this.formatEmployeeLabel(e.getEmployee())),

            col("attendanceDate", attendanceDate.getLabel(),e -> e.getAttendanceDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getAttendanceDate()) : "", e -> e.getAttendanceDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getAttendanceDate()) : ""),
           
            col("reportLocation", reportLocation.getLabel(), e -> e.getReportLocation() != null ? e.getReportLocation().getBranchShortName() : "",e -> e.getReportLocation() != null ? e.getReportLocation().getBranchShortName() : ""),
            
            col("team", this.teamTeam.getLabel(),e -> formatTeams(e),this::formatTeams),
            col("tasks",this.teamTask.getLabel(),e -> formatTasks(e),this::formatTasks),
            
            col("leaveType.leaveTypeGroup", this.leaveTypeGroup.getLabel(), e -> formatLeaveTypeGroup(e.getLeaveType()),e -> formatLeaveTypeGroup(e.getLeaveType())),
            
            col("leaveType", "Attendance Status | ស្ថានភាពវត្តមាន",
                    e -> this.formatLeaveType(e.getLeaveType()),
                    e -> this.formatLeaveType(e.getLeaveType())),

            col("reviewStatus", "Review Status | ស្ថានភាពត្រួតពិនិត្យ",
                    this::formatReviewStatus,
                    this::formatReviewStatus),
            
            col("leaveTypeSubType", leaveTypeSubType.getLabel(), e->this.formatLeaveSubType(e.getLeaveTypeSubType()),e->this.formatLeaveSubType(e.getLeaveTypeSubType())),
            
           col("leaveDuration", leaveDuration.getLabel(),e->e.getLeaveDuration()!=null? e.getLeaveDuration().getLabel() :"", e->e.getLeaveDuration()!=null? e.getLeaveDuration().getLabel() :""),
           
           col("firstIn", firstIn.getLabel(),e->e.getFirstIn()!=null? e.getFirstIn().toString() :"", e->e.getFirstIn()!=null? e.getFirstIn().toString() :""),
           
           col("lastOut", lastOut.getLabel(),e->e.getLastOut()!=null? e.getLastOut() :"",e->e.getLastOut()!=null? e.getLastOut().toString() :""),
           
           col("breakMinutes", breakMinutes.getLabel(),e->e.getBreakMinutes()!=null? e.getBreakMinutes() :"",e->e.getBreakMinutes()!=null? e.getBreakMinutes().toString() :""),  
           
           col("totalHours", totalHours.getLabel(), e->e.getTotalHours()!=null? e.getTotalHours() :"",e->e.getTotalHours()!=null? e.getTotalHours().toString() :""), 
           
           col("normalHours", normalHours.getLabel(),e->e.getNormalHours()!=null? e.getNormalHours() :"",e->e.getNormalHours()!=null? e.getNormalHours().toString() :""),
           
           col("overtimeHours", overtimeHours.getLabel(),e->e.getOvertimeHours()!=null? e.getOvertimeHours() :"", e->e.getOvertimeHours()!=null? e.getOvertimeHours().toString() :""),
           
           col("reportedByEmployee", reportedByEmployee.getLabel(),e->this.formatAttendanceEmployeeLabel(e.getReportedByEmployee()),e->this.formatAttendanceEmployeeLabel(e.getReportedByEmployee())),

           col("reportedByPosition", reportedByPosition.getLabel(), e -> this.formatPosition(e.getReportedByPosition()) , e -> this.formatPosition(e.getReportedByPosition())),

           col("reportedDate", reportedDate.getLabel(),e -> e.getReportedDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getReportedDate()) : "", e -> e.getReportedDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getReportedDate()) : ""),

          
               
           col("dataEntryBy", dataEntryBy.getLabel(), e -> this.formatAttendanceEmployeeLabel(e.getDataEntryBy()), e -> this.formatAttendanceEmployeeLabel(e.getDataEntryBy())),

           col("dataEntryPosition", dataEntryPosition.getLabel(), e -> this.formatPosition(e.getDataEntryPosition()), e -> this.formatPosition(e.getDataEntryPosition())),

           col("dataEntryLocation", dataEntryLocation.getLabel(), e -> e.getDataEntryLocation() != null ? e.getDataEntryLocation().getBranchShortName() : "", e -> e.getDataEntryLocation() != null ? e.getDataEntryLocation().getBranchShortName() : ""),

           col("dataEntryDate", dataEntryDate.getLabel(), e -> e.getDataEntryDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getDataEntryDate()) : "", e -> e.getDataEntryDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getDataEntryDate()) : ""),

           col("entryStatus", entryStatus.getLabel(), e -> e.getEntryStatus() != null ? e.getEntryStatus().getLabel() : "", e -> e.getEntryStatus() != null ? e.getEntryStatus().getLabel() : ""),

           col("qcReturnReason", "QC Return Reason | មូលហេតុ QC បញ្ជូនត្រឡប់",
                   e -> e.getQcReturnReason() != null ? e.getQcReturnReason() : "",
                   e -> e.getQcReturnReason() != null ? e.getQcReturnReason() : ""),
            
           col("remark", remark.getLabel(),e -> e.getRemark() != null ? e.getRemark() : "", e -> e.getRemark() != null ? e.getRemark() : ""), 
            
           col("survey123Id", "Survey123 ID", e -> e.getSurvey123Id() != null ? e.getSurvey123Id().toString() : "", e -> e.getSurvey123Id() != null ? e.getSurvey123Id().toString() : ""),
            
           col("userCreated.name", "Created By",e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "" ),
           
           col("createdAt", "Created At",EmployeeAttendanceDetail::getCreatedAt,e -> e.getCreatedAt() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt())  : ""),

           col("userUpdated.name", "Updated By",e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

           col("updatedAt", "Updated At",EmployeeAttendanceDetail::getUpdatedAt, e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt())  : "")
        );
    }
    
    private String formatLeaveTypeGroup(LeaveType leaveType) {
        if (leaveType == null 
            || leaveType.getLeaveTypeGroup() == null 
            || leaveType.getLeaveTypeGroup().getLeaveTypeGroup() == null) {
            return "";
        }
        return leaveType.getLeaveTypeGroup().getLeaveTypeGroup().toString();
    }

    private String formatReviewStatus(EmployeeAttendanceDetail attendance) {
        return attendance != null && attendance.getQcByEmployee() != null
                ? "Reviewed | បានត្រួតពិនិត្យ"
                : "Pending | រង់ចាំត្រួតពិនិត្យ";
    }
    
    private String formatTasks(EmployeeAttendanceDetail detail) {
        if (detail == null || detail.getAttendanceDetailTeams() == null
                || detail.getAttendanceDetailTeams().isEmpty()) {
            return "";
        }

        return detail.getAttendanceDetailTeams().stream()
                .filter(Objects::nonNull)
                .flatMap(teamRow -> {
                    if (teamRow.getTeamTasks() == null) {
                        return Stream.<EmployeeAttendanceDetailTeamTask>empty();
                    }
                    return teamRow.getTeamTasks().stream();
                })
                .map(EmployeeAttendanceDetailTeamTask::getTask)
                .filter(Objects::nonNull)
                .map(Tasks::getTaskCode)      // 🔹 Task → code
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    
    private String formatTeams(EmployeeAttendanceDetail detail) {
        if (detail == null || detail.getAttendanceDetailTeams() == null
                || detail.getAttendanceDetailTeams().isEmpty()) {
            return "";
        }

        return detail.getAttendanceDetailTeams().stream()
                .map(EmployeeAttendanceDetailTeam::getTeam)
                .filter(Objects::nonNull)
                .map(Teams::getTeamCode)   // or another label if you prefer
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private String formatAttendanceEmployeeLabel(Employee e) {
        if (e == null) {
            return "";
        }

        String nameEn = Optional.ofNullable(e.getNameEn()).orElse("");
        String nameKh = Optional.ofNullable(e.getNameKh()).orElse("");
        String insNo  = Optional.ofNullable(e.getInsuranceNo())
                .map(Object::toString)
                .orElse("");

        String label = nameEn;
        if (!nameKh.isBlank()) {
            label += (label.isEmpty() ? "" : " | ") + nameKh;
        }
        if (!insNo.isBlank()) {
            label += (label.isEmpty() ? "" : " - ") + insNo;
        }

        return label;
    }

    private String formatLeaveType(LeaveType e) {
        if (e == null) {
            return "";
        }
        String nameEn = Optional.ofNullable(e.getLeaveNameEn()).orElse("");
        String nameKh = Optional.ofNullable(e.getLeaveNameKh()).orElse("");
        String label = nameEn;
        if (!nameKh.isBlank()) {
            label += (label.isEmpty() ? "" : " | ") + nameKh;
        }
        return label;
    }
    
    private String formatLeaveSubType(LeaveTypeSubType e) {
        if (e == null) {
            return "";
        }
        String nameEn = Optional.ofNullable(e.getLeaveSubTypeNameEn()).orElse("");
        String nameKh = Optional.ofNullable(e.getLeaveSubTypeNameKh()).orElse("");
        String label = nameEn;
        if (!nameKh.isBlank()) {
            label += (label.isEmpty() ? "" : " | ") + nameKh;
        }
        return label;
    }
    
    private String formatPosition(Positions pos) {
        if (pos == null) {
            return "";
        }

        String nameEn = Optional.ofNullable(pos.getPosition()).orElse("").trim();
        String nameKh = Optional.ofNullable(pos.getPositionKh()).orElse("").trim();

        if (nameKh.isEmpty()) {
            return nameEn;
        }
        if (nameEn.isEmpty()) {
            return nameKh;
        }
        return nameEn + " | " + nameKh;
    }

	// --- ComboBox Helper ---
	 private <T> void setupComboBox(ComboBox<T> comboBox, String placeholder, ValueProvider<T, String> labelGenerator) {
	     comboBox.setPlaceholder(placeholder);
	     comboBox.setItemLabelGenerator(labelGenerator::apply);
	     comboBox.setClearButtonVisible(true);
	     comboBox.setWidthFull();
	 }

	 // --- MultiSelectComboBox Helper ---
	 private <T> void setupMultiSelectComboBox(MultiSelectComboBox<T> comboBox, List<T> items, ValueProvider<T, String> labelGenerator) {
	     comboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
	     comboBox.setWidthFull();
	     comboBox.setClearButtonVisible(true);
	     comboBox.setItems(items);
	     comboBox.setItemLabelGenerator(labelGenerator::apply);
	     comboBox.setAutoExpand(AutoExpandMode.BOTH);
	 }
	 
	    private void configureAttendanceTeamDialog() {

	        this.attendanceDetailTeamDialog.add(new VerticalLayout(new FormLayout(this.teamTeam, this.teamTask)));

	        // Combo items & labels
	        //this.teamTeam.setItems(this.teamsRepository.findAll());
	        this.teamTeam.setItemLabelGenerator(Teams::getTeamCode);

	        //this.teamTask.setItems(this.tasksRepository.findAll());
	        this.teamTask.setItemLabelGenerator(Tasks::getTaskCode);
	        
	        // 🔹 BINDING for EmployeeAttendanceDetailTeam
	        detailTeamBinder.forField(teamTeam)
	            .asRequired("Team is required | សូមជ្រើសរើសក្រុម")
	            .bind(EmployeeAttendanceDetailTeam::getTeam,
	                  EmployeeAttendanceDetailTeam::setTeam);

	        // --- Save button in footer ---
	        Button btnSaveTeam = new Button("Save | រក្សាទុក");
	        btnSaveTeam.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

	        btnSaveTeam.addClickListener(e -> addTeamAssignmentToCurrentDetail());

	        this.attendanceDetailTeamDialog.getFooter().add(btnSaveTeam);
	    }
	    private void addTeamAssignmentToCurrentDetail() {
	        // 1️⃣ Make sure an attendance detail is selected
	        if (this.entity == null) {
	            Notification.show(
	                    "Please select an attendance detail first | សូមជ្រើសរើសវត្តមានជាមុនសិន",
	                    3000,
	                    Position.TOP_CENTER
	            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
	            return;
	        }

	        // 2️⃣ Create a new Team row if needed and attach it to currentDetail
	        if (currentTeam == null) {
	            currentTeam = new EmployeeAttendanceDetailTeam();
	            currentTeam.setAttendanceDetail(this.entity);

	            if (this.entity.getAttendanceDetailTeams() == null) {
	            	this.entity.setAttendanceDetailTeams(new ArrayList<>());
	            }
	            this.entity.getAttendanceDetailTeams().add(currentTeam);
	        }

	        // 3️⃣ Write ComboBox<Teams> value into currentTeam.setTeam(...)
	        try {
	            detailTeamBinder.writeBean(currentTeam);
	        } catch (ValidationException ex) {
	            Notification.show(
	                    "Please check Team | សូមពិនិត្យព័ត៌មានក្រុមម្ដងទៀត",
	                    3000,
	                    Position.TOP_CENTER
	            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
	            return;
	        }

	        // 4️⃣ Audit for Team row itself
	        User auditUser = currentUserLogin.orElse(null);
	        if (auditUser != null) {
	            // Only set created once
	            if (currentTeam.getUserCreated() == null) {
	                currentTeam.setUserCreated(auditUser);
	            }
	            // Always bump updated user
	            currentTeam.setUserUpdated(auditUser);
	        }

	        // 5️⃣ Selected Tasks from UI (MultiSelectComboBox<Tasks>)
	        Set<Tasks> selectedTasks = new java.util.HashSet<>(teamTask.getSelectedItems());

	        // Ensure the join list is not null
	        if (currentTeam.getTeamTasks() == null) {
	            currentTeam.setTeamTasks(new ArrayList<>());
	        }
	        List<EmployeeAttendanceDetailTeamTask> joinList = currentTeam.getTeamTasks();

	        // 6️⃣ Remove join rows that are no longer selected
	        joinList.removeIf(join -> {
	            Tasks task = join.getTask();
	            return task != null && !selectedTasks.contains(task);
	        });

	        // 7️⃣ Add join rows for newly selected Tasks
	        for (Tasks task : selectedTasks) {
	            boolean exists = joinList.stream()
	                    .anyMatch(j -> Objects.equals(j.getTask(), task));

	            if (!exists) {
	                EmployeeAttendanceDetailTeamTask join = new EmployeeAttendanceDetailTeamTask();
	                join.setAttendanceDetailTeam(currentTeam);
	                join.setTask(task);

	                if (auditUser != null) {
	                    join.setUserCreated(auditUser);
	                    join.setUserUpdated(auditUser);
	                }

	                joinList.add(join);
	            }
	        }

	        // 8️⃣ Update updatedBy for existing join rows
	        if (auditUser != null) {
	            for (EmployeeAttendanceDetailTeamTask join : joinList) {
	                // keep existing createdBy, just bump updatedBy
	                if (join.getUserCreated() == null) {
	                    join.setUserCreated(auditUser); // safety net
	                }
	                join.setUserUpdated(auditUser);
	            }
	        }

	        // 9️⃣ Refresh UI + reset dialog state
	        refreshTeamGridForCurrentDetail();
	        attendanceDetailTeamDialog.close();

	        teamTeam.clear();
	        teamTask.clear();
	        currentTeam = null;
	    }
	    
	    private void refreshTeamGridForCurrentDetail() {
	        if (entity == null || entity.getAttendanceDetailTeams() == null) {
	            gridAttendanceDetailTeam.setItems(Collections.emptyList());
	        } else {
	            gridAttendanceDetailTeam.setItems(entity.getAttendanceDetailTeams());
	        }
	    }
	    
		    private void openTeamEditDialog(EmployeeAttendanceDetailTeam team) {
	        if (this.entity == null || team == null) {
	            return;
	        }

	        this.currentTeam = team;  // mark row being edited

		        ensureEditorLookupDataLoaded();

	        // 🔹 Binder for teamTeam
	        detailTeamBinder.setBean(currentTeam);

	        // 🔹 MANUALLY set selected tasks in MultiSelect
	        if (currentTeam.getTeamTasks() != null) {
	            Set<Tasks> selected = currentTeam.getTeamTasks().stream()
	                    .map(EmployeeAttendanceDetailTeamTask::getTask)
	                    .filter(Objects::nonNull)
	                    .collect(Collectors.toSet());
	            teamTask.setValue(selected);
	        } else {
	            teamTask.clear();
	        }

	        attendanceDetailTeamDialog.open();
	    }
	    
	    private void removeTeamFromCurrentDetail(EmployeeAttendanceDetailTeam team) {
	        if (this.entity == null || team == null) {
	            return;
	        }

	        // Clear tasks to mark them as orphans
	        if (team.getTeamTasks() != null) {
	            team.getTeamTasks().clear();
	        }

	        // Break back-reference if your entity has it
	        team.setAttendanceDetail(null); // <-- if you have this field

	        // Remove from the parent collection
	        if (this.entity.getAttendanceDetailTeams() != null) {
	        	this.entity.getAttendanceDetailTeams().remove(team);
	        }

	        gridAttendanceDetailTeam.getDataProvider().refreshAll();
	    }
	    
	    private Button buildSupervisorReviewButton() {
	        Button btn = new Button(
	                "Supervisor Review | ការត្រួតពិនិត្យ",
	                VaadinIcon.CHECK_CIRCLE.create());
	        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

	        btn.addClickListener(e -> {
	            // ✅ targets = selected rows
	            List<EmployeeAttendanceDetail> targets = new ArrayList<>(grid.getSelectedItems());
	            if (targets.isEmpty()) {
	                Notification.show("Please select at least 1 row | សូមជ្រើសយ៉ាងហោចណាស់ ១ ជួរ",
	                        2500, Notification.Position.TOP_CENTER)
	                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
	                return;
	            }

	            Optional<Employee> supOpt = findCurrentEmployee();
	            if (supOpt.isEmpty()) {
	                Notification.show("Cannot detect current employee (insurance no) | មិនអាចរកឃើញបុគ្គលិកបច្ចុប្បន្ន",
	                        3000, Notification.Position.TOP_CENTER)
	                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
	                return;
	            }

	            Employee supervisor = supOpt.get();

	            ConfirmDialog dlg = new ConfirmDialog();
	            dlg.setHeader("Confirm Supervisor Review | បញ្ជាក់ការត្រួតពិនិត្យ");
	            dlg.setText("Apply supervisor review to " + targets.size() + " row(s)?");
	            dlg.setConfirmText("Apply");
	            dlg.setConfirmButtonTheme("primary success");
	            dlg.setCancelText("Cancel");
	            dlg.setCancelable(true);

	            dlg.addConfirmListener(ev -> {
	                try {
	                    for (EmployeeAttendanceDetail r : targets) {
	                        // ✅ set Supervisor fields (using QC fields)
	                        r.setQcByEmployee(supervisor);
	                        r.setQcByPosition(supervisor.getPositions());
	                        r.setQcLocation(supervisor.getBranch());
	                        r.setQcDate(LocalDate.now());

	                        // audit
	                        currentUserLogin.ifPresent(u -> {
	                            r.setUserUpdated(u);
	                            if (r.getUserCreated() == null) r.setUserCreated(u);
	                        });

	                        service.update(r);
	                    }

	                    refreshGrid();
	                    Notification.show("Supervisor review applied ✅", 2000, Notification.Position.TOP_CENTER)
	                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

	                } catch (Exception ex) {
	                    Notification.show("Failed: " + ex.getMessage(), 3500, Notification.Position.TOP_CENTER)
	                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
	                    ex.printStackTrace();
	                }
	            });

	            dlg.open();
	        });

	        return btn;
	    }
	    private Optional<Employee> findCurrentEmployee() {
	        return currentUserLogin.flatMap(u -> {
	            String insuranceStr = u.getInsurance();
	            if (insuranceStr == null || insuranceStr.isBlank()) return Optional.empty();
	            try {
	                int insuranceNo = Integer.parseInt(insuranceStr.trim());
	                return employeeRepository.findByInsuranceNo(insuranceNo);
	            } catch (NumberFormatException ex) {
	                return Optional.empty();
	            }
	        });
	    }


}