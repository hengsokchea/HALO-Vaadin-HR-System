package org.halocambodia.views.leave_management;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.BloodGroup;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeLeaveBalance;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.LeaveType;
import org.halocambodia.data.LeaveTypeRepository;
import org.halocambodia.data.Nationality;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeLeaveBalanceService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.employee.NationalStaffActiveView;

@Route(value = "leave-balances", layout = MainLayout.class)
@PageTitle("Employee Leave Balance Management")
@PermitAll
@Uses(Icon.class)
public class EmployeeLeaveBalanceView extends PageDialogLayout<EmployeeLeaveBalance, EmployeeLeaveBalanceService> {

    // Basic Information Fields
    private  ComboBox<Employee> employee = new ComboBox<>("Employee | បុគ្គលិក");
    private final ComboBox<LeaveType> leaveType = new ComboBox<>("Leave Type | ប្រភេទច្បាប់");
    private final DatePicker employmentStartDate = new DatePicker("Date of join(DOJ) | ថ្ងៃចូលធ្វើការ");
    private final IntegerField serviceYears = new IntegerField("Tenure (Years) | រយៈពេលបម្រើការ (ឆ្នាំ)");
    private final IntegerField serviceMonths = new IntegerField("Tenure (Months) | រយៈពេលបម្រើការ (ខែ)");
    private final IntegerField year = new IntegerField("For Year | សម្រាប់ឆ្នាំ");
    private final DatePicker effectiveDate = new DatePicker("Effective Date | ថ្ងៃមានប្រសិទ្ធភាព");
    private final DatePicker expiryDate = new DatePicker("Expiry Date | ថ្ងៃផុតកំណត់");
    private final TextArea note = new TextArea("Note | កំណត់សម្គាល់");

    // Leave Balance Fields
    private final BigDecimalField entitledDays = new BigDecimalField("Entitled Days | ថ្ងៃដែលទទួលបាន");
    private final BigDecimalField carriedOverDays = new BigDecimalField("Carried Over Days | ថ្ងៃកន្លងមក");
    private final BigDecimalField additionalDays = new BigDecimalField("Additional Days | ថ្ងៃបន្ថែម");
    private final BigDecimalField takenDays = new BigDecimalField("Taken Days | ចំនួនថ្ងៃដែលបានប្រើ");
    private final BigDecimalField pendingDays = new BigDecimalField("Pending Days | ចំនួនថ្ងៃកំពុងរង់ចាំ");
    private final BigDecimalField totalDays = new BigDecimalField("Total Days | ចំនួនថ្ងៃសរុប");
    private final BigDecimalField remainingDaysCalc = new BigDecimalField("Current Balance | ចំនួនសមតុល្យបច្ចុប្បន្ន");
    private final BigDecimalField availableDays = new BigDecimalField("Available Balance | ចំនួនសមតុល្យអាចប្រើបាន");

    // Repositories
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final Optional<User> currentUserLogin;

    // For calculation tracking
    private boolean isAutoCalculating = true;

