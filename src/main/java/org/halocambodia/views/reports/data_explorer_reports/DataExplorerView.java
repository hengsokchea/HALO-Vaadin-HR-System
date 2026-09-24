package org.halocambodia.views.reports.data_explorer_reports;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
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
import org.halocambodia.data.DataExplorer;
import org.halocambodia.data.DataExplorerUser;
import org.halocambodia.data.DataExplorerUserRepository;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.GoalSetting;
import org.halocambodia.data.GoalSettingDetail;
import org.halocambodia.data.HREmployeeData;
import org.halocambodia.data.HREmployeeRepository;
import org.halocambodia.data.HREmployeeWithSupervisor;
import org.halocambodia.data.Positions;
import org.halocambodia.data.ReportQuery;
import org.halocambodia.data.Role;
import org.halocambodia.data.RoleRepository;
import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
import org.halocambodia.data.FileSizeFormatter;
import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.data.Unit;
import org.halocambodia.data.UnitRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.DataExplorerService;
import org.halocambodia.services.GoalSettingEntryService;
import org.halocambodia.services.GoalSettingService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.MasterDetailLayout;
import org.halocambodia.views.MasterPageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.halocambodia.views.goal_setting.GoalSupervisorView;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;


@PageTitle("Data explorer")
@Route(value = "data-explorers", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class DataExplorerView  extends MasterPageDialogLayout<DataExplorer, DataExplorerService> implements BeforeEnterObserver{
	
	// entity attribute
	private TextField dataExplorerName=new TextField("Data Explorer");
	private final MultiSelectComboBox<Role> roles =new MultiSelectComboBox<Role>("Role");
	private TextArea query=new TextArea("Query");
	private TextArea notes=new TextArea("Note");

    
   
    private final BeanValidationBinder<DataExplorer> binder= new BeanValidationBinder<>(DataExplorer.class);    
    
//Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");    
    
    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");    
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");    
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");
    
   // share data 
    private final Optional<User> currentUserLogin;
    private final DataExplorerUserRepository dataExplorerUserRepository;
    private final RoleRepository roleRepository ;
  
    public DataExplorerView(DataExplorerService service,UserService userService,AuthenticatedUser authenticatedUser,DataExplorerUserRepository dataExplorerUserRepository,RoleRepository roleRepository) { 
	 	super(service,userService,authenticatedUser);
	 	this.currentUserLogin=authenticatedUser.get();
	 	this.dataExplorerUserRepository=dataExplorerUserRepository;
	 	this.roleRepository=roleRepository;
	 	
	 	
    	configureGrid();
        configureEditorLayout();    	        
        binderField();
       	createAdvanceFilterLayout(); 

 }

 
 @Override
 protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);	        
        UI.getCurrent().access(() -> { 

	       reloadAdvanceFilterData();
        });
}
 

private void binderField() {
	binder.bindInstanceFields(this);

}


