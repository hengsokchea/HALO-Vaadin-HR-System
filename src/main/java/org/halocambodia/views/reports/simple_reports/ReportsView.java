package org.halocambodia.views.reports.simple_reports;

import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexWrap;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

import jakarta.annotation.security.PermitAll;
import net.sf.jasperreports.engine.JRException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.*;
import org.halocambodia.views.CustomDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@PageTitle("Simple Reports")
@Route(value = "simple-reports", layout = org.halocambodia.views.MainLayout.class)
@PermitAll
public class ReportsView extends FlexLayout implements BeforeEnterObserver {

    // Main report form fields
    private final TextField reportName = new TextField("Report name");
    private final TextArea query = new TextArea("Query definition");
    private final ComboBox<String> reportType = new ComboBox<>("Report Type", "Simple", "Jaspersoft");
    private final ComboBox<String> reportGroupName = new ComboBox<>("Group heading");
    
    private final MultiSelectComboBox<Role> roles =new MultiSelectComboBox<Role>("Role");
    private final TextArea notes = new TextArea("Notes for users");
    private final IntegerField sortOrder = new IntegerField("Sort Order");
    
    //for JasperSoft Report
    private final TextField reportPath = new TextField("Jaspersoft");    
    InMemoryUploadHandler inMemoryHandler = UploadHandler.inMemory((metadata, data) -> {});
    private final Upload upload = new Upload(inMemoryHandler);
    
    private  Path uploadDir;
    private static final String simpleReportsDir="simple-reports";
    private static final String attachmentsDir="attachments";
    private final FileUploadUtility fileUploadUtility ;
    private Path baseDir;
    private Path baseAttachmentsDir;
    
    // Disk-streaming upload state
    private final List<Path> stagedFiles = new ArrayList<>(); // uploaded temp files (not yet committed)
    private Path tempUploadDir;



    private ReportQuery entity;
    private final BeanValidationBinder<ReportQuery> binder = new BeanValidationBinder<>(ReportQuery.class);

    // Parameters grid + inline editor
    private VerticalLayout paramaterLayout;
    private final Grid<ReportQueryParamater> gridParamater = new Grid<>(ReportQueryParamater.class, false);
    private final Editor<ReportQueryParamater> editor = gridParamater.getEditor();
    private final Binder<ReportQueryParamater> editorBinder = new Binder<>(ReportQueryParamater.class);
    
 // DnD + a single backing list the grid always uses
    private ReportQueryParamater draggingParam = null;
    private List<ReportQueryParamater> paramItems = new ArrayList<>();




    // UI state + services
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NamedParameterJdbcTemplate namedJdbcTemplate;

    private final ReportQueryRepository reportQueryRepository;
    private final ReportQueryService service;
    private final ReportQueryParamaterRepository reportQueryParamaterRepository;
    private final LookupService lookupService;
    private final RoleRepository roleRepository ;

    private final Optional<User> currentUserLogin;
    private final AuthenticatedUser authenticatedUser;

    private final Button btnAddSimpleReport =  new Button("Create new simple report", VaadinIcon.FILE_ADD.create(),e -> this.add());

    private final CustomDialog createSimpleReportDialog = new CustomDialog("Create new simple report");

    // Parameter dialog temp components
    private final HashMap<String, Component> paramComponentMap = new HashMap<>();
    private List<String> distinctGroupNames;

    // Excel anchor
    private Anchor excelDownloadLink;

    public ReportsView(AuthenticatedUser authenticatedUser,
                       ReportQueryRepository reportQueryRepository,
                       ReportQueryService service,
                       LookupService lookupService,
                       ReportQueryParamaterRepository reportQueryParamaterRepository,RoleRepository roleRepository,FileUploadUtility fileUploadUtility ) {
        this.authenticatedUser = authenticatedUser;
        this.currentUserLogin = authenticatedUser.get();
        this.reportQueryRepository = reportQueryRepository;
        this.service = service;
        this.lookupService = lookupService;
        this.reportQueryParamaterRepository = reportQueryParamaterRepository;
        this.roleRepository=roleRepository;
        
        this.fileUploadUtility=fileUploadUtility;
	 	 uploadDir=fileUploadUtility.initializeUploadDirectory("hr", "");
	 	baseAttachmentsDir=fileUploadUtility.initializeUploadDirectory("hr", attachmentsDir);

        setFlexWrap(FlexWrap.WRAP);
        getStyle().set("gap", "0.2rem");
        setJustifyContentMode(JustifyContentMode.START);

        List<ReportQuery> reportQueryList = this.service.findAll(null);

        distinctGroupNames = reportQueryList.stream()
            .map(ReportQuery::getReportGroupName)
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .sorted()
            .toList();

        HorizontalLayout reportToolbar = new HorizontalLayout();
        reportToolbar.add(btnAddSimpleReport);
        btnAddSimpleReport.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        reportToolbar.setSizeFull();

        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();

        for (String groupName : distinctGroupNames) {
            List<ReportQuery> groupReports = reportQueryList.stream()
                .filter(r -> groupName.equals(r.getReportGroupName()))
                .sorted(Comparator.comparing(
                    r -> r.getSortOrder() != null ? r.getSortOrder() : 0,
                    Comparator.naturalOrder()
                ))
                .toList();

            HorizontalLayout content = new HorizontalLayout();
            content.setWidthFull();
            content.setSpacing(true);
            content.setAlignItems(Alignment.START);
            content.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(4, 1fr)")
                .set("gap", "10px");

            for (ReportQuery report : groupReports) {
                Card card = new Card();
                HorizontalLayout header = new HorizontalLayout();
                header.setWidthFull();
                header.setAlignItems(Alignment.CENTER);
                header.setJustifyContentMode(JustifyContentMode.BETWEEN);

                Span title = new Span(report.getReportName());
                title.getStyle().set("font-weight", "bold");
                title.getStyle().set("font-size", "1rem");

                MenuBar menuBar = new MenuBar();
                menuBar.addThemeVariants(MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_TERTIARY_INLINE);
                MenuItem mainMenu = menuBar.addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));

                SubMenu subMenu = mainMenu.getSubMenu();

                Icon editIcon = new Icon(VaadinIcon.PENCIL);
                editIcon.getStyle().set("color", "blue");
                subMenu.addItem(new HorizontalLayout(editIcon, new Span("Edit")), e -> {

           	      if (!authenticatedUser.hasPage(ReportsView.class, AccessPageType.UPDATED_PAGE)) {           	          
           	          //showError("You don't have permission to perform this operation");
           	    	  UI.getCurrent().navigate(org.halocambodia.views.access_denied.AccessDeniedView.class);
           	          return;
           	      }
                	this.popupForm(report);
                });

                Icon trashIcon = new Icon(VaadinIcon.TRASH);
                trashIcon.getStyle().set("color", "red");
                subMenu.addItem(new HorizontalLayout(trashIcon, new Span("Delete")), e -> confirmDelete(report, card));

                header.add(title, menuBar);
                card.add(header);

                card.addThemeVariants(CardVariant.LUMO_ELEVATED);
                card.addClassName("card-hover");

                card.addToFooter(new Button("Run report", e -> reportParamaterDialog(report.getId())));
                content.add(card);
            }

