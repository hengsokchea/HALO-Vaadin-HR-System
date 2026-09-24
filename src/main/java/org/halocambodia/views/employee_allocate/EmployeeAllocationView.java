package org.halocambodia.views.employee_allocate;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;


import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeAllocationService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;

import org.halocambodia.views.MasterPageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;

import org.springframework.dao.DataAccessException;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;


@PageTitle("Team Deployment Plan")
@Route(value = "employee-allocation", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class EmployeeAllocationView  extends MasterPageDialogLayout<EmployeeAllocation, EmployeeAllocationService> implements BeforeEnterObserver{

    private IntegerField yearNum =new IntegerField("Year");
    private ComboBox<Month> monthNum =new ComboBox<Month>("Month");
    private ComboBox<Employee> employee=new ComboBox<Employee>("Employee");
    private ComboBox<Positions> positions=new ComboBox<Positions>("Position");
    private ComboBox<Teams> teams=new ComboBox<Teams>("Team");
    private ComboBox<Branch> branch=new ComboBox<Branch>("Loction");
    private ComboBox<Category> category=new ComboBox<Category>("Category");
    private ComboBox<Contracts> contracts=new ComboBox<Contracts>("Contract");
    private ComboBox<PoGrade> poGrade=new ComboBox<PoGrade>("PO Grade");
    private ComboBox<ShiftCycle> shiftCycle=new ComboBox<ShiftCycle>("Shift");
    
    private ComboBox<Tasks> minefield=new ComboBox<Tasks>("Minefiled/BAC |ចម្ការមីន/ចម្ការគ្រាប់");
    
    private ComboBox<LeaveType> duty=new ComboBox<LeaveType>("Daily duties | កាតព្វកិច្ច ប្រចាំថ្ងៃ");

    //share data
    private final Optional<User> currentUserLogin;
    private final BeanValidationBinder<EmployeeAllocation> binder= new BeanValidationBinder<>(EmployeeAllocation.class);

    private final ShiftCycleRepository shiftCycleRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final TeamsRepository teamsRepository;
    private final BranchRepository branchRepository;
    private final ContractsRepository contractsRepository;
    private final CategoryRepository categoryRepository;
    private final PoGradeRepository poGradeRepository;
    private final ShiftRepository shiftRepository;
    private final BloodGroupRepository bloodGroupRepository;
    private final TasksRepository tasksRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    //Advance Filter Components 
 
    private MultiSelectComboBox<Month> advanceFilterMonthNum =new MultiSelectComboBox<Month>("Month");    
    private MultiSelectComboBox<Shift> advanceFilterShift=new MultiSelectComboBox<Shift>("Shift");

    private TextField advanceFilterNameEn=new TextField("Name EN");
    private TextField advanceFilterNameKh=new TextField("Name KH");
    private NumberField advanceFilterInsurance=new NumberField("Insurance");  
    private ComboBox<String> advanceFilterGender=new ComboBox<String>("Gender","Male","Female");
    
    private MultiSelectComboBox<Teams> advanceFilterTeam=new MultiSelectComboBox<Teams>("Team");
    private MultiSelectComboBox<Branch> advanceFilterLocation=new MultiSelectComboBox<Branch>("Location");
    private MultiSelectComboBox<Category> advanceFilterCategory=new MultiSelectComboBox<Category>("Category");
    
    private MultiSelectComboBox<BloodGroup> advanceFilterBloodGroup=new MultiSelectComboBox<BloodGroup>("Blood Group");
    private MultiSelectComboBox<PoGrade> advanceFilterPoGrade=new MultiSelectComboBox<PoGrade>("PO Grade");
    private MultiSelectComboBox<Contracts> advanceFilterContracts=new MultiSelectComboBox<Contracts>("Contract");
    
    
    
    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");

  
    
    public EmployeeAllocationView(EmployeeAllocationService service,UserService userService,AuthenticatedUser authenticatedUser,ShiftCycleRepository shiftCycleRepository,EmployeeRepository employeeRepository,PositionRepository positionRepository,TeamsRepository teamsRepository,BranchRepository branchRepository,ContractsRepository contractsRepository,CategoryRepository categoryRepository,PoGradeRepository poGradeRepository,FileUploadUtility fileUploadUtility, ShiftRepository shiftRepository,BloodGroupRepository bloodGroupRepository,TasksRepository tasksRepository,LeaveTypeRepository leaveTypeRepository) {
        super(service,userService,authenticatedUser);
        this.currentUserLogin=authenticatedUser.get();

        this.shiftCycleRepository=shiftCycleRepository;
        this.employeeRepository=employeeRepository;
        this.positionRepository=positionRepository;
        this.teamsRepository=teamsRepository;
        this.branchRepository=branchRepository;
        this.contractsRepository=contractsRepository;
        this.categoryRepository=categoryRepository;
        this.poGradeRepository=poGradeRepository;
        this.shiftRepository=shiftRepository;
        this.bloodGroupRepository=bloodGroupRepository;
        this.tasksRepository=tasksRepository;
        this.leaveTypeRepository=leaveTypeRepository;

        
        MenuBar menuBar = new MenuBar();
        //menuBar.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS,MenuBarVariant.LUMO_PRIMARY);
        MenuItem menuItem = menuBar.addItem("Data Tools");
        SubMenu menuItemSubmenu = menuItem.getSubMenu();

        // Import menu item with icon and text
        MenuItem importItem = menuItemSubmenu.addItem("Import", e -> UI.getCurrent().navigate(EmployeeAllocationImportView.class));
        importItem.addComponentAsFirst(new Icon(VaadinIcon.UPLOAD));      

        // Clone menu item with icon and text  
        MenuItem cloneItem = menuItemSubmenu.addItem("Clone", e -> UI.getCurrent().navigate(EmployeeAllocationCloneView.class));
        cloneItem.addComponentAsFirst(new Icon(VaadinIcon.COPY));
        
        
        this.leftToolbar.add(menuBar);

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
        } catch (Exception ex) {
            showError("Error initializing view: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadDataToForm() {
        this.monthNum.setItems(Month.values());
        this.employee.setItems(employeeRepository.findActiveLocalStaff());
        this.positions.setItems(this.positionRepository.findAll());
        this.teams.setItems(this.teamsRepository.findAll());
        this.branch.setItems(this.branchRepository.findByIsActiveTrue());
        this.contracts.setItems(this.contractsRepository.findAll());
        this.category.setItems(categoryRepository.findAll());
        this.poGrade.setItems(this.poGradeRepository.findAll());
        
        this.duty.setItems(this.leaveTypeRepository.findByLeaveTypeGroupIdAndIdNotIn(1L, List.of(32L, 18L)));
        this.minefield.setItems(this.tasksRepository.findByTaskTypeIdInOrderByTaskCodeAsc(List.of(1L, 2L)));
    }

    private void binderField() {
        binder.forField(yearNum)
            .asRequired("Year is required.")
            .bind(EmployeeAllocation::getYearNum, EmployeeAllocation::setYearNum);

        binder.forField(this.monthNum)
            .asRequired("Month is required")
            .withConverter(
                (Month month) -> month != null ? month.getValue() : null,
                (Integer monthNumber) -> monthNumber != null ? Month.of(monthNumber) : null
            )
            .bind(EmployeeAllocation::getMonthNum, EmployeeAllocation::setMonthNum);

        binder.bindInstanceFields(this);
    }

    @Override
    protected void configureGrid() {
        getColumnDefinitions().forEach(def -> {
            // Use textFormatter() for visible text, dataProvider() for sorting
            grid.addColumn(def.textFormatter()::apply)
                .setHeader(def.header())
                .setKey(def.key())
                .setSortable(true)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setResizable(true)
                .setAutoWidth(!"ID".equals(def.header()))
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

        createShowHideColumnGridToolBar();
        grid.setAllRowsVisible(true);
        
        List<String> columnsToFreeze = List.of( "monthNum","shiftCycle.shift.shiftName","employee.insuranceNo","employee.nameEn");
        
        for (String key : columnsToFreeze) {
            Grid.Column<EmployeeAllocation> column = grid.getColumnByKey(key);
            if (column != null) {
                column.setFrozen(true);
            }else {
            	System.out.println("Warning: Column with key '" + key + "' not found for freezing");	
            }
        }
      
    }




    @Override
    protected void configureEditorLayout() throws Exception{
        editorLayout.setDialogTitle("Team Deployment Plan");
        FormLayout form =new FormLayout(this.yearNum,this.monthNum,this.shiftCycle,this.employee,this.positions,this.teams,this.branch,this.category,this.contracts,this.poGrade,this.duty,this.minefield);
        form.addClassNames("m-m"); // medium margin
        editorLayout.add(form);
        
        

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

        editorLayout.getFooter().add(buttonLayout);

        int currentYear = java.time.Year.now().getValue();
        this.yearNum.setPlaceholder("Please choose a year");
        this.yearNum.setMin(currentYear);
        this.yearNum.setMax(currentYear + 1);
        this.yearNum.setStep(1);
        this.yearNum.setClearButtonVisible(true);
        this.yearNum.setStepButtonsVisible(true);
        this.yearNum.addValueChangeListener(event -> {
            Integer value = event.getValue();
            if (value != null) {
                shiftCycle.setItems(shiftCycleRepository.findByYear(value));
            } else {
                shiftCycle.setItems(Collections.emptyList());
            }
        });

        configureComboBox(this.monthNum, "Please choose a month", month -> month.getValue() + " - " + month.name());

        configureComboBox(this.shiftCycle, "Please choose a shift",sc -> sc.getYear() + " - " + sc.getShift().getShiftName());
        configureComboBox(this.employee, "Please choose an employee",emp -> String.format("%s%s (%s)", emp.getNameEn(), emp.getNameKh(), emp.getInsuranceNo()));
        this.employee.addValueChangeListener(event -> {
            Employee emp =event.getValue();
            if (emp != null) {
                this.positions.setValue(emp.getPositions());
                this.branch.setValue(emp.getBranch());
                this.teams.setValue(emp.getTeams());
                this.category.setValue(emp.getCategory());
                this.poGrade.setValue(emp.getPoGrade());
            } else {
                positions.clear();
            }
        });

        configureComboBox(this.positions, "Please choose a position", Positions::getPosition);
        configureComboBox(this.teams, "Please choose a team", Teams::getTeamCode);
        configureComboBox(this.branch, "Please choose a location", lo->lo.getBranchShortName() + "-" + lo.getBranchFullName());
        configureComboBox(this.contracts, "Please choose a contract", Contracts::getContractCode);
        configureComboBox(this.category, "Please choose a category", Category::getCategory);
        configureComboBox(this.poGrade, "Please choose a PO grade", PoGrade::getPoGrade);
        
        configureComboBox(this.duty, "Please choose a duty", LeaveType::getLeaveNameEn);
        
        configureComboBox(this.minefield, "Please choose a minefield", Tasks::getTaskCode);

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
            ex.printStackTrace();
            showError("Another user has modified this record." + ex.getMessage());
        } catch (ValidationException ex) {
            ex.printStackTrace();
            showError("Validation failed. Please check your input." + ex.getMessage());
        } catch(UnexpectedTypeException ex) {
            ex.printStackTrace();
            showError("Validation failed. Please check your input. " + ex.getMessage());
        }catch (DataAccessException ex) {
            ex.printStackTrace();
            showError("Database error: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Unexpected error: " + ex.getMessage());
        }
    }


    @Override
    protected void populateForm(EmployeeAllocation entity) throws Exception {
        loadDataToForm();
        binder.readBean(entity);
        editorLayout.open();

       // boolean isEdit=(entity.getId()!=null ?true:false);
    }

    private void clearForm() {
        binder.readBean(null);
        this.entity = null;
    }
    private void closeForm() {
        editorLayout.close();
    }

    @Override
    protected Specification<EmployeeAllocation> buildCombinedSpecification() {
        return (root, query, criteriaBuilder) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                Join<EmployeeAllocation, User> userCreatedJoin = root.join("userCreated", JoinType.LEFT);
                Join<EmployeeAllocation, User> userUpdatedJoin = root.join("userUpdated", JoinType.LEFT);
                
                Join<EmployeeAllocation, ShiftCycle> shiftCycleJoin = root.join("shiftCycle", JoinType.LEFT);
                Join<ShiftCycle, Shift> shiftJoin = shiftCycleJoin.join("shift", JoinType.LEFT);
                
                Join<EmployeeAllocation, Employee> employeeJoin = root.join("employee", JoinType.INNER);
                Join<EmployeeAllocation, Teams> teamJoin = root.join("teams", JoinType.LEFT);
                Join<EmployeeAllocation, Branch> branchJoin = root.join("branch", JoinType.LEFT);
                Join<EmployeeAllocation, Category> categoryJoin = root.join("category", JoinType.LEFT);
                Join<EmployeeAllocation, PoGrade> poGradeJoin = root.join("poGrade", JoinType.LEFT);
                Join<EmployeeAllocation, Contracts> contractsJoin = root.join("contracts", JoinType.LEFT);
                
                Join<EmployeeAllocation, Tasks> tasksJoin = root.join("minefield", JoinType.LEFT);
                Join<EmployeeAllocation, LeaveType> dutyJoin = root.join("duty", JoinType.LEFT);
                

                predicates.add(criteriaBuilder.equal(root.get("yearNum"),  LocalDate.now().getYear()));
                                
                String quickSearchValue = txtQuick.getValue();
                if (quickSearchValue != null && !quickSearchValue.isEmpty()) {
                    String likePattern = "%" + quickSearchValue.toLowerCase().trim() + "%";

                    predicates.add(criteriaBuilder.or(                                                                  
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.function("to_char", String.class, criteriaBuilder.function( "make_date", java.sql.Date.class, criteriaBuilder.literal(2000),root.get("monthNum"), criteriaBuilder.literal(1)),criteriaBuilder.literal("Month"))),likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(shiftJoin.get("shiftName")), likePattern),
                        
                        criteriaBuilder.like(criteriaBuilder.lower(employeeJoin.get("nameEn")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(employeeJoin.get("nameKh")), likePattern),                        
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(employeeJoin.get("insuranceNo"), criteriaBuilder.literal(""))), likePattern), 
                        criteriaBuilder.like(criteriaBuilder.lower(employeeJoin.get("gender")), likePattern),
                        
                        criteriaBuilder.like(criteriaBuilder.lower(teamJoin.get("teamCode")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(branchJoin.get("branchFullName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("category")), likePattern),
                        
                        criteriaBuilder.like(criteriaBuilder.lower(employeeJoin.get("bloodGroup")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(poGradeJoin.get("poGrade")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(contractsJoin.get("contractCode")), likePattern),
                        
                        
                        criteriaBuilder.like(criteriaBuilder.lower(userCreatedJoin.get("name")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower( DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(criteriaBuilder, root.get("createdAt"))), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(userUpdatedJoin.get("name")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(criteriaBuilder, root.get("updatedAt"))), likePattern)
                    ));
                }

                
                if (advanceFilterMonthNum.getValue() != null && !advanceFilterMonthNum.getValue().isEmpty()) {
                    Set<Month> multiMonthFilter = advanceFilterMonthNum.getValue();
                    // Predicate: filter by month number
                    predicates.add(root.get("monthNum").in(
                        multiMonthFilter.stream().map(Month::getValue).collect(Collectors.toSet())
                    ));
                    // SQL-like filter text: show month names (JANUARY, FEBRUARY, …)
                    String monthNames = multiMonthFilter.stream()
                        .map(Month::name)
                        .collect(Collectors.joining(", "));

                    sqlFilter.add("MonthNum IN (" + monthNames + ")");
                }

                if (advanceFilterShift.getValue() != null && !advanceFilterShift.getValue().isEmpty()) {
                    Set<String> shiftFilter = advanceFilterShift.getValue().stream()
                        .map(Shift::getShiftName)
                        .collect(Collectors.toSet());
                    predicates.add(shiftJoin.get("shiftName").in(shiftFilter));
                    
                    sqlFilter.add("Shift IN (" + String.join(", ", shiftFilter) + ")");
                }
                
             // 🔹 Name EN
                if (advanceFilterNameEn.getValue() != null && !advanceFilterNameEn.getValue().isEmpty()) {
                    String nameEnValue = advanceFilterNameEn.getValue().toLowerCase().trim();
                    predicates.add(criteriaBuilder.like( criteriaBuilder.lower(employeeJoin.get("nameEn")),"%" + nameEnValue + "%"));

                    sqlFilter.add("NameEn LIKE '%" + advanceFilterNameEn.getValue().trim() + "%'");
                }

                // 🔹 Name KH
                if (advanceFilterNameKh.getValue() != null && !advanceFilterNameKh.getValue().isEmpty()) {
                    String nameKhValue = advanceFilterNameKh.getValue().toLowerCase().trim();

                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(employeeJoin.get("nameKh")), "%" + nameKhValue + "%"));

                    sqlFilter.add("NameKh LIKE '%" + advanceFilterNameKh.getValue().trim() + "%'");
                }

                // 🔹 Insurance Number
                if (advanceFilterInsurance.getValue() != null) {
                    predicates.add(criteriaBuilder.equal(employeeJoin.get("insuranceNo"), advanceFilterInsurance.getValue()));
                    sqlFilter.add("InsuranceNo = " + advanceFilterInsurance.getValue());
                }

                // 🔹 Gender
                if (advanceFilterGender.getValue() != null && !advanceFilterGender.getValue().isEmpty()) {
                    String genderValue = advanceFilterGender.getValue().toLowerCase();

                    predicates.add(criteriaBuilder.like(criteriaBuilder.lower(employeeJoin.get("gender")),  genderValue  ));

                    sqlFilter.add("Gender = '" + advanceFilterGender.getValue() + "'");
                }


                if (advanceFilterTeam.getValue() != null && !advanceFilterTeam.getValue().isEmpty()) {
                    Set<String> multiFilter = advanceFilterTeam.getValue().stream()
                        .map(Teams::getTeamCode)
                        .collect(Collectors.toSet());
                    predicates.add(teamJoin.get("teamCode").in(multiFilter));
                    sqlFilter.add("Team IN (" + String.join(", ", multiFilter) + ")");
                }
                
                if (advanceFilterLocation.getValue() != null && !advanceFilterLocation.getValue().isEmpty()) {
                    Set<String> multiFilter = advanceFilterLocation.getValue().stream()
                        .map(Branch::getBranchFullName)
                        .collect(Collectors.toSet());
                    predicates.add(branchJoin.get("branchFullName").in(multiFilter));
                    sqlFilter.add("Location IN (" + String.join(", ", multiFilter) + ")");
                }
                
                if (advanceFilterCategory.getValue() != null && !advanceFilterCategory.getValue().isEmpty()) {
                    Set<String> multiFilter = advanceFilterCategory.getValue().stream()
                        .map(Category::getCategory)
                        .collect(Collectors.toSet());
                    predicates.add(categoryJoin.get("category").in(multiFilter));
                    sqlFilter.add("Category IN (" + String.join(", ", multiFilter) + ")");
                }
                
                if (advanceFilterBloodGroup.getValue() != null && !advanceFilterBloodGroup.getValue().isEmpty()) {
                    Set<String> multiFilter = advanceFilterBloodGroup.getValue().stream()
                        .map(BloodGroup::getBloodGroup)
                        .collect(Collectors.toSet());
                    predicates.add(employeeJoin.get("bloodGroup").in(multiFilter));
                    sqlFilter.add("BloodGroup IN (" + String.join(", ", multiFilter) + ")");
                }
                
                
                
                if (advanceFilterPoGrade.getValue() != null && !advanceFilterPoGrade.getValue().isEmpty()) {
                    Set<String> multiFilter = advanceFilterPoGrade.getValue().stream()
                        .map(PoGrade::getPoGrade)
                        .collect(Collectors.toSet());
                    predicates.add(poGradeJoin.get("poGrade").in(multiFilter));
                    sqlFilter.add("PO Grade IN (" + String.join(", ", multiFilter) + ")");
                }
                
                if (advanceFilterContracts.getValue() != null && !advanceFilterContracts.getValue().isEmpty()) {
                    Set<String> multiFilter = advanceFilterContracts.getValue().stream()
                        .map(Contracts::getContractCode)
                        .collect(Collectors.toSet());
                    predicates.add(contractsJoin.get("contractCode").in(multiFilter));
                    sqlFilter.add("Contract IN (" + String.join(", ", multiFilter) + ")");
                }

                
                    
                

                if (advanceFilterCreatedBy.getValue() != null && !advanceFilterCreatedBy.getValue().isEmpty()) {
                    Set<String> userNames = advanceFilterCreatedBy.getValue().stream()
                        .map(User::getName)
                        .collect(Collectors.toSet());
                    predicates.add(userCreatedJoin.get("name").in(userNames));
                    sqlFilter.add("CreatedBy IN (" + String.join(", ", userNames) + ")");
                }

                if (advanceFilterCreatedDateFrom.getValue() != null && advanceFilterCreatedDateTo.getValue() != null) {
                    Expression<LocalDate> truncatedCreatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("createdAt"));
                    predicates.add(criteriaBuilder.between(truncatedCreatedAt, advanceFilterCreatedDateFrom.getValue(), advanceFilterCreatedDateTo.getValue()));
                    sqlFilter.add("CreatedAt BETWEEN " + advanceFilterCreatedDateFrom.getValue() + " AND " + advanceFilterCreatedDateTo.getValue());
                }

                if (advanceFilterUpdatedBy.getValue() != null && !advanceFilterUpdatedBy.getValue().isEmpty()) {
                    Set<String> userNamesUpdated = advanceFilterUpdatedBy.getValue().stream()
                        .map(User::getName)
                        .collect(Collectors.toSet());
                    predicates.add(userUpdatedJoin.get("name").in(userNamesUpdated));
                    sqlFilter.add("UpdatedBy IN (" + String.join(", ", userNamesUpdated) + ")");
                }

                if (advanceFilterUpdaedDateFrom.getValue() != null && advanceFilterUpdaedDateTo.getValue() != null) {
                    Expression<LocalDate> truncatedUpdatedAt = criteriaBuilder.function("DATE", LocalDate.class, root.get("updatedAt"));
                    predicates.add(criteriaBuilder.between(truncatedUpdatedAt, advanceFilterUpdaedDateFrom.getValue(), advanceFilterUpdaedDateTo.getValue()));
                    sqlFilter.add("UpdatedAt BETWEEN " + advanceFilterUpdaedDateFrom.getValue() + " AND " + advanceFilterUpdaedDateTo.getValue());
                }

                this.showHideAdvanceFilter(sqlFilter.isEmpty() ? null : String.join(" AND ", sqlFilter));

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            } catch (Exception ex) {
                showError("Error in filter: " + ex.getMessage());
                ex.printStackTrace();
                return criteriaBuilder.conjunction();
            }
        };
    }

    @Override
    protected void createAdvanceFilterLayout() {


        advanceFilterMonthNum.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterMonthNum.setWidthFull();
        advanceFilterMonthNum.setClearButtonVisible(true);
        advanceFilterMonthNum.setItems(Month.values());
        advanceFilterMonthNum.setAutoExpand(AutoExpandMode.BOTH);

        advanceFilterShift.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterShift.setWidthFull();
        advanceFilterShift.setClearButtonVisible(true);
        advanceFilterShift.setItems(shiftRepository.findAll());
        advanceFilterShift.setItemLabelGenerator(Shift::getShiftName);
        advanceFilterShift.setAutoExpand(AutoExpandMode.BOTH);
        
        advanceFilterNameEn.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        advanceFilterNameKh.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        advanceFilterInsurance.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        
        advanceFilterGender.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        advanceFilterGender.setWidthFull();
        advanceFilterGender.setClearButtonVisible(true);
        
        advanceFilterTeam.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterTeam.setWidthFull();
        advanceFilterTeam.setClearButtonVisible(true);
        advanceFilterTeam.setItems(teamsRepository.findAll());
        advanceFilterTeam.setItemLabelGenerator(Teams::getTeamCode);
        advanceFilterTeam.setAutoExpand(AutoExpandMode.BOTH);
        
        advanceFilterLocation.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterLocation.setWidthFull();
        advanceFilterLocation.setClearButtonVisible(true);
        advanceFilterLocation.setItems(branchRepository.findAll());
        advanceFilterLocation.setItemLabelGenerator(Branch::getBranchFullName);
        advanceFilterLocation.setAutoExpand(AutoExpandMode.BOTH);
        
        advanceFilterCategory.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterCategory.setWidthFull();
        advanceFilterCategory.setClearButtonVisible(true);
        advanceFilterCategory.setItems(categoryRepository.findAll());
        advanceFilterCategory.setItemLabelGenerator(Category::getCategory);
        advanceFilterCategory.setAutoExpand(AutoExpandMode.BOTH);
        
        advanceFilterBloodGroup.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterBloodGroup.setWidthFull();
        advanceFilterBloodGroup.setClearButtonVisible(true);
        advanceFilterBloodGroup.setItems(bloodGroupRepository.findAll());
        advanceFilterBloodGroup.setItemLabelGenerator(BloodGroup::getBloodGroup);
        advanceFilterBloodGroup.setAutoExpand(AutoExpandMode.BOTH);
        
        advanceFilterPoGrade.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterPoGrade.setWidthFull();
        advanceFilterPoGrade.setClearButtonVisible(true);
        advanceFilterPoGrade.setItems(poGradeRepository.findAll());
        advanceFilterPoGrade.setItemLabelGenerator(PoGrade::getPoGrade);
        advanceFilterPoGrade.setAutoExpand(AutoExpandMode.BOTH);
        
        advanceFilterContracts.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        advanceFilterContracts.setWidthFull();
        advanceFilterContracts.setClearButtonVisible(true);
        advanceFilterContracts.setItems(contractsRepository.findAll());
        advanceFilterContracts.setItemLabelGenerator(Contracts::getContractCode);
        advanceFilterContracts.setAutoExpand(AutoExpandMode.BOTH);
        
       

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

        // ✅ Correct way: use varargs (no index argument)
        this.advanceSearchLayout.add(
            advanceFilterMonthNum,
            advanceFilterNameEn,
            advanceFilterNameKh,
            advanceFilterInsurance,
            advanceFilterGender,
            advanceFilterShift,
            advanceFilterTeam,
            advanceFilterLocation,
            advanceFilterCategory,
           advanceFilterBloodGroup,
            advanceFilterPoGrade,
            advanceFilterContracts,
          
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
        // no-op
    }

    @Override
    protected EmployeeAllocation createNewEntity() throws Exception{
        EmployeeAllocation entity = new EmployeeAllocation();
        entity.setMonthNum(LocalDate.now().getMonthValue());
        entity.setYearNum(Year.now().getValue());
        return entity;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if(!authenticatedUser.hasPage(EmployeeAllocationView.class,AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        }
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

                List<EmployeeAllocation> itemsToExport = grid.getSelectedItems().isEmpty()
                        ? grid.getGenericDataView().getItems().toList()
                        : new ArrayList<>(grid.getSelectedItems());

                if (itemsToExport.isEmpty()) {
                    Notification.show("No data to export", 1500, Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                    return;
                }

                StreamResource resource = new StreamResource("Team Deployment Plan.xlsx", () -> {
                    try (Workbook workbook = new XSSFWorkbook()) {
                        Sheet sheet = workbook.createSheet("Team Deployment Plan");

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
                        for (EmployeeAllocation entity : itemsToExport) {
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

 // 🔹 Column definition record
    
    private record ColumnDef(
            String key,
            String header,
            ValueProvider<EmployeeAllocation, ?> dataProvider,      // for grid sorting
            Function<EmployeeAllocation, String> textFormatter      // for export text
    ) {}
    
    private List<ColumnDef> getColumnDefinitions() {
        return List.of(
           // new ColumnDef("id", "ID",EmployeeAllocation::getId, e -> e.getId() != null ? e.getId().toString() : ""),

          //  new ColumnDef("yearNum", "Year", EmployeeAllocation::getYearNum,   e -> e.getYearNum() != null ? e.getYearNum().toString() : ""),

            // ✅ Month column: sorts numerically but shows month name
            new ColumnDef("monthNum", "Month",
                    EmployeeAllocation::getMonthNum,
                    e -> {
                        Integer m = e.getMonthNum();
                        return m != null
                                ? java.time.Month.of(m).getDisplayName(
                                    java.time.format.TextStyle.FULL,
                                    java.util.Locale.ENGLISH)
                                : "";
                    }),

            new ColumnDef("shiftCycle.shift.shiftName", "Shift",
                    e -> e.getShiftCycle() != null ? e.getShiftCycle().getShift().getShiftName() : "",
                    e -> e.getShiftCycle() != null ? e.getShiftCycle().getShift().getShiftName() : ""),
            
            new ColumnDef("employee.insuranceNo", "Insurance Nº",
                    e -> e.getEmployee() != null ? e.getEmployee().getInsuranceNo() : "",
                    e -> e.getEmployee() != null && e.getEmployee().getInsuranceNo() != null
                            ? e.getEmployee().getInsuranceNo().toString() : ""),

            new ColumnDef("employee.nameEn", "Name EN",
                    e -> e.getEmployee() != null ? e.getEmployee().getNameEn() : "",
                    e -> e.getEmployee() != null ? e.getEmployee().getNameEn() : ""),

            new ColumnDef("employee.nameKh", "Name KH",
                    e -> e.getEmployee() != null ? e.getEmployee().getNameKh() : "",
                    e -> e.getEmployee() != null ? e.getEmployee().getNameKh() : ""),



            new ColumnDef("employee.gender", "Gender",
                    e -> e.getEmployee() != null ? e.getEmployee().getGender() : "",
                    e -> e.getEmployee() != null ? e.getEmployee().getGender().getLabel() : ""),

            new ColumnDef("teams.teamCode", "Team",
                    e -> e.getTeams() != null ? e.getTeams().getTeamCode() : "",
                    e -> e.getTeams() != null ? e.getTeams().getTeamCode() : ""),

            new ColumnDef("branch.branchFullName", "Location",
                    e -> e.getBranch() != null ? e.getBranch().getBranchFullName() : "",
                    e -> e.getBranch() != null ? e.getBranch().getBranchFullName() : ""),

            new ColumnDef("category.category", "Category",
                    e -> e.getCategory() != null ? e.getCategory().getCategory() : "",
                    e -> e.getCategory() != null ? e.getCategory().getCategory() : ""),

            new ColumnDef("employee.bloodGroup", "Blood Group",
                    e -> e.getEmployee() != null ? e.getEmployee().getBloodGroup() : "",
                    e -> e.getEmployee() != null ? e.getEmployee().getBloodGroup() : ""),

            new ColumnDef("poGrade.poGrade", "PO Grade",
                    e -> e.getPoGrade() != null ? e.getPoGrade().getPoGrade() : "",
                    e -> e.getPoGrade() != null ? e.getPoGrade().getPoGrade() : ""),

            new ColumnDef("Contracts.contractCode", "Contract Code",
                    e -> e.getContracts() != null ? e.getContracts().getContractCode() : "",
                    e -> e.getContracts() != null ? e.getContracts().getContractCode() : ""),
            
            new ColumnDef("minefield.taskCode", this.minefield.getLabel(),
                    e -> e.getMinefield() != null ? e.getMinefield().getTaskCode() : "",
                    e -> e.getMinefield() != null ? e.getMinefield().getTaskCode() : ""),
            
            new ColumnDef("duty.leaveNameEn", this.duty.getLabel(),
                    e -> e.getDuty() != null ? e.getDuty().getLeaveNameEn() : "",
                    e -> e.getDuty() != null ? e.getDuty().getLeaveNameEn() : ""),
            

            new ColumnDef("userCreated.name", "Created By",
                    e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                    e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

            new ColumnDef("createdAt", "Created At",
                    EmployeeAllocation::getCreatedAt,
                    e -> e.getCreatedAt() != null
                            ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt())
                            : ""),

            new ColumnDef("userUpdated.name", "Updated By",
                    e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                    e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

            new ColumnDef("updatedAt", "Updated At",
                    EmployeeAllocation::getUpdatedAt,
                    e -> e.getUpdatedAt() != null
                            ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt())
                            : "")
        );
    }



}