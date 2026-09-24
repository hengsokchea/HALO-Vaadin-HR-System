package org.halocambodia.views.leave_management;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.LeaveType;
import org.halocambodia.data.LeaveTypeGroup;
import org.halocambodia.data.LeaveTypeGroupRepository;
import org.halocambodia.data.LeaveTypeSubType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.LeaveTypeService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.halocambodia.views.attendance_management.DailyAttendanceView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;

@Route(value = "leave-types", layout = MainLayout.class)
@PageTitle("Duty / Leave | កាតព្វកិច្ច / ច្បាប់ឈប់សម្រាក")
@PermitAll
public class LeaveTypeView extends PageDialogLayout<LeaveType, LeaveTypeService> {

    private final LeaveTypeGroupRepository groupRepository;

    private final TextField leavTypeCode = new TextField("Code | កូដ");
    private final TextField leaveNameEn = new TextField("Duty / Leave Name (English) | ឈ្មោះកាតព្វកិច្ច / ច្បាប់ជាអង់គ្លេស");
    private final TextField leaveNameKh = new TextField("Duty / Leave Name (Khmer) | ឈ្មោះកាតព្វកិច្ច / ច្បាប់ជាខ្មែរ");
    private final ComboBox<LeaveTypeGroup> leaveTypeGroup = new ComboBox<>("Duty / Leave Group | ក្រុមកាតព្វកិច្ច / ច្បាប់");
    private final DatePicker obsoleteDate = new DatePicker("Obsolete Date | កាលបរិច្ឆេទឈប់ប្រើ");
    private final TextArea remarks = new TextArea("Remarks | កំណត់សម្គាល់");
    private final TextField legendColor = new TextField("Legend Color | ពណ៌សម្គាល់");
    private final Input legendColorPicker = new Input();

    private final Grid<LeaveTypeSubType> editorSubTypeGrid = new Grid<>(LeaveTypeSubType.class, false);
    private final List<LeaveTypeSubType> subTypeBuffer = new ArrayList<>();
    private final Span subTypeCount = new Span();
    private Button addSubTypeButton;