            Details details = new Details(new H3(groupName), content);
            details.setOpened(true);
            details.setWidthFull();
            container.add(details);
        }

        add(reportToolbar, container);
    }

    /* =========================
     *  Parameter dialog
     * ========================= */
    private void reportParamaterDialog(Long reportID) {
        if (reportID == null) {
            this.showError("No report to run.");
            return;
        }

        try {
            Optional<ReportQuery> reportFound = this.reportQueryRepository.findById(reportID);
            if (reportFound.isEmpty()) {
                this.showError("Report not found.");
                return;
            }
            
            if (!canRunReport(currentUserLogin, reportFound.get())) {
                // You are not in a BeforeEnter handler here, so don't use event.rerouteTo(...)
                UI.getCurrent().navigate(org.halocambodia.views.access_denied.AccessDeniedView.class);
                return;
            }

            Dialog dialogParamater = new Dialog();
            dialogParamater.setModal(false);
            dialogParamater.setDraggable(true);
            dialogParamater.setResizable(true);
            dialogParamater.setHeaderTitle(reportFound.get().getReportName());

            Button closeButtonDialogParamater =
                new Button(new Icon(VaadinIcon.CLOSE_SMALL), (e) -> dialogParamater.close());
            closeButtonDialogParamater.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            dialogParamater.getHeader().add(closeButtonDialogParamater);

            // Build parameter form
            FormLayout form = new FormLayout();
            paramComponentMap.clear();

            reportFound.get().getReportQueryParamaterList().stream()
                .sorted(Comparator.comparingInt(ReportQueryParamater::getParamaterSortOrder))
                .forEach(param -> {
                    if (param.getParamaterType().equalsIgnoreCase("TextField")) {
                        TextField textField = new TextField(param.getParamaterLabel());
                        textField.setClearButtonVisible(true);
                        textField.setRequired(Boolean.TRUE.equals(param.getRequiredField()));
                        form.add(textField);
                        paramComponentMap.put(param.getParamaterName(), textField);
                    } else if (param.getParamaterType().equalsIgnoreCase("NumberField")) {
                        NumberField numberField = new NumberField(param.getParamaterLabel());
                        numberField.setClearButtonVisible(true);
                        numberField.setRequired(Boolean.TRUE.equals(param.getRequiredField()));
                        form.add(numberField);
                        paramComponentMap.put(param.getParamaterName(), numberField);
                    } else if (param.getParamaterType().equalsIgnoreCase("DatePicker")) {
                        DatePicker datePicker = new DatePicker(param.getParamaterLabel());
                        datePicker.setRequired(Boolean.TRUE.equals(param.getRequiredField()));
                        datePicker.setClearButtonVisible(true);
                        form.add(datePicker);
                        paramComponentMap.put(param.getParamaterName(), datePicker);
                    } else if (param.getParamaterType().equalsIgnoreCase("ComboBox")) {
                        ComboBox<LookupValue> comboBox = new ComboBox<>(param.getParamaterLabel(),
                            e -> this.comboBoxChangeValue(param, e.getValue()));
                        comboBox.setItems(lookupService.getValues(param.getParamaterLookup()));
                        comboBox.setItemLabelGenerator(LookupValue::getDescription);
                        comboBox.setClearButtonVisible(true);
                        comboBox.setRequired(Boolean.TRUE.equals(param.getRequiredField()));
                        form.add(comboBox);
                        paramComponentMap.put(param.getParamaterName(), comboBox);
                    } else if (param.getParamaterType().equalsIgnoreCase("MultiSelectComboBox")) {
                        MultiSelectComboBox<LookupValue> multi = new MultiSelectComboBox<>(param.getParamaterLabel());
                        multi.setItems(lookupService.getValues(param.getParamaterLookup()));
                        multi.setItemLabelGenerator(LookupValue::getDescription);
                        multi.setAutoExpand(AutoExpandMode.VERTICAL);
                        multi.setClearButtonVisible(true);
                        multi.setRequired(Boolean.TRUE.equals(param.getRequiredField()));
                        form.add(multi);
                        paramComponentMap.put(param.getParamaterName(), multi);
                    }
                });

           dialogParamater.add(form, new Span(reportFound.get().getNotes()));

            Button runReportButton = new Button("Preview Report", e -> previewReport(reportFound.get()));
            runReportButton.addClickShortcut(Key.ENTER);
            runReportButton.getStyle().set("margin-right", "auto");
            dialogParamater.getFooter().add(runReportButton);

            MenuBar exportMenuBar = new MenuBar();
            exportMenuBar.addThemeVariants(MenuBarVariant.LUMO_ICON);

            exportMenuBar.addItem("Export XLSX", e -> {
                try {
                    HashMap<String, Object> parameters = collectParametersOrThrow();
                    List<Map<String, Object>> data = fetchQueryData(reportFound.get().getQuery(), parameters);
                    exportToExcel(data, reportFound.get().getReportName());
                } catch (IllegalStateException ex) {
                	 ex.printStackTrace();
                    this.showError(ex.getMessage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.showError("Failed to export Excel.");
                }
            });

            MenuItem exportMenuBarItem = exportMenuBar.addItem(new Icon(VaadinIcon.CHEVRON_DOWN));
            SubMenu exportMenuBarSubItems = exportMenuBarItem.getSubMenu();
            exportMenuBarSubItems.addItem("Export PDF", e -> Notification.show("PDF export not implemented here."));
            exportMenuBarSubItems.addItem("Export DOCX", e -> Notification.show("DOCX export not implemented here."));

            dialogParamater.getFooter().add(exportMenuBar);
           
            dialogParamater.open();

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Something went wrong.");
        }
    }

    /* =========================
     *  Preview (reuses collector)
     * ========================= */
    private void previewReport(ReportQuery reportQuery) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Report Preview (" + reportQuery.getReportName() + ")");

        Button minimizeButton = new Button(new Icon(VaadinIcon.MINUS), (e) -> {
            dialog.setWidth("50%");
            dialog.setHeight("100%");
        });
        Button fullScreenButton = new Button(new Icon(VaadinIcon.EXPAND_FULL), (e) -> dialog.setSizeFull());
        Button closeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL), (e) -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        closeButton.setTooltipText("Close the report");

        dialog.getHeader().add(minimizeButton, fullScreenButton, closeButton);

        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
        dialog.setModal(false);
        dialog.setDraggable(true);
        dialog.setResizable(true);

        dialog.setWidth("50%");
        dialog.setHeight("100%");

        try {
            HashMap<String, Object> parameters = collectParametersOrThrow();

            VerticalLayout dialogLayout = new VerticalLayout();
            dialogLayout.setSizeFull();

            Button downloadButtonELSX = new Button("XLSX", VaadinIcon.FILE_TABLE.create());
            downloadButtonELSX.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            downloadButtonELSX.getElement().setProperty("title", "Download XLSX");

            Button downloadButton = new Button("PDF", VaadinIcon.FILE_PRESENTATION.create());
            downloadButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            downloadButton.getElement().setProperty("title", "Download PDF");

            if ("Simple".equalsIgnoreCase(reportQuery.getReportType())) {
                Grid<Map<String, Object>> gridReport = createGridFromQuery(reportQuery.getQuery(), parameters);
                gridReport.setSizeFull();
                gridReport.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_WRAP_CELL_CONTENT);
                gridReport.getStyle().set("flex-grow", "1");

                dialogLayout.setSizeFull();
                dialogLayout.setPadding(false);
                dialogLayout.setSpacing(false);
                dialogLayout.setAlignItems(Alignment.STRETCH);
                dialogLayout.setJustifyContentMode(JustifyContentMode.START);
                dialogLayout.setFlexGrow(1, gridReport);
                dialogLayout.add(gridReport);

                String renderedQuery = renderQueryWithParams(reportQuery.getQuery(), parameters);

                TextArea sqlArea = new TextArea();
                sqlArea.setValue(renderedQuery);
                sqlArea.setReadOnly(true);
                sqlArea.setWidthFull();
                sqlArea.getStyle().set("font-family", "monospace").set("font-size", "12px");

                Button copyButton = new Button("Copy SQL", new Icon(VaadinIcon.COPY));
                copyButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
                copyButton.addClickListener(e -> sqlArea.getElement().executeJs(
                    "navigator.clipboard.writeText($0).then(() => $1.textContent = 'Copied!')",
                    sqlArea.getValue(),
                    e.getSource().getElement()
                ));

                HorizontalLayout sqlToolbar = new HorizontalLayout(copyButton);
                sqlToolbar.setPadding(false);
                sqlToolbar.setSpacing(true);

                VerticalLayout sqlPanel = new VerticalLayout(sqlToolbar, sqlArea);
                sqlPanel.setPadding(false);
                sqlPanel.setSpacing(false);
                sqlPanel.setWidthFull();

                Accordion accordion = new Accordion();
                accordion.getStyle().set("align-self", "flex-start");
                accordion.setWidthFull();
                accordion.add("SQL", sqlPanel);
                accordion.close();
                
      	      	if (authenticatedUser.hasPage(ReportsView.class, AccessPageType.UPDATED_PAGE)) {
      	      		dialogLayout.add(accordion);
      	      	}
                downloadButtonELSX.addClickListener(e -> {
                    List<Map<String, Object>> data = fetchQueryData(reportQuery.getQuery(), parameters);
                    exportToExcel(data, reportQuery.getReportName());
                });

            } else {
               // byte[] pdfReport = ReportService.generateReport(reportQuery.getReportPath(), parameters);
            	Path tmpReportPath = baseDir.resolve(reportQuery.getId()+"/"+ reportQuery.getReportPath());
            	byte[] pdfReport = ReportService.generateReportExternal(tmpReportPath, parameters,this.baseAttachmentsDir);
                String pdfBase64 = java.util.Base64.getEncoder().encodeToString(pdfReport);

                IFrame pdfIframe = new IFrame();
                pdfIframe.setSrc("data:application/pdf;base64," + pdfBase64);
                pdfIframe.setSizeFull();

                downloadButton.addClickListener(downloadEvent -> {
                    String downloadUrl = "data:application/pdf;base64," + pdfBase64;
                    Anchor downloadLink = new Anchor(downloadUrl, "Download PDF");
                    downloadLink.getElement().setAttribute("download", "report.pdf");
                    UI ui = UI.getCurrent();
                    ui.add(downloadLink);
                    downloadLink.getElement().executeJs("this.click();").then(result -> {
                        ui.remove(downloadLink);
                        dialog.close();
                    });
                });

                dialogLayout.add(pdfIframe);
            }

            dialogLayout.setSizeFull();
            dialogLayout.setPadding(false);
            dialogLayout.setAlignItems(Alignment.CENTER);

            dialog.add(dialogLayout);
            dialog.getFooter().add(downloadButtonELSX);
            dialog.open();

        } catch (JRException e) {
            e.printStackTrace();
            
            Div errorMessage = new Div();
            errorMessage.setText("Failed to generate report: " + e.getMessage());
            errorMessage.getStyle().setColor("red");
            dialog.add(errorMessage);
            dialog.open();
            
        } catch (IllegalStateException e) {
        	e.printStackTrace();
            this.showError(e.getMessage());

        } catch (RuntimeException e) {
            e.printStackTrace();
            Div errorMessage = new Div();
            errorMessage.setText("Failed to load UI context: " + e.getMessage());
            errorMessage.getStyle().setColor("red");
            dialog.add(errorMessage);
            dialog.open();
        }
    }

    /* =========================
     *  Shared parameter collector
     * ========================= */
    @SuppressWarnings("unchecked")
    private HashMap<String, Object> collectParametersOrThrow() {
        HashMap<String, Object> parameters = new HashMap<>();
        boolean valid = true;

        for (Map.Entry<String, Component> entry : paramComponentMap.entrySet()) {
            String paramName = entry.getKey();
            Component component = entry.getValue();

            if (component instanceof HasValidationProperties hvp) {
                hvp.setInvalid(false);
            }

            if (component instanceof TextField textField) {
                if (textField.isRequired() && (textField.getValue() == null || textField.getValue().isBlank())) {
                    textField.setInvalid(true);
                    textField.setErrorMessage("Required field missing");
                    valid = false;
                } else {
                    parameters.put(paramName, textField.getValue());
                }

            } else if (component instanceof NumberField numberField) {
                if (numberField.isRequired() && numberField.getValue() == null) {
                    numberField.setInvalid(true);
                    numberField.setErrorMessage("Required field missing");
                    valid = false;
                } else {
                    parameters.put(paramName, numberField.getValue());
                }

            } else if (component instanceof DatePicker datePicker) {
                if (datePicker.isRequired() && datePicker.getValue() == null) {
                    datePicker.setInvalid(true);
                    datePicker.setErrorMessage("Required field missing");
                    valid = false;
                } else if (datePicker.getValue() != null) {
                    LocalDate value = datePicker.getValue();
                    java.util.Date date = java.sql.Date.valueOf(value);
                    parameters.put(paramName, date);
                } else {
                    parameters.put(paramName, null);
                }

            } else if (component instanceof ComboBox<?> comboBox) {
                if (comboBox.isRequired() && comboBox.getValue() == null) {
                    comboBox.setInvalid(true);
                    comboBox.setErrorMessage("Required field missing");
                    valid = false;
                } else {
                    //Object value = comboBox.getValue();
                    //parameters.put(paramName, value != null ? value.toString() : null);
                    LookupValue selectedValues = (LookupValue) comboBox.getValue();
                    parameters.put(paramName, selectedValues.getId());
                }

            } else if (component instanceof MultiSelectComboBox<?> multiSelectComboBox) {
                if (multiSelectComboBox.isRequired() && multiSelectComboBox.getValue().isEmpty()) {
                    multiSelectComboBox.setInvalid(true);
                    multiSelectComboBox.setErrorMessage("Please select at least one option!");
                    valid = false;
                } else {
                    Set<LookupValue> selectedValues = (Set<LookupValue>) multiSelectComboBox.getValue();
                    List<String> selectedIds = selectedValues.stream().map(LookupValue::getId).toList();
                    parameters.put(paramName, selectedIds);
                }
            }
        }

        if (!valid) {
            throw new IllegalStateException("Please fill all required fields");
        }
        return parameters;
    }

    /* Render SQL with parameters (for display only) */
    private String renderQueryWithParams(String query, Map<String, Object> parameters) {
        String rendered = query;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String replacement;

            if (value == null) {
                replacement = "NULL";
            } else if (value instanceof String || value instanceof java.util.Date) {
                replacement = "'" + value.toString() + "'";
            } else if (value instanceof List<?> list) {
                replacement = "(" + list.stream()
                    .map(Object::toString)
                    .map(s -> "'" + s + "'")
                    .collect(Collectors.joining(", ")) + ")";
            } else {
                replacement = String.valueOf(value);
            }

            rendered = rendered.replaceAll(":" + key + "\\b", replacement);
        }
        return rendered;
    }

    /* =========================
     *  Data helpers
     * ========================= */
    private Grid<Map<String, Object>> createGridFromQuery(String query, Map<String, Object> params) {
        Grid<Map<String, Object>> grid = new Grid<>();
        grid.setHeight("300px");

        try {
            List<Map<String, Object>> rows = fetchQueryData(query, params);
            if (!rows.isEmpty()) {
                Map<String, Object> firstRow = rows.get(0);
                for (String col : firstRow.keySet()) {
                    grid.addColumn(map -> String.valueOf(map.getOrDefault(col, ""))).setHeader(col);
                }
            }

            grid.getColumns().forEach(column -> {
                column.setResizable(true);
                column.setSortable(true);
                column.setAutoWidth(true);
            });

            grid.setItems(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return grid;
    }
    

    
    private List<Map<String, Object>> fetchQueryData(String query, Map<String, Object> params) {
        try {
            return namedJdbcTemplate.query(query, params, (rs, rowNum) -> {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    rowMap.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                return rowMap;
            });
        } catch (Exception e) {
        	
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /* =========================
     *  Dependent lookups
     * ========================= */
    private void comboBoxChangeValue(ReportQueryParamater parentParam, LookupValue changeValue) {
        Specification<ReportQueryParamater> spec = (root, query, cb) ->
            cb.and(
                cb.equal(root.get("parentParamaterName"), parentParam.getParamaterName()),
                cb.isNotNull(root.get("parentParamaterName")),
                cb.in(root.get("paramaterType")).value("ComboBox").value("MultiSelectComboBox")
            );

        List<ReportQueryParamater> linkedParameters = reportQueryParamaterRepository.findAll(spec);

        if (linkedParameters != null && !linkedParameters.isEmpty()) {
            for (ReportQueryParamater paramLink : linkedParameters) {
                for (Map.Entry<String, Component> entry : paramComponentMap.entrySet()) {
                    String paramName = entry.getKey();
                    Component component = entry.getValue();

                    if (paramName.equalsIgnoreCase(paramLink.getParamaterName()) && component instanceof ComboBox<?> comboBox) {
                        if (changeValue != null) {
                            List<LookupValue> filteredValues =
                                lookupService.getValues(paramLink.getParamaterLookup(), changeValue.getId());
                            ((ComboBox<LookupValue>) comboBox).setItems(filteredValues);
                        } else {
                            ((ComboBox<LookupValue>) comboBox).setItems(Collections.emptyList());
                        }
                    } else if (paramName.equalsIgnoreCase(paramLink.getParamaterName()) && component instanceof MultiSelectComboBox<?> multi) {
                        if (changeValue != null) {
                            List<LookupValue> filteredValues =
                                lookupService.getValues(paramLink.getParamaterLookup(), changeValue.getId());
                            ((MultiSelectComboBox<LookupValue>) multi).setItems(filteredValues);
                        } else {
                            ((MultiSelectComboBox<LookupValue>) multi).setItems(Collections.emptyList());
                        }
                    }
                }
            }
        }
    }

    /* =========================
     *  Excel export
     * ========================= */
    private void exportToExcel(List<Map<String, Object>> data, String fileName) {
        // Defensive: limit extreme exports (optional but recommended)
        final int MAX_ROWS = 1_000_000; // Excel limit is 1,048,576
        if (data.size() > MAX_ROWS) {
            showError("Too many rows to export. Please filter your data.");
            return;
        }

        // Prepare file name
        String base = (fileName == null || fileName.isBlank()) ? "report" : fileName;
        String finalName = base + ".xlsx";

        // Create a streaming workbook: row window ~ 500 rows in memory at a time
        // Constructor: (XSSFWorkbook wb, int rowAccessWindowSize, boolean compressTmpFiles, boolean useSharedStringsTable)
        org.apache.poi.xssf.streaming.SXSSFWorkbook wb =
                new org.apache.poi.xssf.streaming.SXSSFWorkbook(null, 500, true, false);
        wb.setCompressTempFiles(true);

        java.nio.file.Path temp;
        try {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Data");

            // Write header + rows
            if (!data.isEmpty()) {
                Map<String, Object> firstRow = data.get(0);
                java.util.List<String> columns = new java.util.ArrayList<>(firstRow.keySet());

                org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                for (int c = 0; c < columns.size(); c++) {
                    headerRow.createCell(c).setCellValue(columns.get(c));
                }

                int r = 1;
                for (Map<String, Object> rowData : data) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                    for (int c = 0; c < columns.size(); c++) {
                        Object v = rowData.get(columns.get(c));
                        org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                        if (v instanceof Number num) {
                            cell.setCellValue(num.doubleValue());
                        } else if (v instanceof java.util.Date d) {
                            cell.setCellValue(d);
                        } else if (v != null) {
                            cell.setCellValue(v.toString());
                        } else {
                            cell.setBlank();
                        }
                    }
                }
            }

            // Write to a temp file on disk (not heap)
            temp = java.nio.file.Files.createTempFile("export-", ".xlsx");
            try (java.io.OutputStream fos = java.nio.file.Files.newOutputStream(temp)) {
                wb.write(fos);
            }
            // Best-effort cleanup if JVM exits; (can't know exact download completion)
            temp.toFile().deleteOnExit();

        } catch (Exception e) {
            showError("Failed to export Excel: " + e.getMessage());
            e.printStackTrace();
            return;
        } finally {
            try { wb.dispose(); } catch (Throwable ignore) {}
            try { wb.close(); } catch (Throwable ignore) {}
        }

        // ---- Vaadin 24.8+ download (no StreamResource) ----
        com.vaadin.flow.server.streams.DownloadHandler handler =
            com.vaadin.flow.server.streams.DownloadHandler.fromInputStream(event -> {
                try {
                    long size = java.nio.file.Files.size(temp);
                    java.io.InputStream is = java.nio.file.Files.newInputStream(temp);
                    return new com.vaadin.flow.server.streams.DownloadResponse(
                        is,
                        finalName,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        size
                    );
                } catch (java.io.IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            });

        // Hidden anchor to trigger download
        if (excelDownloadLink == null) {
            excelDownloadLink = new Anchor(handler, "");
            excelDownloadLink.getStyle().set("display", "none");
            UI.getCurrent().add(excelDownloadLink);
        } else {
            // Reuse existing anchor
            excelDownloadLink.setHref(handler);
        }
        // HTML download attribute for suggested filename
        excelDownloadLink.getElement().setAttribute("download", finalName);

        // Trigger click
        excelDownloadLink.getElement().executeJs("this.click()");
    }


    /* =========================
     *  Lifecycle / UI wiring
     * ========================= */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(ReportsView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(org.halocambodia.views.access_denied.AccessDeniedView.class);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        try {
            UI.getCurrent().access(() -> {
                try {
                    UI.getCurrent().getPage().executeJs("""
                        if (!document.querySelector('style[data-reports-view]')) {
                          const s = document.createElement('style');
                          s.setAttribute('data-reports-view','');
                          s.textContent = `
                            .card-hover:hover {
                              transform: translateY(-3px);
                              background-color: var(--lumo-primary-color-10pct);
                              box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                              transition: all 0.2s ease;
                              cursor: pointer;
                            }
                          `;
                          document.head.appendChild(s);
                        }
                    """);
                    configureEditorLayout();
                    binderField();
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

    private void popupForm(ReportQuery entity) {
        try {
            // Always start from a fresh copy if we have an id
            if (entity != null && entity.getId() != null) {
                this.entity = service.loadFresh(entity.getId()); // uses the method above
            } else {
                this.entity = (entity == null ? new ReportQuery() : entity);
            }

            preLoadFormData();
            binder.readBean(this.entity);

            String title = (this.entity.getId() == null) ? "Create new simple report" : "Edit simple report";
            createSimpleReportDialog.setHeaderTitle(title);

            // Bind the same backing list instance to the grid
            paramItems = (this.entity.getReportQueryParamaterList() != null)
                    ? this.entity.getReportQueryParamaterList()
                    : new ArrayList<>();
            this.entity.setReportQueryParamaterList(paramItems);

            // (Optional) enforce order
            paramItems.sort(Comparator.comparingInt(p -> p.getParamaterSortOrder() == null ? 0 : p.getParamaterSortOrder()));

            gridParamater.setItems(paramItems);

            createSimpleReportDialog.open();
        } catch (Exception e) {
            e.printStackTrace();
            this.showError(e.getMessage());
        }
    }


    private void configureEditorLayout() throws Exception {
        FormLayout frmLayout = new FormLayout();

        notes.setTooltipText("These notes will be displayed in the report heading. " +
            "It can be useful to describe the methodology and any calculations used in the reports.");

        frmLayout.add(reportName,reportGroupName, reportType, query,reportPath,upload,roles,  sortOrder, notes);
        frmLayout.setColspan(reportName, 2);
        frmLayout.setColspan(query, 2);
        frmLayout.setColspan(notes, 2);
        frmLayout.setColspan(upload, 2);
        
        reportPath.setPlaceholder("e.g. donor.jasper");
        query.setVisible(false);
        reportPath.setVisible(false);
        upload.setVisible(false);
        reportType.addValueChangeListener(e->reportTypeChange());
        

        paramaterLayout = new VerticalLayout(
            new Span("Parameters are entered by the user. Parameter names:\n"
            		+ "\n"
            		+ "Must start with a letter or an underscore (_).\n"
            		+ "\n"
            		+ "May contain letters, digits, underscores (_), and dollar signs ($), but no spaces.\n"
            		+ "\n"
            		+ "Must not be a Java keyword."),
            new Button("Add parameter", e -> this.addParamater()),
            gridParamater
        );
        initGridParamater();

        SplitLayout splitLayout = new SplitLayout(frmLayout, paramaterLayout);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(50);

        createSimpleReportDialog.add(splitLayout);
        createSimpleReportDialog.setSizeFull();

        reportGroupName.setAllowCustomValue(true);
        reportGroupName.addCustomValueSetListener(event -> {
            String customValue = event.getDetail();
            reportGroupName.setItems(Stream.concat(
                reportGroupName.getListDataView().getItems(),
                Stream.of(customValue)
            ).toList());
            reportGroupName.setValue(customValue);
        });

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");

        Button btnCancel = new Button("Cancel | បោះបង់", e -> { closeForm(); clearForm(); });
        btnCancel.setIcon(new Icon(VaadinIcon.CLOSE));
        btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        btnCancel.addClickShortcut(Key.ESCAPE);

        Button btnSave = new Button("Save | រក្សាទុក", e -> save());
        btnSave.setIcon(new Icon(VaadinIcon.CHECK));
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnSave.addClickShortcut(Key.ENTER);

        buttonLayout.add(btnSave, btnCancel);
        createSimpleReportDialog.getFooter().add(buttonLayout);
        try {
            initUpload();
        } catch (Exception ex) {
            showError("Cannot initialize upload: " + ex.getMessage());
        } 

    }
    // ---------------- Disk streaming upload ----------------
    private void initUpload() throws Exception {
        baseDir = fileUploadUtility.initializeUploadDirectory("hr", simpleReportsDir);
        tempUploadDir = baseDir.resolve("tmp");
        Files.createDirectories(tempUploadDir);

        // Create a handler that writes the upload to a temp file and then lets you move it.
        UploadHandler diskHandler = UploadHandler.toTempFile((metadata, tempFile) -> {
            try {
                String safeName = sanitizeFilename(metadata.fileName());
                Path target = uniquePath(tempUploadDir, safeName); // no checked throws now
                Files.move(tempFile.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                stagedFiles.add(target);
            } catch (java.io.IOException ex) {
                throw new RuntimeException("Failed to store upload: " + ex.getMessage(), ex);
            }
        });
        
        upload.setUploadHandler(diskHandler);
        upload.setDropAllowed(true);
        //upload.setMaxFiles(10);
        //upload.setAcceptedFileTypes(".jasper", ".jrxml");
    }

    private static String sanitizeFilename(String name) {
        String base = Path.of(name).getFileName().toString();
        return base.replaceAll("[\\\\/:*?\"<>|]+", "_");
    }

    private static Path uniquePath(Path dir, String fileName) {
        Path p = dir.resolve(fileName);
        if (!Files.exists(p)) return p;

        String name = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) { name = fileName.substring(0, dot); ext = fileName.substring(dot); }
        for (int i = 1; ; i++) {
            Path cand = dir.resolve(name + " (" + i + ")" + ext);
            if (!Files.exists(cand)) return cand;
        }
    }

    private void initGridParamater() {
        // ===== Columns (no Sort column) =====
        gridParamater.addComponentColumn(item -> {
            Icon grip = VaadinIcon.MENU.create();
            grip.getStyle().set("cursor", "move").set("opacity", "0.7");
            return grip;
        }).setHeader("").setFlexGrow(0).setAutoWidth(true);

        Grid.Column<ReportQueryParamater> colLabel = gridParamater
            .addColumn(ReportQueryParamater::getParamaterLabel)
            .setHeader("Label for users")
            .setKey("paramaterLabel")
            .setAutoWidth(true).setResizable(true);

        Grid.Column<ReportQueryParamater> colName = gridParamater
            .addColumn(ReportQueryParamater::getParamaterName)
            .setHeader("Paramater name")
            .setKey("paramaterName")
            .setAutoWidth(true).setResizable(true);

        Grid.Column<ReportQueryParamater> colType = gridParamater
            .addColumn(ReportQueryParamater::getParamaterType)
            .setHeader("Component").setAutoWidth(true).setResizable(true);
        
        Grid.Column<ReportQueryParamater> colParentParamaterName = gridParamater
                .addColumn(ReportQueryParamater::getParentParamaterName)
                .setHeader("Parent Paramater").setAutoWidth(true).setResizable(true);

        Grid.Column<ReportQueryParamater> colLookup = gridParamater
            .addColumn(ReportQueryParamater::getParamaterLookup)
            .setHeader("Lookup (view)").setAutoWidth(true).setResizable(true);

        Grid.Column<ReportQueryParamater> colRequired = gridParamater
            .addColumn(p -> Boolean.TRUE.equals(p.getRequiredField()) ? "Yes" : "No")
            .setHeader("Required").setAutoWidth(true).setResizable(true);



        // ===== Inline editor wiring =====
        editor.setBinder(editorBinder);
        editor.setBuffered(true);

        // Helper
        final java.util.function.Predicate<String> comboLike =
            t -> "ComboBox".equalsIgnoreCase(t) || "MultiSelectComboBox".equalsIgnoreCase(t);

        final java.util.Set<String> JAVA_KEYWORDS = java.util.Set.of(
            "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue",
            "default","do","double","else","enum","extends","final","finally","float","for","goto","if",
            "implements","import","instanceof","int","interface","long","native","new","package","private",
            "protected","public","return","short","static","strictfp","super","switch","synchronized","this",
            "throw","throws","transient","try","void","volatile","while","record","sealed","permits","var"
        );

        // Editor components
        TextField tfLabel = new TextField();
        tfLabel.setPlaceholder("e.g. Contract");
        tfLabel.setRequiredIndicatorVisible(true);
        colLabel.setEditorComponent(tfLabel);

        TextField tfName = new TextField();
        tfName.setPlaceholder("e.g. CONTRACT_ID");
        tfName.setRequiredIndicatorVisible(true);
        colName.setEditorComponent(tfName);

        ComboBox<String> cbType = new ComboBox<>();
        cbType.setItems("TextField", "NumberField", "DatePicker", "ComboBox", "MultiSelectComboBox");
        cbType.setAllowCustomValue(false);
        cbType.setRequiredIndicatorVisible(true);
        colType.setEditorComponent(cbType);

        // Lookup view (schema.view_name). Replaces TextArea with ComboBox + suggestions.
        ComboBox<String> cbLookupView = new ComboBox<>();
        cbLookupView.setWidthFull();
        cbLookupView.setAllowCustomValue(true);
        cbLookupView.setClearButtonVisible(true);
        cbLookupView.setPlaceholder("schema.view_name (defaults to public)");
        cbLookupView.addCustomValueSetListener(ev -> cbLookupView.setValue(ev.getDetail()));
        cbLookupView.setRequiredIndicatorVisible(false); // toggled by type
        colLookup.setEditorComponent(cbLookupView);

        Checkbox cbRequired = new Checkbox();
        colRequired.setEditorComponent(cbRequired);

        TextField tfParentParamater = new TextField();
        colParentParamaterName.setEditorComponent(tfParentParamater);

        // ---- Bindings + VALIDATION ----
        editorBinder.forField(tfLabel)
            .asRequired("Label is required")
            .withValidator(v -> v == null || !v.trim().isEmpty(), "Label is required")
            .bind(ReportQueryParamater::getParamaterLabel, ReportQueryParamater::setParamaterLabel);

        editorBinder.forField(tfName)
            .asRequired("Parameter name is required")
            .withValidator(v -> v != null && v.matches("^[A-Za-z_][A-Za-z0-9_\\$]*$"),
                "Must start with a letter/_ and contain letters, digits, _ or $")
            .withValidator(v -> v == null || !JAVA_KEYWORDS.contains(v),
                "Must not be a Java keyword")
            .withValidator(v -> {
                if (v == null || paramItems == null) return true;
                ReportQueryParamater current = editor.getItem();
                return paramItems.stream()
                    .filter(p -> p != current)
                    .noneMatch(p -> v.equalsIgnoreCase(p.getParamaterName()));
            }, "Parameter name must be unique")
            .bind(ReportQueryParamater::getParamaterName, ReportQueryParamater::setParamaterName);

        editorBinder.forField(cbType)
            .asRequired("Component type is required")
            .bind(ReportQueryParamater::getParamaterType, ReportQueryParamater::setParamaterType);

        // Lookup: required for ComboBox/MultiSelectComboBox and must be a valid view with the required columns
        editorBinder.forField(cbLookupView)
            .withValidator(v -> {
                String t = cbType.getValue();
                boolean needsLookup = comboLike.test(t);
                if (!needsLookup) return true;               // not required
                if (v == null || v.isBlank()) return false;  // required

                String parentName = tfParentParamater.getValue();
                boolean hasParent = parentName != null && !parentName.isBlank();
                try {
                    return validateLookupView(v, hasParent);
                } catch (Exception ex) {
                    return false; // conservative
                }
            }, "Lookup must be an existing view with columns lookup_id, lookup_description"
               + " (and parent_lookup_id if this parameter has a parent)")
            .bind(ReportQueryParamater::getParamaterLookup, ReportQueryParamater::setParamaterLookup);

        editorBinder.forField(cbRequired)
            .bind(ReportQueryParamater::getRequiredField, ReportQueryParamater::setRequiredField);

        editorBinder.forField(tfParentParamater)
            .bind(ReportQueryParamater::getParentParamaterName, ReportQueryParamater::setParentParamaterName);

        // Toggle required indicator + refresh suggestions on type change
        cbType.addValueChangeListener(e -> {
            boolean need = comboLike.test(e.getValue());
            cbLookupView.setRequiredIndicatorVisible(need);
            if (need) {
                boolean needParent = tfParentParamater.getValue() != null && !tfParentParamater.getValue().isBlank();
                cbLookupView.setItems(findLookupViews(needParent));
            } else {
                cbLookupView.clear();
                cbLookupView.setItems();
            }
            editorBinder.validate();
        });

        // If parent changes, suggestions may need parent_lookup_id
        tfParentParamater.addValueChangeListener(e -> {
            boolean need = comboLike.test(cbType.getValue());
            if (need) {
                boolean needParent = e.getValue() != null && !e.getValue().isBlank();
                cbLookupView.setItems(findLookupViews(needParent));
            }
            editorBinder.validate();
        });

        // ===== Actions column =====
        gridParamater.addComponentColumn(item -> {
            boolean isEditing = editor.isOpen() && Objects.equals(editor.getItem(), item);

            Icon editIcon = VaadinIcon.PENCIL.create();
            Icon saveIcon = VaadinIcon.CHECK.create();
            Button editSave = new Button(isEditing ? saveIcon : editIcon);
            editSave.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            editSave.getElement().setProperty("title", isEditing ? "Save" : "Edit");
            editSave.addClickListener(e -> {
                if (isEditing) {
                    editor.save(); // will only save if validation passes
                } else {
                    if (editor.isOpen()) editor.cancel();
                    editor.editItem(item);
                    // init required-indicator & suggestions for current row
                    boolean need = comboLike.test(item.getParamaterType());
                    cbLookupView.setRequiredIndicatorVisible(need);
                    if (need) {
                        boolean needParent = item.getParentParamaterName() != null
                                && !item.getParentParamaterName().isBlank();
                        cbLookupView.setItems(findLookupViews(needParent));
                    } else {
                        cbLookupView.setItems();
                    }
                }
            });

            Button remove = new Button(new Icon(VaadinIcon.TRASH));
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
            remove.getElement().setProperty("title", "Remove");
            remove.addClickListener(e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Remove parameter?");
                dialog.setText("This will permanently remove \"" +
                    (item.getParamaterLabel() == null || item.getParamaterLabel().isBlank()
                        ? "(unnamed parameter)" : item.getParamaterLabel()) + "\".");
                dialog.setCancelable(true);
                dialog.setConfirmText("Remove");
                dialog.setConfirmButtonTheme("error primary");
                dialog.addConfirmListener(ev -> {
                    if (editor.isOpen() && Objects.equals(editor.getItem(), item)) editor.cancel();
                    deleteParameterRow(item);
                });
                dialog.open();
            });

            HorizontalLayout actions = new HorizontalLayout(editSave, remove);
            actions.setPadding(false);
            actions.setSpacing(true);
            actions.setWidthFull();
            actions.setJustifyContentMode(JustifyContentMode.CENTER);
            actions.setAlignItems(Alignment.CENTER);
            actions.getStyle().set("min-width", "0");
            return actions;
        })
        .setHeader("Action")
        .setResizable(false)
        .setFlexGrow(0)
        .setAutoWidth(false)
        .setTextAlign(ColumnTextAlign.CENTER);

        // Double-click to edit (also seed suggestions)
        gridParamater.addItemDoubleClickListener(ev -> {
            if (editor.isOpen()) editor.cancel();
            editor.editItem(ev.getItem());
            boolean need = comboLike.test(ev.getItem().getParamaterType());
            cbLookupView.setRequiredIndicatorVisible(need);
            if (need) {
                boolean needParent = ev.getItem().getParentParamaterName() != null
                        && !ev.getItem().getParentParamaterName().isBlank();
                cbLookupView.setItems(findLookupViews(needParent));
            } else {
                cbLookupView.setItems();
            }
        });

        editor.addSaveListener(e -> gridParamater.getDataProvider().refreshItem(e.getItem()));

        // ===== Drag & Drop (Vaadin 24) =====
        gridParamater.setRowsDraggable(true);
        gridParamater.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        gridParamater.getElement().getStyle()
            .set("--vaadin-grid-dragover-background", "var(--lumo-primary-color-10pct)");

        gridParamater.addDragStartListener(e -> {
            if (editor.isOpen()) editor.cancel();
            draggingParam = e.getDraggedItems().isEmpty() ? null : e.getDraggedItems().get(0);
        });
        gridParamater.addDragEndListener(e -> draggingParam = null);

        gridParamater.addDropListener(event -> {
            if (draggingParam == null) return;

            var targetOpt = event.getDropTargetItem();
            if (targetOpt.isEmpty()) return;

            ReportQueryParamater target = targetOpt.get();
            if (draggingParam == target) return;
            if (paramItems == null) return;

            int fromIndex = paramItems.indexOf(draggingParam);
            int targetIndex = paramItems.indexOf(target);
            if (fromIndex < 0 || targetIndex < 0) return;

            if (event.getDropLocation() == GridDropLocation.BELOW) targetIndex++;

            paramItems.remove(fromIndex);
            if (targetIndex > paramItems.size()) targetIndex = paramItems.size();
            paramItems.add(targetIndex, draggingParam);

            for (int i = 0; i < paramItems.size(); i++) {
                paramItems.get(i).setParamaterSortOrder(i + 1);
            }

            gridParamater.getDataProvider().refreshAll();
        });
    }

    /** Parse "schema.table" or fallback to public. */
    private String[] parseSchemaAndTable(String raw) {
        String s = raw.trim();
        int dot = s.indexOf('.');
        if (dot > 0 && dot < s.length() - 1) {
            return new String[]{ s.substring(0, dot), s.substring(dot + 1) };
        }
        return new String[]{ "public", s };
    }

    /** Validate that a view exists and has required columns.
     *  If needParent==true, also require parent_lookup_id.
     */
    private boolean validateLookupView(String viewName, boolean needParent) {
        if (viewName == null || viewName.isBlank()) return false;

        String[] st = parseSchemaAndTable(viewName);
        String schema = st[0], table = st[1];

        // Pull all column names for the view
        String sql =
            "SELECT column_name " +
            "FROM information_schema.columns " +
            "WHERE table_schema = ? AND table_name = ?";

        Set<String> cols = new HashSet<>(jdbcTemplate.query(sql, ps -> {
            ps.setString(1, schema);
            ps.setString(2, table);
        }, (rs, rowNum) -> rs.getString("column_name").toLowerCase(Locale.ROOT)));

        if (!cols.contains("lookup_id")) return false;
        if (!cols.contains("lookup_description")) return false;
        if (needParent && !cols.contains("parent_lookup_id")) return false;

        // Ensure it's a VIEW (not a table)
        Integer isView = jdbcTemplate.query(
            "SELECT 1 FROM information_schema.views WHERE table_schema=? AND table_name=?",
            ps -> { ps.setString(1, schema); ps.setString(2, table); },
            rs -> rs.next() ? 1 : 0
        );
        return isView != null && isView == 1;
    }

    /** Suggest views having the required columns. */
    private List<String> findLookupViews(boolean needParent) {
        String sql =
            "SELECT c.table_schema, c.table_name, " +
            "       COUNT(DISTINCT CASE WHEN c.column_name IN ('lookup_id','lookup_description' " +
                     (needParent ? ",'parent_lookup_id'" : "") +
            "       ) THEN c.column_name END) AS matched " +
            "FROM information_schema.columns c " +
            "JOIN information_schema.views v " +
            "  ON v.table_schema = c.table_schema AND v.table_name = c.table_name " +
            "WHERE c.table_schema NOT IN ('pg_catalog','information_schema') " +
            "GROUP BY c.table_schema, c.table_name " +
            "HAVING COUNT(DISTINCT CASE WHEN c.column_name IN ('lookup_id','lookup_description' " +
                     (needParent ? ",'parent_lookup_id'" : "") +
            "       ) THEN c.column_name END) = " + (needParent ? "3" : "2") + " " +
            "ORDER BY c.table_schema, c.table_name";

        return jdbcTemplate.query(sql, rs -> {
            List<String> out = new ArrayList<>();
            while (rs.next()) {
                out.add(rs.getString("table_schema") + "." + rs.getString("table_name"));
            }
            return out;
        });
    }



    private void deleteParameterRow(ReportQueryParamater item) {
        try {
            // Close inline editor if it's on this row
            if (editor.isOpen() && Objects.equals(editor.getItem(), item)) {
                editor.cancel();
            }

            if (entity != null && entity.getReportQueryParamaterList() != null) {
                List<ReportQueryParamater> list = entity.getReportQueryParamaterList();
                Long id = item.getId();

                if (id != null) {
                    // Remove exactly the one with this id (not all nulls!)
                    // Using iterator + break for safety/clarity
                    for (Iterator<ReportQueryParamater> it = list.iterator(); it.hasNext();) {
                        ReportQueryParamater p = it.next();
                        if (Objects.equals(p.getId(), id)) {
                            it.remove();
                            break;
                        }
                    }
                } else {
                    // Unsaved row (id == null): remove by object identity
                    // (Grid uses same instances if you keep the same backing list)
                    list.remove(item);
                }

                // Break association (helps with orphanRemoval setups)
                item.setReportQuery(null);

                // Resequence 1..N
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setParamaterSortOrder(i + 1);
                }
            }

            // Refresh UI
            gridParamater.getSelectionModel().deselectAll();
            gridParamater.getDataProvider().refreshAll();

            //Notification.show("Parameter deleted", 2000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception ex) {
            Notification.show("Failed to delete: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }



    private void addParamater() {
        try {
            if (entity == null) {
                showError("Please create the report first.");
                return;
            }
            if (entity.getReportQueryParamaterList() == null) {
                entity.setReportQueryParamaterList(new ArrayList<>());
            }

            ReportQueryParamater newParam = new ReportQueryParamater();
            newParam.setParamaterName("");
            newParam.setParamaterLabel("");
            newParam.setParamaterType("TextField");
            newParam.setRequiredField(true);
            newParam.setParamaterSortOrder(
                entity.getReportQueryParamaterList().size() + 1
            );

            entity.getReportQueryParamaterList().add(newParam);
            paramItems = entity.getReportQueryParamaterList(); // keep field in sync
            gridParamater.getDataProvider().refreshAll();
            gridParamater.getEditor().editItem(newParam);

        } catch (Exception e) {
            e.printStackTrace();
            showError(e.getMessage());
        }
    }

    private void clearForm() {
        binder.readBean(null);
        this.entity = null;
    }

    private void closeForm() {
    	cleanupStagedFilesQuietly();
        createSimpleReportDialog.close();
    }

    private void binderField() {
        binder.bindInstanceFields(this);
    }
    private static String slugify(String s) {
        if (s == null) return null;
        String out = s.trim().toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
            .replaceAll("(^-+|-+$)", "")
            .replaceAll("-{2,}", "-");
        return out.isEmpty() ? null : out;
    }
    private void save() {
        if (entity == null) { showError("No entity to save."); return; }
        try {
        	
        	
            // If the grid editor might be open, close/save it so binder has latest values
            if (editor.isOpen()) editor.save();
           
            binder.writeBean(entity);

            if (entity.getReportQueryParamaterList() != null) {
                for (ReportQueryParamater p : entity.getReportQueryParamaterList()) {
                    p.setReportQuery(entity); // <<< ensure FK is set
                }
            }

            entity = service.update(entity);
            
            // Commit files to a dated subfolder
            Path baseDir = fileUploadUtility.initializeUploadDirectory("hr", simpleReportsDir );
            Path commitDir = baseDir.resolve(entity.getId().toString());
            Files.createDirectories(commitDir);

            for (Path tmp : stagedFiles) {
                Path dest = commitDir.resolve(tmp.getFileName());
                Files.move(tmp, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                // TODO: if you store file paths in DB, attach here and save again
            }
            stagedFiles.clear();
            
            Notification.show("Data saved successfully | ទិន្នន័យបានរក្សាទុកដោយជោគជ័យ",
                1000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            clearForm();
            closeForm();
            
        } catch (Exception ex) {
            showError("Unexpected error: " + ex.getMessage());
        }
    }
    private void cleanupStagedFilesQuietly() {
        for (Path p : stagedFiles) {
            try { Files.deleteIfExists(p); } catch (Exception ignore) {}
        }
        stagedFiles.clear();
    }

    private void preLoadFormData() {
        this.reportGroupName.setItems(distinctGroupNames);
        this.roles.setItems(roleRepository.findAll());
        this.roles.setItemLabelGenerator(Role::getName);
    }

    private void confirmDelete(ReportQuery report, Component cardToRemove) {
    	
    	if (!authenticatedUser.hasPage(ReportsView.class, AccessPageType.DELETED_PAGE)) {           	          
      		UI.getCurrent().navigate(org.halocambodia.views.access_denied.AccessDeniedView.class);
 	        return;
 	    }
    	

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete report?");
        dialog.setText("This will permanently delete \"" + report.getReportName() + "\".");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(ev -> deleteReport(report, cardToRemove));
        dialog.open();
    }

    private void deleteReport(ReportQuery report, Component cardToRemove) {
        try {
            service.delete(Set.of(report));
            cardToRemove.getParent().ifPresent(parent -> {
                if (parent instanceof com.vaadin.flow.component.HasComponents hc) {
                    hc.remove(cardToRemove);
                } else {
                    cardToRemove.getElement().removeFromParent();
                }
            });
            Notification.show("Report deleted.", 3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (DataAccessException ex) {
            showError("Database error while deleting: " + ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to delete: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        Notification.show(message, 9000, Position.TOP_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    private void add() {
    	try {
        	if (!authenticatedUser.hasPage(ReportsView.class, AccessPageType.INSERTED_PAGE)) {           	          
   	          //showError("You don't have permission to perform this operation");
        		UI.getCurrent().navigate(org.halocambodia.views.access_denied.AccessDeniedView.class);
   	          return;
   	      	}
            this.entity = (this.entity == null ? new ReportQuery() : entity); popupForm(entity);
		} catch (Exception e) {
			 showError(e.getMessage());
			 e.printStackTrace();
		}
    }
    private boolean canRunReport(Optional<User> userOpt, ReportQuery report) {
        if (userOpt.isEmpty()) return false;

        Set<Role> reportRoles = Optional.ofNullable(report.getRoles()).orElse(Set.of());
        // ⛔️ Change here: empty = deny
        if (reportRoles.isEmpty()) return false;

        Set<Role> userRoles = Optional.ofNullable(userOpt.get().getRoles()).orElse(Set.of());

        // Compare by id (fallback to name if id is null)
        return userRoles.stream().anyMatch(ur ->
            reportRoles.stream().anyMatch(rr ->
                (rr.getId() != null && rr.getId().equals(ur.getId()))
                || (rr.getId() == null && ur.getId() == null
                    && Objects.equals(rr.getName(), ur.getName()))
            )
        );
    }
    private void reportTypeChange() {
        String v = reportType.getValue();                 // may be null
        boolean isSimple = "Simple".equalsIgnoreCase(v);  // safe: constant on the left
        boolean isJasper = "Jaspersoft".equalsIgnoreCase(v);

        query.setVisible(isSimple);
        reportPath.setVisible(isJasper);
        upload.setVisible(isJasper);
    }

}
