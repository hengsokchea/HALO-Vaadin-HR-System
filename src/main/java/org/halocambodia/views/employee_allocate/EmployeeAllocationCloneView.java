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
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import org.halocambodia.views.MainLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeAllocationCloneService;
import org.halocambodia.services.EmployeeAllocationImportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Month;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import java.time.LocalDate;

@PageTitle("Clone Employee Allocation")
@Route(value = "employee-allocation-clone", layout = MainLayout.class)
@PermitAll
public class EmployeeAllocationCloneView extends VerticalLayout implements BeforeEnterObserver {

    // Services & repos – same style as Import view
    private final EmployeeAllocationCloneService cloneService;
    private final EmployeeAllocationImportService importService;
    private final AuthenticatedUser authenticatedUser;

    private final ShiftCycleRepository shiftCycleRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final TeamsRepository teamsRepository;
    private final BranchRepository branchRepository;
    private final ContractsRepository contractsRepository;
    private final CategoryRepository categoryRepository;
    private final PoGradeRepository poGradeRepository;

    // Clone toolbar
    private final ComboBox<Month> fromMonth = new ComboBox<>("From Month");
    private final ComboBox<Integer> fromYear = new ComboBox<>("From Year");
    private final ComboBox<Month> toMonth = new ComboBox<>("To Month");
    private final ComboBox<Integer> toYear = new ComboBox<>("To Year");
    private final Button cloneButton = new Button("Clone Records", new Icon(VaadinIcon.COPY));
    private final Button saveAllButton = new Button("Save Valid Allocations", new Icon(VaadinIcon.CHECK));

    // Grid & data (same field names/keys as Import view)
    private final Grid<EmployeeAllocation> grid = new Grid<>(EmployeeAllocation.class, false);
    private List<EmployeeAllocation> allocations = new ArrayList<>();

    // Inline editors (same as Import)
    private ComboBox<Contracts> contractEditor;
    private ComboBox<Positions> positionsEditor;
    private ComboBox<Teams> teamsEditor;
    private ComboBox<Branch> branchEditor;
    private ComboBox<ShiftCycle> shiftEditor;

    // Editable column keys (match Import)
    private final Set<String> EDITABLE_KEYS = Set.of(
        "contracts.contractCode",
        "teams.teamCode",
        "positions.position",
        "branch.branchFullName",
        "shiftCycle.shift.shiftName"
    );

    // Error/duplicate tracking (same as Import)
    private final Set<EmployeeAllocation> duplicateRecords = new HashSet<>();
    private final Map<EmployeeAllocation, String> errorRecords = new HashMap<>();

    // Progress dialog bits (same pattern)
    private ProgressBar progressBar;
    private Span progressText;

    @Autowired
    public EmployeeAllocationCloneView(EmployeeAllocationCloneService cloneService,
                                       EmployeeAllocationImportService importService,
                                       AuthenticatedUser authenticatedUser,
                                       ShiftCycleRepository shiftCycleRepository,
                                       EmployeeRepository employeeRepository,
                                       PositionRepository positionRepository,
                                       TeamsRepository teamsRepository,
                                       BranchRepository branchRepository,
                                       ContractsRepository contractsRepository,
                                       CategoryRepository categoryRepository,
                                       PoGradeRepository poGradeRepository) {
        this.cloneService = cloneService;
        this.importService = importService;
        this.authenticatedUser = authenticatedUser;

        this.shiftCycleRepository = shiftCycleRepository;
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
        this.teamsRepository = teamsRepository;
        this.branchRepository = branchRepository;
        this.contractsRepository = contractsRepository;
        this.categoryRepository = categoryRepository;
        this.poGradeRepository = poGradeRepository;

        initView();
    }

    private void initView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addInlineStyles();

        // Header with Back + Clone toolbar
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Button backButton = new Button("Back to Allocation",
            new Icon(VaadinIcon.ARROW_LEFT),
            e -> UI.getCurrent().navigate(EmployeeAllocationView.class));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Div toolbar = buildCloneToolbar();
        toolbar.getStyle()
            .set("padding", "10px")
            .set("border-radius", "10px")
            .set("box-shadow", "0 2px 6px rgba(0,0,0,0.1)")
            .set("background-color", "var(--lumo-base-color)");