    public EmployeeLeaveBalanceView(EmployeeLeaveBalanceService service, 
                                   UserService userService,
                                   AuthenticatedUser authenticatedUser,
                                   EmployeeRepository employeeRepository,
                                   LeaveTypeRepository leaveTypeRepository) {
        super(EmployeeLeaveBalance.class, service, userService, authenticatedUser);
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.currentUserLogin = authenticatedUser.get();
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        this.enableToggleColumn = false;
        this.toggleColumnFrozen = false;
        loadComboBoxData();
        configureGrid();
        configureEditorLayout();
        binderField();
        setupFieldListeners();
        
        freezeMainColumns();

        //
    }
    private void freezeMainColumns() {
        Optional.ofNullable(grid.getColumnByKey("year")).ifPresent(c -> {
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
        Optional.ofNullable(grid.getColumnByKey("leaveType")).ifPresent(c -> {
            c.setFrozen(true);
            c.setFlexGrow(0);
        });
    }
    
    /**
     * Load data for combo boxes using repositories directly
     */
    private void loadComboBoxData() {

        employee = buildLazyComboBox(
                e -> formatEnKh(e.getNameEn(), e.getNameKh()+ "-"+ e.getInsuranceNo()),
                (filter, pageable) -> this.employeeRepository.searchActiveLocalStaff(filter, pageable),
                filter -> employeeRepository.countActiveLocalStaff(filter)
        );
        this.employee.setLabel("Employee | បុគ្គលិក");

        
        // Load all active leave types using repository
        leaveType.setItems(leaveTypeRepository.findByLeaveTypeGroupIdAndObsoleteDateIsNull(2L));
        leaveType.setItemLabelGenerator(type -> formatEnKh(type.getLeaveNameEn(), type.getLeaveNameKh()));
    }

    /**
     * Setup field listeners for auto-calculation
     */
    private void setupFieldListeners() {
    	year.addValueChangeListener(e -> {

    	    if (e.getValue() == null) return;

    	    int y = e.getValue();

    	    effectiveDate.setMin(LocalDate.of(y, 1, 1));
    	    effectiveDate.setMax(LocalDate.of(y, 12, 31));

    	    expiryDate.setMin(LocalDate.of(y, 1, 1));
    	    expiryDate.setMax(LocalDate.of(y, 12, 31));

    	    if (effectiveDate.getValue() == null) {
    	        effectiveDate.setValue(LocalDate.of(y, 1, 1));
    	    }

    	    if (expiryDate.getValue() == null) {
    	        expiryDate.setValue(LocalDate.of(y, 12, 31));
    	    }
    	});
    	


        // Auto-calculate totals when any balance field changes
        entitledDays.addValueChangeListener(e -> calculateAllBalances());
        carriedOverDays.addValueChangeListener(e -> calculateAllBalances());
        additionalDays.addValueChangeListener(e -> calculateAllBalances());
        takenDays.addValueChangeListener(e -> calculateAllBalances());
        pendingDays.addValueChangeListener(e -> calculateAllBalances());

        // Auto-calculate expiry date based on effective date
        effectiveDate.addValueChangeListener(e -> {
            if (isAutoCalculating && e.getValue() != null) {
                calculateServiceDuration(e.getValue());
            }
            
            updateEntitledDays();
        });
        

        expiryDate.addValueChangeListener(e -> updateEntitledDays());

        // When employee changes, populate employment start date
        employee.addValueChangeListener(e -> {
            if (isAutoCalculating && e.getValue() != null) {
                employmentStartDate.setValue(e.getValue().getJoinDate());
                calculateServiceDuration(effectiveDate.getValue());
            }
            updateEntitledDays();
        });
        
     // Listener on serviceYears
        serviceYears.addValueChangeListener(e -> updateAdditionalDays());

        // Listener on leaveType
        leaveType.addValueChangeListener(e ->{ updateAdditionalDays();updateEntitledDays();});
        
        

    }
    private void updateAdditionalDays() {
        Integer years = serviceYears.getValue();
        if (years != null && years > 0 
            && leaveType.getValue() != null 
            && leaveType.getValue().getId() == 1L) {
            BigDecimal additional = BigDecimal.valueOf(years) .divide(BigDecimal.valueOf(3), 0, RoundingMode.DOWN);
            additionalDays.setValue(additional);
        } else {
            additionalDays.setValue(BigDecimal.ZERO);
        }
    }

    private void updateEntitledDays() {
        LocalDate start = effectiveDate.getValue();
        LocalDate end = expiryDate.getValue();

        if (start != null && end != null && !end.isBefore(start) && this.leaveType.getValue() != null) {

            BigDecimal maxEntitledDays = this.leaveType.getValue().getEntitledDays();

            // ❌ If no configuration → stop calculation
            if (maxEntitledDays == null) {
                entitledDays.setValue(BigDecimal.ZERO);
                return;
            }

            // ✅ Monthly rate from leave type
            BigDecimal monthlyRate = maxEntitledDays
                    .divide(BigDecimal.valueOf(12), 4, RoundingMode.DOWN);

            // Step 1: Full months
            long fullMonths = ChronoUnit.MONTHS.between(start, end);

            // Step 2: Remaining days
            LocalDate tempDate = start.plusMonths(fullMonths);
            long remainingDays = ChronoUnit.DAYS.between(tempDate, end) + 1;

            // Step 3: Calculate leave
            BigDecimal fullMonthLeave = BigDecimal.valueOf(fullMonths)
                    .multiply(monthlyRate);

            BigDecimal partialLeave = BigDecimal.valueOf(remainingDays)
                    .multiply(monthlyRate)
                    .divide(BigDecimal.valueOf(30), 4, RoundingMode.DOWN);

            BigDecimal total = fullMonthLeave.add(partialLeave);

            // ✅ Cap to maxEntitledDays
            if (total.compareTo(maxEntitledDays) > 0) {
                total = maxEntitledDays;
            }

            // Step 4: Round DOWN to nearest 0.5
            entitledDays.setValue(roundToHalfOrWhole(total));

        } else {
            entitledDays.setValue(BigDecimal.ZERO);
        }
    }
    
    private BigDecimal roundToHalfOrWhole(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;

        // Multiply by 2 → drop decimals → divide back by 2
        return value
                .multiply(BigDecimal.valueOf(2))
                .setScale(0, RoundingMode.DOWN)
                .divide(BigDecimal.valueOf(2));
    }
    
    /**
     * Calculate service years and months based on effective date
     */
    private void calculateServiceDuration(LocalDate effective) {
        if (employee.getValue() == null || effective == null) {
            serviceYears.setValue(0);
            serviceMonths.setValue(0);
            return;
        }

        LocalDate joinDate = employee.getValue().getJoinDate();
        if (joinDate == null) {
            serviceYears.setValue(0);
            serviceMonths.setValue(0);
            return;
        }

        // Calculate duration from join date to effective date
        long totalMonths = ChronoUnit.MONTHS.between(joinDate, effective);
        int years = (int) (totalMonths / 12);
        int months = (int) (totalMonths % 12);

        serviceYears.setValue(years);
        serviceMonths.setValue(months);
        
        
    }

    /**
     * Calculate all balance fields (total, current balance, available balance)
     */
    private void calculateAllBalances() {
        // Get values safely
        BigDecimal entitled = getValueSafely(entitledDays.getValue());
        BigDecimal carried = getValueSafely(carriedOverDays.getValue());
        BigDecimal additional = getValueSafely(additionalDays.getValue());
        BigDecimal taken = getValueSafely(takenDays.getValue());
        BigDecimal pending = getValueSafely(pendingDays.getValue());
        
        // Calculate total days (entitled + carried + additional)
        BigDecimal total = entitled.add(carried).add(additional);
        totalDays.setValue(total);
        
        // Calculate current balance (total - taken)
        BigDecimal currentBalance = total.subtract(taken);
        // Ensure not negative
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            currentBalance = BigDecimal.ZERO;
        }
        remainingDaysCalc.setValue(currentBalance);
        
        // Calculate available balance (current balance - pending)
        BigDecimal available = currentBalance.subtract(pending);
        // Ensure not negative
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            available = BigDecimal.ZERO;
        }
        availableDays.setValue(available);
        
        // Style the available balance field based on value
        styleAvailableBalanceField(available);
    }

