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
import java.util.Collections;
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
import org.halocambodia.data.HREmployeeWithSupervisor;
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


@PageTitle("Goal Entry")
@Route(value = "goal-entry", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class GoalSettingEntryView  extends MasterPageDialogLayout<GoalSetting, GoalSettingEntryService> implements BeforeEnterObserver{
	
	// entity attribute
	
    private DatePicker fromDate=new DatePicker("From Date | ថ្ងៃចាប់ផ្ដើម");
    private DatePicker toDate=new DatePicker("To Date | ថ្ងៃបញ្ចប់");
    private ComboBox<HREmployeeData> reportedBy=new ComboBox("Staff ​​Name | ឈ្មោះបុគ្គលិក");
    private ComboBox<Positions> reportedPosition=new ComboBox("Staff Position | មុខដំណែងបុគ្គលិក");
    private ComboBox<Branch> branch=new ComboBox("Location | តំបន់");
    private ComboBox<Department> department=new ComboBox("Department​ | ផ្នែក");
    
    private DatePicker reportedDate=new DatePicker("Reported Date | ថ្ងៃរាយការណ៍");
    private TextArea developmentGoal=new TextArea("Development Goal | គោលដៅដើម្បីការអភិវឌ្ឍខ្លួន");
    private DatePicker developmentDueDate =new DatePicker("Development Goal Due Date | ថ្ងៃផុតកំណត់គោលដៅអភិវឌ្ឍន៍");
    private ComboBox<HREmployeeData> supervisor=new ComboBox("Supervisor Name | ឈ្មោះអ្នកគ្រប់គ្រង");
    private ComboBox<Positions> supervisorPosition=new ComboBox("Supervisor Positions | មុខដំណែងអ្នកគ្រប់គ្រង");
     
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
    
    Optional<HREmployeeData> currentStaff;
    private Optional <List<HREmployeeWithSupervisor>> currentSupervisor;
    
   
    private final BeanValidationBinder<GoalSetting> binder= new BeanValidationBinder<>(GoalSetting.class);    
    
//Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");    
    
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
    

	Grid<GoalSettingDetail> gridWorkGoal =new Grid <>(GoalSettingDetail.class, false);  
	private final BeanValidationBinder<GoalSettingDetail> workGoalBinder = new BeanValidationBinder<>(GoalSettingDetail.class);
  
	 public GoalSettingEntryView(GoalSettingEntryService service,UserService userService,AuthenticatedUser authenticatedUser,HREmployeeRepository hrEmployeeRepository,BranchRepository branchRepository,DepartmentRepository departmentRepository,PositionRepository positionRepository,FileUploadUtility fileUploadUtility,AttachmentRepository attachmentRepository) { 
		 	super(service,userService,authenticatedUser);
		 	this.currentUserLogin=authenticatedUser.get();
		 	this.attachmentRepository=attachmentRepository;
		 	
		 	this.hrEmployeeRepository=hrEmployeeRepository;		 
		 	this.branchRepository=branchRepository;
		 	this.departmentRepository=departmentRepository;
		 	this.positionRepository=positionRepository;
		 	
		 	this.fileUploadUtility=fileUploadUtility;
		 	 uploadDir=fileUploadUtility.initializeUploadDirectory("hr", "");

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
		        	} catch (Exception e) {
		        		showError("Error during UI setup: " + e.getMessage());
						e.printStackTrace();
					}
		        });
		        //Defaul current staff
		        currentStaff =hrEmployeeRepository.findOne((root, query, cb) -> cb.equal(root.get("insurance"), currentUserLogin.get().getInsurance()));
	        
		     
		        // Set default supervisor list from HREmployeeWithSupervisor
		        List<HREmployeeWithSupervisor> supervisorList = currentStaff
		            .map(HREmployeeData::getEmployeeWithSupervisors)
		            .orElse(Collections.emptyList());
		        currentSupervisor = Optional.of(supervisorList);
		    } catch (Exception ex) {
		    	showError("Error initializing view: " + ex.getMessage());
		        ex.printStackTrace();
		    }

	}
	 	
	private void binderField() {
		binder.bindInstanceFields(this);
	}


	@Override
	 protected void configureGrid() throws Exception {
	    	grid.addColumn(GoalSetting::getId).setHeader("ID").setFooter("Total Records:").setKey("id");
	        grid.addColumn(GoalSetting::getFromDate).setHeader(this.fromDate.getLabel()).setKey("fromDate");   
	        grid.addColumn(GoalSetting::getToDate).setHeader(this.toDate.getLabel()).setKey("toDate");
	        
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
	        	        	        
	        grid.addColumn(GoalSetting::getReportedDate).setHeader(this.reportedDate.getLabel()).setKey("reportedDate");
	        
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

	        grid.addColumn(GoalSetting::getDevelopmentDueDate).setHeader(this.developmentDueDate.getLabel()).setKey("developmentDueDate");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getSupervisor() != null ? (entitySelected.getSupervisor().getNameEn()) + " - "+ entitySelected.getSupervisor().getInsurance() : ""; 
	        }).setHeader(this.supervisor.getLabel()).setSortProperty("supervisor.insurance").setKey("supervisor.insurance");
	        
	        grid.addColumn(entitySelected -> {
	            return entitySelected.getSupervisorPosition() != null ? entitySelected.getSupervisorPosition().getPosition() : ""; 
	        }).setHeader(this.supervisorPosition.getLabel()).setSortProperty("supervisorPosition.position").setKey("supervisorPosition.position");	        	       
	        
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
    	
    	gridDevelopmentGoal.addColumn(GoalSettingDetail::getSurvey123ID).setHeader("Survey123").setKey("survey123ID");	
    	
    	
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

    	return gridDevelopmentGoal;
	}
	

	 @Override
	 protected void configureEditorLayout() throws Exception{

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
	        FormLayout formSupervisorInfo=new FormLayout(SupervisorInfoTitle, supervisor,this.supervisorPosition);
	        formConfigProperty(formSupervisorInfo);	        
	        formSupervisorInfo.setColspan(SupervisorInfoTitle, 4);
	        
	        supervisor.addValueChangeListener(event -> {getSupervisorPosition(event.getValue());});


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
	        
	        editorLayout.setDialogTitle("Goal Submission Form | ទម្រង់នៃការដាក់គោលដៅ");       
	        
	        editorLayout.add(formLayout); 
	        editorLayout.getFooter().add(buttonLayout); 

	        

	        

	        
	        this.reportedBy.setItems(hrEmployeeRepository.findAll());
	        this.reportedBy.setItemLabelGenerator( employee -> employee.getNameEn() + " - " + employee.getInsurance());
	        
	        this.reportedPosition.setItems(positionRepository.findAll());
	        this.reportedPosition.setItemLabelGenerator(Positions::getPosition);
	        
	        this.branch.setItems(branchRepository.findAll());
	        this.branch.setItemLabelGenerator( Branch::getBranchShortName);
	        
	        this.department.setItems(departmentRepository.findAll());
	        this.department.setItemLabelGenerator(Department::getName);
	        
	        this.supervisor.setItems(hrEmployeeRepository.findAll());
	        this.supervisor.setItemLabelGenerator( employee -> employee.getNameEn() + " - " + employee.getInsurance());
	        
	        this.supervisorPosition.setItems(positionRepository.findAll());
	        this.supervisorPosition.setItemLabelGenerator(Positions::getPosition);

	 }
	 private void getSupervisorPosition(HREmployeeData selected) {
		    try {
		        if (selected != null) {
		            Positions matchedPosition = positionRepository.findAll().stream()
		                .filter(pos -> pos.getPosition().equalsIgnoreCase(selected.getPosition()))
		                .findFirst()
		                .orElse(null);
		            supervisorPosition.setValue(matchedPosition);
		        } else {
		            supervisorPosition.clear();
		        }
		    } catch (Exception ex) {
		        supervisorPosition.clear(); // fallback
		        showError("Error loading supervisor position: " + ex.getMessage());
		        ex.printStackTrace(); // optionally log or report
		    }
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
		    	showError("Error: " + event.getErrorMessage())
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
		    	entity.setGoalSettingDetails(goalSettingDetails);

		        binder.writeBean(entity);

		        // STEP 1: Save the entity first (get ID)
		        entity = service.update(entity); // This gives you an ID

		        // STEP 2: Now that entity has ID, set attachments
		        signaturePadReporter.getImageData(reporterDataUrl -> {
		                setSignatureImage(entity, 1L, reporterDataUrl);
		               
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
		    }
		}

	 
	 @Override
	 protected void populateForm(GoalSetting entity) throws Exception{
	     binder.readBean(entity); // Populate the form using the binder
	     editorLayout.open();
	     boolean isEdit = entity != null && entity.getId() != null;
	     
	     if (entity != null) {
	    	    // 1. Load attachments manually
	    	    List<Attachment> attachments = attachmentRepository.findByEntity(entity);
	    	    entity.setAttachments(attachments);
	    	    
	            loadSignatureIfPresent(attachments, 1L, signaturePadReporter);
	            
	            goalSettingDetails=entity.getGoalSettingDetails();
	     }
	     // Add New Record
	     if(!isEdit) {
	   	  // Set default value to 1 Jan of current year
		     LocalDate firstJan = LocalDate.now().withDayOfYear(1);
		     fromDate.setValue(firstJan);
		     
		  // Set value to 31 December of the current year
		     LocalDate lastDayOfYear = LocalDate.now().withMonth(12).withDayOfMonth(31);
		     toDate.setValue(lastDayOfYear);
		  //Set default Current Staff 		     
		     currentStaff.map(List::of).ifPresentOrElse(
		    		    list -> {
		    		        reportedBy.setItems(list);
		    		        reportedBy.setValue(list.get(0));
		    		    },
		    		    () -> reportedBy.setItems(List.of())
		    		);
		   //Set default Current Staff Position		     		    
		     currentStaff.ifPresent(staff -> {
		    	    Positions matchedPosition = positionRepository.findAll().stream()
		    	        .filter(pos -> pos.getPosition().equalsIgnoreCase(staff.getPosition()))
		    	        .findFirst()
		    	        .orElse(null);
		    	    reportedPosition.setValue(matchedPosition);
		    	});
		     
		   //Set default Current reported date 
		     this.reportedDate.setValue(LocalDate.now());
		     
			   //Set default Current Branch	     		    
		     currentStaff.ifPresent(staff -> {
		    	    Branch matchedBranch = branchRepository.findAll().stream()
		    	        .filter(br -> br.getBranchShortName().equalsIgnoreCase(staff.getLocation()))
		    	        .findFirst()
		    	        .orElse(null);
		    	    this.branch.setValue(matchedBranch);
		    	});
		     
			   //Set default Current deparment	     		    
		     currentStaff.ifPresent(staff -> {
		    	    Department matchedDepartment = departmentRepository.findAll().stream()
		    	        .filter(de -> de.getName().equalsIgnoreCase(staff.getDepartment()))
		    	        .findFirst()
		    	        .orElse(null);
		    	    this.department.setValue(matchedDepartment);
		    	});
		     
		   //Set default Current supervisor	     		    
		  // Populate supervisor ComboBox with extracted supervisors
		     this.supervisor.setItems(
		    		    currentSupervisor
		    		        .orElse(Collections.emptyList()) // unwrap the Optional
		    		        .stream()
		    		        .map(HREmployeeWithSupervisor::getSupervisor)
		    		        .toList()
		    		);

	     }
 
	     gridWorkGoal.setItems(goalSettingDetails);
	     gridWorkGoal.getDataProvider().refreshAll();
	    
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
		            Join<GoalSetting, HREmployeeData> employeeJoin = root.join("reportedBy", JoinType.INNER);	
		            
		            //Default Filter
		            predicates.add(criteriaBuilder.equal(employeeJoin.get("insurance"),currentUserLogin.get().getInsurance()));
		            predicates.add(criteriaBuilder.equal(root.get("supervisorQcYesNo"),false));
		            
		       	            
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
	    protected GoalSetting createNewEntity() throws Exception{
	        return new GoalSetting(); // Initialize a new ServiceType entity
	    }

	
	    @Override
	    public void beforeEnter(BeforeEnterEvent event) {
	    	if(!authenticatedUser.hasPage(GoalSettingEntryView.class,AccessPageType.SELECTED_PAGE)) {
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