@Override
 protected void configureGrid() {
    	grid.addColumn(DataExplorer::getId).setHeader("ID").setFooter("Total Records:").setKey("id");
    	grid.addColumn(DataExplorer::getDataExplorerName).setHeader("Name").setKey("dataExplorerName") .setSortProperty("dataExplorerName");
    	//grid.addColumn(DataExplorer::getQuery).setHeader("Query").setKey("query") .setSortProperty("query");
    	//grid.addColumn(DataExplorer::getNotes).setHeader("Note").setKey("notes") .setSortProperty("notes");
    	
    	grid.addColumn(new ComponentRenderer<>(entitySelected -> {
	        Span span = new Span(entitySelected.getNotes());
	        span.getStyle().set("white-space", "pre-wrap");
	        span.getStyle().set("max-width", "400px");
	        return span;
	    })).setHeader("Note").setKey("notes") .setSortProperty("notes");	    
    	
    	grid.addColumn(new ComponentRenderer<>(item -> {
    	    if (currentUserLogin.isPresent()) {
    	        User currentUser = currentUserLogin.get();

    	        boolean alreadyAdded = dataExplorerUserRepository.existsByUserAndDataExplorer(currentUser, item);
    	        //System.out.println("Checking: userId=" + currentUser.getId() + ", dataExplorerId=" + item.getId() + ", alreadyAdded=" + alreadyAdded);

    	        if (alreadyAdded) {
    	            Span alreadyAddedText = new Span("Already added");
    	            return alreadyAddedText;
    	        }

    	        Button addButton = new Button(VaadinIcon.PLUS.create());
    	        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    	        addButton.getElement().setProperty("title", "Add to your Dashboard");

    	        addButton.addClickListener(e -> {
    	            try {
    	                if (!canAddDashboard(currentUserLogin, item)) {
    	                    // You are not in a BeforeEnter handler here, so don't use event.rerouteTo(...)
    	                    UI.getCurrent().navigate(org.halocambodia.views.access_denied.AccessDeniedView.class);
    	                    return;
    	                }
    	                
    	                Map<String, Object> configItem = new LinkedHashMap<>();
    	                configItem.put("width", 24);

    	                ObjectMapper mapper = new ObjectMapper();
    	                String jsonConfig = mapper.writeValueAsString(configItem); // ✅ store as JSON string

    	                DataExplorerUser newRecord = new DataExplorerUser();
    	                newRecord.setDataExplorer(item);
    	                newRecord.setUser(currentUser);
    	                newRecord.setSortOrder( dataExplorerUserRepository.findMaxSortOrderByUser(currentUser) +1);
    	                newRecord.setConfig(jsonConfig); // ✅ JSON string for jsonb
    	                newRecord.setUserCreated(currentUser);
    	                newRecord.setUserUpdated(currentUser);

    	                dataExplorerUserRepository.save(newRecord);

    	                Notification.show("Added to your dashboard", 2000, Position.TOP_CENTER)
    	                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

    	                grid.getDataProvider().refreshItem(item); // update row to show "Already added"

    	            } catch (Exception ex) {
    	                showError("Error: " + ex.getMessage());
    	            }
    	        });

    	        return addButton;
    	    }

    	    return new Span("Not logged in");
    	})).setHeader("Dashboard usage").setKey("dashboard_usage").setSortProperty("dashboard_usage");

    	grid.addColumn(items -> items.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "))).setHeader("Roles").setKey("roles.name").setSortProperty("roles.name");

    	
 	       
        
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
        	return entityRowUserUpdateddAt.getUpdatedAt() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRowUserUpdateddAt.getUpdatedAt()) : "";
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
        
 }
