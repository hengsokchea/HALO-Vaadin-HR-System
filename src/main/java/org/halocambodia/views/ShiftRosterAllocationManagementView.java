package org.halocambodia.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.LocalDateRenderer;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shift | Roster | Allocation Management (Dialog-based CRUD)
 * Vaadin 24, Java 21
 *
 * NOTE:
 *  - This UI intentionally avoids placeholder entity/service classes.
 *  - Wire your own services in the TODO blocks (load/save/delete methods).
 *  - Grids use in-memory providers populated by the TODO loaders.
 */
@PageTitle("Shift · Roster · Allocation")
@Route(value = "shift-roster-allocation", layout = MainLayout.class)
@PermitAll
public class ShiftRosterAllocationManagementView extends VerticalLayout {

    // ===== In-memory data providers (replace with your services/data) =====
    private final ListDataProvider<ShiftRow> shiftProvider = new ListDataProvider<>(new ArrayList<>());
    private final ListDataProvider<RosterRow> rosterProvider = new ListDataProvider<>(new ArrayList<>());
    private final ListDataProvider<AllocationRow> allocationProvider = new ListDataProvider<>(new ArrayList<>());

    // ===== Grids =====
    private Grid<ShiftRow> shiftGrid;
    private Grid<RosterRow> rosterGrid;
    private Grid<AllocationRow> allocationGrid;

