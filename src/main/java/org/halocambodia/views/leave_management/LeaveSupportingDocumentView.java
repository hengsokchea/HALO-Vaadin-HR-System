package org.halocambodia.views.leave_management;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.IFrame;
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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.halocambodia.component.SignaturePad;
import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeLeaveRequestService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.MasterPageDialogLayout;
import org.halocambodia.views.PreviewReport;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route(value = "leave-supporting-documents", layout = MainLayout.class)
@PageTitle("Leave Summary")
@PermitAll
@Uses(Icon.class)
public class LeaveSupportingDocumentView extends MasterPageDialogLayout<EmployeeLeave, EmployeeLeaveRequestService> implements BeforeEnterObserver {
	private final PasswordEncoder passwordEncoder;

    // Form Fields for Leave Request
    private DatePicker requestDate = new DatePicker("Requested Date | ថ្ងៃស្នើសុំច្បាប់");
    private ComboBox<Employee> employee = new ComboBox<>("Requested By | ឈ្មោះអ្នកស្នើសុំច្បាប់");
    private ComboBox<Department> department = new ComboBox<>("Department | ផ្នែក");
    private ComboBox<Positions> positions = new ComboBox<>("Position | តួនាទី");
    private TextArea contractNumber = new TextArea("Contact Number | លេខទូរស័ព្ទ");
    private ComboBox<Employee> lineManager = new ComboBox<>("Line Manager | ប្រធានគ្រប់គ្រង",e->changeLineManager());
    private ComboBox<Positions> lineManagerPositions = new ComboBox<>("Line Manager Position |តួនាទីប្រធានគ្រប់គ្រង");
    private TextArea reason = new TextArea("Reason | មូលហេតុ");
    private ComboBox<LeaveStatus> leaveStatus = new ComboBox<>("Leave Status | ស្ថានភាព");
    

    
 // Leave summary fields
    private BigDecimalField annualLeaveAvailableBalance = new BigDecimalField("Annual Leave Available Balance | ច្បាប់សម្រាកប្រចាំឆ្នាំអាចប្រើប្រាស់បាន");
    private BigDecimalField sickLeaveUsed = new BigDecimalField("Sick Leave Used | ច្បាប់សម្រាកឈឺបានប្រើ");
    private BigDecimalField specialLeaveUsed = new BigDecimalField("Special Leave Used | ច្បាប់សម្រាកពិសេសបានប្រើ");
    private BigDecimalField unpaidLeaveUsed = new BigDecimalField("Unpaid Leave Used | ច្បាប់សម្រាកមិនយកប្រាក់");
    private BigDecimalField paternityLeaveUsed = new BigDecimalField("Paternity Leave Used | ច្បាប់សម្រាកបិតុភាព");
    private BigDecimalField maternityLeaveUsed = new BigDecimalField("Maternity Leave Used | ច្បាប់សម្រាកលំហែមាតុភាព");
    private BigDecimalField compensatoryLeaveRemaining = new BigDecimalField("Compensatory Leave Remining| ច្បាប់សម្រាកសងនៅសល់");
    
    //Line Manager check
    private DatePicker lineManagerCheckDate = new DatePicker("Check Date | ថ្ងៃត្រួតពិនិត្យ"); 
    private ComboBox<Employee> lineManagerChecked = new ComboBox<>("Checked By | ត្រួតពិនិត្យដោយ",e->changeLineManager());
    private ComboBox<Positions> lineManagerCheckedPositions = new ComboBox<>("Position | តួនាទី");
    private TextArea lineManagerCheckComments = new TextArea("Comments | មតិយោបល់");
    private ComboBox<LeaveStatus> lineManagerCheckedStatus = new ComboBox<>("Line Manager's Leave Status | ស្ថានភាពរបស់ប្រធានគ្រប់គ្រង");
    
    //HR check
    private DatePicker hrVerificationDate = new DatePicker("Verification Date | ថ្ងៃត្រួតពិនិត្យ"); 
    private ComboBox<Employee> hrVerificationBy = new ComboBox<>("Verified By | បានត្រួតពិនិត្យដោយ",e->changeLineManager());
    private ComboBox<Positions> hrVerificationPosition = new ComboBox<>("Position | តួនាទី");
    private TextArea hrVerificationComments = new TextArea("Comments | មតិយោបល់");
    private ComboBox<LeaveStatus> hrVerificationStatus = new ComboBox<>("Verification| ការត្រួតពិនិត្យ");
    
    
    // Form Fields for Leave Request
    private ComboBox<LeaveType> leaveType=new ComboBox<LeaveType>("Leave Type | ប្រភេទច្បាប់");
    private ComboBox<LeaveTypeSubType> leaveTypeSubType=new ComboBox<LeaveTypeSubType>("Sub Special Leave | ប្រភេទច្បាប់ឈប់សម្រាកពិសេស");
    private DatePicker fromDate =new DatePicker("From Date | ស្នើសុំច្បាប់ពីថ្ងៃទី");
    private DatePicker toDate =new DatePicker("To Date | សុំច្បាប់ដល់ថ្ងៃទី");
    private ComboBox<EmployeeLeaveDetail.LeaveDuration> leaveDuration = new ComboBox<>("Leave Duration | រយៈពេលច្បាប់");
    private NumberField numberOfDay = new NumberField("Number of Days | ចំនួនថ្ងៃច្បាប់");
    private TextArea remark =new TextArea("Remarks | កំណត់សម្គាល់");
    CustomDialog leaveRequstDetailDialog = new CustomDialog("Leave Request Information | ព័ត៌មានថ្ងៃឈប់សម្រាក");
    private Grid<EmployeeLeaveDetail> gridLeaveDetail = new Grid<>(EmployeeLeaveDetail.class);
    
 // Binder for the leave request detail dialog
    private final BeanValidationBinder<EmployeeLeaveDetail> leaveDetailBinder =  new BeanValidationBinder<>(EmployeeLeaveDetail.class);
    
 // Holds the details shown in gridLeaveDetail
    private final List<EmployeeLeaveDetail> leaveDetails = new ArrayList<>();
    
    private EmployeeLeaveDetail currentEditingDetail = null;



    private  Path uploadDirReportEmbed;
    private  Path uploadDir;

    private static final String attachmentsSubDirectory="attachments";
    private final FileUploadUtility fileUploadUtility ;
    private static final String reportEmbed="report_embed";
    private Path baseAttachmentsDir;

    
    SignaturePad requesterSignature = new SignaturePad();
    VerticalLayout requesterSignaturerLayout = new VerticalLayout(new Span("🖋 Requester Signature | ហត្ថលេខាអ្នកស្នើរ"), requesterSignature, new HorizontalLayout(createClearButton(requesterSignature),createImageUploadComponent(requesterSignature) ));
    
    SignaturePad supervisorSignature = new SignaturePad();
    VerticalLayout supervisorSignatureLayout = new VerticalLayout(new Span("🖋 Supervisor Signature | ហត្ថលេខាអ្នកគ្រប់គ្រង"), supervisorSignature, new HorizontalLayout(createClearButton(supervisorSignature),createImageUploadComponent(supervisorSignature) ));

    SignaturePad hrSignature = new SignaturePad();
    VerticalLayout hrSignatureLayout = new VerticalLayout(new Span("🖋 HR Signature | ហត្ថលេខានាយកដ្ឋានធនធានមនុស្ស"), hrSignature, new HorizontalLayout(createClearButton(hrSignature),createImageUploadComponent(hrSignature) ));

    


    // Advance Filter Components
    private NumberField advanceFilterID = new NumberField("ID");
    private DatePicker advanceFilterRequestDateFrom = new DatePicker("Request Date From");
    private DatePicker advanceFilterRequestDateTo = new DatePicker("Request Date To");
    private MultiSelectComboBox<Employee> advanceFilterEmployee = new MultiSelectComboBox<>("Employee");
    private MultiSelectComboBox<Department> advanceFilterDepartment = new MultiSelectComboBox<>("Department");
    
    private MultiSelectComboBox<Positions> advanceFilterPosition = new MultiSelectComboBox<>("Positions");
    
    private MultiSelectComboBox<Employee> advanceFilterLineManager = new MultiSelectComboBox<>("Line Manager");
    private MultiSelectComboBox<Positions> advanceFilterLineManagerPosition = new MultiSelectComboBox<>("Line Manager Position");
    
    
    
    private MultiSelectComboBox<LeaveStatus> advanceFilterLeaveStatus = new MultiSelectComboBox<>("Leave Status");
    private TextField advanceFilterContactNumber = new TextField("Contact Number");
    
    
    private MultiSelectComboBox<Employee> advanceFilterlineManagerChecked = new MultiSelectComboBox<>("Line Manager Checked");
    private MultiSelectComboBox<Positions> advanceFilterlineManagerCheckedPositions = new MultiSelectComboBox<>("Line Manager Checked Position");
    private DatePicker advanceFilterlineManagerCheckDateFrom=new DatePicker("line Manager Checked Date(From)");
    private DatePicker advanceFilterlineManagerCheckDateTo=new DatePicker("line Manager Checked Date (To)");
    private MultiSelectComboBox<LeaveStatus> advanceFilterlineManagerCheckedStatus = new MultiSelectComboBox<>("line Manager Checked Status");
    
    private MultiSelectComboBox<Employee> advanceFilterHrVerificationBy = new MultiSelectComboBox<>("HR Verified By");
    private MultiSelectComboBox<Positions> advanceFilterHrVerificationPosition = new MultiSelectComboBox<>("HR Verified  Position");
    private DatePicker advanceFilterHrVerificationDateFrom=new DatePicker("HR Verification Date(From)");
    private DatePicker advanceFilterHrVerificationDateTo=new DatePicker("HR Verification Date(To)");
    private MultiSelectComboBox<LeaveStatus> advanceFilterHrVerificationStatus = new MultiSelectComboBox<>("HR Verified Status");
    
    
    
    

    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");

    private final Optional<User> currentUserLogin;
    private final BeanValidationBinder<EmployeeLeave> binder = new BeanValidationBinder<>(EmployeeLeave.class);

    // Repositories
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final LeaveStatusRepository leaveStatusRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AttachmentRepository attachmentRepository;
    private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;
    private final AttachmentTypeRepository attachmentTypeRepository;
    