    /**
     * Style the available balance field based on value
     */
    private void styleAvailableBalanceField(BigDecimal available) {
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            availableDays.getElement().getThemeList().add("badge error");
        } else if (available.compareTo(new BigDecimal("5")) < 0) {
            availableDays.getElement().getThemeList().add("badge warning");
        } else {
            availableDays.getElement().getThemeList().add("badge success");
        }
    }

    /**
     * Safely get BigDecimal value, return ZERO if null
     */
    private BigDecimal getValueSafely(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }


    private void calculateExpiryDate(LocalDate effectiveDate) {

        if (effectiveDate != null) {

            int yearValue = effectiveDate.getYear();

            expiryDate.setValue(LocalDate.of(yearValue, 12, 31));

        }

    }

    /**
     * Manually recalculate all values
     */
    private void recalculateAll() {
        isAutoCalculating = false;
        try {
            calculateAllBalances();
            if (effectiveDate.getValue() != null) {
                calculateExpiryDate(effectiveDate.getValue());
            }
        } finally {
            isAutoCalculating = true;
        }
    }

    @Override
    protected void configureEditorLayout() throws Exception {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();

        // Tab 1: Basic Information
        FormLayout basicForm = new FormLayout();
        basicForm.setWidthFull();


        configureBasicFields();

        // Row 1: Employee and Leave Type
        basicForm.add(employee);
        
        // Row 2: Employment Start Date and Year
        basicForm.add(employmentStartDate, year,effectiveDate, expiryDate);
        
        // Row 3: Service Years and Service Months
        basicForm.add(serviceYears, serviceMonths);

        

        Tab basicTab = new Tab(VaadinIcon.CALENDAR.create(), 
            buildTabEnKh("Basic Information", "ព័ត៌មានមូលដ្ឋាន"));
        basicTab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);

        // Tab 2: Leave Balances
        FormLayout balanceForm = new FormLayout();
        balanceForm.setWidthFull();


        configureBalanceFields();



        // Row 1: Entitled and Carried Over
        balanceForm.add(leaveType,entitledDays, carriedOverDays);

        
        // Row 2: Additional and Taken
        balanceForm.add(additionalDays, takenDays);

        
        // Row 3: Pending Days (new field)
        balanceForm.add(pendingDays);
        

        
        // Row 4: Total and Current Balance
        
        totalDays.addThemeName("total-field");
        
        
        remainingDaysCalc.addThemeName("current-balance-field");
        
        balanceForm.add(totalDays, remainingDaysCalc);

        
        // Row 5: Available Balance (full width, highlighted)
        
        availableDays.addThemeName("available-balance-field");
        availableDays.setWidthFull();
        
        // Style the available balance field
        availableDays.getElement().getStyle().set("font-weight", "bold");
        availableDays.getElement().getStyle().set("font-size", "1.2em");
        
        balanceForm.add(availableDays);
        

        
        balanceForm.add(note);
        balanceForm.setColspan(note, 3);
        

        


        Tab balanceTab = new Tab(VaadinIcon.CHART.create(), 
            buildTabEnKh("Leave Balances", "សមតុល្យច្បាប់"));
        balanceTab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);

        // Add tabs to tab sheet
        Div basicWrapper = new Div(basicForm);
        basicWrapper.getStyle().set("padding", "1rem");
        
        Div balanceWrapper = new Div(balanceForm);
        balanceWrapper.getStyle().set("padding", "1rem");

        tabs.add(basicTab, basicWrapper);
        tabs.add(balanceTab, balanceWrapper);

        Div wrapper = new Div(tabs);
        wrapper.getStyle()
                .set("padding", "0 1rem")
                .set("box-sizing", "border-box");

        editorLayout.add(wrapper);
        configureEditorFooter();
    }

    private void configureBasicFields() {
        employee.setWidthFull();
        employee.setRequired(true);
        employee.setClearButtonVisible(true);
        employee.setPlaceholder("Select employee...");

        leaveType.setWidthFull();
        leaveType.setRequired(true);
        leaveType.setClearButtonVisible(true);
        leaveType.setPlaceholder("Select leave type...");

        employmentStartDate.setWidthFull();
        employmentStartDate.setRequired(true);
        employmentStartDate.setPlaceholder("DD/MM/YYYY");
        employmentStartDate.setReadOnly(true);

        serviceYears.setWidthFull();
        serviceYears.setRequired(true);
        serviceYears.setMin(0);
        serviceYears.setStepButtonsVisible(true);
        serviceYears.setValue(0);
        serviceYears.setReadOnly(true);

        serviceMonths.setWidthFull();
        serviceMonths.setRequired(true);
        serviceMonths.setMin(0);
        serviceMonths.setMax(11);
        serviceMonths.setStepButtonsVisible(true);
        serviceMonths.setValue(0);
        serviceMonths.setReadOnly(true);

        year.setWidthFull();
        year.setRequired(true);
        year.setMin(2000);
        year.setMax(2100);
        year.setStepButtonsVisible(true);

        effectiveDate.setWidthFull();
        effectiveDate.setRequired(true);
        effectiveDate.setPlaceholder("DD/MM/YYYY");

        expiryDate.setWidthFull();
        expiryDate.setRequired(true);
        expiryDate.setPlaceholder("DD/MM/YYYY");

        note.setWidthFull();
        note.setPlaceholder("Enter note...");
        
        totalDays.setReadOnly(true);
        remainingDaysCalc.setReadOnly(true);
        availableDays.setReadOnly(true);
        totalDays.setReadOnly(true);
        remainingDaysCalc.setReadOnly(true);
        availableDays.setReadOnly(true);
        
        serviceYears.setHelperText("Years worked since join date. Auto-calculated. | ចំនួនឆ្នាំធ្វើការចាប់ពីថ្ងៃចូលធ្វើការ (គណនាដោយស្វ័យប្រវត្តិ)");
        
        serviceMonths.setHelperText("Remaining months after service years. Auto-calculated. | ចំនួនខែដែលនៅសល់បន្ទាប់ពីឆ្នាំសេវាកម្ម (គណនាដោយស្វ័យប្រវត្តិ)");
        entitledDays.setHelperText("Annual leave granted by company policy. | ចំនួនថ្ងៃច្បាប់ប្រចាំឆ្នាំដែលក្រុមហ៊ុនផ្តល់"	);

        effectiveDate.setHelperText("Date when leave balance becomes active. | ថ្ងៃដែលសមតុល្យច្បាប់ចាប់ផ្តើមមានសុពលភាព");    
        expiryDate.setHelperText("Date when leave balance expires (normally end of leave cycle). | ថ្ងៃផុតកំណត់នៃសមតុល្យច្បាប់");

        additionalDays.setHelperText("Extra leave granted by management. | ថ្ងៃច្បាប់បន្ថែមដែលផ្តល់ដោយអ្នកគ្រប់គ្រង");
        takenDays.setHelperText("Leave days already used by employee. | ចំនួនថ្ងៃច្បាប់ដែលបុគ្គលិកបានប្រើ");

        pendingDays.setHelperText("Leave requests submitted but not yet approved. | ថ្ងៃច្បាប់ដែលបានស្នើសុំ ប៉ុន្តែមិនទាន់អនុម័ត");
        totalDays.setHelperText("Total leave = Entitled + Carried Over + Additional. | សរុបថ្ងៃច្បាប់ = ថ្ងៃដែលទទួលបាន + ថ្ងៃពីឆ្នាំមុន + ថ្ងៃបន្ថែម");
        remainingDaysCalc.setHelperText("Remaining leave after used days. | សមតុល្យច្បាប់ដែលនៅសល់បន្ទាប់ពីបានប្រើ");

        availableDays.setHelperText("Leave available for new requests (excluding pending). | ចំនួនថ្ងៃច្បាប់ដែលអាចប្រើបាន (មិនរាប់បញ្ចូលថ្ងៃកំពុងរង់ចាំ)");
        
        this.takenDays.setReadOnly(true);
        this.pendingDays.setReadOnly(true);
    }

    private void configureBalanceFields() {
        entitledDays.setWidthFull();
        entitledDays.setPlaceholder("0.00");
        entitledDays.setValue(BigDecimal.ZERO);


        carriedOverDays.setWidthFull();
        carriedOverDays.setPlaceholder("0.00");
        carriedOverDays.setValue(BigDecimal.ZERO);


        additionalDays.setWidthFull();
        additionalDays.setPlaceholder("0.00");
        additionalDays.setValue(BigDecimal.ZERO);


        takenDays.setWidthFull();
        takenDays.setPlaceholder("0.00");
        takenDays.setValue(BigDecimal.ZERO);


        pendingDays.setWidthFull();
        pendingDays.setPlaceholder("0.00");
        pendingDays.setValue(BigDecimal.ZERO);

        pendingDays.setLabel("Pending Days | ថ្ងៃកំពុងរង់ចាំ");

        totalDays.setWidthFull();
        totalDays.setPlaceholder("0.00");
        totalDays.setValue(BigDecimal.ZERO);

        remainingDaysCalc.setWidthFull();
        remainingDaysCalc.setPlaceholder("0.00");
        remainingDaysCalc.setValue(BigDecimal.ZERO);

        availableDays.setWidthFull();
        availableDays.setPlaceholder("0.00");
        availableDays.setValue(BigDecimal.ZERO);
        
        

    }

    @Override
    protected void populateForm(EmployeeLeaveBalance entity) throws Exception {
        this.entity = entity;

        // Temporarily disable auto-calculation while populating
        isAutoCalculating = false;
        
        binder.readBean(this.entity);
        
        // Re-enable auto-calculation
        isAutoCalculating = true;
        
        // Recalculate to ensure all fields are consistent
        calculateAllBalances();

        editorLayout.open();
    }

    @Override
    protected void beforeSave(EmployeeLeaveBalance entity, boolean isNew) throws Exception {
        if (entity.getEmployee() == null) {
            throw new IllegalArgumentException("Employee is required.");
        }
        if (entity.getLeaveType() == null) {
            throw new IllegalArgumentException("Leave type is required.");
        }
        if (entity.getEffectiveDate().isAfter(entity.getExpiryDate())) {
            throw new IllegalArgumentException("Effective date cannot be after expiry date.");
        }
        
        validateLeaveBalance(entity, isNew);

        if (isNew) {
            entity.setId(null); // ensure Hibernate inserts
        }
    }

    /**
     * Calculate total days from entity
     */
    private BigDecimal calculateTotalDaysFromEntity(EmployeeLeaveBalance entity) {
        BigDecimal entitled = entity.getEntitledDays() != null ? entity.getEntitledDays() : BigDecimal.ZERO;
        BigDecimal carried = entity.getCarriedOverDays() != null ? entity.getCarriedOverDays() : BigDecimal.ZERO;
        BigDecimal additional = entity.getAdditionalDays() != null ? entity.getAdditionalDays() : BigDecimal.ZERO;
        
        return entitled.add(carried).add(additional);
    }

    /**
     * Calculate current balance from entity
     */
    private BigDecimal calculateCurrentBalanceFromEntity(EmployeeLeaveBalance entity) {
        BigDecimal total = entity.getTotalDays() != null ? entity.getTotalDays() : BigDecimal.ZERO;
        BigDecimal taken = entity.getTakenDays() != null ? entity.getTakenDays() : BigDecimal.ZERO;
        
        BigDecimal current = total.subtract(taken);
        return current.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : current;
    }

    /**
     * Calculate available balance from entity
     */
    private BigDecimal calculateAvailableBalanceFromEntity(EmployeeLeaveBalance entity) {
        BigDecimal current = entity.getRemainingDaysCalc() != null ? entity.getRemainingDaysCalc() : BigDecimal.ZERO;
        BigDecimal pending = entity.getPendingDays() != null ? entity.getPendingDays() : BigDecimal.ZERO;
        
        BigDecimal available = current.subtract(pending);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    /**
     * Validate leave balance business rules
     */
    private void validateLeaveBalance(EmployeeLeaveBalance entity, boolean isNew) throws Exception {
        if (entity.getEmployee() == null) {
            throw new IllegalArgumentException("Employee is required. | តម្រូវឱ្យជ្រើសរើសបុគ្គលិក។");
        }

        if (entity.getLeaveType() == null) {
            throw new IllegalArgumentException("Leave type is required. | តម្រូវឱ្យជ្រើសរើសប្រភេទច្បាប់។");
        }

        if (entity.getEffectiveDate().isAfter(entity.getExpiryDate())) {
            throw new IllegalArgumentException(
                "Effective date cannot be after expiry date. | " +
                "ថ្ងៃមានប្រសិទ្ធភាពមិនអាចក្រោយថ្ងៃផុតកំណត់បានទេ។"
            );
        }

        if (entity.getTakenDays() != null && entity.getTotalDays() != null) {
            if (entity.getTakenDays().compareTo(entity.getTotalDays()) > 0) {
                throw new IllegalArgumentException(
                    "Taken days cannot exceed total days. | " +
                    "ថ្ងៃដែលបានយកមិនអាចលើសថ្ងៃសរុបបានទេ។"
                );
            }
        }
/*
        if (entity.getPendingDays() != null && entity.getRemainingDaysCalc() != null) {
            if (entity.getPendingDays().compareTo(entity.getRemainingDaysCalc()) > 0) {
                throw new IllegalArgumentException(
                    "Pending days cannot exceed current balance. | " +
                    "ថ្ងៃកំពុងរង់ចាំមិនអាចលើសសមតុល្យបច្ចុប្បន្នបានទេ។"
                );
            }
        }
*/
        // Check for duplicate
        checkForDuplicate(entity, isNew);
    }

    /**
     * Check for duplicate leave balance
     */
    private void checkForDuplicate(EmployeeLeaveBalance entity, boolean isNew) {
        Specification<EmployeeLeaveBalance> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get("employee"), entity.getEmployee()),
            cb.equal(root.get("year"), entity.getYear()),
            cb.equal(root.get("leaveType"), entity.getLeaveType())
        );
        
        List<EmployeeLeaveBalance> existing = service.findAll(spec);

        if (!existing.isEmpty()) {
            boolean isDuplicate = isNew ? 
                true : 
                existing.stream().anyMatch(b -> !b.getId().equals(entity.getId()));

            if (isDuplicate) {
                throw new IllegalArgumentException(
                    "Leave balance already exists for this employee, year, and leave type. | " +
                    "សមតុល្យច្បាប់មានរួចហើយសម្រាប់បុគ្គលិក ឆ្នាំ និងប្រភេទច្បាប់នេះ។"
                );
            }
        }
    }

    @Override
    protected void binderField() {
        binder.bindInstanceFields(this);
    }

    @Override
    protected void focusFirstField() {
        this.employee.focus();
    }

    @Override
    protected EmployeeLeaveBalance createNewEntity() {
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance();

        int y = LocalDate.now().getYear();
        balance.setYear(y);

        balance.setEffectiveDate(LocalDate.of(y, 1, 1));
        balance.setExpiryDate(LocalDate.of(y, 12, 31));

        balance.setServiceYears(0);
        balance.setService_months(0);

        balance.setEntitledDays(BigDecimal.ZERO);
        balance.setCarriedOverDays(BigDecimal.ZERO);
        balance.setAdditionalDays(BigDecimal.ZERO);

        balance.setNote("");
        
        return balance;
    }

    @Override
    protected List<ColumnDef<EmployeeLeaveBalance>> getColumnDefs() {
        return List.of(
                col("year", this.year.getLabel(),  EmployeeLeaveBalance::getYear, e -> e.getYear() != null ? e.getYear().toString() : ""),
                
            col("employee.insuranceNo", "Insurance",
                        e -> e.getEmployee(),
                        e -> e.getEmployee().getInsuranceNo() !=null  ? e.getEmployee().getInsuranceNo().toString() : "" ),
                
            col("employee.nameEn", "Employee Name(EN)",
                e -> e.getEmployee(),
                e -> e.getEmployee().getNameEn() !=null  ? e.getEmployee().getNameEn() : "" ),
            
            col("employee.nameKh", "Employee Name(KH)",
                    e -> e.getEmployee(),
                    e -> e.getEmployee().getNameKh() !=null  ? e.getEmployee().getNameKh() : "" ),
            
            
            col("leaveType", this.leaveType.getLabel(),
                e -> e.getLeaveType(),
                e -> {
                    if (e.getLeaveType() == null) return "";
                    return formatEnKh(e.getLeaveType().getLeaveNameEn(), e.getLeaveType().getLeaveNameKh());
                }),
            
            col("employmentStartDate", this.employmentStartDate.getLabel(),
                EmployeeLeaveBalance::getEmploymentStartDate,
                e -> e.getEmploymentStartDate() != null ? 
                    DateTimeUtilFormart.DATE_FORMATTER.format(e.getEmploymentStartDate()) : ""),
            
            col("serviceYears", this.serviceYears.getLabel(),
                EmployeeLeaveBalance::getServiceYears,
                e -> e.getServiceYears() != null ? e.getServiceYears().toString() : "0"),
            
            col("serviceMonths", this.serviceMonths.getLabel(),
                EmployeeLeaveBalance::getService_months,
                e -> e.getService_months() != null ? e.getService_months().toString() : "0"),
            

            
            col("entitledDays", this.entitledDays.getLabel(),
                EmployeeLeaveBalance::getEntitledDays,
                e -> e.getEntitledDays() != null ? e.getEntitledDays().toString() : "0"),
            
            col("carriedOverDays",this.carriedOverDays.getLabel(),
                EmployeeLeaveBalance::getCarriedOverDays,
                e -> e.getCarriedOverDays() != null ? e.getCarriedOverDays().toString() : "0"),
            
            col("additionalDays", this.additionalDays.getLabel(),
                EmployeeLeaveBalance::getAdditionalDays,
                e -> e.getAdditionalDays() != null ? e.getAdditionalDays().toString() : "0"),
            
            col("totalDays", this.totalDays.getLabel(),
                    EmployeeLeaveBalance::getTotalDays,
                    e -> e.getTotalDays() != null ? e.getTotalDays().toString() : "0"),
            
            col("takenDays", this.takenDays.getLabel(),
                EmployeeLeaveBalance::getTakenDays,
                e -> e.getTakenDays() != null ? e.getTakenDays().toString() : "0"),
            
            col("pendingDays", this.pendingDays.getLabel(),
                EmployeeLeaveBalance::getPendingDays,
                e -> e.getPendingDays() != null ? e.getPendingDays().toString() : "0"),
            

            
            col("remainingDaysCalc",this.remainingDaysCalc.getLabel(),
                EmployeeLeaveBalance::getRemainingDaysCalc,
                e -> e.getRemainingDaysCalc() != null ? e.getRemainingDaysCalc().toString() : "0"),
            
            col("availableDays", this.availableDays.getLabel(),
                EmployeeLeaveBalance::getAvailableDays,
                e -> {
                    if (e.getAvailableDays() == null) return "0";
                    BigDecimal available = e.getAvailableDays();
                    String value = available.toString();
                    // Add visual indicator
                    if (available.compareTo(BigDecimal.ZERO) <= 0) {
                        return "🔴 " + value + " (No balance)";
                    } else if (available.compareTo(new BigDecimal("5")) < 0) {
                        return "🟡 " + value + " (Low balance)";
                    }
                    return "🟢 " + value;
                }),
            
            col("effectiveDate",this.effectiveDate.getLabel(),
                EmployeeLeaveBalance::getEffectiveDate,
                e -> e.getEffectiveDate() != null ? 
                    DateTimeUtilFormart.DATE_FORMATTER.format(e.getEffectiveDate()) : ""),
            
            col("expiryDate", this.expiryDate.getLabel(),
                EmployeeLeaveBalance::getExpiryDate,
                e -> e.getExpiryDate() != null ? 
                    DateTimeUtilFormart.DATE_FORMATTER.format(e.getExpiryDate()) : ""),
            
            col("note", this.note.getLabel(),
                EmployeeLeaveBalance::getNote,
                e -> e.getNote() != null ? e.getNote() : ""),
            
            // Audit columns
            col("userCreated.name", "Created By | បង្កើតដោយ", 
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),
            
            col("createdAt", "Created At | ថ្ងៃបង្កើត", 
                e -> e.getCreatedAt(),
                e -> e.getCreatedAt() != null ? 
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),
            
            col("userUpdated.name", "Updated By | កែប្រែដោយ", 
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),
            
            col("updatedAt", "Updated At | ថ្ងៃកែប្រែ", 
                e -> e.getUpdatedAt(),
                e -> e.getUpdatedAt() != null ? 
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }



    private  String staffLabel(Employee e) {
        if (e == null) return "";
        String ins = (e.getInsuranceNo() == null) ? "" : e.getInsuranceNo().toString();
        String name = formatEnKh(e.getNameEn(), e.getNameKh());
        if (ins.isBlank()) return name;
        if (name.isBlank()) return ins;
        return name  + " - " + ins;
    }
  
    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
                new AdvancedSearchPanel.FilterDef(
                	    "employee", // ✅ unique key (NOT "insuranceNo" again)
                	    "Staff Name | ឈ្មោះបុគ្គលិក",
                	    () -> buildLazyMultiSelect(
                	        e -> String.format("%s - %s",formatEnKh(e.getNameEn(), e.getNameKh()), e.getInsuranceNo() == null ? "" : e.getInsuranceNo() ),
                	        (filter, pageable) -> this.employeeRepository.searchEmployeesByNameEnKhInsuranceList(filter, pageable),
                	        filter -> employeeRepository.countEmployeesByNameEnKhInsurance(filter)
                	    ),
                	    c -> ((MultiSelectComboBox<?>) c).clear()
                	),
                
                new AdvancedSearchPanel.FilterDef(
                	    "leaveType",
                	    this.leaveType.getLabel(),
                	    () -> {
                	        return buildMultiSelect(
                	            s -> formatEnKh(s.getLeaveNameEn(), s.getLeaveNameKh()),
                	            leaveTypeRepository.findByLeaveTypeGroupId(2L)
                	        );
                	    },
                	    c -> ((MultiSelectComboBox<?>) c).clear()
                	),


            
            new FilterDef(
                "availableBalance",
                "Available Balance | សមតុល្យអាចប្រើបាន",
                () -> {
                    ComboBox<String> cb = new ComboBox<>();
                    cb.setItems("Greater than 0", "Equal to 0", "Less than 5", "Greater than or equal 5");
                    cb.setPlaceholder("Select condition...");
                    cb.setClearButtonVisible(true);
                    cb.setWidthFull();
                    return cb;
                },
                c -> ((ComboBox<?>) c).clear()
            )
        ));

        return advPanel;
    }
    
    @Override
    protected Specification<EmployeeLeaveBalance> buildCombinedSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> sqlFilter = new ArrayList<>();
            
            Join<EmployeeLeaveBalance, Employee> employeeJoin = root.join("employee", JoinType.INNER);
            Join<EmployeeLeaveBalance, LeaveType> leaveTypeJoin = root.join("leaveType", JoinType.INNER);

            // Default to current year if no year filter is applied
            Integer selectedYear = null;
            if (advPanel != null) {
                IntegerField advYear = advPanel.getField("year", IntegerField.class);
                if (advYear != null) {
                    selectedYear = advYear.getValue();
                }
            }

            int yearToFilter = (selectedYear != null) ? selectedYear : LocalDate.now().getYear();
            predicates.add(cb.equal(root.get("year"), yearToFilter));
            
            if (selectedYear != null) {
                sqlFilter.add("Year = " + selectedYear);
            } else {
                sqlFilter.add("Year = " + yearToFilter + " (Current)");
            }

            String quick = quickSearchField.getValue();
            if (quick != null && !quick.isEmpty()) {
                String like = "%" + quick.toLowerCase().trim() + "%";
                predicates.add(cb.or(                   
                    buildLikePredicate(cb, employeeJoin.get("insuranceNo"), like),
                    buildLikePredicate(cb, employeeJoin.get("nameEn"), like),
                    buildLikePredicate(cb, employeeJoin.get("nameKh"), like),
                    
                    buildLikePredicate(cb, leaveTypeJoin.get("leaveNameEn"), like),
                    buildLikePredicate(cb, leaveTypeJoin.get("leaveNameKh"), like),
                    buildLikePredicate(cb, root.get("note"), like)
                    
                    
                    
                ));
            }

            // Advanced filters
            if (advPanel != null) {

            	@SuppressWarnings("unchecked")
            	MultiSelectComboBox<Employee> advanceFilterStaffMs =(MultiSelectComboBox<Employee>) advPanel.getField("employee", MultiSelectComboBox.class);
            	advanceFilterBuildInPredicate(
            	        cb,
            	        employeeJoin,
            	        advanceFilterStaffMs == null ? null : advanceFilterStaffMs.getValue(),
            	        this::staffLabel, 
            	        "Staff | បុគ្គលិក",
            	        predicates,
            	        sqlFilter
            	);
            	
            	
            	@SuppressWarnings("unchecked")
            	MultiSelectComboBox<LeaveType> advanceFilterLeaveType =(MultiSelectComboBox<LeaveType>) advPanel.getField("leaveType", MultiSelectComboBox.class);
            	advanceFilterBuildInPredicate(
            	        cb,
            	        leaveTypeJoin,
            	        advanceFilterLeaveType == null ? null : advanceFilterLeaveType.getValue(),
            	        e-> formatEnKh(e.getLeaveNameEn(), e.getLeaveNameKh()),
            	        this.leaveType.getLabel(),
            	        predicates,
            	        sqlFilter
            	);
            	

                // Available balance condition filter
                ComboBox<String> advAvailableBalance = advPanel.getField("availableBalance", ComboBox.class);
                if (advAvailableBalance != null && advAvailableBalance.getValue() != null) {
                    switch (advAvailableBalance.getValue()) {
                        case "Greater than 0":
                            predicates.add(cb.greaterThan(root.get("availableDays"), BigDecimal.ZERO));
                            sqlFilter.add("Available Balance > 0");
                            break;
                        case "Equal to 0":
                            predicates.add(cb.equal(root.get("availableDays"), BigDecimal.ZERO));
                            sqlFilter.add("Available Balance = 0");
                            break;
                        case "Less than 5":
                            predicates.add(cb.and(
                                cb.greaterThanOrEqualTo(root.get("availableDays"), BigDecimal.ZERO),
                                cb.lessThan(root.get("availableDays"), new BigDecimal("5"))
                            ));
                            sqlFilter.add("Available Balance < 5");
                            break;
                        case "Greater than or equal 5":
                            predicates.add(cb.greaterThanOrEqualTo(root.get("availableDays"), new BigDecimal("5")));
                            sqlFilter.add("Available Balance >= 5");
                            break;
                    }
                }
            }

            showSqlFilterTokens(sqlFilter);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }

    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of();
    }

    @Override
    protected void onDelete() {
        Set<EmployeeLeaveBalance> selected = new LinkedHashSet<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            showErrorMessage("Please select record(s) to delete. | សូមជ្រើសរើសកំណត់ត្រា ដើម្បីលុប។");
            return;
        }

        try {
            validateBeforeDelete(selected);
        } catch (Exception ex) {
            showErrorMessage(ex.getMessage());
            return;
        }

        int count = selected.size();
        String labelEn = (count == 1) ? getEntityLabelSingular() : getEntityLabelPlural();
        String labelKm = "កំណត់ត្រា";

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm delete | បញ្ជាក់ការលុប");
        dialog.setText("Delete " + count + " " + labelEn + "? This action cannot be undone."
                + " | តើចង់លុប " + count + " " + labelKm + " មែនទេ? មិនអាចត្រឡប់យកមកវិញបានទេ។");
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel | បោះបង់");
        dialog.setCancelButtonTheme("tertiary");

        dialog.setConfirmText("Delete | លុប");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                service.delete(selected);
                grid.deselectAll();
                refreshGrid();
                showSuccessMessage("Deleted " + count + " " + labelEn + " successfully. | លុបបានជោគជ័យ។");
            } catch (Exception ex) {
                showErrorMessage((ex.getMessage() != null ? ex.getMessage() : "Delete failed.")
                        + " | ការលុបបរាជ័យ។");
                ex.printStackTrace();
            }
        });

        dialog.open();
    }

    private void validateBeforeDelete(Set<EmployeeLeaveBalance> entities) throws Exception {
        // Check if any balances have pending days
       /* for (EmployeeLeaveBalance balance : entities) {
            if (balance.getPendingDays() != null && balance.getPendingDays().compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException(
                    "Cannot delete balance with pending leave requests. | " +
                    "មិនអាចលុបសមតុល្យដែលមានពាក្យសុំច្បាប់កំពុងរង់ចាំបានទេ។"
                );
            }
        }
        */
    }

    private Component buildTabEnKh(String en, String kh) {
        Div label = new Div(
            new Span(en),
            new Span(kh)
        );
        label.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("line-height", "1.1")
            .set("font-size", "12px");
        return label;
    }

    @Override
    protected String getEntityLabelSingular() {
        return "Employee Leave Balance";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "Employee Leave Balances";
    }
}