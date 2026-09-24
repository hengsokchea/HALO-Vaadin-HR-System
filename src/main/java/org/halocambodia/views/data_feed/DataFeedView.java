package org.halocambodia.views.data_feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Branch;
import org.halocambodia.data.BranchRepository;
import org.halocambodia.data.DataFeeds;
import org.halocambodia.data.DataFeedsLog;
import org.halocambodia.data.DataFeedsStep;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.GoalSetting;
import org.halocambodia.data.GoalSettingDetail;
import org.halocambodia.data.HREmployeeData;
import org.halocambodia.data.HREmployeeRepository;
import org.halocambodia.data.Positions;

import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
import org.halocambodia.data.Unit;
import org.halocambodia.data.UnitRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.DataFeedsService;
import org.halocambodia.services.GoalSettingService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.MasterDetailLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.hibernate.mapping.Collection;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;




@PageTitle("Data Feeds")
@Route(value = "datafeed", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class DataFeedView  extends MasterDetailLayout<DataFeeds, DataFeedsService> implements BeforeEnterObserver{
	
	private enum LogMode { ALL, OK, ERROR }
	
	// entity attribute
    private TextArea firstQuery=new TextArea("First Query");
    private TextField dataFeedName=new TextField("Data Feed Name");
    private Checkbox runInBackground=new Checkbox("Run in the background?");
    private TimePicker  scheduleStartTime=new TimePicker ("schedule start time");
    private TimePicker  scheduleEndTime=new TimePicker ("schedule end time");
    private IntegerField frequencyValue= new IntegerField("Frequency Value");
    private ComboBox<String> frequencyUnits = new ComboBox<>("Frequency Units", List.of("Minute(s)", "Hour(s)", "Day(s)"));
    
    //share data

   
    private final BeanValidationBinder<DataFeeds> binder= new BeanValidationBinder<>(DataFeeds.class); 
//Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");
 
    
    
    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");
    
    
	 public DataFeedView(DataFeedsService service,UserService userService,AuthenticatedUser authenticatedUser) { 
		 	super(service,userService,authenticatedUser);
		 	//this.currentUserLogin=authenticatedUser.get();


	 }
	 
	 @Override
	    protected void onAttach(AttachEvent attachEvent) {
	        super.onAttach(attachEvent);
	        
	        UI.getCurrent().access(() -> {
	        	configureGrid();
		        configureEditorLayout();    	        
		        binderField();
		        createAdvanceFilterLayout(); 

	        });

	    }
	 
	
	private void binderField() {
		binder.bindInstanceFields(this);
 
	}


	@Override
	 protected void configureGrid() {
	    	grid.addColumn(DataFeeds::getId).setHeader("ID").setFooter("Total Records:").setKey("id");
	        grid.addColumn(DataFeeds::getDataFeedName).setHeader(this.dataFeedName.getLabel()).setKey("dataFeedName");
	        //grid.addColumn(DataFeeds::getFirstQuery).setHeader(this.firstQuery.getLabel()).setKey("firstQuery");
	        grid.addColumn(DataFeeds::getScheduleStartTime).setHeader(this.scheduleStartTime.getLabel()).setKey("scheduleStartTime"); 
	        grid.addColumn(DataFeeds::getScheduleEndTime).setHeader(this.scheduleEndTime.getLabel()).setKey("scheduleEndTime");
	        grid.addColumn(DataFeeds::getFrequencyValue).setHeader(this.frequencyValue.getLabel()).setKey("frequencyValue");
	        grid.addColumn(DataFeeds::getFrequencyUnits).setHeader(this.frequencyUnits.getLabel()).setKey("frequencyUnits");
	        //grid.addColumn(DataFeeds::getRunInBackground).setHeader(this.runInBackground.getLabel()).setKey("runInBackground"); 
	        grid.addComponentColumn(entitySelected -> createYesNoIcon(entitySelected.getRunInBackground())).setHeader(this.runInBackground.getLabel()).setKey("runInBackground").setSortProperty("runInBackground");
	        grid.addColumn(entityRow -> {        	
	        	return entityRow.getLastRun() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRow.getLastRun()) : "";
	        }).setHeader("Last run").setSortProperty("lastRun").setKey("lastRun");
	        	        
	        grid.addColumn(DataFeeds::getLastBatchID).setHeader("Last Batch ID").setKey("lastBatchID");
	        
	        // === NEW COLUMN: Error Count (group by parentPrimaryKey fallback to id) ===
	        /*grid.addColumn(DataFeedView::computeErrorGroupCount)
	            .setHeader("Error Count")
	            .setKey("errorCount")	           
	            .setAutoWidth(true)
	            .setTextAlign(ColumnTextAlign.CENTER);
	            */
	        
	        grid.addColumn(DataFeeds::getErrorCount)
	        .setHeader("Error Count")
	        .setKey("errorCount")
	        .setAutoWidth(true)
	        .setTextAlign(ColumnTextAlign.CENTER);
	        
	        
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
	            
	            column.setTextAlign(ColumnTextAlign.CENTER);
	            if(column.getKey()!="errorCount") {
	            	column.setSortable(true);    // Enable sorting for all columns
	            }
	            if(column.getHeaderText() !="ID") {
	            	 column.setAutoWidth(true);
	            }
	           
	        });
	        
	        grid.setItemDetailsRenderer(this.createTabRenderer());
	        createShowHideColumnGridToolBar();   
	        
	 }
	  // === NEW: helper to compute grouped error count per DataFeeds row ===
  /*  private static int computeErrorGroupCount(DataFeeds entity) {
        if (entity == null || entity.getDataFeedsLogs() == null || entity.getDataFeedsLogs().isEmpty()) return 0;
        long groupsWithErrors = entity.getDataFeedsLogs().stream()
            .filter(log -> Boolean.TRUE.equals(log.getHasErrors()))
            .map(log -> log.getParentPrimaryKey() != null
                ? log.getParentPrimaryKey().toString()
                : String.valueOf(log.getId()))
            .distinct()
            .count();
        return (int) groupsWithErrors;
    }
    */

	private Icon createYesNoIcon(Boolean pm) {
        Icon icon;
        if (pm) {
            icon = VaadinIcon.CHECK.create();
            icon.getElement().getThemeList().add("badge success");
        } else {
            icon = VaadinIcon.CLOSE_SMALL.create();
            icon.getElement().getThemeList().add("badge error");
        }
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
	}
	private ComponentRenderer<Component, DataFeeds> createTabRenderer() {
        return new ComponentRenderer<>(entityRecord -> {
        	
        	TabSheet tabSheet = new TabSheet();  
        	//For Tab Step
        	tabSheet.add("Step",this.stepTab(entityRecord));
        	//For Tab Log
        	tabSheet.add("Log",this.logTab(entityRecord));
        	
        	tabSheet.getStyle().set("border", "1px solid #ccc");
        	tabSheet.setWidthFull();

            return tabSheet;
        });
	}
	private Component logTab(DataFeeds entityRecord) {
        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Show all logs", this.tabShowAllLog(entityRecord));
        tabSheet.add("Show only logs without errors", this.tabShowOnlyWithoutErrors(entityRecord));
        tabSheet.add("Show only logs with errors", this.tabShowOnlyWithErrors(entityRecord));
        tabSheet.getStyle().set("border", "1px solid #ccc");
        tabSheet.setWidthFull();
        tabSheet.setSizeFull();
        return tabSheet;
    }
	
    // --- FULL LOGS with details renderer
	// ALL
	private Component tabShowAllLog(DataFeeds df) {
	    return buildPagedGroupedGrid(df.getId(), LogMode.ALL);
	}
	// ONLY SUCCESS (no errors)
	private Component tabShowOnlyWithoutErrors(DataFeeds df) {
	    return buildPagedGroupedGrid(df.getId(), LogMode.OK);
	}
	// ONLY FAILED
	private Component tabShowOnlyWithErrors(DataFeeds df) {
	    return buildPagedGroupedGrid(df.getId(), LogMode.ERROR);
	}

	private Component buildPagedGroupedGrid(Long feedId, LogMode mode) {
	    Function<DataFeedsLog, Long> occFn = log -> {
	        UUID ppk = log.getParentPrimaryKey();
	        Long selfId = (ppk == null ? log.getId() : null);
	        return service.countOccurrences(feedId, mode.name(), ppk, selfId);
	    };

	    Grid<DataFeedsLog> grid = buildLogsGrid(occFn);  // <-- pass the fn so "Occurrences" column renders

	    CallbackDataProvider<DataFeedsLog, Void> dp = new CallbackDataProvider<>(
	        q -> service.pageLatestGroups(feedId, mode.name(), q.getOffset(), q.getLimit()).stream(),
	        q -> service.countLatestGroups(feedId, mode.name())
	    );

	    grid.setItems(dp);
	    grid.setPageSize(25);
	    grid.setHeight("600px");
	    return grid;
	}
  
    private Grid<DataFeedsLog> buildLogsGrid(java.util.function.Function<DataFeedsLog, Long> occurrencesFn) {
        Grid<DataFeedsLog> gridLogAll = new Grid<>(DataFeedsLog.class, false);

        gridLogAll.addColumn(DataFeedsLog::getId)
            .setHeader("ID")
            .setKey("id")
            .setAutoWidth(true)
            .setTextAlign(ColumnTextAlign.CENTER);

        gridLogAll.addColumn(DataFeedsLog::getBatchID)
            .setHeader("Batch ID")
            .setKey("batchID")
            .setAutoWidth(true);

        gridLogAll.addComponentColumn(entitySelected -> {
            Boolean hasErrors = entitySelected.getHasErrors();
            if (hasErrors == null) {
                return new Span(""); // empty
            }

            Span statusSpan;
            if (!hasErrors) {
                statusSpan = new Span("Success");
                statusSpan.getElement().getThemeList().add("badge success"); // green
            } else {
                statusSpan = new Span("Failed");
                statusSpan.getElement().getThemeList().add("badge error"); // red
            }

            return statusSpan;
        })
        .setHeader("Status")
        .setKey("hasErrors")
        .setAutoWidth(true);


        gridLogAll.addColumn(entitySelected ->
                entitySelected.getRunStart() != null
                    ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getRunStart())
                    : "")
            .setHeader("Run Start")
            .setKey("runStart")
            .setAutoWidth(true);

        gridLogAll.addColumn(entitySelected ->
                entitySelected.getRunEnd() != null
                    ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getRunEnd())
                    : "")
            .setHeader("Run End")            
            .setKey("runEnd")
            .setAutoWidth(true);

        gridLogAll.addColumn(DataFeedsLog::getParentPrimaryKey)
            .setHeader("Parent Primary Key")
            .setKey("parentPrimaryKey")
            .setAutoWidth(true);
        
        
        // 🔹 Add Occurrences if a provider is supplied
        if (occurrencesFn != null) {
            Grid.Column<DataFeedsLog> occCol = gridLogAll.addColumn(log -> {
                    Long v = occurrencesFn.apply(log);
                    return (v == null || v < 0) ? "" : String.valueOf(v);
                })
                .setHeader("Occurrences")
                .setKey("occurrences")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setAutoWidth(true)
                .setSortable(true);

            occCol.setComparator((a, b) -> {
                long ca = Optional.ofNullable(occurrencesFn.apply(a)).orElse(-1L);
                long cb = Optional.ofNullable(occurrencesFn.apply(b)).orElse(-1L);
                return Long.compare(ca, cb);
            });
        }
        
 

        // Common grid tuning
        gridLogAll.getColumns().forEach(column -> {
            column.setResizable(true);
            //column.setSortable(true);
        });
        gridLogAll.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // Details renderer
        gridLogAll.setItemDetailsRenderer(new ComponentRenderer<>(log -> {
            DataFeedLogDetailsLayout details = new DataFeedLogDetailsLayout();
            details.setLog(log);
            return details;
        }));

        // Double-click to toggle details
        gridLogAll.addItemDoubleClickListener(ev ->
            gridLogAll.setDetailsVisible(ev.getItem(), !gridLogAll.isDetailsVisible(ev.getItem()))
        );

        return gridLogAll;
    }


	private VerticalLayout stepTab(DataFeeds entityRecord) {
		
    	Grid<DataFeedsStep> gridStep =new Grid <>(DataFeedsStep.class, false);
    	gridStep.addColumn(DataFeedsStep::getId)
        .setHeader("ID")
        .setFooter("Total Records:")
        .setKey("id");
    	
    	gridStep.addColumn(DataFeedsStep::getStepDescription)
        .setHeader("Step Description")
        .setKey("stepDescription");
    	
    	gridStep.addColumn(DataFeedsStep::getStepNumber)
        .setHeader("Step Number")
        .setKey("stepNumber");
    	
    	gridStep.addColumn(DataFeedsStep::getIsActive)
        .setHeader("Is Active")
        .setKey("IsActive");
    	
    	gridStep.addColumn(DataFeedsStep::getStepQuery)
        .setHeader("Step Query")
        .setKey("stepQuery");
    	  		
    	
    	gridStep.addColumn(entitySelected -> {
	        return entitySelected.getUserCreated() != null ? entitySelected.getUserCreated().getName() : "";
	    }).setHeader("Created by")
	      .setSortProperty("userCreated.name")
	      .setKey("userCreated.name");

    	gridStep.addColumn(entitySelected -> {
	        return entitySelected.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getCreatedAt()) : "";
	    }).setHeader("Created At")
	      .setSortProperty("createdAt")
	      .setKey("createdAt");

    	gridStep.addColumn(entitySelected -> {
	        return entitySelected.getUserUpdated() != null ? entitySelected.getUserUpdated().getName() : "";
	    }).setHeader("Updated by")
	      .setSortProperty("userUpdated.name")
	      .setKey("userUpdated.name");

    	gridStep.addColumn(entitySelected -> {
	        return entitySelected.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entitySelected.getUpdatedAt()) : "";
	    }).setHeader("Updated At")
	      .setSortProperty("updatedAt")
	      .setKey("updatedAt");
    	
    	List<DataFeedsStep> sortedSteps = new ArrayList<>(entityRecord.getDataFeedsSteps());
    	sortedSteps.sort(Comparator.comparing(DataFeedsStep::getStepNumber));
    	gridStep.setItems(sortedSteps);

    	gridStep.getColumnByKey("id").setFooter("Total Records: " + entityRecord.getDataFeedsSteps().size());
    	
    	gridStep.getColumns().forEach(column -> {
            column.setResizable(true);   // Enable resizing for all columns
            column.setSortable(true);    // Enable sorting for all columns
            //column.setTextAlign(ColumnTextAlign.CENTER);
            if(column.getHeaderText() !="ID") {
            	 column.setAutoWidth(true);
            }
           
        });
    	
    	
    	VerticalLayout statusLayout =new VerticalLayout(new HorizontalLayout( new Button("Full View", new Icon(VaadinIcon.EXPAND_FULL),ee-> { UI.getCurrent().navigate("datafeedstep/" + entityRecord.getId().toString());})), gridStep);
    	statusLayout.setMargin(false);
    	statusLayout.setPadding(false);
    	return statusLayout;
	}
	

	 @Override
	 protected void configureEditorLayout() {
	        FormLayout formLayout = new FormLayout();
	        formLayout.add( this.dataFeedName,this.firstQuery,this.runInBackground,this.scheduleStartTime,scheduleEndTime,this.frequencyValue,this.frequencyUnits);
	        formLayout.setColspan(firstQuery,2);
	        HorizontalLayout buttonLayout = new HorizontalLayout();
	        buttonLayout.setClassName("button-layout");
	        
	        Button btnCancel=new Button("Cancel",e ->{closeForm();});
	        Button btnSave =new Button("Save", e -> save());

	        buttonLayout.add(btnSave, btnCancel);
	        editorLayout.add(formLayout,buttonLayout); 
	        

	        btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_WARNING);
	        btnCancel.addClickShortcut(Key.ESCAPE);
	        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_SUCCESS);
	        btnSave.addClickShortcut(Key.ENTER);
   
	        //this.name.setRequiredIndicatorVisible(true);	         	     
	 }


	    private void save() {
	    	try {
	    		if (entity == null) {
	                throw new IllegalStateException("No entity is set for saving.");
	            }
	            binder.writeBean(entity);
                service.update(entity); 
                Notification.show("Data saved successfully",1000,Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                clearForm();
               	closeForm();
                refreshGrid();   
              
            } catch (ObjectOptimisticLockingFailureException exception) {
            	Notification.show("Error updating the data. Somebody else has updated the record while you were making changes.",9000,Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);            	
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid",9000,Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }catch (DataAccessException dataAccessException) {
                // Handle SQL exceptions here
                Notification.show("Database error: " + dataAccessException.getMessage(),9000,Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
               
            } catch (Exception ex) {
                // Catch all for any other exceptions
                Notification.show("An unexpected error occurred: " + ex.getMessage(),9000,Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
                
            }
	    }
	    @Override
	    protected void populateForm(DataFeeds entity) {
	        binder.readBean(entity); // Populate the form using the binder
	        editorLayout.setVisible(true); // Show the editor layout
	    }
	    
	    private void clearForm() {
	        //populateForm(null);
	    	 binder.readBean(null);
	    	 this.entity = null;
	    }
	    private void closeForm() {
	    	editorLayout.setVisible(false);
	    	//this.entity=null;
	    }
	    
	    
	    @Override
	    protected Specification<DataFeeds> buildCombinedSpecification() {
	        return (root, query, criteriaBuilder) -> {
	            List<Predicate> predicates = new ArrayList<>();
	            List<String> sqlFilter = new ArrayList<>();


	                     	            
	            Join<DataFeeds, User> userCreatedJoin = root.join("userCreated", JoinType.LEFT);
	            Join<DataFeeds, User> userUpdatedJoin = root.join("userUpdated", JoinType.LEFT);	 
	            //Join<DataFeeds, HREmployeeData> supervisorJoin = root.join("supervisor", JoinType.INNER);	
	            
	            //Default Filter
	            //predicates.add(criteriaBuilder.equal(supervisorJoin.get("insurance"),currentUserLogin.get().getInsurance()));
	            
	       	            
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
	        this.dataFeedName.focus(); // Focus the "name" field
	    }
	    
	    
	    @Override
	    protected DataFeeds createNewEntity() {
	        return new DataFeeds(); // Initialize a new ServiceType entity
	    }

	
	    @Override
	    public void beforeEnter(BeforeEnterEvent event) {
	    	if(!authenticatedUser.hasPage(DataFeedView.class,AccessPageType.SELECTED_PAGE)) {
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
	            StreamResource resource = new StreamResource("DataFeeds.xlsx", () -> {
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	                try (Workbook workbook = new XSSFWorkbook()) {
	                    Sheet sheet = workbook.createSheet("Category");

	                    // Write headers dynamically from grid columns
	                    Row headerRow = sheet.createRow(0);
	                    List<Grid.Column<DataFeeds>> columns = grid.getColumns();
	                    for (int i = 0; i < columns.size(); i++) {
	                        headerRow.createCell(i).setCellValue(columns.get(i).getHeaderText());
	                    }

	                    // Write data
	                    List<DataFeeds> itemsToExport = grid.getSelectedItems().isEmpty() ?
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
	                                        Field field = getFieldFromClassHierarchy(DataFeeds.class, columnKey);
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
	    
	    
	 // ===== Details component for logs (step_sqls only) =====
	    private static class DataFeedLogDetailsLayout extends VerticalLayout {
	        private final VerticalLayout details = new VerticalLayout();
	        private final ObjectMapper mapper = new ObjectMapper();

	        DataFeedLogDetailsLayout() {
	            setPadding(false);
	            setSpacing(false);
	            setMargin(false);
	            details.setSizeFull();
	            
                details.setPadding(false);
                details.setMargin(false);
                details.setSpacing(false);
	           
	            add(details);
	        }

	        void setLog(DataFeedsLog log) {
	            details.removeAll();

	            String data = log.getData();
	            if (data == null || data.isBlank()) {
	                details.add(new Span("No step_sqls data"));
	                return;
	            }

	            try {
	                JsonNode root = mapper.readTree(data);
	                JsonNode steps = root.path("step_sqls");
	                if (!steps.isArray() || steps.isEmpty()) {
	                    details.add(new Span("No steps found"));
	                    return;
	                }

	                for (JsonNode stepNode : steps) {
	                    String step = textOf(stepNode, "step");
	                    String status = textOf(stepNode, "status");
	                    String sql = textOf(stepNode, "sql");
	                    String err = textOf(stepNode, "error_message");
	                    String affected_rows = textOf(stepNode, "affected_rows");
	                    String execution_time_ms = textOf(stepNode, "execution_time_ms");

	                    Span stepLabel = new Span(step != null ? step : "step");

	                    // 1) Build a ONE-COLUMN form for the content
	                    FormLayout stepContent = new FormLayout();
	                    stepContent.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

	                    // SQL
	                    stepContent.addFormItem(new Span(sql != null ? sql : ""), "SQL");

	                    // Rows affected
	                    if (affected_rows != null && !affected_rows.isBlank()) {
	                    	stepContent.addFormItem(new Span(affected_rows != null ? affected_rows : "0"), "Rows affected");
	                    }

	                    // Execution time (ms -> s)
	                    if (execution_time_ms != null && !execution_time_ms.isBlank()) {
	                        try {
	                            double seconds = Double.parseDouble(execution_time_ms) / 1000.0;
	                            stepContent.addFormItem(new Span(String.format("%.3f s", seconds)), "Execution Time");
	                        } catch (NumberFormatException nfe) {
	                            stepContent.addFormItem(new Span(execution_time_ms + " ms"), "Execution Time");
	                        }
	                    }

	                    // Error Message (only if present)
	                    if (err != null && !err.isBlank()) {
	                        stepContent.addFormItem(new Span(err), "Error Message");
	                    }

	                    // 2) Wrap the form in a styled container (so we can color the background cleanly)
	                    Div wrapper = new Div(stepContent);
	                    wrapper.getStyle()
	                        .set("background", bgForStatus(status))
	                        .set("border", "1px solid var(--lumo-contrast-20pct)")
	                        .set("border-radius", "var(--lumo-border-radius-m)")
	                        .set("padding", "var(--lumo-space-m)")
	                        .set("margin", "var(--lumo-space-s) 0");

	                    // 3) Create the collapsible Details with our wrapper as content
	                    
	                 // Status badge
	                    Span statusBadge = new Span(status != null ? status : "unknown");
	                    statusBadge.getElement().getThemeList().add("badge");

	                    if ("success".equalsIgnoreCase(status)) {
	                        statusBadge.getElement().getThemeList().add("success");
	                    } else if ("failed".equalsIgnoreCase(status)) {
	                        statusBadge.getElement().getThemeList().add("error");
	                    } else {
	                        statusBadge.getElement().getThemeList().add("contrast");
	                    }

	                    // Wrap header (step + badge) in a HorizontalLayout
	                    HorizontalLayout header = new HorizontalLayout(stepLabel, statusBadge);
	                    header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
	                    header.setSpacing(true);
	                    header.setPadding(false);
	                    header.setMargin(false);
	                    
	                    Details row = new Details(header, wrapper);
	                    row.setWidthFull();
	                    row.setOpened(false);

	                    details.add(row);
	                }
	            } catch (Exception e) {
	                details.add(new Span("Invalid JSON: " + e.getMessage()));
	            }
	        }

	        private static String textOf(JsonNode node, String field) {
	            JsonNode n = node.get(field);
	            return (n == null || n.isNull()) ? null : n.asText();
	        }
	        
	        private static String bgForStatus(String status) {
	            if (status == null) return "var(--lumo-contrast-5pct)";
	            if ("failed".equalsIgnoreCase(status))  return "var(--lumo-error-color-10pct)";
	            if ("success".equalsIgnoreCase(status)) return "var(--lumo-success-color-10pct)";
	            return "var(--lumo-contrast-5pct)";
	        }
	    }
}
