package org.halocambodia.views.data_feed;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.*;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.halocambodia.views.MainLayout;
import org.halocambodia.views.MasterDetailLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.function.SerializableFunction;




@PageTitle("Data Feeds Step")
@Route(value = "datafeedstep/:id?", layout = MainLayout.class)
@PermitAll
public class DataFeedStepView extends MasterDetailLayout<DataFeedsStep, DataFeedsStepService> implements BeforeEnterObserver{
	
	// entity attribute
	private ComboBox<DataFeeds> dataFeeds=new ComboBox<DataFeeds>("Data Feed");
	private TextField stepDescription=new TextField("Step Description");
	private IntegerField stepNumber =new IntegerField("Step Number");
	private Checkbox isActive=new Checkbox("Is Active");
	private TextArea stepQuery=new TextArea("Step Query");	




	 
	 
    private final BeanValidationBinder<DataFeedsStep> binder= new BeanValidationBinder<>(DataFeedsStep.class);
     
//Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");
   
   

  
    
    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");
    
    
    //Share List
    private Optional<Long> masterID;
    private List<DataFeeds> masterItems;
    private DataFeedsRepository dataFeedsRepository;

    
	 public DataFeedStepView(DataFeedsStepService service,UserService userService,AuthenticatedUser authenticatedUser,DataFeedsRepository dataFeedsRepository) { 
		 	super(service,userService,authenticatedUser);
		 	this.dataFeedsRepository=dataFeedsRepository;

	 }
	 
	
	private void binderField() {
		binder.bindInstanceFields(this);
	}


	@Override
	protected void configureGrid() {
	    grid.addColumn(DataFeedsStep::getId)
	        .setHeader("ID")
	        .setFooter("Total Records:")
	        .setKey("id");
	    
    
	    grid.addColumn(DataFeedsStep::getStepDescription)
        .setHeader(this.stepDescription.getLabel())
        .setSortProperty("stepDescription")
        .setKey("stepDescription");
	    
	    grid.addColumn(DataFeedsStep::getStepNumber)
        .setHeader(this.stepNumber.getLabel())
        .setSortProperty("stepNumber")
        .setKey("stepNumber");
	    
	    grid.addColumn(DataFeedsStep::getIsActive)
        .setHeader(this.isActive.getLabel())
        .setSortProperty("isActive")
        .setKey("isActive");
	    
	    grid.addColumn(DataFeedsStep::getStepQuery)
        .setHeader(this.stepQuery.getLabel())
        .setSortProperty("stepQuery")
        .setKey("stepQuery");

	    grid.addColumn(entityRowUserCreated -> {
	        User userCreated = entityRowUserCreated.getUserCreated(); // Get the related User object
	        return userCreated != null ? userCreated.getName() : ""; // Display the username or a default value
	    }).setHeader("Created by")
	      .setSortProperty("userCreated.name")
	      .setKey("userCreated.name");

	    grid.addColumn(entityRowUserCreatedAt -> {
	        return entityRowUserCreatedAt.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRowUserCreatedAt.getCreatedAt()) : "";
	    }).setHeader("Created At")
	      .setSortProperty("createdAt")
	      .setKey("createdAt");

	    grid.addColumn(entityRowUserUpdated -> {
	        User userUpdated = entityRowUserUpdated.getUserCreated(); // Get the related User object
	        return userUpdated != null ? userUpdated.getName() : ""; // Display the username or a default value
	    }).setHeader("Updated by")
	      .setSortProperty("userUpdated.name")
	      .setKey("userUpdated.name");

