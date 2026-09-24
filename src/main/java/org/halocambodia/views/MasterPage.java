package org.halocambodia.views;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.GenericService;
import org.halocambodia.services.UserService;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;

public abstract class MasterPage<T, S extends GenericService<T>> extends VerticalLayout{
	
	
	protected final ConfirmDialog dialogDelete = new ConfirmDialog();
	protected final Dialog dialogAdvanceSearch = new Dialog();
	protected final Dialog dialogInputQuickSearch = new Dialog();
	
	//Tool Bar
    // Left Tool Bar
    protected final Button btnAddNew = new Button("Add new", new Icon(VaadinIcon.PLUS),e->addNew());
    protected final Button btnEdit = new Button("Edit", new Icon(VaadinIcon.EDIT),e->edit());
    protected final Button btnRefresh = new Button("Refresh", new Icon(VaadinIcon.REFRESH),e ->refreshGrid());
    protected final Button btnDelete = new Button("Delete", new Icon(VaadinIcon.TRASH),e->{dialogDelete.open();});
    

    // Right Tool Bar
    protected final TextField txtQuick = new TextField();
    protected final Button btnFilter = new Button("Search", new Icon(VaadinIcon.FILTER),e ->{refreshGrid();});
    protected final Button btnClearFilter = new Button("Clear", new Icon(VaadinIcon.CLOSE),e->{clearSearchFilters();refreshGrid();});
    protected final Button btnAdvanceSearch =new Button("Advance Search",new Icon(VaadinIcon.FILTER),e->{	dialogAdvanceSearch.open();});
    
    HorizontalLayout toolbarLayout;
    protected  Grid<T> grid = new Grid<>();
	protected final Div editorLayout = new Div();
    
	protected final S service;
	protected T entity;
	
	//Filter Message
	private  Div displayAdvanceFilterGroup = new Div();
	private Span filterMessage=new Span();
	
	protected final UserService userService; 
	protected final AuthenticatedUser authenticatedUser;
    
	
	public MasterPage(S service,UserService userService,AuthenticatedUser authenticatedUser) {
		 this.service = service;
	     this.userService=userService;
	     this.authenticatedUser=authenticatedUser;
	     
		 setHeightFull();
		 
	     add(createToolBarLayout(), initializeFilterMessage(), createContentLayout());
	         
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
	

	private HorizontalLayout createToolBarLayout() {
        // Main layout
         toolbarLayout = new HorizontalLayout();
        toolbarLayout.setWidthFull();
        toolbarLayout.getElement().getStyle().set("border-radius", "20px");
        toolbarLayout.getElement().getStyle().set("background-color", "#f0f0f0");
        toolbarLayout.getElement().getStyle().set("padding", "10px");
        
        toolbarLayout.getElement().getStyle().set("position", "sticky");
        toolbarLayout.getElement().getStyle().set("z-index", "100");
        toolbarLayout.getElement().getStyle().set("top", "100");
        toolbarLayout.getElement().getStyle().set("box-shadow", "0 2px 5px rgba(0, 0, 0, 0.5)");
        
        
        

        // Menu Bar
        MenuBar menuBar = new MenuBar();
        menuBar.addClassName("menu-bar");

        // Add menu items
        MenuItem addItem = menuBar.addItem(btnAddNew.getText(), e -> btnAddNew.click());
        MenuItem editItem = menuBar.addItem(btnEdit.getText(), e -> btnEdit.click());
        MenuItem deleteItem = menuBar.addItem(btnDelete.getText(), e -> btnDelete.click());
        MenuItem refreshItem = menuBar.addItem(btnRefresh.getText(), e -> btnRefresh.click());
        MenuItem searchItem = menuBar.addItem("Quick Search",e -> dialogInputQuickSearch.open());
        MenuItem advanceSearchItem = menuBar.addItem(btnAdvanceSearch.getText(),e -> btnAdvanceSearch.click());

        // Standard toolbar for larger screens
        HorizontalLayout leftToolbar = new HorizontalLayout(btnAddNew, btnEdit, btnDelete, btnRefresh);
        leftToolbar.addClassName("toolbar-left");

        // Right toolbar with the quick search field
        HorizontalLayout rightToolbar = new HorizontalLayout(txtQuick, btnFilter, btnClearFilter,btnAdvanceSearch,exportToCsvFile());
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
	private SplitLayout createContentLayout() {
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSplitterPosition(80);
        splitLayout.setSizeFull();

        //grid.setHeightFull();
        grid.setSizeFull();
        //grid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS,GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT,GridVariant.LUMO_COLUMN_BORDERS);
        
        grid.setSelectionMode(SelectionMode.MULTI);
        splitLayout.addToPrimary(grid);

        editorLayout.addClassName("editor-layout");
        editorLayout.setSizeFull();
        splitLayout.addToSecondary(editorLayout);

       editorLayout.setVisible(false); // Initially hidden
        return splitLayout;
    }
	
	
	private void clearSearchFilters() {
		// TODO Auto-generated method stub
		
	}

	private Object refreshGrid() {
		// TODO Auto-generated method stub
		return null;
	}

	private Object edit() {
		// TODO Auto-generated method stub
		return null;
	}

	private Object addNew() {
		// TODO Auto-generated method stub
		return null;
	}
	protected abstract void configureGrid();
	
    protected abstract Component exportToCsvFile();
}
