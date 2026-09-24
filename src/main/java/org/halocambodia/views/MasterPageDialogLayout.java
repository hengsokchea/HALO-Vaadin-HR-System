package org.halocambodia.views;

import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.GenericService;
import org.halocambodia.services.UserService;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
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
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datepicker.DatePickerVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.vaadin.lineawesome.LineAwesomeIcon;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;
import java.util.function.Consumer;




public abstract class MasterPageDialogLayout<T, S extends GenericService<T>> extends VerticalLayout implements BeforeEnterObserver{
	protected enum SensitiveAction { EDIT, DELETE }

	protected  Grid<T> grid = new Grid<>();
	    
	//protected final Dialog editorLayout = new Dialog();
	protected  CustomDialog editorLayout = new CustomDialog("");
	    
	protected final S service;
	protected T entity;
	    
	protected final ConfirmDialog dialogDelete = new ConfirmDialog();
	    
	protected final UserService userService; 
	protected final AuthenticatedUser authenticatedUser;
	    
	//advance filter
	protected final Dialog dialogAdvanceSearch = new Dialog();
	protected final FormLayout advanceSearchLayout=new FormLayout();
	Button btnApplyAdvnaceFilter = new Button("Filter", e->{this.refreshGrid(); dialogAdvanceSearch.close();});
	Button btnClearAdvanceFilter = new Button("Clear", e -> {this.clearSearchFilters(); refreshGrid(); dialogAdvanceSearch.close();});	  
	
	protected HorizontalLayout toolbarLayout = new HorizontalLayout();
	protected HorizontalLayout leftToolbar;
	protected HorizontalLayout rightToolbar;
	
    // Left Tool Bar
    protected final Button btnAddNew = new Button("Add new", new Icon(VaadinIcon.PLUS),e->addNew());
    //protected final Button btnEdit = new Button("Edit", new Icon(VaadinIcon.EDIT),e->edit());
    protected final Button btnEdit = new Button("Edit", new Icon(VaadinIcon.EDIT), e -> requestEdit());
    
    //protected final Button btnDelete = new Button("Delete", new Icon(VaadinIcon.TRASH),e->{dialogDelete.open();});
    protected final Button btnDelete = new Button("Delete", new Icon(VaadinIcon.TRASH), e -> requestDelete());
    
    protected final Button btnRefresh = new Button("Refresh", new Icon(VaadinIcon.REFRESH),e ->refreshGrid());
    
    // Right Tool Bar
    protected final TextField txtQuick = new TextField();
    protected final Button btnFilter = new Button("Search", new Icon(VaadinIcon.FILTER),e ->{refreshGrid();});
    protected final Button btnClearFilter = new Button("Clear", new Icon(VaadinIcon.CLOSE),e->{clearSearchFilters();refreshGrid();});
    protected final Button btnAdvanceSearch =new Button("Advance Search",new Icon(VaadinIcon.FILTER),e->{	dialogAdvanceSearch.open();});
    protected final Button btnShowHideColumnGrid = new Button(VaadinIcon.GRID_H.create());

    



    protected final Dialog dialogInputQuickSearch = new Dialog();
    
    protected  VerticalLayout masterRecord = new VerticalLayout();
    
    private  Div displayAdvanceFilterGroup = new Div();
    private Span filterMessage=new Span();
    
