package org.halocambodia.views.employee_allocate;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import jakarta.annotation.security.PermitAll;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.halocambodia.data.*;
import org.halocambodia.services.*;
import org.halocambodia.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.ConstraintViolation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Collections;
import java.util.UUID;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;


@PageTitle("Import Employee Allocation")
@Route(value = "employee-allocation-import", layout = MainLayout.class)
@PermitAll
public class EmployeeAllocationImportView extends VerticalLayout implements BeforeEnterObserver {

    private final EmployeeAllocationImportService service;
    private final AuthenticatedUser authenticatedUser;
    private final ShiftCycleRepository shiftCycleRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final TeamsRepository teamsRepository;
    private final BranchRepository branchRepository;
    private final ContractsRepository contractsRepository;
    private final CategoryRepository categoryRepository;
    private final PoGradeRepository poGradeRepository;
    private final FileUploadUtility fileUploadUtility;
    
    private final LeaveTypeRepository leaveTypeRepository;     // NEW
    private final TasksRepository tasksRepository;

    private ComboBox<String> sheetSelector = new ComboBox<>("Select Sheet");
    private Upload upload = new Upload();
    private Button importButton = new Button("Import Selected Sheet");
    private ComboBox<Month> monthNumImport = new ComboBox<>("Month");
    private ComboBox<Integer> yearNumImport = new ComboBox<>("Year");
    
    private Grid<EmployeeAllocation> gridImport = new Grid<>(EmployeeAllocation.class, false);
    private List<EmployeeAllocation> allocations = new ArrayList<>();
    private Path lastUploadedFile = null;

    // Editor components
    private ComboBox<Contracts> contractEditor;
    private ComboBox<Positions> positionsEditor;
    private ComboBox<Teams> teamsEditor;
    private ComboBox<Branch> branchEditor;
    private ComboBox<ShiftCycle> shiftEditor;
    
    // NEW EDITOR FIELDS
    private ComboBox<LeaveType> dutyEditor;
    private ComboBox<Tasks> minefieldEditor;
    

    // set of editable column keys
    private final Set<String> EDITABLE_KEYS = Set.of(
        "contracts.contractCode",
        "teams.teamCode",
        "positions.position",
        "branch.branchFullName",
        "shiftCycle.shift.shiftName",
        "duty.leaveNameEn",
        "minefield.taskCode"
    );

    // Error tracking
    private Set<EmployeeAllocation> duplicateRecords = new HashSet<>();
    private Map<EmployeeAllocation, String> errorRecords = new HashMap<>();
    
 // Preloaded data
    private Map<String, LeaveType> leaveTypesByName = new HashMap<>();
    private Map<String, Tasks> tasksByCode = new HashMap<>();
    
    private ProgressBar progressBar;
    private Span progressText;

    @Autowired
    public EmployeeAllocationImportView(EmployeeAllocationImportService service,
                                      AuthenticatedUser authenticatedUser,
                                      ShiftCycleRepository shiftCycleRepository,
                                      EmployeeRepository employeeRepository,
                                      PositionRepository positionRepository,
                                      TeamsRepository teamsRepository,
                                      BranchRepository branchRepository,
                                      ContractsRepository contractsRepository,
                                      CategoryRepository categoryRepository,
                                      PoGradeRepository poGradeRepository,
                                      FileUploadUtility fileUploadUtility,LeaveTypeRepository leaveTypeRepository,  TasksRepository tasksRepository) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
        this.shiftCycleRepository = shiftCycleRepository;
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
        this.teamsRepository = teamsRepository;
        this.branchRepository = branchRepository;
        this.contractsRepository = contractsRepository;
        this.categoryRepository = categoryRepository;
        this.poGradeRepository = poGradeRepository;
        this.fileUploadUtility = fileUploadUtility;
        this.leaveTypeRepository = leaveTypeRepository; 
        this.tasksRepository = tasksRepository;