        headerLayout.add(backButton, toolbar);
        add(headerLayout);

        // Grid setup (same structure/keys as Import)
        grid.setSizeFull();
        grid.setEmptyStateText("No cloned allocations to display.");
        grid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES);
        configureGridColumns();                // columns & status renderer + footers
        configureInlineEditableColumns();      // binder + editors + double-click behavior
        configureGridContextMenu();            // delete single / delete filtered / clear all
        setFlexGrow(1, grid);
        add(grid);
    }

    private Div buildCloneToolbar() {
        // Month/Year combos
        fromMonth.setItems(Month.values());
        toMonth.setItems(Month.values());
        fromMonth.setPlaceholder("From Month");
        toMonth.setPlaceholder("To Month");

        int currentYear = java.time.Year.now().getValue();
        List<Integer> years = List.of(currentYear - 1, currentYear);
        List<Integer> yearsTo = List.of(currentYear, currentYear + 1);
        fromYear.setItems(years);
        toYear.setItems(yearsTo);
        fromYear.setValue(currentYear);
        toYear.setValue(currentYear);
        toMonth.setValue(LocalDate.now().getMonth());

        // Buttons
        cloneButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Tooltip.forComponent(cloneButton).withText("Clone allocations from source to target month/year");
        Tooltip.forComponent(saveAllButton).withText("Save valid allocations to the database");

        cloneButton.addClickListener(e -> startClone());
        saveAllButton.addClickListener(e -> saveAll());

        Icon arrow = VaadinIcon.ARROW_RIGHT.create();
        arrow.setColor("var(--lumo-primary-color)");

        HorizontalLayout bar = new HorizontalLayout(fromMonth, fromYear, arrow, toMonth, toYear,
                cloneButton, saveAllButton);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setSpacing(true);

        return new Div(bar);
    }

    /* ----------------------------- Clone logic (sync) ----------------------------- */

    private void startClone() {
        if (fromMonth.isEmpty() || fromYear.isEmpty() || toMonth.isEmpty() || toYear.isEmpty()) {
            showError("Please select all source and target month/year values.");
            return;
        }

        var userOpt = authenticatedUser.get();
        if (userOpt.isEmpty()) {
            showError("User not logged in. Please sign in again.");
            return;
        }

        Dialog progressDialog = createProgressDialog("Cloning allocations...");
        progressDialog.open();

        try {
            List<EmployeeAllocation> cloned = cloneService.cloneAllocationsForUser(
                fromMonth.getValue().getValue(), fromYear.getValue(),
                toMonth.getValue().getValue(), toYear.getValue(),
                userOpt.get()
            );

            progressDialog.close();

            duplicateRecords.clear();
            errorRecords.clear();

            if (cloned.isEmpty()) {
                showError("No data cloned — no records found or duplicates exist.");
            } else {
                this.allocations = cloned;
                grid.setItems(allocations);
                updateGridFooters();
                refreshStatusColumnRenderer();
                showSuccess("Cloned " + cloned.size() + " records successfully.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            progressDialog.close();
            showError("Clone failed: " + ex.getMessage());
        }
    }

    private Dialog createProgressDialog(String title) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(title);
        dlg.setModal(true);
        dlg.setCloseOnEsc(false);
        dlg.setCloseOnOutsideClick(false);

        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressText = new Span("Working...");

        VerticalLayout content = new VerticalLayout(progressBar, progressText);
        content.setSpacing(true);
        content.setPadding(true);
        dlg.add(content);

        return dlg;
    }

    /* ----------------------------- Grid columns (same as Import) ----------------------------- */

    private void configureGridColumns() {
        grid.removeAllColumns();

        grid.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getNameEn() : "";
        }).setHeader("Name EN").setKey("employee.nameEn").setFrozen(true);

        grid.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getNameKh() : "";
        }).setHeader("Name KH").setKey("employee.nameKh").setFrozen(true);

        grid.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getInsuranceNo() : "";
        }).setHeader("Insurance Nº").setKey("employee.insuranceNo").setFrozen(true);

        grid.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getGender() : "";
        }).setHeader("Gender").setKey("employee.gender");

        grid.addColumn(entityRow -> {
            return entityRow.getPositions() != null ? entityRow.getPositions().getPosition() : "";
        }).setHeader("Position").setKey("positions.position");

        grid.addColumn(entityRow -> {
            return entityRow.getBranch() != null ? entityRow.getBranch().getBranchFullName() : "";
        }).setHeader("Location").setKey("branch.branchFullName");

        grid.addColumn(entityRow -> {
            return entityRow.getTeams() != null ? entityRow.getTeams().getTeamCode() : "";
        }).setHeader("Team").setKey("teams.teamCode");

        grid.addColumn(entityRow -> {
            return entityRow.getCategory() != null ? entityRow.getCategory().getCategory() : "";
        }).setHeader("Category").setKey("category.category");

        grid.addColumn(entityRow -> {
            return entityRow.getEmployee() != null ? entityRow.getEmployee().getBloodGroup() : "";
        }).setHeader("Blood Group").setKey("employee.bloodGroup");

        grid.addColumn(entityRow -> {
            return entityRow.getPoGrade() != null ? entityRow.getPoGrade().getPoGrade() : "";
        }).setHeader("PO Grade").setKey("poGrade.poGrade");

        grid.addColumn(entityRow -> {
            return entityRow.getContracts() != null ? entityRow.getContracts().getContractCode() : "";
        }).setHeader("Contract Code").setKey("contracts.contractCode");

        grid.addColumn(entityRow -> {
            return entityRow.getShiftCycle() != null ? entityRow.getShiftCycle().getShift().getShiftName() : "";
        }).setHeader("Shift").setKey("shiftCycle.shift.shiftName");

        // Status column (same behavior as Import)
        grid.addColumn(buildStatusRenderer()).setHeader("Status").setKey("status");

        grid.getColumns().forEach(column -> {
            column.setResizable(true);
            column.setSortable(true);
            column.setTextAlign(ColumnTextAlign.CENTER);
            column.setAutoWidth(true);
        });

        // Same classNameGenerator as Import (validation/duplicate/save-error highlighting)
        grid.setPartNameGenerator(entity -> {

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

            if (errorRecords.containsKey(entity)) {
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
                Icon duplicateIcon = VaadinIcon.WARNING.create();
                duplicateIcon.setColor("orange");
                Tooltip.forComponent(duplicateIcon)
                    .withText("Duplicate - Already exists in DB for selected month/year")
                    .withPosition(Tooltip.TooltipPosition.TOP_START);
                return duplicateIcon;
            }
            else if (!violations.isEmpty()) {
                Icon errorIcon = VaadinIcon.CLOSE_CIRCLE.create();
                errorIcon.setColor("red");
                String tooltipText = violations.stream()
                    .map(v -> {
                        String fieldName = v.getPropertyPath().toString();
                        String message = v.getMessage();
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
                Icon okIcon = VaadinIcon.CHECK.create(); // fallback if not available -> use CHECK_CIRCLE
                try { okIcon = VaadinIcon.CHECK_CIRCLE.create(); } catch (Exception ignored) {}
                okIcon.setColor("green");
                Tooltip.forComponent(okIcon)
                    .withText("Valid - Ready to save")
                    .withPosition(Tooltip.TooltipPosition.TOP_START);
                return okIcon;
            }
        });
    }

    /* ----------------------------- Inline editing (same as Import) ----------------------------- */

    private void configureInlineEditableColumns() {
        BeanValidationBinder<EmployeeAllocation> binder = new BeanValidationBinder<>(EmployeeAllocation.class);
        Editor<EmployeeAllocation> editor = grid.getEditor();
        editor.setBinder(binder);
        editor.setBuffered(true);

        // Team
        teamsEditor = new ComboBox<>();
        teamsEditor.setItems(teamsRepository.findAll());
        teamsEditor.setItemLabelGenerator(Teams::getTeamCode);
        teamsEditor.setClearButtonVisible(true);
        binder.forField(teamsEditor).bind(EmployeeAllocation::getTeams, EmployeeAllocation::setTeams);

        // Contract
        contractEditor = new ComboBox<>();
        contractEditor.setItems(contractsRepository.findAll());
        contractEditor.setItemLabelGenerator(Contracts::getContractCode);
        contractEditor.setClearButtonVisible(true);
        binder.forField(contractEditor).bind(EmployeeAllocation::getContracts, EmployeeAllocation::setContracts);

        // Position
        positionsEditor = new ComboBox<>();
        positionsEditor.setItems(positionRepository.findAll());
        positionsEditor.setItemLabelGenerator(Positions::getPosition);
        positionsEditor.setClearButtonVisible(true);
        binder.forField(positionsEditor).bind(EmployeeAllocation::getPositions, EmployeeAllocation::setPositions);

        // Branch
        branchEditor = new ComboBox<>();
        branchEditor.setItems(branchRepository.findByIsActiveTrue());
        branchEditor.setItemLabelGenerator(Branch::getBranchFullName);
        branchEditor.setClearButtonVisible(true);
        binder.forField(branchEditor).bind(EmployeeAllocation::getBranch, EmployeeAllocation::setBranch);

        // Shift
        shiftEditor = new ComboBox<>();
        shiftEditor.setItems(shiftCycleRepository.findAll());
        shiftEditor.setItemLabelGenerator(sc -> sc.getShift().getShiftName());
        shiftEditor.setClearButtonVisible(true);
        binder.forField(shiftEditor).bind(EmployeeAllocation::getShiftCycle, EmployeeAllocation::setShiftCycle);

        setupCellEditingVisualFeedback();
    }

    private void setupCellEditingVisualFeedback() {
        grid.addItemDoubleClickListener(event -> {
            if (event.getColumn() == null || event.getColumn().getKey() == null) return;
            EmployeeAllocation clicked = event.getItem();
            String key = event.getColumn().getKey();
            if (clicked != null && EDITABLE_KEYS.contains(key)) {
                setActiveEditor(key, clicked);
            }
        });
    }

    private void setActiveEditor(String columnKey, EmployeeAllocation item) {
        if (grid.getEditor().isOpen()) {
            grid.getEditor().cancel();
        }
        configureGridColumns();          // restore renderers
        configureInlineEditableColumns();// reattach editors

        Component editorComponent = null;
        switch (columnKey) {
            case "contracts.contractCode":
                editorComponent = createInlineEditorLayout(contractEditor, grid.getEditor());
                break;
            case "teams.teamCode":
                editorComponent = createInlineEditorLayout(teamsEditor, grid.getEditor());
                break;
            case "branch.branchFullName":
                editorComponent = createInlineEditorLayout(branchEditor, grid.getEditor());
                break;
            case "positions.position":
                editorComponent = createInlineEditorLayout(positionsEditor, grid.getEditor());
                break;
            case "shiftCycle.shift.shiftName":
                editorComponent = createInlineEditorLayout(shiftEditor, grid.getEditor());
                break;
            default:
                return;
        }

        if (editorComponent != null) {
            Grid.Column<EmployeeAllocation> col = grid.getColumnByKey(columnKey);
            if (col != null) {
                col.setEditorComponent(editorComponent);
                grid.getEditor().editItem(item);
            }
        }
    }

    private HorizontalLayout createInlineEditorLayout(Component editorComponent, Editor<EmployeeAllocation> editor) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setWidthFull();

        editorComponent.getElement().getStyle().set("width", "100%");
        layout.add(editorComponent);
        layout.setFlexGrow(1, editorComponent);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(false);
        buttons.setPadding(false);

        Button saveBtn = new Button(new Icon(VaadinIcon.CHECK), e -> {
            try {
                EmployeeAllocation itemEdit = editor.getItem();
                if (itemEdit == null) return;

                @SuppressWarnings("unchecked")
                BeanValidationBinder<EmployeeAllocation> binder =
                    (BeanValidationBinder<EmployeeAllocation>) editor.getBinder();
                if (binder != null) {
                    binder.writeBeanAsDraft(itemEdit);
                }

                editor.save();

                // clear transient flags for this item
                errorRecords.remove(itemEdit);
                duplicateRecords.remove(itemEdit);

                configureGridColumns();
                refreshStatusColumnRenderer();
                grid.getDataProvider().refreshItem(itemEdit);
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
            configureGridColumns();
        });
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        cancelBtn.getElement().setAttribute("title", "Cancel");

        buttons.add(saveBtn, cancelBtn);
        layout.add(buttons);
        layout.setFlexGrow(0, buttons);
        return layout;
    }

    /* ----------------------------- Footers & status refresh (same as Import) ----------------------------- */

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

        long invalidCount = allocations != null
            ? allocations.stream().filter(e ->
                !validator.validate(e).isEmpty() &&
                !errorRecords.containsKey(e) &&
                !duplicateRecords.contains(e))
              .count()
            : 0;

        if (grid.getColumnByKey("employee.nameEn") != null) {
            Span totalSpan = new Span("📊 Total Records: " + total);
            Tooltip.forComponent(totalSpan).withText("Click to show all records.");
            totalSpan.getStyle().set("cursor", "pointer").set("font-weight", "bold");
            totalSpan.addClickListener(e -> {
                grid.setItems(allocations);
                refreshStatusColumnRenderer();
                grid.getDataProvider().refreshAll();
            });
            grid.getColumnByKey("employee.nameEn").setFooter(totalSpan);
        }

        if (grid.getColumnByKey("employee.nameKh") != null) {
            Span validSpan = new Span("✅ Valid: " + validCount);
            Tooltip.forComponent(validSpan).withText("Click to show only the valid rows.");
            validSpan.getStyle().set("color", "green").set("cursor", "pointer");
            validSpan.addClickListener(e -> {
                ValidatorFactory f = Validation.buildDefaultValidatorFactory();
                Validator v = f.getValidator();
                grid.setItems(allocations.stream()
                    .filter(entity ->
                        v.validate(entity).isEmpty() &&
                        !errorRecords.containsKey(entity) &&
                        !duplicateRecords.contains(entity))
                    .toList());
                refreshStatusColumnRenderer();
                grid.getDataProvider().refreshAll();
            });
            grid.getColumnByKey("employee.nameKh").setFooter(validSpan);
        }

        if (grid.getColumnByKey("employee.insuranceNo") != null) {
            VerticalLayout stats = new VerticalLayout();
            stats.setSpacing(true);
            stats.setPadding(false);

            if (invalidCount > 0) {
                Span invalidSpan = new Span("⚠️ Invalid: " + invalidCount);
                Tooltip.forComponent(invalidSpan).withText("Click to show only records with validation errors.");
                invalidSpan.getStyle().set("color", "#ff9800").set("cursor", "pointer");
                invalidSpan.addClickListener(e -> {
                    ValidatorFactory f = Validation.buildDefaultValidatorFactory();
                    Validator v = f.getValidator();
                    grid.setItems(allocations.stream()
                        .filter(entity ->
                            !v.validate(entity).isEmpty() &&
                            !errorRecords.containsKey(entity) &&
                            !duplicateRecords.contains(entity))
                        .toList());
                    refreshStatusColumnRenderer();
                    grid.getDataProvider().refreshAll();
                });
                stats.add(invalidSpan);
            }

            if (duplicateCount > 0) {
                Span duplicateSpan = new Span("🔄 Duplicates: " + duplicateCount);
                Tooltip.forComponent(duplicateSpan).withText("Click to show only duplicate records.");
                duplicateSpan.getStyle().set("color", "#ff5722").set("cursor", "pointer");
                duplicateSpan.addClickListener(e -> {
                    grid.setItems(allocations.stream()
                        .filter(duplicateRecords::contains)
                        .toList());
                    refreshStatusColumnRenderer();
                    grid.getDataProvider().refreshAll();
                });
                stats.add(duplicateSpan);
            }

            if (errorCount > 0) {
                Span errorSpan = new Span("❌ Errors: " + errorCount);
                Tooltip.forComponent(errorSpan).withText("Click to show only records with save errors.");
                errorSpan.getStyle().set("color", "red").set("cursor", "pointer");
                errorSpan.addClickListener(e -> {
                    grid.setItems(allocations.stream()
                        .filter(errorRecords::containsKey)
                        .toList());
                    refreshStatusColumnRenderer();
                    grid.getDataProvider().refreshAll();
                });
                stats.add(errorSpan);
            }

            grid.getColumnByKey("employee.insuranceNo").setFooter(stats);

            refreshStatusColumnRenderer();
            grid.getDataProvider().refreshAll();
        }
    }

    private void refreshStatusColumnRenderer() {
        Grid.Column<EmployeeAllocation> statusColumn = grid.getColumnByKey("status");
        if (statusColumn != null) {
            boolean autoWidth = statusColumn.isAutoWidth();
            ColumnTextAlign textAlign = statusColumn.getTextAlign();
            String header = statusColumn.getHeaderText();

            grid.removeColumn(statusColumn);

            Grid.Column<EmployeeAllocation> newStatusColumn = grid.addColumn(buildStatusRenderer())
                .setHeader(header)
                .setKey("status")
                .setAutoWidth(autoWidth)
                .setTextAlign(textAlign);

            newStatusColumn.setResizable(true);
            newStatusColumn.setSortable(true);
        }
    }

    /* ----------------------------- Save (same as Import) ----------------------------- */

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
                    List<String> msgs = violations.stream()
                        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                        .collect(Collectors.toList());
                    validationErrors.put(entityTmp, msgs);
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

            duplicateRecords.clear();
            errorRecords.clear();

            for (EmployeeAllocation entity : validEntities) {
                try {
                    importService.update(entity); // same as Import save per row
                    successfullySaved.add(entity);

                } catch (DataIntegrityViolationException ex) {
                    if (ex.getMessage() != null && ex.getMessage().contains("duplicate key value violates unique constraint")) {
                        skippedDuplicates.add(entity);
                        duplicateRecords.add(entity);
                    } else {
                        String msg = extractMeaningfulErrorMessage(ex);
                        otherErrors.put(entity, msg);
                        errorRecords.put(entity, msg);
                    }

                } catch (Exception ex) {
                    String msg = extractMeaningfulErrorMessage(ex);
                    otherErrors.put(entity, msg);
                    errorRecords.put(entity, msg);
                }
            }

            refreshStatusColumnRenderer();

            StringBuilder info = new StringBuilder();
            if (!successfullySaved.isEmpty()) {
                info.append(successfullySaved.size()).append(" records saved");
            }
            if (!skippedDuplicates.isEmpty()) {
                if (info.length() > 0) info.append(", ");
                info.append(skippedDuplicates.size()).append(" duplicates skipped");
            }
            if (!otherErrors.isEmpty()) {
                if (info.length() > 0) info.append(", ");
                info.append(otherErrors.size()).append(" failed with errors");
            }

            if (!otherErrors.isEmpty() || !skippedDuplicates.isEmpty()) {
                Notification.show(info.toString(), 7000, Position.TOP_CENTER)
                    .addThemeVariants(otherErrors.isEmpty() ?
                        NotificationVariant.LUMO_WARNING : NotificationVariant.LUMO_ERROR);
            } else if (!successfullySaved.isEmpty()) {
                Notification.show(info.toString(), 5000, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            allocations.removeAll(successfullySaved);
            grid.getDataProvider().refreshAll();
            updateGridFooters();

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

        errors.forEach((allocation, error) -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setSpacing(true);
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            icon.setColor("red");
            icon.setSize("16px");

            String employeeInfo = allocation.getEmployee() != null
                ? allocation.getEmployee().getNameEn() + " (" + allocation.getEmployee().getInsuranceNo() + ")"
                : "Unknown Employee";

            Span text = new Span(employeeInfo + ": " + error);
            text.getStyle().set("font-size", "small");

            row.add(icon, text);
            content.add(row);
        });

        Button closeButton = new Button("Close", e -> errorDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        errorDialog.add(content);
        errorDialog.getFooter().add(closeButton);
        errorDialog.open();
    }

    private void showValidationErrorsInGrid(Map<EmployeeAllocation, List<String>> validationErrors) {
        grid.getColumns().stream()
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

        grid.setPartNameGenerator(entity ->
            validationErrors.containsKey(entity) ? "validation-error-row" : null
        );

        grid.getDataProvider().refreshAll();
        updateGridFooters();
    }

    /* ----------------------------- Context menu (same as Import) ----------------------------- */

    private void configureGridContextMenu() {
        GridContextMenu<EmployeeAllocation> menu = new GridContextMenu<>(grid);

        menu.addItem(iconItem(VaadinIcon.TRASH, "Delete this record", "red"), e -> {
            e.getItem().ifPresent(this::deleteSingleRecord);
        });

        menu.addItem(iconItem(VaadinIcon.FILTER, "Delete all filtered records", "#ff9800"), e -> {
            deleteFilteredRecords();
        });

        menu.addItem(iconItem(VaadinIcon.ERASER, "Clear all cloned data", "#c62828"), e -> {
            clearAllImportedData();
        });
    }

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

    private void deleteSingleRecord(EmployeeAllocation item) {
        if (item == null) return;
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Confirm Delete");
        confirm.add(new Span("Are you sure you want to delete this record?"));

        Button yes = new Button("Delete", e -> {
            allocations.remove(item);
            duplicateRecords.remove(item);
            errorRecords.remove(item);
            grid.getDataProvider().refreshAll();
            updateGridFooters();
            showSuccess("Record deleted.");
            confirm.close();
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> confirm.close());
        confirm.getFooter().add(new HorizontalLayout(yes, cancel));
        confirm.open();
    }

    private void deleteFilteredRecords() {
        List<EmployeeAllocation> visible = grid.getGenericDataView().getItems().collect(Collectors.toList());
        int count = visible.size();

        if (count == 0) {
            showError("No records to delete in the current view.");
            return;
        }

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Confirm Bulk Delete");

        Span text = new Span("This will delete all " + count + " record" + (count > 1 ? "s" : "") + " currently shown in the grid. Continue?");
        text.getStyle().set("font-weight", "bold");

        Button yes = new Button("Delete " + count + " Record" + (count > 1 ? "s" : ""), e -> {
            allocations.removeAll(visible);
            visible.forEach(it -> {
                duplicateRecords.remove(it);
                errorRecords.remove(it);
            });

            grid.setItems(allocations);
            grid.getDataProvider().refreshAll();
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

    private void clearAllImportedData() {
        int count = allocations != null ? allocations.size() : 0;

        if (count == 0) {
            showError("No cloned data to clear.");
            return;
        }

        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Clear All Cloned Data");

        Span text = new Span("This will permanently clear all " + count + " cloned record" + (count > 1 ? "s" : "") + ". Are you sure?");
        text.getStyle().set("font-weight", "bold");

        Button yes = new Button("Clear All (" + count + ")", e -> {
            allocations.clear();
            duplicateRecords.clear();
            errorRecords.clear();
            grid.setItems(allocations);
            grid.getDataProvider().refreshAll();
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

    /* ----------------------------- Misc & security ----------------------------- */

    private void addInlineStyles() {
        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `" +
            "  .duplicate-row { background-color: #fff3e0 !important; }" +
            "  .duplicate-row:hover { background-color: #ffe0b2 !important; }" +
            "  .save-error-row { background-color: #ffebee !important; }" +
            "  .validation-error-row { background-color: #fff8e1 !important; }" +
            "`;" +
            "document.head.appendChild(style);"
        );
    }

    private void showError(String message) {
        Notification.show(message, 10000, Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification.show(message, 5000, Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(EmployeeAllocationCloneView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }
}
