package org.halocambodia.views.admin.role_management;

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
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
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
import org.halocambodia.data.PermissionRepository;
import org.halocambodia.data.Permissions;
import org.halocambodia.data.Positions;
import org.halocambodia.data.Role;
import org.halocambodia.data.RolePermission;
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
import org.halocambodia.services.GoalSettingEntryService;
import org.halocambodia.services.GoalSettingService;
import org.halocambodia.services.RoleService;
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


@PageTitle("Role")
@Route(value = "role-management", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class RoleManagementView  extends MasterPageDialogLayout<Role, RoleService> implements BeforeEnterObserver {
	
	// entity attribute	
	private TextField name=new TextField("Role");
    private MultiSelectComboBox<Branch> branchs=new MultiSelectComboBox("Location");
    private com.vaadin.flow.component.checkbox.Checkbox canSeeSalary=new com.vaadin.flow.component.checkbox.Checkbox("Can See Salary");
    //share data
    private final Optional<User> currentUserLogin;
    private final BranchRepository branchRepository;
    private final PermissionRepository permissionRepository;
       
    private final BeanValidationBinder<Role> binder= new BeanValidationBinder<>(Role.class);
   
//Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");

    Grid<RolePermission> gridRolePermission = new Grid<>(RolePermission.class, false);
    
	 public RoleManagementView(RoleService service,UserService userService,AuthenticatedUser authenticatedUser,BranchRepository branchRepository,PermissionRepository permissionRepository) { 
		 	super(service,userService,authenticatedUser);
		 	this.currentUserLogin=authenticatedUser.get();
		 	this.branchRepository=branchRepository;
		 	this.permissionRepository=permissionRepository;
		 	
        	configureGrid();
	        configureEditorLayout();    	        
	        binderField();
	       	createAdvanceFilterLayout(); 
	
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

	 }

	@Override
	 protected void configureGrid() {
		grid.addColumn(Role::getId).setHeader("ID").setFooter("Total Records:").setKey("id");	          
		grid.addColumn(Role::getName).setHeader("Role").setKey("name");
		grid.addColumn(entitySelected -> entitySelected.getBranchs().stream()
				.sorted(Comparator.comparing(Branch::getBranchShortName))
				.map(Branch::getBranchShortName) 
				.collect(Collectors.joining(", "))
		).setHeader("Locations").setSortProperty("branchs.branchShortName").setKey("branchs.branchShortName");	  

		grid.addComponentColumn(e -> createPermissionIcon(e.getCanSeeSalary())).setHeader(canSeeSalary.getLabel()).setKey("canSeeSalary");

		grid.getColumns().forEach(column -> {
			column.setResizable(true);   // Enable resizing for all columns
	          // Enable sorting for all columns
	        column.setTextAlign(ColumnTextAlign.CENTER);
	        column.setSortable(true); 
	        if(column.getKey().equalsIgnoreCase("branchs.branchShortName") ) {
	        	column.setSortable(false);
	        }
	        if(!column.getKey().equalsIgnoreCase("id") ) {
	        	column.setAutoWidth(true);
	        }
		});

		
		
		grid.setItemDetailsRenderer(this.createTabRenderer());
	    createShowHideColumnGridToolBar();   
	        
	    grid.setAllRowsVisible(true);
	        
	 }
	private ComponentRenderer<Component, Role> createTabRenderer() {
        return new ComponentRenderer<>(entityRecord -> {
        	
        	
        	Grid<RolePermission> gridRolePermission =new Grid <>(RolePermission.class, false);
        	gridRolePermission.addColumn(RolePermission::getId)
            	.setHeader("ID")
            	.setFooter("Total Records:")
            	.setKey("id");
        	
        	gridRolePermission.addColumn(entitySelected -> {
	            return entitySelected.getPermission() != null ? entitySelected.getPermission().getRouteName()  : ""; 
	        }).setHeader("Pages").setSortProperty("permission.routeName").setKey("permission.routeName").setAutoWidth(true);
        	
        	gridRolePermission.addComponentColumn(entitySelected -> createPermissionIcon(entitySelected.getAdminPage())).setHeader("Admin").setKey("adminPage");
        	gridRolePermission.addComponentColumn(entitySelected -> createPermissionIcon(entitySelected.getSelectedPage())).setHeader("View").setKey("selectedPage");
        	gridRolePermission.addComponentColumn(entitySelected -> createPermissionIcon(entitySelected.getInsertedPage())).setHeader("Add").setKey("insertedPage");
        	gridRolePermission.addComponentColumn(entitySelected -> createPermissionIcon(entitySelected.getUpdatedPage())).setHeader("Edit").setKey("updatedPage");
        	gridRolePermission.addComponentColumn(entitySelected -> createPermissionIcon(entitySelected.getDeletedPage())).setHeader("Remove").setKey("deletedPage");

        	gridRolePermission.setItems(entityRecord.getRolePermissions());
        	
        	TabSheet tabSheet = new TabSheet();  
        	tabSheet.add("Permissions",gridRolePermission);
        	tabSheet.getStyle().set("border", "1px solid #ccc");
        	tabSheet.setWidthFull();

            return tabSheet;
        });
	}
	private Icon createPermissionIcon(Boolean pm) {
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
	 
	 @Override
	 protected void configureEditorLayout() {

		 VerticalLayout  formLayout = new VerticalLayout ();
	     formLayout.setSizeFull();
	     FormLayout frm=new FormLayout( this.name,this.branchs,canSeeSalary);
         frm.setResponsiveSteps(
                 new FormLayout.ResponsiveStep("0", 1),
                 new FormLayout.ResponsiveStep("600px", 2),
                 new FormLayout.ResponsiveStep("1000px", 3)
         );
	     formLayout.add(frm,initConfigGridPermision());
	     
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
	        
	     editorLayout.setDialogTitle("Role");       	        
	     editorLayout.add(formLayout); 
	     editorLayout.getFooter().add(buttonLayout); 
	     
	     branchs.setItemLabelGenerator(Branch::getBranchShortName);
	     branchs.setAutoExpand(AutoExpandMode.BOTH);
	 }
	 private Component initConfigGridPermision() {
		 gridRolePermission.setSizeFull();
		 gridRolePermission.addColumn(RolePermission::getId).setHeader("ID").setKey("id");	
		 gridRolePermission.addColumn(entitySelected -> {
	            return entitySelected.getPermission() != null ? entitySelected.getPermission().getRouteName()  : ""; 
	        }).setHeader("Pages").setSortProperty("permission.routeName").setKey("permission.routeName").setAutoWidth(true);	
		 // Use helper method to add checkbox columns
		    addCheckboxColumn("Admin", "adminPage", RolePermission::getAdminPage, RolePermission::setAdminPage);
		    addCheckboxColumn("View", "selectedPage", RolePermission::getSelectedPage, RolePermission::setSelectedPage);
		    addCheckboxColumn("Add", "insertedPage", RolePermission::getInsertedPage, RolePermission::setInsertedPage);
		    addCheckboxColumn("Edit", "updatedPage", RolePermission::getUpdatedPage, RolePermission::setUpdatedPage);
		    addCheckboxColumn("Remove", "deletedPage", RolePermission::getDeletedPage, RolePermission::setDeletedPage);

		 VerticalLayout grpLayout = new VerticalLayout(new H3("Permission"), gridRolePermission);
		 grpLayout.setSizeFull();
		 return grpLayout;
	 }
	 private void addCheckboxColumn(String header, String key,
		        java.util.function.Function<RolePermission, Boolean> getter,
		        java.util.function.BiConsumer<RolePermission, Boolean> setter) {

		    Checkbox headerCheckbox = new Checkbox();
		    headerCheckbox.getStyle().set("margin", "0");

		    Span label = new Span(header);
		    HorizontalLayout headerLayout = new HorizontalLayout(label,headerCheckbox );
		    //headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		    headerLayout.setSpacing(true);

		    headerCheckbox.getElement().setAttribute("title", "Check/Uncheck All");
		    headerCheckbox.addValueChangeListener(headerEvent -> {
		        boolean checked = headerEvent.getValue();
		        gridRolePermission.getListDataView().getItems().forEach(rp -> {
		            if ("adminPage".equals(key)) {
		                rp.setAdminPage(checked);
		                if (checked) {
		                    rp.setSelectedPage(false);
		                    rp.setInsertedPage(false);
		                    rp.setUpdatedPage(false);
		                    rp.setDeletedPage(false);
		                }
		            } else {
		                if (Boolean.TRUE.equals(rp.getAdminPage())) {
		                    return;
		                }
		                setter.accept(rp, checked);
		            }
		            gridRolePermission.getDataProvider().refreshItem(rp);
		        });
		    });

		    gridRolePermission.addColumn(new ComponentRenderer<>(rp -> {
		        Checkbox checkbox = new Checkbox(Boolean.TRUE.equals(getter.apply(rp)));

		        checkbox.addValueChangeListener(event -> {
		            boolean newValue = event.getValue();

		            if ("adminPage".equals(key)) {
		                setter.accept(rp, newValue);
		                if (newValue) {
		                    rp.setSelectedPage(false);
		                    rp.setInsertedPage(false);
		                    rp.setUpdatedPage(false);
		                    rp.setDeletedPage(false);
		                }
		            } else {
		                if (Boolean.TRUE.equals(rp.getAdminPage())) {
		                    checkbox.setValue(false);
		                    this.showError("មិនអាចកែប្រែសិទ្ធិដាច់ដោយឡែកបានទេ នៅពេលបានជ្រើស Admin || Cannot modify individual permissions when Admin is selected.");
		                    return;
		                } else {
		                    setter.accept(rp, newValue);
		                }
		            }

		            gridRolePermission.getDataProvider().refreshItem(rp);
		        });

		        return checkbox;
		    }))
		    .setHeader(headerLayout)
		    .setKey(key).setAutoWidth(true).setTextAlign(ColumnTextAlign.CENTER);
		}




	 private void save() {
		 if (entity == null) {
			 showError("No entity to save.");
		     return;
		}
		try {
			
			binder.writeBean(entity);
		    entity = service.update(entity);

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
	 protected void populateForm(Role entity) {
	     binder.readBean(entity); // Populate the form using the binder
	     editorLayout.open();

	     Set<RolePermission> existingPermissions = entity.getRolePermissions();
	     if (existingPermissions == null) {
	         existingPermissions = new HashSet<>();
	         entity.setRolePermissions(existingPermissions);
	     }

	     // Get all permissions from DB
	     List<Permissions> allPermissions = permissionRepository.findAll();

	     // Add only the missing ones
	     for (Permissions permission : allPermissions) {
	         boolean exists = existingPermissions.stream()
	             .anyMatch(rp -> rp.getPermission() != null && rp.getPermission().getId().equals(permission.getId()));

	         if (!exists) {
	             RolePermission newRP = new RolePermission();
	             newRP.setRole(entity);
	             newRP.setPermission(permission);
	             newRP.setAdminPage(false);
	             newRP.setSelectedPage(false);
	             newRP.setInsertedPage(false);
	             newRP.setUpdatedPage(false);
	             newRP.setDeletedPage(false);
	             existingPermissions.add(newRP);
	         }
	     }

	     gridRolePermission.setItems(existingPermissions);
	 }


	 private void clearForm() {
		 binder.readBean(null);
		 this.entity = null;
	 }
	 private void closeForm() {
	 editorLayout.close();
	 }

	 @Override
	 protected Specification<Role> buildCombinedSpecification() {
		 return (root, query, criteriaBuilder) -> {
			 try {
				 List<Predicate> predicates = new ArrayList<>();
		         List<String> sqlFilter = new ArrayList<>();		            		       	           
		         // Quick Search Filter
		         String quickSearchValue = txtQuick.getValue();
		         if (quickSearchValue != null && !quickSearchValue.isEmpty()) {
		        	 String likePattern = "%" + quickSearchValue.toLowerCase().trim() + "%";	          	               
	
		             predicates.add(criteriaBuilder.or(		                	
		            		 criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(root.get("id"), criteriaBuilder.literal(""))), likePattern)		                   
		            ));
		         }
	
		         // Advanced Filters
		         //ID Filter
		         if (advanceFilterID.getValue() != null) {
		                predicates.add(criteriaBuilder.equal(root.get("id"), advanceFilterID.getValue()));
		                sqlFilter.add("ID = " + advanceFilterID.getValue());
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
	    	
	    	 this.advanceSearchLayout.add(advanceFilterID);
	    	//return formLayout;
	    }
	    private void reloadAdvanceFilterData() {

	    }
	    private void reloadEditorData() {
	    	 this.branchs.setItems(branchRepository.findAll());
	    }


	    @Override
	    protected void focusFirstField() {
	        this.name.focus(); // Focus the "name" field
	    }	    
	    
	    @Override
	    protected Role createNewEntity() {
	        return new Role(); // Initialize a new ServiceType entity
	    }
	
	    @Override
	    public void beforeEnter(BeforeEnterEvent event) {
	    	if(!authenticatedUser.hasPage(RoleManagementView.class,AccessPageType.SELECTED_PAGE)) {
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
	            StreamResource resource = new StreamResource("Role.xlsx", () -> {
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	                try (Workbook workbook = new XSSFWorkbook()) {
	                    Sheet sheet = workbook.createSheet("Role");

	                    // Write headers dynamically from grid columns
	                    Row headerRow = sheet.createRow(0);
	                    List<Grid.Column<Role>> columns = grid.getColumns();
	                    for (int i = 0; i < columns.size(); i++) {
	                        headerRow.createCell(i).setCellValue(columns.get(i).getHeaderText());
	                    }

	                    // Write data
	                    List<Role> itemsToExport = grid.getSelectedItems().isEmpty() ?
	                            grid.getGenericDataView().getItems().toList() : new ArrayList<>(grid.getSelectedItems());
	                    AtomicInteger rowIndex = new AtomicInteger(1); // Use AtomicInteger to keep track of row index

	                    itemsToExport.forEach(row2bExport -> {
	                        Row dataRow = sheet.createRow(rowIndex.getAndIncrement());

	                        // Loop over columns dynamically
	                        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
	                            String columnKey = columns.get(columnIndex).getKey(); // Get column key
	                            switch (columnKey) {
	                                // Add other specific cases as needed
	                                default:
	                                    try {
	                                        // Use reflection to check fields in Asset and its superclasses
	                                        Field field = getFieldFromClassHierarchy(Role.class, columnKey);
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