    MenuBar menuBar = new MenuBar();
    //menuBar.addClassName("menu-bar");
    MenuItem addItem = menuBar.addItem(btnAddNew.getText(), e -> btnAddNew.click());
    MenuItem editItem = menuBar.addItem(btnEdit.getText(), e -> btnEdit.click());
    MenuItem deleteItem = menuBar.addItem(btnDelete.getText(), e -> btnDelete.click());
    MenuItem refreshItem = menuBar.addItem(btnRefresh.getText(), e -> btnRefresh.click());
    MenuItem searchItem = menuBar.addItem("Quick Search",e -> dialogInputQuickSearch.open());
    MenuItem advanceSearchItem = menuBar.addItem(btnAdvanceSearch.getText(),e -> btnAdvanceSearch.click());
    
    
    public MasterPageDialogLayout(S service,UserService userService,AuthenticatedUser authenticatedUser) {
    	
        this.service = service;
        this.userService=userService;
        this.authenticatedUser=authenticatedUser;
        setHeightFull();
       
        
        // Add the wrapper to the layout
        add(masterRecord,createToolBarLayout(), initializeFilterMessage(), grid);
        masterRecord.setVisible(false);
        createContentLayout();


        initializeDialogDelete();

        // Fetch data for the grid
        fetchData();
             
        // Add selection listener for grid actions
        grid.addSelectionListener(event -> {
            Set<T> selectedItems = event.getAllSelectedItems();
            btnDelete.setEnabled(!selectedItems.isEmpty());
            btnEdit.setEnabled(selectedItems.size() == 1);
            
            deleteItem.setEnabled(!selectedItems.isEmpty());
            editItem.setEnabled(selectedItems.size() == 1);
           
        });
     
        initializeDialogAdvanceSearch();
       
    }

   private Div initializeFilterMessage (){
	   Button clearButton = new Button(VaadinIcon.CLOSE_SMALL.create(),e->{this.clearSearchFilters(); this.refreshGrid();});
       clearButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_TERTIARY_INLINE);
       clearButton.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
       // Accessible button name
       clearButton.getElement().setAttribute("aria-label", "Clear filter");
       // Tooltip
       clearButton.getElement().setAttribute("title", "Clear filter");

       // Create the "Test" text span
      
       filterMessage.getStyle().set("overflow", "hidden");
       filterMessage.getStyle().set("white-space", "normal"); // Allow text to wrap
       filterMessage.getStyle().set("text-overflow", "ellipsis");
       filterMessage.getStyle().set("padding-right", "30px"); // Leave space for the button

       // Wrap testText and clearButton in a RelativeLayout
      
       displayAdvanceFilterGroup.getStyle().set("position", "relative");
       displayAdvanceFilterGroup.getStyle().set("background-color", "var(--lumo-error-color-50pct)"); // Warning background color
       //displayAdvanceFilterGroup.getStyle().set("background-color", "var(--lumo-success-color-50pct)");
       displayAdvanceFilterGroup.getStyle().set("padding", "var(--lumo-space-s)"); // Add padding
       displayAdvanceFilterGroup.getStyle().set("border-radius", "var(--lumo-border-radius-s)"); // Add rounded corners
       displayAdvanceFilterGroup.setWidthFull(); // Full width container

       // Add testText to the wrapper
       displayAdvanceFilterGroup.add(filterMessage);

       // Positions the clearButton absolutely within the wrapper
       clearButton.getStyle().set("position", "absolute");
       clearButton.getStyle().set("top", "50%"); // Center vertically
       clearButton.getStyle().set("right", "0"); // Align to the right
       clearButton.getStyle().set("transform", "translateY(-50%)"); // Adjust for vertical alignment
       displayAdvanceFilterGroup.add(clearButton);
       displayAdvanceFilterGroup.setVisible(false);
       
