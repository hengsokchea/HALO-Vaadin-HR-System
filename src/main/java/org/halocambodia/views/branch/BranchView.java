package org.halocambodia.views.branch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.Branch;
import org.halocambodia.data.BranchManagerAssignment;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.BranchService;
import org.halocambodia.services.EmployeeService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;

@Route(value = "branches", layout = MainLayout.class)
@PageTitle("Location Management")
@PermitAll
@Uses(Icon.class)
public class BranchView extends PageDialogLayout<Branch, BranchService> {

    private final TextField branchFullName = new TextField("Branch Full Name | ឈ្មោះសាខា​ពេញ");
    private final TextField branchShortName = new TextField("Branch Short Name | ឈ្មោះសាខា​ខ្លី");
    private final Checkbox isActive = new Checkbox("Active | សកម្ម");

    // For Manager Assignment (like Supervisor tab)
    private final Grid<BranchManagerAssignment> managerAssignmentGrid = new Grid<>(BranchManagerAssignment.class, false);
    private final List<BranchManagerAssignment> managerAssignmentBuffer = new ArrayList<>();

    private final EmployeeService employeeService;
    private final Optional<User> currentUserLogin;

    public BranchView(BranchService service, UserService userService, 
                      AuthenticatedUser authenticatedUser,
                      EmployeeService employeeService) {
        super(Branch.class, service, userService, authenticatedUser);
        this.employeeService = employeeService;
        this.currentUserLogin = authenticatedUser.get();
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        this.enableToggleColumn = true;
        this.toggleColumnFrozen = false;

        configureGrid();
        configureEditorLayout();
        binderField();

        // Set default active to true for new records
        isActive.setValue(true);
        
        // Set the item details renderer to show manager assignments
        if (enableToggleColumn) {
            grid.setItemDetailsRenderer(buildManagerDetailsRenderer());
            
        }
        grid.setAllRowsVisible(true);

            
    }

    /**
     * Build the details renderer that shows manager assignments when toggle is clicked
     */
    private Renderer<Branch> buildManagerDetailsRenderer() {
        return new ComponentRenderer<>(branch -> {
            TabSheet tabSheet = new TabSheet();
            tabSheet.setSizeFull();
            tabSheet.setWidthFull();
            
            // Get the assignments to calculate height
            List<BranchManagerAssignment> assignments = branch.getBranchManagerAssignments();
            int assignmentCount = assignments != null ? assignments.size() : 0;
            
            // Calculate dynamic height based on number of rows
            // Base height: header (50px) + row height (40px per row) + footer (30px) + padding (20px)
            int rowHeight = 40; // Approximate height per row
            int headerHeight = 50;
            int footerHeight = 30;
            int padding = 20;
            
            // Minimum height: 200px, Maximum height: 500px (to prevent too tall)
            int calculatedHeight = headerHeight + (assignmentCount * rowHeight) + footerHeight + padding;
            int finalHeight = Math.min(Math.max(calculatedHeight, 200), 500);
            
            tabSheet.setHeight(finalHeight + "px");

            // Manager Assignments Tab
            Tab managerTab = new Tab(
                VaadinIcon.USER_STAR.create(),
                buildTabEnKh("Manager Assignments", "ការចាត់តាំងអ្នកគ្រប់គ្រង")
            );
            
            // Create and configure the details grid
            Grid<BranchManagerAssignment> detailsGrid = new Grid<>(BranchManagerAssignment.class, false);
            
            // Set grid to show all rows without scroll
            detailsGrid.setAllRowsVisible(true);
            //detailsGrid.setHeightByRows(true);
            
            configureDetailsGrid(detailsGrid);
            
            tabSheet.add(managerTab, detailsGrid);

            // Load data when tab is selected
            tabSheet.addSelectedChangeListener(event -> {
                if (event.getSelectedTab().equals(managerTab)) {
                    if (assignments != null && !assignments.isEmpty()) {
                        detailsGrid.setItems(assignments);
                        updateGridFooter(detailsGrid, assignments.size());
                        
                        // Recalculate height after data is loaded
                        int newHeight = headerHeight + (assignments.size() * rowHeight) + footerHeight + padding;
                        int newFinalHeight = Math.min(Math.max(newHeight, 200), 500);
                        tabSheet.setHeight(newFinalHeight + "px");
                    } else {
                        detailsGrid.setItems(Collections.emptyList());
                        // Set minimum height for empty grid
                        tabSheet.setHeight("200px");
                    }
                }
            });

            // Load data immediately
            if (assignments != null && !assignments.isEmpty()) {
                detailsGrid.setItems(assignments);
                updateGridFooter(detailsGrid, assignments.size());
            }

            // Add icon-on-top theme to tabs
            for (int i = 0; i < tabSheet.getTabCount(); i++) {
                tabSheet.getTabAt(i).addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
            }

            return tabSheet;
        });
    }

