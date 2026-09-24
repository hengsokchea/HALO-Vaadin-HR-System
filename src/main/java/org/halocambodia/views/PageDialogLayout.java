package org.halocambodia.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.LocalDate;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.GazetteerMultiSearchField;
import org.halocambodia.component.ResponsiveMenuBar;
import org.halocambodia.component.PaginationControls;

import org.halocambodia.data.*;

import org.halocambodia.security.*;
import org.halocambodia.services.*;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadEvent;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;


import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@CssImport("./styles/menu-styles.css")
@CssImport("./styles/sql_bar.css")
public abstract class PageDialogLayout<T extends AbstractEntity, S extends GenericService<T>>
        extends VerticalLayout implements BeforeEnterObserver {
	
	protected enum SensitiveAction { EDIT, DELETE }

    protected enum FormMode { ADD, EDIT }

    private FormMode formMode = FormMode.ADD;
    private long lastTotalRecords = -1;

    protected T entity;
    protected final S service;
    protected final UserService userService;
    protected final AuthenticatedUser authenticatedUser;

    protected final Grid<T> grid = new Grid<>();
    private final PaginationControls paginationControls = new PaginationControls();

    protected final ResponsiveMenuBar leftMenu = new ResponsiveMenuBar();
    protected final ResponsiveMenuBar rightMenu = new ResponsiveMenuBar();

    // Left menu items
    protected MenuItem miAddNew;
    protected MenuItem miEdit;
    protected MenuItem miDelete;
    protected MenuItem miRefresh;

    // Right menu items
    protected MenuItem miAdvancedSearch;
    protected MenuItem miExportExcel;
 // Remember last grid sort orders so export matches what user sees
    private volatile List<QuerySortOrder> lastGridSortOrders = List.of();

    // Export tuning
    protected int getExportBatchSize() { return 5_000; }      // DB page size during export
    protected long getExportWarnThreshold() { return 50_000; } // show confirm if huge
    protected String getExportFileBaseName() { return getViewPageTitle(); }
    private static final int EXCEL_MAX_ROWS_PER_SHEET = 1_048_576;      // includes header row
    private static final int EXCEL_MAX_DATA_ROWS_PER_SHEET = EXCEL_MAX_ROWS_PER_SHEET - 1;

    protected MenuItem miToggleColumns;

    // Quick Search (toolbar)
    protected TextField quickSearchField;
    private boolean syncingQuickSearch = false;

    // Quick Search (overflow menu)
    private TextField quickSearchOverflowField;

    protected CustomDialog editorLayout = new CustomDialog("");
    protected final BeanValidationBinder<T> binder;
    private final Class<T> beanType;
    
    protected Button btnSave;
    protected Button btnCancel;
    private HorizontalLayout editorFooterLayout;

    // ✅ keep your record style (rename generic to E to avoid shadowing PageDialogLayout<T>)
    protected record ColumnDef<E>(
            String key,
            String header,
            ValueProvider<E, ?> dataProvider,
            Function<E, String> textFormatter
    ) {}

    // ✅ helper so BuildingView can do col("id",... ) without new ColumnDef<Building>(...)
    protected static <E> ColumnDef<E> col(
            String key,
            String header,
            ValueProvider<E, ?> provider,
            Function<E, String> formatter
    ) {
        return new ColumnDef<>(key, header, provider, formatter);
    }
    

    
 // +++++++++++++++++++++++ Advanced Search (Popover) +++++++++++++++++++++++
    private Popover advancedSearchPopover;
    private Component advancedSearchBody;
    protected AdvancedSearchPanel advPanel;
    
    //++++++++++++++++++++++++++show/hide column on Grid++++++++++++++
    private boolean columnVisibilityInitialized = false;
    private Popover columnsPopover;
    private CheckboxGroup<String> columnsGroup;

    private final LinkedHashMap<String, Grid.Column<T>> columnsByKey = new LinkedHashMap<>();
    private final Map<String, String> headersByKey = new HashMap<>();
    private List<String> orderedKeys = new ArrayList<>();

    // ✅ Better than header text (works even if you make bilingual headers later)
    //protected  final Set<String> excludedDefaultKeys = Set.of("userCreated.name", "createdAt", "userUpdated.name", "updatedAt");
    protected final Set<String> excludedDefaultKeys = new HashSet<>(Set.of("userCreated.name", "createdAt", "userUpdated.name", "updatedAt"));
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of();
    }
    
 // ====================== SQL Filter Bar (chips) ======================
    private final HorizontalLayout sqlFilterBar = new HorizontalLayout();
    private final FlexLayout sqlChipContainer = new FlexLayout();
    private String lastSqlFilterKey = null;


    private boolean viewAttachInitialized = false;
    
    
    protected boolean enableToggleColumn = false;
    protected int toggleColumnWidth = 80;
    protected boolean toggleColumnFrozen = true;
    protected String toggleColumnKey = "toggleDetails";
    
    public PageDialogLayout(Class<T> beanType, S service, UserService userService, AuthenticatedUser authenticatedUser) {
        this.beanType = beanType;
        this.binder = new BeanValidationBinder<>(beanType);
        this.service = service;
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("overflow", "hidden");

       // add(buildToolBar(), grid,paginationControls);
        Component toolbar = buildToolBar();
        initSqlFilterBar();
        add(toolbar, sqlFilterBar, grid, paginationControls);
      

        setFlexGrow(1, grid);
        grid.setSizeFull();
        paginationControls.bind(grid);
        paginationControls.addClassName("sticky-pagination");

        
       grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_COMPACT,GridVariant.LUMO_COLUMN_BORDERS);
       
       grid.setPageSize(500); // Match Vaadin's DataCommunicator limit
       grid.setAllRowsVisible(false); // Enable virtual scrolling
       grid.getElement().setProperty("scrollingMode", "virtual"); // Force virtual scrolling
       
        GridMultiSelectionModel<T> sm = (GridMultiSelectionModel<T>) grid.setSelectionMode(Grid.SelectionMode.MULTI);
        sm.setDragSelect(true);
        
     // Make entire row clickable to toggle selection
        grid.addItemClickListener(event -> {
            T item = event.getItem();
            if (item == null) return;

            if (sm.isSelected(item)) {
            	sm.deselect(item);
            } else {
            	sm.select(item);
            }
        });
        
     // Optional: Visual feedback for selected rows
        grid.setPartNameGenerator(item -> 
        	sm.isSelected(item) ? "selected-row" : null);

        // Make cursor pointer on rows
        grid.getElement().getStyle().set("cursor", "pointer");

        // Optional: Nice hover effect (you can also add in CSS)
        grid.getElement().executeJs("""
            this.shadowRoot.querySelectorAll('vaadin-grid-row').forEach(row => {
                row.style.cursor = 'pointer';
            });
        """);
        

        
        grid.setColumnReorderingAllowed(true);
        
        grid.getStyle().set("min-height", "0");
        //grid.setAllRowsVisible(true);

        //fetchData(); // set data provider once
        bindSelectionState();
        enableDoubleClickToEdit();
        initDownloadAnchor();
    }

    // ------------------------------
    // Toolbar
    // ------------------------------
    private Component buildToolBar() {
        // LEFT
        miAddNew = leftMenu.addAction(VaadinIcon.PLUS_CIRCLE,null, this::onAddNew,"Add new | បន្ថែមថ្មី");
        miEdit = leftMenu.addAction(VaadinIcon.EDIT,null, this::requestEdit,"Edit | កែប្រែ");
        miDelete = leftMenu.addAction(VaadinIcon.TRASH, null, this::requestDelete,"Delete | លុប"); // ✅ now implemented in base
        miRefresh = leftMenu.addAction(VaadinIcon.REFRESH,null, this::refreshGrid,"Refresh | ផ្ទុកឡើងវិញ");

        // RIGHT
        quickSearchField = buildQuickSearchField(false);
        rightMenu.addComponentItem(quickSearchField, "Quick Search");

        quickSearchOverflowField = buildQuickSearchField(true);
        rightMenu.registerOverflowComponent("Quick Search", () -> quickSearchOverflowField);

        miAdvancedSearch = rightMenu.addAction(VaadinIcon.SEARCH_PLUS, null, this::onAdvancedSearch,"Advanced search | ស្វែងរកកម្រិតខ្ពស់");
        miExportExcel = rightMenu.addAction(VaadinIcon.TABLE, null, this::onExportExcel,"Export to Excel | នាំចេញជា Excel");
        miToggleColumns = rightMenu.addAction(VaadinIcon.GRID_H, null, this::onToggleColumns,"Show / hide columns | បង្ហាញ / លាក់ជួរឈរ");

        // (keep your class hooks if you use css)
        miAddNew.addClassNames("mi-primary");
        miAdvancedSearch.addClassNames("mi-primary");
        miDelete.addClassNames("mi-danger");

        miEdit.addClassNames("mi-neutral");
        miRefresh.addClassNames("mi-neutral");
        miExportExcel.addClassNames("mi-neutral");
        miToggleColumns.addClassNames("mi-neutral");
        

        FlexLayout toolbar = new FlexLayout();
        toolbar.addClassName("sticky-toolbar");
        
        toolbar.getStyle().set("min-width", "0");
        leftMenu.getStyle().set("min-width", "0");
        rightMenu.getStyle().set("min-width", "0");

        toolbar.setWidthFull();
        toolbar.getStyle()
                .set("box-sizing", "border-box")
                .set("padding-left", "0.75rem")
                .set("padding-right", "0.75rem");

        toolbar.setFlexDirection(FlexLayout.FlexDirection.ROW);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        rightMenu.addThemeVariants(MenuBarVariant.LUMO_END_ALIGNED);
        toolbar.add(leftMenu, rightMenu);

        configureToolBar();
        return toolbar;
    }

    private TextField buildQuickSearchField(boolean overflow) {
        TextField tf = new TextField();
        tf.setPlaceholder("Quick search...");
        tf.setPrefixComponent(VaadinIcon.SEARCH.create());
        tf.setClearButtonVisible(true);

        if (overflow) {
            tf.setWidth("220px");
            tf.getStyle().set("max-width", "260px");
        } else {
            tf.setWidth("160px");
        }

        // ✅ Search only when Enter pressed
        tf.addKeyDownListener(Key.ENTER, e -> runQuickSearch(tf.getValue()));

        // ✅ Auto-search when user clears text (built-in clear button)
        tf.getElement().addEventListener("value-changed", ev -> {
            if (syncingQuickSearch) return;

            var node = ev.getEventData().get("event.detail.value"); // JsonNode
            /*
            String v = (node == null || node.isNull()) ? "" : node.asText("");

            if (v.trim().isEmpty()) {
                runQuickSearch("");
            }
            */

            String v = node == null ? "" : node.asText("");

            if (v.trim().isEmpty()) {
                runQuickSearch("");
            }
            
        }).addEventData("event.detail.value");

        return tf;
    }
    private void runQuickSearch(String value) {
        String v = value == null ? "" : value.trim();

        // sync both fields without recursion
        syncingQuickSearch = true;
        try {
            if (quickSearchField != null && !Objects.equals(quickSearchField.getValue(), v)) {
                quickSearchField.setValue(v);
            }
            if (quickSearchOverflowField != null && !Objects.equals(quickSearchOverflowField.getValue(), v)) {
                quickSearchOverflowField.setValue(v);
            }
        } finally {
            syncingQuickSearch = false;
        }

        onQuickSearch(v);
        paginationControls.resetToFirstPage();
        refreshGrid();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // ✅ IMPORTANT: only run once per view instance
        if (viewAttachInitialized) return;
        viewAttachInitialized = true;

        leftMenu.initOverflowObserver();
        rightMenu.initOverflowObserver();

        stopBubblingOnce(quickSearchField);
        stopBubblingOnce(quickSearchOverflowField);

        addClassName("responsive-dialog-layout");

        // ✅ view-specific init moved here (hook)
        try {
            onViewAttachOnce(attachEvent);
        } catch (Exception e) {
            showErrorMessage("Error during UI setup: " + e.getMessage());
            e.printStackTrace();
        }

        // ✅ set data provider once (same as your current logic)
        fetchData();
        afterDataProviderConfigured();

        injectGridSelectionStyles();
    }

    private void stopBubblingOnce(TextField tf) {
        if (tf == null) return;
        if ("1".equals(tf.getElement().getProperty("__stopBubbling"))) return;

        tf.getElement().setProperty("__stopBubbling", "1");
        tf.getElement().executeJs("""
            this.addEventListener('mousedown', e => e.stopPropagation());
            this.addEventListener('click', e => e.stopPropagation());
            this.addEventListener('keydown', e => e.stopPropagation());
        """);
    }

    // ------------------------------
    // Public helpers
    // ------------------------------
    protected void refreshGrid() {
	    grid.deselectAll();
	    grid.getDataProvider().refreshAll();
    }

    protected void showErrorMessage(String message) {
        Notification.show(message, 9000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    protected void showSuccessMessage(String message) {
        Notification.show(message, 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    // ------------------------------
    // Hooks
    // ------------------------------
    protected void configureToolBar() {}

    protected void onQuickSearch(String text) {}
    
    protected void onAdvancedSearch() {
        ensureAdvancedSearchPopover();
        advancedSearchPopover.setOpened(true);
    }

    private void ensureAdvancedSearchPopover() {
        if (advancedSearchPopover != null) return;

        advancedSearchPopover = new Popover();
        advancedSearchPopover.setModal(true);
        advancedSearchPopover.setBackdropVisible(true);
        advancedSearchPopover.setPosition(PopoverPosition.BOTTOM_END);

        // open programmatically (same pattern as columns popover)
        advancedSearchPopover.setOpenOnClick(false);

        // anchor to rightMenu so it still works even if miAdvancedSearch goes into overflow "..."
        advancedSearchPopover.setTarget(rightMenu);

        Div heading = new Div("Advanced search");
        heading.getStyle()
                .set("font-weight", "600")
                .set("padding", "var(--lumo-space-xs)");

        // body (child view will provide the filter layout)
        advancedSearchBody = buildAdvancedSearchLayout();
        if (advancedSearchBody == null) {
            advancedSearchBody = new Div(new Span("No filters"));
        }

        // make body scrollable if many fields
        Div bodyWrap = new Div(advancedSearchBody);
        bodyWrap.getStyle()
                .set("max-height", "60vh")
                .set("overflow", "auto")
                .set("padding", "var(--lumo-space-xs)");

        Button apply = new Button("Apply", e -> {
            paginationControls.resetToFirstPage();
            refreshGrid();                 // your dataProvider reads buildCombinedSpecification()
            advancedSearchPopover.setOpened(false);
        });
        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        apply.addClickShortcut(Key.ENTER);

        Button reset = new Button("Reset", e -> {
            resetAdvancedSearchFields();   // child clears fields
            paginationControls.resetToFirstPage();
            refreshGrid();
        });
        reset.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button close = new Button("Close", e -> advancedSearchPopover.setOpened(false));
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        close.addClickShortcut(Key.ESCAPE);

        HorizontalLayout footer = new HorizontalLayout(apply, reset, close);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();

        advancedSearchPopover.add(heading, bodyWrap, footer);

        // attach overlay to the view
        if (advancedSearchPopover.getParent().isEmpty()) {
            this.add(advancedSearchPopover);
        }
    }
    protected Component buildAdvancedSearchLayout() {
        return new Div(); // default empty
    }
    
    protected void resetAdvancedSearchFields() {
        // default no-op
    }

    //++++++++++++++++Export to excel+++++++++++++++++
    private final Anchor downloadAnchor = new Anchor();

    private void initDownloadAnchor() {
        downloadAnchor.getElement().setAttribute("download", true);
        downloadAnchor.getStyle().set("display", "none");
        add(downloadAnchor);
    }


    protected void onExportExcel() {

        final List<ExportCol<T>> cols = buildVisibleExportColumns();
        if (cols.isEmpty()) {
            showErrorMessage("No visible columns to export. | មិនមានជួរឈរដែលបង្ហាញសម្រាប់នាំចេញ។");
            return;
        }

        final List<T> selected = new ArrayList<>(grid.getSelectedItems());
        if (!selected.isEmpty()) {
            // keep your confirm + selected export
            openExportConfirmDialogWithTotal(true, selected, null, cols);
            return;
        }

        // ✅ no selection: ask scope (current page vs all filtered)
        openExportScopeDialog(cols);
    }
    private void openExportScopeDialog(List<ExportCol<T>> cols) {
        final Specification<T> spec = buildCombinedSpecification();

        Dialog d = new Dialog();
        d.setCloseOnEsc(true);
        d.setCloseOnOutsideClick(true);
      

        H3 title = new H3("Export scope | ជម្រើសនាំចេញ");

        Span msg = new Span(
                "No selection. Export current page only, or all filtered rows?\n" +
                "មិនបានជ្រើសរើសទិន្នន័យទេ។ តើចង់នាំចេញតែទំព័របច្ចុប្បន្ន ឬទាំងអស់តាមការច្រោះ?"
        );
        msg.getStyle().set("white-space", "pre-line");


        ProgressBar pb = new ProgressBar();
        pb.setIndeterminate(true);
        pb.setVisible(true);
        pb.setWidthFull();

        Span allInfo = new Span("Calculating total... | កំពុងគណនា...");
        allInfo.getStyle().set("white-space", "pre-line");

        Button cancel = new Button("Cancel | បោះបង់", e -> d.close());
        cancel.addThemeVariants( ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_ERROR,ButtonVariant.LUMO_SMALL);

        Button currentPage = new Button("Current page | ទំព័របច្ចុប្បន្ន", e -> {
            d.close();
            exportCurrentPageStreaming(spec, cols);
        });
        currentPage.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button allFiltered = new Button("All filtered | ទាំងអស់តាមការច្រោះ");
        allFiltered.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_WARNING, ButtonVariant.LUMO_SMALL);
        allFiltered.setEnabled(false); // enable after count finishes

        VerticalLayout body = new VerticalLayout(title, msg, pb, allInfo);
        body.setPadding(false);
        body.setSpacing(true);

        HorizontalLayout footer = new HorizontalLayout(cancel, currentPage, allFiltered);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        d.add(body, footer);
        d.open();

        // async count (so dialog doesn’t freeze)
        UI ui = UI.getCurrent();
        CompletableFuture
                .supplyAsync(() -> service.count(spec), exportExecutor)
                .whenComplete((total, err) -> ui.access(() -> {
                    pb.setVisible(false);

                    if (err != null) {
                        allInfo.setText("Failed to count: " + err.getMessage());
                        allFiltered.setEnabled(false);
                        return;
                    }

                    long sheets = (total + EXCEL_MAX_DATA_ROWS_PER_SHEET - 1) / EXCEL_MAX_DATA_ROWS_PER_SHEET;

                    allInfo.setText("All filtered: " + total + " rows"
                            + (sheets > 1 ? " (" + sheets + " sheets)" : "")
                            + "\nទាំងអស់តាមការច្រោះ: " + total + " ជួរ"
                            + (sheets > 1 ? " (" + sheets + " សន្លឹក)" : ""));

                    allFiltered.setEnabled(true);
                    allFiltered.addClickListener(e -> {
                        d.close();
                        exportAllFilteredWithProgress(spec, cols); // your big export with progress + sheet split
                    });
                }));
    }
    private void exportCurrentPageStreaming(Specification<T> spec, List<ExportCol<T>> cols) {

        final Sort exportSort = resolveExportSort();
        final int pageIndex = paginationControls.getCurrentPageIndex();
        final int pageSize  = paginationControls.getPageSizeValue();

        final String fileName = getExportFileBaseName() + "_page_" + (pageIndex + 1) + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        DownloadHandler handler = (DownloadEvent event) -> {
            event.setFileName(fileName);
            event.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            try (OutputStream os = event.getOutputStream();
                 SXSSFWorkbook wb = new SXSSFWorkbook(200)) {

                wb.setCompressTempFiles(true);

                Page<T> p = service.list(PageRequest.of(pageIndex, pageSize, exportSort), spec);
                List<T> items = p.getContent();

                // current page will never exceed Excel limit, so simple single-sheet writer is fine
                writeWorkbookFromItems(wb, cols, items);

                wb.write(os);
                wb.dispose();
            } catch (Exception ex) {
                event.getResponse().setStatus(500);
            }
        };

        downloadAnchor.setHref(handler);
        downloadAnchor.getElement().callJsFunction("click");
    }

    private void openExportConfirmDialogWithTotal(
            boolean hasSelection,
            List<T> selected,
            Specification<T> spec,
            List<ExportCol<T>> cols
    ) {
        ConfirmDialog cd = new ConfirmDialog();
        cd.setHeader("Confirm export | បញ្ជាក់ការនាំចេញ");
        cd.setCancelable(true);
        cd.setCancelText("Cancel | បោះបង់");
        cd.setConfirmText("Export | នាំចេញ");
        cd.setConfirmButtonTheme("primary");

        if (hasSelection) {
            cd.setText("Export " + selected.size() + " selected record(s)?"
                    + " | នាំចេញ " + selected.size() + " កំណត់ត្រាដែលបានជ្រើសរើស?");
            cd.addConfirmListener(e -> exportSelectedStreaming(selected, cols));
            cd.open();
            return;
        }

        // ⚠️ count can be slow on big queries (but you asked to show it)
        long total = service.count(spec);
        long sheets = (total + EXCEL_MAX_DATA_ROWS_PER_SHEET - 1) / EXCEL_MAX_DATA_ROWS_PER_SHEET;

        cd.setText("Export ALL " + total + " record(s)?"
                + (sheets > 1 ? " (" + sheets + " sheets)" : "")
                + " | នាំចេញ " + total + " កំណត់ត្រា?"
                + (sheets > 1 ? " (" + sheets + " សន្លឹក)" : ""));

        cd.addConfirmListener(e -> exportAllFilteredWithProgress(spec, cols));

        cd.open();
    }
    private void exportSelectedStreaming(List<T> selected, List<ExportCol<T>> cols) {

        final String fileName = getExportFileBaseName() + "_selected_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

        DownloadHandler handler = (DownloadEvent event) -> {
            event.setFileName(fileName);
            event.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            try (OutputStream os = event.getOutputStream();
                 SXSSFWorkbook wb = new SXSSFWorkbook(200)) {

                wb.setCompressTempFiles(true);
                //writeWorkbookFromItems(wb, cols, selected);
                writeWorkbookFromItemsSplit(wb, cols, selected);
                wb.write(os);
                wb.dispose();
            } catch (Exception ex) {
                event.getResponse().setStatus(500);
            }
        };

        downloadAnchor.setHref(handler);
        downloadAnchor.getElement().callJsFunction("click");
    }
    private void exportAllFilteredWithProgress(Specification<T> spec, List<ExportCol<T>> cols) {
        UI ui = UI.getCurrent();

        Dialog d = new Dialog();
        d.setCloseOnOutsideClick(false);
        d.setHeaderTitle("Exporting... | កំពុងនាំចេញ...");

        Span info = new Span("Preparing... | កំពុងរៀបចំ...");
        ProgressBar bar = new ProgressBar(0, 1, 0);
        bar.setWidthFull();

        Button close = new Button("Close | បិទ", e -> d.close());
        close.setEnabled(false);

        Anchor download = new Anchor();
        download.getElement().setAttribute("download", true);
        download.setText("Download | ទាញយក");
        download.setVisible(false);

        VerticalLayout content = new VerticalLayout(info, bar, download);
        content.setPadding(false);
        content.setSpacing(true);

        d.add(content);
        d.getFooter().add(close);
        d.open();

        // ✅ enable polling so UI updates show (no @Push needed)
        int oldPoll = ui.getPollInterval();
        ui.setPollInterval(700);
        d.addDetachListener(e -> ui.setPollInterval(oldPoll));

        CompletableFuture
                .supplyAsync(() -> {
                    // count can be slow -> do it in background too
                    long total = service.count(spec);
                    Sort sort = resolveExportSort();

                    try {
                        Path tmp = Files.createTempFile("export_", ".xlsx");
                        writeXlsxToFile(tmp, spec, sort, cols, total, (done) -> {
                            // progress callback (done rows)
                            ui.access(() -> {
                                double p = (total <= 0) ? 0 : Math.min(1.0, done / (double) total);
                                bar.setValue(p);
                                info.setText("Exported " + done + " / " + total + " rows");
                            });
                        });
                        return new ExportResult(tmp, total);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }, exportExecutor)
                .whenComplete((result, err) -> ui.access(() -> {
                    if (err != null) {
                        info.setText("Export failed: " + err.getMessage());
                        close.setEnabled(true);
                        return;
                    }

                    bar.setValue(1);
                    info.setText("Ready: " + result.total + " rows | រួចរាល់");

                    // ✅ download finished file (fast)
                    String fileName = getExportFileBaseName() + "_" +
                            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            + ".xlsx";

                    DownloadHandler handler = (DownloadEvent event) -> {
                        event.setFileName(fileName);
                        event.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        try (OutputStream os = event.getOutputStream()) {
                            Files.copy(result.file, os);
                        } finally {
                            // cleanup temp file after download attempt
                            try { Files.deleteIfExists(result.file); } catch (Exception ignore) {}
                        }
                    };

                    download.setHref(handler);
                    download.setVisible(true);
                    close.setEnabled(true);

                    // optional: auto-trigger download
                    download.getElement().callJsFunction("click");
                }));
    }

    private record ExportResult(Path file, long total) {}
    private interface ProgressFn { void onProgress(long doneRows); }

    private void writeXlsxToFile(
            Path file,
            Specification<T> spec,
            Sort sort,
            List<ExportCol<T>> cols,
            long total,
            ProgressFn progress
    ) throws Exception {

        int batchSize = Math.max(500, getExportBatchSize());

        try (SXSSFWorkbook wb = new SXSSFWorkbook(200);
             OutputStream os = Files.newOutputStream(file)) {

            wb.setCompressTempFiles(true);

            int sheetNo = 1;
            SheetCtx ctx = createSheetWithHeader(wb, sheetNo, cols);

            long done = 0;
            int page = 0;

            while (true) {
                Page<T> p = service.list(PageRequest.of(page, batchSize, sort), spec);
                List<T> items = p.getContent();
                if (items.isEmpty()) break;

                for (T item : items) {

                    // ✅ Split to new sheet when reaching limit
                    if (ctx.rowIdx >= EXCEL_MAX_ROWS_PER_SHEET) {
                        sheetNo++;
                        ctx = createSheetWithHeader(wb, sheetNo, cols);
                    }

                    Row r = ctx.sheet.createRow(ctx.rowIdx);
                    int rowIdxNext = ctx.rowIdx + 1;

                    for (int c = 0; c < cols.size(); c++) {
                        Object v = cols.get(c).valueOf(item); // or .value().apply(item) depending on your ExportCol
                        r.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
                    }

                    ctx = new SheetCtx(ctx.sheet, rowIdxNext, ctx.sheetNo);
                    done++;
                }

                if (progress != null) progress.onProgress(done);

                if (!p.hasNext()) break;
                page++;
            }

            wb.write(os);
            wb.dispose();
        }
    }

    private void writeWorkbookFromItemsSplit(SXSSFWorkbook wb, List<ExportCol<T>> cols, List<T> items) {
        int sheetNo = 1;
        SheetCtx ctx = createSheetWithHeader(wb, sheetNo, cols);

        for (T item : items) {
            if (ctx.rowIdx >= EXCEL_MAX_ROWS_PER_SHEET) {
                sheetNo++;
                ctx = createSheetWithHeader(wb, sheetNo, cols);
            }

            Row r = ctx.sheet.createRow(ctx.rowIdx);
            int next = ctx.rowIdx + 1;

            for (int c = 0; c < cols.size(); c++) {
                Object v = cols.get(c).valueOf(item);
                r.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
            }

            ctx = new SheetCtx(ctx.sheet, next, ctx.sheetNo);
        }
    }

    private void writeWorkbookFromItems(SXSSFWorkbook wb, List<ExportCol<T>> cols, List<T> items) {
        Sheet sheet = wb.createSheet("Export");
        int rowIdx = 0;

        Row header = sheet.createRow(rowIdx++);
        CellStyle headerStyle = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        headerStyle.setFont(bold);

        for (int c = 0; c < cols.size(); c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(cols.get(c).header());
            cell.setCellStyle(headerStyle);
        }

        for (T item : items) {
            Row r = sheet.createRow(rowIdx++);
            for (int c = 0; c < cols.size(); c++) {                
                Object v = cols.get(c).valueOf(item);
                r.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
            }
        }
    }


    private record ExportCol<E>(
            String key,
            String header,
            java.util.function.Function<E, Object> getter
    ) {
        Object valueOf(E item) {
            return getter == null ? null : getter.apply(item);
        }
    }


    private List<ExportCol<T>> buildVisibleExportColumns() {

        // Map your ColumnDef by key (what you use in grid.setKey(...))
        Map<String, ColumnDef<T>> defByKey = getColumnDefs().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ColumnDef::key,
                        java.util.function.Function.identity(),
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));

        List<ExportCol<T>> out = new ArrayList<>();

        // Keep grid column order + only visible columns
        for (Grid.Column<T> gc : grid.getColumns()) {
            String key = gc.getKey();
            if (key == null) continue;
            if (!gc.isVisible()) continue;

            ColumnDef<T> def = defByKey.get(key);
            if (def == null) continue; // skip columns not defined for export

            // Export “as displayed” (fast + avoids type issues)
            out.add(new ExportCol<>(
                    def.key(),
                    def.header(),
                    e -> def.textFormatter().apply(e)
            ));
        }

        return out;
    }
    private Sort resolveExportSort() {
        Sort sort = toSpringSort(lastGridSortOrders);
        if (sort == null || sort.isUnsorted()) {
            return getDefaultSort();
        }
        return sort;
    }
    private final ExecutorService exportExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "excel-export");
                t.setDaemon(true);
                return t;
            });

    private record SheetCtx(Sheet sheet, int rowIdx, int sheetNo) {}
    
    private SheetCtx createSheetWithHeader(
            SXSSFWorkbook wb,
            int sheetNo,
            List<ExportCol<T>> cols
    ) {
        // Sheet name must be <= 31 chars
        String name = "Export_" + sheetNo;
        if (name.length() > 31) name = name.substring(0, 31);

        Sheet sheet = wb.createSheet(name);

        // header style
        CellStyle headerStyle = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        headerStyle.setFont(bold);

        // header row (row 0)
        Row header = sheet.createRow(0);
        for (int c = 0; c < cols.size(); c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(cols.get(c).header());
            cell.setCellStyle(headerStyle);
        }

        return new SheetCtx(sheet, 1, sheetNo); // next row index = 1
    }
    
  //+++++++++++++++++++++++Show/Hide column on grid+++++++++++++++++
 /*   private void applyDefaultColumnVisibilityIfNeeded() {
        if (columnVisibilityInitialized) return;
        columnVisibilityInitialized = true;

        // Hide excluded keys by default (only for columns that have keys)
        grid.getColumns().forEach(col -> {
            String key = col.getKey();
            if (key != null) {
                col.setVisible(!excludedDefaultKeys.contains(key));
            }
        });
    }
    */
    private void applyDefaultColumnVisibilityIfNeeded() {
        if (columnVisibilityInitialized) return;
        columnVisibilityInitialized = true;

        // Combine parent and child excluded keys
        Set<String> allExcluded = new HashSet<>(excludedDefaultKeys);
        allExcluded.addAll(getAdditionalExcludedKeys());

        // Hide excluded keys by default (only for columns that have keys)
        grid.getColumns().forEach(col -> {
            String key = col.getKey();
            if (key != null) {
                col.setVisible(!allExcluded.contains(key));
            }
        });
    }
    
    protected void onToggleColumns() {
        ensureColumnsPopover();
        rebuildColumnsModel();          // refresh list (in case columns changed / reordered)
        columnsPopover.setOpened(true); // open programmatically
    }

    private void ensureColumnsPopover() {
        if (columnsPopover != null) return;

        columnsPopover = new Popover();
        columnsPopover.setModal(true);
        columnsPopover.setBackdropVisible(true);
        columnsPopover.setPosition(PopoverPosition.BOTTOM_END);

        // ✅ IMPORTANT: we open it ourselves when miToggleColumns is clicked
        columnsPopover.setOpenOnClick(false);

        // Anchor to your right menu bar (works even if miToggleColumns is inside overflow "dots")
        columnsPopover.setTarget(rightMenu);

        Div heading = new Div();
        heading.setText("Configure columns");
        heading.getStyle()
               .set("font-weight", "600")
               .set("padding", "var(--lumo-space-xs)");

        columnsGroup = new CheckboxGroup<>();
        columnsGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);

        // Apply visibility when user changes selection
        columnsGroup.addValueChangeListener(e -> applyColumnVisibility(e.getValue()));

        Button showAll = new Button("Show all", e -> columnsGroup.setValue(new LinkedHashSet<>(orderedKeys)));
        showAll.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Button reset = new Button("Reset", e -> columnsGroup.setValue(defaultVisibleKeys()));
        reset.addThemeVariants(ButtonVariant.LUMO_SMALL);

        HorizontalLayout footer = new HorizontalLayout(showAll, reset);
        footer.setSpacing(true);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        columnsPopover.add(heading, columnsGroup, footer);

        // ✅ Popover must be attached to the UI (adding it anywhere is fine; it's an overlay)
        if (columnsPopover.getParent().isEmpty()) {
            this.add(columnsPopover);
        }
    }

    private void rebuildColumnsModel() {
        columnsByKey.clear();
        headersByKey.clear();

        // only columns with keys
        List<Grid.Column<T>> cols = grid.getColumns().stream()
                .filter(c -> c.getKey() != null)
                .toList();

        for (Grid.Column<T> c : cols) {
            String key = c.getKey();
            columnsByKey.put(key, c);

            //String header = (c.getHeaderText() != null) ? c.getHeaderText() : key;
            String header = extractHeaderText(c);
            headersByKey.put(key, header);
        }

        orderedKeys = new ArrayList<>(columnsByKey.keySet());

        columnsGroup.setItems(orderedKeys);
        columnsGroup.setItemLabelGenerator(k -> headersByKey.getOrDefault(k, k));

        // Keep current selection if possible; otherwise use default
        Set<String> current = columnsGroup.getValue();
        Set<String> validCurrent = (current == null) ? Set.of()
                : current.stream()
                         .filter(columnsByKey::containsKey)
                         .collect(Collectors.toCollection(LinkedHashSet::new));

        if (validCurrent.isEmpty()) {
            columnsGroup.setValue(defaultVisibleKeys());
        } else {
            columnsGroup.setValue(validCurrent);
        }

        applyColumnVisibility(columnsGroup.getValue());
    }
    /**
     * Extract header text from column, handling both simple text and component headers
     */
    private String extractHeaderText(Grid.Column<T> column) {
        // Try to get simple header text first
        String headerText = column.getHeaderText();
        if (headerText != null && !headerText.isEmpty()) {
            return headerText;
        }
        
        // If no simple header text, try to extract from component
        Component headerComponent = column.getHeaderComponent();
        if (headerComponent != null) {
            return extractTextFromComponent(headerComponent);
        }
        
        // Fallback to column key
        return column.getKey();
    }

    /**
     * Recursively extract text from component
     */
    private String extractTextFromComponent(Component component) {
        if (component == null) return "";
        
        // If it's a Span or Text component, get its text
        if (component instanceof Span) {
            String text = ((Span) component).getText();
            if (text != null && !text.isEmpty()) {
                return text;
            }
        }
        
        // If it's a FlexLayout (your biHeader), try to get text from its children
        if (component instanceof FlexLayout) {
            StringBuilder text = new StringBuilder();
            component.getChildren().forEach(child -> {
                String childText = extractTextFromComponent(child);
                if (!childText.isEmpty()) {
                    if (text.length() > 0) text.append(" | ");
                    text.append(childText);
                }
            });
            return text.toString();
        }
        
        // If it's a VerticalLayout, try to get text from its children
        if (component instanceof VerticalLayout) {
            StringBuilder text = new StringBuilder();
            component.getChildren().forEach(child -> {
                String childText = extractTextFromComponent(child);
                if (!childText.isEmpty()) {
                    if (text.length() > 0) text.append(" | ");
                    text.append(childText);
                }
            });
            return text.toString();
        }
        
        // If it's a Div, try to get text from its children or text content
        if (component instanceof Div) {
            // Try to get text content
            String text = component.getElement().getText();
            if (text != null && !text.isEmpty()) {
                return text;
            }
            
            // Otherwise try children
            StringBuilder textBuilder = new StringBuilder();
            component.getChildren().forEach(child -> {
                String childText = extractTextFromComponent(child);
                if (!childText.isEmpty()) {
                    if (textBuilder.length() > 0) textBuilder.append(" | ");
                    textBuilder.append(childText);
                }
            });
            return textBuilder.toString();
        }
        
        return "";
    }

    private Set<String> defaultVisibleKeys() {
    	 Set<String> allExcluded = new HashSet<>(excludedDefaultKeys);
    	    allExcluded.addAll(getAdditionalExcludedKeys());
    	    
        return orderedKeys.stream()
                .filter(k -> !allExcluded.contains(k))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void applyColumnVisibility(Set<String> visibleKeys) {
        Set<String> safe = (visibleKeys == null) ? Set.of() : visibleKeys;
        columnsByKey.forEach((key, col) -> col.setVisible(safe.contains(key)));
    }
//+++++++++++++++++++++++Show/Hide column on grid+++++++++++++++++

    // labels for confirm dialog
    protected String getEntityLabelSingular() { return "record"; }
    protected String getEntityLabelPlural() { return "records"; }

    // ------------------------------
    // Required for child
    // ------------------------------
    protected abstract T createNewEntity() throws Exception;
    protected abstract void populateForm(T entity) throws Exception;
    protected abstract void focusFirstField();
    protected abstract void configureEditorLayout() throws Exception;
    protected abstract void binderField();
    //protected abstract T save() throws Exception;

    protected abstract Specification<T> buildCombinedSpecification();
    protected abstract List<ColumnDef<T>> getColumnDefs();

    // ------------------------------
    // Mode / title
    // ------------------------------
    protected void setFormMode(FormMode mode) {
        this.formMode = (mode == null) ? FormMode.ADD : mode;
    }

    protected boolean isAddMode() {
        return formMode == FormMode.ADD;
    }

    protected String getViewPageTitle() {
        PageTitle pt = getClass().getAnnotation(PageTitle.class);
        return pt != null ? pt.value() : getClass().getSimpleName();
    }

    protected String getDialogTitleWithMode() {
        String en = getViewPageTitle();
        //String km = getViewPageTitle();

        String modeEn = isAddMode() ? "New" : "Edit";
        //String modeKm = isAddMode() ? "ថ្មី" : "កែប្រែ";

        //return String.format("%s – %s | %s – %s", en, modeEn, km, modeKm);
        return String.format("%s – %s", en, modeEn);
    }

    // ------------------------------
    // Actions
    // ------------------------------
    protected void onAddNew() {
        try {
            setFormMode(FormMode.ADD);
            editorLayout.setDialogTitle(getDialogTitleWithMode());
            entity = createNewEntity();
            populateForm(entity);
            focusFirstField();
        } catch (Exception ex) {
            showErrorMessage("Error adding new record: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * MULTI selection, but Edit only allows ONE row.
     */
    protected void onEdit() {
        Set<T> selected = new LinkedHashSet<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            showErrorMessage("Please select a record to edit. | សូមជ្រើសរើសកំណត់ត្រាមួយ ដើម្បីកែប្រែ។");
            return;
        }
        if (selected.size() > 1) {
            showErrorMessage("Please select only one record to edit. | សូមជ្រើសរើសតែ ១ កំណត់ត្រាប៉ុណ្ណោះ ដើម្បីកែប្រែ។");
            return;
        }
        onEdit(selected.iterator().next());
    }

    protected void onEdit(T selected) {
        try {
            setFormMode(FormMode.EDIT);
            editorLayout.setDialogTitle(getDialogTitleWithMode());
            entity = selected;
            populateForm(entity);
            focusFirstField();
        } catch (Exception ex) {
            showErrorMessage("Error editing record: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    /**
     * ✅ Multi-delete + confirm dialog (no need to implement in view)
     * Uses service.delete(Set<T>) (your BuildingService already has delete(Set<Building>)).
     */
    protected void onDelete() {
        Set<T> selected = new LinkedHashSet<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            showErrorMessage("Please select record(s) to delete. | សូមជ្រើសរើសកំណត់ត្រា ដើម្បីលុប។");
            return;
        }

        int count = selected.size();
        String labelEn = (count == 1) ? getEntityLabelSingular() : getEntityLabelPlural();
        String labelKm = "កំណត់ត្រា"; // same for singular/plural in Khmer

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


    // ------------------------------
    // Data provider
    // ------------------------------
    private Sort toSpringSort(List<QuerySortOrder> sortOrders) {
        if (sortOrders == null || sortOrders.isEmpty()) return Sort.unsorted();

        Sort sort = Sort.unsorted();
        for (QuerySortOrder so : sortOrders) {
            Sort.Direction dir = (so.getDirection() == SortDirection.ASCENDING)
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            sort = sort.and(Sort.by(dir, so.getSorted()));
        }
        return sort;
    }

/*    private void fetchData() {
    	grid.setDataProvider(new CallbackDataProvider<>(
    		    gridQuery -> {
    	            // ✅ capture current sort orders (for export)
    	            lastGridSortOrders = (gridQuery.getSortOrders() == null) ? List.of() : List.copyOf(gridQuery.getSortOrders());
    		        int pageIndex = paginationControls.getCurrentPageIndex();
    		        int pageSize  = paginationControls.getPageSizeValue();

    		        Sort sort = toSpringSort(gridQuery.getSortOrders());
    		        if (sort.isUnsorted()) {
    		            sort = Sort.by(Sort.Direction.DESC, "updatedAt");
    		        }

    		        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);

    		        int innerOffset = Math.max(0, Math.min(gridQuery.getOffset(), pageSize));
    		        int innerLimit  = Math.max(0, Math.min(gridQuery.getLimit(), pageSize - innerOffset));
    		        if (innerLimit == 0) return java.util.stream.Stream.<T>empty();

    		        Page<T> page = service.list(pageable, buildCombinedSpecification());
    		        List<T> pageItems = page.getContent();

    		        return pageItems.stream()
    		                .skip(innerOffset)
    		                .limit(innerLimit);
    		    },
    		    gridQuery -> {
    		        long total = service.count(buildCombinedSpecification());
    		        paginationControls.setTotal(total);

    		        int pageIndex = paginationControls.getCurrentPageIndex();
    		        int pageSize  = paginationControls.getPageSizeValue();

    		        long start = (long) pageIndex * pageSize;
    		        long remaining = Math.max(0, total - start);

    		        // strict paging: grid sees only current page count
    		        return (int) Math.min(pageSize, remaining);
    		    }
    		));

    }
*/
    
    private void fetchData() {
        // Use the new SmartDataProvider
        SmartDataProvider<T> dataProvider = new SmartDataProvider<>(
            paginationControls,
            service,
            this::buildCombinedSpecification,
            getDefaultSort()
        );
        
        grid.setDataProvider(dataProvider);
        
        // Configure grid page size based on mode
        if (paginationControls.isShowingAll()) {
            // For virtual scrolling with "All" mode
            grid.setPageSize(500); // Must match Vaadin's limit
        } else {
            grid.setPageSize(paginationControls.getPageSizeValue());
        }
    }
    // ------------------------------
    // Grid columns from ColumnDef
    // ------------------------------
    protected void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        addToggleColumn();
        
        for (ColumnDef<T> def : getColumnDefs()) {
            Grid.Column<T> column = grid.addColumn(item -> {
                        Object v = def.dataProvider() == null ? null : def.dataProvider().apply(item);
                        if (def.textFormatter() != null) return def.textFormatter().apply(item);
                        return v == null ? "" : Objects.toString(v);
                    })
                    .setKey(def.key())
                    .setResizable(true)
                    .setAutoWidth(true)
                    .setTextAlign(ColumnTextAlign.CENTER);

            String sortProperty = getSortProperty(def.key());
            if (sortProperty == null || sortProperty.isBlank()) {
                column.setSortable(false);
            } else {
                column.setSortable(true);
                column.setSortProperty(sortProperty);
            }

            // Optional one-row or two-row header.
            if (isGridColumn2Title(def.header())) {
                String[] title = def.header().split("\\|", 2);
                column.setHeader(biHeader(title[0].trim(), title[1].trim()));
            } else {
                column.setHeader(def.header());
            }

        }
        applyDefaultColumnVisibilityIfNeeded();
    }

    // 2-row header component

    boolean isGridColumn2Title(String gridColumnTitle) {
        return gridColumnTitle != null && !gridColumnTitle.isBlank() && gridColumnTitle.contains("|");
    }

    
    private static Component biHeader(String en, String kh) {
        FlexLayout header = new FlexLayout();
        header.setFlexDirection(FlexLayout.FlexDirection.COLUMN);  // This works with FlexLayout
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        header.setWidthFull();
        header.setHeightFull();  // Take full column height
        header.getStyle().set("min-height", "50px");  // Ensure minimum height

        Span enLabel = new Span(en == null ? "" : en);
        enLabel.getStyle()
               //.set("font-size", "var(--lumo-font-size-s)")
               .set("text-align", "center")
               .set("width", "100%")
               .set("line-height", "1.4");

        Span khLabel = new Span(kh == null ? "" : kh);
        khLabel.getStyle()
              // .set("font-size", "var(--lumo-font-size-s)")
               .set("text-align", "center")
               .set("width", "100%")
               .set("line-height", "1.4");
               //.set("color", "var(--lumo-secondary-text-color)"); // Optional: lighter color for Khmer

        header.add(enLabel, khLabel);
        return header;
    }

    

    
    protected void clearForm() {
        binder.readBean(null);
        this.entity = null;
    }

    protected void closeForm() {
        editorLayout.close();
    }
    
    private void bindSelectionState() {
        // initial state
        updateSelectionButtons(grid.getSelectedItems().size());

        grid.addSelectionListener(e -> updateSelectionButtons(e.getAllSelectedItems().size()));
    }

    private void updateSelectionButtons(int selectedCount) {
            miEdit.setEnabled(selectedCount == 1);
            miDelete.setEnabled(selectedCount >= 1);
    }
    
    protected void enableDoubleClickToEdit() {
        grid.addItemDoubleClickListener(e -> {
            T item = e.getItem();
            if (item == null) return;

            // optional: keep selection in sync
            grid.deselectAll();
            grid.select(item);

            onEdit(item); // open dialog
        });
    }

    protected String getSaveCaption() { return "Save | រក្សាទុក"; }
    protected String getCancelCaption() { return "Cancel | បោះបង់"; }

    protected void configureEditorFooter() {
        // avoid duplicates if called more than once
        if (editorFooterLayout != null) return;

        btnCancel = new Button(getCancelCaption(), ev -> {
            clearForm();
            closeForm();
        });
        btnCancel.setIcon(new Icon(VaadinIcon.CLOSE));
        btnCancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        btnCancel.addClickShortcut(Key.ESCAPE);

        btnSave = new Button(getSaveCaption(), ev -> {
            try {
                save();          // child implements
                clearForm();
                closeForm();
                refreshGrid();
            } catch (ValidationException e) {
                showErrorMessage("Please check required fields. | សូមពិនិត្យវាលដែលត្រូវបំពេញ។ " + e.getMessage());
            } catch (IllegalArgumentException e) {
                showErrorMessage(e.getMessage());
            } catch (DataIntegrityViolationException e) {
                showErrorMessage(e.getMessage());
            } catch (ObjectOptimisticLockingFailureException e) {
                showErrorMessage(e.getMessage());
                refreshGrid();
            } catch (Exception e) {
                showErrorMessage(e.getMessage());
            }
        });
        btnSave.setIcon(new Icon(VaadinIcon.CHECK));
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnSave.addClickShortcut(Key.ENTER);

        editorFooterLayout = new HorizontalLayout(btnCancel,btnSave);
        editorFooterLayout.setClassName("button-layout");

        editorLayout.getFooter().add(editorFooterLayout);
    }
    
    protected T save() throws Exception {
        if (entity == null) {
            throw new IllegalStateException("No record to save. | មិនមានទិន្នន័យសម្រាប់រក្សាទុក។");
        }

        
        boolean isNew = (entity.getId() == null || entity.getId() == 0L);

        binder.writeBean(entity);

        beforeSave(entity, isNew);
        entity = service.update(entity);
        afterSave(entity, isNew);

        showSuccessMessage(getSaveSuccessMessage(isNew));
        return entity;
    }

    protected void beforeSave(T entity, boolean isNew) throws Exception {
        // optional override in child
    }

    protected void afterSave(T entity, boolean isNew) throws Exception {
        // optional override in child
    }

    protected String getSaveSuccessMessage(boolean isNew) {
        return isNew
                ? "Saved successfully. | រក្សាទុកបានជោគជ័យ។"
                : "Updated successfully. | កែប្រែបានជោគជ័យ។";
    }
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Class<?> navigationTarget = event.getNavigationTarget();

        boolean allowed = authenticatedUser.hasPage( navigationTarget, AccessPageType.SELECTED_PAGE);

        if (!allowed) {
            event.rerouteTo( AccessDeniedView.class );
        }
    }
    


    protected <T> MultiSelectComboBox<T> buildLazyMultiSelect(
            ItemLabelGenerator<T> itemLabel,
            BiFunction<String, Pageable, List<T>> fetchFn,
            ToLongFunction<String> countFn,
            MultiSelectComboBoxVariant... variants   // ✅ add this
    ) {
        MultiSelectComboBox<T> ms = new MultiSelectComboBox<>();
        String placeholder = "Type at least 3 letters";
        int minChars = 3;
        int pageSize = 50;

        ms.setWidthFull();
        ms.setClearButtonVisible(true);
        ms.setPlaceholder(placeholder);
        //ms.setItemLabelGenerator(itemLabel);
        ms.setItemLabelGenerator(item -> {
            if (item == null) return "";
            String s = (itemLabel == null) ? "" : itemLabel.apply(item);
            return s == null ? "" : s;
        });
        ms.setPageSize(pageSize);
        ms.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);

        // ✅ optional variants
        if (variants != null && variants.length > 0) {
            ms.addThemeVariants(variants);
        }

        ms.getElement().setProperty("filterDebounceTimeout", 400);

        CallbackDataProvider.FetchCallback<T, String> fetch = q -> {
            String filter = q.getFilter().orElse("").trim();
            if (filter.length() < minChars) return Stream.empty();

            int offset = q.getOffset();
            int limit  = q.getLimit();
            int page   = offset / Math.max(limit, 1);

            Pageable pageable = PageRequest.of(page, limit);
            return fetchFn.apply(filter, pageable).stream();
        };

        CallbackDataProvider.CountCallback<T, String> count = q -> {
            String filter = q.getFilter().orElse("").trim();
            if (filter.length() < minChars) return 0;

            long c = countFn.applyAsLong(filter);
            return (int) Math.min(Integer.MAX_VALUE, c);
        };

        CallbackDataProvider<T, String> dp = new CallbackDataProvider<>(fetch, count);
        ms.setDataProvider(dp, text -> text == null ? "" : text.trim());

        return ms;
    }
    
    protected <E> MultiSelectComboBox<E> buildMultiSelect(
            ItemLabelGenerator<E> itemLabel,
            Collection<E> items,
            MultiSelectComboBoxVariant... variants
    ) {
        MultiSelectComboBox<E> ms = new MultiSelectComboBox<>();

        ms.setWidthFull();
        ms.setClearButtonVisible(true);
        ms.setPlaceholder("Select...");
        ms.setItemLabelGenerator(itemLabel);
        ms.setAutoExpand(AutoExpandMode.BOTH);

        if (variants != null && variants.length > 0) {
            ms.addThemeVariants(variants);
        }

        ms.setItems(items == null ? List.of() : items);

        return ms;
    }
    protected ComboBox<Boolean> buildBooleanAsYesNoComboBox(String placeholder,ComboBoxVariant... variants) {
        ComboBox<Boolean> cb = new ComboBox<>();
        cb.setItems(Boolean.TRUE, Boolean.FALSE);
        cb.setItemLabelGenerator(v -> Boolean.TRUE.equals(v) ? "Yes" : "No");
        cb.setClearButtonVisible(true); // clear = no filter
        cb.setPlaceholder(placeholder);
        cb.setWidthFull();
        
        if (variants != null && variants.length > 0) {
        	cb.addThemeVariants(variants);
        }
        
        return cb;
    }

    protected <T> ComboBox<T> buildLazyComboBox(
            ItemLabelGenerator<T> itemLabel,
            BiFunction<String, Pageable, List<T>> fetchFn,
            ToLongFunction<String> countFn,
            ComboBoxVariant... variants
    ) {
        ComboBox<T> cb = new ComboBox<>();
        String placeholder = "Type at least 3 letters";
        int minChars = 3;

        cb.setWidthFull();
        cb.setClearButtonVisible(true);
        cb.setPlaceholder(placeholder);
        //cb.setItemLabelGenerator(itemLabel);
        cb.setItemLabelGenerator(item -> {
            if (item == null) return "";
            String s = (itemLabel == null) ? "" : itemLabel.apply(item);
            return s == null ? "" : s;
        });
        cb.setPageSize(50);

        if (variants != null && variants.length > 0) cb.addThemeVariants(variants);

        cb.getElement().setProperty("filterDebounceTimeout", 400);

        CallbackDataProvider.FetchCallback<T, String> fetch = q -> {
            String filter = q.getFilter().orElse("");
            filter = (filter == null) ? "" : filter.trim();
            if (filter.length() < minChars) return Stream.empty();

            int offset = q.getOffset();
            int limit  = q.getLimit();
            int page   = (limit <= 0) ? 0 : (offset / limit);

            Pageable pageable = PageRequest.of(page, limit);
            return fetchFn.apply(filter, pageable).stream();
        };

        CallbackDataProvider.CountCallback<T, String> count = q -> {
            String filter = q.getFilter().orElse("");
            filter = (filter == null) ? "" : filter.trim();
            if (filter.length() < minChars) return 0;

            long c = countFn.applyAsLong(filter);
            return (int) Math.min(Integer.MAX_VALUE, c);
        };

        CallbackDataProvider<T, String> dp = new CallbackDataProvider<>(fetch, count);

        // ✅ Vaadin 24.9.x way
        cb.setDataProvider(dp, text -> text == null ? "" : text.trim());

        return cb;
    }



   //+++++++++++++++++++++++++++Advance Search++++++++++++++++++++++++++++++++++ 
  protected Predicate buildLikePredicate(CriteriaBuilder cb, Expression<?> expression, String likePattern) {
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
    
    @SuppressWarnings("unchecked")
    protected void advanceFilterBuildLikePredicate(
            CriteriaBuilder cb,
            Expression<?> expression,
            String input,              // <-- raw text: "halo"
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter
    ) {
        if (input == null || input.isBlank()) {
            return;
        }

        // Build LIKE pattern: contains search
        String trimmed = input.trim();

        // escape LIKE special chars for display + query (Postgres): % and _
        String escapedLike = trimmed
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        String likePattern = "%" + escapedLike.toLowerCase() + "%";

        Expression<String> stringExpr;

        if (String.class.equals(expression.getJavaType())) {
            stringExpr = cb.lower((Expression<String>) expression);
        } else if (Number.class.isAssignableFrom(expression.getJavaType())) {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        } else if (java.time.temporal.Temporal.class.isAssignableFrom(expression.getJavaType())
                || java.util.Date.class.isAssignableFrom(expression.getJavaType())) {
            stringExpr = cb.lower(DateTimeUtilFormart.DATE_TIME_FORMATTER_DB(cb, expression));
        } else {
            stringExpr = cb.lower(cb.concat(expression.as(String.class), ""));
        }

        // ✅ use ESCAPE '\' so our \% and \_ work
        Predicate p = cb.like(stringExpr, likePattern, '\\');
        predicates.add(p);

        // ✅ UI string
        if (sqlFilter != null) {
            String ui = escapedLike.replace("'", "''");
            sqlFilter.add(label + " ILIKE '%" + ui + "%'");
        }
    }

    
    protected <E> void advanceFilterBuildInPredicate(
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
    
    protected <T extends Comparable<? super T>> void advanceFilterBuildBetweenPredicate(
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
    
    protected void advanceFilterBuildGazetteerGroupedPredicate(
            CriteriaBuilder cb,
            Root<?> root,
            String fieldName, // "presentAddress" / "permanentAddress" / "nativeLocation"
            GazetteerMultiSearchField gf,
            String label,
            List<Predicate> predicates,
            List<String> sqlFilter
    ) {
        if (gf == null) return;

        var provSel = safeSet(gf.getSelectedProvinces());
        var distSel = safeSet(gf.getSelectedDistricts());
        var commSel = safeSet(gf.getSelectedCommunes());
        var villSel = safeSet(gf.getSelectedVillages());

        if (provSel.isEmpty() && distSel.isEmpty() && commSel.isEmpty() && villSel.isEmpty()) return;

        // Join chain: g -> parent -> parent -> parent
        var g  = root.join(fieldName, JoinType.LEFT);
        var p1 = g.join("parent", JoinType.LEFT);
        var p2 = p1.join("parent", JoinType.LEFT);
        var p3 = p2.join("parent", JoinType.LEFT);

        // Group selected districts/communes/villages by their Province(id)
        var distByProv = groupByProvince(distSel);
        var commByProv = groupByProvince(commSel);
        var villByProv = groupByProvince(villSel);

        // Province IDs that must appear:
        // - explicit provinces selected
        // - provinces implied by selected districts/communes/villages
        java.util.LinkedHashSet<Long> provinceIds = new java.util.LinkedHashSet<>();
        for (Gazetteer p : provSel) if (p != null && p.getId() != null) provinceIds.add(p.getId());
        provinceIds.addAll(distByProv.keySet());
        provinceIds.addAll(commByProv.keySet());
        provinceIds.addAll(villByProv.keySet());

        if (provinceIds.isEmpty()) return;

        List<Predicate> orGroups = new ArrayList<>();

        for (Long provId : provinceIds) {
            if (provId == null) continue;

            // "this record is inside province provId"
            Predicate inProvince = cb.or(
                    g.get("id").in(provId),
                    p1.get("id").in(provId),
                    p2.get("id").in(provId),
                    p3.get("id").in(provId)
            );

            List<Predicate> andInside = new ArrayList<>();
            andInside.add(inProvince);

            // Apply lower-level filters ONLY for this province if user selected any within it

            var villIds = toIds(villByProv.get(provId));
            if (!villIds.isEmpty()) {
                // village selection is most specific (exact)
                andInside.add(g.get("id").in(villIds));
            } else {
                var commIds = toIds(commByProv.get(provId));
                if (!commIds.isEmpty()) {
                    andInside.add(cb.or(
                            g.get("id").in(commIds),   // stored as commune
                            p1.get("id").in(commIds)   // stored as village
                    ));
                }

                var distIds = toIds(distByProv.get(provId));
                if (!distIds.isEmpty()) {
                    andInside.add(cb.or(
                            g.get("id").in(distIds),   // stored as district
                            p1.get("id").in(distIds),  // stored as commune
                            p2.get("id").in(distIds)   // stored as village
                    ));
                }
            }

            orGroups.add(cb.and(andInside.toArray(Predicate[]::new)));
        }

        predicates.add(cb.or(orGroups.toArray(Predicate[]::new)));
        //sqlFilter.add(label + " (grouped by province)");
        
     // ---- SQL chip text (show selected values) ----
        if (sqlFilter != null) {
            // provinceById from selected provinces + implied provinces from lower levels
            Map<Long, Gazetteer> provinceById = new LinkedHashMap<>();
            for (Gazetteer p : provSel) {
                if (p != null && p.getId() != null) provinceById.put(p.getId(), p);
            }

            // If user selected district/commune/village but didn't explicitly select province,
            // we still want province name to appear.
            // We'll try to fetch province object from one of the selected items (walking parent chain).
            Stream.of(distSel, commSel, villSel)
                    .filter(Objects::nonNull)
                    .flatMap(Set::stream)
                    .filter(Objects::nonNull)
                    .forEach(x -> {
                        Gazetteer prov = findProvinceNode(x); // level=1 node
                        if (prov != null && prov.getId() != null) provinceById.putIfAbsent(prov.getId(), prov);
                    });

            String desc = describeProvinceGroups(
                    provinceById,
                    distByProv,
                    commByProv,
                    villByProv
            );

            if (desc == null || desc.isBlank()) {
                sqlFilter.add(label + " (grouped by province)");
            } else {
                sqlFilter.add(label + " (grouped by province): " + desc);
            }
        }

    }

    // ---------------- helpers ----------------
    private static Gazetteer findProvinceNode(Gazetteer g) {
        Gazetteer cur = g;
        while (cur != null && cur.getLevel() != null && cur.getLevel() > 1) {
            cur = cur.getParent();
        }
        return (cur != null && cur.getLevel() != null && cur.getLevel() == 1) ? cur : null;
    }

    private static <T> java.util.Set<T> safeSet(java.util.Set<T> s) {
        return s == null ? java.util.Set.of() : s;
    }

    private static Map<Long, Set<Gazetteer>> groupByProvince(Set<Gazetteer> items) {
        Map<Long, Set<Gazetteer>> map = new LinkedHashMap<>();
        if (items == null) return map;

        for (Gazetteer g : items) {
            Long provId = provinceIdOf(g);
            if (provId == null) continue;
            map.computeIfAbsent(provId, k -> new LinkedHashSet<>()).add(g);
        }
        return map;
    }


    private static Long provinceIdOf(Gazetteer g) {
        if (g == null) return null;

        Gazetteer cur = g;
        while (cur != null && cur.getLevel() != null && cur.getLevel() > 1) {
            cur = cur.getParent();
        }
        return (cur != null && cur.getLevel() != null && cur.getLevel() == 1) ? cur.getId() : null;
    }

    private static java.util.Set<Long> toIds(java.util.Set<Gazetteer> gs) {
        if (gs == null || gs.isEmpty()) return java.util.Set.of();
        java.util.LinkedHashSet<Long> out = new java.util.LinkedHashSet<>();
        for (Gazetteer g : gs) if (g != null && g.getId() != null) out.add(g.getId());
        return out;
    }

    private static String formatEnKh(Gazetteer g) {
        if (g == null) return "";
        String en = g.getNameEn() == null ? "" : g.getNameEn();
        String kh = g.getNameKh() == null ? "" : g.getNameKh();
        return (en + " | " + kh).trim();
    }

    private static String fmtList(Collection<Gazetteer> list) {
        if (list == null || list.isEmpty()) return "";
        return list.stream()
                .filter(Objects::nonNull)
                .map(g -> formatEnKh(g) + (g.getCode() != null ? " (" + g.getCode() + ")" : ""))
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * groups: provinceId -> selections inside that province
     * Each value may contain selected districts/communes/villages under that province.
     */
    private static String describeProvinceGroups(
            Map<Long, Gazetteer> provinceById,
            Map<Long, Set<Gazetteer>> districtByProvince,
            Map<Long, Set<Gazetteer>> communeByProvince,
            Map<Long, Set<Gazetteer>> villageByProvince
    ) {
        return provinceById.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(g -> (g.getNameEn() == null ? "" : g.getNameEn()))))
                .map(e -> {
                    Long pid = e.getKey();
                    Gazetteer p = e.getValue();

                    String d = fmtList(districtByProvince.get(pid));
                    String c = fmtList(communeByProvince.get(pid));
                    String v = fmtList(villageByProvince.get(pid));

                    StringBuilder sb = new StringBuilder();
                    sb.append(formatEnKh(p));

                    List<String> parts = new ArrayList<>();
                    if (!v.isBlank()) parts.add("Village: " + v);
                    if (!c.isBlank()) parts.add("Commune: " + c);
                    if (!d.isBlank()) parts.add("District: " + d);

                    if (!parts.isEmpty()) sb.append(" [").append(String.join("; ", parts)).append("]");
                    else sb.append(" [Province only]");

                    return sb.toString();
                })
                .collect(java.util.stream.Collectors.joining(" ; "));
    }



//+++++++++++++++++++++++++++++show Sql Filter
    private void initSqlFilterBar() {
        sqlFilterBar.addClassName("sql-filter-bar");

        sqlFilterBar.setWidthFull();
        sqlFilterBar.setSpacing(true);
        sqlFilterBar.setAlignItems(FlexComponent.Alignment.CENTER);

        Span label = new Span("Filter:");
        label.getStyle().set("font-weight", "600");

        sqlChipContainer.addClassName("sql-filter-chips");
        sqlChipContainer.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        sqlChipContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        
        Button hide = new Button(new Icon(VaadinIcon.CLOSE_SMALL), e -> onCloseSqlFilterBar());

        hide.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        hide.getElement().setAttribute("aria-label", "Hide filter");

        sqlFilterBar.add(label, sqlChipContainer, hide);
        sqlFilterBar.setFlexGrow(1, sqlChipContainer);

        sqlFilterBar.setVisible(false);
    }
    private void onCloseSqlFilterBar() {
        // clear advanced search (child overrides this; e.g. advPanel.clearAll()) :contentReference[oaicite:1]{index=1}
        resetAdvancedSearchFields();

        // close advanced search popover if open
        if (advancedSearchPopover != null) {
            advancedSearchPopover.setOpened(false);
        }

        // clear sql chips
        lastSqlFilterKey = null;     // force rerender next time
        showSqlFilterTokens(null);

        // reload data like your Apply/Reset does :contentReference[oaicite:2]{index=2}
        paginationControls.resetToFirstPage();
        refreshGrid();
    }

    protected final void showSqlFilterTokens(List<String> tokens) {
        List<String> clean = (tokens == null) ? List.of()
                : tokens.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(String::trim)
                        .toList();

        // stable key so we don't re-render if unchanged
        String key = clean.stream().collect(Collectors.joining("\n"));
        if (Objects.equals(key, lastSqlFilterKey)) return;
        lastSqlFilterKey = key;

        sqlChipContainer.removeAll();

        if (clean.isEmpty()) {
            sqlFilterBar.setVisible(false);
            return;
        }

        for (String t : clean) {
            Span chip = new Span(t);
            chip.addClassName("sql-filter-chip");

            // Option B: Lumo badge chips (nice in light)
            chip.getElement().getThemeList().add("badge");
            chip.getElement().getThemeList().add("pill");
            chip.getElement().getThemeList().add("small");
            chip.getElement().getThemeList().add("primary");

            // long token safe
            chip.getStyle()
                    .set("max-width", "520px")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");

            chip.getElement().setProperty("title", t);
            sqlChipContainer.add(chip);
        }

        sqlFilterBar.setVisible(true);
    }

    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        // default no-op
    }

    /**
     * Called after the grid data provider is installed. Child views may use
     * this hook for functionality that needs an active data provider.
     */
    protected void afterDataProviderConfigured() {
        // default no-op
    }

    /**
     * Maps a grid column key to a Spring Data sort property. Return null to
     * disable sorting for a computed or collection-backed column.
     */
    protected String getSortProperty(String columnKey) {
        return columnKey;
    }

    /** Configures and validates a From/To DatePicker pair. */
    protected void advanceFilterSettingDateRank(DatePicker fromDate, DatePicker toDate) {
        fromDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        fromDate.setClearButtonVisible(true);
        fromDate.setWidthFull();

        toDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
        toDate.setClearButtonVisible(true);
        toDate.setWidthFull();

        fromDate.addValueChangeListener(event -> {
            LocalDate value = event.getValue();
            toDate.setMin(value);

            if (value != null
                    && toDate.getValue() != null
                    && toDate.getValue().isBefore(value)) {
                toDate.clear();
            }
        });

        toDate.addValueChangeListener(event -> {
            LocalDate value = event.getValue();
            fromDate.setMax(value);

            if (value != null
                    && fromDate.getValue() != null
                    && fromDate.getValue().isAfter(value)) {
                fromDate.clear();
            }
        });

        fromDate.setPlaceholder(fromDate.getLabel());
        toDate.setPlaceholder(toDate.getLabel());
    }
    
    /** Override in child view if you want password confirmation. */
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        return false;
    }

    /**
     * Override in child view to show a password dialog.
     * Default = just continue.
     */
    protected void confirmPassword(SensitiveAction action, Runnable onSuccess) {
        onSuccess.run();
    }

    private void runSensitive(SensitiveAction action, Runnable onSuccess) {
        if (!requiresPasswordConfirmation(action)) {
            onSuccess.run();
            return;
        }
        confirmPassword(action, onSuccess);
    }
    
    private void requestEdit() {
        runSensitive(SensitiveAction.EDIT, this::onEdit);
    }

    private void requestDelete() {
        runSensitive(SensitiveAction.DELETE,  this::onDelete);
    }
    
    protected <E> void addAuditColumns(Grid<E> grid) {
        grid.addColumn(item -> {
            if (item instanceof AbstractEntity) {
                AbstractEntity entity = (AbstractEntity) item;
                return entity.getUserCreated() != null ? entity.getUserCreated().getName() : "";
            }
            return "";
        }).setHeader("Created By | បង្កើតដោយ")
          .setAutoWidth(true)
          .setResizable(true);
        
        grid.addColumn(item -> {
            if (item instanceof AbstractEntity) {
                AbstractEntity entity = (AbstractEntity) item;
                return entity.getCreatedAt() != null ? 
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entity.getCreatedAt()) : "";
            }
            return "";
        }).setHeader("Created At | ថ្ងៃបង្កើត")
          .setAutoWidth(true)
          .setResizable(true);
        
        grid.addColumn(item -> {
            if (item instanceof AbstractEntity) {
                AbstractEntity entity = (AbstractEntity) item;
                return entity.getUserUpdated() != null ? entity.getUserUpdated().getName() : "";
            }
            return "";
        }).setHeader("Updated By | កែប្រែដោយ")
          .setAutoWidth(true)
          .setResizable(true);
        
        grid.addColumn(item -> {
            if (item instanceof AbstractEntity) {
                AbstractEntity entity = (AbstractEntity) item;
                return entity.getUpdatedAt() != null ? 
                    DateTimeUtilFormart.DATE_TIME_FORMATTER.format(entity.getUpdatedAt()) : "";
            }
            return "";
        }).setHeader("Updated At | ថ្ងៃកែប្រែ")
          .setAutoWidth(true)
          .setResizable(true);
    }
    
    public static String formatEnKh(String en, String kh) {
        boolean enBlank = (en == null || en.trim().isEmpty());
        boolean khBlank = (kh == null || kh.trim().isEmpty());

        if (enBlank && khBlank) return "";          // or "N/A"
        if (khBlank) return en.trim();
        if (enBlank) return kh.trim();
        return en.trim() + " | " + kh.trim();
    }
    
    public static String formatEmployeeLabel(Employee e) {
        if (e == null) return "";
        return formatEnKh(e.getNameEn(), e.getNameKh())+ "-" + e.getInsuranceNo().toString();
        
    }
    
    //++++++++++++++++++++++++ToggleColumn+++++++++++++++++++++++++++++++++++
    protected Grid.Column<T> addToggleColumn() {
        if (!enableToggleColumn) {
            return null;
        }
        
        Grid.Column<T> toggleColumn = grid.addColumn(createToggleDetailsRenderer())
                .setKey(toggleColumnKey)
                //.setWidth(toggleColumnWidth + "px")
                .setFlexGrow(0)
                .setFrozen(toggleColumnFrozen)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setHeader("");
        
        toggleColumn.setVisible(true);
        
        // Add to excluded keys so it never appears in column visibility menu
        //if (!excludedDefaultKeys.contains(toggleColumnKey)) {
        //    excludedDefaultKeys = new HashSet<>(excludedDefaultKeys);
        //    excludedDefaultKeys.add(toggleColumnKey);
        //}
        
        return toggleColumn;
    }
    
    protected Renderer<T> createToggleDetailsRenderer() {
        return new ComponentRenderer<>(item -> {
            Button toggleButton = new Button();
            toggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            toggleButton.getElement().setAttribute("aria-label", "Toggle details");
            
            // Set initial icon based on current state
            boolean isExpanded = grid.isDetailsVisible(item);
            toggleButton.setIcon(new Icon(isExpanded ? VaadinIcon.ANGLE_DOWN : VaadinIcon.ANGLE_RIGHT));
            toggleButton.getElement().setAttribute("aria-expanded", String.valueOf(isExpanded));
            
            // Add click listener to programmatically toggle details
            toggleButton.addClickListener(e -> {
                boolean newState = !grid.isDetailsVisible(item);
                grid.setDetailsVisible(item, newState);
                
                // Update button icon
                toggleButton.setIcon(new Icon(newState ? VaadinIcon.ANGLE_DOWN : VaadinIcon.ANGLE_RIGHT));
                toggleButton.getElement().setAttribute("aria-expanded", String.valueOf(newState));
            });
            
            return toggleButton;
        });
    }
    
    protected void injectGridSelectionStyles() {
        UI ui = UI.getCurrent();
        if (ui == null) return;

        ui.getPage().executeJs("""
            if (!document.getElementById('grid-selection-css')) {
                const style = document.createElement('style');
                style.id = 'grid-selection-css';
                style.textContent = `
                    vaadin-grid {
                        --lumo-primary-color-10pct: rgba(0, 100, 200, 0.12);
                    }
                    .selected-row {
                        background-color: var(--lumo-primary-color-10pct) !important;
                        font-weight: 500;
                    }
                    vaadin-grid::part(row) {
                        cursor: pointer;
                    }
                    vaadin-grid::part(row):hover {
                        background-color: var(--lumo-contrast-5pct);
                    }
                `;
                document.head.appendChild(style);
            }
        """);
    }
    
    protected Sort getDefaultSort() {
        return Sort.by(
                Sort.Direction.DESC,
                "updatedAt"
        );
    }
}