	   return displayAdvanceFilterGroup;
   }
   protected void showHideAdvanceFilter(String message) {
	   
	   if (message == null || message.trim().isEmpty()) {
		  
		   displayAdvanceFilterGroup.setVisible(false);
		   filterMessage.setText("");
	   }else {
		   filterMessage.setText(message.trim());
		   displayAdvanceFilterGroup.setVisible(true);
		  
	   }
   }
	private void initializeDialogAdvanceSearch() {
		
		dialogAdvanceSearch.setHeaderTitle("Advance Search");
		dialogAdvanceSearch.setDraggable(true);
		dialogAdvanceSearch.setResizable(true);
		
		 Button minimizeButton = new Button(new Icon(VaadinIcon.MINUS), e -> {
			 dialogAdvanceSearch.setWidth("50%"); 
			 dialogAdvanceSearch.setHeight("50%");
			 dialogAdvanceSearch.setTop("25%");
			 dialogAdvanceSearch.setLeft("25%");
			
			 });

	     Button fullScreenButton = new Button(new Icon(VaadinIcon.EXPAND_FULL), e -> {
	    	 dialogAdvanceSearch.setSizeFull();
			 dialogAdvanceSearch.setTop("0%");
			 dialogAdvanceSearch.setLeft("0%");
	     });

	     Button closeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL), e -> dialogAdvanceSearch.close());
	     closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
	     closeButton.setTooltipText("Close the Advance Search");

	     dialogAdvanceSearch.getHeader().add(minimizeButton, fullScreenButton, closeButton);
		
	

		advanceSearchLayout.setResponsiveSteps(
	        // Use one column by default
	        new ResponsiveStep("0", 1),
	        // Use two columns, if the layout's width exceeds 320px
	        new ResponsiveStep("250px", 2),
	        // Use three columns, if the layout's width exceeds 500px
	        new ResponsiveStep("500px", 3),
	        new ResponsiveStep("750px", 4),
	        new ResponsiveStep("1000px", 5),
	        new ResponsiveStep("1250px", 6),
	        new ResponsiveStep("1500px", 7)
		);
		
		
		advanceSearchLayout.getElement().getStyle().set("width", "60vw"); // 50% of the viewport width
		//advanceSearchLayout.getElement().getStyle().set("min-width", "300px");
		//advanceSearchLayout.getElement().getStyle().set("max-width", "90vw");
		
		
		
		advanceSearchLayout.getElement().getStyle().set("height", "70vh"); // 50% of the viewport height
		advanceSearchLayout.getElement().getStyle().set("overflow", "auto"); // Handle overflow if necessary

				
		dialogAdvanceSearch.add(advanceSearchLayout);
		

		btnApplyAdvnaceFilter.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
		btnApplyAdvnaceFilter.addClickShortcut(Key.ENTER);

		btnClearAdvanceFilter.addThemeVariants( ButtonVariant.LUMO_ERROR);
		
		dialogAdvanceSearch.getFooter().add(btnClearAdvanceFilter);
		dialogAdvanceSearch.getFooter().add(btnApplyAdvnaceFilter);
		
		

	}

	private void createContentLayout() {
       
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT,GridVariant.LUMO_COLUMN_BORDERS);
       // grid.setSelectionMode(SelectionMode.MULTI);
        
        GridMultiSelectionModel<T> sm = (GridMultiSelectionModel<T>) grid.setSelectionMode(Grid.SelectionMode.MULTI);

        sm.setDragSelect(true);
       
     
        
        editorLayout.addClassName("editor-layout");
        editorLayout.setDraggable(true);
        editorLayout.setResizable(true);
        editorLayout.setCloseOnEsc(false);
        editorLayout.setCloseOnOutsideClick(false);        
        editorLayout.setWidth("90vw"); 
        editorLayout.setHeightFull();

    }

    
    private HorizontalLayout createToolBarLayout() {
    	
        // Main layout
        //HorizontalLayout toolbarLayout = new HorizontalLayout();
        toolbarLayout.setWidthFull();
        toolbarLayout.getElement().getStyle().set("border-radius", "20px");
        toolbarLayout.getElement().getStyle().set("background-color", "#f0f0f0");
        toolbarLayout.getElement().getStyle().set("padding", "10px");
        
        toolbarLayout.getElement().getStyle().set("position", "sticky");
        //toolbarLayout.getElement().getStyle().set("z-index", "100");
        toolbarLayout.getElement().getStyle().set("top", "100");
        toolbarLayout.getElement().getStyle().set("box-shadow", "0 2px 5px rgba(0, 0, 0, 0.5)");
        
        
        

        /*      // Menu Bar
        MenuBar menuBar = new MenuBar();
        menuBar.addClassName("menu-bar");

        // Add menu items
       
        MenuItem addItem = menuBar.addItem(btnAddNew.getText(), e -> btnAddNew.click());
        MenuItem editItem = menuBar.addItem(btnEdit.getText(), e -> btnEdit.click());
        MenuItem deleteItem = menuBar.addItem(btnDelete.getText(), e -> btnDelete.click());
        MenuItem refreshItem = menuBar.addItem(btnRefresh.getText(), e -> btnRefresh.click());
        MenuItem searchItem = menuBar.addItem("Quick Search",e -> dialogInputQuickSearch.open());
        MenuItem advanceSearchItem = menuBar.addItem(btnAdvanceSearch.getText(),e -> btnAdvanceSearch.click());
*/
        // Standard toolbar for larger screens
        leftToolbar = new HorizontalLayout(btnAddNew, btnEdit, btnDelete, btnRefresh);
        leftToolbar.addClassName("toolbar-left");

        // Right toolbar with the quick search field
        rightToolbar = new HorizontalLayout(txtQuick, btnFilter, btnClearFilter,btnAdvanceSearch,exportToExcelFile(),btnShowHideColumnGrid);
        rightToolbar.addClassName("toolbar-right");

        toolbarLayout.add(leftToolbar, rightToolbar, menuBar);
        toolbarLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbarLayout.addClassName("responsive-toolbar");
        
      
        

        // Drawer toggle behavior for responsiveness
        UI.getCurrent().getPage().executeJs(
            """
            const layout = $0;
            const leftToolbar = $1;
            const rightToolbar = $2;
            const menuBar = $3;
            const resizeObserver = new ResizeObserver(entries => {
                entries.forEach(entry => {
                    const width = entry.contentRect.width;
                    if (width < 768) { // Mobile/Tablet breakpoint
                        leftToolbar.style.display = 'none';
                        rightToolbar.style.display = 'none';
                        menuBar.style.display = 'flex';
                    } else {
                        leftToolbar.style.display = 'flex';
                        rightToolbar.style.display = 'flex';
                        menuBar.style.display = 'none';
                    }
                });
            });
            resizeObserver.observe(layout);
            """,
            toolbarLayout.getElement(),
            leftToolbar.getElement(),
            rightToolbar.getElement(),
            menuBar.getElement()
        );

        // MenuBar is initially hidden
        menuBar.getStyle().set("display", "none");
        
        this.configureComponentsToolBar();

        return toolbarLayout;
    }

    protected abstract Component exportToExcelFile();


	private void configureComponentsToolBar() {
    	txtQuick.setPlaceholder("Quick search...");
        txtQuick.setPrefixComponent(new Icon("lumo", "search"));
        txtQuick.setTooltipText("Search All Column");
        txtQuick.setClearButtonVisible(true);
        txtQuick.addKeyPressListener(Key.ENTER, event -> {   btnFilter.click(); });        
        txtQuick.addValueChangeListener(event -> {if (event.getValue().isEmpty()) {btnFilter.click();}});
        

        btnFilter.setTooltipText("Select the record based on the column filter and quick search");
        btnFilter.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnClearFilter.setTooltipText("Remove all filtering");
        btnClearFilter.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        btnAddNew.setTooltipText("New Item");
        btnAddNew.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        btnEdit.setTooltipText("Edit Item");
        btnEdit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_WARNING);
        btnEdit.setEnabled(false);

        btnRefresh.setTooltipText("Refresh");
        btnRefresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        

        btnDelete.setTooltipText("Delete");
        btnDelete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        btnDelete.setEnabled(false);
        
        btnAdvanceSearch.setTooltipText("Search By Columns");
        btnAdvanceSearch.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_WARNING);
        
        dialogInputQuickSearch.setHeaderTitle("Quick Search...");
        TextField dialogInputQuickSearchField=new TextField();
        dialogInputQuickSearchField.setPlaceholder("Quick search...");
        dialogInputQuickSearchField.setPrefixComponent(new Icon("lumo", "search"));
        dialogInputQuickSearchField.setClearButtonVisible(true);
        
        dialogInputQuickSearch.add(dialogInputQuickSearchField);
        Button dialogInputQuickSearchApply = new Button("Apply",e->{txtQuick.setValue(dialogInputQuickSearchField.getValue());this.refreshGrid(); dialogInputQuickSearch.close();});
        dialogInputQuickSearchApply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button dialogInputQuickSearchCancel = new Button("Cancel", e -> dialogInputQuickSearch.close());
        
        dialogInputQuickSearch.getFooter().add(dialogInputQuickSearchCancel);
        dialogInputQuickSearch.getFooter().add(dialogInputQuickSearchApply);
        
      
	    
        
    }

    // Abstract methods to be implemented in subclasses
    
    protected void fetchData() {
        grid.setDataProvider(new CallbackDataProvider<>(
            gridQuery -> {
                int offset = gridQuery.getOffset();
                int limit = gridQuery.getLimit();
                int page = offset / limit;

                // Sort logic
                List<QuerySortOrder> sortOrders = gridQuery.getSortOrders();
                Sort sort = Sort.unsorted();
                if (!sortOrders.isEmpty()) {
                    QuerySortOrder sortOrder = sortOrders.get(0);
                    String sortedField = sortOrder.getSorted();
                    sort = Sort.by(sortOrder.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC, sortedField);
                    
                    //System.out.println("Field Name:"+ sortedField);
                }else {
                	 String sortedField = "updatedAt"; // Default field
                     sort = Sort.by(Sort.Direction.DESC, sortedField);     
                }

                Pageable pageable = PageRequest.of(page, limit, sort);

                // Fetch data using the service with combined specification
                Specification<T> specification = buildCombinedSpecification();
                return service.list(pageable, specification).stream();
            },
            gridQuery -> {
                // Count the total number of records with the combined specification
                Specification<T> specification = buildCombinedSpecification();
                int totalRecords = (int) service.count(specification);

                // Update footer dynamically
                Grid.Column<T> idColumn = grid.getColumnByKey("id");
                if (idColumn != null) {
                    idColumn.setFooter("Total Records: " + totalRecords);
                }
               

                return totalRecords;
                
            }
        ));
        
    }
    protected void refreshGrid() {
	    grid.select(null);
	    grid.deselectAll();
	    grid.getDataProvider().refreshAll();
	}

    private void addNew() {
        try {
	        entity = createNewEntity(); // Initialize a new instance of the entity	        
	        populateForm(entity);
	        focusFirstField();
        } catch (Exception ex) {
            Notification.show("Error adding new record: " + ex.getMessage(), 6000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            ex.printStackTrace(); // Optional: log to console or logger
        }
    }
    
    private void edit() {
        try {
	    	 Set<T> selectedItems = grid.getSelectedItems();
	         if (selectedItems.size() == 1) {
	             entity = selectedItems.iterator().next(); // Get the selected entity
	             populateForm(entity); // Call the abstract method
	             focusFirstField(); // Optional: add a hook to focus a specific field
	         } else {
	             Notification.show("Please select a single item to edit.", 3000, Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
	         }
        } catch (Exception ex) {
            Notification.show("Error adding new record: " + ex.getMessage(), 6000, Notification.Position.TOP_CENTER) .addThemeVariants(NotificationVariant.LUMO_ERROR);
            ex.printStackTrace(); // Optional: log to console or logger
        }
    }

    
    private void initializeDialogDelete() {
        dialogDelete.setHeader("Delete?");
        dialogDelete.setText("Are you sure you want to permanently delete the selected items?");

        dialogDelete.setCancelable(true);
        dialogDelete.addCancelListener(event -> dialogDelete.close());

        dialogDelete.setConfirmText("Delete");
        dialogDelete.setConfirmButtonTheme("error primary");
        dialogDelete.addConfirmListener(event -> {
            try {
                Set<T> selectedItems = grid.getSelectedItems();
                if (!selectedItems.isEmpty()) {
                    service.delete(selectedItems);
                    Notification.show("Data deleted successfully", 3000, Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refreshGrid();
                } else {
                    Notification.show("Please select items to delete!", 3000, Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show("An unexpected error occurred: " + ex.getMessage(), 3000, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                dialogDelete.close();
            }
        });
    }
    
    protected void clearSearchFilters() {
        txtQuick.clear();
        dialogAdvanceSearch.getChildren().forEach(this::clearComponentValues);
       // clearAdvancedSearchFilters();
        
        
    }
    private void clearComponentValues(Component component) {
        if (component instanceof TextField) {
            ((TextField) component).clear();
        } else if (component instanceof ComboBox) {
            ((ComboBox<?>) component).clear();
        } else if (component instanceof MultiSelectComboBox) {
            ((MultiSelectComboBox<?>) component).clear();
        } else if (component instanceof DatePicker) {
            ((DatePicker) component).clear();
        } else if (component instanceof NumberField) {
            ((NumberField) component).clear();
        }  else if (component instanceof TextArea) {
            ((TextArea) component).clear();
        } 

        // Recursively scan child components
        component.getChildren().forEach(this::clearComponentValues);
    }
    
 // Helper method to find a field in the class or its superclasses
    protected Field getFieldFromClassHierarchy(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (field != null) {
                    return field;
                }
            } catch (NoSuchFieldException e) {
                // Move up to the superclass
                clazz = clazz.getSuperclass();
            }
        }
        return null; // Return null if the field is not found in the class hierarchy
    }
    

	//protected abstract void clearAdvancedSearchFilters(); 
	
	protected abstract void createAdvanceFilterLayout();
	
    protected abstract Specification<T> buildCombinedSpecification();
    
    protected abstract void configureGrid() throws Exception;
    
    protected abstract void configureEditorLayout() throws Exception;
    
    /**
     * Abstract method to populate the form with the selected entity's data.
     * Must be implemented in subclasses.
     */
    protected abstract void populateForm(T entity) throws Exception;

    /**
     * Optional method to focus the first field in the editor layout.
     * Subclasses can override this if needed.
     */
    protected void focusFirstField() {
        // Default implementation: do nothing
    }
    protected abstract T createNewEntity() throws Exception;
    

    protected void createShowHideColumnGridToolBar() {

        btnShowHideColumnGrid.addThemeVariants(ButtonVariant.LUMO_ICON);
        btnShowHideColumnGrid.setAriaLabel("Show / hide columns");
        btnShowHideColumnGrid.setTooltipText("Show / hide columns");

        Popover popover = new Popover();
        popover.setModal(true);
        popover.setBackdropVisible(true);
        popover.setPosition(PopoverPosition.BOTTOM_END);
        popover.setTarget(btnShowHideColumnGrid);

        Div heading = new Div("Configure columns");
        heading.getStyle().set("font-weight", "600");
        heading.getStyle().set("padding", "var(--lumo-space-xs)");

        CheckboxGroup<String> chkShowHideColumnGrid = new CheckboxGroup<>();
        chkShowHideColumnGrid.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);

        // Step 1: Get columns with non-null keys
        List<Grid.Column<T>> orderedColumns = grid.getColumns().stream()
            .filter(col -> col.getKey() != null)
            .toList();

        // Step 2: Map key -> column and key -> header
        LinkedHashMap<String, Grid.Column<?>> columnMap = new LinkedHashMap<>();
        Map<String, String> columnHeaders = new HashMap<>();

        for (Grid.Column<?> col : orderedColumns) {
            String key = col.getKey();
            columnMap.put(key, col);

            String header = col.getHeaderText() != null ? col.getHeaderText() : "";
            columnHeaders.put(key, header);
        }

        List<String> orderedKeys = new ArrayList<>(columnMap.keySet());
        chkShowHideColumnGrid.setItems(orderedKeys);

        // Use the column header text as label
        chkShowHideColumnGrid.setItemLabelGenerator(key -> columnHeaders.getOrDefault(key, key));

        // Columns to exclude (matching header)
        Set<String> excludedHeaders = Set.of("Created By", "Created At", "Updated By", "Updated At");

        // Default value (same as reset)
        Set<String> defaultVisibleKeys = orderedKeys.stream()
            .filter(key -> !excludedHeaders.contains(columnHeaders.getOrDefault(key, "")))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        chkShowHideColumnGrid.setValue(defaultVisibleKeys);

        // Apply initial visibility
        columnMap.forEach((key, column) -> column.setVisible(defaultVisibleKeys.contains(key)));

        // On change: show/hide columns
        chkShowHideColumnGrid.addValueChangeListener(event -> {
            Set<String> selectedKeys = event.getValue();
            columnMap.forEach((key, column) -> column.setVisible(selectedKeys.contains(key)));
        });

        // Show all button
        Button showAll = new Button("Show all", e -> {
            chkShowHideColumnGrid.setValue(new LinkedHashSet<>(orderedKeys));
        });
        showAll.addThemeVariants(ButtonVariant.LUMO_SMALL);

        // Reset button (hide audit columns)
        Button reset = new Button("Reset", e -> {
            Set<String> filteredKeys = orderedKeys.stream()
                .filter(key -> !excludedHeaders.contains(columnHeaders.getOrDefault(key, "")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            chkShowHideColumnGrid.setValue(filteredKeys);
        });
        reset.addThemeVariants(ButtonVariant.LUMO_SMALL);

        HorizontalLayout footer = new HorizontalLayout(showAll, reset);
        footer.setSpacing(true);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        popover.add(heading, chkShowHideColumnGrid, footer);
    }
    
    protected <T> void advanceFilterSettingComboBox(ComboBox<T> comboBox) {
        comboBox.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();
        comboBox.setPlaceholder("Filter by "+ comboBox.getLabel());
    }
    
    protected <T> void advanceFilterSettingMultiSelectComboBox(MultiSelectComboBox<T> comboBox) {
        comboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        comboBox.setAutoExpand(AutoExpandMode.BOTH);
        comboBox.setClearButtonVisible(true);
        comboBox.setWidthFull();  
        comboBox.setPlaceholder("Filter by "+ comboBox.getLabel());
    }

    protected void advanceFilterSettingEmailField(EmailField num) {
		num.addThemeVariants(TextFieldVariant.LUMO_SMALL);
		num.setClearButtonVisible(true);
		num.setWidthFull();
		num.setPlaceholder("Filter by "+ num.getLabel());
	}	    
    protected void advanceFilterSettingTextField(TextField num) {
		num.addThemeVariants(TextFieldVariant.LUMO_SMALL);
		num.setClearButtonVisible(true);
		num.setWidthFull();
		num.setPlaceholder("Filter by "+ num.getLabel());
	}
    protected void advanceFilterSettingNumberField(NumberField num) {
		num.addThemeVariants(TextFieldVariant.LUMO_SMALL);
		num.setClearButtonVisible(true);
		num.setWidthFull();
		num.setPlaceholder("Filter by "+ num.getLabel());
	}
    protected void advanceFilterSettingDateRank(DatePicker fromDate,DatePicker toDate) {
		fromDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
		fromDate.setClearButtonVisible(true);
		toDate.addThemeVariants(DatePickerVariant.LUMO_SMALL);
		toDate.setClearButtonVisible(true);
    	fromDate.addValueChangeListener(e -> toDate.setMin(e.getValue()));
    	toDate.addValueChangeListener(e -> fromDate.setMax(e.getValue()));
    	
    	fromDate.setPlaceholder( fromDate.getLabel());    	
    	toDate.setPlaceholder( toDate.getLabel());
		
	}
    
    protected void showError(String message) {
		    Notification.show(message, 9000, Position.TOP_CENTER) .addThemeVariants(NotificationVariant.LUMO_ERROR);
	}
    
    protected void showSuccess(String message) {
	    Notification.show(message, 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    protected <T> void configureComboBox(ComboBox<T> comboBox, String placeholder, ItemLabelGenerator<T> labelGenerator) {
		 comboBox.setClearButtonVisible(true);
		 if(placeholder!=null) {
			 comboBox.setPlaceholder(placeholder);
		 }		    
		 if (labelGenerator != null) {
		     comboBox.setItemLabelGenerator(labelGenerator);
		 }
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
        runSensitive(SensitiveAction.EDIT, this::editInternal);
    }

    private void requestDelete() {
        runSensitive(SensitiveAction.DELETE, () -> dialogDelete.open());
    }

    // moved from your old private edit() logic
    private void editInternal() {
        try {
            Set<T> selectedItems = grid.getSelectedItems();
            if (selectedItems.size() == 1) {
                entity = selectedItems.iterator().next();
                populateForm(entity);
                focusFirstField();
            } else {
                Notification.show("Please select a single item to edit.", 3000, Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception ex) {
            Notification.show("Error editing record: " + ex.getMessage(), 6000, Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            ex.printStackTrace();
        }
    }
    public static String formatEnKh(String en, String kh) {
        boolean enBlank = (en == null || en.trim().isEmpty());
        boolean khBlank = (kh == null || kh.trim().isEmpty());

        if (enBlank && khBlank) return "";          // or "N/A"
        if (khBlank) return en.trim();
        if (enBlank) return kh.trim();
        return en.trim() + " | " + kh.trim();
    }
	
}