    /**
     * Configure the details grid with all columns
     */
    private void configureDetailsGrid(Grid<BranchManagerAssignment> grid) {
        grid.setSizeFull();
        grid.setWidthFull();
        
        // Important: Set height to be determined by rows
        //grid.setHeightByRows(true);
        grid.setAllRowsVisible(true);
        
        grid.setEmptyStateText("No manager assignments found. | មិនមានការចាត់តាំងអ្នកគ្រប់គ្រង។");
        
        grid.addThemeVariants(
            GridVariant.LUMO_COLUMN_BORDERS,
            GridVariant.LUMO_ROW_STRIPES,
            GridVariant.LUMO_COMPACT
        );

        // Sort Order column - set a key
        grid.addColumn(BranchManagerAssignment::getSortOrder)
            .setKey("sortOrder")
            .setHeader("Order | លំដាប់")
            .setWidth("100px")
            .setFlexGrow(0)
            .setResizable(true)
            .setSortable(true)
            .setTextAlign(ColumnTextAlign.CENTER);

        // Employee/Manager column - set a key
        grid.addColumn(assignment -> {
            if (assignment.getEmployee() != null) {
                return formatEnKh(
                    assignment.getEmployee().getNameEn(), 
                    assignment.getEmployee().getNameKh()
                ) + "-" +  assignment.getEmployee().getInsuranceNo();
            }
            return "";
        })
        .setKey("managerName")
        .setHeader("Manager Name | ឈ្មោះអ្នកគ្រប់គ្រង")
        .setWidth("200px")
        .setResizable(true)
        .setSortable(true);

        // Position - set a key
        grid.addColumn(assignment -> {
            if (assignment.getEmployee() != null && assignment.getEmployee().getPositions() != null) {
                return formatEnKh(
                    assignment.getEmployee().getPositions().getPosition(),
                    assignment.getEmployee().getPositions().getPositionKh()
                );
            }
            return "";
        })
        .setKey("position")
        .setHeader("Position | មុខតំណែង")
        .setWidth("150px")
        .setResizable(true)
        .setSortable(true);

        // Department - set a key
        grid.addColumn(assignment -> {
            if (assignment.getEmployee() != null && assignment.getEmployee().getDepartment() != null) {
                return formatEnKh(
                    assignment.getEmployee().getDepartment().getName(),
                    assignment.getEmployee().getDepartment().getNameKH()
                );
            }
            return "";
        })
        .setKey("department")
        .setHeader("Department | ផ្នែក")
        .setWidth("150px")
        .setResizable(true)
        .setSortable(true);

        // Make all columns auto-width after setting widths
        grid.getColumns().forEach(column -> {
            column.setResizable(true);
            String key = column.getKey();
            if (key != null && !key.equals("sortOrder")) {
                column.setAutoWidth(true);
            }
        });
    }
    