private boolean canAddDashboard(Optional<User> userOpt, DataExplorer report) {
    if (userOpt.isEmpty()) return false;

    Set<Role> reportRoles = Optional.ofNullable(report.getRoles()).orElse(Set.of());
    // ⛔️ Change here: empty = deny
    if (reportRoles.isEmpty()) return false;

    Set<Role> userRoles = Optional.ofNullable(userOpt.get().getRoles()).orElse(Set.of());

    // Compare by id (fallback to name if id is null)
    return userRoles.stream().anyMatch(ur ->
        reportRoles.stream().anyMatch(rr ->
            (rr.getId() != null && rr.getId().equals(ur.getId()))
            || (rr.getId() == null && ur.getId() == null
                && Objects.equals(rr.getName(), ur.getName()))
        )
    );
}

 @Override
 protected void configureEditorLayout() {

	 FormLayout  formLayout = new FormLayout ();
     formLayout.setSizeFull();

        
        formLayout.add(dataExplorerName,query,roles,notes);
        formLayout.setColspan(query, 2);
        formLayout.setColspan(notes, 2);
        
        
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
        
        editorLayout.setDialogTitle("Data Explorer");       
        
        editorLayout.add(formLayout); 
        editorLayout.getFooter().add(buttonLayout); 



 }

 	private void save() {
	    if (entity == null) {
	        showError("No entity to save.");
	        return;
	    }

	    try {
	        binder.writeBean(entity);

	        // STEP 1: Save the entity first (get ID)
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
	 protected void populateForm(DataExplorer entity) {
		 preLoadFormData();
	     binder.readBean(entity); // Populate the form using the binder
	     editorLayout.open(); 
	 }
	    private void preLoadFormData() {
	        this.roles.setItems(roleRepository.findAll());
	        this.roles.setItemLabelGenerator(Role::getName);
	    }
    
    private void clearForm() {
    	 binder.readBean(null);
    	 this.entity = null;
    }
    private void closeForm() {
    	//editorLayout.setVisible(false);
    	editorLayout.close();
    }
    
    
    @Override
    protected Specification<DataExplorer> buildCombinedSpecification() {
        return (root, query, criteriaBuilder) -> {
        	try {
	            List<Predicate> predicates = new ArrayList<>();
	            List<String> sqlFilter = new ArrayList<>();
                    	            
	            Join<GoalSetting, User> userCreatedJoin = root.join("userCreated", JoinType.LEFT);
	            Join<GoalSetting, User> userUpdatedJoin = root.join("userUpdated", JoinType.LEFT);	 

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
    	advanceFilterSettingNumberField(advanceFilterID);	    	   
    	
    	advanceFilterSettingMultiSelectComboBox(advanceFilterCreatedBy);
    	advanceFilterCreatedBy.setItemLabelGenerator(User::getName); 
    	
    	advanceFilterSettingDateRank(advanceFilterCreatedDateFrom,advanceFilterCreatedDateTo);    	
    	advanceFilterSettingMultiSelectComboBox(advanceFilterUpdatedBy);
    	advanceFilterUpdatedBy.setItemLabelGenerator(User::getName);  
    	
    	advanceFilterSettingDateRank(advanceFilterUpdaedDateFrom,advanceFilterUpdaedDateTo);	    	
    	
    	 this.advanceSearchLayout.add(
    			 advanceFilterID,
 
    			 advanceFilterCreatedBy,
    			 advanceFilterCreatedDateFrom,advanceFilterCreatedDateTo,
    			 advanceFilterUpdatedBy,
    			 advanceFilterUpdaedDateFrom,advanceFilterUpdaedDateTo);
    }
    private void reloadAdvanceFilterData() {

        advanceFilterCreatedBy.setItems(userService.getAllUser());
        advanceFilterUpdatedBy.setItems(userService.getAllUser());
    }


    @Override
    protected void focusFirstField() {
        this.dataExplorerName.focus(); // Focus the "name" field
    }
    
    
    @Override
    protected DataExplorer createNewEntity() {
        return new DataExplorer(); // Initialize a new ServiceType entity
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
    	if(!authenticatedUser.hasPage(this.getClass(),AccessPageType.SELECTED_PAGE)) {
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
            StreamResource resource = new StreamResource("Date Explorer.xlsx", () -> {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try (Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("Date Explorer");

                    // Write headers dynamically from grid columns
                    Row headerRow = sheet.createRow(0);
                    List<Grid.Column<DataExplorer>> columns = grid.getColumns();
                    for (int i = 0; i < columns.size(); i++) {
                        headerRow.createCell(i).setCellValue(columns.get(i).getHeaderText());
                    }

                    // Write data
                    List<DataExplorer> itemsToExport = grid.getSelectedItems().isEmpty() ?
                            grid.getGenericDataView().getItems().toList() : new ArrayList<>(grid.getSelectedItems());
                    AtomicInteger rowIndex = new AtomicInteger(1); // Use AtomicInteger to keep track of row index

                    itemsToExport.forEach(row2bExport -> {
                        Row dataRow = sheet.createRow(rowIndex.getAndIncrement());

                        // Loop over columns dynamically
                        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                            String columnKey = columns.get(columnIndex).getKey(); // Get column key
                            switch (columnKey) {
                            
	                        	case "roles.name":
	                                dataRow.createCell(columnIndex).setCellValue(row2bExport.getRoles()!= null ? row2bExport.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")):"");
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
                                        Field field = getFieldFromClassHierarchy(DataExplorer.class, columnKey);
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
