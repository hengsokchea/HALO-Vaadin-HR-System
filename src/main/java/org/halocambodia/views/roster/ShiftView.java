package org.halocambodia.views.roster;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.UnexpectedTypeException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.component.SignaturePad;
import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeLeaveRequestService;
import org.halocambodia.services.ShiftService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.CustomDialog;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.MasterPageDialogLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Shift")
@Route(value = "shift", layout = MainLayout.class)
@PermitAll
@Uses(Icon.class)
public class ShiftView extends MasterPageDialogLayout<Shift, ShiftService> implements BeforeEnterObserver {

    // Form Fields for Leave Request
	private TextField shiftName=new TextField("Shift Name");
	private TimePicker startTime=new TimePicker("Start Time");
	private TimePicker endTime=new TimePicker("End Time");
	private IntegerField breakMinutes =new IntegerField("Break minutes");
	private TextArea remark = new TextArea("Remark");
	
	private TimePicker morningStartTime=new TimePicker("Start Time(Morning)");
	private TimePicker morningEndTime=new TimePicker("End Time(Morning)");
	private IntegerField morningBreakMinutes =new IntegerField("Break minutes(Morning)");
	
	private TimePicker afternoonStartTime=new TimePicker("Start Time(Afternoon)");
	private TimePicker afternoonEndTime=new TimePicker("End Time(Afternoon)");
	private IntegerField afternoonBreakMinutes =new IntegerField("Break minutes(Afternoon)");
	
	
	
	
	


    // Advance Filter Components
    private NumberField advanceFilterID = new NumberField("ID");
   
    private MultiSelectComboBox<User> advanceFilterCreatedBy=new MultiSelectComboBox<User>("Created By");
    private DatePicker advanceFilterCreatedDateFrom=new DatePicker("Created Date From");
    private DatePicker advanceFilterCreatedDateTo=new DatePicker("Created Date To");
    private MultiSelectComboBox<User> advanceFilterUpdatedBy=new MultiSelectComboBox<User>("Updated By");
    private DatePicker advanceFilterUpdaedDateFrom=new DatePicker("Updated Date From");
    private DatePicker advanceFilterUpdaedDateTo=new DatePicker("Updated Date To");

    private final Optional<User> currentUserLogin;
    private final BeanValidationBinder<Shift> binder = new BeanValidationBinder<>(Shift.class);

    // Repositories
 
    

