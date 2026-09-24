package org.halocambodia.views.goal_setting;

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
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
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
import org.halocambodia.data.Positions;

import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
import org.halocambodia.data.FileSizeFormatter;
import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.data.Unit;
import org.halocambodia.data.UnitRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.GoalSettingEntryService;
import org.halocambodia.services.GoalSettingService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.MasterDetailLayout;
import org.halocambodia.views.MasterPageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;


@PageTitle("Supervisor Goal Review")
@Route(value = "goal-setting", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class GoalSupervisorView  extends MasterPageDialogLayout<GoalSetting, GoalSettingService> implements BeforeEnterObserver{
	
	// entity attribute	
    private DatePicker fromDate=new DatePicker("From Date | ថ្ងៃចាប់ផ្ដើម");
    private DatePicker toDate=new DatePicker("To Date | ថ្ងៃបញ្ចប់");
    private ComboBox<HREmployeeData> reportedBy=new ComboBox<HREmployeeData>("Staff ​​Name | ឈ្មោះបុគ្គលិក");
    private ComboBox<Positions> reportedPosition=new ComboBox<Positions>("Staff Position | មុខដំណែងបុគ្គលិក");
    private ComboBox<Branch> branch=new ComboBox<Branch>("Location | តំបន់");
    private ComboBox<Department> department=new ComboBox<Department>("Department​ | ផ្នែក");
    
    private DatePicker reportedDate=new DatePicker("Reported Date | ថ្ងៃរាយការណ៍");
    private TextArea developmentGoal=new TextArea("Development Goal | គោលដៅដើម្បីការអភិវឌ្ឍខ្លួន");
    private DatePicker developmentDueDate =new DatePicker("Development Goal Due Date | ថ្ងៃផុតកំណត់គោលដៅអភិវឌ្ឍន៍");
    private ComboBox<HREmployeeData> supervisor=new ComboBox<HREmployeeData>("Supervisor Name | ឈ្មោះអ្នកគ្រប់គ្រង");
    private ComboBox<Positions> supervisorPosition=new ComboBox<Positions>("Supervisor Positions | មុខដំណែងអ្នកគ្រប់គ្រង");
    private DatePicker supervisorQcAt=new DatePicker("Supervisor Qc At | អ្នកគ្រប់គ្រងធ្វើ QC នៅថ្ងៃ");
    private Checkbox supervisorQcYesNo=new Checkbox("Supervisor Qc Yes/No | តើអ្នកគ្រប់គ្រង QC ហើយឬនៅ?",e->{supervisorQcYesNoShowHide(e.getValue());});
    
    private TextField survey123ID=new TextField("Survey123 ID");
       
    //entity for workrelate goal
    
    private TextArea workGoal=new TextArea("Work-related Goals | គោលដៅការងារ");
    private DatePicker workGoalDate =new DatePicker("Work-related Goals (Due Date)​|ថ្ងៃផុតកំណត់ គោលដៅពាក់ព័ន្ធនឹងការងារនិមួយៗ");
    CustomDialog workRelateGoalDialog = new CustomDialog("Work-related Goals | ដាក់គោលដៅពាក់ព័ន្ធនឹងការងារនិមួយៗ");
    
    private final List<GoalSettingDetail> deletedGoalSettingDetails = new ArrayList<>();
  
    //share data
    private final HREmployeeRepository hrEmployeeRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final Optional<User> currentUserLogin;
    private final AttachmentRepository attachmentRepository;
    
    private List<GoalSettingDetail> goalSettingDetails;
    private GoalSettingDetail editingDetail;
       
    private final BeanValidationBinder<GoalSetting> binder= new BeanValidationBinder<>(GoalSetting.class);
   
//Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");
    private DatePicker advanceFilterFromDateFrom=new DatePicker("From Date(From)");
    private DatePicker advanceFilterFromDateDateTo=new DatePicker("From Date(To)");
    
    private DatePicker advanceFilterToDateFrom=new DatePicker("To Date(From)");
    private DatePicker advanceFilterToDateTo=new DatePicker("To Date(To)");
    private TextField advanceFilterSurveyName=new TextField(this.supervisor.getLabel());
    private MultiSelectComboBox<Positions> advanceFilterSurveyPosition=new MultiSelectComboBox<Positions>(this.supervisorPosition.getLabel());
    private MultiSelectComboBox<Branch> advanceFilterSurveyBranch=new MultiSelectComboBox<Branch>(this.branch.getLabel());
    private MultiSelectComboBox<Department> advanceFilterDepartment=new MultiSelectComboBox<Department>(this.department.getLabel());    
    private DatePicker advanceFilterReportedDateFrom=new DatePicker("Reported Date(From)");
    private DatePicker advanceFilterReportedDateTo=new DatePicker("Reported Date(To)");
    
    private TextField advanceFilterDevelopmentGoal=new TextField(this.developmentGoal.getLabel());
    private DatePicker advanceFilterDevelopmentGoalDualDateFrom=new DatePicker("Development Goal Dua Date(From)");
    private DatePicker advanceFilterDevelopmentGoalDualDateTo=new DatePicker("Development Goal Dua Date(To)");
    
    private TextField advanceFilterSupervisorName=new TextField(this.supervisor.getLabel());
    private MultiSelectComboBox<Positions> advanceFilterSupervisorPosition=new MultiSelectComboBox<Positions>(this.supervisorPosition.getLabel());
    private ComboBox<String> advanceFilterSupervisorQcYesNo=new ComboBox("Supervisor Qc Yes/No | តើអ្នកគ្រប់គ្រង QC ហើយឬនៅ?","Yes","No");
    private DatePicker advanceFilterSupervisorQcAtFrom=new DatePicker("Supervisor Qc At(From)");
    private DatePicker advanceFilterSupervisorQcAtTo=new DatePicker("Supervisor Qc At(To)");
      
    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");
    
    private  Path uploadDir;
    private static final String attachmentsSubDirectory="attachments";
    private final FileUploadUtility fileUploadUtility ;
    
    SignaturePad signaturePadReporter = new SignaturePad();
    VerticalLayout reporterSig = new VerticalLayout(new Span("Reporter Signature | ហត្ថលេខារបស់អ្នករាយការណ៍"), signaturePadReporter, new HorizontalLayout(createClearButton(signaturePadReporter),createImageUploadComponent(signaturePadReporter) ));
    
    SignaturePad signaturePadSupervisor = new SignaturePad();
    VerticalLayout supervisorSig = new VerticalLayout(new Span("Supervisor Signature | ហត្ថលេខារបស់អ្នកគ្រប់គ្រង"),signaturePadSupervisor, new HorizontalLayout(createClearButton(signaturePadSupervisor), createImageUploadComponent(signaturePadSupervisor)));
    
	Grid<GoalSettingDetail> gridWorkGoal =new Grid <>(GoalSettingDetail.class, false);  
	private final BeanValidationBinder<GoalSettingDetail> workGoalBinder = new BeanValidationBinder<>(GoalSettingDetail.class);

	 public GoalSupervisorView(GoalSettingService service,UserService userService,AuthenticatedUser authenticatedUser,HREmployeeRepository hrEmployeeRepository,BranchRepository branchRepository,DepartmentRepository departmentRepository,PositionRepository positionRepository,FileUploadUtility fileUploadUtility,AttachmentRepository attachmentRepository) { 
		 	super(service,userService,authenticatedUser);
		 	this.currentUserLogin=authenticatedUser.get();
		 	this.attachmentRepository=attachmentRepository;
		 	
		 	this.hrEmployeeRepository=hrEmployeeRepository;		 
		 	this.branchRepository=branchRepository;
		 	this.departmentRepository=departmentRepository;
		 	this.positionRepository=positionRepository;
		 	
		 	this.fileUploadUtility=fileUploadUtility;
		 	uploadDir=fileUploadUtility.initializeUploadDirectory("hr", "");
		 	
        	configureGrid();
	        configureEditorLayout();    	        
	        binderField();
	       	createAdvanceFilterLayout(); 
		 	 
		 	btnAddNew.setVisible(false);
		 	

	 }
	 private void supervisorQcYesNoShowHide(Boolean checked) {
		 supervisorQcAt.setVisible(checked);
		 supervisorQcAt.setRequired(checked);
		 
		 supervisorSig.setVisible(checked);
		 if (checked && supervisorQcAt==null) {			 
			 supervisorQcAt.setErrorMessage("supervisorQcAt cannot be null or empty.");
			 supervisorQcAt.setInvalid(checked); 
		 } else {
			 supervisorQcAt.setInvalid(false); 
		 }
		 
	 }
	 
	 @Override
	 protected void onAttach(AttachEvent attachEvent) {
	        super.onAttach(attachEvent);	        
	        UI.getCurrent().access(() -> { 
		        reloadEditorData();
		        reloadAdvanceFilterData();
	        });
	}
	 
	
	private void binderField() {
		binder.bindInstanceFields(this);
		binder.forField(supervisorQcAt)
	    .withValidator(date -> {
	        Boolean qcYes = supervisorQcYesNo.getValue();
	        // if checked Yes, then date must not be null
	        return !(qcYes != null && qcYes && date == null);
	    }, "Supervisor QC Date is required when 'Yes' is selected")
	    .bind(GoalSetting::getSupervisorQcAt, GoalSetting::setSupervisorQcAt);

	}


	@Override
	 protected void configureGrid() {
	    	grid.addColumn(GoalSetting::getId).setHeader("ID").setFooter("Total Records:").setKey("id");	          
	        grid.addColumn(entityRow -> {        	
	        	return entityRow.getFromDate() != null  ? DateTimeUtilFormart.DATE_FORMATTER.format(entityRow.getFromDate()) : "";
	        }).setHeader(this.fromDate.getLabel()) 
	        .setSortProperty("fromDate").setKey("fromDate");
	        grid.addColumn(entityRow -> {        	
	        	return entityRow.getToDate() != null  ? DateTimeUtilFormart.DATE_FORMATTER.format(entityRow.getToDate()) : "";
	        }).setHeader(this.toDate.getLabel()) 
	        .setSortProperty("toDate").setKey("toDate");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getReportedBy() != null ? (entitySelected.getReportedBy().getNameEn()) + " - "+ entitySelected.getReportedBy().getInsurance() : ""; 
	        }).setHeader(this.reportedBy.getLabel()).setSortProperty("reportedBy.insurance").setKey("reportedBy.insurance");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getReportedPosition() != null ? entitySelected.getReportedPosition().getPosition() : ""; 
	        }).setHeader(this.reportedPosition.getLabel()).setSortProperty("reportedPosition.position").setKey("reportedPosition.position");
	        
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getBranch() != null ? entitySelected.getBranch().getBranchShortName() : ""; 
	        }).setHeader(this.branch.getLabel()).setSortProperty("branch.branchShortName").setKey("branch.branchShortName");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getDepartment() != null ? entitySelected.getDepartment().getName() : ""; 
	        }).setHeader(this.department.getLabel()).setSortProperty("department.name").setKey("department.name");
	        	        	        
	        grid.addColumn(entityRow -> {        	
	        	return entityRow.getReportedDate() != null  ? DateTimeUtilFormart.DATE_FORMATTER.format(entityRow.getReportedDate()) : "";
	        }).setHeader(this.reportedDate.getLabel()) 
	        .setSortProperty("reportedDate").setKey("reportedDate");
	        
	        grid.addColumn(new ComponentRenderer<Component, GoalSetting>(goal -> {
	            Span span = new Span(goal.getDevelopmentGoal());
	            span.getStyle().set("white-space", "pre-wrap");     // allow wrapping
	            span.getStyle().set("max-width", "400px");          // control column width
	            return span;
	        }))
	        .setHeader(this.developmentGoal.getLabel())
	        .setSortProperty("developmentGoal")
	        .setKey("developmentGoal")
	        .setAutoWidth(true);
	        
	        grid.addColumn(entityRow -> {        	
	        	return entityRow.getDevelopmentDueDate() != null  ? DateTimeUtilFormart.DATE_FORMATTER.format(entityRow.getDevelopmentDueDate()) : "";
	        }).setHeader(this.developmentDueDate.getLabel()) 
	        .setSortProperty("developmentDueDate").setKey("developmentDueDate");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getSupervisor() != null ? (entitySelected.getSupervisor().getNameEn()) + " - "+ entitySelected.getSupervisor().getInsurance() : ""; 
	        }).setHeader(this.supervisor.getLabel()).setSortProperty("supervisor.insurance").setKey("supervisor.insurance");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getSupervisorPosition() != null ? entitySelected.getSupervisorPosition().getPosition() : ""; 
	        }).setHeader(this.supervisorPosition.getLabel()).setSortProperty("supervisorPosition.position").setKey("supervisorPosition.position");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getSupervisorQcYesNo() != null ? (entitySelected.getSupervisorQcYesNo().equals(true)? "Yes":"No")  : ""; 
	        }).setHeader(this.supervisorQcYesNo.getLabel()).setSortProperty("supervisorQcYesNo").setKey("supervisorQcYesNo");
	        
	        grid.addColumn(entityRow -> {        	
	        	return entityRow.getSupervisorQcAt() != null  ? DateTimeUtilFormart.DATE_FORMATTER.format(entityRow.getSupervisorQcAt()) : "";
	        }).setHeader(this.supervisorQcAt.getLabel()) 
	        .setSortProperty("supervisorQcAt").setKey("supervisorQcAt");
	        	        
	        grid.addColumn(GoalSetting::getSurvey123ID).setHeader(this.survey123ID.getLabel()).setKey("survey123ID");		       
	        
	        grid.addColumn(entityRowUserCreated -> {
	            User userCreated = entityRowUserCreated.getUserCreated(); // Get the related User object
	            return userCreated != null ? userCreated.getName() : ""; // Display the username or a default value
	        }).setHeader("Created by")
	        .setSortProperty("userCreated.name").setKey("userCreated.name");
	        
	        grid.addColumn(entityRowUserCreatedAt -> {        	
	        	return entityRowUserCreatedAt.getCreatedAt() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRowUserCreatedAt.getCreatedAt()) : "";
	        }).setHeader("Created At") 
	        .setSortProperty("createdAt").setKey("createdAt");
	        
	        grid.addColumn(entityRowUserUpdated -> {
	            User userUpdated = entityRowUserUpdated.getUserUpdated(); // Get the related User object
	            return userUpdated != null ? userUpdated.getName() : ""; // Display the username or a default value
	        }).setHeader("Updated by")
	        .setSortProperty("userUpdated.name").setKey("userUpdated.name");
	        
	        grid.addColumn(entityRowUserUpdateddAt -> {        	
	        	return entityRowUserUpdateddAt.getCreatedAt() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRowUserUpdateddAt.getUpdatedAt()) : "";
	        }).setHeader("Updated At") 
	        .setSortProperty("updatedAt").setKey("updatedAt");

	        grid.getColumns().forEach(column -> {
	            column.setResizable(true);   // Enable resizing for all columns
	            column.setSortable(true);    // Enable sorting for all columns
	            //column.setTextAlign(ColumnTextAlign.CENTER);
	            if(column.getHeaderText() !="ID") {
	            	 column.setAutoWidth(true);
	            }
	           
	        });
	        
	        grid.setItemDetailsRenderer(this.createTabRenderer());
	        createShowHideColumnGridToolBar();   
	        
	        grid.setAllRowsVisible(true);
	        
	 }
	
	private ComponentRenderer<Component, GoalSetting> createTabRenderer() {
        return new ComponentRenderer<>(entityRecord -> {
        	
        	TabSheet tabSheet = new TabSheet();  
        	tabSheet.add("Work-related Goals and Development Goal",this.workRelatedGoalsTab(entityRecord));
        	tabSheet.add("Attachments",this.attachmentsTab(entityRecord));
        	
        	tabSheet.getStyle().set("border", "1px solid #ccc");
        	tabSheet.setWidthFull();

            return tabSheet;
        });
	}
	public Component attachmentsTab(GoalSetting entity) {
	    if (entity == null) {
	        return new Span("No attachments available.");
	    }

	    List<Attachment> attachments = attachmentRepository.findByEntity(entity);
	    entity.setAttachments(attachments);

	    if (attachments == null || attachments.isEmpty()) {
	        return new Span("No attachments found for this record.");
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


	    gridAttachment.setWidthFull();
	    gridAttachment.setAllRowsVisible(true);

	    return gridAttachment;
	}

	
	private Component workRelatedGoalsTab(GoalSetting entityRecord) {
    	Grid<GoalSettingDetail> gridDevelopmentGoal =new Grid <>(GoalSettingDetail.class, false);
    	gridDevelopmentGoal.addColumn(GoalSettingDetail::getId)
        .setHeader("ID")
        .setFooter("Total Records:")
        .setKey("id");
    	    	
    	gridDevelopmentGoal.addColumn(new ComponentRenderer<Component, GoalSettingDetail>(goal -> {
            Span span = new Span(goal.getWorkGoal());
            span.getStyle().set("white-space", "pre-wrap");     // allow wrapping
            span.getStyle().set("max-width", "400px");          // control column width
            return span;
        }))
        .setHeader("Work-related Goals")
        .setSortProperty("workGoal")
        .setKey("workGoal")
        .setAutoWidth(true);
    	

    	gridDevelopmentGoal.addColumn(entitySelected -> {
	        return entitySelected.getWorkGoalDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(entitySelected.getWorkGoalDate()) : "";
	    })
        .setHeader("Work-related Goals (Due Date)")
        .setKey("workGoalDate");
    	
    	gridDevelopmentGoal.addColumn(GoalSettingDetail::getSurvey123ID).setHeader(this.survey123ID.getLabel()).setKey("survey123ID");	
    	
    	
    	gridDevelopmentGoal.addColumn(entitySelected -> {
	        return entitySelected.getUserCreated() != null ? entitySelected.getUserCreated().getName() : "";
	    }).setHeader("Created by")
	      .setSortProperty("userCreated.name")
	      .setKey("userCreated.name");

    	gridDevelopmentGoal.addColumn(entitySelected -> {
	        return entitySelected.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getCreatedAt()) : "";
	    }).setHeader("Created At")
	      .setSortProperty("createdAt")
	      .setKey("createdAt");

    	gridDevelopmentGoal.addColumn(entitySelected -> {
	        return entitySelected.getUserUpdated() != null ? entitySelected.getUserUpdated().getName() : "";
	    }).setHeader("Updated by")
	      .setSortProperty("userUpdated.name")
	      .setKey("userUpdated.name");

    	gridDevelopmentGoal.addColumn(entitySelected -> {
	        return entitySelected.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getUpdatedAt()) : "";
	    }).setHeader("Updated At")
	      .setSortProperty("updatedAt")
	      .setKey("updatedAt");
    	
    	gridDevelopmentGoal.setItems(entityRecord.getGoalSettingDetails());
    	gridDevelopmentGoal.getColumnByKey("id").setFooter("Total Records: " + entityRecord.getGoalSettingDetails().size());
    	
    	gridDevelopmentGoal.getColumns().forEach(column -> {
            column.setResizable(true);   // Enable resizing for all columns
            column.setSortable(true);    // Enable sorting for all columns
            column.setTextAlign(ColumnTextAlign.CENTER);
            if(column.getHeaderText() !="ID") {
            	 column.setAutoWidth(true);
            }
           
        });
    	
    	//VerticalLayout statusLayout =new VerticalLayout(new HorizontalLayout( new Button("Full View", new Icon(VaadinIcon.EXPAND_FULL),ee-> { UI.getCurrent().navigate("goal-setting-detail/" + entityRecord.getId().toString());})), gridDevelopmentGoal);
    	//statusLayout.setMargin(false);
    	//statusLayout.setPadding(false);
    	return gridDevelopmentGoal;
	}
	

	 @Override
	 protected void configureEditorLayout() {

		 VerticalLayout  formLayout = new VerticalLayout ();
	     formLayout.setSizeFull();

	        VerticalLayout detailGoalLayout=createWorkGoal();	       
	        detailGoalLayout.getStyle().set("border", "3px solid rgb(232, 125, 30)").set("border-radius", "15px"); // Optional: rounded corners
	        
	        //++++++++++++++++++++++++++++++Staff Info++++++++++++++++++++++++ 
	        H4 staffInfoTitle=new H4("Staff information | ព័ត៌មានបុគ្គលិក");

	        FormLayout formStaffinfo=new FormLayout(staffInfoTitle,this.fromDate,this.toDate,this.reportedBy,this.reportedPosition, reportedDate,this.branch,this.department,reporterSig);
	        formConfigProperty(formStaffinfo);
	        formStaffinfo.setColspan(staffInfoTitle, 4);
	        //++++++++++++++++++++++++++++++Development Goal Info++++++++++++++++++++++++        	        	       
	        H4 DevelopmentGoalTitle=new H4("Development Goal | គោលដៅដើម្បីការអភិវឌ្ឍខ្លួន");
	        FormLayout formDevelopmentGoal=new FormLayout(DevelopmentGoalTitle, developmentGoal,developmentDueDate);
	        formConfigProperty(formDevelopmentGoal);

	        formDevelopmentGoal.setColspan(DevelopmentGoalTitle, 4);
	        formDevelopmentGoal.setColspan(developmentGoal, 4);
	        developmentGoal.getStyle().set("resize", "both");
	        developmentGoal.getStyle().set("overflow", "auto");

	        //++++++++++++++++++++++++++++++Development Goal Info++++++++++++++++++++++++        	        	       
	        H4 SupervisorInfoTitle=new H4("Supervisor Information | ព័ត៌មានអ្នកគ្រប់គ្រង");
	        FormLayout formSupervisorInfo=new FormLayout(SupervisorInfoTitle, supervisor,this.supervisorPosition,this.supervisorQcYesNo,this.supervisorQcAt,supervisorSig);
	        formConfigProperty(formSupervisorInfo);	        
	        formSupervisorInfo.setColspan(SupervisorInfoTitle, 4);

	        H4 formDescription=new H4("Work with your manager to agree on 2 work-related goals for this year. Then, write 1 personal development goal that will help you achieve those 2 work goals.\r\n"
	        		+ "Your goals should be SMART (specific, measurable, actionable, realistic, timebound).\r\n"
	        		+ "ពិភាក្សាជាមួយប្រធានគ្រប់គ្រងរបស់អ្នកក្នុងការឯកភាពគ្នាកំណត់គោលដៅ២សម្រាប់ឆ្នាំនេះ។ បន្ទាប់មក កំណត់គោលដៅមួយទៀតដើម្បីធ្វើអោយមានការអភិវឌ្ឍខ្លួនដើម្បីជួយអ្នកក្នុងការសម្រេចបានគោលដៅការងារទាំង២នោះ។\r\n"
	        		+ "គោលដៅរបស់អ្នក គួរតែមាននៅក្នុងភាពវៀងវៃ (ជាក់លាក់, អាចវាស់វែងបាន, មានសកម្មភាព, ពិតប្រាកដ, ពេលវេលាជាក់លាក់)");
	        formDescription.getStyle().set("color", "blue");
	        
	        formLayout.add(formDescription, formStaffinfo,formDevelopmentGoal,detailGoalLayout,formSupervisorInfo);
	        HorizontalLayout buttonLayout = new HorizontalLayout();
	        buttonLayout.setClassName("button-layout");
	        
	        Button btnCancel=new Button("Cancel | បោះបង់",e ->{closeForm();clearForm();});
	        btnCancel.setIcon(new Icon(VaadinIcon.CLOSE));
	        btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_ERROR);
	        btnCancel.addClickShortcut(Key.ESCAPE);
	        
	        Button btnSave =new Button("Save | រក្សាទុក", e -> save());	        
	        btnSave.setIcon(new Icon(VaadinIcon.CHECK));
	        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_SUCCESS);
	        btnSave.addClickShortcut(Key.ENTER);

	        buttonLayout.add(btnSave, btnCancel);
	        
	        editorLayout.setDialogTitle("Supervisor Goal Review | ការត្រួតពិនិត្យគោលដៅដោយអ្នកគ្រប់គ្រង");       
	        
	        editorLayout.add(formLayout); 
	        editorLayout.getFooter().add(buttonLayout); 


	        this.reportedBy.setItemLabelGenerator( employee -> employee.getNameEn() + " - " + employee.getInsurance());
	        reportedBy.setReadOnly(true);
	        
	        this.reportedPosition.setItemLabelGenerator(Positions::getPosition);
	        	        
	        this.branch.setItemLabelGenerator( Branch::getBranchShortName);
	        	        
	        this.department.setItemLabelGenerator(Department::getName);
	        
	        this.supervisor.setItemLabelGenerator( employee -> employee.getNameEn() + " - " + employee.getInsurance());
	               
	        this.supervisorPosition.setItemLabelGenerator(Positions::getPosition);

	 }
	 private void formConfigProperty(FormLayout frm) {
		 frm.getStyle().set("border", "3px solid rgb(232, 125, 30)").set("border-radius", "15px").set("padding", "15px");
		 frm.setResponsiveSteps(
	        	    new FormLayout.ResponsiveStep("0", 1),//Extra small, Small, Medium
	        	    new FormLayout.ResponsiveStep("992px", 2),//Large
	        	    new FormLayout.ResponsiveStep("1200px", 3),//X-Large
	        	    new FormLayout.ResponsiveStep("1400px", 4) //XX-Large
	    );
	 }
	 
	 private VerticalLayout createWorkGoal() {
		    VerticalLayout layout = new VerticalLayout();
		    
		    gridWorkGoal.addColumn(new ComponentRenderer<>(detail -> {
		        Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
		        Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));

		        editBtn.addThemeVariants(ButtonVariant.LUMO_ICON,ButtonVariant.LUMO_TERTIARY_INLINE);
		        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);

		        editBtn.getElement().setProperty("title", "Edit | កែសម្រូល");
		        deleteBtn.getElement().setProperty("title", "Delete  | លុប");

		        HorizontalLayout actions = new HorizontalLayout(editBtn, deleteBtn);
		        actions.setSpacing(false);  // Remove spacing between buttons
		        actions.setPadding(false);  // Remove padding in layout
		        actions.setMargin(false);   // Remove margin in layout
		        actions.getStyle().set("padding", "0");  // Ensure no extra padding
		        actions.getStyle().set("margin", "0");   // Ensure no extra margin
		        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

		        editBtn.addClickListener(e -> {
		            workGoalBinder.readBean(detail);
		            editingDetail = detail;
		            workRelateGoalDialog.setHeight("50%");
		            workRelateGoalDialog.open();
		           
		        });

		        deleteBtn.addClickListener(e -> { 
		            goalSettingDetails.remove(detail);
		            entity.getGoalSettingDetails().remove(detail);
		            if (detail.getId() != null) {
		                deletedGoalSettingDetails.add(detail); // Track for DB deletion
		            }		            
		            gridWorkGoal.setItems(goalSettingDetails);
		        });

		        return actions;
		    })).setHeader("Actions").setAutoWidth(true);

		    gridWorkGoal.addColumn(GoalSettingDetail::getId)
		        .setHeader("ID")
		        //.setFooter("Total Records:")
		        .setKey("id");

		    gridWorkGoal.addColumn(new ComponentRenderer<>(goal -> {
		        Span span = new Span(goal.getWorkGoal());
		        span.getStyle().set("white-space", "pre-wrap");
		        span.getStyle().set("max-width", "400px");
		        return span;
		    }))
		        .setHeader("Work-related Goals")
		        .setSortProperty("workGoal")
		        .setKey("workGoal")
		        .setAutoWidth(true);

		    gridWorkGoal.addColumn(goal -> {
		        return goal.getWorkGoalDate() != null ? DateTimeUtilFormart.DATE_FORMATTER.format(goal.getWorkGoalDate()) : "";
		    })
		        .setHeader("Work-related Goals (Due Date)")
		        .setKey("workGoalDate");
		    
		    gridWorkGoal.getColumns().forEach(column -> {
	            column.setResizable(true);   // Enable resizing for all columns
	            column.setSortable(true);    // Enable sorting for all columns
	            //column.setTextAlign(ColumnTextAlign.CENTER);
	            if(column.getHeaderText() !="ID") {
	            	 column.setAutoWidth(true);
	            }
	           
	        });



		    FormLayout formWorkRelateGoals = new FormLayout(workGoal, workGoalDate);
		    formWorkRelateGoals.setColspan(workGoal, 2);
		    workGoal.getStyle().set("resize", "both");
		    workGoal.getStyle().set("overflow", "auto");

		    // Bind form fields to GoalSettingDetail
		    workGoalBinder.bindInstanceFields(this);

		    Button btnCancel = new Button("Cancel", e -> workRelateGoalDialog.close());
		    Button btnSave = new Button("រក្សាទុក | Save", e -> {
		    	try {
			        saveWorkRelateGoal();
			        workRelateGoalDialog.close();
			       
		    	} catch (ValidationException ev) {
		    	    showError("Please fill in required fields correctly. | សូមបំពេញសំនួរដែលត្រូវបញ្ចូលអោយបានត្រឹមត្រូវ។" + ev.getMessage());
	            } catch (Exception ex) {
	                showError("Unexpected error: " + ex.getMessage());
	            }

		    });

		    btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_WARNING);
		    btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
		    btnSave.setIcon(new Icon(VaadinIcon.BRIEFCASE));
		    btnSave.getElement().setProperty("title", "ចុចដើម្បីរក្សាទុកគោលដៅពាក់ព័ន្ធនឹងការងារនេះ | Click to save this work-related goal");
		    

		    HorizontalLayout buttonLayout = new HorizontalLayout(btnSave, btnCancel);
		    workRelateGoalDialog.add(formWorkRelateGoals);
		    workRelateGoalDialog.getFooter().add(buttonLayout);

		    Button addButton = new Button("Add Work-related Goal | បន្ថែមគោលដៅពាក់ព័ន្ធនឹងការងារ", e -> {
		    	 editingDetail = null;
		        workGoalBinder.readBean(new GoalSettingDetail()); // reset fields
		        workRelateGoalDialog.setHeight("50%");
		        workRelateGoalDialog.open();
		    });
		    
		    addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		    addButton.setIcon(new Icon(VaadinIcon.PLUS));
		    

		    layout.add(new H4("Work-related Goals | គោលដៅការងារ "),addButton, gridWorkGoal);
		    return layout;
		}

	 

	 private void saveWorkRelateGoal() throws ValidationException{
		        if (editingDetail == null) {
		            editingDetail = new GoalSettingDetail();
		        }
		        workGoalBinder.writeBean(editingDetail); // Validate and populate

		        if (goalSettingDetails == null) {
		            goalSettingDetails = new ArrayList<>();
		        }

		        editingDetail.setGoalSetting(entity);
		        editingDetail.setUserCreated(currentUserLogin.orElse(null));
		        editingDetail.setUserUpdated(currentUserLogin.orElse(null));
		        editingDetail.setCreatedAt(ZonedDateTime.now());
		        editingDetail.setUpdatedAt(ZonedDateTime.now());
		        
		        if (!goalSettingDetails.contains(editingDetail)) {
		            goalSettingDetails.add(editingDetail);
		        }

		        gridWorkGoal.setItems(goalSettingDetails); // Refresh
		        gridWorkGoal.getDataProvider().refreshAll();
		        
		        editingDetail = null; // clear after save
		        

		        Notification.show("Work-related goal saved | បានរក្សាទុកគោលដៅពាក់ព័ន្ធនឹងការងាររួចរាល់ហើយ", 1500, Position.TOP_CENTER)
		            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

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
		        Notification.show("Error: " + event.getErrorMessage(), 3000, Notification.Position.MIDDLE)
		            .addThemeVariants(NotificationVariant.LUMO_ERROR)
		    );

		    return upload;
		}
	 private Button createClearButton(SignaturePad pad) {
		    Button clearButton = new Button("Clear", e -> pad.clear());
		    clearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		    return clearButton;
		}
	 private void save() {
		    if (entity == null) {
		        showError("No entity to save.");
		        return;
		    }

		    try {
		    	/*if(supervisorQcYesNo.getValue()==true) {
		    		if(supervisorQcAt.getValue()==null) {		    			
		    			supervisorQcAt.setErrorMessage("Supervisor QC Date is required");
		    			supervisorQcAt.setInvalid(true);
		    			return;
		    		}
		    	}
		    	*/
		    	entity.setGoalSettingDetails(goalSettingDetails);

		        binder.writeBean(entity);

		        // STEP 1: Save the entity first (get ID)
		        entity = service.update(entity); // This gives you an ID

		        // STEP 2: Now that entity has ID, set attachments
		        signaturePadReporter.getImageData(reporterDataUrl -> {
		            signaturePadSupervisor.getImageData(supervisorDataUrl -> {
		                setSignatureImage(entity, 1L, reporterDataUrl);
		                setSignatureImage(entity, 2L, supervisorDataUrl);

		                // STEP 3: Update again with attachments
		                service.update(entity);
		                
		                // Now delete removed GoalSettingDetail entries
		                deletedGoalSettingDetails.forEach(detail -> {
		                    if (detail.getId() != null) {
		                        service.deleteGoalSettingDetail(detail.getId()); // You'll implement this
		                    }
		                });
		                deletedGoalSettingDetails.clear(); // Reset after deletion

		                Notification.show("Data saved successfully | ទិន្នន័យបានរក្សាទុកដោយជោគជ័យ", 1000, Position.TOP_CENTER)
		                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

		                clearForm();
		                closeForm();
		                refreshGrid();
		            });
		        });

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

	 private void setSignatureImage(GoalSetting entity, Long attachmentTypeId, String dataUrl) {
		    System.out.println("Signature " + attachmentTypeId + ": " + dataUrl);

		    List<Attachment> currentAttachments = attachmentRepository.findByEntityAndType(entity, attachmentTypeId);

		    if (dataUrl == null || !dataUrl.contains(",")) {
		        attachmentRepository.deleteAll(currentAttachments);
		        return;
		    }

		    byte[] imageBytes = Base64.getDecoder().decode(dataUrl.split(",", 2)[1]);
		    ZonedDateTime now = ZonedDateTime.now();

		    if (!currentAttachments.isEmpty()) {
		        // update
		        Attachment attachment = currentAttachments.get(0);
		        attachment.setFileData(imageBytes);
		        attachment.setDataSize((long) imageBytes.length);
		        attachment.setUserUpdated(currentUserLogin.orElse(null));
		        attachment.setUpdatedAt(now);
		        attachmentRepository.save(attachment);
		    } else {
		        // create
		        Attachment newAttachment = new Attachment();
		        newAttachment.setAttachmentType(fileUploadUtility.getAttachmentTypeById(attachmentTypeId));
		        newAttachment.setFileName(UUID.randomUUID() + "_employee_signature.jpg");
		        newAttachment.setContentType("image/jpeg");
		        newAttachment.setFileData(imageBytes);
		        newAttachment.setDataSize((long) imageBytes.length);
		        newAttachment.setAttachmentEntityTable(entity);
		        newAttachment.setUserCreated(currentUserLogin.orElse(null));
		        newAttachment.setCreatedAt(now);
		        newAttachment.setUserUpdated(currentUserLogin.orElse(null));
		        newAttachment.setUpdatedAt(now);

		        attachmentRepository.save(newAttachment);
		        //entity.getAttachments().add(newAttachment);
		        //entity.setAttachments(List.of(newAttachment));

		    }
		}


	 
	 @Override
	 protected void populateForm(GoalSetting entity) {
	     binder.readBean(entity); // Populate the form using the binder
	     editorLayout.open();
	     if (entity != null) {
	    	    // 1. Load attachments manually
	    	    List<Attachment> attachments = attachmentRepository.findByEntity(entity);
	    	    entity.setAttachments(attachments);
	    	    
	            loadSignatureIfPresent(attachments, 1L, signaturePadReporter);
	            loadSignatureIfPresent(attachments, 2L, signaturePadSupervisor);
	            
	            goalSettingDetails=entity.getGoalSettingDetails();
	     }
	     
	     gridWorkGoal.setItems(goalSettingDetails);
	     gridWorkGoal.getDataProvider().refreshAll();

	     //editorLayout.setVisible(true); // Show the editor layout
	    
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

	 
	    
	    private void clearForm() {
	        //populateForm(null);
	    	 binder.readBean(null);
	    	 this.entity = null;
	    	 signaturePadReporter.clear(); // <-- Add this
	    	 signaturePadSupervisor.clear(); // <-- Add this
	    }
	    private void closeForm() {
	    	//editorLayout.setVisible(false);
	    	editorLayout.close();
	    	//this.entity=null;
	    }
	    
	    
	    @Override
	    protected Specification<GoalSetting> buildCombinedSpecification() {
	        return (root, query, criteriaBuilder) -> {
	        	try {
		            List<Predicate> predicates = new ArrayList<>();
		            List<String> sqlFilter = new ArrayList<>();
	
	
		                     	            
		            Join<GoalSetting, User> userCreatedJoin = root.join("userCreated", JoinType.LEFT);
		            Join<GoalSetting, User> userUpdatedJoin = root.join("userUpdated", JoinType.LEFT);	 
		            Join<GoalSetting, HREmployeeData> supervisorJoin = root.join("supervisor", JoinType.INNER);	
		            Join<GoalSetting, Positions> supervisorPositionJoin = root.join("supervisorPosition", JoinType.LEFT);
		            Join<GoalSetting, HREmployeeData> employeeJoin = root.join("reportedBy", JoinType.INNER);
		            Join<GoalSetting, Positions> employeePositionJoin = root.join("reportedPosition", JoinType.LEFT);
		            Join<GoalSetting, Branch> branchJoin = root.join("branch", JoinType.LEFT);
		            Join<GoalSetting, Department> departmentJoin = root.join("department", JoinType.LEFT);
		            
		            
		            
		            //Default Filter
		            predicates.add(criteriaBuilder.equal(supervisorJoin.get("insurance"),currentUserLogin.get().getInsurance()));
		            predicates.add(criteriaBuilder.equal(root.get("supervisorQcYesNo"),false));
		            
		       	            
		            // Quick Search Filter
		            String quickSearchValue = txtQuick.getValue();
		            if (quickSearchValue != null && !quickSearchValue.isEmpty()) {
		                String likePattern = "%" + quickSearchValue.toLowerCase().trim() + "%";	          	               
	
		                predicates.add(criteriaBuilder.or(
		                	
		                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(root.get("id"), criteriaBuilder.literal(""))), likePattern),
		                    
		                    criteriaBuilder.like(criteriaBuilder.lower( DateTimeUtilFormart.DATE_FORMATTER_DB(criteriaBuilder, root.get("fromDate"))), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower( DateTimeUtilFormart.DATE_FORMATTER_DB(criteriaBuilder, root.get("toDate"))), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat( criteriaBuilder.concat(employeeJoin.get("nameEn") ,"-"), employeeJoin.get("insurance"))), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(employeePositionJoin.get("position")), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(branchJoin.get("branchShortName")), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(departmentJoin.get("name")), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower( DateTimeUtilFormart.DATE_FORMATTER_DB(criteriaBuilder, root.get("reportedDate"))), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(root.get("developmentGoal")), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower( DateTimeUtilFormart.DATE_FORMATTER_DB(criteriaBuilder, root.get("developmentDueDate"))), likePattern),		                    
		                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat( criteriaBuilder.concat(supervisorJoin.get("nameEn") ,"-"), supervisorJoin.get("insurance"))), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(supervisorPositionJoin.get("position")), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower(
		                    	        criteriaBuilder.<String>selectCase()
		                    	            .when(criteriaBuilder.isTrue(root.get("supervisorQcYesNo")), "yes")
		                    	            .when(criteriaBuilder.isFalse(root.get("supervisorQcYesNo")), "no")
		                    	            .otherwise(" ")
		                    	    ),likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower( DateTimeUtilFormart.DATE_FORMATTER_DB(criteriaBuilder, root.get("supervisorQcAt"))), likePattern),
		                    criteriaBuilder.like(criteriaBuilder.lower( criteriaBuilder.concat(root.get("survey123ID"),"")), likePattern),   
		                    	                    	              
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
		            // From Date Filter
			        if (advanceFilterFromDateFrom.getValue() != null && advanceFilterFromDateDateTo.getValue() != null) {
			        	Expression<LocalDate> truncatedCreatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("fromDate"));
			        	predicates.add(criteriaBuilder.between(truncatedCreatedAt, advanceFilterFromDateFrom.getValue(), advanceFilterFromDateDateTo.getValue()));
			            sqlFilter.add("Survey Start Date BETWEEN " + advanceFilterFromDateFrom.getValue() + " AND " + advanceFilterFromDateDateTo.getValue());
			        }
			     // To Date Filter
			        if (advanceFilterToDateFrom.getValue() != null && advanceFilterToDateTo.getValue() != null) {
			        	Expression<LocalDate> truncatedCreatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("toDate"));
			        	predicates.add(criteriaBuilder.between(truncatedCreatedAt, advanceFilterToDateFrom.getValue(), advanceFilterToDateTo.getValue()));
			            sqlFilter.add("Survey Start Date BETWEEN " + advanceFilterToDateFrom.getValue() + " AND " + advanceFilterToDateTo.getValue());
			        }
			        
		            //Staff name Filter
		            if (advanceFilterSurveyName.getValue() != null && !advanceFilterSurveyName.isEmpty()) {
		            	String likePattern = "%" + advanceFilterSurveyName.getValue().toLowerCase() + "%";
		                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(employeeJoin.get("nameEn"),employeeJoin.get("insurance"))), likePattern));
		                sqlFilter.add("Staff name like " + likePattern);
		            }
		            
		            //Staff position Filter
		            if (advanceFilterSurveyPosition.getValue() != null && !advanceFilterSurveyPosition.isEmpty()) { 
		                Set<String> patternFilter = advanceFilterSurveyPosition.getValue().stream()
				                .map(Positions::getPosition)
				                .collect(Collectors.toSet());
				            predicates.add(employeePositionJoin.get("position").in(patternFilter));
				            sqlFilter.add("Survey Position IN (" + String.join(", ", patternFilter) + ")"); 
		            }
		            
		            //Staff Branch Filter
		            if (advanceFilterSurveyBranch.getValue() != null && !advanceFilterSurveyBranch.isEmpty()) { 
		                Set<String> patternFilter = advanceFilterSurveyBranch.getValue().stream()
				                .map(Branch::getBranchShortName)
				                .collect(Collectors.toSet());
				            predicates.add(branchJoin.get("branchShortName").in(patternFilter));
				            sqlFilter.add("Location IN (" + String.join(", ", patternFilter) + ")"); 
		            }
		            
		            //Staff advanceFilterDepartment Filter
		            if (advanceFilterDepartment.getValue() != null && !advanceFilterDepartment.isEmpty()) { 
		                Set<String> patternFilter = advanceFilterDepartment.getValue().stream()
				                .map(Department::getName)
				                .collect(Collectors.toSet());
				            predicates.add(departmentJoin.get("name").in(patternFilter));
				            sqlFilter.add("Department IN (" + String.join(", ", patternFilter) + ")"); 
		            }
		            
				     // Reported date Filter
			        if (advanceFilterReportedDateFrom.getValue() != null && advanceFilterReportedDateTo.getValue() != null) {
			        	Expression<LocalDate> truncatedCreatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("reportedDate"));
			        	predicates.add(criteriaBuilder.between(truncatedCreatedAt, advanceFilterReportedDateFrom.getValue(), advanceFilterReportedDateTo.getValue()));
			            sqlFilter.add("Survey Start Date BETWEEN " + advanceFilterReportedDateFrom.getValue() + " AND " + advanceFilterReportedDateTo.getValue());
			        }
			        
		            //DevelopmentGoal Filter
		            if (advanceFilterDevelopmentGoal.getValue() != null && !advanceFilterDevelopmentGoal.isEmpty()) {
		            	String likePattern = "%" + advanceFilterDevelopmentGoal.getValue().toLowerCase() + "%";
		                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("developmentGoal")), likePattern));
		                sqlFilter.add("Staff name like " + likePattern);
		            }
				     // Due Goal date Filter
			        if (advanceFilterDevelopmentGoalDualDateFrom.getValue() != null && advanceFilterDevelopmentGoalDualDateTo.getValue() != null) {
			        	Expression<LocalDate> truncatedCreatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("developmentDueDate"));
			        	predicates.add(criteriaBuilder.between(truncatedCreatedAt, advanceFilterDevelopmentGoalDualDateFrom.getValue(), advanceFilterDevelopmentGoalDualDateTo.getValue()));
			            sqlFilter.add("Survey Start Date BETWEEN " + advanceFilterDevelopmentGoalDualDateFrom.getValue() + " AND " + advanceFilterDevelopmentGoalDualDateTo.getValue());
			        }
			        
		            //Supervisor name Filter
		            if (advanceFilterSupervisorName.getValue() != null && !advanceFilterSupervisorName.isEmpty()) {
		            	String likePattern = "%" + advanceFilterSupervisorName.getValue().toLowerCase() + "%";
		                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(supervisorJoin.get("nameEn"),supervisorJoin.get("insurance"))), likePattern));
		                sqlFilter.add("Supervisor name like " + likePattern);
		            }
		            
		            //Supervisor position Filter
		            if (advanceFilterSupervisorPosition.getValue() != null && !advanceFilterSupervisorPosition.isEmpty()) { 
		                Set<String> patternFilter = advanceFilterSupervisorPosition.getValue().stream()
				                .map(Positions::getPosition)
				                .collect(Collectors.toSet());
				            predicates.add(supervisorPositionJoin.get("position").in(patternFilter));
				            sqlFilter.add("Supervisor Position IN (" + String.join(", ", patternFilter) + ")"); 
		            }
			        
		            if (advanceFilterSupervisorQcYesNo.getValue() != null && !advanceFilterSupervisorQcYesNo.getValue().isEmpty()) {
		                Boolean filterPattern = "Yes".equalsIgnoreCase(advanceFilterSupervisorQcYesNo.getValue());
		                predicates.add(criteriaBuilder.equal(root.get("supervisorQcYesNo"), filterPattern));
		                sqlFilter.add("SupervisorQcYesNo = " + filterPattern);
		            }
		            
				     // Due Goal date Filter
			        if (advanceFilterSupervisorQcAtFrom.getValue() != null && advanceFilterSupervisorQcAtTo.getValue() != null) {
			        	Expression<LocalDate> truncatedCreatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("supervisorQcAt"));
			        	predicates.add(criteriaBuilder.between(truncatedCreatedAt, advanceFilterSupervisorQcAtFrom.getValue(), advanceFilterSupervisorQcAtTo.getValue()));
			            sqlFilter.add("Supervisor Qc At BETWEEN " + advanceFilterSupervisorQcAtFrom.getValue() + " AND " + advanceFilterSupervisorQcAtTo.getValue());
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
	    	advanceFilterSettingNumberField(advanceFilterID);	    	
	    	advanceFilterSettingDateRank(advanceFilterFromDateFrom,advanceFilterFromDateDateTo);
	    	advanceFilterSettingDateRank(advanceFilterToDateFrom,advanceFilterToDateTo);	    		    	
	    	advanceFilterSettingTextField(advanceFilterSurveyName);	 
	    	
	    	advanceFilterSettingMultiSelectComboBox(advanceFilterSurveyPosition);	    	
	    	advanceFilterSurveyPosition.setItemLabelGenerator(Positions::getPosition);
	    	
	    	advanceFilterSettingMultiSelectComboBox(advanceFilterSurveyBranch);	    	
	    	advanceFilterSurveyBranch.setItemLabelGenerator(Branch::getBranchShortName);
	    	
	    	advanceFilterSettingMultiSelectComboBox(advanceFilterDepartment);	    	
	    	advanceFilterDepartment.setItemLabelGenerator(Department::getName);
	    	
	    	advanceFilterSettingDateRank(advanceFilterReportedDateFrom,advanceFilterReportedDateTo);		    		
	    	advanceFilterSettingTextField(advanceFilterDevelopmentGoal);
	    	advanceFilterSettingDateRank(advanceFilterDevelopmentGoalDualDateFrom,advanceFilterDevelopmentGoalDualDateTo);
	    	advanceFilterSettingTextField(advanceFilterSupervisorName);
	    	
	    	advanceFilterSettingMultiSelectComboBox(advanceFilterSupervisorPosition);	    	
	    	advanceFilterSupervisorPosition.setItemLabelGenerator(Positions::getPosition);	 
	    	
	    	advanceFilterSettingComboBox(advanceFilterSupervisorQcYesNo);	    	    	
	    	advanceFilterSettingDateRank(advanceFilterSupervisorQcAtFrom,advanceFilterSupervisorQcAtTo);
	    	
	    	advanceFilterSettingMultiSelectComboBox(advanceFilterCreatedBy);
	    	advanceFilterCreatedBy.setItemLabelGenerator(User::getName); 
	    	
	    	advanceFilterSettingDateRank(advanceFilterCreatedDateFrom,advanceFilterCreatedDateTo);    	
	    	advanceFilterSettingMultiSelectComboBox(advanceFilterUpdatedBy);
	    	advanceFilterUpdatedBy.setItemLabelGenerator(User::getName);  
	    	
	    	advanceFilterSettingDateRank(advanceFilterUpdaedDateFrom,advanceFilterUpdaedDateTo);	    	
	    	
	    	 this.advanceSearchLayout.add(advanceFilterID,
	    			 advanceFilterFromDateFrom,advanceFilterFromDateDateTo,
	    			 advanceFilterToDateFrom,advanceFilterToDateTo,
	    			 advanceFilterSurveyName,
	    			 advanceFilterSurveyPosition,
	    			 advanceFilterSurveyBranch,
	    			 advanceFilterDepartment,
	    			 advanceFilterReportedDateFrom,
	    			 advanceFilterReportedDateTo,
	    			 advanceFilterDevelopmentGoal,
	    			 advanceFilterDevelopmentGoalDualDateFrom,advanceFilterDevelopmentGoalDualDateTo,
	    			 advanceFilterSupervisorName,
	    			 advanceFilterSupervisorPosition,
	    			 advanceFilterSupervisorQcYesNo,
	    			 advanceFilterSupervisorQcAtFrom, advanceFilterSupervisorQcAtTo,
	    			 advanceFilterCreatedBy,
	    			 advanceFilterCreatedDateFrom,advanceFilterCreatedDateTo,
	    			 advanceFilterUpdatedBy,
	    			 advanceFilterUpdaedDateFrom,advanceFilterUpdaedDateTo);
	    	//return formLayout;
	    }
	    private void reloadAdvanceFilterData() {
	        advanceFilterSurveyPosition.setItems(this.positionRepository.findAll());
	        advanceFilterSurveyBranch.setItems(this.branchRepository.findAll());
	        advanceFilterDepartment.setItems(this.departmentRepository.findAll());
	        advanceFilterSupervisorPosition.setItems(this.positionRepository.findAll());
	        advanceFilterCreatedBy.setItems(userService.getAllUser());
	        advanceFilterUpdatedBy.setItems(userService.getAllUser());
	    }
	    private void reloadEditorData() {
	    	 this.reportedBy.setItems(hrEmployeeRepository.findAll());
	    	 this.reportedPosition.setItems(positionRepository.findAll());
	    	 this.branch.setItems(branchRepository.findAll());
	    	 this.department.setItems(departmentRepository.findAll());
	    	 this.supervisor.setItems(hrEmployeeRepository.findAll());
	    	 this.supervisorPosition.setItems(positionRepository.findAll());
	    }

	    



	    @Override
	    protected void focusFirstField() {
	        this.fromDate.focus(); // Focus the "name" field
	    }
	    
	    
	    @Override
	    protected GoalSetting createNewEntity() {
	        return new GoalSetting(); // Initialize a new ServiceType entity
	    }

	
	    @Override
	    public void beforeEnter(BeforeEnterEvent event) {
	    	if(!authenticatedUser.hasPage(GoalSupervisorView.class,AccessPageType.SELECTED_PAGE)) {
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
	            StreamResource resource = new StreamResource("GoalSetting.xlsx", () -> {
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	                try (Workbook workbook = new XSSFWorkbook()) {
	                    Sheet sheet = workbook.createSheet("Category");

	                    // Write headers dynamically from grid columns
	                    Row headerRow = sheet.createRow(0);
	                    List<Grid.Column<GoalSetting>> columns = grid.getColumns();
	                    for (int i = 0; i < columns.size(); i++) {
	                        headerRow.createCell(i).setCellValue(columns.get(i).getHeaderText());
	                    }

	                    // Write data
	                    List<GoalSetting> itemsToExport = grid.getSelectedItems().isEmpty() ?
	                            grid.getGenericDataView().getItems().toList() : new ArrayList<>(grid.getSelectedItems());
	                    AtomicInteger rowIndex = new AtomicInteger(1); // Use AtomicInteger to keep track of row index

	                    itemsToExport.forEach(row2bExport -> {
	                        Row dataRow = sheet.createRow(rowIndex.getAndIncrement());

	                        // Loop over columns dynamically
	                        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
	                            String columnKey = columns.get(columnIndex).getKey(); // Get column key
	                            switch (columnKey) {
	                            	case "reportedBy.insurance":
	                                    dataRow.createCell(columnIndex).setCellValue(row2bExport.getReportedBy()!= null ? row2bExport.getReportedBy().getNameEn() + " - "+ row2bExport.getReportedBy().getInsurance() :"");
	                                    break;
	                            	case "supervisor.insurance":
	                                    dataRow.createCell(columnIndex).setCellValue(row2bExport.getSupervisor()!= null ? row2bExport.getSupervisor().getNameEn() + " - "+ row2bExport.getSupervisor().getInsurance() :"");
	                                    break;	                                    
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
	                                        Field field = getFieldFromClassHierarchy(GoalSetting.class, columnKey);
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

}