    /**
     * Build the grid for displaying manager assignments in the details section
     */
    private Grid<BranchManagerAssignment> buildManagerDetailsGrid() {
        Grid<BranchManagerAssignment> grid = new Grid<>(BranchManagerAssignment.class, false);
        
        grid.setSizeFull();
        grid.setWidthFull();
        grid.setHeight("350px");
        grid.setEmptyStateText("No manager assignments found. | មិនមានការចាត់តាំងអ្នកគ្រប់គ្រង។");
        
        grid.addThemeVariants(
            GridVariant.LUMO_COLUMN_BORDERS,
            GridVariant.LUMO_ROW_STRIPES,
            GridVariant.LUMO_COMPACT
        );

        // Sort Order column
        grid.addColumn(BranchManagerAssignment::getSortOrder)
            .setHeader("Order | លំដាប់")
            .setAutoWidth(true)
            .setResizable(true)
            .setSortable(true);

        // Employee/Manager column
        grid.addColumn(assignment -> {
            if (assignment.getEmployee() != null) {
                return formatEnKh(
                    assignment.getEmployee().getNameEn(), 
                    assignment.getEmployee().getNameKh()
                ) + (assignment.getEmployee().getInsuranceNo() != null ? 
                    " - " + assignment.getEmployee().getInsuranceNo() : "");
            }
            return "";
        }).setHeader("Manager | អ្នកគ្រប់គ្រង")
          .setAutoWidth(true)
          .setResizable(true)
          .setSortable(true);



        // Position
        grid.addColumn(assignment -> {
            if (assignment.getEmployee() != null && assignment.getEmployee().getPositions() != null) {
                return formatEnKh(
                    assignment.getEmployee().getPositions().getPosition(),
                    assignment.getEmployee().getPositions().getPositionKh()
                );
            }
            return "";
        }).setHeader("Position | មុខតំណែង")
          .setAutoWidth(true)
          .setResizable(true);

        // Department
        grid.addColumn(assignment -> {
            if (assignment.getEmployee() != null && assignment.getEmployee().getDepartment() != null) {
                return formatEnKh(
                    assignment.getEmployee().getDepartment().getName(),
                    assignment.getEmployee().getDepartment().getNameKH()
                );
            }
            return "";
        }).setHeader("Department | ផ្នែក")
          .setAutoWidth(true)
          .setResizable(true);

        // Audit columns
       // addAuditColumnsToDetailsGrid(grid);

        // Make all columns auto-width
        grid.getColumns().forEach(column -> {
            column.setResizable(true);
            column.setSortable(true);
            column.setTextAlign(ColumnTextAlign.CENTER);
        });

        return grid;
    }

    /**
     * Add audit columns to the details grid
     */
    private void addAuditColumnsToDetailsGrid(Grid<BranchManagerAssignment> grid) {
        grid.addColumn(assignment -> 
            assignment.getUserCreated() != null ? assignment.getUserCreated().getName() : "")
            .setHeader("Created By | បង្កើតដោយ")
            .setAutoWidth(true)
            .setResizable(true);

        grid.addColumn(assignment -> 
            assignment.getCreatedAt() != null ? 
                DateTimeUtilFormart.DATE_TIME_FORMATTER.format(assignment.getCreatedAt()) : "")
            .setHeader("Created At | ថ្ងៃបង្កើត")
            .setAutoWidth(true)
            .setResizable(true);

        grid.addColumn(assignment -> 
            assignment.getUserUpdated() != null ? assignment.getUserUpdated().getName() : "")
            .setHeader("Updated By | កែប្រែដោយ")
            .setAutoWidth(true)
            .setResizable(true);

        grid.addColumn(assignment -> 
            assignment.getUpdatedAt() != null ? 
                DateTimeUtilFormart.DATE_TIME_FORMATTER.format(assignment.getUpdatedAt()) : "")
            .setHeader("Updated At | ថ្ងៃកែប្រែ")
            .setAutoWidth(true)
            .setResizable(true);
    }

    /**
     * Update the grid footer with total count
     */
    private void updateGridFooter(Grid<?> grid, int totalCount) {
        if (grid.getColumns().size() > 0) {
            grid.getColumns().get(0).setFooter(
                "Total: " + totalCount + " | សរុប: " + totalCount
            );
        }
    }

    @Override
    protected void configureEditorLayout() throws Exception {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();

        // Tab 1: Basic Information
        FormLayout basicForm = new FormLayout();
        basicForm.setWidthFull();
        basicForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );

        branchFullName.setWidthFull();
        branchShortName.setWidthFull();
        isActive.setWidthFull();

        basicForm.add(branchFullName, branchShortName, isActive);
        basicForm.setColspan(branchFullName, 2);
        basicForm.setColspan(branchShortName, 2);