    public ShiftView(ShiftService service, UserService userService, 
                          AuthenticatedUser authenticatedUser) {
        super(service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
     
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

 
    private void binderField() {
        binder.bindInstanceFields(this);
  
    }

    @Override
    protected void configureGrid() {
        getColumnDefinitions().forEach(def -> {
            grid.addColumn(def.textFormatter()::apply)
                .setHeader(def.header())
                .setKey(def.key())
                .setSortProperty(def.key())
                .setSortable(true)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setResizable(true)
                //.setAutoWidth(!"ID".equals(def.header()))
                .setAutoWidth(true)
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
    }
    
 
    @Override
    protected void configureEditorLayout() throws Exception {
        editorLayout.setDialogTitle("Shift | វេន");
 
        // ============================================================
        // MAIN FORM
        // ============================================================
        startTime.setStep(Duration.ofMinutes(30));
        startTime.setMin(LocalTime.of(6, 0));
        startTime.setMax(LocalTime.of(8, 0));
        
        endTime.setStep(Duration.ofMinutes(30));
        endTime.setMin(LocalTime.of(15, 0));
        endTime.setMax(LocalTime.of(18, 0));
        
        breakMinutes.setMin(0);
        breakMinutes.setStep(1);
        breakMinutes.setStepButtonsVisible(true);
        
        
        remark.setHeight("10rem");
        
        FormLayout mainForm = new FormLayout(shiftName,startTime,endTime,breakMinutes,morningStartTime,morningEndTime,morningBreakMinutes,afternoonStartTime,afternoonEndTime,afternoonBreakMinutes,remark);
        mainForm.setWidthFull();
        mainForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("900px", 3),
                new FormLayout.ResponsiveStep("1200px", 4)
        );
        
        mainForm.setColspan(remark, 4);
        
        HorizontalLayout layout = new HorizontalLayout(mainForm);
        layout.setPadding(false);
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle()
            .set("padding-left", "var(--lumo-space-m)")
            .set("padding-right", "var(--lumo-space-m)")
            .set("margin", "0");
        editorLayout.add(layout);

        // ============================================================
        // FOOTER BUTTONS
        // ============================================================
        Button btnCancel = new Button("Cancel | បោះបង់", e -> {
            closeForm();
            clearForm();
        });
        btnCancel.setIcon(new Icon(VaadinIcon.CLOSE));
        btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        btnCancel.addClickShortcut(Key.ESCAPE);

        Button btnSave = new Button("Save | រក្សាទុក", e -> save());
        btnSave.setIcon(new Icon(VaadinIcon.CHECK));
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnSave.addClickShortcut(Key.ENTER);

        HorizontalLayout buttonLayout = new HorizontalLayout(btnSave, btnCancel);
        buttonLayout.setClassName("button-layout");
        editorLayout.getFooter().add(buttonLayout);

 
    }


    private void save() {
        try {
            binder.writeBean(entity);
            entity = service.update(entity);

                    Notification.show("Shift saved successfully", 2000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    clearForm();
                    closeForm();
                    refreshGrid();
   

        } catch (ValidationException ex) {
            showError("Validation failed: " + ex.getMessage());
        } catch (Exception ex) {
            showError("Error saving record: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    @Override
    protected void populateForm(Shift entity) throws Exception {
        binder.readBean(entity);
        editorLayout.open();
        boolean isNewEntity = entity.getId() == null || entity.getId() == 0;
    }


    private void clearForm() {
        binder.readBean(null);
        this.entity = null;
    }

    private void closeForm() {
        editorLayout.close();
    }
    
    @Override
    protected Specification<Shift> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                // === JOINS ===
                Join<EmployeeLeave, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<EmployeeLeave, User> userUpdated = root.join("userUpdated", JoinType.LEFT);
               
                // === QUICK SEARCH ===
                String quick = txtQuick.getValue();
                if (quick != null && !quick.isEmpty()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";

                    predicates.add(cb.or(
                        buildLikePredicate(cb, root.get("id"), like),
                       
                        buildLikePredicate(cb, userCreated.get("name"), like),
                        buildLikePredicate(cb, userUpdated.get("name"), like),
                        buildLikePredicate(cb, root.get("createdAt"), like),
                        buildLikePredicate(cb, root.get("updatedAt"), like)
                    ));
                }

                // === ID FILTER ===
                if (advanceFilterID.getValue() != null) {
                    predicates.add(cb.equal(root.get("id"), advanceFilterID.getValue().longValue()));
                    sqlFilter.add("ID = " + advanceFilterID.getValue().longValue());
                }


                buildInPredicate(cb, userCreated, advanceFilterCreatedBy.getValue(), User::getName, "CreatedBy", predicates, sqlFilter);

                buildInPredicate(cb, userUpdated, advanceFilterUpdatedBy.getValue(),User::getName, "UpdatedBy", predicates, sqlFilter);

                // === FINAL DISPLAY ===
                this.showHideAdvanceFilter(sqlFilter.isEmpty() ? null : String.join(" AND ", sqlFilter));

                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception ex) {
                showError("Error in filter: " + ex.getMessage());
                ex.printStackTrace();
                return cb.conjunction();
            }
        };
    }

    /**
     * 🔹 Universal LIKE predicate builder
     * Handles String, numeric, and temporal (date/time) fields.
     */
    private Predicate buildLikePredicate(CriteriaBuilder cb, Expression<?> expression, String likePattern) {
        Expression<String> stringExpr;

        // Handle String fields directly
        if (String.class.equals(expression.getJavaType())) {
            stringExpr = cb.lower((Expression<String>) expression);
        }
        // Handle numeric fields (Integer, Long, BigDecimal, etc.)
        else if (Number.class.isAssignableFrom(expression.getJavaType())) {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        }
        // Handle date/time fields (LocalDate, LocalDateTime, Date, Timestamp)
        else if (java.time.temporal.Temporal.class.isAssignableFrom(expression.getJavaType())
                || java.util.Date.class.isAssignableFrom(expression.getJavaType())) {
            stringExpr = cb.lower(DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(cb, expression));
        }
        // Fallback for any other type (safe cast to text)
        else {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        }

        return cb.like(stringExpr, likePattern);
    }

    /**
     * 🔹 IN predicate builder (for multi-select filters).
     */
    private <E> void buildInPredicate(
            CriteriaBuilder cb,
            Expression<E> expression,
            Collection<E> values,
            Function<E, String> labelMapper,
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter) {

        if (values != null && !values.isEmpty()) {
            predicates.add(expression.in(values));
            if (sqlFilter != null) { // ✅ Only build SQL string if needed
                String joined = values.stream()
                        .map(labelMapper)
                        .filter(Objects::nonNull)
                        .map(v -> "'" + v.replace("'", "''") + "'")
                        .collect(Collectors.joining(", "));
                sqlFilter.add(label + " IN (" + joined + ")");
            }
        }
    }


    /**
     * 🔹 BETWEEN predicate builder (for date range filters).
     */
    private <T extends Comparable<? super T>> void buildBetweenPredicate(
            CriteriaBuilder cb,
            Expression<T> expression,
            T from,
            T to,
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter) {

        if (from != null && to != null) {
            predicates.add(cb.between(expression, from, to));
            sqlFilter.add(String.format("%s BETWEEN '%s' AND '%s'", label, from, to));
        }
    }


    @Override
    protected void createAdvanceFilterLayout() {
        // Configure advance filter components
        advanceFilterID.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        
        setupMultiSelectComboBox(advanceFilterCreatedBy, userService.getAllUser(),User::getName);
        setupMultiSelectComboBox(advanceFilterUpdatedBy, userService.getAllUser(),User::getName); 


        advanceFilterSettingDateRank(advanceFilterCreatedDateFrom, advanceFilterCreatedDateTo);
        advanceFilterSettingDateRank(advanceFilterUpdaedDateFrom, advanceFilterUpdaedDateTo);


        this.advanceSearchLayout.add(
            advanceFilterID,
 
            
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
        shiftName.focus();
    }

    @Override
    protected Shift createNewEntity() throws Exception {        
        return new Shift();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(ShiftView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    // Column Definitions for Leave Request
    private record ColumnDef(
        String key,
        String header,
        ValueProvider<Shift, ?> dataProvider,
        Function<Shift, String> textFormatter
    ) {}

    private List<ColumnDef> getColumnDefinitions() {
        return List.of(
            new ColumnDef("id", "ID",
            		Shift::getId,
                e -> e.getId() != null ? e.getId().toString() : ""),
            
            new ColumnDef("shiftName", "Shift Name",
            		Shift::getShiftName,
                e -> e.getShiftName() != null ? e.getShiftName().toString() : ""),
            new ColumnDef("startTime", "Start Time",
            		Shift::getStartTime,
                e -> e.getStartTime() != null ? e.getStartTime().toString() : ""),
            
            new ColumnDef("endTime", "End Time",
            		Shift::getEndTime,
                e -> e.getEndTime() != null ? e.getEndTime().toString() : ""),
            new ColumnDef("breakMinutes", "Total Break (Min)",
            		Shift::getBreakMinutes,
                e -> e.getBreakMinutes() != null ? e.getBreakMinutes().toString() : ""),
            
            
            
            new ColumnDef("morningStartTime", morningStartTime.getLabel(),
            		Shift::getMorningStartTime,
                e -> e.getMorningStartTime() != null ? e.getMorningStartTime().toString() : ""),
            
            new ColumnDef("morningEndTime", morningEndTime.getLabel(),
            		Shift::getMorningEndTime,
                e -> e.getMorningEndTime() != null ? e.getMorningEndTime().toString() : ""),
            new ColumnDef("morningBreakMinutes", morningBreakMinutes.getLabel(),
            		Shift::getMorningBreakMinutes,
                e -> e.getMorningBreakMinutes() != null ? e.getMorningBreakMinutes().toString() : ""),
            
            
            
            new ColumnDef("afternoonStartTime", afternoonStartTime.getLabel(),
            		Shift::getAfternoonStartTime,
                e -> e.getAfternoonStartTime() != null ? e.getAfternoonStartTime().toString() : ""),            
            new ColumnDef("afternoonEndTime", afternoonEndTime.getLabel(),
            		Shift::getAfternoonEndTime,
                e -> e.getAfternoonEndTime() != null ? e.getAfternoonEndTime().toString() : ""),
            new ColumnDef("afternoonBreakMinutes", afternoonBreakMinutes.getLabel(),
            		Shift::getAfternoonBreakMinutes,
                e -> e.getAfternoonBreakMinutes() != null ? e.getAfternoonBreakMinutes().toString() : ""),
            
            
            
            new ColumnDef("remark", "Remark",
            		Shift::getRemark,
                e -> e.getRemark() != null ? e.getRemark().toString() : ""),

            new ColumnDef("userCreated.name", "Created By",
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

            new ColumnDef("createdAt", "Created At",
            		Shift::getCreatedAt,
                e -> e.getCreatedAt() != null ?
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

            new ColumnDef("userUpdated.name", "Updated By",
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

            new ColumnDef("updatedAt", "Updated At",
            		Shift::getUpdatedAt,
                e -> e.getUpdatedAt() != null ?
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
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

                List<Shift> itemsToExport = grid.getSelectedItems().isEmpty()
                        ? grid.getGenericDataView().getItems().toList()
                        : new ArrayList<>(grid.getSelectedItems());

                if (itemsToExport.isEmpty()) {
                    Notification.show("No data to export", 1500, Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                    return;
                }

                StreamResource resource = new StreamResource("Shift.xlsx", () -> {
                    try (Workbook workbook = new XSSFWorkbook()) {
                        Sheet sheet = workbook.createSheet("Shift");

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
                        for (Shift entity : itemsToExport) {
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
    
	 // --- MultiSelectComboBox Helper ---
	 private <T> void setupMultiSelectComboBox(MultiSelectComboBox<T> comboBox, List<T> items, ValueProvider<T, String> labelGenerator) {
	     comboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
	     comboBox.setWidthFull();
	     comboBox.setClearButtonVisible(true);
	     comboBox.setItems(items);
	     comboBox.setItemLabelGenerator(labelGenerator::apply);
	     comboBox.setAutoExpand(AutoExpandMode.BOTH);
	 }

}