	    grid.addColumn(entityRowUserUpdateddAt -> {
	        return entityRowUserUpdateddAt.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entityRowUserUpdateddAt.getUpdatedAt()) : "";
	    }).setHeader("Updated At")
	      .setSortProperty("updatedAt")
	      .setKey("updatedAt");

	    grid.getColumns().forEach(column -> {
	        column.setResizable(true);   // Enable resizing for all columns
	        column.setSortable(true);    // Enable sorting for all columns
	        //column.setTextAlign(ColumnTextAlign.CENTER);
	        if (!column.getHeaderText().equals("ID")) {
	            column.setAutoWidth(true);
	        }
	    });
	    createShowHideColumnGridToolBar(); 
	}


	 @Override
	 protected void configureEditorLayout() {
	        FormLayout formLayout = new FormLayout();
	        //formLayout.setWidthFull();
	        //formLayout.setAutoResponsive(true);
	        formLayout.setSizeUndefined();
	        formLayout.setMaxColumns(1);
	        formLayout.add(this.dataFeeds,this.stepDescription,this.stepNumber,this.isActive,this.stepQuery );
	        formLayout.setColspan(stepQuery,2);
	        stepQuery.getStyle().set("resize", "both");
	        stepQuery.setMinHeight("120px");
	        
	        stepDescription.setWidthFull();
	        HorizontalLayout buttonLayout = new HorizontalLayout();
	        buttonLayout.setClassName("button-layout");
	        
	        Button btnCancel=new Button("Cancel",e ->{closeForm();});
	        Button btnSave =new Button("Save", e -> save());
	        
	        btnSave.getStyle().set("border", "1px solid red");
	        btnSave.getStyle().set("display", "block");

	        btnCancel.getStyle().set("border", "1px solid blue");
	        btnCancel.getStyle().set("display", "block");

	        buttonLayout.add(btnSave, btnCancel);
	        buttonLayout.setVisible(true);
	        buttonLayout.setWidthFull();
	        buttonLayout.setWidthFull();
	        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
	        
	        editorLayout.add(formLayout,buttonLayout); 
	        
	        editorLayout.setSizeFull();
	        editorLayout.getStyle().set("overflow", "auto");
	        
	        
	        
	        btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_WARNING);
	        btnCancel.addClickShortcut(Key.ESCAPE);
	        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_SUCCESS);
	        btnSave.addClickShortcut(Key.ENTER);
	        


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
            	 ex.printStackTrace();
                // Catch all for any other exceptions
                Notification.show("An unexpected error occurred: " + ex.getMessage(),9000,Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
                
            }
	    }
	    @Override
	    protected void populateForm(DataFeedsStep entity) {
	        binder.readBean(entity); // Populate the form using the binder
	        editorLayout.setVisible(true); // Show the editor layout
	        
	        this.dataFeeds.setValue(this.dataFeeds.getDataProvider().fetch(new Query<>()).findFirst().orElse(null));
	    	this.dataFeeds.setReadOnly(true);
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
	    protected Specification<DataFeedsStep> buildCombinedSpecification() {
	        return (root, query, criteriaBuilder) -> {
	            List<Predicate> predicates = new ArrayList<>();
	            List<String> sqlFilter = new ArrayList<>();

	            Join<DataFeedsStep, User> userCreatedJoin = root.join("userCreated", JoinType.LEFT);
	            Join<DataFeedsStep, User> userUpdatedJoin = root.join("userUpdated", JoinType.LEFT);
	           

	            // Parent Location Filter
	            this.masterID.ifPresent(id -> predicates.add(
	                criteriaBuilder.equal(root.get("dataFeeds"), this.masterItems.getFirst())
	            ));

	            // Quick Search Filter
	            String quickSearchValue = txtQuick.getValue();
	            if (quickSearchValue != null && !quickSearchValue.isEmpty()) {
	                String likePattern = "%" + quickSearchValue.toLowerCase().trim() + "%";
	                predicates.add(criteriaBuilder.or(
	                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(root.get("id"), criteriaBuilder.literal(""))), likePattern),
    

	                    criteriaBuilder.like(criteriaBuilder.lower(userCreatedJoin.get("name")), likePattern),
	                    criteriaBuilder.like(criteriaBuilder.lower(
	                        DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(criteriaBuilder, root.get("createdAt"))), likePattern),
	                    criteriaBuilder.like(criteriaBuilder.lower(userUpdatedJoin.get("name")), likePattern),
	                    criteriaBuilder.like(criteriaBuilder.lower(
	                        DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(criteriaBuilder, root.get("updatedAt"))), likePattern)
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
		                Join<GoalSettingDetail, User> userJoinUpdated = root.join("userUpdated", JoinType.LEFT);
		                Set<String> userNamesUpdated = advanceFilterUpdatedBy.getValue().stream()
		                    .map(User::getName)
		                    .collect(Collectors.toSet());
		                predicates.add(userJoinUpdated.get("name").in(userNamesUpdated));
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
	        this.stepDescription.focus(); // Focus the "name" field
	    }
	    
	    
	    @Override
	    protected DataFeedsStep createNewEntity() {
	        DataFeedsStep step = new DataFeedsStep();

	        if (masterItems != null && !masterItems.isEmpty()) {
	            step.setDataFeeds(masterItems.get(0));
	        }

	        return step;
	    }
	    


	
	    @Override
	    public void beforeEnter(BeforeEnterEvent event) {
	    	if(!authenticatedUser.hasPage(DataFeedStepView.class,AccessPageType.SELECTED_PAGE)) {
	    		 event.rerouteTo(AccessDeniedView.class);
	    		 

	    	}
   		 	masterID = event.getRouteParameters().get("id").map(Long::parseLong);
   		 	masterItems = this.dataFeedsRepository.findAll((root, query, criteriaBuilder) ->  criteriaBuilder.equal(root.get("id"), masterID.get()));
    
   		 	parentLocationDetail();
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
	            StreamResource resource = new StreamResource("DataFeedStep.xlsx", () -> {
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	                try (Workbook workbook = new XSSFWorkbook()) {
	                    Sheet sheet = workbook.createSheet("DataFeedStep");

	                    // Write headers dynamically from grid columns
	                    Row headerRow = sheet.createRow(0);
	                    List<Grid.Column<DataFeedsStep>> columns = grid.getColumns();
	                    for (int i = 0; i < columns.size(); i++) {
	                        headerRow.createCell(i).setCellValue(columns.get(i).getHeaderText());
	                    }

	                    // Write data
	                    List<DataFeedsStep> itemsToExport = grid.getSelectedItems().isEmpty() ?
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
	                                        Field field = getFieldFromClassHierarchy(DataFeedsStep.class, columnKey);
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

		private void parentLocationDetail() {
			//Master Record
			TextField dataFeedName=new TextField("Data Feed Name");
			dataFeedName.setValue(this.masterItems.get(0).getDataFeedName() );
			dataFeedName.setReadOnly(true);
			
			TextArea firstQuery=new TextArea("The first query\n"
					+ "This query should retrieve all of the parent records. The steps below will iterate for each row returned. The data returned by the first column will be placed into the variable called :PARENT_PRIMARY_KEY.");
			firstQuery.setValue(this.masterItems.get(0).getFirstQuery() );
			//firstQuery.setReadOnly(true);
			firstQuery.setWidthFull();
	
			
		    
		    Button navigateButton = new Button("Master record (return to list)",new Icon(VaadinIcon.ARROW_LEFT));
		    navigateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		    navigateButton.addClickListener(clickEvent -> { UI.getCurrent().navigate("datafeed"); });
		    
		    //masterRecord.getStyle().set("background-color", "lightblue");
		    masterRecord.setPadding(false);
		    //masterRecord.getStyle().set("margin", "0px");
		    //masterRecord.getStyle().set("padding", "0px");
		    masterRecord.setWidthFull();
		    Details parentDetails = new Details(new H3("Master record information"), dataFeedName,firstQuery);
		    parentDetails.getStyle()
		    .set("border", "1px solid black") // Add a black border
		    .set("padding", "0px")    
		    .set("margin", "0px") // Optional: Add padding
		    .set("border-radius", "10px");    // Optional: Add rounded corners
		    parentDetails.setWidthFull();
				    
		    parentDetails.setOpened(true);
		    masterRecord.add(navigateButton,parentDetails  );
		    masterRecord.setVisible(true);
		}
    
	    @Override
	    protected void onAttach(AttachEvent attachEvent) {
	        super.onAttach(attachEvent);
	        
	        UI.getCurrent().access(() -> {
	        	configureGrid();
		        configureEditorLayout();    	        
		        binderField();
		        createAdvanceFilterLayout(); 

		        this.dataFeeds.setItems(this.masterItems);		        
		        this.dataFeeds.setItemLabelGenerator(g -> g.getDataFeedName());
		        this.dataFeeds.setValue(this.masterItems.get(0));
		        this.dataFeeds.setReadOnly(true);
		        this.dataFeeds.setVisible(false); // if you want to hide it

		        this.dataFeeds.setVisible(false);
  
	        });  	 	        
	    }
	    
}