    public ShiftRosterAllocationManagementView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildHeader());
        add(buildTabs());
        expand(getChildren().filter(c -> c instanceof TabSheet).findFirst().orElse(null));
    }

    // ---------- Header ----------
    private Div buildHeader() {
        var header = new Div();
        header.getStyle()
                .set("padding", "12px 16px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        var title = new H2("Shift · Roster · Allocation — Management");
        title.getStyle().set("margin", "0");

        header.add(title);
        return header;
    }

    // ---------- Tabs ----------
    private TabSheet buildTabs() {
        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();

        tabs.add(new HorizontalLayout(VaadinIcon.CLOCK.create(), new Span("Shift")), buildShiftCard());
        tabs.add(new HorizontalLayout(VaadinIcon.CALENDAR.create(), new Span("Roster")), buildRosterCard());
        tabs.add(new HorizontalLayout(VaadinIcon.USER_HEART.create(), new Span("Allocation")), buildAllocationCard());


        return tabs;
    }

    // ---------- Card wrapper ----------
    private Div cardWrapper(String title, HorizontalLayout toolbar, Grid<?> grid) {
        var wrap = new Div();
        wrap.getStyle()
                .set("margin", "16px")
                .set("padding", "8px 12px 12px")
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("box-shadow", "var(--lumo-box-shadow-s)"); // soft shadow

        var header = new HorizontalLayout(new Span(title));
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.setWidthFull();
        header.getStyle()
                .set("font-weight", "600")
                .set("padding", "8px 0")
                .set("border-bottom", "1px dashed var(--lumo-contrast-10pct)");

        var inner = new VerticalLayout(toolbar, grid);
        inner.setPadding(false);
        inner.setSpacing(false);
        inner.setSizeFull();
        inner.getStyle().set("gap", "8px");

        wrap.add(header, inner);
        return wrap;
    }

    // ---------- Shift Tab ----------
    private Div buildShiftCard() {
        buildShiftGrid();
        HorizontalLayout toolbar = buildToolbar(
                // Add
                () -> openShiftDialog(null),
                // Edit
                () -> openShiftDialog(shiftGrid.asSingleSelect().getValue()),
                // Delete
                () -> {
                    var selected = shiftGrid.getSelectedItems();
                    if (!selected.isEmpty()) {
                        // TODO replace with service delete
                        shiftProvider.getItems().removeAll(selected);
                        shiftProvider.refreshAll();
                    }
                },
                // Refresh
                this::reloadShifts
        );
        Div card = cardWrapper("Shift Management", toolbar, shiftGrid);
        card.setSizeFull();
        return card;
    }

    private void buildShiftGrid() {
        shiftGrid = new Grid<>();
        shiftGrid.setSizeFull();
        shiftGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_COMPACT);

        shiftGrid.addColumn(ShiftRow::shiftName).setHeader("Shift Name").setAutoWidth(true).setSortable(true);
        shiftGrid.addColumn(row -> row.startTime() != null ? row.startTime().toString() : "")
        .setHeader("Start")
        .setAutoWidth(true);

        shiftGrid.addColumn(row -> row.endTime() != null ? row.endTime().toString() : "")
        .setHeader("End")
        .setAutoWidth(true);

        shiftGrid.addColumn(ShiftRow::breakMinutes).setHeader("Break (min)").setAutoWidth(true);
        shiftGrid.addColumn(ShiftRow::remark).setHeader("Remark").setFlexGrow(1);

        shiftGrid.setDataProvider(shiftProvider);
    }

    private void openShiftDialog(ShiftRow row) {
        boolean isEdit = row != null;
        var dlg = new Dialog();
        dlg.setHeaderTitle(isEdit ? "Edit Shift" : "Add Shift");
        dlg.setWidth(520, Unit.PIXELS);

        // Form fields
        TextField name = new TextField("Shift Name");
        name.setRequired(true);
        TimePicker start = new TimePicker("Start Time");
        start.setRequired(true);
        TimePicker end = new TimePicker("End Time");
        end.setRequired(true);
        IntegerField breakMin = new IntegerField("Break Minutes");
        breakMin.setMin(0);
        breakMin.setStepButtonsVisible(true);
        breakMin.setValue(60);
        TextArea remark = new TextArea("Remark");
        remark.setMaxLength(512);
        remark.setHeight("120px");

        if (isEdit) {
            name.setValue(row.shiftName());
            start.setValue(row.startTime());
            end.setValue(row.endTime());
            breakMin.setValue(row.breakMinutes() == null ? 0 : row.breakMinutes());
            remark.setValue(row.remark() == null ? "" : row.remark());
        }

        FormLayout form = new FormLayout(name, start, end, breakMin, remark);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        form.setColspan(remark, 2);

        // Footer buttons
        Button cancel = new Button("Cancel", e -> dlg.close());
        Button save = new Button(isEdit ? "Save Changes" : "Create",
                e -> {
                    // Minimal validation
                    if (name.isEmpty() || start.isEmpty() || end.isEmpty()) {
                        name.setInvalid(name.isEmpty());
                        start.setInvalid(start.isEmpty());
                        end.setInvalid(end.isEmpty());
                        return;
                    }
                    // TODO: replace with service save
                    if (isEdit) {
                        row.shiftName = name.getValue();
                        row.startTime = start.getValue();
                        row.endTime = end.getValue();
                        row.breakMinutes = breakMin.getValue();
                        row.remark = remark.getValue();
                        shiftProvider.refreshItem(row);
                    } else {
                        ShiftRow created = new ShiftRow(
                                name.getValue(),
                                start.getValue(),
                                end.getValue(),
                                breakMin.getValue(),
                                remark.getValue()
                        );
                        shiftProvider.getItems().add(created);
                        shiftProvider.refreshAll();
                    }
                    dlg.close();
                });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dlg.add(form);
        dlg.getFooter().add(new HorizontalLayout(cancel, save));
        dlg.open();
    }

    private void reloadShifts() {
        // TODO: load from your service/repository
        shiftProvider.getItems().clear();
        shiftProvider.getItems().addAll(Collections.emptyList());
        shiftProvider.refreshAll();
    }

    // ---------- Roster Tab ----------
    private Div buildRosterCard() {
        buildRosterGrid();
        HorizontalLayout toolbar = buildToolbar(
                () -> openRosterDialog(null),
                () -> openRosterDialog(rosterGrid.asSingleSelect().getValue()),
                () -> {
                    var selected = rosterGrid.getSelectedItems();
                    if (!selected.isEmpty()) {
                        // TODO replace with service delete
                        rosterProvider.getItems().removeAll(selected);
                        rosterProvider.refreshAll();
                    }
                },
                this::reloadRoster
        );
        Div card = cardWrapper("Roster Management", toolbar, rosterGrid);
        card.setSizeFull();
        return card;
    }

    private void buildRosterGrid() {
        rosterGrid = new Grid<>();
        rosterGrid.setSizeFull();
        rosterGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_COMPACT);

        rosterGrid.addColumn(RosterRow::empId).setHeader("Emp ID").setAutoWidth(true).setSortable(true);
        rosterGrid.addColumn(new LocalDateRenderer<>(RosterRow::rosterDate, "yyyy-MM-dd")).setHeader("Date").setAutoWidth(true);
        
        rosterGrid.addColumn(row -> row.startTime() != null ? row.startTime().toString() : "")
        .setHeader("Start")
        .setAutoWidth(true);
        rosterGrid.addColumn(row -> row.endTime() != null ? row.endTime().toString() : "")
        .setHeader("End")
        .setAutoWidth(true);

        rosterGrid.addColumn(RosterRow::remark).setHeader("Remark").setFlexGrow(1);

        rosterGrid.setDataProvider(rosterProvider);
    }

    private void openRosterDialog(RosterRow row) {
        boolean isEdit = row != null;
        var dlg = new Dialog();
        dlg.setHeaderTitle(isEdit ? "Edit Roster" : "Add Roster");
        dlg.setWidth(560, Unit.PIXELS);

        IntegerField empId = new IntegerField("Employee ID");
        empId.setRequiredIndicatorVisible(true);
        empId.setMin(1);
        DatePicker date = new DatePicker("Roster Date");
        date.setRequired(true);
        TimePicker start = new TimePicker("Start Time");
        start.setRequired(true);
        TimePicker end = new TimePicker("End Time");
        end.setRequired(true);
        TextArea remark = new TextArea("Remark");
        remark.setMaxLength(512);
        remark.setHeight("120px");
        ComboBox<Integer> shiftCycleId = new ComboBox<>("Shift Cycle ID");
        shiftCycleId.setAllowCustomValue(true); // let users type; replace with real items
        shiftCycleId.setItems(Collections.emptyList()); // TODO populate with your shift_cycle list

        if (isEdit) {
            empId.setValue(row.empId());
            date.setValue(row.rosterDate());
            start.setValue(row.startTime());
            end.setValue(row.endTime());
            remark.setValue(row.remark() == null ? "" : row.remark());
            shiftCycleId.setValue(row.shiftCycleId());
        }

        FormLayout form = new FormLayout(empId, date, start, end, shiftCycleId, remark);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        form.setColspan(remark, 2);

        Button cancel = new Button("Cancel", e -> dlg.close());
        Button save = new Button(isEdit ? "Save Changes" : "Create", e -> {
            if (empId.isEmpty() || date.isEmpty() || start.isEmpty() || end.isEmpty()) {
                empId.setInvalid(empId.isEmpty());
                date.setInvalid(date.isEmpty());
                start.setInvalid(start.isEmpty());
                end.setInvalid(end.isEmpty());
                return;
            }
            // TODO: replace with service save
            if (isEdit) {
                row.empId = empId.getValue();
                row.rosterDate = date.getValue();
                row.startTime = start.getValue();
                row.endTime = end.getValue();
                row.remark = remark.getValue();
                row.shiftCycleId = shiftCycleId.getValue();
                rosterProvider.refreshItem(row);
            } else {
                RosterRow created = new RosterRow(
                        empId.getValue(),
                        date.getValue(),
                        start.getValue(),
                        end.getValue(),
                        remark.getValue(),
                        shiftCycleId.getValue()
                );
                rosterProvider.getItems().add(created);
                rosterProvider.refreshAll();
            }
            dlg.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dlg.add(form);
        dlg.getFooter().add(new HorizontalLayout(cancel, save));
        dlg.open();
    }

    private void reloadRoster() {
        // TODO: load from your service/repository
        rosterProvider.getItems().clear();
        rosterProvider.getItems().addAll(Collections.emptyList());
        rosterProvider.refreshAll();
    }

    // ---------- Allocation Tab ----------
    private Div buildAllocationCard() {
        buildAllocationGrid();
        HorizontalLayout toolbar = buildToolbar(
                () -> openAllocationDialog(null),
                () -> openAllocationDialog(allocationGrid.asSingleSelect().getValue()),
                () -> {
                    var selected = allocationGrid.getSelectedItems();
                    if (!selected.isEmpty()) {
                        // TODO replace with service delete
                        allocationProvider.getItems().removeAll(selected);
                        allocationProvider.refreshAll();
                    }
                },
                this::reloadAllocations
        );
        Div card = cardWrapper("Employee Allocation", toolbar, allocationGrid);
        card.setSizeFull();
        return card;
    }

    private void buildAllocationGrid() {
        allocationGrid = new Grid<>();
        allocationGrid.setSizeFull();
        allocationGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_COMPACT);

        allocationGrid.addColumn(AllocationRow::empId).setHeader("Emp ID").setAutoWidth(true).setSortable(true);
        allocationGrid.addColumn(AllocationRow::yearNumber).setHeader("Year").setAutoWidth(true);
        allocationGrid.addColumn(AllocationRow::monthNumber).setHeader("Month").setAutoWidth(true);
        allocationGrid.addColumn(AllocationRow::branchId).setHeader("Branch ID").setAutoWidth(true);
        allocationGrid.addColumn(AllocationRow::positionId).setHeader("Position ID").setAutoWidth(true);
        allocationGrid.addColumn(AllocationRow::shiftCycleId).setHeader("Shift Cycle ID").setAutoWidth(true);

        allocationGrid.setDataProvider(allocationProvider);
    }

    private void openAllocationDialog(AllocationRow row) {
        boolean isEdit = row != null;
        var dlg = new Dialog();
        dlg.setHeaderTitle(isEdit ? "Edit Allocation" : "Add Allocation");
        dlg.setWidth(560, Unit.PIXELS);

        IntegerField empId = new IntegerField("Employee ID");
        empId.setRequiredIndicatorVisible(true);
        empId.setMin(1);

        IntegerField year = new IntegerField("Year");
        year.setMin(2000);
        year.setMax(2100);
        year.setRequiredIndicatorVisible(true);

        IntegerField month = new IntegerField("Month (1-12)");
        month.setMin(1);
        month.setMax(12);
        month.setRequiredIndicatorVisible(true);

        ComboBox<Integer> branchId = new ComboBox<>("Branch ID");
        branchId.setAllowCustomValue(true);
        branchId.setItems(Collections.emptyList()); // TODO populate with branch list

        ComboBox<Integer> positionId = new ComboBox<>("Position ID");
        positionId.setAllowCustomValue(true);
        positionId.setItems(Collections.emptyList()); // TODO populate with position list

        ComboBox<Integer> shiftCycleId = new ComboBox<>("Shift Cycle ID");
        shiftCycleId.setAllowCustomValue(true);
        shiftCycleId.setItems(Collections.emptyList()); // TODO populate with shift_cycle list

        if (isEdit) {
            empId.setValue(row.empId());
            year.setValue(row.yearNumber());
            month.setValue(row.monthNumber());
            branchId.setValue(row.branchId());
            positionId.setValue(row.positionId());
            shiftCycleId.setValue(row.shiftCycleId());
        }

        FormLayout form = new FormLayout(empId, year, month, branchId, positionId, shiftCycleId);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        Button cancel = new Button("Cancel", e -> dlg.close());
        Button save = new Button(isEdit ? "Save Changes" : "Create", e -> {
            if (empId.isEmpty() || year.isEmpty() || month.isEmpty()) {
                empId.setInvalid(empId.isEmpty());
                year.setInvalid(year.isEmpty());
                month.setInvalid(month.isEmpty());
                return;
            }
            // TODO: replace with service save
            if (isEdit) {
                row.empId = empId.getValue();
                row.yearNumber = year.getValue();
                row.monthNumber = month.getValue();
                row.branchId = branchId.getValue();
                row.positionId = positionId.getValue();
                row.shiftCycleId = shiftCycleId.getValue();
                allocationProvider.refreshItem(row);
            } else {
                AllocationRow created = new AllocationRow(
                        empId.getValue(),
                        year.getValue(),
                        month.getValue(),
                        branchId.getValue(),
                        positionId.getValue(),
                        shiftCycleId.getValue()
                );
                allocationProvider.getItems().add(created);
                allocationProvider.refreshAll();
            }
            dlg.close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dlg.add(form);
        dlg.getFooter().add(new HorizontalLayout(cancel, save));
        dlg.open();
    }

    private void reloadAllocations() {
        // TODO: load from your service/repository
        allocationProvider.getItems().clear();
        allocationProvider.getItems().addAll(Collections.emptyList());
        allocationProvider.refreshAll();
    }

    // ---------- Toolbar ----------
    private HorizontalLayout buildToolbar(Runnable onAdd, Runnable onEdit, Runnable onDelete, Runnable onRefresh) {
        Button addBtn = buildIconButton("Add", VaadinIcon.PLUS, onAdd, ButtonVariant.LUMO_PRIMARY);
        Button editBtn = buildIconButton("Edit", VaadinIcon.EDIT, onEdit);
        Button delBtn = buildIconButton("Delete", VaadinIcon.TRASH, onDelete, ButtonVariant.LUMO_ERROR);
        Button refBtn = buildIconButton("Refresh", VaadinIcon.REFRESH, onRefresh);

        var left = new HorizontalLayout(addBtn, editBtn, delBtn, refBtn);
        left.setSpacing(true);
        left.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        // Slot for future filters/search
        var right = new FlexLayout();
        right.setJustifyContentMode(JustifyContentMode.END);
        right.setWidthFull();

        var bar = new HorizontalLayout(left, right);
        bar.setWidthFull();
        bar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        bar.getStyle().set("padding", "8px 0");
        return bar;
    }

    private Button buildIconButton(String label, VaadinIcon icon, Runnable action, ButtonVariant... variants) {
        Button b = new Button(label, new Icon(icon));
        b.addClickListener(e -> action.run());
        b.addThemeVariants(variants);
        b.getElement().getThemeList().add("tertiary-inline");
        return b;
    }

    // ---------- Data Rows (simple UI models; NOT entities) ----------
    private static class ShiftRow {
        private String shiftName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer breakMinutes;
        private String remark;

        ShiftRow(String shiftName, LocalTime startTime, LocalTime endTime, Integer breakMinutes, String remark) {
            this.shiftName = shiftName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.breakMinutes = breakMinutes;
            this.remark = remark;
        }
        public String shiftName() { return shiftName; }
        public LocalTime startTime() { return startTime; }
        public LocalTime endTime() { return endTime; }
        public Integer breakMinutes() { return breakMinutes; }
        public String remark() { return remark; }
    }

    private static class RosterRow {
        private Integer empId;
        private LocalDate rosterDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String remark;
        private Integer shiftCycleId;

        RosterRow(Integer empId, LocalDate rosterDate, LocalTime startTime, LocalTime endTime, String remark, Integer shiftCycleId) {
            this.empId = empId;
            this.rosterDate = rosterDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.remark = remark;
            this.shiftCycleId = shiftCycleId;
        }
        public Integer empId() { return empId; }
        public LocalDate rosterDate() { return rosterDate; }
        public LocalTime startTime() { return startTime; }
        public LocalTime endTime() { return endTime; }
        public String remark() { return remark; }
        public Integer shiftCycleId() { return shiftCycleId; }
    }

    private static class AllocationRow {
        private Integer empId;
        private Integer yearNumber;   // maps to year_number / year_numger (DB)
        private Integer monthNumber;
        private Integer branchId;
        private Integer positionId;
        private Integer shiftCycleId;

        AllocationRow(Integer empId, Integer yearNumber, Integer monthNumber, Integer branchId, Integer positionId, Integer shiftCycleId) {
            this.empId = empId;
            this.yearNumber = yearNumber;
            this.monthNumber = monthNumber;
            this.branchId = branchId;
            this.positionId = positionId;
            this.shiftCycleId = shiftCycleId;
        }
        public Integer empId() { return empId; }
        public Integer yearNumber() { return yearNumber; }
        public Integer monthNumber() { return monthNumber; }
        public Integer branchId() { return branchId; }
        public Integer positionId() { return positionId; }
        public Integer shiftCycleId() { return shiftCycleId; }
    }

    // ---------- Initial Load ----------
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Preload data (replace with real calls)
        reloadShifts();
        reloadRoster();
        reloadAllocations();
    }
}