    public LeaveTypeView(LeaveTypeService service, LeaveTypeGroupRepository groupRepository,
            UserService userService, AuthenticatedUser authenticatedUser) {
        super(LeaveType.class, service, userService, authenticatedUser);
        this.groupRepository = groupRepository;
        addClassNames("leave-type-view", "payroll-tax-rate-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(LeaveTypeView.class, AccessPageType.SELECTED_PAGE)
                || authenticatedUser.hasPage(DailyAttendanceView.class, AccessPageType.SELECTED_PAGE);
        if (!allowed) event.rerouteTo(AccessDeniedView.class);
    }

    @Override
    protected void onViewAttachOnce(AttachEvent event) throws Exception {
        enableToggleColumn = true;
        toggleColumnFrozen = true;
        toggleColumnWidth = 56;
        toggleColumnKey = "subLeaveTypes";
        configureGrid();
        configureLegendColorColumn();
        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::buildReadOnlySubTypes));
        configureEditorLayout();
        binderField();
    }

    @Override
    protected void configureEditorLayout() {
        leaveTypeGroup.setItems(groupRepository.findAll(Sort.by("leaveTypeGroup")));
        leaveTypeGroup.setItemLabelGenerator(this::groupLabel);
        leaveTypeGroup.setRequiredIndicatorVisible(true);
        leavTypeCode.setRequiredIndicatorVisible(true);
        leaveNameEn.setRequiredIndicatorVisible(true);
        legendColor.setRequiredIndicatorVisible(true);
        legendColor.setPlaceholder("#2563EB");
        legendColor.setMaxLength(7);
        legendColor.setValueChangeMode(ValueChangeMode.EAGER);
        legendColor.setWidthFull();

        legendColorPicker.getElement().setAttribute("type", "color");
        legendColorPicker.setValue("#78716C");
        legendColorPicker.getElement().setAttribute(
                "aria-label", "Select legend color | ជ្រើសរើសពណ៌និមិត្តសញ្ញា");
        legendColorPicker.getStyle()
                .set("width", "52px")
                .set("min-width", "52px")
                .set("height", "var(--lumo-size-m)")
                .set("padding", "2px")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-base-color)")
                .set("box-sizing", "border-box")
                .set("cursor", "pointer");

        legendColorPicker.addValueChangeListener(event -> {
            String color = normalizeLegendColor(event.getValue());
            if (color != null && !color.equals(legendColor.getValue())) {
                legendColor.setValue(color);
            }
        });
        legendColor.addValueChangeListener(event -> {
            String color = normalizeLegendColor(event.getValue());
            if (color != null && !color.equalsIgnoreCase(legendColorPicker.getValue())) {
                legendColorPicker.setValue(color);
            }
        });

        HorizontalLayout legendColorControl = new HorizontalLayout(
                legendColor, legendColorPicker);
        legendColorControl.setWidthFull();
        legendColorControl.setPadding(false);
        legendColorControl.setSpacing(true);
        legendColorControl.setAlignItems(FlexComponent.Alignment.END);
        legendColorControl.setFlexGrow(1, legendColor);
        remarks.setHeight("100px");

        FormLayout form = new FormLayout(leavTypeCode, leaveNameEn, leaveNameKh,
                leaveTypeGroup, legendColorControl, obsoleteDate, remarks);
        form.setWidthFull();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("650px", 2),
                new FormLayout.ResponsiveStep("1050px", 3));
        form.setColspan(remarks, 3);
        Paragraph help = new Paragraph("Maintain the Duty / Leave and its Sub Duty / Leave records, then click Save once to store them together. "
                + "| កំណត់កាតព្វកិច្ច / ច្បាប់ និងប្រភេទរង រួចចុច រក្សាទុក ម្តង។");
        help.getStyle().set("margin", "0").set("color", "var(--lumo-primary-text-color)");
        Div wrapper = new Div(help, form);
        wrapper.setWidthFull();
        wrapper.getStyle().set("padding", "1rem").set("box-sizing", "border-box");
        editorLayout.add(wrapper, buildSubTypeEditorSection());
        configureEditorFooter();
    }

    @Override
    protected void binderField() {
        binder.forField(leavTypeCode).asRequired("Code is required. | ត្រូវបញ្ចូលកូដ។")
                .bind(LeaveType::getLeavTypeCode, LeaveType::setLeavTypeCode);
        binder.forField(leaveNameEn).asRequired("English name is required. | ត្រូវបញ្ចូលឈ្មោះជាអង់គ្លេស។")
                .bind(LeaveType::getLeaveNameEn, LeaveType::setLeaveNameEn);
        binder.bind(leaveNameKh, LeaveType::getLeaveNameKh, LeaveType::setLeaveNameKh);
        binder.forField(legendColor)
                .asRequired("Legend color is required. | ត្រូវបញ្ចូលពណ៌និមិត្តសញ្ញា។")
                .withValidator(value -> value != null && value.matches("^#[0-9A-Fa-f]{6}$"),
                        "Use a hex color such as #2563EB. | ប្រើពណ៌ Hex ដូចជា #2563EB។")
                .bind(LeaveType::getLegendColor, LeaveType::setLegendColor);
        binder.forField(leaveTypeGroup).asRequired("Duty / Leave Group is required. | ត្រូវជ្រើសក្រុមកាតព្វកិច្ច / ច្បាប់។")
                .bind(LeaveType::getLeaveTypeGroup, LeaveType::setLeaveTypeGroup);
        binder.bind(obsoleteDate, LeaveType::getObsoleteDate, LeaveType::setObsoleteDate);
        binder.bind(remarks, LeaveType::getRemarks, LeaveType::setRemarks);
    }

    @Override protected LeaveType createNewEntity() {
        LeaveType v = new LeaveType();
        v.setPaid(true);
        v.setLegendColor("#78716C");
        return v;
    }

    @Override
    protected void populateForm(LeaveType value) {
        entity = value != null && value.getId() != null ? service.findById(value.getId()).orElseThrow() : value;
        loadSubTypeBuffer(entity);
        binder.readBean(entity);
        editorLayout.open();
    }

    @Override
    protected LeaveType save() throws Exception {
        boolean isNew = entity.getId() == null;
        binder.writeBean(entity);
        entity.setLeavTypeCode(entity.getLeavTypeCode().trim().toUpperCase(Locale.ROOT));
        entity.setLeaveNameEn(entity.getLeaveNameEn().trim());
        entity.setLeaveNameKh(blankToNull(entity.getLeaveNameKh()));
        entity.setLegendColor(entity.getLegendColor().trim().toUpperCase(Locale.ROOT));
        entity.setRemarks(blankToNull(entity.getRemarks()));
        validateSubTypes();
        entity = service.updateWithSubTypes(entity, subTypeBuffer);
        showSuccessMessage(getSaveSuccessMessage(isNew));
        return entity;
    }

    @Override protected void clearForm() { super.clearForm(); subTypeBuffer.clear(); refreshSubTypeGrid(); }
    @Override protected void focusFirstField() { leavTypeCode.focus(); }

    @Override
    protected List<ColumnDef<LeaveType>> getColumnDefs() {
        return List.of(
                col("leavTypeCode", "Code | កូដ", LeaveType::getLeavTypeCode, v -> text(v.getLeavTypeCode())),
                col("leaveNameEn", "Duty / Leave Name (English) | ឈ្មោះជាអង់គ្លេស", LeaveType::getLeaveNameEn, v -> text(v.getLeaveNameEn())),
                col("leaveNameKh", "Duty / Leave Name (Khmer) | ឈ្មោះជាខ្មែរ", LeaveType::getLeaveNameKh, v -> text(v.getLeaveNameKh())),
                col("legendColor", "Legend Color | ពណ៌សម្គាល់", LeaveType::getLegendColor, v -> text(v.getLegendColor())),
                col("leaveTypeGroup.leaveTypeGroup", "Duty / Leave Group | ក្រុមកាតព្វកិច្ច / ច្បាប់", v -> v.getLeaveTypeGroup(), v -> groupLabel(v.getLeaveTypeGroup())),
                col("obsoleteDate", "Obsolete Date | ថ្ងៃឈប់ប្រើ", LeaveType::getObsoleteDate, v -> text(v.getObsoleteDate())),
                col("remarks", "Remarks | កំណត់សម្គាល់", LeaveType::getRemarks, v -> text(v.getRemarks())));
    }

    private static String normalizeLegendColor(String value) {
        if (value == null) return null;
        String color = value.trim().toUpperCase(Locale.ROOT);
        return color.matches("^#[0-9A-F]{6}$") ? color : null;
    }

    private void configureLegendColorColumn() {
        Grid.Column<LeaveType> colorColumn = grid.getColumnByKey("legendColor");
        if (colorColumn == null) return;

        colorColumn.setRenderer(new ComponentRenderer<>(this::legendColorCell));
        colorColumn.setWidth("155px");
        colorColumn.setFlexGrow(0);
        colorColumn.setResizable(true);
    }

    private Component legendColorCell(LeaveType leaveType) {
        String color = normalizeLegendColor(leaveType.getLegendColor());
        if (color == null) color = "#78716C";

        Span swatch = new Span();
        swatch.getElement().setAttribute("aria-hidden", "true");
        swatch.getStyle()
                .set("display", "inline-block")
                .set("width", "22px")
                .set("height", "22px")
                .set("min-width", "22px")
                .set("background-color", color)
                .set("border", "1px solid var(--lumo-contrast-30pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("box-shadow", "inset 0 0 0 1px rgba(255, 255, 255, 0.35)");

        Span value = new Span(color);
        value.getStyle()
                .set("font-family", "monospace")
                .set("font-weight", "600");

        HorizontalLayout cell = new HorizontalLayout(swatch, value);
        cell.setPadding(false);
        cell.setSpacing(true);
        cell.setAlignItems(FlexComponent.Alignment.CENTER);
        cell.getElement().setAttribute("title", "Legend color: " + color);
        return cell;
    }

    @Override
    protected Specification<LeaveType> buildCombinedSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> sqlFilter = new ArrayList<>();

            String q = quickSearchField == null ? "" : quickSearchField.getValue().trim().toLowerCase(Locale.ROOT);
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                predicates.add(cb.or(
                        buildLikePredicate(cb, root.get("leavTypeCode"), like),
                        buildLikePredicate(cb, root.get("leaveNameEn"), like),
                        buildLikePredicate(cb, root.get("leaveNameKh"), like),
                        buildLikePredicate(cb, root.get("remarks"), like)));
                sqlFilter.add("Quick Search: " + q);
            }

            if (advPanel != null) {
                TextField code = advPanel.getField("leavTypeCode", TextField.class);
                advanceFilterBuildLikePredicate(cb, root.get("leavTypeCode"),
                        code == null ? null : code.getValue(), leavTypeCode.getLabel(), predicates, sqlFilter);

                TextField nameEn = advPanel.getField("leaveNameEn", TextField.class);
                advanceFilterBuildLikePredicate(cb, root.get("leaveNameEn"),
                        nameEn == null ? null : nameEn.getValue(), leaveNameEn.getLabel(), predicates, sqlFilter);

                TextField nameKh = advPanel.getField("leaveNameKh", TextField.class);
                advanceFilterBuildLikePredicate(cb, root.get("leaveNameKh"),
                        nameKh == null ? null : nameKh.getValue(), leaveNameKh.getLabel(), predicates, sqlFilter);

                MultiSelectComboBox<LeaveTypeGroup> groups = advPanel.getField(
                        "leaveTypeGroup", MultiSelectComboBox.class);
                if (groups != null && groups.getValue() != null && !groups.getValue().isEmpty()) {
                    predicates.add(root.get("leaveTypeGroup").in(groups.getValue()));
                    sqlFilter.add(leaveTypeGroup.getLabel() + " IN ("
                            + groups.getValue().stream().map(this::groupLabel).sorted().reduce((a, b) -> a + ", " + b).orElse("")
                            + ")");
                }

                DatePicker obsolete = advPanel.getField("obsoleteDate", DatePicker.class);
                if (obsolete != null && obsolete.getValue() != null) {
                    predicates.add(cb.equal(root.get("obsoleteDate"), obsolete.getValue()));
                    sqlFilter.add(obsoleteDate.getLabel() + " = " + obsolete.getValue());
                }

                TextField note = advPanel.getField("remarks", TextField.class);
                advanceFilterBuildLikePredicate(cb, root.get("remarks"),
                        note == null ? null : note.getValue(), remarks.getLabel(), predicates, sqlFilter);
            }

            showSqlFilterTokens(sqlFilter);
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
                textFilter("leavTypeCode", leavTypeCode.getLabel()),
                textFilter("leaveNameEn", leaveNameEn.getLabel()),
                textFilter("leaveNameKh", leaveNameKh.getLabel()),
                new FilterDef(
                        "leaveTypeGroup",
                        leaveTypeGroup.getLabel(),
                        () -> buildMultiSelect(
                                this::groupLabel,
                                groupRepository.findAll(Sort.by("leaveTypeGroup"))),
                        component -> ((MultiSelectComboBox<?>) component).clear()),
                new FilterDef(
                        "obsoleteDate",
                        obsoleteDate.getLabel(),
                        () -> {
                            DatePicker field = new DatePicker();
                            field.setClearButtonVisible(true);
                            field.setWidthFull();
                            return field;
                        },
                        component -> ((DatePicker) component).clear()),
                textFilter("remarks", remarks.getLabel())));

        advPanel.addFilter("leavTypeCode");
        return advPanel;
    }

    private FilterDef textFilter(String key, String label) {
        return new FilterDef(
                key,
                label,
                () -> {
                    TextField field = new TextField();
                    field.setPlaceholder("Contains...");
                    field.setClearButtonVisible(true);
                    field.setWidthFull();
                    return field;
                },
                component -> ((TextField) component).clear());
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }
    @Override protected Set<String> getAdditionalExcludedKeys() { return Set.of("version", "leaveTypeSubTypes"); }
    @Override protected Sort getDefaultSort() { return Sort.by("leaveNameEn"); }
    @Override protected String getEntityLabelSingular() { return "duty / leave"; }
    @Override protected String getEntityLabelPlural() { return "duty / leave records"; }
    @Override protected boolean requiresPasswordConfirmation(SensitiveAction action) { return action == SensitiveAction.DELETE; }

    private Component buildReadOnlySubTypes(LeaveType parent) {
        Grid<LeaveTypeSubType> childGrid = new Grid<>(LeaveTypeSubType.class, false);
        configureSubTypeGrid(childGrid);
        List<LeaveTypeSubType> rows = parent.getId() == null ? List.of()
                : service.findById(parent.getId()).map(LeaveType::getLeaveTypeSubTypes).orElse(List.of());
        childGrid.setItems(rows);
        H3 title = new H3("Sub Duty / Leave | ប្រភេទរង — " + text(parent.getLeaveNameEn()));
        title.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-l)");
        Span count = new Span(rows.size() + " sub type(s) | " + rows.size() + " ប្រភេទរង");
        count.getElement().getThemeList().add("badge contrast");
        HorizontalLayout row = new HorizontalLayout(title, count);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        VerticalLayout details = new VerticalLayout(row, childGrid);
        details.setWidthFull(); details.setPadding(true);
        details.getStyle().set("background", "var(--lumo-contrast-5pct)");
        return details;
    }

    private Component buildSubTypeEditorSection() {
        configureSubTypeGrid(editorSubTypeGrid);
        editorSubTypeGrid.addComponentColumn(row -> {
            Button edit = iconButton(VaadinIcon.EDIT, "Edit Sub Duty / Leave | កែប្រែប្រភេទរង", () -> openSubTypeDialog(row));
            Button delete = iconButton(VaadinIcon.TRASH, "Delete Sub Duty / Leave | លុបប្រភេទរង", () -> confirmRemove(row));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Action | សកម្មភាព").setFrozenToEnd(true).setAutoWidth(true).setFlexGrow(0);
        subTypeCount.getElement().getThemeList().add("badge contrast");
        H3 title = new H3("Sub Duty / Leave | ប្រភេទរង"); title.getStyle().set("margin", "0");
        HorizontalLayout titleRow = new HorizontalLayout(title, subTypeCount); titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        addSubTypeButton = new Button("Add | បន្ថែម", new Icon(VaadinIcon.PLUS), e -> openSubTypeDialog(null));
        addSubTypeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        VerticalLayout section = new VerticalLayout(titleRow, addSubTypeButton, editorSubTypeGrid);
        section.setWidthFull(); section.setPadding(true);
        section.getStyle().set("margin", "0 1rem 1rem").set("width", "calc(100% - 2rem)")
                .set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "var(--lumo-border-radius-m)");
        refreshSubTypeGrid(); return section;
    }

    private void configureSubTypeGrid(Grid<LeaveTypeSubType> g) {
        g.setWidthFull(); g.setAllRowsVisible(true); g.setSelectionMode(Grid.SelectionMode.NONE);
        g.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT, GridVariant.LUMO_COLUMN_BORDERS);
        g.addColumn(LeaveTypeSubType::getLeaveSubTypeNameEn).setHeader("Name (English) | ឈ្មោះអង់គ្លេស").setResizable(true).setAutoWidth(true);
        g.addColumn(LeaveTypeSubType::getLeaveSubTypeNameKh).setHeader("Name (Khmer) | ឈ្មោះខ្មែរ").setResizable(true).setAutoWidth(true);
        g.addColumn(v -> text(v.getObsoleteDate())).setHeader("Obsolete Date | ថ្ងៃឈប់ប្រើ").setResizable(true).setAutoWidth(true);
        g.addColumn(LeaveTypeSubType::getRemarks).setHeader("Remarks | កំណត់សម្គាល់").setResizable(true).setAutoWidth(true);
    }

    private void loadSubTypeBuffer(LeaveType parent) {
        subTypeBuffer.clear();
        if (parent != null && parent.getLeaveTypeSubTypes() != null) parent.getLeaveTypeSubTypes().stream().map(this::copy).forEach(subTypeBuffer::add);
        refreshSubTypeGrid();
    }

    private LeaveTypeSubType copy(LeaveTypeSubType s) {
        LeaveTypeSubType c = new LeaveTypeSubType(); c.setId(s.getId()); c.setVersion(s.getVersion());
        c.setLeaveSubTypeNameEn(s.getLeaveSubTypeNameEn()); c.setLeaveSubTypeNameKh(s.getLeaveSubTypeNameKh());
        c.setEntitledDay(s.getEntitledDay()); c.setObsoleteDate(s.getObsoleteDate()); c.setRemarks(s.getRemarks()); return c;
    }

    private void refreshSubTypeGrid() {
        subTypeBuffer.sort(Comparator.comparing(LeaveTypeSubType::getLeaveSubTypeNameEn, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        editorSubTypeGrid.setItems(new ArrayList<>(subTypeBuffer));
        subTypeCount.setText(subTypeBuffer.size() + " sub type(s) | " + subTypeBuffer.size() + " ប្រភេទរង");
    }

    private void openSubTypeDialog(LeaveTypeSubType existing) {
        CustomDialog dialog = new CustomDialog(existing == null ? "New Sub Duty / Leave | ប្រភេទរងថ្មី" : "Edit Sub Duty / Leave | កែប្រែប្រភេទរង");
        dialog.setWidth("min(850px, calc(100vw - 32px))");
        TextField en = new TextField("Name (English) | ឈ្មោះអង់គ្លេស");
        TextField kh = new TextField("Name (Khmer) | ឈ្មោះខ្មែរ");
        DatePicker obsolete = new DatePicker("Obsolete Date | កាលបរិច្ឆេទឈប់ប្រើ");
        TextArea note = new TextArea("Remarks | កំណត់សម្គាល់"); en.setRequiredIndicatorVisible(true);
        if (existing != null) { en.setValue(text(existing.getLeaveSubTypeNameEn())); kh.setValue(text(existing.getLeaveSubTypeNameKh())); obsolete.setValue(existing.getObsoleteDate()); note.setValue(text(existing.getRemarks())); }
        FormLayout form = new FormLayout(en, kh, obsolete, note); form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("650px", 2)); form.setColspan(note, 2);
        VerticalLayout body = new VerticalLayout(form); body.setPadding(true); dialog.add(body);
        Button cancel = new Button("Cancel | បោះបង់", new Icon(VaadinIcon.CLOSE), e -> dialog.close()); cancel.addClickShortcut(Key.ESCAPE);
        Button save = new Button("Save | រក្សាទុក", new Icon(VaadinIcon.CHECK), e -> {
            if (en.getValue().isBlank()) { showErrorMessage("English name is required. | ត្រូវបញ្ចូលឈ្មោះជាអង់គ្លេស។"); return; }
            boolean duplicate = subTypeBuffer.stream().anyMatch(v -> v != existing && v.getLeaveSubTypeNameEn() != null && v.getLeaveSubTypeNameEn().equalsIgnoreCase(en.getValue().trim()));
            if (duplicate) { showErrorMessage("Sub Duty / Leave name already exists. | ឈ្មោះប្រភេទរងមានរួចហើយ។"); return; }
            LeaveTypeSubType v = existing == null ? new LeaveTypeSubType() : existing;
            v.setLeaveSubTypeNameEn(en.getValue().trim()); v.setLeaveSubTypeNameKh(blankToNull(kh.getValue())); v.setObsoleteDate(obsolete.getValue()); v.setRemarks(blankToNull(note.getValue()));
            if (existing == null) subTypeBuffer.add(v); refreshSubTypeGrid(); dialog.close();
        }); save.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS); save.addClickShortcut(Key.ENTER);
        dialog.getFooter().add(cancel, save); dialog.open();
    }

    private void confirmRemove(LeaveTypeSubType value) {
        ConfirmDialog dialog = new ConfirmDialog("Delete Sub Duty / Leave | លុបប្រភេទរង",
                "Remove this Sub Duty / Leave? It is deleted only after the parent Save. | លុបប្រភេទរងនេះមែនទេ? វានឹងលុបពេលរក្សាទុកទិន្នន័យមេ។",
                "Delete | លុប", e -> { subTypeBuffer.remove(value); refreshSubTypeGrid(); }, "Cancel | បោះបង់", e -> {});
        dialog.setConfirmButtonTheme("error primary"); dialog.open();
    }

    private void validateSubTypes() {
        for (LeaveTypeSubType v : subTypeBuffer) {
            if (v.getLeaveSubTypeNameEn() == null || v.getLeaveSubTypeNameEn().isBlank())
                throw new IllegalArgumentException("Every Sub Duty / Leave requires an English name. | គ្រប់ប្រភេទរងត្រូវមានឈ្មោះអង់គ្លេស។");
        }
    }

    private Button iconButton(VaadinIcon icon, String title, Runnable action) {
        Button b = new Button(icon.create(), e -> action.run()); b.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON); b.getElement().setAttribute("title", title); return b;
    }
    private String groupLabel(LeaveTypeGroup value) { return value == null ? "" : text(value.getLeaveTypeGroup()); }
    private static String text(Object value) { return value == null ? "" : value.toString(); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