        initView();
    }

    private void initView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        // Add inline styles
        addInlineStyles();

        // Header with back button and upload controls on the same line
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);
        
        // Back button
        Button backButton = new Button("Back to Allocation", 
            new Icon(VaadinIcon.ARROW_LEFT), 
            e -> UI.getCurrent().navigate(EmployeeAllocationView.class));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        // Upload controls
        HorizontalLayout uploadControls = new HorizontalLayout();
        uploadControls.setSpacing(true);
        uploadControls.setPadding(false);
        uploadControls.setAlignItems(FlexComponent.Alignment.CENTER);
        uploadControls.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        uploadControls.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        uploadControls.getStyle().set("gap", "8px"); // consistent spacing

        // Make components visually align
        upload.getStyle().set("margin-top", "0");
        sheetSelector.getStyle().set("margin-top", "0");
        monthNumImport.getStyle().set("margin-top", "0");
        yearNumImport.getStyle().set("margin-top", "0");
        importButton.getStyle().set("margin-top", "0");
        //saveAllButton.getStyle().set("margin-top", "0");
        
        // File upload
        upload.setAcceptedFileTypes(".xls", ".xlsx");
        upload.setMaxFiles(1);
        upload.setDropAllowed(false);
        
        // Sheet selector
        sheetSelector.setPlaceholder("Select sheet");
        sheetSelector.setClearButtonVisible(true);
        
        // Month selector
        monthNumImport.setItems(Month.values());
        monthNumImport.setPlaceholder("Month");
        monthNumImport.setValue(java.time.LocalDate.now().getMonth());
        monthNumImport.addValueChangeListener(e -> {
            if (e.getValue() != null && !allocations.isEmpty()) {
                updateAllocationsMonthYear();
            }
        });
        
        // Year selector
        List<Integer> years = new ArrayList<>();
        int currentYear = java.time.Year.now().getValue();
        years.add(currentYear);
        years.add(currentYear + 1);
        yearNumImport.setItems(years);
        yearNumImport.setValue(currentYear);
        yearNumImport.addValueChangeListener(e -> {
            if (e.getValue() != null && !allocations.isEmpty()) {
                updateAllocationsMonthYear();
            }
        });
        
        // Import button
        importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        importButton.setIcon(new Icon(VaadinIcon.UPLOAD));
        importButton.addClickListener(e -> importSheet());
        
        Button saveAllButton = new Button("Save Valid Allocations", 
                new Icon(VaadinIcon.CHECK), 
                e -> saveAll());
        saveAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        
        // Add upload controls
        uploadControls.add(upload, sheetSelector, monthNumImport, yearNumImport, importButton, saveAllButton);
        
        // Add to header layout
        Div toolbar = new Div(backButton,uploadControls);
        toolbar.getStyle()
            .set("padding", "10px")
            .set("border-radius", "10px")
            .set("box-shadow", "0 2px 6px rgba(0,0,0,0.1)")
            .set("background-color", "var(--lumo-base-color)");

        headerLayout.add(backButton, toolbar);
        
        Tooltip.forComponent(importButton).withText("Import the selected sheet from Excel");
        Tooltip.forComponent(saveAllButton).withText("Save valid allocations to the database");
        Tooltip.forComponent(upload).withText("Upload Excel file (.xls or .xlsx)");

        
        // Grid
        gridImport.setSizeFull();
        gridImport.setEmptyStateText("No employees found.");
        gridImport.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS,GridVariant.LUMO_ROW_STRIPES);
        configureGridImportColumns();
        
        // Configure inline editing
        configureInlineEditableColumns();
        
        add(headerLayout, gridImport);
        setFlexGrow(1, gridImport);
        
        // Configure upload handler
        configureUploadHandler();
        
        // Configure menu on grid
        configureGridContextMenu();

    }

    private void configureUploadHandler() {
        Path uploadDir = fileUploadUtility.initializeUploadDirectory("hr", "");
        Path targetDir = uploadDir.resolve("allocation");

        UploadHandler diskHandler = UploadHandler.toTempFile((metadata, tempFile) -> {
            try {
                Files.createDirectories(targetDir);

                String uniqueName = UUID.randomUUID() + "_" + metadata.fileName();
                Path target = targetDir.resolve(uniqueName);

                Files.copy(tempFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

                String fileNameLower = metadata.fileName().toLowerCase(Locale.ENGLISH);
                boolean extOk = fileNameLower.endsWith(".xls") || fileNameLower.endsWith(".xlsx");

                String probeType = null;
                try {
                    probeType = Files.probeContentType(tempFile.toPath());
                } catch (IOException ignored) { }

                if (probeType == null) {
                    probeType = URLConnection.guessContentTypeFromName(metadata.fileName());
                }

                boolean mimeOk = probeType != null && (
                    probeType.contains("spreadsheet") ||
                        probeType.contains("excel") ||
                        "application/vnd.ms-excel".equalsIgnoreCase(probeType) ||
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(probeType)
                );

                if (!extOk && !mimeOk) {
                    showError("Invalid file type. Please upload an Excel (.xls or .xlsx) file.");
                    Files.deleteIfExists(target);
                    return;
                }

                List<String> sheetNames = new ArrayList<>();
                try (OPCPackage pkg = OPCPackage.open(target.toFile())) {
                    XSSFReader reader = new XSSFReader(pkg);
                    XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) reader.getSheetsData();
                    while (iter.hasNext()) {
                        iter.next();
                        sheetNames.add(iter.getSheetName());
                    }
                }

                UI.getCurrent().access(() -> {
                    sheetSelector.setItems(sheetNames);
                    if (!sheetNames.isEmpty()) {
                        sheetSelector.setValue(sheetNames.get(0));
                    }
                    lastUploadedFile = target;
                    showSuccess("File uploaded. Select a sheet to import.");
                });

            } catch (Exception ex) {
                throw new RuntimeException("Failed to process upload: " + ex.getMessage(), ex);
            } finally {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (IOException cleanupEx) {
                    cleanupEx.printStackTrace();
                }
            }
        });
        
        upload.setUploadHandler(diskHandler);

        upload.addFileRemovedListener(event -> {
            sheetSelector.clear();
            sheetSelector.setItems(Collections.emptyList());
            this.lastUploadedFile = null;
        });
    }

    private void configureGridImportColumns() {
        gridImport.removeAllColumns();

        gridImport.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getNameEn() : "";
        }).setHeader("Name EN").setKey("employee.nameEn").setFrozen(true);

        gridImport.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getNameKh() : "";
        }).setHeader("Name KH").setKey("employee.nameKh").setFrozen(true);

        gridImport.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getInsuranceNo() : "";
        }).setHeader("Insurance Nº").setKey("employee.insuranceNo").setFrozen(true);

        gridImport.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getGender() : "";
        }).setHeader("Gender").setKey("employee.gender");

        gridImport.addColumn(entityRow -> {
            return entityRow.getPositions() != null ? entityRow.getPositions().getPosition() : "";
        }).setHeader("Position").setKey("positions.position");

        gridImport.addColumn(entityRow -> {
            return entityRow.getBranch() != null ? entityRow.getBranch().getBranchFullName() : "";
        }).setHeader("Location").setKey("branch.branchFullName");

        gridImport.addColumn(entityRow -> {
            return entityRow.getTeams() != null ? entityRow.getTeams().getTeamCode() : "";
        }).setHeader("Team").setKey("teams.teamCode");

        gridImport.addColumn(entityRow -> {
            return entityRow.getCategory() != null ? entityRow.getCategory().getCategory() : "";
        }).setHeader("Category").setKey("category.category");

        gridImport.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getBloodGroup() : "";
        }).setHeader("Blood Group").setKey("employee.bloodGroup");

        gridImport.addColumn(entityRow -> {
            return entityRow.getPoGrade() != null ? entityRow.getPoGrade().getPoGrade() : "";
        }).setHeader("PO Grade").setKey("poGrade.poGrade");

        gridImport.addColumn(entityRow -> {
            return entityRow.getContracts() != null ? entityRow.getContracts().getContractCode() : "";
        }).setHeader("Contract Code").setKey("contracts.contractCode");

        gridImport.addColumn(entityRow -> {
            return entityRow.getShiftCycle() != null ? entityRow.getShiftCycle().getShift().getShiftName() : "";
        }).setHeader("Shift").setKey("shiftCycle.shift.shiftName");
        
        gridImport.addColumn(e -> e.getDuty() != null ? e.getDuty().getLeaveNameEn() : "")
        .setHeader("Duty").setKey("duty.leaveNameEn");

        gridImport.addColumn(e -> e.getMinefield() != null ? e.getMinefield().getTaskCode() : "")
        .setHeader("MineField/BAC").setKey("minefield.taskCode");
        

        // Status column with validation feedback
        gridImport.addColumn(buildStatusRenderer()).setHeader("Status").setKey("status");

        gridImport.getColumns().forEach(column -> {
            column.setResizable(true);
            column.setSortable(true);
            column.setTextAlign(ColumnTextAlign.CENTER);
            column.setAutoWidth(true);
        });

        gridImport.setPartNameGenerator(entity -> {

            if (errorRecords.containsKey(entity)) {
                return "save-error-row";
            }

            if (duplicateRecords.contains(entity)) {
                return "duplicate-row";
            }

            Validator validator =
                    Validation.buildDefaultValidatorFactory()
                              .getValidator();

            return validator.validate(entity).isEmpty()
                    ? null
                    : "validation-error-row";
        });

        updateGridFooters();
    }

    private ComponentRenderer<Component, EmployeeAllocation> buildStatusRenderer() {
        return new ComponentRenderer<>(entity -> {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<EmployeeAllocation>> violations = validator.validate(entity);

            // Check for save errors first (these take priority over validation errors)
            if (errorRecords.containsKey(entity)) {
                // Record that failed during save operation
                Icon errorIcon = VaadinIcon.EXCLAMATION.create();
                errorIcon.setColor("red");
                String errorMessage = errorRecords.get(entity);
                Tooltip.forComponent(errorIcon)
                    .withText("Save Failed:\n" + errorMessage)
                    .withPosition(Tooltip.TooltipPosition.TOP_START)
                    .withHideDelay(5000);
                return errorIcon;
            }
            else if (duplicateRecords.contains(entity)) {
                // Duplicate record
                Icon duplicateIcon = VaadinIcon.WARNING.create();
                duplicateIcon.setColor("orange");
                Tooltip.forComponent(duplicateIcon)
                    .withText("Duplicate - This record already exists in the database for the selected month/year")
                    .withPosition(Tooltip.TooltipPosition.TOP_START);
                return duplicateIcon;
            }
            else if (!violations.isEmpty()) {
                // Validation errors
                Icon errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
                errorIcon.setColor("red");
                String tooltipText = violations.stream()
                    .map(v -> {
                        String fieldName = v.getPropertyPath().toString();
                        String message = v.getMessage();
                        // Format field names for better readability
                        switch (fieldName) {
                            case "employee": return "Employee: " + message;
                            case "yearNum": return "Year: " + message;
                            case "monthNum": return "Month: " + message;
                            case "positions": return "Position: " + message;
                            case "teams": return "Team: " + message;
                            case "branch": return "Branch: " + message;
                            case "contracts": return "Contract: " + message;
                            case "shiftCycle": return "Shift: " + message;
                            case "category": return "Category: " + message;
                            case "poGrade": return "PO Grade: " + message;
                            default: return fieldName + ": " + message;
                        }
                    })
                    .collect(Collectors.joining("\n"));
                
                Tooltip.forComponent(errorIcon)
                    .withText("Validation Errors:\n" + tooltipText)
                    .withPosition(Tooltip.TooltipPosition.TOP_START)
                    .withHideDelay(5000);
                return errorIcon;
            }
            else {
                // Valid record - ready to save
                Icon okIcon = VaadinIcon.CHECK_CIRCLE.create();
                okIcon.setColor("green");
                Tooltip.forComponent(okIcon)
                    .withText("Valid - Ready to save")
                    .withPosition(Tooltip.TooltipPosition.TOP_START);
                return okIcon;
            }
        });
    }

    private void configureInlineEditableColumns() {
        BeanValidationBinder<EmployeeAllocation> importBinder = new BeanValidationBinder<>(EmployeeAllocation.class);
        Editor<EmployeeAllocation> editor = gridImport.getEditor();
        editor.setBinder(importBinder);
        editor.setBuffered(true);

        // Team editor
        teamsEditor = new ComboBox<>();
        teamsEditor.setItems(teamsRepository.findAll());
        teamsEditor.setItemLabelGenerator(Teams::getTeamCode);
        teamsEditor.setWidthFull();
        teamsEditor.setClearButtonVisible(true);
        importBinder.forField(teamsEditor)
            .bind(EmployeeAllocation::getTeams, EmployeeAllocation::setTeams);

        // Contract editor
        contractEditor = new ComboBox<>();
        contractEditor.setItems(contractsRepository.findAll());
        contractEditor.setItemLabelGenerator(Contracts::getContractCode);
        contractEditor.setWidthFull();
        contractEditor.setClearButtonVisible(true);
        importBinder.forField(contractEditor)
            .bind(EmployeeAllocation::getContracts, EmployeeAllocation::setContracts);

        // Position editor
        positionsEditor = new ComboBox<>();
        positionsEditor.setItems(positionRepository.findAll());
        positionsEditor.setItemLabelGenerator(Positions::getPosition);
        positionsEditor.setWidthFull();
        positionsEditor.setClearButtonVisible(true);
        importBinder.forField(positionsEditor)
            .bind(EmployeeAllocation::getPositions, EmployeeAllocation::setPositions);

        // Branch editor
        branchEditor = new ComboBox<>();
        branchEditor.setItems(branchRepository.findByIsActiveTrue());
        branchEditor.setItemLabelGenerator(Branch::getBranchFullName);
        branchEditor.setWidthFull();
        branchEditor.setClearButtonVisible(true);
        importBinder.forField(branchEditor)
            .bind(EmployeeAllocation::getBranch, EmployeeAllocation::setBranch);

        // Shift editor
        shiftEditor = new ComboBox<>();
        shiftEditor.setItems(shiftCycleRepository.findAll());
        shiftEditor.setItemLabelGenerator(sc -> sc.getShift().getShiftName());
        shiftEditor.setWidthFull();
        shiftEditor.setClearButtonVisible(true);
        importBinder.forField(shiftEditor)
            .bind(EmployeeAllocation::getShiftCycle, EmployeeAllocation::setShiftCycle);
        
        dutyEditor = new ComboBox<>();
        dutyEditor.setItems(leaveTypeRepository.findByLeaveTypeGroupIdAndIdNotIn(1L, List.of(32L, 18L)));
        dutyEditor.setItemLabelGenerator(LeaveType::getLeaveNameEn);
        dutyEditor.setWidthFull();
        dutyEditor.setClearButtonVisible(true);
        importBinder.forField(dutyEditor)
            .bind(EmployeeAllocation::getDuty, EmployeeAllocation::setDuty);

        // ==================== NEW: Minefield Editor ====================
        minefieldEditor = new ComboBox<>();
        minefieldEditor.setItems(tasksRepository.findByTaskTypeIdInOrderByTaskCodeAsc(List.of(1L, 2L)));
        minefieldEditor.setItemLabelGenerator(Tasks::getTaskCode);
        minefieldEditor.setWidthFull();
        minefieldEditor.setClearButtonVisible(true);
        importBinder.forField(minefieldEditor)
            .bind(EmployeeAllocation::getMinefield, EmployeeAllocation::setMinefield);

        // Setup cell editing
        setupCellEditingVisualFeedback();
    }

    private void setupCellEditingVisualFeedback() {
        gridImport.addItemDoubleClickListener(event -> {
            // Check if column is not null and has a key
            if (event.getColumn() == null || event.getColumn().getKey() == null) {
                return; // Exit if no valid column
            }
            
            EmployeeAllocation clickedItem = event.getItem();
            String columnKey = event.getColumn().getKey();

            if (clickedItem != null && EDITABLE_KEYS.contains(columnKey)) {
                setActiveEditor(columnKey, clickedItem);
            }
        });
    }

    private void setActiveEditor(String columnKey, EmployeeAllocation item) {
        // Cancel existing editor
        if (gridImport.getEditor().isOpen()) {
            gridImport.getEditor().cancel();
        }

        // Re-configure grid columns to restore normal rendering
        configureGridImportColumns();
        
        // Re-configure editors
        configureInlineEditableColumns();

        // Attach editor only for the active column
        Component editorComponent = null;
        switch (columnKey) {
            case "contracts.contractCode":
                editorComponent = createInlineEditorLayout(contractEditor, gridImport.getEditor());
                break;
            case "teams.teamCode":
                editorComponent = createInlineEditorLayout(teamsEditor, gridImport.getEditor());
                break;
            case "branch.branchFullName":
                editorComponent = createInlineEditorLayout(branchEditor, gridImport.getEditor());
                break;
            case "positions.position":
                editorComponent = createInlineEditorLayout(positionsEditor, gridImport.getEditor());
                break;
            case "shiftCycle.shift.shiftName":
                editorComponent = createInlineEditorLayout(shiftEditor, gridImport.getEditor());
                break;
                
            case "duty.leaveNameEn": editorComponent = createInlineEditorLayout(dutyEditor, gridImport.getEditor());  break;
            case "minefield.taskCode": editorComponent = createInlineEditorLayout(minefieldEditor, gridImport.getEditor()); break;
                
            default:
                return; // Exit if column key is not recognized
        }

        if (editorComponent != null) {
            Grid.Column<EmployeeAllocation> activeCol = gridImport.getColumnByKey(columnKey);
            if (activeCol != null) {
                activeCol.setEditorComponent(editorComponent);
                
                // Open editor for the clicked row
                gridImport.getEditor().editItem(item);
            }
        }
    }

    private HorizontalLayout createInlineEditorLayout(Component editorComponent, Editor<EmployeeAllocation> editor) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setWidthFull();

        // Make the editor stretch full width
        editorComponent.getElement().getStyle().set("width", "100%");
        layout.add(editorComponent);
        layout.setFlexGrow(1, editorComponent);

        // Buttons wrapper (compact, right aligned)
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(false);
        buttons.setPadding(false);

        Button saveBtn = new Button(new Icon(VaadinIcon.CHECK), e -> {
            try {
                EmployeeAllocation itemEdit = editor.getItem();
                if (itemEdit == null) return;

                BeanValidationBinder<EmployeeAllocation> binder =
                    (BeanValidationBinder<EmployeeAllocation>) editor.getBinder();
                if (binder != null) {
                    binder.writeBeanAsDraft(itemEdit);
                }

                editor.save();

                // Clear any previous errors for this item
                errorRecords.remove(itemEdit);
                duplicateRecords.remove(itemEdit);

                // Re-configure grid to restore normal rendering after save
                configureGridImportColumns();
                
                // re-validate and update status icon
                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                Validator validator = factory.getValidator();

                gridImport.getColumnByKey("status").setRenderer(buildStatusRenderer());

                gridImport.getDataProvider().refreshItem(itemEdit);
                updateGridFooters();
                showSuccess("Cell updated successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Error saving: " + ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SUCCESS);
        saveBtn.getElement().setAttribute("title", "Save");

        Button cancelBtn = new Button(new Icon(VaadinIcon.CLOSE), e -> {
            editor.cancel();
            // Re-configure grid to restore normal rendering after cancel
            configureGridImportColumns();
        });
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        cancelBtn.getElement().setAttribute("title", "Cancel");

        buttons.add(saveBtn, cancelBtn);
        layout.add(buttons);
        layout.setFlexGrow(0, buttons);
        return layout;
    }

    private void updateGridFooters() {
        int total = allocations != null ? allocations.size() : 0;

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        long validCount = allocations != null
            ? allocations.stream().filter(e -> 
                validator.validate(e).isEmpty() && 
                !errorRecords.containsKey(e) && 
                !duplicateRecords.contains(e))
            .count()
            : 0;

        long errorCount = errorRecords.size();
        long duplicateCount = duplicateRecords.size();
        
        // Calculate invalid count (records with validation errors but no save errors)
        long invalidCount = allocations != null
            ? allocations.stream().filter(e -> 
                !validator.validate(e).isEmpty() && 
                !errorRecords.containsKey(e) && 
                !duplicateRecords.contains(e))
            .count()
            : 0;

        if (gridImport.getColumnByKey("employee.nameEn") != null) {
            Span totalSpan = new Span("📊 Total Records: " + total);
            Tooltip.forComponent(totalSpan).withText("Click to show all records.");
            totalSpan.getStyle().set("cursor", "pointer").set("font-weight", "bold");
            totalSpan.addClickListener(e -> {
                gridImport.setItems(allocations);
                // FIX: Refresh status column when showing all
                refreshStatusColumnRenderer();
                gridImport.getDataProvider().refreshAll();
            });
            gridImport.getColumnByKey("employee.nameEn").setFooter(totalSpan);
        }
        
        if (gridImport.getColumnByKey("employee.nameKh") != null) {
            Span validSpan = new Span("✅ Valid: " + validCount);
            Tooltip.forComponent(validSpan).withText("Click to show only the valid rows.");         
            validSpan.getStyle().set("color", "green").set("cursor", "pointer");
            validSpan.addClickListener(e -> {
                ValidatorFactory f = Validation.buildDefaultValidatorFactory();
                Validator v = f.getValidator();
                gridImport.setItems(allocations.stream()
                    .filter(entity -> 
                        v.validate(entity).isEmpty() && 
                        !errorRecords.containsKey(entity) && 
                        !duplicateRecords.contains(entity))
                    .toList());
                
                // FIX: Refresh status column
                refreshStatusColumnRenderer();
                gridImport.getDataProvider().refreshAll();
            });
            gridImport.getColumnByKey("employee.nameKh").setFooter(validSpan);
        }

        if (gridImport.getColumnByKey("employee.insuranceNo") != null) {
        	VerticalLayout errorStats = new VerticalLayout();
            errorStats.setSpacing(true);
            errorStats.setPadding(false);

            if (invalidCount > 0) {
                Span invalidSpan = new Span("⚠️ Invalid: " + invalidCount);
                Tooltip.forComponent(invalidSpan).withText("Click to show only records with validation errors.");
                invalidSpan.getStyle().set("color", "#ff9800").set("cursor", "pointer"); // Orange color
                invalidSpan.addClickListener(e -> {
                    ValidatorFactory f = Validation.buildDefaultValidatorFactory();
                    Validator v = f.getValidator();
                    gridImport.setItems(allocations.stream()
                        .filter(entity -> 
                            !v.validate(entity).isEmpty() && 
                            !errorRecords.containsKey(entity) && 
                            !duplicateRecords.contains(entity))
                        .toList());
                    
                    // FIX: Refresh status column
                    refreshStatusColumnRenderer();
                    gridImport.getDataProvider().refreshAll();
                });
                errorStats.add(invalidSpan);
            }

            if (duplicateCount > 0) {
                Span duplicateSpan = new Span("🔄 Duplicates: " + duplicateCount);
                Tooltip.forComponent(duplicateSpan).withText("Click to show only duplicate records.");
                duplicateSpan.getStyle().set("color", "#ff5722").set("cursor", "pointer"); // Dark orange
                duplicateSpan.addClickListener(e -> {
                    gridImport.setItems(allocations.stream()
                        .filter(duplicateRecords::contains)
                        .toList());
                    
                    refreshStatusColumnRenderer();
                    gridImport.getDataProvider().refreshAll();
                });
                errorStats.add(duplicateSpan);
            }

            if (errorCount > 0) {
                Span errorSpan = new Span("❌ Errors: " + errorCount);
                Tooltip.forComponent(errorSpan).withText("Click to show only records with save errors.");
                errorSpan.getStyle().set("color", "red").set("cursor", "pointer");
                errorSpan.addClickListener(e -> {
                    gridImport.setItems(allocations.stream()
                        .filter(errorRecords::containsKey)
                        .toList());
                    
                    // FIX: Refresh status column
                    refreshStatusColumnRenderer();
                    gridImport.getDataProvider().refreshAll();
                });
                errorStats.add(errorSpan);
            }

            gridImport.getColumnByKey("employee.insuranceNo").setFooter(errorStats);
            
            refreshStatusColumnRenderer();
            gridImport.getDataProvider().refreshAll();
        }
    }

    private void importSheet() {
        String selectedSheet = sheetSelector.getValue();
        Integer selectedYear = this.yearNumImport.getValue();
        Month selectedMonth = this.monthNumImport.getValue();

        if (selectedSheet == null || lastUploadedFile == null) {
            showError("Please upload a file and select a sheet.");
            return;
        }
        if (selectedYear == null || selectedMonth == null) {
            showError("Please select a month and year.");
            return;
        }

        // Store UI reference in the UI thread
        UI ui = UI.getCurrent();
        
        // Show progress indicator
        Dialog progressDialog = new Dialog();
        progressDialog.setHeaderTitle("Importing...");
        progressDialog.setModal(true);
        progressDialog.setCloseOnEsc(false);
        progressDialog.setCloseOnOutsideClick(false);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressDialog.add(progressBar);
        progressDialog.open();

        // Run import in background thread
        new Thread(() -> {
            try {
                importSheetInBackground(selectedSheet, selectedYear, selectedMonth, ui, progressDialog);
            } catch (Exception e) {
                e.printStackTrace();
                // Ensure progress dialog is closed even on error
                if (ui != null && progressDialog != null) {
                    ui.access(() -> {
                        progressDialog.close();
                        showError("Error during import: " + e.getMessage());
                    });
                }
            }
        }).start();
    }

    private void importSheetInBackground(String selectedSheet, Integer selectedYear, Month selectedMonth, UI ui, Dialog progressDialog) {
        // Check if we have existing data and warn about overwriting
        if (allocations != null && !allocations.isEmpty()) {
            if (ui != null) {
                ui.access(() -> {
                    Dialog warningDialog = new Dialog();
                    warningDialog.setHeaderTitle("Overwrite Existing Data?");
                    
                    VerticalLayout content = new VerticalLayout();
                    content.add(new Span("You have " + allocations.size() + " unsaved records. " +
                                       "Importing new data will replace all current records."));
                    
                    Button proceedButton = new Button("Proceed", e -> {
                        warningDialog.close();
                        // Clear error tracking when overwriting
                        duplicateRecords.clear();
                        errorRecords.clear();
                        // Continue with import
                        continueImport(selectedSheet, selectedYear, selectedMonth, ui, progressDialog);
                    });
                    proceedButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    
                    Button cancelButton = new Button("Cancel", e -> {
                        warningDialog.close();
                        progressDialog.close();
                    });
                    
                    HorizontalLayout buttons = new HorizontalLayout(proceedButton, cancelButton);
                    warningDialog.add(content, buttons);
                    warningDialog.open();
                });
            }
            return;
        }
        
        // If no existing data, proceed normally
        continueImport(selectedSheet, selectedYear, selectedMonth, ui, progressDialog);
    }
    private void preloadReferenceData(Integer selectedYear) {
        leaveTypesByName = leaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(lt -> lt.getLeaveNameEn(). toLowerCase().trim(), lt -> lt));

        tasksByCode = tasksRepository.findAll().stream()
                .collect(Collectors.toMap(t -> t.getTaskCode().toLowerCase().trim(), t -> t));
    }
 // Update the continueImport method to accept UI and progressDialog parameters
    private void continueImport(String selectedSheet, Integer selectedYear, Month selectedMonth, UI ui, Dialog progressDialog) {
        if (ui == null) {
            System.err.println("UI is null, cannot continue import");
            return;
        }
        
        try (Workbook workbook = WorkbookFactory.create(lastUploadedFile.toFile())) {
            Sheet sheet = workbook.getSheet(selectedSheet);
            if (sheet == null) {
                ui.access(() -> {
                    progressDialog.close();
                    showError("Sheet not found: " + selectedSheet);
                });
                return;
            }
            preloadReferenceData(selectedYear);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                ui.access(() -> {
                    progressDialog.close();
                    showError("The selected sheet is empty or has no header row.");
                });
                return;
            }

            // Create header mapping once
            Map<String, Integer> headerMap = createHeaderMapping(headerRow);
            
            // Pre-load all reference data to avoid multiple database calls
            Map<Integer, Employee> employeesByInsurance = preloadEmployees();
            Map<String, Positions> positionsByName = preloadPositions();
            Map<String, Teams> teamsByCode = preloadTeams();
            Map<String, Branch> branchesByName = preloadBranches();
            Map<String, Category> categoriesByName = preloadCategories();
            Map<String, PoGrade> poGradesByName = preloadPoGrades();
            Map<String, Contracts> contractsByCode = preloadContracts();
            Map<String, ShiftCycle> shiftsByNameAndYear = preloadShifts(selectedYear);

            // Clear existing data
            List<EmployeeAllocation> newAllocations = new ArrayList<>();
            duplicateRecords.clear();
            errorRecords.clear();

            int totalRows = sheet.getLastRowNum();
            AtomicInteger processedRows = new AtomicInteger(0);
            AtomicInteger successfulRows = new AtomicInteger(0);

            // Process rows in batches
            for (int rowNum = 1; rowNum <= totalRows; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    processedRows.incrementAndGet();
                    continue;
                }

                try {
                    EmployeeAllocation allocation = processRow(
                        row, headerMap, selectedYear, selectedMonth,
                        employeesByInsurance, positionsByName, teamsByCode,
                        branchesByName, categoriesByName, poGradesByName,
                        contractsByCode, shiftsByNameAndYear
                    );

                    if (allocation != null) {
                        newAllocations.add(allocation);
                        successfulRows.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error processing row " + (rowNum + 1) + ": " + e.getMessage());
                } finally {
                    processedRows.incrementAndGet();
                    
                    // Update progress every 50 rows - only if UI is still valid
                    if (processedRows.get() % 50 == 0 && ui != null) {
                        int progress = processedRows.get();
                        ui.access(() -> {
                            if (progressDialog.isOpened()) {
                                updateProgress(progressDialog, progress, totalRows);
                            }
                        });
                    }
                }
            }

            // Final UI update
            ui.access(() -> {
                progressDialog.close();
                this.allocations = newAllocations;
                gridImport.setItems(allocations);
                updateGridFooters();
                
                if (allocations.isEmpty()) {
                    showError("No valid data found. Processed " + processedRows.get() + " rows, successful: " + successfulRows.get());
                } else {
                    showSuccess("Imported " + allocations.size() + " records from " + processedRows.get() + " rows");
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            if (ui != null) {
                ui.access(() -> {
                    progressDialog.close();
                    showError("Error importing sheet: " + ex.getMessage());
                });
            }
        }
    }
    
    private Map<Integer, Employee> preloadEmployees() {
        return employeeRepository.findAll().stream()
            .collect(Collectors.toMap(Employee::getInsuranceNo, e -> e));
    }

    private Map<String, Positions> preloadPositions() {
        return positionRepository.findAll().stream()
            .collect(Collectors.toMap(p -> p.getPosition().toLowerCase().trim(), p -> p));
    }

    private Map<String, Teams> preloadTeams() {
        return teamsRepository.findAll().stream()
            .collect(Collectors.toMap(t -> t.getTeamCode().toLowerCase().trim(), t -> t));
    }

    private Map<String, Branch> preloadBranches() {
        return branchRepository.findByIsActiveTrue().stream()
            .collect(Collectors.toMap(b -> b.getBranchFullName().toLowerCase().trim(), b -> b));
    }

    private Map<String, Category> preloadCategories() {
        return categoryRepository.findAll().stream()
            .collect(Collectors.toMap(c -> c.getCategory().toLowerCase().trim(), c -> c));
    }

    private Map<String, PoGrade> preloadPoGrades() {
        return poGradeRepository.findAll().stream()
            .collect(Collectors.toMap(p -> p.getPoGrade().toLowerCase().trim(), p -> p));
    }

    private Map<String, Contracts> preloadContracts() {
        return contractsRepository.findAll().stream()
            .collect(Collectors.toMap(c -> c.getContractCode().toLowerCase().trim(), c -> c));
    }

    private Map<String, ShiftCycle> preloadShifts(Integer year) {
        return shiftCycleRepository.findByYear(year).stream()
            .collect(Collectors.toMap(
                sc -> (sc.getShift().getShiftName() + "_" + year).toLowerCase().trim(), 
                sc -> sc
            ));
    }
    private EmployeeAllocation processRow(Row row, Map<String, Integer> headerMap, Integer selectedYear, Month selectedMonth,
            Map<Integer, Employee> employeesByInsurance, Map<String, Positions> positionsByName,
            Map<String, Teams> teamsByCode, Map<String, Branch> branchesByName,
            Map<String, Category> categoriesByName, Map<String, PoGrade> poGradesByName,
            Map<String, Contracts> contractsByCode, Map<String, ShiftCycle> shiftsByNameAndYear) {

		// Get insurance number
		Integer insuranceNo = getNumericCellValue(row, headerMap.get("Insurance"));
		if (insuranceNo == 0) {
			return null;
		}
		
		// Find employee
		Employee employee = employeesByInsurance.get(insuranceNo);
		if (employee == null) {
			return null;
		}
		
		// Create allocation
		EmployeeAllocation allocation = new EmployeeAllocation();
		allocation.setEmployee(employee);
		allocation.setYearNum(selectedYear);
		allocation.setMonthNum(selectedMonth.getValue());
		
		// Set position with cached data
		String positionName = getStringCellValue(row, headerMap.get("Position"));
		if (positionName != null && !positionName.isEmpty()) {
			Positions position = positionsByName.get(positionName.toLowerCase().trim());
			allocation.setPositions(position != null ? position : employee.getPositions());
		} else {
			allocation.setPositions(employee.getPositions());
		}
		
		// Set team with cached data
		String teamCode = getStringCellValue(row, headerMap.get("Team"));
		if (teamCode != null && !teamCode.isEmpty()) {
			Teams team = teamsByCode.get(teamCode.toLowerCase().trim());
			allocation.setTeams(team != null ? team : employee.getTeams());
		} else {
			allocation.setTeams(employee.getTeams());
		}
		
		// Set branch with cached data
		String branchName = getStringCellValue(row, headerMap.get("Location"));
		if (branchName != null && !branchName.isEmpty()) {
			Branch branch = branchesByName.get(branchName.toLowerCase().trim());
			allocation.setBranch(branch != null ? branch : employee.getBranch());
		} else {
			allocation.setBranch(employee.getBranch());
		}
		
		// Set category with cached data
		String categoryName = getStringCellValue(row, headerMap.get("Category"));
		if (categoryName != null && !categoryName.isEmpty()) {
			Category category = categoriesByName.get(categoryName.toLowerCase().trim());
			allocation.setCategory(category != null ? category : employee.getCategory());
		} else {
			allocation.setCategory(employee.getCategory());
		}
		
		// Set PO grade with cached data
		String poGradeName = getStringCellValue(row, headerMap.get("PO Grade"));
		if (poGradeName != null && !poGradeName.isEmpty()) {
			PoGrade poGrade = poGradesByName.get(poGradeName.toLowerCase().trim());
			allocation.setPoGrade(poGrade != null ? poGrade : employee.getPoGrade());
		} else {
			allocation.setPoGrade(employee.getPoGrade());
		}
		
		// Set contract with cached data
		String contractCode = getStringCellValue(row, headerMap.get("Contract"));
		if (contractCode != null && !contractCode.isEmpty()) {
			Contracts contract = contractsByCode.get(contractCode.toLowerCase().trim());
			if (contract != null) {
				allocation.setContracts(contract);
			}
		}
		
		// Set shift with cached data
		String shiftName = getStringCellValue(row, headerMap.get("Ins Type"));
		if (shiftName != null && !shiftName.isEmpty()) {
			String shiftKey = (shiftName + "_" + selectedYear).toLowerCase().trim();
			ShiftCycle shiftCycle = shiftsByNameAndYear.get(shiftKey);
			if (shiftCycle != null) {
				allocation.setShiftCycle(shiftCycle);
			}
		}
		
		// Duty (LeaveType)
		String dutyName = getStringCellValue(row, headerMap.get("Duty"));
        if (dutyName != null && !dutyName.isEmpty()) {
            allocation.setDuty(leaveTypesByName.get(dutyName.toLowerCase().trim()));
        }

        // Minefield / Task
        String minefieldCode = getStringCellValue(row, headerMap.get("MineField") != null ?
                headerMap.get("MineField") : headerMap.get("MineField/BAC"));

        if (minefieldCode != null && !minefieldCode.isEmpty()) {
            allocation.setMinefield(tasksByCode.get(minefieldCode.toLowerCase().trim()));
        }
        
		
		// Set user
		User currentUser = authenticatedUser.get().orElse(null);
		if (currentUser != null) {
			allocation.setUserCreated(currentUser);
			allocation.setUserUpdated(currentUser);
		}
		
		return allocation;
	}
    
 // Update the createProgressDialog method to store references
    private Dialog createProgressDialog() {
        Dialog progressDialog = new Dialog();
        progressDialog.setHeaderTitle("Importing...");
        progressDialog.setModal(true);
        progressDialog.setCloseOnEsc(false);
        progressDialog.setCloseOnOutsideClick(false);

        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        
        progressText = new Span("Loading data...");
        
        VerticalLayout content = new VerticalLayout(progressBar, progressText);
        content.setSpacing(true);
        content.setPadding(true);
        
        progressDialog.add(content);
        return progressDialog;
    }


 // Simplify the updateProgress method
    private void updateProgress(Dialog progressDialog, int processed, int total) {
        if (progressBar != null && progressText != null) {
            if (total > 0) {
                progressBar.setIndeterminate(false);
                progressBar.setValue((double) processed / total);
            }
            progressText.setText(String.format("Processed %d of %d rows...", processed, total));
        }
    }
    
    private Map<String, Integer> createHeaderMapping(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            String headerName = getStringCellValue(headerRow, cell.getColumnIndex()).trim();
            if (!headerName.isEmpty()) {
                headerMap.put(headerName, cell.getColumnIndex());
            }
        }
        return headerMap;
    }
    // Helper methods for reading Excel cells
    private int getNumericCellValue(Row row, Integer colIndex) {
        if (colIndex == null) return 0;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) Math.round(cell.getNumericCellValue());
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    return value.isEmpty() ? 0 : Integer.parseInt(value);
                case FORMULA:
                    switch (cell.getCachedFormulaResultType()) {
                        case NUMERIC:
                            return (int) Math.round(cell.getNumericCellValue());
                        case STRING:
                            String formulaValue = cell.getStringCellValue().trim();
                            return formulaValue.isEmpty() ? 0 : Integer.parseInt(formulaValue);
                        default:
                            return 0;
                    }
                default:
                    return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getStringCellValue(Row row, Integer colIndex) {
        if (colIndex == null) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        double numValue = cell.getNumericCellValue();
                        if (numValue == (long) numValue) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    switch (cell.getCachedFormulaResultType()) {
                        case STRING:
                            return cell.getStringCellValue().trim();
                        case NUMERIC:
                            return String.valueOf(cell.getNumericCellValue());
                        case BOOLEAN:
                            return String.valueOf(cell.getBooleanCellValue());
                        default:
                            return "";
                    }
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private void saveAll() {
        if (allocations == null || allocations.isEmpty()) {
            showError("No entities to save.");
            return;
        }

        try {
            List<EmployeeAllocation> validEntities = new ArrayList<>();
            Map<EmployeeAllocation, List<String>> validationErrors = new HashMap<>();

            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();

            for (EmployeeAllocation entityTmp : allocations) {
                Set<ConstraintViolation<EmployeeAllocation>> violations = validator.validate(entityTmp);

                if (violations.isEmpty()) {
                    validEntities.add(entityTmp);
                } else {
                    List<String> errorMessages = violations.stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .collect(Collectors.toList());
                    validationErrors.put(entityTmp, errorMessages);
                }
            }

            if (!validationErrors.isEmpty() && !validEntities.isEmpty()) {
                askUserConfirmationAsync(validEntities.size(), validationErrors.size(), proceed -> {
                    UI.getCurrent().access(() -> {
                        if (proceed) {
                            saveValidEntities(validEntities, validationErrors);
                            updateGridFooters();
                        } else {
                            showValidationErrorsInGrid(validationErrors);
                            updateGridFooters();
                            showError("Save cancelled. " + validationErrors.size() + " records have validation errors.");
                        }
                    });
                });
                return;
            }

            if (!validEntities.isEmpty()) {
                saveValidEntities(validEntities, validationErrors);
                updateGridFooters();
            }

            if (validEntities.isEmpty() && !validationErrors.isEmpty()) {
                showValidationErrorsInGrid(validationErrors);
                updateGridFooters();
                showError("No valid records to save. " + validationErrors.size() + " records have validation errors.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Unexpected error: " + ex.getMessage());
        }
    }

    private void askUserConfirmationAsync(int validCount, int errorCount, Consumer<Boolean> callback) {
        Dialog confirmationDialog = new Dialog();
        confirmationDialog.setHeaderTitle("Validation Results");
        confirmationDialog.setModal(true);
        confirmationDialog.setCloseOnEsc(true);
        confirmationDialog.setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        Div successDiv = new Div();
        successDiv.add(new Icon(VaadinIcon.CHECK_CIRCLE));
        successDiv.add(new Span(" " + validCount + " valid records ready to save"));
        successDiv.getStyle().set("color", "green");

        Div errorDiv = new Div();
        errorDiv.add(new Icon(VaadinIcon.EXCLAMATION_CIRCLE));
        errorDiv.add(new Span(" " + errorCount + " records have validation errors"));
        errorDiv.getStyle().set("color", "red");

        Div questionDiv = new Div();
        questionDiv.add(new Span("Do you want to save only the valid records?"));
        questionDiv.getStyle().set("font-weight", "bold").set("margin-top", "1em");

        content.add(successDiv, errorDiv, questionDiv);
        confirmationDialog.add(content);

        Button saveButton = new Button("Save Valid Records", e -> {
            confirmationDialog.close();
            callback.accept(true);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> {
            confirmationDialog.close();
            callback.accept(false);
        });
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
        buttons.setSpacing(true);
        confirmationDialog.getFooter().add(buttons);

        confirmationDialog.open();
    }

    private void saveValidEntities(List<EmployeeAllocation> validEntities, Map<EmployeeAllocation, List<String>> validationErrors) {
        try {
            List<EmployeeAllocation> successfullySaved = new ArrayList<>();
            List<EmployeeAllocation> skippedDuplicates = new ArrayList<>();
            Map<EmployeeAllocation, String> otherErrors = new HashMap<>();
            
            // Clear previous error tracking
            duplicateRecords.clear();
            errorRecords.clear();
            
            for (EmployeeAllocation entity : validEntities) {
                try {
                    // Try to save each entity individually
                    service.update(entity);
                    successfullySaved.add(entity);
                    
                } catch (DataIntegrityViolationException ex) {
                    // Handle duplicate key violations
                    if (ex.getMessage() != null && ex.getMessage().contains("duplicate key value violates unique constraint")) {
                        skippedDuplicates.add(entity);
                        duplicateRecords.add(entity);
                        System.out.println("Duplicate record skipped: " + 
                            entity.getEmployee().getInsuranceNo() + " for " + 
                            entity.getYearNum() + "-" + entity.getMonthNum());
                    } else {
                        // Other data integrity errors
                        String errorMsg = "Data integrity error: " + extractMeaningfulErrorMessage(ex);
                        otherErrors.put(entity, errorMsg);
                        errorRecords.put(entity, errorMsg);
                    }
                    
                } catch (Exception ex) {
                    // Handle all other types of exceptions
                    String errorMsg = extractMeaningfulErrorMessage(ex);
                    otherErrors.put(entity, errorMsg);
                    errorRecords.put(entity, errorMsg);
                    System.err.println("Error saving record for employee " + 
                        entity.getEmployee().getInsuranceNo() + ": " + errorMsg);
                }
            }
            
            // FIX: Force refresh the status column renderer
            refreshStatusColumnRenderer();
            
            // Build comprehensive result message
            StringBuilder message = new StringBuilder();
            if (!successfullySaved.isEmpty()) {
                message.append(successfullySaved.size()).append(" records saved successfully");
            }
            
            if (!skippedDuplicates.isEmpty()) {
                if (!successfullySaved.isEmpty()) message.append(", ");
                message.append(skippedDuplicates.size()).append(" duplicates skipped");
            }
            
            if (!otherErrors.isEmpty()) {
                if (!successfullySaved.isEmpty() || !skippedDuplicates.isEmpty()) message.append(", ");
                message.append(otherErrors.size()).append(" records failed with errors");
            }
            
            // Show appropriate notification
            if (!otherErrors.isEmpty() || !skippedDuplicates.isEmpty()) {
                Notification.show(message.toString(), 7000, Position.TOP_CENTER)
                    .addThemeVariants(otherErrors.isEmpty() ? 
                        NotificationVariant.LUMO_WARNING : 
                        NotificationVariant.LUMO_ERROR);
            } else if (!successfullySaved.isEmpty()) {
                Notification.show(message.toString(), 5000, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
            
            // Remove successfully saved entities from current allocations
            allocations.removeAll(successfullySaved);
            
            // Refresh grid to show updated status
            gridImport.getDataProvider().refreshAll();
            updateGridFooters();

            // Show detailed error dialog if there are other errors
            if (!otherErrors.isEmpty()) {
                showDetailedErrorDialog(otherErrors);
            }

            if (!validationErrors.isEmpty()) {
                showValidationErrorsInGrid(validationErrors);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Unexpected error during save operation: " + ex.getMessage());
        }
    }
    
    private void refreshStatusColumnRenderer() {
        // Remove and re-add the status column to ensure renderer is refreshed
        Grid.Column<EmployeeAllocation> statusColumn = gridImport.getColumnByKey("status");
        if (statusColumn != null) {
            // Store column properties
            boolean autoWidth = statusColumn.isAutoWidth();
            ColumnTextAlign textAlign = statusColumn.getTextAlign();
            String header = statusColumn.getHeaderText();
            
            // Remove and re-add the column
            gridImport.removeColumn(statusColumn);
            
            Grid.Column<EmployeeAllocation> newStatusColumn = gridImport.addColumn(buildStatusRenderer())
                .setHeader(header)
                .setKey("status")
                .setAutoWidth(autoWidth)
                .setTextAlign(textAlign);
            
            // Ensure the new column is properly configured
            newStatusColumn.setResizable(true);
            newStatusColumn.setSortable(true);
        }
    }

    private String extractMeaningfulErrorMessage(Exception ex) {
        if (ex instanceof DataIntegrityViolationException) {
            return "Database constraint violation. Please check the data.";
        } else if (ex instanceof org.springframework.dao.EmptyResultDataAccessException) {
            return "Referenced data not found. Please check related entities.";
        } else if (ex instanceof org.springframework.orm.jpa.JpaSystemException) {
            return "Database system error. Please try again.";
        } else if (ex.getCause() != null) {
            return ex.getCause().getMessage();
        } else {
            return ex.getMessage();
        }
    }

    private void updateAllStatusInGrid(List<EmployeeAllocation> duplicates, Map<EmployeeAllocation, String> otherErrors) {
        // Update both duplicates and other errors
        duplicateRecords.addAll(duplicates);
        errorRecords.putAll(otherErrors);
        
        // FIX: More comprehensive refresh of the status column
        refreshStatusColumnRenderer();
        
        // Also refresh individual items to ensure tooltips are updated
        duplicates.forEach(entity -> gridImport.getDataProvider().refreshItem(entity));
        otherErrors.keySet().forEach(entity -> gridImport.getDataProvider().refreshItem(entity));
    }

    private void showDetailedErrorDialog(Map<EmployeeAllocation, String> errors) {
        Dialog errorDialog = new Dialog();
        errorDialog.setHeaderTitle("Save Errors Details");
        errorDialog.setModal(true);
        errorDialog.setCloseOnEsc(true);
        errorDialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        Span summary = new Span("The following records failed to save:");
        summary.getStyle().set("font-weight", "bold");

        content.add(summary);

        // Add each error with employee details
        errors.forEach((allocation, error) -> {
            HorizontalLayout errorLayout = new HorizontalLayout();
            errorLayout.setSpacing(true);
            errorLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            Icon errorIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            errorIcon.setColor("red");
            errorIcon.setSize("16px");

            String employeeInfo = allocation.getEmployee() != null ? 
                allocation.getEmployee().getNameEn() + " (" + allocation.getEmployee().getInsuranceNo() + ")" : 
                "Unknown Employee";

            Span errorText = new Span(employeeInfo + ": " + error);
            errorText.getStyle().set("font-size", "small");

            errorLayout.add(errorIcon, errorText);
            content.add(errorLayout);
        });

        Button closeButton = new Button("Close", e -> errorDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        errorDialog.add(content);
        errorDialog.getFooter().add(closeButton);
        errorDialog.open();
    }

    private void showValidationErrorsInGrid(Map<EmployeeAllocation, List<String>> validationErrors) {
        gridImport.getColumns().stream()
            .filter(c -> "status".equals(c.getKey()))
            .findFirst()
            .ifPresent(col -> {
                col.setRenderer(new ComponentRenderer<>(entity -> {
                    List<String> errors = validationErrors.get(entity);
                    if (errors != null && !errors.isEmpty()) {
                        Icon errorIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
                        errorIcon.setColor("red");
                        Tooltip.forComponent(errorIcon)
                            .withText(String.join("\n", errors))
                            .withPosition(Tooltip.TooltipPosition.TOP_START)
                            .withHideDelay(5000);
                        return errorIcon;
                    } else {
                        Icon okIcon = VaadinIcon.CHECK_CIRCLE.create();
                        okIcon.setColor("green");
                        return okIcon;
                    }
                }));
            });

        gridImport.setPartNameGenerator(entity ->
            validationErrors.containsKey(entity) ? "validation-error-row" : null
        );

        gridImport.getDataProvider().refreshAll();
        updateGridFooters();
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if(!authenticatedUser.hasPage(EmployeeAllocationImportView.class,AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    private void showError(String message) {
        Notification.show(message, 10000, Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification.show(message, 5000, Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void updateAllocationsMonthYear() {
        Month selectedMonth = monthNumImport.getValue();
        Integer selectedYear = yearNumImport.getValue();
        
        if (selectedMonth == null || selectedYear == null) {
            return;
        }
        
        if (allocations != null && !allocations.isEmpty()) {
            // Show confirmation dialog
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Update Month/Year");
            
            VerticalLayout content = new VerticalLayout();
            content.add(new Span("Update month to " + selectedMonth + " and year to " + selectedYear + 
                               " for all " + allocations.size() + " records?"));
            
            Button confirmButton = new Button("Update", e -> {
                performMonthYearUpdate(selectedMonth, selectedYear);
                confirmDialog.close();
            });
            confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button cancelButton = new Button("Cancel", e -> {
                // Revert combo box values
                revertComboBoxValues();
                confirmDialog.close();
            });
            
            HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
            confirmDialog.add(content, buttons);
            confirmDialog.open();
        }
    }

    private void performMonthYearUpdate(Month month, Integer year) {
        for (EmployeeAllocation allocation : allocations) {
            allocation.setMonthNum(month.getValue());
            allocation.setYearNum(year);
            
            // Update shift cycle for new year
            if (allocation.getShiftCycle() != null) {
                String shiftName = allocation.getShiftCycle().getShift().getShiftName();
                Optional<ShiftCycle> shiftCycleOptional = shiftCycleRepository
                    .findByShift_ShiftNameIgnoreCaseAndYear(shiftName, year);
                shiftCycleOptional.ifPresent(allocation::setShiftCycle);
            }
            
            User currentUser = authenticatedUser.get().orElse(null);
            if (currentUser != null) {
                allocation.setUserUpdated(currentUser);
            }
        }
        
        gridImport.getDataProvider().refreshAll();
        updateGridFooters();
        showSuccess("Updated month/year for " + allocations.size() + " records");
    }

    private void revertComboBoxValues() {
        // Find current values from first allocation (if exists)
        if (!allocations.isEmpty()) {
            EmployeeAllocation firstAllocation = allocations.get(0);
            monthNumImport.setValue(Month.of(firstAllocation.getMonthNum()));
            yearNumImport.setValue(firstAllocation.getYearNum());
        }
    }
    
    private void addInlineStyles() {
        // Add CSS styles directly to the component
        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `" +
            "  .duplicate-row {" +
            "    background-color: #fff3e0 !important;" +
            "  }" +
            "  .duplicate-row:hover {" +
            "    background-color: #ffe0b2 !important;" +
            "  }" +
            "  .save-error-row {" +
            "    background-color: #ffebee !important;" +
            "  }" +
            "  .validation-error-row {" +
            "    background-color: #fff8e1 !important;" +
            "  }" +
            "`;" +
            "document.head.appendChild(style);"
        );
    }
    
    private void configureGridContextMenu() {
        GridContextMenu<EmployeeAllocation> menu = new GridContextMenu<>(gridImport);

        // Single delete (row-level)
        menu.addItem(iconItem(VaadinIcon.TRASH, "Delete this record", "red"), e -> {
            e.getItem().ifPresent(this::deleteSingleRecord);
        });

        // Delete currently shown (filtered) rows
        menu.addItem(iconItem(VaadinIcon.FILTER, "Delete all filtered records", "#ff9800"), e -> {
            deleteFilteredRecords();
        });

        // Clear everything in the import buffer
        menu.addItem(iconItem(VaadinIcon.ERASER, "Clear all imported data", "#c62828"), e -> {
            clearAllImportedData();
        });
    }

    /** Helper for an icon + label menu item with a colorized icon. */
    private Component iconItem(VaadinIcon icon, String label, String color) {
        Icon i = icon.create();
        i.setSize("16px");
        i.setColor(color);

        Span text = new Span(" " + label);
        HorizontalLayout row = new HorizontalLayout(i, text);
        row.setSpacing(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        return row;
    }
 // Delete one record (row)
    private void deleteSingleRecord(EmployeeAllocation item) {
        if (item == null) return;

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Confirm Delete");
        confirm.add(new Span("Are you sure you want to delete this record?"));

        Button yes = new Button("Delete", e -> {
            allocations.remove(item);
            duplicateRecords.remove(item);
            errorRecords.remove(item);
            gridImport.getDataProvider().refreshAll();
            updateGridFooters();
            showSuccess("Record deleted.");
            confirm.close();
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> confirm.close());
        confirm.getFooter().add(new HorizontalLayout(yes, cancel));
        confirm.open();
    }

 // Delete everything currently displayed (respects current filtered view)
    private void deleteFilteredRecords() {
        List<EmployeeAllocation> visible = gridImport.getGenericDataView()
                .getItems().collect(Collectors.toList());
        int count = visible.size();

        if (count == 0) {
            showError("No records to delete in the current view.");
            return;
        }

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Confirm Bulk Delete");

        Span text = new Span("This will delete all " + count + " record"
                + (count > 1 ? "s" : "") + " currently shown in the grid. Continue?");
        text.getStyle().set("font-weight", "bold");

        Button yes = new Button("Delete " + count + " Record" + (count > 1 ? "s" : ""), e -> {
            allocations.removeAll(visible);
            visible.forEach(it -> {
                duplicateRecords.remove(it);
                errorRecords.remove(it);
            });

            gridImport.setItems(allocations);
            gridImport.getDataProvider().refreshAll();
            updateGridFooters();
            showSuccess("Deleted " + count + " record" + (count > 1 ? "s" : "") + ".");
            confirm.close();
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> confirm.close());
        confirm.getFooter().add(new HorizontalLayout(yes, cancel));
        confirm.add(text);
        confirm.open();
    }


    // Clear all imported (unsaved) data
    private void clearAllImportedData() {
        int count = allocations != null ? allocations.size() : 0;

        if (count == 0) {
            showError("No imported data to clear.");
            return;
        }

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Clear All Imported Data");

        Span text = new Span("This will permanently clear all " + count + " imported record"
                + (count > 1 ? "s" : "") + ". Are you sure?");
        text.getStyle().set("font-weight", "bold");

        Button yes = new Button("Clear All (" + count + ")", e -> {
            allocations.clear();
            duplicateRecords.clear();
            errorRecords.clear();
            gridImport.setItems(allocations);
            gridImport.getDataProvider().refreshAll();
            updateGridFooters();
            showSuccess("Cleared " + count + " record" + (count > 1 ? "s" : "") + ".");
            confirm.close();
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> confirm.close());
        confirm.getFooter().add(new HorizontalLayout(yes, cancel));
        confirm.add(text);
        confirm.open();
    }



}