        Tab basicTab = new Tab(VaadinIcon.BUILDING.create(), buildTabEnKh("Basic Information", "ព័ត៌មានមូលដ្ឋាន"));
        basicTab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);

        // Tab 2: Manager Assignment (like Supervisor tab)
        Tab managerTab = new Tab(VaadinIcon.USER_STAR.create(), buildTabEnKh("Manager Assignment", "ការចាត់តាំងអ្នកគ្រប់គ្រង"));
        managerTab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);

        Component managerComponent = buildManagerAssignmentTab();

        tabs.add(basicTab, basicForm);
        tabs.add(managerTab, managerComponent);

        Div wrapper = new Div(tabs);
        wrapper.getStyle()
                .set("padding", "0 1rem")
                .set("box-sizing", "border-box");

        editorLayout.add(wrapper);
        configureEditorFooter();
    }

    private Component buildManagerAssignmentTab() {
        Button btnAdd = new Button("Add Manager Assignment", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAdd.addClickListener(e -> {
            BranchManagerAssignment assignment = new BranchManagerAssignment();
            assignment.setSortOrder(nextManagerSortOrder());
            managerAssignmentBuffer.add(assignment);
            managerAssignmentGrid.getDataProvider().refreshAll();
        });

        managerAssignmentGrid.removeAllColumns();
        managerAssignmentGrid.setWidthFull();
        managerAssignmentGrid.setAllRowsVisible(true);
        managerAssignmentGrid.addThemeVariants(
            GridVariant.LUMO_COLUMN_BORDERS,
            GridVariant.LUMO_ROW_STRIPES,
            GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        // Sort Order column
        managerAssignmentGrid.addComponentColumn(assignment -> {
            IntegerField orderField = new IntegerField();
            orderField.setWidthFull();
            orderField.setStepButtonsVisible(true);
            orderField.setMin(1);
            orderField.setValue(assignment.getSortOrder());
            orderField.addValueChangeListener(ev -> assignment.setSortOrder(ev.getValue()));
            return orderField;
        })
        .setHeader("Order | លំដាប់")
        .setWidth("120px")
        .setFlexGrow(0)
        .setAutoWidth(false)
        .setResizable(true)
        .setTextAlign(ColumnTextAlign.CENTER);

        // Employee column (like Supervisor in NationalStaffActiveView)
        managerAssignmentGrid.addComponentColumn(assignment -> {
            ComboBox<Employee> employeeCombo = buildEmployeeComboBox();
            employeeCombo.setWidthFull();
            employeeCombo.setValue(assignment.getEmployee());
            employeeCombo.addValueChangeListener(ev -> assignment.setEmployee(ev.getValue()));
            return employeeCombo;
        })
        .setHeader("Manager | អ្នកគ្រប់គ្រង")
        .setFlexGrow(1)
        .setResizable(true);


        // Position (read-only display)
        managerAssignmentGrid.addComponentColumn(assignment -> {
            Span position = new Span();
            if (assignment.getEmployee() != null && assignment.getEmployee().getPositions() != null) {
                position.setText(formatEnKh(
                    assignment.getEmployee().getPositions().getPosition(),
                    assignment.getEmployee().getPositions().getPositionKh()
                ));
            }
            return position;
        })
        .setHeader("Position | មុខតំណែង")
        .setAutoWidth(true)
        .setResizable(true);

        // Department (read-only display)
        managerAssignmentGrid.addComponentColumn(assignment -> {
            Span department = new Span();
            if (assignment.getEmployee() != null && assignment.getEmployee().getDepartment() != null) {
                department.setText(formatEnKh(
                    assignment.getEmployee().getDepartment().getName(),
                    assignment.getEmployee().getDepartment().getNameKH()
                ));
            }
            return department;
        })
        .setHeader("Department | ផ្នែក")
        .setAutoWidth(true)
        .setResizable(true);

        // Delete button
        managerAssignmentGrid.addComponentColumn(assignment -> {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, 
                                       ButtonVariant.LUMO_ICON, 
                                       ButtonVariant.LUMO_ERROR);

            deleteBtn.addClickListener(click -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader("Confirm Delete");
                confirmDialog.setText("Are you sure you want to delete this manager assignment?");
                confirmDialog.setCancelable(true);
                confirmDialog.setCancelText("Cancel");
                confirmDialog.setConfirmText("Delete");
                confirmDialog.setConfirmButtonTheme("error primary");

                confirmDialog.addConfirmListener(ev -> {
                    managerAssignmentBuffer.remove(assignment);
                    managerAssignmentGrid.getDataProvider().refreshAll();
                });

                confirmDialog.open();
            });

            return deleteBtn;
        })
        .setHeader("Action | សកម្មភាព")
        .setFlexGrow(0)
        .setAutoWidth(true)
        .setTextAlign(ColumnTextAlign.CENTER);

        managerAssignmentGrid.setItems(managerAssignmentBuffer);

        VerticalLayout layout = new VerticalLayout(btnAdd, managerAssignmentGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setMargin(false);
        layout.setWidthFull();

        return layout;
    }

    private ComboBox<Employee> buildEmployeeComboBox() {
        ComboBox<Employee> comboBox = new ComboBox<>();
        
        // Use lazy loading for better performance with many employees
        comboBox.setItems(query -> {
            String filter = query.getFilter().orElse("");
            int offset = query.getOffset();
            int limit = query.getLimit();
            
            return employeeService.searchEmployeesByNameEnKhInsurance(filter, 
                org.springframework.data.domain.PageRequest.of(offset / limit, limit))
                .stream();
        });

        comboBox.setItemLabelGenerator(employee -> {
            if (employee == null) return "";
            String name = formatEnKh(employee.getNameEn(), employee.getNameKh());
            String insurance = employee.getInsuranceNo() != null ? 
                " - " + employee.getInsuranceNo().toString() : "";
            return name + insurance;
        });

        comboBox.setPlaceholder("Select manager...");
        comboBox.setClearButtonVisible(true);
        
        return comboBox;
    }

    private Integer nextManagerSortOrder() {
        return managerAssignmentBuffer.stream()
                .map(BranchManagerAssignment::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(v -> v + 1)
                .orElse(1);
    }

    @Override
    protected void populateForm(Branch entity) throws Exception {
        this.entity = entity;

        // Clear and reload manager assignments buffer
        managerAssignmentBuffer.clear();
        if (this.entity.getBranchManagerAssignments() != null) {
            managerAssignmentBuffer.addAll(this.entity.getBranchManagerAssignments());
        }
        managerAssignmentGrid.getDataProvider().refreshAll();

        binder.readBean(this.entity);
        editorLayout.open();
    }

    @Override
    protected void beforeSave(Branch entity, boolean isNew) throws Exception {
        // Sync manager assignments to entity
        syncManagerAssignmentsToEntity(entity);
    }

    private void syncManagerAssignmentsToEntity(Branch target) {
        // Clear current list (orphanRemoval=true will delete removed rows)
        target.getBranchManagerAssignments().clear();

        for (BranchManagerAssignment assignment : managerAssignmentBuffer) {
            // Skip empty rows
            if (assignment.getEmployee() == null) continue;

            // Set the owning side
            assignment.setBranch(target);
            
            // Set audit fields
            if (assignment.getId() == null) {
                assignment.setUserCreated(this.currentUserLogin.get());
            }
            assignment.setUserUpdated(this.currentUserLogin.get());

            // Set default sort order if missing
            if (assignment.getSortOrder() == null) {
                assignment.setSortOrder(nextManagerSortOrder());
            }

            target.getBranchManagerAssignments().add(assignment);
        }
    }

    @Override
    protected void binderField() {
        binder.bindInstanceFields(this);
    }

    @Override
    protected void focusFirstField() {
        this.branchFullName.focus();
    }

    @Override
    protected Branch createNewEntity() {
        Branch branch = new Branch();
        branch.setActive(true); // Default to active
        return branch;
    }

    @Override
    protected List<ColumnDef<Branch>> getColumnDefs() {
        return List.of(
            col("id", "ID", Branch::getId, e -> e.getId() != null ? e.getId().toString() : ""),
            col("branchFullName", this.branchFullName.getLabel(), 
                Branch::getBranchFullName, e -> e.getBranchFullName() != null ? e.getBranchFullName() : ""),
            col("branchShortName", this.branchShortName.getLabel(), 
                Branch::getBranchShortName, e -> e.getBranchShortName() != null ? e.getBranchShortName() : ""),
            col("isActive", this.isActive.getLabel(), 
                Branch::isActive, e -> e.isActive() ? "Yes | បាទ/ចា៎" : "No | ទេ"),
            
            // Manager count column (optional - shows how many managers assigned)
            col("managerCount", "Managers | អ្នកគ្រប់គ្រង", 
                Branch::getBranchManagerAssignments, 
                e -> e.getBranchManagerAssignments() != null ? 
                    String.valueOf(e.getBranchManagerAssignments().size()) : "0"),
            
            // Audit columns
            col("userCreated.name", "Created By | បង្កើតដោយ", 
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),
            col("createdAt", "Created At | ថ្ងៃបង្កើត", 
                e -> e.getCreatedAt(),
                e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),
            col("userUpdated.name", "Updated By | កែប្រែដោយ", 
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),
            col("updatedAt", "Updated At | ថ្ងៃកែប្រែ", 
                e -> e.getUpdatedAt(),
                e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
            new FilterDef(
                "branchFullName",
                this.branchFullName.getLabel(),
                () -> {
                    TextField tf = new TextField();
                    tf.setPlaceholder("Contains...");
                    tf.setWidthFull();
                    return tf;
                },
                c -> ((TextField) c).clear()
            ),
            new FilterDef(
                "branchShortName",
                this.branchShortName.getLabel(),
                () -> {
                    TextField tf = new TextField();
                    tf.setPlaceholder("Contains...");
                    tf.setWidthFull();
                    return tf;
                },
                c -> ((TextField) c).clear()
            ),
            new FilterDef(
                "isActive",
                this.isActive.getLabel(),
                () -> {
                    ComboBox<Boolean> cb = new ComboBox<>();
                    cb.setItems(true, false);
                    cb.setItemLabelGenerator(value -> value ? "Yes | បាទ/ចា៎" : "No | ទេ");
                    cb.setPlaceholder("Select...");
                    cb.setClearButtonVisible(true);
                    cb.setWidthFull();
                    return cb;
                },
                c -> ((ComboBox<?>) c).clear()
            )
        ));

        advPanel.addFilter("name");
        return advPanel;
    }

    @Override
    protected Specification<Branch> buildCombinedSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> sqlFilter = new ArrayList<>();

            String quick = quickSearchField.getValue();
            if (quick != null && !quick.isEmpty()) {
                String like = "%" + quick.toLowerCase().trim() + "%";
                predicates.add(cb.or(
                    buildLikePredicate(cb, root.get("branchFullName"), like),
                    buildLikePredicate(cb, root.get("branchShortName"), like)
                ));
            }

            // Advanced filter
            if (advPanel != null) {
                TextField advBranchFullName = advPanel.getField("branchFullName", TextField.class);
                advanceFilterBuildLikePredicate(
                    cb, root.get("branchFullName"),
                    advBranchFullName == null ? null : advBranchFullName.getValue(),
                    this.branchFullName.getLabel(),
                    predicates, sqlFilter
                );

                TextField advBranchShortName = advPanel.getField("branchShortName", TextField.class);
                advanceFilterBuildLikePredicate(
                    cb, root.get("branchShortName"),
                    advBranchShortName == null ? null : advBranchShortName.getValue(),
                    this.branchShortName.getLabel(),
                    predicates, sqlFilter
                );

                ComboBox<Boolean> advIsActive = advPanel.getField("isActive", ComboBox.class);
                if (advIsActive != null && advIsActive.getValue() != null) {
                    predicates.add(cb.equal(root.get("isActive"), advIsActive.getValue()));
                    sqlFilter.add(this.isActive.getLabel() + " = " + 
                        (advIsActive.getValue() ? "Yes | បាទ/ចា៎" : "No | ទេ"));
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
        return Set.of("managerCount");
    }

    /**
     * Override onDelete to add validation before deletion
     */
    @Override
    protected void onDelete() {
        Set<Branch> selected = new LinkedHashSet<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            showErrorMessage("Please select record(s) to delete. | សូមជ្រើសរើសកំណត់ត្រា ដើម្បីលុប។");
            return;
        }

        // Validate before showing confirmation dialog
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

    /**
     * Validate entities before deletion
     */
    private void validateBeforeDelete(Set<Branch> entities) throws Exception {
        // Check if branches have any manager assignments before deletion
        for (Branch branch : entities) {
            if (branch.getBranchManagerAssignments() != null && 
                !branch.getBranchManagerAssignments().isEmpty()) {
                throw new IllegalArgumentException(
                    "Cannot delete branch with manager assignments. Please remove assignments first. | " +
                    "មិនអាចលុបសាខាដែលមានការចាត់តាំងអ្នកគ្រប់គ្រងបានទេ។ សូមលុបការចាត់តាំងជាមុនសិន។"
                );
            }
        }
    }

    private Component buildTabEnKh(String en, String kh) {
        Div label = new Div(
            new Span(en),
            new Span(kh)
        );
        label.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("line-height", "1.1");
        return label;
    }
}