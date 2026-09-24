package org.halocambodia.views.admin.user_management;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
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
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
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
import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.ResetPasswordDialog;
import org.halocambodia.services.*;
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


@PageTitle("User")
@Route(value = "user-management", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class UserManagementView  extends MasterPageDialogLayout<User, UserService> implements BeforeEnterObserver{
	
	// entity attribute	
    private TextField name=new TextField("Display Name");
    private TextField username=new TextField("User Name");
    private EmailField email=new EmailField("Email");    
    private PasswordField hashedPassword=new PasswordField("Password");
    private PasswordField confirmPassword=new PasswordField("Conform Password");
    private MultiSelectComboBox<Role> roles=new MultiSelectComboBox<Role>("Role");      
    private Checkbox canLogin=new Checkbox("Can Login");
    private Checkbox api=new Checkbox("API");
    private TextField insurance=new TextField("Insurance");

    //share data
    private final Optional<User> currentUserLogin;
    private final RoleRepository roleRepository;
    
    VerticalLayout rulesLayout;
    //Rule checkboxes (disabled to prevent user input)
    Checkbox ruleLength8 = createRuleCheckbox("At least 8 characters long");
    Checkbox ruleLength64 = createRuleCheckbox("Between 10 and 64 characters long");
    Checkbox ruleUpper = createRuleCheckbox("At least 1 uppercase letter");
    Checkbox ruleLower = createRuleCheckbox("At least 1 lowercase letter");
    Checkbox ruleDigit = createRuleCheckbox("At least 1 digit");
    Checkbox ruleSpecial = createRuleCheckbox("At least 1 special character");
    Checkbox ruleGroup4 = createRuleCheckbox("Characters from at least 4 of the groups: uppercase, lowercase, digits, & specials");
    //Checkbox ruleDifferent = createRuleCheckbox("Different from current password");
    Checkbox ruleStrong = createRuleCheckbox("Minimum strength of Strong (simulated)");
    Checkbox ruleMatch = createRuleCheckbox("password and Confirm match");

       
    private final BeanValidationBinder<User> binder= new BeanValidationBinder<>(User.class);
   
//Advance Filter Components
    private NumberField advanceFilterID=new NumberField("ID");    
    private TextField advanceFilterName=new TextField(name.getLabel());
    private TextField advanceFilterUsername=new TextField(username.getLabel());
    private EmailField advanceFilterEmail=new EmailField(email.getLabel());    
    private MultiSelectComboBox<Role> advanceFilterRoles=new MultiSelectComboBox<Role>(roles.getLabel());      
    private ComboBox<String> advanceFilterCanLogin=new ComboBox <String>(canLogin.getLabel(),"Yes","No");
    private ComboBox<String> advanceFilterApi=new ComboBox <String>(api.getLabel(),"Yes","No");
    private TextField advanceFilterInsurance=new TextField(insurance.getLabel());
    


	 public UserManagementView(UserService service,UserService userService,AuthenticatedUser authenticatedUser,RoleRepository roleRepository) { 
		 	super(service,userService,authenticatedUser);
		 	this.currentUserLogin=authenticatedUser.get();
		 	this.roleRepository=roleRepository;

		 	
        	configureGrid();
	        configureEditorLayout();    	        
	        binderField();
	       	createAdvanceFilterLayout(); 
	
	 }
	 private Checkbox createRuleCheckbox(String text) {
	        Checkbox checkbox = new Checkbox(text);
	        checkbox.setReadOnly(true);	       
	        return checkbox;
	  }
	 
     // 🔄 Real-time validation
     Runnable validatePassword = () -> {
         String password = this.hashedPassword.getValue();
         String confirm = confirmPassword.getValue();


         ruleLength8.setValue(password.length() >= 8);           
         ruleLength64.setValue(password.length() >= 10 && password.length() <= 64);
         ruleUpper.setValue(password.matches(".*[A-Z].*"));
         ruleLower.setValue(password.matches(".*[a-z].*"));
         ruleDigit.setValue(password.matches(".*\\d.*"));
         ruleSpecial.setValue(password.matches(".*[^a-zA-Z0-9].*"));

         int groups = 0;
         if (ruleUpper.getValue()) groups++;
         if (ruleLower.getValue()) groups++;
         if (ruleDigit.getValue()) groups++;
         if (ruleSpecial.getValue()) groups++;
         ruleGroup4.setValue(groups >= 4);

         ruleMatch.setValue(password.equals(confirm));
         ruleStrong.setValue(password.length() >= 12 && groups >= 3);
         
     };
     
	    
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

		    // Password validation (only on insert)
		    Binding <User, String> hashedPasswordBinding = binder
		    	    .forField(hashedPassword)
		    	    .withValidator((password, context) -> {
		    	        if (this.entity.getId() == null) {
		    	            return validatePasswordRules(password);
		    	        }
		    	        return ValidationResult.ok();
		    	    })
		    	    .bind(User::getHashedPassword, User::setHashedPassword);

		    	Binding <User, String> confirmPasswordBinding = binder
		    	    .forField(confirmPassword)
		    	    .withValidator((value, context) -> {
		    	        String password = hashedPassword.getValue();
		    	        boolean match = value != null && value.equals(password);
		    	        ruleMatch.setValue(match);

		    	        if (this.entity.getId() == null && !match) {
		    	            return ValidationResult.error("Passwords do not match");
		    	        }
		    	        return ValidationResult.ok();
		    	    })
		    	    .bind(user -> "", (user, value) -> {});



		}
	 private ValidationResult validatePasswordRules(String password) {
		    boolean length8 = password != null && password.length() >= 8;
		    boolean length64 = password != null && password.length() >= 10 && password.length() <= 64;
		    boolean upper = password != null && password.matches(".*[A-Z].*");
		    boolean lower = password != null && password.matches(".*[a-z].*");
		    boolean digit = password != null && password.matches(".*\\d.*");
		    boolean special = password != null && password.matches(".*[^a-zA-Z0-9].*");

		    int groups = 0;
		    if (upper) groups++;
		    if (lower) groups++;
		    if (digit) groups++;
		    if (special) groups++;

		    boolean group4 = groups >= 4;
		    boolean strong = password != null && password.length() >= 12 && group4;

		    // Update rule indicators (you already have these fields)
		    ruleLength8.setValue(length8);
		    ruleLength64.setValue(length64);
		    ruleUpper.setValue(upper);
		    ruleLower.setValue(lower);
		    ruleDigit.setValue(digit);
		    ruleSpecial.setValue(special);
		    ruleGroup4.setValue(group4);
		    ruleStrong.setValue(strong);

		    if (!length8) return ValidationResult.error("Password must be at least 8 characters.");
		    if (!group4) return ValidationResult.error("Password must contain at least 4 groups (upper, lower, digit, special).");

		    return ValidationResult.ok();
		}



	@Override
	 protected void configureGrid() {
		grid.addColumn(User::getId).setHeader("ID").setFooter("Total Records:").setKey("id");	          
		grid.addColumn(User::getName).setHeader(this.name.getLabel()).setKey("name").setSortProperty("name");
		grid.addColumn(User::getUsername).setHeader(this.username.getLabel()).setKey("username").setSortProperty("username");
		grid.addColumn(User::getEmail).setHeader(this.email.getLabel()).setKey("email").setSortProperty("email");
	    //grid.addColumn(user -> user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "))).setHeader(this.roles.getLabel()).setKey("roles.name").setSortProperty("roles.name");
		grid.addColumn(User::getRoleNames).setHeader("Roles").setKey("roleNames").setSortable(true);

	    grid.addColumn(User::getInsurance).setHeader(this.insurance.getLabel()).setKey("insurance").setSortProperty("insurance");
	    grid.addComponentColumn(entitySelected -> createYesNoIcon(entitySelected.getCanLogin())).setHeader(this.canLogin.getLabel()).setKey("canLogin").setSortProperty("canLogin");
	    grid.addComponentColumn(entitySelected -> createYesNoIcon(entitySelected.getApi())).setHeader(this.api.getLabel()).setKey("api").setSortProperty("api");
	    
	    GridContextMenu<User> menu = grid.addContextMenu();
    	menu.addItem(new Span(new Icon(VaadinIcon.PASSWORD), new Text("reset password")), event -> event.getItem().ifPresent(item -> {	    	    	
    		  new ResetPasswordDialog(item, userService).open();
    	    }));

		grid.getColumns().forEach(column -> {
			column.setResizable(true);   // Enable resizing for all columns
	          // Enable sorting for all columns
	        column.setTextAlign(ColumnTextAlign.CENTER);
	        column.setSortable(true); 
	        if(!column.getKey().equalsIgnoreCase("id") ) {
	        	column.setAutoWidth(true);
	        }
		});

	    createShowHideColumnGridToolBar();   	        
	    grid.setAllRowsVisible(true);
	        
	 }
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
	

	 @Override
	 protected void configureEditorLayout() {
	        rulesLayout = new VerticalLayout(
	                new Paragraph("Password Complexity Rules"),
	                ruleLength8,
	                ruleLength64,
	                ruleUpper,
	                ruleLower,
	                ruleDigit,
	                ruleSpecial,
	                ruleGroup4,
	               
	                ruleStrong,
	                ruleMatch
	               
	        );
	        
	        confirmPassword.setValueChangeMode(ValueChangeMode.EAGER);
	        hashedPassword.setValueChangeMode(ValueChangeMode.EAGER);
	        
	        hashedPassword.addValueChangeListener(e -> binder.validate());
	        confirmPassword.addValueChangeListener(e -> binder.validate());

	        

		 HorizontalLayout  formLayout = new HorizontalLayout ();
		 formLayout.setMargin(true);
	     formLayout.setSizeFull();	        
	     formLayout.add(new FormLayout(name,username,email,hashedPassword,confirmPassword,roles,insurance,canLogin,api),rulesLayout);

	     
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
	        
	     editorLayout.setDialogTitle("User");       	        
	     editorLayout.add(formLayout); 
	     editorLayout.getFooter().add(buttonLayout); 
	     
	     roles.setItemLabelGenerator(Role::getName);
	     roles.setAutoExpand(AutoExpandMode.BOTH);
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
	 protected void populateForm(User entity) {
	     binder.readBean(entity); // Populate the form using the binder
	     
	     boolean isNew = entity.getId() == null;
	     
	     hashedPassword.setVisible(isNew);
	     confirmPassword.setVisible(isNew);	     
	     rulesLayout.setVisible(isNew);	     
	     editorLayout.open();
	 }


	 private void clearForm() {
		 binder.readBean(null);
		 this.entity = null;
	 }
	 private void closeForm() {
		 editorLayout.close();
	 }

	 @Override
	 protected Specification<User> buildCombinedSpecification() {
		 return (root, query, criteriaBuilder) -> {
			 try {
				 query.distinct(true);
				 Join<User, Role> rolesJoin = root.join("roles", JoinType.LEFT);	
				 
				 List<Predicate> predicates = new ArrayList<>();
		         List<String> sqlFilter = new ArrayList<>();		            		       	           
		         // Quick Search Filter
		         String quickSearchValue = txtQuick.getValue();
		         if (quickSearchValue != null && !quickSearchValue.isEmpty()) {
		        	 String likePattern = "%" + quickSearchValue.toLowerCase().trim() + "%";	          	               
	
		             predicates.add(criteriaBuilder.or(		                	
		            		 criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(root.get("id"), criteriaBuilder.literal(""))), likePattern),		                   
		            		 criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
		            		 criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern),
		            		 criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern),
		            		 criteriaBuilder.like(criteriaBuilder.lower(root.get("insurance")), likePattern),
		            		 criteriaBuilder.like(criteriaBuilder.lower(rolesJoin.get("name")), likePattern)
		            ));
		         }
	
		         // Advanced Filters
		         //ID Filter
		         if (advanceFilterID.getValue() != null) {
		                predicates.add(criteriaBuilder.equal(root.get("id"), advanceFilterID.getValue()));
		                sqlFilter.add("ID = " + advanceFilterID.getValue());
		         }
		         
		         //Display name Filter
		         if (advanceFilterName.getValue() != null && !advanceFilterName.getValue().isEmpty()) {
		            	String likePattern = "%" + advanceFilterName.getValue().toLowerCase() + "%";
		                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern));
		                sqlFilter.add("Display name LIKE '" +likePattern + "'");
		         }
		         
		         //User name Filter
		         if (advanceFilterUsername.getValue() != null && !advanceFilterUsername.getValue().isEmpty()) {
		            	String likePattern = "%" + advanceFilterUsername.getValue().toLowerCase() + "%";
		                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern));
		                sqlFilter.add("UserName LIKE '" +likePattern + "'");
		         }
		         
		         //Email Filter
		         if (advanceFilterEmail.getValue() != null && !advanceFilterEmail.getValue().isEmpty()) {
		            	String likePattern = "%" + advanceFilterEmail.getValue().toLowerCase() + "%";
		                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern));
		                sqlFilter.add("Email LIKE '" +likePattern + "'");
		         }
		         
		         //Email Filter
		         if (advanceFilterInsurance.getValue() != null && !advanceFilterInsurance.getValue().isEmpty()) {
		            	String likePattern = "%" + advanceFilterInsurance.getValue().toLowerCase() + "%";
		                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("insurance")), likePattern));
		                sqlFilter.add("Insurance LIKE '" +likePattern + "'");
		         }
		         
		         
		         // Role Filter
		        if (advanceFilterRoles.getValue() != null && !advanceFilterRoles.getValue().isEmpty()) {
			            Set<String> itemsFilter = advanceFilterRoles.getValue().stream()
			                .map(Role::getName)
			                .collect(Collectors.toSet());
			            predicates.add(rolesJoin.get("name").in(itemsFilter));
			            sqlFilter.add("Role IN (" + String.join(", ", itemsFilter) + ")");
			    }
		        
		         //Can Login Filter
		         if (advanceFilterCanLogin.getValue() != null && !advanceFilterCanLogin.getValue().isEmpty()) {
		            	Boolean flag = advanceFilterCanLogin.getValue().equalsIgnoreCase("Yes") ? true:false;
		            	predicates.add(criteriaBuilder.equal(root.get("canLogin"), flag));
		                sqlFilter.add("Can Login = '" +advanceFilterCanLogin.getValue() + "'");
		         }
		         //API Filter
		         if (advanceFilterApi.getValue() != null && !advanceFilterApi.getValue().isEmpty()) {
		            	Boolean flag = advanceFilterApi.getValue().equalsIgnoreCase("Yes") ? true:false;
		            	predicates.add(criteriaBuilder.equal(root.get("api"), flag));
		                sqlFilter.add("API = '" +advanceFilterApi.getValue() + "'");
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
	    	advanceFilterSettingTextField(advanceFilterName);
	    	advanceFilterSettingTextField(advanceFilterUsername);
	    	advanceFilterSettingEmailField(advanceFilterEmail);
	    	advanceFilterSettingMultiSelectComboBox(advanceFilterRoles);
	    	advanceFilterRoles.setItemLabelGenerator(Role::getName);
	    	advanceFilterSettingComboBox(advanceFilterCanLogin);
	    	advanceFilterSettingComboBox(advanceFilterApi);
	        advanceFilterSettingTextField(advanceFilterInsurance);	 
	        
	    	 this.advanceSearchLayout.add(advanceFilterID,advanceFilterName,advanceFilterUsername,advanceFilterEmail,advanceFilterRoles,advanceFilterCanLogin,advanceFilterApi,advanceFilterInsurance);
	    	//return formLayout;
	    }

		
	    private void reloadAdvanceFilterData() {
	    	this.advanceFilterRoles.setItems(roleRepository.findAll());
	    }
	    private void reloadEditorData() {
	    	 this.roles.setItems(roleRepository.findAll());
	    }

	    @Override
	    protected void focusFirstField() {
	        this.name.focus(); // Focus the "name" field
	    }	    
	    
	    @Override
	    protected User createNewEntity() {
	        return new User(); // Initialize a new ServiceType entity
	    }
	
	    @Override
	    public void beforeEnter(BeforeEnterEvent event) {
	    	if(!authenticatedUser.hasPage(UserManagementView.class,AccessPageType.SELECTED_PAGE)) {
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
	            StreamResource resource = new StreamResource("User.xlsx", () -> {
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	                try (Workbook workbook = new XSSFWorkbook()) {
	                    Sheet sheet = workbook.createSheet("User");

	                    // Write headers dynamically from grid columns
	                    Row headerRow = sheet.createRow(0);
	                    List<Grid.Column<User>> columns = grid.getColumns();
	                    for (int i = 0; i < columns.size(); i++) {
	                        headerRow.createCell(i).setCellValue(columns.get(i).getHeaderText());
	                    }

	                    // Write data
	                    List<User> itemsToExport = grid.getSelectedItems().isEmpty() ?
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
	                                default:
	                                    try {
	                                        // Use reflection to check fields in Asset and its superclasses
	                                        Field field = getFieldFromClassHierarchy(User.class, columnKey);
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