    public LeaveSupportingDocumentView(EmployeeLeaveRequestService service, UserService userService, 
                          AuthenticatedUser authenticatedUser, EmployeeRepository employeeRepository,
                          DepartmentRepository departmentRepository, PositionRepository positionRepository,
                          LeaveStatusRepository leaveStatusRepository,LeaveTypeRepository leaveTypeRepository,FileUploadUtility fileUploadUtility,AttachmentRepository attachmentRepository,EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository,AttachmentTypeRepository attachmentTypeRepository,PasswordEncoder passwordEncoder) {
        super(service, userService, authenticatedUser);
        this.passwordEncoder = passwordEncoder;
        this.currentUserLogin = authenticatedUser.get();
        
	 	this.fileUploadUtility=fileUploadUtility;
	 	 uploadDir=fileUploadUtility.initializeUploadDirectory("hr", "");
	 	 
	 	uploadDirReportEmbed=fileUploadUtility.initializeUploadDirectory("hr", this.reportEmbed);
		 baseAttachmentsDir=fileUploadUtility.initializeUploadDirectory("hr", attachmentsSubDirectory);
        
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.leaveStatusRepository = leaveStatusRepository;
        this.leaveTypeRepository=leaveTypeRepository;
        this.attachmentRepository=attachmentRepository;
        this.employeeLeaveBalanceRepository=employeeLeaveBalanceRepository;
        this.attachmentTypeRepository=attachmentTypeRepository;
        
        MenuBar menuBar = new MenuBar();
        

        Icon printIcon = VaadinIcon.PRINT.create();
        Span label = new Span("Print");

        HorizontalLayout content = new HorizontalLayout(printIcon, label);
        content.setSpacing(true);
        content.setPadding(false);

        MenuItem menuItem = menuBar.addItem(content);  // can be contextmenu.MenuItem or MenuBar.MenuItem
        menuItem.addClickListener(e -> {
            if (grid.getSelectedItems().isEmpty()) {
                Notification.show("Please select grid before print",
                        5000, Notification.Position.TOP_CENTER);
                return;
            }

            // Collect all selected IDs
            java.util.Set<Long> ids = grid.getSelectedItems().stream()
                    .map(EmployeeLeave::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            if (ids.isEmpty()) {
                Notification.show("Selected rows have no IDs.",5000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            generateReportForm(ids); 
        });

        this.leftToolbar.add(menuBar);



    }
    private void generateReportForm(Collection<Long> empLeaveIds) {
        if (empLeaveIds == null || empLeaveIds.isEmpty()) {
            Notification.show("Employee leave ID cannot be null or empty.",  9000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("emp_leave_id", empLeaveIds); // ✅ Collection<Long>
        parameters.put("SUBREPORT_DIR", "report_embed");
        parameters.put("ATTACHMENT_DIR", baseAttachmentsDir.toString());
        parameters.put("currentUserLogin", this.currentUserLogin.get().getName());

        PreviewReport previewReport =  new PreviewReport("report_embed/leave_request_form.jasper", parameters);
        previewReport.open();
    }
    private void changeLineManager() {
        Optional.ofNullable(lineManager.getValue())
                .map(Employee::getPositions)
                .ifPresentOrElse(
                    lineManagerPositions::setValue,
                    lineManagerPositions::clear
                );
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
                    
                    configureLeaveRequestDetailDialog();
                    gridLeaveDetail.setItems(leaveDetails);


                } catch (Exception e) {
                    showError("Error during UI setup: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception ex) {
            showError("Error initializing view: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadDataToForm() {
        this.employee.setItems(employeeRepository.findActiveLocalStaff());
        this.department.setItems(this.departmentRepository.findAll());
        this.positions.setItems(this.positionRepository.findAll());
        this.lineManager.setItems(employeeRepository.findAll());
        this.lineManagerPositions.setItems(this.positionRepository.findAll());
        this.leaveStatus.setItems(this.leaveStatusRepository.findAll().stream() .filter(ls -> ls.getId() == 1 || ls.getId() == 4 ).toList());
        
        this.lineManagerChecked.setItems(employeeRepository.findActiveLocalAndInternationalStaff());
        this.lineManagerCheckedPositions.setItems(this.positionRepository.findAll());
        this.lineManagerCheckedStatus.setItems(this.leaveStatusRepository.findAll().stream() .filter(ls -> ls.getId() ==8  || ls.getId() == 2 || ls.getId() == 3).toList());
        
        this.hrVerificationBy.setItems(employeeRepository.findActiveLocalAndInternationalStaff());
        this.hrVerificationPosition.setItems(this.positionRepository.findAll());
        this.hrVerificationStatus.setItems(this.leaveStatusRepository.findAll().stream() .filter(ls -> ls.getId() ==5  || ls.getId() == 6 || ls.getId() == 7L || ls.getId() == 9).toList());
       
    }

    private void binderField() {
        binder.bindInstanceFields(this);
        
        // Line Manager fields
        binder.forField(lineManagerCheckDate)
            .asRequired("Line Manager Check Date is required")
            .bind(EmployeeLeave::getLineManagerCheckDate, EmployeeLeave::setLineManagerCheckDate);
        
        binder.forField(lineManagerChecked)
            .asRequired("Line Manager Check is required")
            .bind(EmployeeLeave::getLineManagerChecked, EmployeeLeave::setLineManagerChecked);
        
        binder.forField(lineManagerCheckedPositions)
            .asRequired("Line Manager Position is required")
            .bind(EmployeeLeave::getLineManagerCheckedPositions, EmployeeLeave::setLineManagerCheckedPositions);
        
        binder.forField(lineManagerCheckedStatus)
        .asRequired("Line Manager Check Status is required")
        .bind(EmployeeLeave::getLineManagerCheckedStatus, EmployeeLeave::setLineManagerCheckedStatus);
        
        // HR fields
        binder.forField(hrVerificationDate)
            .asRequired("HR Verification Date is required")
            .bind(EmployeeLeave::getHrVerificationDate, EmployeeLeave::setHrVerificationDate);
        
        binder.forField(hrVerificationBy)
            .asRequired("HR Verification By is required")
            .bind(EmployeeLeave::getHrVerificationBy, EmployeeLeave::setHrVerificationBy);
        
        binder.forField(hrVerificationPosition)
            .asRequired("HR Verification Position is required")
            .bind(EmployeeLeave::getHrVerificationPosition, EmployeeLeave::setHrVerificationPosition);
        
        binder.forField(hrVerificationStatus)
            .asRequired("HR Verification Status is required")
            .bind(EmployeeLeave::getHrVerificationStatus, EmployeeLeave::setHrVerificationStatus);
    }

    @Override
    protected void configureGrid() {
        getColumnDefinitions().forEach(def -> {
            grid.addColumn(def.textFormatter()::apply)
                .setHeader(def.header())
                .setKey(def.key())
                .setSortProperty(def.key())
                .setSortable(true)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setResizable(true)
                //.setAutoWidth(!"ID".equals(def.header()))
                .setAutoWidth(true)
                .setComparator((a, b) -> {
                    Object va = def.dataProvider().apply(a);
                    Object vb = def.dataProvider().apply(b);
                    if (va == null && vb == null) return 0;
                    if (va == null) return -1;
                    if (vb == null) return 1;
                    if (va instanceof Comparable<?> && vb instanceof Comparable<?>)
                        return ((Comparable) va).compareTo(vb);
                    return va.toString().compareTo(vb.toString());
                });
        });

        List<String> columnsToFreeze = List.of("employee.insuranceNo", "employee.nameEn");
        
        for (String key : columnsToFreeze) {
            Grid.Column<EmployeeLeave> column = grid.getColumnByKey(key);
            if (column != null) {
                column.setFrozen(true);
            }
        }
        
        grid.setItemDetailsRenderer(this.createTabRenderer());
        createShowHideColumnGridToolBar();
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        var col = grid.getColumnByKey("leaveDetailSummary");
        if (col != null) {
            col.setSortable(false);
           
        }
    }
    
    private ComponentRenderer<Component, EmployeeLeave> createTabRenderer() {
        return new ComponentRenderer<>(entityRecord -> {
            TabSheet tabSheet = new TabSheet();
            tabSheet.add("Leave Detail | ព័ត៌មានលម្អិតអំពីច្បាប់", this.LeaveDetailTab(entityRecord));
            tabSheet.add("Supporting Documents | ឯកសារគាំទ្រ", this.attachmentsTab(entityRecord));

            tabSheet.setSizeFull();

            // ✅ Wrapping container ensures full expansion
            VerticalLayout wrapper = new VerticalLayout(tabSheet);
            wrapper.setPadding(false);
            wrapper.setSpacing(false);
            wrapper.setMargin(false);
            wrapper.setSizeFull();
            wrapper.setHeight("600px"); // or use "100%" if inside a full-height dialog
            wrapper.setFlexGrow(1, tabSheet);

            return wrapper;
        });
    }

	private Component LeaveDetailTab(EmployeeLeave entityRecord) {
    	Grid<EmployeeLeaveDetail> gridDetailTab =new Grid <>(EmployeeLeaveDetail.class, false);
    	gridDetailTab.addColumn(EmployeeLeaveDetail::getId)
        .setHeader("ID")
        .setFooter("Total Records:")
        .setKey("id");
    	
    	gridDetailTab.addColumn(entitySelected -> {
	        return entitySelected.getLeaveType() != null ? entitySelected.getLeaveType().getLeaveNameEn() + " " + entitySelected.getLeaveType().getLeaveNameKh() : "";
	    })
        .setHeader(this.leaveType.getLabel())
        .setKey("leaveType");
    	
    	gridDetailTab.addColumn(entitySelected -> {
	        return entitySelected.getLeaveTypeSubType() != null ? entitySelected.getLeaveTypeSubType().getLeaveSubTypeNameEn() + " " + entitySelected.getLeaveTypeSubType().getLeaveSubTypeNameKh() : "";
	    }).setHeader(this.leaveTypeSubType.getLabel()).setKey("leaveTypeSubType");
    	
    	gridDetailTab.addColumn(entitySelected -> {
	        return entitySelected.getFromDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(entitySelected.getFromDate()) : "";
	    })
        .setHeader(this.fromDate.getLabel())
        .setKey("fromDate");
    	
    	gridDetailTab.addColumn(entitySelected -> {
	        return entitySelected.getToDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(entitySelected.getToDate()) : "";
	    })
        .setHeader(this.toDate.getLabel())
        .setKey("toDate");
    	
    	gridDetailTab.addColumn(entitySelected -> {
	        return entitySelected.getLeaveDuration() != null ? entitySelected.getLeaveDuration().getLabel() : "";
	    })
        .setHeader(this.leaveDuration.getLabel())
        .setKey("leaveDuration");
    	
    	gridDetailTab.addColumn(EmployeeLeaveDetail::getNumberOfDay)
        .setHeader(this.numberOfDay.getLabel())
        .setKey("numberOfDay");
    	
    	gridDetailTab.addColumn(EmployeeLeaveDetail::getRemark)
        .setHeader(this.remark.getLabel())
        .setKey("remark");

    	
    	
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
    	

    	gridDetailTab.setItems(entityRecord.getEmployeeLeaveDetails());
    	gridDetailTab.getColumnByKey("id").setFooter("Total Records: " + entityRecord.getEmployeeLeaveDetails().size());
    	
    	gridDetailTab.getColumns().forEach(column -> {
            column.setResizable(true);   // Enable resizing for all columns
            column.setSortable(true);    // Enable sorting for all columns
            column.setTextAlign(ColumnTextAlign.CENTER);
            if(column.getHeaderText() !="ID") {
            	 column.setAutoWidth(true);
            }
           
        });
    	gridDetailTab.setSizeFull();

    	return gridDetailTab;
	}

	public Component attachmentsTab(EmployeeLeave entity) {
	    if (entity == null) {
	   //     return new Span("No attachments available.");
	    }

	    List<Attachment> attachments = attachmentRepository.findByEntity(entity);
	    entity.setAttachments(attachments);

	    if (attachments == null || attachments.isEmpty()) {
	    //    return new Span("No attachments found for this record.");
	    }

	    Grid<Attachment> gridAttachment = new Grid<>(Attachment.class, false);
	    gridAttachment.setItems(attachments);

	    gridAttachment.addColumn(Attachment::getId)
        .setHeader("ID")
        .setFooter("Total Records:")
        .setKey("id");

	    
        gridAttachment.addColumn(new ComponentRenderer<Anchor, Attachment>(entitySelected -> {
        	File attachmentFile = uploadDir.resolve(attachmentsSubDirectory + "/" + entitySelected.getFileName()).toFile();
        	Anchor download;
        	if (attachmentFile.exists()) {
        	    download = new Anchor(DownloadHandler.forFile(attachmentFile).inline(), AttachmentType.INLINE, entitySelected.getFileName());
        	    download.setTarget("_blank");
        	} else {
        	    download = new Anchor("#", "File not found");
        	    download.getElement().getStyle().set("color", "red");
        	    System.err.println("Missing file: " + attachmentFile.getAbsolutePath());
        	}
        	return download;


        })).setHeader("File Name")
          .setSortProperty("fileName")
          .setKey("fileName");
      	gridAttachment.addColumn(entitySelected -> {
	        return entitySelected.getAttachmentType() != null ? entitySelected.getAttachmentType().getAttachmentTypeName() : "";
	    }).setHeader("Attachment Type")
	      .setSortProperty("attachmentType.attachmentTypeName")
	      .setKey("attachmentType.attachmentTypeName");
    	
    	gridAttachment.addColumn(entitySelected -> {
	        return entitySelected.getDataSize() != null ? FileSizeFormatter.formatBytes(entitySelected.getDataSize()) : "";
	    }).setHeader("File Size")
	      .setSortProperty("dataSize")
	      .setKey("dataSize");
    	    	

        gridAttachment.addColumn(new ComponentRenderer<Anchor, Attachment>(entitySelected -> {
            
        	File attachmentFile = uploadDir.resolve(attachmentsSubDirectory + "/" + entitySelected.getFileName()).toFile();
        	Anchor download;
        	if (attachmentFile.exists()) {
        	    download = new Anchor(DownloadHandler.forFile(attachmentFile), "Download");

        	} else {
        	    download = new Anchor("#", "File not found");
        	    download.getElement().getStyle().set("color", "red");
        	    System.err.println("Missing file: " + attachmentFile.getAbsolutePath());
        	}
        	return download;
        	
        })).setHeader("Download") // Header for the new column
          .setKey("downloadButton") // Unique key for the column
          .setTextAlign(ColumnTextAlign.CENTER); // Center the button in the column

    	gridAttachment.addColumn(entitySelected -> {
	        return entitySelected.getUserCreated() != null ? entitySelected.getUserCreated().getName() : "";
	    }).setHeader("Created by")
	      .setSortProperty("userCreated.name")
	      .setKey("userCreated.name");

    	gridAttachment.addColumn(entitySelected -> {
	        return entitySelected.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getCreatedAt()) : "";
	    }).setHeader("Created At")
	      .setSortProperty("createdAt")
	      .setKey("createdAt");

    	gridAttachment.addColumn(entitySelected -> {
	        return entitySelected.getUserUpdated() != null ? entitySelected.getUserUpdated().getName() : "";
	    }).setHeader("Updated by")
	      .setSortProperty("userUpdated.name")
	      .setKey("userUpdated.name");

    	gridAttachment.addColumn(entitySelected -> {
	        return entitySelected.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getUpdatedAt()) : "";
	    }).setHeader("Updated At")
	      .setSortProperty("updatedAt")
	      .setKey("updatedAt");

    	gridAttachment.getColumnByKey("id").setFooter("Total Records: " + attachments.size());
    	
    	gridAttachment.getColumns().forEach(column -> {
	            column.setResizable(true);   // Enable resizing for all columns
	            column.setSortable(true);    // Enable sorting for all columns
	            column.setTextAlign(ColumnTextAlign.CENTER);
	            if(column.getHeaderText() !="ID") {
	            	 column.setAutoWidth(true);
	            }
	           
	        });


	    gridAttachment.setSizeFull();
	    //gridAttachment.setAllRowsVisible(true);

	 // 📎 Add Attachment Button
	    Button btnAddAttach = new Button("Add Document | បន្ថែមឯកសារ", VaadinIcon.PLUS.create());
	    btnAddAttach.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_SUCCESS);
	    btnAddAttach.addClickListener(e -> openAttachmentDialog(entity));

	    VerticalLayout layout = new VerticalLayout(btnAddAttach, gridAttachment);
	    layout.setPadding(false);
	    layout.setSpacing(false);
	    layout.setMargin(false);
	    layout.setSizeFull();

	    // ✅ Let the grid expand
	    layout.setFlexGrow(1, gridAttachment);
	    gridAttachment.setSizeFull();
	    gridAttachment.getStyle().set("flex", "1 1 auto");

	    return layout;


	}
	
	private void openAttachmentDialog(EmployeeLeave entity) {
	    CustomDialog dialog = new CustomDialog("Upload Supporting Documents");

	    // === Attachment Type ===
	    ComboBox<org.halocambodia.data.AttachmentType> attachmentTypeCombo = new ComboBox<>("Attachment Type");
	    attachmentTypeCombo.setItems(
	            attachmentTypeRepository.findAllById(List.of(4L, 5L, 6L, 7L, 18L)));
	    attachmentTypeCombo.setItemLabelGenerator(org.halocambodia.data.AttachmentType::getAttachmentTypeName);
	    attachmentTypeCombo.setWidthFull();

	    // === Multi-file Upload (Disk-based) ===
	    com.vaadin.flow.component.upload.receivers.MultiFileBuffer buffer =
	            new com.vaadin.flow.component.upload.receivers.MultiFileBuffer();
	    Upload upload = new Upload(buffer);
	    upload.setAcceptedFileTypes("image/*", "video/*", "audio/*", "application/pdf");
	    upload.setDropAllowed(true);
	    upload.setAutoUpload(true);
	    upload.setMaxFiles(100);
	    upload.setWidthFull();

	    List<Attachment> attachmentsToSave = new ArrayList<>();

	    // === Preview Container ===
	    VerticalLayout previewLayout = new VerticalLayout();
	    previewLayout.setPadding(false);
	    previewLayout.setSpacing(false);

	    // === Upload Listeners ===
	    upload.addSucceededListener(event -> {
	        try (InputStream input = buffer.getInputStream(event.getFileName());
	             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

	            input.transferTo(output);
	            byte[] bytes = output.toByteArray();

	            Attachment attachment = new Attachment();
	            attachment.setFileName(event.getFileName());
	            attachment.setContentType(event.getMIMEType());
	            attachment.setFileData(bytes);
	            attachment.setDataSize((long) bytes.length);
	            attachmentsToSave.add(attachment);

	            // Build preview with lightbox
	            Component preview = buildFilePreviewWithLightbox(
	                    event.getFileName(), event.getMIMEType(), bytes, attachmentsToSave, attachment);
	            previewLayout.add(preview);

	            Notification.show(event.getFileName() + " uploaded.",
	                    1500, Notification.Position.BOTTOM_END)
	                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
	        } catch (IOException ex) {
	            Notification.show("Error reading " + event.getFileName() + ": " + ex.getMessage(),
	                    2500, Notification.Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
	        }
	    });

	    upload.addFileRejectedListener(e ->
	            Notification.show(e.getErrorMessage(), 2500, Notification.Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_ERROR));

	    upload.addFailedListener(e ->
	            Notification.show("Failed to upload " + e.getFileName(),
	                    2000, Notification.Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_ERROR));

	    // === Binder ===
	    BeanValidationBinder<Attachment> binder = new BeanValidationBinder<>(Attachment.class);
	    Attachment meta = new Attachment();
	    binder.forField(attachmentTypeCombo)
	            .asRequired("Attachment type is required")
	            .bind(Attachment::getAttachmentType, Attachment::setAttachmentType);

	    FormLayout form = new FormLayout(attachmentTypeCombo, upload, previewLayout);
	    form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
	    dialog.add(new VerticalLayout(form) );

	    // === Buttons ===
	    Button btnSave = new Button("Save", VaadinIcon.CHECK.create(), e -> {
	        try {
	            binder.writeBean(meta);
	            if (attachmentsToSave.isEmpty()) {
	                Notification.show("Please upload at least one file.",
	                        2200, Notification.Position.TOP_CENTER)
	                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
	                return;
	            }

	            for (Attachment a : attachmentsToSave) {
	                a.setAttachmentType(meta.getAttachmentType());
	                a.setAttachmentEntityTable(entity);
	                a.setUserCreated(currentUserLogin.get());
	                a.setUserUpdated(currentUserLogin.get());
	                attachmentRepository.save(a);
	            }

	            Notification.show(attachmentsToSave.size() + " file(s) saved.",
	                    2200, Notification.Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
	            dialog.close();
	            refreshGrid();
	        } catch (ValidationException ex) {
	            Notification.show("Validation failed: " + ex.getMessage(),
	                    2500, Notification.Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
	        } catch (Exception ex) {
	            Notification.show("Error saving attachments: " + ex.getMessage(),
	                    3000, Notification.Position.TOP_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
	            ex.printStackTrace();
	        }
	    });

	    Button btnCancel = new Button("Cancel", VaadinIcon.CLOSE.create(), e -> dialog.close());
	    btnCancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

	    HorizontalLayout footer = new HorizontalLayout(btnSave, btnCancel);
	    footer.setJustifyContentMode(JustifyContentMode.END);
	    dialog.getFooter().add(footer);
	    dialog.open();
	}

	/**
	 * Builds thumbnail preview with "click to enlarge" lightbox dialog.
	 */
	/**
	 * Builds thumbnail preview with "click to enlarge" dialog (images and PDFs).
	 */
	private Component buildFilePreviewWithLightbox(
	        String fileName, String mimeType, byte[] bytes,
	        List<Attachment> attachmentsToSave, Attachment attachment) {

	    HorizontalLayout layout = new HorizontalLayout();
	    layout.setAlignItems(Alignment.CENTER);
	    layout.setSpacing(true);

	    Component previewContent;

	    // === 🖼️ IMAGE HANDLING ===
	    if (mimeType != null && mimeType.startsWith("image/")) {
	        Image thumb = new Image();
	        thumb.setSrc("data:" + mimeType + ";base64," +
	                Base64.getEncoder().encodeToString(bytes));
	        thumb.setAlt(fileName);
	        thumb.setMaxWidth("120px");
	        thumb.setMaxHeight("80px");
	        thumb.getStyle().set("cursor", "pointer");

	        Dialog lightbox = new Dialog();
	        lightbox.setCloseOnOutsideClick(true);
	        lightbox.setCloseOnEsc(true);

	        Image fullImage = new Image();
	        fullImage.setSrc("data:" + mimeType + ";base64," +
	                Base64.getEncoder().encodeToString(bytes));
	        fullImage.setMaxWidth("100%");
	        fullImage.setMaxHeight("100%");
	        fullImage.getStyle().set("display", "block").set("margin", "auto");

	        lightbox.add(fullImage);
	        thumb.addClickListener(e -> lightbox.open());
	        previewContent = thumb;

	    // === 🎬 VIDEO HANDLING ===
	    } else if (mimeType != null && mimeType.startsWith("video/")) {
	        Icon videoIcon = VaadinIcon.FILM.create();
	        videoIcon.setSize("30px");
	        videoIcon.getStyle().set("cursor", "pointer");

	        StreamResource videoResource = new StreamResource(fileName,
	                () -> new ByteArrayInputStream(bytes));
	        videoResource.setContentType(mimeType);

	        StreamRegistration registration = VaadinSession.getCurrent()
	                .getResourceRegistry()
	                .registerResource(videoResource);
	        String videoUrl = registration.getResourceUri().toASCIIString();

	        Dialog videoDialog = new Dialog();
	        videoDialog.setCloseOnOutsideClick(true);
	        videoDialog.setCloseOnEsc(true);
	        videoDialog.setWidth("90vw");
	        videoDialog.setHeight("90vh");

	        Html player = new Html(
	            "<video controls autoplay width='100%' height='100%' style='border:none;'>" +
	            "  <source src='" + videoUrl + "' type='" + mimeType + "'>" +
	            "  Your browser does not support HTML5 video." +
	            "</video>"
	        );

	        videoDialog.add(player);
	        videoIcon.addClickListener(e -> videoDialog.open());
	        previewContent = videoIcon;

	    // === 🎧 AUDIO HANDLING ===
	    } else if (mimeType != null && mimeType.startsWith("audio/")) {
	        Icon audioIcon = VaadinIcon.MUSIC.create();
	        audioIcon.setSize("30px");
	        audioIcon.getStyle().set("cursor", "pointer");

	        StreamResource audioResource = new StreamResource(fileName,
	                () -> new ByteArrayInputStream(bytes));
	        audioResource.setContentType(mimeType);

	        StreamRegistration registration = VaadinSession.getCurrent()
	                .getResourceRegistry()
	                .registerResource(audioResource);
	        String audioUrl = registration.getResourceUri().toASCIIString();

	        Dialog audioDialog = new Dialog();
	        audioDialog.setCloseOnOutsideClick(true);
	        audioDialog.setCloseOnEsc(true);
	        audioDialog.setWidth("600px");
	        audioDialog.setHeight("120px");

	        Html player = new Html(
	            "<audio controls autoplay style='width:100%;'>" +
	            "  <source src='" + audioUrl + "' type='" + mimeType + "'>" +
	            "  Your browser does not support the audio element." +
	            "</audio>"
	        );

	        audioDialog.add(player);
	        audioIcon.addClickListener(e -> audioDialog.open());
	        previewContent = audioIcon;

	    // === 📄 PDF HANDLING ===
	    } else if ("application/pdf".equalsIgnoreCase(mimeType)) {
	        Icon pdfIcon = VaadinIcon.FILE_PRESENTATION.create();
	        pdfIcon.setSize("30px");
	        pdfIcon.getStyle().set("cursor", "pointer");

	        StreamResource pdfResource = new StreamResource(fileName,
	                () -> new ByteArrayInputStream(bytes));
	        pdfResource.setContentType("application/pdf");

	        StreamRegistration registration = VaadinSession.getCurrent()
	                .getResourceRegistry()
	                .registerResource(pdfResource);
	        String resourceUrl = registration.getResourceUri().toASCIIString();

	        // 📄 Open PDF in new browser tab
	        pdfIcon.addClickListener(e -> {
	            UI.getCurrent().getPage().open(resourceUrl, "_blank");
	        });

	        previewContent = pdfIcon;

	    // === 📎 OTHER FILES ===
	    } else {
	        Icon fileIcon = VaadinIcon.FILE.create();
	        fileIcon.setSize("30px");
	        previewContent = fileIcon;
	    }

	    // === FILE NAME + REMOVE BUTTON ===
	    Span name = new Span(fileName);
	    name.getStyle().set("font-size", "14px");

	    Button remove = new Button(VaadinIcon.TRASH.create(), e -> {
	        attachmentsToSave.remove(attachment);
	        layout.getParent().ifPresent(parent -> ((VerticalLayout) parent).remove(layout));
	    });
	    remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

	    layout.add(previewContent, name, remove);
	    return layout;
	}





	

    @Override
    protected void configureEditorLayout() throws Exception {
    	btnAddNew.setVisible(false);
    	this.lineManager.setReadOnly(true);
    	this.lineManagerPositions.setReadOnly(true);
    	this.contractNumber.setReadOnly(true);
    	this.leaveStatus.setReadOnly(true);
    
        editorLayout.setDialogTitle("Leave Request | សំណើសុំច្បាប់ឈប់សម្រាក");

        // --- Add button for leave details ---
        Button addButton = new Button("Add Leave | បន្ថែមថ្ងៃឈប់សម្រាក", e -> {
            currentEditingDetail = null;
            leaveDetailBinder.readBean(new EmployeeLeaveDetail());
            leaveRequstDetailDialog.open();
        });
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setIcon(new Icon(VaadinIcon.PLUS));

        // --- Grid configuration for leave details ---
        gridLeaveDetail = new Grid<>(EmployeeLeaveDetail.class, false);
        gridLeaveDetail.addColumn(detail -> detail.getLeaveType() != null
                ? detail.getLeaveType().getLeaveNameEn() + " | " + detail.getLeaveType().getLeaveNameKh()
                : "").setHeader(this.leaveType.getLabel());
        
        gridLeaveDetail.addColumn(detail -> detail.getLeaveTypeSubType() != null
                ? detail.getLeaveTypeSubType().getLeaveSubTypeNameEn() + " | " + detail.getLeaveTypeSubType().getLeaveSubTypeNameKh()
                : "").setHeader(this.leaveTypeSubType.getLabel());
        
        gridLeaveDetail.addColumn(EmployeeLeaveDetail::getFromDate).setHeader(this.fromDate.getLabel());
        gridLeaveDetail.addColumn(EmployeeLeaveDetail::getToDate).setHeader(this.toDate.getLabel());
        gridLeaveDetail.addColumn(detail -> detail.getLeaveDuration() != null
                ? detail.getLeaveDuration().getLabel()
                : "").setHeader(this.leaveDuration.getLabel());
        gridLeaveDetail.addColumn(EmployeeLeaveDetail::getNumberOfDay).setHeader(this.numberOfDay.getLabel());
        gridLeaveDetail.addColumn(EmployeeLeaveDetail::getRemark).setHeader(this.remark.getLabel());
        gridLeaveDetail.addComponentColumn(detail -> {
            Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            editBtn.getElement().setAttribute("title", "Edit | កែប្រែ");
            editBtn.addClickListener(e -> {
                currentEditingDetail = detail;
                leaveDetailBinder.readBean(detail);
                leaveRequstDetailDialog.open();
            });

            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_ICON);
            deleteBtn.getElement().setAttribute("title", "Delete | លុប");
            deleteBtn.addClickListener(e -> {
                ConfirmDialog confirm = new ConfirmDialog();
                confirm.setHeader("Confirm Delete | បញ្ជាក់ការលុប");
                confirm.setText("Are you sure you want to delete this leave detail? | តើអ្នកចង់លុបព័ត៌មាននេះមែនទេ?");
                confirm.setConfirmText("Delete | លុប");
                confirm.setCancelText("Cancel | បោះបង់");
                confirm.addConfirmListener(ev -> {
                    leaveDetails.remove(detail);
                    gridLeaveDetail.getDataProvider().refreshAll();
                    Notification.show("Deleted successfully | លុបដោយជោគជ័យ", 1500, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                });
                confirm.open();
            });

            HorizontalLayout actions = new HorizontalLayout(editBtn, deleteBtn);
            actions.setJustifyContentMode(JustifyContentMode.CENTER);
            actions.setSpacing(false);
            actions.setPadding(false);
            return actions;
        })
        .setHeader("Actions | សកម្មភាព")
        .setTextAlign(ColumnTextAlign.CENTER)
        .setAutoWidth(true)
        .setFlexGrow(0);

        
        gridLeaveDetail.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_COLUMN_BORDERS
        );
        gridLeaveDetail.setWidthFull();
        gridLeaveDetail.setAllRowsVisible(true);
        
        gridLeaveDetail.getColumns().forEach(c -> {
            c.setResizable(true);
            c.setAutoWidth(true);
        });


        // ============================================================
        // 🧍‍♂️ REQUESTER INFORMATION
        // ============================================================


        FormLayout requesterInfoForm = new FormLayout(
                requestDate, employee, department, positions,
                contractNumber, lineManager, lineManagerPositions
        );
        requesterInfoForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3)
        );
        requesterInfoForm.setWidthFull();

        Details requesterInfoSection = new Details(
                "🧍‍♂️ Requester Information | ព័ត៌មានអ្នកស្នើសុំច្បាប់",
                requesterInfoForm
        );
        requesterInfoSection.addThemeVariants(DetailsVariant.FILLED);
        requesterInfoSection.setOpened(true);

        // ============================================================
        // 📋 LEAVE SUMMARY
        // ============================================================
        Stream.of(
        		annualLeaveAvailableBalance, sickLeaveUsed, specialLeaveUsed,
                unpaidLeaveUsed, paternityLeaveUsed, maternityLeaveUsed,
                compensatoryLeaveRemaining
        ).forEach(field -> {
            field.setWidthFull();
            field.getElement().setProperty("step", "0.5");
            field.setReadOnly(true);
        });

        FormLayout leaveSummaryForm = new FormLayout(
        		annualLeaveAvailableBalance, sickLeaveUsed, specialLeaveUsed,
                unpaidLeaveUsed, paternityLeaveUsed, maternityLeaveUsed,
                compensatoryLeaveRemaining
        );
        leaveSummaryForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3),
                new FormLayout.ResponsiveStep("1200px", 4)
        );

        Details leaveSummarySection = new Details(
                "📋 Leave Summary | សង្ខេបអំពីច្បាប់",
                leaveSummaryForm
        );
        leaveSummarySection.addThemeVariants(DetailsVariant.FILLED);
        leaveSummarySection.setOpened(true);

        // ============================================================
        // 🗂️ LEAVE DETAILS
        // ============================================================
        VerticalLayout leaveDetailContent = new VerticalLayout(addButton, gridLeaveDetail);
        leaveDetailContent.setPadding(false);
        leaveDetailContent.setSpacing(true);
        leaveDetailContent.setWidthFull();

        Details leaveDetailSection = new Details(
                "🗂️ Leave Details | ព័ត៌មានលម្អិតអំពីច្បាប់",
                leaveDetailContent
        );
        leaveDetailSection.addThemeVariants(DetailsVariant.FILLED);
        leaveDetailSection.setOpened(true);

        // ============================================================
        // Line Manager Check
        // ============================================================

        FormLayout lineManagerCheckForm = new FormLayout(lineManagerCheckDate,lineManagerChecked, lineManagerCheckedPositions,lineManagerCheckedStatus ,lineManagerCheckComments,this.supervisorSignatureLayout);
        lineManagerCheckForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3)
        );
        lineManagerCheckForm.setWidthFull();
        lineManagerCheckForm.setColspan(lineManagerCheckComments, 3);

        Details lineManagerCheckSection = new Details(
                "🧍‍♂️ Line Manager Check Information | ព័ត៌មានអ្នកគ្រប់គ្រង",
                lineManagerCheckForm
        );
        lineManagerCheckSection.addThemeVariants(DetailsVariant.FILLED);
        lineManagerCheckSection.setOpened(true);
        
        // ============================================================
        // HR Manager Check
        // ============================================================

        FormLayout hrCheckForm = new FormLayout(hrVerificationDate,hrVerificationBy, hrVerificationPosition,hrVerificationStatus ,hrVerificationComments,hrSignatureLayout);
        hrCheckForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("600px", 3)
        );
        hrCheckForm.setWidthFull();
        hrCheckForm.setColspan(hrVerificationComments, 3);

        Details lhrCheckSection = new Details(
                "🧍‍♂️ HR Information | ព័ត៌មានផ្នែកធនធានមនុស្ស",
                hrCheckForm
        );
        lhrCheckSection.addThemeVariants(DetailsVariant.FILLED);
        lhrCheckSection.setOpened(true);
        
        
        // ============================================================
        // MAIN FORM
        // ============================================================
        FormLayout mainForm = new FormLayout(
                requesterInfoSection,
                leaveSummarySection,
                leaveDetailSection,
                reason, 
                leaveStatus,
                requesterSignaturerLayout,
                lineManagerCheckSection,
                lhrCheckSection
        );
        mainForm.setColspan(requesterInfoSection, 2);
        mainForm.setColspan(leaveSummarySection, 2);
        mainForm.setColspan(leaveDetailSection, 2);
        mainForm.setColspan(lineManagerCheckSection, 2);
        mainForm.setColspan(lhrCheckSection, 2);
        mainForm.setColspan(reason, 2);
        //mainForm.setColspan(leaveStatus, 1);
        //mainForm.setColspan(requesterSignatureSection, 1);
        mainForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2)
        );

        // --- Add visual separation and styling between Details ---
        Stream.of(requesterInfoSection, leaveSummarySection, leaveDetailSection)
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


        // --- Add spacing for bottom fields ---
        reason.getStyle().set("margin-top", "var(--lumo-space-m)");
        leaveStatus.getStyle().set("margin-top", "var(--lumo-space-m)");
        //requesterSignatureSection.getStyle().set("margin-top", "var(--lumo-space-m)");

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

        // ============================================================
        // FOOTER BUTTONS
        // ============================================================
        Button btnCancel = new Button("Cancel | បោះបង់", e -> {
            closeForm();
            clearForm();
        });
        btnCancel.setIcon(new Icon(VaadinIcon.CLOSE));
        btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        btnCancel.addClickShortcut(Key.ESCAPE);

        Button btnSave = new Button("Save | រក្សាទុក", e -> save());
        btnSave.setIcon(new Icon(VaadinIcon.CHECK));
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnSave.addClickShortcut(Key.ENTER);

        HorizontalLayout buttonLayout = new HorizontalLayout(btnSave, btnCancel);
        buttonLayout.setClassName("button-layout");
        editorLayout.getFooter().add(buttonLayout);

        // ============================================================
        // COMBOBOX CONFIGURATION & LISTENERS
        // ============================================================
        setupComboBox(this.employee, "Please choose an employee",
                emp -> String.format("%s %s (%s)",
                        emp.getNameEn(), emp.getNameKh(),
                        emp.getInsuranceNo() != null ? emp.getInsuranceNo().toString() : ""));
        setupComboBox(this.department, "Please choose a department", Department::getName);
        setupComboBox(this.positions, "Please choose a position", Positions::getPosition);
        setupComboBox(this.leaveStatus, "Please choose leave status", LeaveStatus::getLeaveStatusName);
        setupComboBox(this.lineManager, "Please choose a line manager",
                emp -> String.format("%s %s (%s)",
                        emp.getNameEn(), emp.getNameKh(),
                        emp.getInsuranceNo() != null ? emp.getInsuranceNo().toString() : ""));
        setupComboBox(this.lineManagerPositions, "Please choose line manager position", Positions::getPosition);


        
        setupComboBox(this.lineManagerChecked, "Please choose an employee",  emp -> String.format("%s - %s ", formatEnKh(emp.getNameEn(), emp.getNameKh()),  emp.getInsuranceNo() != null ? emp.getInsuranceNo().toString() : ""));
        setupComboBox(this.lineManagerCheckedPositions, "Please choose line manager position", Positions::getPosition);
        setupComboBox(this.lineManagerCheckedStatus, "Please choose leave status", LeaveStatus::getLeaveStatusName);
        
        setupComboBox(this.hrVerificationBy, "Please choose an employee", emp -> String.format("%s - %s ", formatEnKh(emp.getNameEn(), emp.getNameKh()),  emp.getInsuranceNo() != null ? emp.getInsuranceNo().toString() : ""));
        setupComboBox(this.hrVerificationPosition, "Please choose position", Positions::getPosition);
        setupComboBox(this.hrVerificationStatus, "Please choose leave status", ls -> String.format("%s | %s", ls.getLeaveStatusName(), ls.getLeaveStatusNameKh()));
        //setupComboBox(this.hrVerificationStatus, "Please choose leave status", LeaveStatus::getLeaveStatusName);
        
        // --- Employee listener: Auto-fill department & manager ---
        this.employee.addValueChangeListener(event -> {
            Employee emp = event.getValue();
            if (emp != null) {
                this.department.setValue(emp.getDepartment());
                this.positions.setValue(emp.getPositions());
                this.contractNumber.setValue(emp.getPhoneNumber() != null ? emp.getPhoneNumber() : "");

                if (emp.getEmployeeSupervisions() != null && !emp.getEmployeeSupervisions().isEmpty()) {
                    List<Employee> supervisors = emp.getEmployeeSupervisions().stream()
                            .sorted(Comparator.comparingInt(EmployeeSupervision::getSortOrder))
                            .map(EmployeeSupervision::getSupervisor)
                            .filter(Objects::nonNull)
                            .toList();
                    this.lineManager.setItems(supervisors);
                    if (!supervisors.isEmpty()) {
                        Employee firstSupervisor = supervisors.get(0);
                        this.lineManager.setValue(firstSupervisor);
                        this.lineManagerPositions.setValue(firstSupervisor.getPositions());
                    }
                } else {
                    this.lineManager.setItems(List.of());
                    this.lineManager.clear();
                    this.lineManagerPositions.clear();
                    this.lineManager.setInvalid(true);
                    this.lineManager.setErrorMessage("No line managers available for this employee");
                }
            } else {
                department.clear();
                positions.clear();
                contractNumber.clear();
                lineManager.clear();
                lineManagerPositions.clear();
                lineManager.setInvalid(false);
                lineManager.setErrorMessage(null);
            }
        });
        
        this.lineManagerChecked.addValueChangeListener(event -> {
            Employee emp = event.getValue();
            if (emp != null) {
                this.lineManagerCheckedPositions.setValue(emp.getPositions());

            } else {
            	lineManagerCheckedPositions.clear();
            }
        });
        
        this.hrVerificationBy.addValueChangeListener(event -> {
            Employee emp = event.getValue();
            if (emp != null) {
                this.hrVerificationPosition.setValue(emp.getPositions());

            } else {
            	hrVerificationPosition.clear();
            }
        });
        
    }


    private void save() {
        try {
            binder.writeBean(entity);
            entity.setEmployeeLeaveDetails(leaveDetails);

            // Save the leave first
            entity = service.update(entity);

            // Now capture signature asynchronously
            requesterSignature.getImageData(base64 -> {
                if (base64 != null && !base64.isEmpty()) {
                    saveRequesterSignatureAttachment(entity, base64);
                }
                
                supervisorSignature.getImageData(supervisorBase64 -> {
                    if (supervisorBase64 != null && !supervisorBase64.isEmpty()) {
                        saveSupervisorSignatureAttachment(entity, supervisorBase64);
                    }
                    
                    hrSignature.getImageData(hrBase64 -> {
                        if (hrBase64 != null && !hrBase64.isEmpty()) {
                            saveHrSignatureAttachment(entity, hrBase64);
                        }

                // UI feedback after async completion
                UI.getCurrent().access(() -> {
                    Notification.show("Leave request saved successfully",
                            2000, Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    clearForm();
                    closeForm();
                    refreshGrid();
                });
                });
                });
            });

        } catch (ValidationException ex) {
            showError("Validation failed: " + ex.getMessage());
        } catch (Exception ex) {
            showError("Error saving record: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    @Override
    protected void populateForm(EmployeeLeave entity) throws Exception {
    	
        if (entity.getId() != null) {
            Long id = entity.getId();
            entity = service.findById(id) .orElseThrow(() -> new RuntimeException("Record not found: " + id));
        }

        this.entity = entity;
        
        loadDataToForm();
        binder.readBean(entity);
        editorLayout.open();

        boolean isNewEntity = entity.getId() == null || entity.getId() == 0;

        if (isNewEntity) {
            employee.setValue(employeeRepository.findById(Long.valueOf(currentUserLogin.get().getInsurance())) .orElse(null));

            Integer insuranceNo = Optional.ofNullable(employee.getValue()).map(Employee::getInsuranceNo).orElse(null);
            Integer year = Optional.ofNullable(requestDate.getValue()) .map(LocalDate::getYear).orElse(LocalDate.now().getYear());

            if (insuranceNo != null) {
                List<EmployeeLeaveBalance> balances =
                        employeeLeaveBalanceRepository.findByEmployeeInsuranceNoAndYear(insuranceNo, year);

                // Map leaveType IDs to UI fields
                Map<Long, BigDecimalField> typeToField = Map.ofEntries(
                    Map.entry(1L, annualLeaveAvailableBalance),    // Annual
                    Map.entry(10L, sickLeaveUsed),          // Sick
                    Map.entry(9L, specialLeaveUsed),        // Special
                    Map.entry(2L, unpaidLeaveUsed),         // Unpaid
                    Map.entry(21L, paternityLeaveUsed),     // Paternity
                    Map.entry(3L, maternityLeaveUsed),      // Maternity
                    Map.entry(22L, compensatoryLeaveRemaining) // Compensatory
                );

                for (EmployeeLeaveBalance b : balances) {
                    BigDecimalField targetField = typeToField.get(b.getLeaveType().getId());
                    if (targetField == null) continue;

                    BigDecimal value = switch (b.getLeaveType().getId().intValue()) {
                        case 1, 22 -> b.getRemainingDaysCalc();
                        default -> b.getTakenDays();
                    };

                    if (value != null)
                        targetField.setValue(value);
                    else
                        targetField.clear();
                }
            }
        }
        
        //this.lineManagerCheckDate.setValue(LocalDate.now());
        //this.lineManagerChecked.setValue(employeeRepository.findByInsuranceNo(Integer.valueOf(currentUserLogin.get().getInsurance())) .orElse(null));
        lineManagerCheckDate.setReadOnly(true);
        lineManagerChecked.setReadOnly(true);
        this.lineManagerCheckedPositions.setReadOnly(true);
        this.lineManagerCheckedStatus.setReadOnly(true);
        
        
        //this.hrVerificationDate.setValue(LocalDate.now());
        //this.hrVerificationBy.setValue(employeeRepository.findByInsuranceNo(Integer.valueOf(currentUserLogin.get().getInsurance())) .orElse(null));
        
        this.hrVerificationDate.setReadOnly(true);
        this.hrVerificationBy.setReadOnly(true);
        
        this.leaveStatus.setReadOnly(true);
        this.requestDate.setReadOnly(true);
        this.employee.setReadOnly(true);
        this.department.setReadOnly(true);
        this.positions.setReadOnly(true);

        // ✅ Load detail list into the grid when editing
        leaveDetails.clear();
        if (entity.getEmployeeLeaveDetails() != null) {
            leaveDetails.addAll(entity.getEmployeeLeaveDetails());
        }
        gridLeaveDetail.setItems(leaveDetails);
        gridLeaveDetail.getDataProvider().refreshAll();
        
     // Load existing signature attachment

	    List<Attachment> attachments = attachmentRepository.findByEntity(entity);
	    entity.setAttachments(attachments);
	    
	    loadSignatureIfPresent(attachments, 1L, requesterSignature);   // Requester Signature
	    loadSignatureIfPresent(attachments, 2L, supervisorSignature);  // Supervisor Signature
	    loadSignatureIfPresent(attachments, 3L, hrSignature);  // Supervisor Signature


    }


    private void clearForm() {
        binder.readBean(null);
        this.entity = null;
    }

    private void closeForm() {
        editorLayout.close();
    }
    
    @Override
    protected Specification<EmployeeLeave> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                // ✅ Always mark query as distinct
                query.distinct(true);

                // ✅ Optional: if this is the count query (used by Vaadin DataProvider)
                if (query.getResultType() == Long.class || query.getResultType() == long.class) {
                    // Don't call query.select() here — just ensure it's distinct
                    // (Spring Data will auto-generate COUNT(DISTINCT root))
                }
                
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                // === JOINS ===
                Join<EmployeeLeave, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<EmployeeLeave, User> userUpdated = root.join("userUpdated", JoinType.LEFT);
                Join<EmployeeLeave, Employee> employee = root.join("employee", JoinType.INNER);
                Join<Employee, EmployeeSupervision> employeeSupervision = employee.join("employeeSupervisions", JoinType.LEFT);
                Join<EmployeeSupervision, Employee> supervisor = employeeSupervision.join("supervisor", JoinType.LEFT);
                
                Join<EmployeeLeave, Department> department = root.join("department", JoinType.LEFT);
                Join<EmployeeLeave, Positions> position = root.join("positions", JoinType.LEFT);
                Join<EmployeeLeave, LeaveStatus> leaveStatus = root.join("leaveStatus", JoinType.LEFT);
                Join<EmployeeLeave, Employee> lineManager = root.join("lineManager", JoinType.LEFT);
                Join<EmployeeLeave, Positions> lineManagerPos = root.join("lineManagerPositions", JoinType.LEFT);
                Join<EmployeeLeave, LeaveStatus> lineManagerleaveStatus = root.join("lineManagerCheckedStatus", JoinType.LEFT);
                Join<EmployeeLeave, LeaveStatus> hrleaveStatus = root.join("hrVerificationStatus", JoinType.LEFT);
                //Line manager Check
                Join<EmployeeLeave, Employee> lineManagerChecked = root.join("lineManagerChecked", JoinType.LEFT);
                Join<EmployeeLeave, Positions> lineManagerCheckedPosition = root.join("lineManagerCheckedPositions", JoinType.LEFT);
                Join<EmployeeLeave, LeaveStatus> lineManagerCheckedStatus = root.join("lineManagerCheckedStatus", JoinType.LEFT);
                
                //Hr Check
                Join<EmployeeLeave, Employee> hrVerificationBy = root.join("hrVerificationBy", JoinType.LEFT);
                Join<EmployeeLeave, Positions> hrVerificationPosition = root.join("hrVerificationPosition", JoinType.LEFT);
                Join<EmployeeLeave, LeaveStatus> hrVerificationStatus = root.join("hrVerificationStatus", JoinType.LEFT);
                
	            //Default Filter
                //predicates.add(cb.equal(supervisor.get("insuranceNo"), currentUserLogin.get().getInsurance()));
	            //buildInPredicate(cb,leaveStatus.get("id"),List.of(1L), Object::toString,"Leave Status",predicates,null);
	            //buildInPredicate(cb,lineManagerleaveStatus.get("id"),List.of(2L), Object::toString,"Line Manager Leave Status",predicates,null);
	            buildInPredicate(cb,hrVerificationStatus.get("id"),List.of(6L), Object::toString,"HR Leave Status",predicates,null);


                // === QUICK SEARCH ===
                String quick = txtQuick.getValue();
                if (quick != null && !quick.isEmpty()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";

                    predicates.add(cb.or(
                        buildLikePredicate(cb, root.get("id"), like),
                        buildLikePredicate(cb, root.get("requestDate"), like),
                        buildLikePredicate(cb, employee.get("nameEn"), like),
                        buildLikePredicate(cb, employee.get("nameKh"), like),
                        buildLikePredicate(cb, employee.get("insuranceNo"), like),
                        buildLikePredicate(cb, department.get("name"), like),
                        buildLikePredicate(cb, position.get("position"), like),
                        //Line Manager                       
                        buildLikePredicate(cb, lineManager.get("nameEn"), like),
                        buildLikePredicate(cb, lineManager.get("nameKh"), like),
                        buildLikePredicate(cb, lineManager.get("insuranceNo"), like),
                        buildLikePredicate(cb, lineManagerPos.get("position"), like),
                        //Balance
                        buildLikePredicate(cb, root.get("annualLeaveAvailableBalance"), like),
                        buildLikePredicate(cb, root.get("sickLeaveUsed"), like),
                        buildLikePredicate(cb, root.get("specialLeaveUsed"), like),
                        buildLikePredicate(cb, root.get("unpaidLeaveUsed"), like),
                        buildLikePredicate(cb, root.get("paternityLeaveUsed"), like),
                        buildLikePredicate(cb, root.get("maternityLeaveUsed"), like),
                        buildLikePredicate(cb, root.get("compensatoryLeaveRemaining"), like), 
                        
                        buildLikePredicate(cb, leaveStatus.get("leaveStatusName"), like),
                        buildLikePredicate(cb, root.get("reason"), like),
                        buildLikePredicate(cb, root.get("contractNumber"), like),
                        
                        //Line manager Check
                        buildLikePredicate(cb, lineManagerChecked.get("nameEn"), like),
                        buildLikePredicate(cb, lineManagerChecked.get("nameKh"), like),
                        buildLikePredicate(cb, lineManagerChecked.get("insuranceNo"), like),
                        buildLikePredicate(cb, lineManagerCheckedPosition.get("position"), like),
                        buildLikePredicate(cb, root.get("lineManagerCheckDate"), like),
                        buildLikePredicate(cb, lineManagerCheckedStatus.get("leaveStatusName"), like),
                        buildLikePredicate(cb, root.get("lineManagerCheckComments"), like),
                        
                        //HR Check
                        buildLikePredicate(cb, hrVerificationBy.get("nameEn"), like),
                        buildLikePredicate(cb, hrVerificationBy.get("nameKh"), like),
                        buildLikePredicate(cb, hrVerificationBy.get("insuranceNo"), like),
                        buildLikePredicate(cb, hrVerificationPosition.get("position"), like),
                        buildLikePredicate(cb, root.get("hrVerificationDate"), like),
                        buildLikePredicate(cb, hrVerificationStatus.get("leaveStatusName"), like),
                        buildLikePredicate(cb, root.get("hrVerificationComments"), like),
                        
                        
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

                // === DATE RANGE FILTERS ===
                buildBetweenPredicate(cb, root.get("requestDate"),
                        advanceFilterRequestDateFrom.getValue(),
                        advanceFilterRequestDateTo.getValue(),
                        "RequestDate", predicates, sqlFilter);

                buildBetweenPredicate(cb,
                        cb.function("DATE", LocalDate.class, root.get("createdAt")),
                        advanceFilterCreatedDateFrom.getValue(),
                        advanceFilterCreatedDateTo.getValue(),
                        "Created At", predicates, sqlFilter);

                buildBetweenPredicate(cb,
                        cb.function("DATE", LocalDate.class, root.get("updatedAt")),
                        advanceFilterUpdaedDateFrom.getValue(),
                        advanceFilterUpdaedDateTo.getValue(),
                        "Updated At", predicates, sqlFilter);

                // === IN FILTERS ===
                buildInPredicate(cb, employee, advanceFilterEmployee.getValue(), Employee::getNameEn, "Employee", predicates, sqlFilter);

                buildInPredicate(cb, department, advanceFilterDepartment.getValue(), Department::getName, "Department", predicates, sqlFilter);

                buildInPredicate(cb, position, advanceFilterPosition.getValue(),Positions::getPosition, "Position", predicates, sqlFilter);
                
                if (advanceFilterContactNumber.getValue() != null && !advanceFilterContactNumber.getValue().isEmpty()) {
                    predicates.add(cb.equal(root.get("contractNumber"), advanceFilterContactNumber.getValue()));
                    sqlFilter.add("Contact Numvber = " + advanceFilterContactNumber.getValue());
                }

                buildInPredicate(cb, lineManager, advanceFilterLineManager.getValue(),e -> e.getNameEn() + " " + e.getNameKh(), "Line Manager", predicates, sqlFilter);

                buildInPredicate(cb, lineManagerPos, advanceFilterLineManagerPosition.getValue(), Positions::getPosition, "Line Manager Position", predicates, sqlFilter);

                buildInPredicate(cb, leaveStatus, advanceFilterLeaveStatus.getValue(), LeaveStatus::getLeaveStatusName, "Leave Status", predicates, sqlFilter);
                
                
                
                buildInPredicate(cb, lineManagerChecked, advanceFilterlineManagerChecked.getValue(),e -> e.getNameEn() + " " + e.getNameKh(), "Line Manager Checked", predicates, sqlFilter);
                buildInPredicate(cb, lineManagerCheckedPosition, advanceFilterlineManagerCheckedPositions.getValue(), Positions::getPosition, "Line Manager Checked Position", predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("lineManagerCheckDate"), advanceFilterlineManagerCheckDateFrom.getValue(), advanceFilterlineManagerCheckDateTo.getValue(),"line Manager Checked Date", predicates, sqlFilter);
                buildInPredicate(cb, lineManagerCheckedStatus, advanceFilterlineManagerCheckedStatus.getValue(), LeaveStatus::getLeaveStatusName, "line Manager Checked Status", predicates, sqlFilter);
                
                buildInPredicate(cb, hrVerificationBy, advanceFilterHrVerificationBy.getValue(),e -> e.getNameEn() + " " + e.getNameKh(), "HR Verified By", predicates, sqlFilter);
                buildInPredicate(cb, hrVerificationPosition, advanceFilterHrVerificationPosition.getValue(), Positions::getPosition, "HR Verified  Position", predicates, sqlFilter);
                buildBetweenPredicate(cb, root.get("hrVerificationDate"), advanceFilterHrVerificationDateFrom.getValue(), advanceFilterHrVerificationDateTo.getValue(),"HR Verification Date", predicates, sqlFilter);
                buildInPredicate(cb, hrVerificationStatus, advanceFilterHrVerificationStatus.getValue(), LeaveStatus::getLeaveStatusName, "HR Verified Status", predicates, sqlFilter);
                

                buildInPredicate(cb, userCreated, advanceFilterCreatedBy.getValue(), User::getName, "Created By", predicates, sqlFilter);

                buildInPredicate(cb, userUpdated, advanceFilterUpdatedBy.getValue(),User::getName, "Updated By", predicates, sqlFilter);

                // === FINAL DISPLAY ===
                this.showHideAdvanceFilter(sqlFilter.isEmpty() ? null : String.join(" AND ", sqlFilter));

                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception ex) {
                showError("Error in filter: " + ex.getMessage());
                ex.printStackTrace();
                return cb.conjunction();
            }
        };
    }

    /**
     * 🔹 Universal LIKE predicate builder
     * Handles String, numeric, and temporal (date/time) fields.
     */
    private Predicate buildLikePredicate(CriteriaBuilder cb, Expression<?> expression, String likePattern) {
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


    /**
     * 🔹 BETWEEN predicate builder (for date range filters).
     */
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


    @Override
    protected void createAdvanceFilterLayout() {
        // Configure advance filter components
        advanceFilterID.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        advanceFilterSettingDateRank(advanceFilterRequestDateFrom, advanceFilterRequestDateTo);
        
        setupMultiSelectComboBox(advanceFilterCreatedBy, userService.getAllUser(),User::getName);
        setupMultiSelectComboBox(advanceFilterUpdatedBy, userService.getAllUser(),User::getName); 

        setupMultiSelectComboBox(advanceFilterEmployee, employeeRepository.findActiveLocalStaff(), emp -> String.format("%s %s %s", emp.getNameEn(), emp.getNameKh(),emp.getInsuranceNo()));
        advanceFilterSettingDateRank(advanceFilterCreatedDateFrom, advanceFilterCreatedDateTo);
        advanceFilterSettingDateRank(advanceFilterUpdaedDateFrom, advanceFilterUpdaedDateTo);
        
        setupMultiSelectComboBox(advanceFilterDepartment, departmentRepository.findAll(), Department::getName);
        setupMultiSelectComboBox(advanceFilterPosition, positionRepository.findAll(), Positions::getPosition);
        
        setupMultiSelectComboBox(advanceFilterLineManager, employeeRepository.findAll(), emp -> (emp.getNameEn() + " " + emp.getNameKh() + " (" + (emp.getInsuranceNo() != null ? emp.getInsuranceNo() : "")+")"));
        setupMultiSelectComboBox(advanceFilterLineManagerPosition, positionRepository.findAll(), Positions::getPosition);
        
        setupMultiSelectComboBox(advanceFilterLeaveStatus, leaveStatusRepository.findAllById(List.of(1L,4L)), LeaveStatus::getLeaveStatusName);    
        
        advanceFilterContactNumber.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        
        setupMultiSelectComboBox(advanceFilterlineManagerChecked, employeeRepository.findActiveLocalStaff(), emp -> String.format("%s %s %s", emp.getNameEn(), emp.getNameKh(),emp.getInsuranceNo()));
        setupMultiSelectComboBox(advanceFilterlineManagerCheckedPositions, positionRepository.findAll(), Positions::getPosition);
        advanceFilterSettingDateRank(advanceFilterlineManagerCheckDateFrom, advanceFilterlineManagerCheckDateTo);
        setupMultiSelectComboBox(advanceFilterlineManagerCheckedStatus, leaveStatusRepository.findAllById(List.of(2L,3L,8L)), LeaveStatus::getLeaveStatusName);
        
        setupMultiSelectComboBox(advanceFilterHrVerificationBy, employeeRepository.findActiveLocalStaff(), emp -> String.format("%s %s %s", emp.getNameEn(), emp.getNameKh(),emp.getInsuranceNo()));
        setupMultiSelectComboBox(advanceFilterHrVerificationPosition, positionRepository.findAll(), Positions::getPosition);
        advanceFilterSettingDateRank(advanceFilterHrVerificationDateFrom, advanceFilterHrVerificationDateTo);
        setupMultiSelectComboBox(advanceFilterHrVerificationStatus, leaveStatusRepository.findAllById(List.of(5L,6L,7L)), LeaveStatus::getLeaveStatusName);

        // Add created/updated filters configuration (similar to original)

        this.advanceSearchLayout.add(
            advanceFilterID,
            advanceFilterRequestDateFrom,
            advanceFilterRequestDateTo,
            advanceFilterEmployee,
            advanceFilterDepartment,
            advanceFilterPosition,
            advanceFilterLineManager,
            advanceFilterLineManagerPosition,
            advanceFilterLeaveStatus,
            advanceFilterContactNumber,
            
            advanceFilterlineManagerChecked,
            advanceFilterlineManagerCheckedPositions,
            advanceFilterlineManagerCheckDateFrom,
            advanceFilterlineManagerCheckDateTo,
            advanceFilterlineManagerCheckedStatus,
            
            advanceFilterHrVerificationBy,
            advanceFilterHrVerificationPosition,
            advanceFilterHrVerificationDateFrom,
            advanceFilterHrVerificationDateTo,
            advanceFilterHrVerificationStatus,
            
            advanceFilterCreatedBy,
            advanceFilterCreatedDateFrom,
            advanceFilterCreatedDateTo,
            advanceFilterUpdatedBy,
            advanceFilterUpdaedDateFrom,
            advanceFilterUpdaedDateTo
        );
    }

   @Override
    protected void focusFirstField() {
        requestDate.focus();
    }

    @Override
    protected EmployeeLeave createNewEntity() throws Exception {
        EmployeeLeave entity = new EmployeeLeave();
        entity.setEmployee(employeeRepository.findById(Long.valueOf(currentUserLogin.get().getInsurance())).orElse(null));
        entity.setRequestDate(LocalDate.now());
        entity.setLeaveStatus(leaveStatusRepository.findById(1L).orElse(null));
        return entity;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(LeaveSupportingDocumentView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    // Column Definitions for Leave Request
    private record ColumnDef(
        String key,
        String header,
        ValueProvider<EmployeeLeave, ?> dataProvider,
        Function<EmployeeLeave, String> textFormatter
    ) {}

    private List<ColumnDef> getColumnDefinitions() {
        return List.of(
            new ColumnDef("id", "ID",
                EmployeeLeave::getId,
                e -> e.getId() != null ? e.getId().toString() : ""),
            new ColumnDef("employee.insuranceNo", "Insurance No",
                    e -> e.getEmployee() != null ? e.getEmployee().getInsuranceNo() : "",
                    e -> e.getEmployee() != null ? e.getEmployee().getInsuranceNo().toString() : ""),


            new ColumnDef("employee.nameEn", "Employee Name EN",
                e -> e.getEmployee() != null ? e.getEmployee().getNameEn() : "",
                e -> e.getEmployee() != null ? e.getEmployee().getNameEn() : ""),

            new ColumnDef("employee.nameKh", "Employee Name KH",
                e -> e.getEmployee() != null ? e.getEmployee().getNameKh() : "",
                e -> e.getEmployee() != null ? e.getEmployee().getNameKh() : ""),
            

            new ColumnDef("requestDate", "Request Date",
                    EmployeeLeave::getRequestDate,
                    e -> e.getRequestDate() != null ? 
                        DateTimeUtilFormart.DATE_FORMATTER.format(e.getRequestDate()) : ""),

            new ColumnDef("department.departmentName", "Department",
                e -> e.getDepartment() != null ? e.getDepartment().getName() : "",
                e -> e.getDepartment() != null ? e.getDepartment().getName() : ""),

            new ColumnDef("positions.position", "Position",
                e -> e.getPositions() != null ? e.getPositions().getPosition() : "",
                e -> e.getPositions() != null ? e.getPositions().getPosition() : ""),
            
            new ColumnDef("contractNumber", "Contact Number",
            		EmployeeLeave::getContractNumber ,
                    e -> e.getContractNumber() != null ? e.getContractNumber() : ""),
            
            new ColumnDef("leaveDetailSummary", "Leave Detail", this::formatLeaveDetailSummary, this::formatLeaveDetailSummary),

            
            new ColumnDef("lineManager.name", "Line Manager",
                e -> e.getLineManager() != null ? e.getLineManager().getNameEn() + " " + e.getLineManager().getNameKh() + "("+ e.getLineManager().getInsuranceNo() + ")": "",
                e -> e.getLineManager() != null ? e.getLineManager().getNameEn() + " " + e.getLineManager().getNameKh() + "("+ e.getLineManager().getInsuranceNo() + ")": ""),
            
            new ColumnDef("lineManagerPositions.position", "Line Manager Position",
                    e -> e.getLineManagerPositions() != null ? e.getLineManagerPositions().getPosition() : "",
                    		 e -> e.getLineManagerPositions() != null ? e.getLineManagerPositions().getPosition() : ""),
            
            new ColumnDef("annualLeaveAvailableBalance", annualLeaveAvailableBalance.getLabel(),
                    EmployeeLeave::getAnnualLeaveAvailableBalance,
                    e -> e.getAnnualLeaveAvailableBalance() != null ? e.getAnnualLeaveAvailableBalance().toString() : ""),
            
            new ColumnDef("sickLeaveUsed", "Sick Leave Used",
                    EmployeeLeave::getSickLeaveUsed,
                    e -> e.getSickLeaveUsed() != null ? e.getSickLeaveUsed().toString() : ""),
            
            new ColumnDef("specialLeaveUsed", "Special Leave Used",
                    EmployeeLeave::getSpecialLeaveUsed,
                    e -> e.getSpecialLeaveUsed() != null ? e.getSpecialLeaveUsed().toString() : ""),
            
            new ColumnDef("unpaidLeaveUsed", "Unpaid Leave Used",
                    EmployeeLeave::getUnpaidLeaveUsed,
                    e -> e.getUnpaidLeaveUsed() != null ? e.getUnpaidLeaveUsed().toString() : ""),
            
            new ColumnDef("paternityLeaveUsed", "Paternity Leave Used",
                    EmployeeLeave::getPaternityLeaveUsed,
                    e -> e.getPaternityLeaveUsed() != null ? e.getPaternityLeaveUsed().toString() : ""),
            
            new ColumnDef("maternityLeaveUsed", "Maternity Leave Used",
                    EmployeeLeave::getMaternityLeaveUsed,
                    e -> e.getMaternityLeaveUsed() != null ? e.getMaternityLeaveUsed().toString() : ""),
            
            new ColumnDef("compensatoryLeaveRemaining", "Compensatory Leave Remaining",
                    EmployeeLeave::getCompensatoryLeaveRemaining,
                    e -> e.getCompensatoryLeaveRemaining() != null ? e.getCompensatoryLeaveRemaining().toString() : ""),
            
            
            new ColumnDef("leaveStatus.statusName", "Leave Status",
                    e -> e.getLeaveStatus() != null ? e.getLeaveStatus().getLeaveStatusName() : "",
                    e -> e.getLeaveStatus() != null ? e.getLeaveStatus().getLeaveStatusName() : ""),
            

            new ColumnDef("reason", "Reason",
                EmployeeLeave::getReason,
                e -> e.getReason() != null ? 
                    (e.getReason().length() > 50 ? e.getReason().substring(0, 50) + "..." : e.getReason()) : ""),
         //Line manager   
            new ColumnDef("lineManagerChecked.name", "Line Manager Checked",
                    e -> e.getLineManagerChecked() != null ? e.getLineManagerChecked().getNameEn() + " " + e.getLineManagerChecked().getNameKh() + "("+ e.getLineManagerChecked().getInsuranceNo() + ")": "",
                    e -> e.getLineManagerChecked() != null ? e.getLineManagerChecked().getNameEn() + " " + e.getLineManagerChecked().getNameKh() + "("+ e.getLineManagerChecked().getInsuranceNo() + ")": ""),
            
            new ColumnDef("lineManagerChecked.position", "Line Manager Checked Position",
                    e -> e.getLineManagerCheckedPositions() != null ? e.getLineManagerCheckedPositions().getPosition() : "",
                    e -> e.getLineManagerCheckedPositions() != null ? e.getLineManagerCheckedPositions().getPosition() : ""), 
            
            new ColumnDef("lineManagerCheckDate", "Line Manager Checked Date",
                    EmployeeLeave::getLineManagerCheckDate,
                    e -> e.getLineManagerCheckDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getLineManagerCheckDate()) : ""),
            
            new ColumnDef("lineManagerCheckedStatus.leaveStatusName", "Line Manager Checked Status",
                    e -> e.getLineManagerCheckedStatus() != null ? e.getLineManagerCheckedStatus().getLeaveStatusName() : "",
                    e -> e.getLineManagerCheckedStatus() != null ? e.getLineManagerCheckedStatus().getLeaveStatusName() : ""),
            
            new ColumnDef("lineManagerCheckComments", "Line Manager Check Comments",
                    EmployeeLeave::getLineManagerCheckComments,
                    e -> e.getLineManagerCheckComments() != null ? 
                        (e.getLineManagerCheckComments().length() > 50 ? e.getLineManagerCheckComments().substring(0, 50) + "..." : e.getLineManagerCheckComments()) : ""),
            
            //HR manager   
            new ColumnDef("hrVerificationBy.nameEn", "HR Verification By",
                    e -> e.getHrVerificationBy() != null ? e.getHrVerificationBy().getNameEn() + " " + e.getHrVerificationBy().getNameKh() + "("+ e.getHrVerificationBy().getInsuranceNo() + ")": "",
                    e -> e.getHrVerificationBy() != null ? e.getHrVerificationBy().getNameEn() + " " + e.getHrVerificationBy().getNameKh() + "("+ e.getHrVerificationBy().getInsuranceNo() + ")": ""),
            
            new ColumnDef("hrVerificationPosition.position", "HR Verification Position",
                    e -> e.getHrVerificationPosition() != null ? e.getHrVerificationPosition().getPosition() : "",
                    e -> e.getHrVerificationPosition() != null ? e.getHrVerificationPosition().getPosition() : ""), 
            
            new ColumnDef("hrVerificationDate", "HR Verification Date",
                    EmployeeLeave::getHrVerificationDate,
                    e -> e.getHrVerificationDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(e.getHrVerificationDate()) : ""),
            
            new ColumnDef("hrVerificationStatus.leaveStatusName", "HR Verification Status",
                    e -> e.getHrVerificationStatus() != null ? e.getHrVerificationStatus().getLeaveStatusName() : "",
                    e -> e.getHrVerificationStatus() != null ? e.getHrVerificationStatus().getLeaveStatusName() : ""),
            
            
            new ColumnDef("hrVerificationComments", "HR Verification Comments",
                    EmployeeLeave::getHrVerificationComments,
                    e -> e.getHrVerificationComments() != null ? (e.getHrVerificationComments().length() > 50 ? e.getHrVerificationComments().substring(0, 50) + "..." : e.getHrVerificationComments()) : ""),
            

            new ColumnDef("userCreated.name", "Created By",
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

            new ColumnDef("createdAt", "Created At",
                EmployeeLeave::getCreatedAt,
                e -> e.getCreatedAt() != null ?
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

            new ColumnDef("userUpdated.name", "Updated By",
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

            new ColumnDef("updatedAt", "Updated At",
                EmployeeLeave::getUpdatedAt,
                e -> e.getUpdatedAt() != null ?
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }
    private String formatLeaveDetailSummary(EmployeeLeave e) {
        if (e == null || e.getEmployeeLeaveDetails() == null) return "";

        return e.getEmployeeLeaveDetails().stream()
                .map(d -> {
                    String type = d.getLeaveType() == null ? ""
                            : String.format("%s | %s",
                            Optional.ofNullable(d.getLeaveType().getLeaveNameEn()).orElse(""),
                            Optional.ofNullable(d.getLeaveType().getLeaveNameKh()).orElse(""));

                    String sub = d.getLeaveTypeSubType() == null ? ""
                            : String.format("%s | %s",
                            Optional.ofNullable(d.getLeaveTypeSubType().getLeaveSubTypeNameEn()).orElse(""),
                            Optional.ofNullable(d.getLeaveTypeSubType().getLeaveSubTypeNameKh()).orElse(""));

                    String typeText = sub.isBlank() ? type : (type + " (" + sub + ")");

                    String from = d.getFromDate() == null ? ""
                            : DateTimeUtilFormart.DATE_FORMATTER.format(d.getFromDate());
                    String to = d.getToDate() == null ? ""
                            : DateTimeUtilFormart.DATE_FORMATTER.format(d.getToDate());

                    String dateRange = (!from.isBlank() && !to.isBlank())
                            ? (from + " → " + to)
                            : (!from.isBlank() ? from : (!to.isBlank() ? to : ""));

                    String duration = d.getLeaveDuration() != null ? d.getLeaveDuration().getLabel() : "";
                    String days = d.getNumberOfDay() != null ? d.getNumberOfDay().toString() : "";

                    // Build: Type (Sub) | 2026-01-10 → 2026-01-12 | Full Day | 3
                    return Stream.of(typeText, dateRange, duration, days)
                            .filter(s -> s != null && !s.isBlank())
                            .collect(Collectors.joining(" | "));
                })
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" ; "));
    }

    
    @Override
    protected HorizontalLayout exportToExcelFile() {
        Anchor downloadLink = new Anchor();
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.getElement().getStyle().set("display", "none");

        Button exportButton = new Button("Export to Excel", new Icon(VaadinIcon.DOWNLOAD));
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_WARNING);

        exportButton.addClickListener(event -> {
            try {
                List<ColumnDef> visibleColumns = getColumnDefinitions().stream()
                        .filter(col -> {
                            var gridCol = grid.getColumnByKey(col.key());
                            return gridCol != null && gridCol.isVisible();
                        })
                        .toList();

                List<EmployeeLeave> itemsToExport = grid.getSelectedItems().isEmpty()
                        ? grid.getGenericDataView().getItems().toList()
                        : new ArrayList<>(grid.getSelectedItems());

                if (itemsToExport.isEmpty()) {
                    Notification.show("No data to export", 1500, Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                    return;
                }

                StreamResource resource = new StreamResource("EmployeeLeaveRequest.xlsx", () -> {
                    try (Workbook workbook = new XSSFWorkbook()) {
                        Sheet sheet = workbook.createSheet("EmployeeLeaveRequest");

                        // 🔹 Create cell styles
                        CreationHelper creationHelper = workbook.getCreationHelper();

                        CellStyle headerStyle = workbook.createCellStyle();
                        Font headerFont = workbook.createFont();
                        headerFont.setBold(true);
                        headerStyle.setFont(headerFont);

                        // DateTime style (used for real date objects)
                        CellStyle dateTimeStyle = workbook.createCellStyle();
                        dateTimeStyle.setDataFormat(
                                creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss")
                        );

                        // 🔹 Header
                        Row header = sheet.createRow(0);
                        for (int i = 0; i < visibleColumns.size(); i++) {
                            Cell cell = header.createCell(i);
                            cell.setCellValue(visibleColumns.get(i).header());
                            cell.setCellStyle(headerStyle);
                        }

                        // 🔹 Data rows
                        int rowIndex = 1;
                        for (EmployeeLeave entity : itemsToExport) {
                            Row row = sheet.createRow(rowIndex++);
                            for (int i = 0; i < visibleColumns.size(); i++) {
                                ColumnDef def = visibleColumns.get(i);
                                Object rawValue = def.dataProvider().apply(entity);
                                Cell cell = row.createCell(i);

                                if (rawValue == null) {
                                    cell.setBlank();
                                    continue;
                                }

                                // ✅ Always use formatted text for specific columns
                                String key = def.key();
                                if ("monthNum".equals(key)
                                        || "createdAt".equals(key)
                                        || "updatedAt".equals(key)) {
                                    cell.setCellValue(def.textFormatter().apply(entity));
                                    continue;
                                }

                                // 🔹 Detect type and assign appropriately
                                if (rawValue instanceof Number num) {
                                    cell.setCellValue(num.doubleValue());
                                } else if (rawValue instanceof java.time.LocalDate ld) {
                                    cell.setCellValue(java.sql.Date.valueOf(ld));
                                    cell.setCellStyle(dateTimeStyle);
                                } else if (rawValue instanceof java.time.LocalDateTime ldt) {
                                    cell.setCellValue(java.util.Date
                                            .from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()));
                                    cell.setCellStyle(dateTimeStyle);
                                } else if (rawValue instanceof java.time.ZonedDateTime zdt) {
                                    cell.setCellValue(java.util.Date.from(zdt.toInstant()));
                                    cell.setCellStyle(dateTimeStyle);
                                } else {
                                    // fallback to formatted text
                                    cell.setCellValue(def.textFormatter().apply(entity));
                                }
                            }
                        }

                        // 🔹 Auto-size columns
                        for (int i = 0; i < visibleColumns.size(); i++) {
                            sheet.autoSizeColumn(i);
                        }

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        workbook.write(out);
                        return new ByteArrayInputStream(out.toByteArray());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

                downloadLink.setHref(resource);
                downloadLink.getElement().callJsFunction("click");
            } catch (Exception e) {
                Notification.show("Export failed: " + e.getMessage(), 3000, Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        return new HorizontalLayout(exportButton, downloadLink);
    }
    
    private void configureLeaveRequestDetailDialog() {
        leaveRequstDetailDialog = new CustomDialog("Leave Request Information | ព័ត៌មានថ្ងៃឈប់សម្រាក");

        // Configure fields
        leaveType.setWidthFull();
        fromDate.setWidthFull();
        toDate.setWidthFull();
        leaveDuration.setWidthFull();
        leaveDuration.setItems(EmployeeLeaveDetail.LeaveDuration.values());
        leaveDuration.setItemLabelGenerator(EmployeeLeaveDetail.LeaveDuration::getLabel);
        
        leaveType.setItems(leaveTypeRepository.findAllById(List.of(1L,2L,3L,9L,10L,19L,21L,22L)));
        leaveType.setItemLabelGenerator(
        	    leave -> String.format("%s | %s",
        	        leave.getLeaveNameEn() != null ? leave.getLeaveNameEn() : "",
        	        leave.getLeaveNameKh() != null ? leave.getLeaveNameKh() : ""
        	    )
        );
        
        leaveType.addValueChangeListener(e -> {
        	if (e.getValue() == null) return;
            List<LeaveTypeSubType> subTypes = Optional.ofNullable(e.getValue())
                    .map(LeaveType::getLeaveTypeSubTypes)
                    .orElse(Collections.emptyList());
            
            boolean hasSubTypes =subTypes.size()>0? true:false;
            
            leaveTypeSubType.setVisible(hasSubTypes);
            leaveTypeSubType.setRequiredIndicatorVisible(hasSubTypes);
            leaveTypeSubType.clear();
            if( hasSubTypes) {
            	leaveTypeSubType.setItems(subTypes);
            }
            
        });


        
        leaveTypeSubType.setItemLabelGenerator(
        	    leave -> String.format("%s | %s",
            	        leave.getLeaveSubTypeNameEn() != null ? leave.getLeaveSubTypeNameEn() : "",
            	        leave.getLeaveSubTypeNameKh() != null ? leave.getLeaveSubTypeNameKh() : ""
            	    )
        	);

        numberOfDay.setWidthFull();
        numberOfDay.setReadOnly(true);
        
        // Bind fields to the entity
        leaveDetailBinder.forField(leaveType)
                .asRequired("Leave type is required")
                .bind(EmployeeLeaveDetail::getLeaveType, EmployeeLeaveDetail::setLeaveType);
        
        leaveDetailBinder.forField(leaveTypeSubType)
        .withValidator(
            subType -> {
                // Get currently selected leaveType
                LeaveType selectedType = leaveType.getValue();

                // If the selected type has subtypes, validation requires one selected
                if (selectedType != null && selectedType.getLeaveTypeSubTypes() != null
                        && !selectedType.getLeaveTypeSubTypes().isEmpty()) {
                    return subType != null; // must choose one
                }

                // No subtypes? Then field is optional
                return true;
            },
            "Leave sub type is required when available"
        )
        .bind(EmployeeLeaveDetail::getLeaveTypeSubType, EmployeeLeaveDetail::setLeaveTypeSubType);

        

        leaveDetailBinder.forField(fromDate)
                .asRequired("From date is required")
                .bind(EmployeeLeaveDetail::getFromDate, EmployeeLeaveDetail::setFromDate);

        leaveDetailBinder.forField(toDate)
                .asRequired("To date is required")
                .bind(EmployeeLeaveDetail::getToDate, EmployeeLeaveDetail::setToDate);

        leaveDetailBinder.forField(leaveDuration)
                .asRequired("Leave duration is required")
                .bind(EmployeeLeaveDetail::getLeaveDuration, EmployeeLeaveDetail::setLeaveDuration);
        
        leaveDetailBinder.forField(numberOfDay)
        .bind(
            detail -> detail.getNumberOfDay() != null ? detail.getNumberOfDay().doubleValue() : null,
            (detail, value) -> detail.setNumberOfDay(value != null ? value.floatValue() : null)
        );

        
        leaveDetailBinder.forField(remark)
        .bind(EmployeeLeaveDetail::getRemark, EmployeeLeaveDetail::setRemark);

     // Recalculate number of days whenever fromDate, toDate, or leaveDuration changes
        Runnable recalcDays = () -> {
            EmployeeLeaveDetail temp = new EmployeeLeaveDetail();
            temp.setFromDate(fromDate.getValue());
            temp.setToDate(toDate.getValue());
            temp.setLeaveDuration(leaveDuration.getValue());
            temp.calculateNumberOfDays(); // ✅ reuse backend logic
            numberOfDay.setValue(temp.getNumberOfDay() != null ? temp.getNumberOfDay().doubleValue() : null);
        };

        fromDate.addValueChangeListener(e -> toDate.setMin(e.getValue()));
        toDate.addValueChangeListener(e -> fromDate.setMax(e.getValue()));

        fromDate.addValueChangeListener(e -> recalcDays.run());
        toDate.addValueChangeListener(e -> recalcDays.run());
        leaveDuration.addValueChangeListener(e -> recalcDays.run());

        
        // Layout
        FormLayout form = new FormLayout(leaveType,leaveTypeSubType, fromDate, toDate, leaveDuration,numberOfDay,remark);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 2)
        );
        form.setColspan(remark, 2);
        form.setColspan(leaveTypeSubType, 2);

        // Buttons
        Button btnSave = new Button("Save", e -> {
            try {
                if (currentEditingDetail == null) {
                    // ➕ New record
                    EmployeeLeaveDetail newDetail = new EmployeeLeaveDetail();
                    leaveDetailBinder.writeBean(newDetail);
                    leaveDetails.add(newDetail);
                } else {
                    // ✏️ Edit existing record
                    leaveDetailBinder.writeBean(currentEditingDetail);
                }

                gridLeaveDetail.getDataProvider().refreshAll();
                leaveRequstDetailDialog.close();

                Notification.show("Leave detail saved successfully",
                        2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (ValidationException ex) {
                Notification.show("Validation failed: " + ex.getMessage(),
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });


        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button btnCancel = new Button("Cancel", e -> leaveRequstDetailDialog.close());
        btnCancel.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout buttons = new HorizontalLayout(btnSave, btnCancel);

        // Add everything
        VerticalLayout layout = new VerticalLayout(form, buttons);
        layout.setPadding(true);
        layout.setSpacing(true);
        leaveRequstDetailDialog.add(layout);
    }
    
	private Button createClearButton(SignaturePad pad) {
		    Button clearButton = new Button("Clear", e -> pad.clear());
		    clearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		    return clearButton;
	}
	 
	private Upload createImageUploadComponent(SignaturePad pad) {
		    InMemoryUploadHandler handler = UploadHandler.inMemory((metadata, data) -> {
		        if (!"image/jpeg".equalsIgnoreCase(metadata.contentType())) {
		            throw new IllegalArgumentException("Only JPEG images are allowed");
		        }

		            byte[] bytes = data;
		            String base64 = Base64.getEncoder().encodeToString(bytes);
		            String dataUrl = "data:image/jpeg;base64," + base64;
		            pad.loadImage(dataUrl);
		    });

		    Upload upload = new Upload(handler);
		    upload.setDropAllowed(true);
		    upload.setAcceptedFileTypes("image/jpeg");
		    upload.setMaxFiles(1);
		    upload.setMaxFileSize(5 * 1024 * 1024); // 5MB limit

		    upload.addFileRejectedListener(event -> 
		    	showError("Error: " + event.getErrorMessage())
		    );

		    return upload;
	}
	
	private void saveRequesterSignatureAttachment(EmployeeLeave leave, String dataUrl) {
	    if (dataUrl == null || dataUrl.isEmpty()) return;

	    try {
	        String base64Data = dataUrl.split(",")[1];
	        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
	        Long typeId = 1L; // “Requester Signature” AttachmentType ID

	        List<Attachment> existingList = service.findAttachmentsByEntityAndType(leave, typeId);
	        Attachment attachment;

	        if (!existingList.isEmpty()) {
	            // Update existing attachment
	            attachment = existingList.get(0);
	            attachment.setFileData(imageBytes);
	            attachment.setDataSize((long) imageBytes.length);
	            attachment.setUserUpdated(currentUserLogin.get());
	        } else {
	            // Create new attachment
	            attachment = new Attachment();
	            attachment.setFileName(UUID.randomUUID() + "_requester_signature.jpg");
	            attachment.setContentType("image/jpeg");
	            attachment.setDataSize((long) imageBytes.length);
	            attachment.setFileData(imageBytes);
	            attachment.setUserCreated(currentUserLogin.get());
	            attachment.setUserUpdated(currentUserLogin.get());
	            attachment.setAttachmentEntityTable(leave);

	            // ✅ fetch existing type from DB
	            org.halocambodia.data.AttachmentType type = attachmentTypeRepository.findById(typeId)
	                .orElseThrow(() -> new IllegalStateException("AttachmentType with ID " + typeId + " not found"));
	            attachment.setAttachmentType(type);
	        }

	        service.saveAttachment(attachment);

	    } catch (Exception ex) {
	        showError("Error saving signature: " + ex.getMessage());
	        ex.printStackTrace();
	    }
	}

	private void saveSupervisorSignatureAttachment(EmployeeLeave leave, String dataUrl) {
	    if (dataUrl == null || dataUrl.isEmpty()) return;

	    try {
	        String base64Data = dataUrl.split(",")[1];
	        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
	        Long typeId = 2L; // “Supervisor Signature” AttachmentType ID

	        List<Attachment> existingList = service.findAttachmentsByEntityAndType(leave, typeId);
	        Attachment attachment;

	        if (!existingList.isEmpty()) {
	            // Update existing attachment
	            attachment = existingList.get(0);
	            attachment.setFileData(imageBytes);
	            attachment.setDataSize((long) imageBytes.length);
	            attachment.setUserUpdated(currentUserLogin.get());
	        } else {
	            // Create new attachment
	            attachment = new Attachment();
	            attachment.setFileName(UUID.randomUUID() + "_supervisor_signature.jpg");
	            attachment.setContentType("image/jpeg");
	            attachment.setDataSize((long) imageBytes.length);
	            attachment.setFileData(imageBytes);
	            attachment.setUserCreated(currentUserLogin.get());
	            attachment.setUserUpdated(currentUserLogin.get());
	            attachment.setAttachmentEntityTable(leave);

	            org.halocambodia.data.AttachmentType type = attachmentTypeRepository.findById(typeId)
	                .orElseThrow(() -> new IllegalStateException("AttachmentType with ID " + typeId + " not found"));
	            attachment.setAttachmentType(type);
	        }

	        service.saveAttachment(attachment);

	    } catch (Exception ex) {
	        showError("Error saving supervisor signature: " + ex.getMessage());
	        ex.printStackTrace();
	    }
	}

	private void saveHrSignatureAttachment(EmployeeLeave leave, String dataUrl) {
	    if (dataUrl == null || dataUrl.isEmpty()) return;

	    try {
	        String base64Data = dataUrl.split(",")[1];
	        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
	        Long typeId = 3L; // “Supervisor Signature” AttachmentType ID

	        List<Attachment> existingList = service.findAttachmentsByEntityAndType(leave, typeId);
	        Attachment attachment;

	        if (!existingList.isEmpty()) {
	            // Update existing attachment
	            attachment = existingList.get(0);
	            attachment.setFileData(imageBytes);
	            attachment.setDataSize((long) imageBytes.length);
	            attachment.setUserUpdated(currentUserLogin.get());
	        } else {
	            // Create new attachment
	            attachment = new Attachment();
	            attachment.setFileName(UUID.randomUUID() + "_hr_signature.jpg");
	            attachment.setContentType("image/jpeg");
	            attachment.setDataSize((long) imageBytes.length);
	            attachment.setFileData(imageBytes);
	            attachment.setUserCreated(currentUserLogin.get());
	            attachment.setUserUpdated(currentUserLogin.get());
	            attachment.setAttachmentEntityTable(leave);

	            org.halocambodia.data.AttachmentType type = attachmentTypeRepository.findById(typeId)
	                .orElseThrow(() -> new IllegalStateException("AttachmentType with ID " + typeId + " not found"));
	            attachment.setAttachmentType(type);
	        }

	        service.saveAttachment(attachment);

	    } catch (Exception ex) {
	        showError("Error saving supervisor signature: " + ex.getMessage());
	        ex.printStackTrace();
	    }
	}
	

	 private void loadSignatureIfPresent(List<Attachment> attachments, long typeId, SignaturePad pad) {
		 pad.clear();
		    attachments.stream()
		        .filter(att -> att.getAttachmentType().getId() == typeId)
		        .findFirst()
		        .ifPresent(att -> {
		            try {
		                Path filePath = uploadDir.resolve(attachmentsSubDirectory + "/" + att.getFileName());
		                byte[] bytes = null;

		                if (att.getFileData() != null) {
		                	System.out.println("Signature" + typeId +" From Database");
		                	 bytes = att.getFileData();		                   
		                } else if (Files.exists(filePath)) {
		                	System.out.println("Signature" + typeId +filePath.toAbsolutePath());
		                	 bytes = Files.readAllBytes(filePath);
		                }
		                
		                if (bytes != null) {
		                    String base64 = Base64.getEncoder().encodeToString(bytes);
		                    String dataUrl = "data:image/jpeg;base64," + base64;
		                    //pad.loadImage(dataUrl);
		                    pad.getElement().getNode().runWhenAttached(ui -> {
		                        ui.beforeClientResponse(pad, context -> {
		                            //pad.getElement().executeJs("this.loadImage($0);", dataUrl);
		                            pad.loadImage(dataUrl);
		                        });
		                    });

		                    
		                }

		            } catch (IOException e) {
		                e.printStackTrace(); // consider using a logger
		            }
		        });
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
	 
	    @Override
	    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
	        return action == SensitiveAction.EDIT || action == SensitiveAction.DELETE;
	    }

	    @Override
	    protected void confirmPassword(SensitiveAction action, Runnable onSuccess) {
	        Dialog dlg = new Dialog();
	        dlg.setHeaderTitle("Confirm Password");

	        PasswordField pwd = new PasswordField("Password");
	        pwd.setWidthFull();
	        pwd.setRevealButtonVisible(true);
	        pwd.setRequired(true);

	        Span msg = new Span("Please enter your password to continue.");
	        msg.getStyle().set("font-size", "var(--lumo-font-size-s)");

	        Button cancel = new Button("Cancel", e -> dlg.close());
	        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

	        Button confirm = new Button("Confirm", e -> {
	            String raw = pwd.getValue();
	            if (raw == null || raw.isBlank()) {
	                pwd.setInvalid(true);
	                pwd.setErrorMessage("Password is required");
	                return;
	            }

	            User u = currentUserLogin.orElse(null);
	            if (u == null || u.getHashedPassword() == null) {
	                Notification.show("Cannot detect current user/password", 2500, Position.TOP_CENTER)
	                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
	                return;
	            }

	            boolean ok = passwordEncoder.matches(raw, u.getHashedPassword());
	            if (!ok) {
	                pwd.setInvalid(true);
	                pwd.setErrorMessage("Wrong password");
	                return;
	            }

	            dlg.close();
	            onSuccess.run();
	        });
	        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

	        // Enter key = confirm
	        confirm.addClickShortcut(Key.ENTER);
	        cancel.addClickShortcut(Key.ESCAPE);

	        VerticalLayout body = new VerticalLayout(msg, pwd);
	        body.setPadding(false);
	        body.setSpacing(true);
	        body.setWidthFull();

	        dlg.add(body);
	        dlg.getFooter().add(cancel, confirm);

	        dlg.open();

	        // focus password field
	        UI.getCurrent().access(pwd::focus);
	    }

}