package org.halocambodia.component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
//import elemental.json.JsonObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@CssImport("./styles/responsive-toolbar.css")
@CssImport("./styles/pagination-controls.css")
public class PaginationControls extends HorizontalLayout {
    
    private ComboBox<String> responsivePageSize;
    private Grid<?> grid;

    // Use icons only for small screens
    private final Button btnFirst = new Button(new Icon(VaadinIcon.ANGLE_DOUBLE_LEFT));
    private final Button btnPrev  = new Button(new Icon(VaadinIcon.ANGLE_LEFT));
    private final Button btnNext  = new Button(new Icon(VaadinIcon.ANGLE_RIGHT));
    private final Button btnLast  = new Button(new Icon(VaadinIcon.ANGLE_DOUBLE_RIGHT));

    private final IntegerField pageField = new IntegerField();
    private final Span pageInfo = new Span();
    private final Span ofText = new Span("of");

    private final ComboBox<Integer> pageSize = new ComboBox<>();
    private final Span rangeInfo = new Span();

    private long total = 0L;
    private int currentPage = 0;
    private int size = 25;
    private boolean showAll = false;

    // Track responsive state
    private boolean isMobileMode = false;
    private final AtomicBoolean responsiveSetupDone = new AtomicBoolean(false);

    private static final int ALL_RECORDS_PAGE_SIZE = 5000;

    public PaginationControls() {
        setWidthFull();
        setPadding(false);
        setSpacing(true);
        setAlignItems(FlexComponent.Alignment.CENTER);
        getStyle().set("gap", "0.4rem");
        
        addClassName("pagination-controls");
        btnFirst.addClassName("pagination-btn");
        btnPrev.addClassName("pagination-btn");
        btnNext.addClassName("pagination-btn");
        btnLast.addClassName("pagination-btn");
        pageField.addClassName("pagination-page-field");
        pageSize.addClassName("pagination-page-size");
        rangeInfo.addClassName("pagination-range-info");

        addTooltip(btnFirst, "First page | ទៅទំព័រដំបូង");
        addTooltip(btnPrev,  "Previous page | ទៅទំព័រមុន");
        addTooltip(btnNext,  "Next page | ទៅទំព័របន្ទាប់");
        addTooltip(btnLast,  "Last page | ទៅទំព័រចុងក្រោយ");

        List.of(btnFirst, btnPrev, btnNext, btnLast).forEach(b -> {
            b.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            b.getElement().setAttribute("aria-label", "Navigation button");
        });

        // Page field
        pageField.setMin(1);
        pageField.setStepButtonsVisible(false);
        pageField.setWidth("70px"); // Slightly wider for better touch targets
        pageField.setSuffixComponent(ofText);
        
        pageField.addKeyDownListener(Key.ENTER, e -> goToPageFromField());
        pageField.addBlurListener(e -> goToPageFromField());
        pageField.addValueChangeListener(e -> {
            if (!e.isFromClient()) return;
            Integer v = e.getValue();
            if (v == null) return;
            navigateTo(v - 1);
        });

        pageField.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        // Desktop page size combobox
        pageSize.setItems(List.of( 25, 50, 100, 200, 500, 1000,2000,5000, -1));
        pageSize.setValue(size);
        pageSize.setPlaceholder("Size");
        pageSize.setClearButtonVisible(false);
        pageSize.setAllowCustomValue(true);
        pageSize.setWidth("90px");
        
        pageSize.setItemLabelGenerator(item -> {
            if (item == -1) {
                return "All";
            }
            return String.valueOf(item);
        });

        pageSize.addValueChangeListener(e -> {
            Integer v = e.getValue();
            if (v == null) return;

            if (v == -1) {
                showAll = true;
                size = ALL_RECORDS_PAGE_SIZE;
                Notification.show("Showing all records with virtual scrolling", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
            } else {
                showAll = false;
                size = Math.max(1, v);
            }
            
            if (grid != null) {
                // For "All" mode, use a page size that matches Vaadin's limits
                grid.setPageSize(showAll ? 500 : size);
            }

            currentPage = 0;
            navigateTo(currentPage);
            
            // Sync with mobile combobox if it exists
            if (responsivePageSize != null) {
                String mobileValue = showAll ? "All" : String.valueOf(size);
                responsivePageSize.setValue(mobileValue);
            }
        });

        pageSize.addCustomValueSetListener(e -> {
            String text = e.getDetail();
            if ("All".equalsIgnoreCase(text.trim())) {
                pageSize.setValue(-1);
                return;
            }
            
            Integer v = parsePositiveInt(text);
            if (v == null) {
                pageSize.setValue(showAll ? -1 : size);
                return;
            }

            if (!pageSize.getListDataView().getItems().anyMatch(i -> i.equals(v))) {
                pageSize.getListDataView().addItem(v);
            }

            pageSize.setValue(v);
            size = v;
            showAll = false;

            if (grid != null) grid.setPageSize(size);
            currentPage = 0;
            navigateTo(currentPage);
        });

        pageSize.addThemeVariants(ComboBoxVariant.LUMO_SMALL);

        // Navigation buttons
        btnFirst.addClickListener(e -> navigateTo(0));
        btnPrev.addClickListener(e -> navigateTo(currentPage - 1));
        btnNext.addClickListener(e -> navigateTo(currentPage + 1));
        btnLast.addClickListener(e -> navigateTo(getMaxPageIndex()));

        // Initial layout
        add(btnFirst, btnPrev,
            pageField, pageInfo,
            btnNext, btnLast,
            pageSize,
            rangeInfo);

        updateUi();
    }
    
    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        // Setup responsive mode after UI is attached
        setupResponsiveMode();
        
        // Add window resize listener
        UI ui = attachEvent.getUI();
        ui.getPage().addBrowserWindowResizeListener(event -> {
            if (!responsiveSetupDone.get()) return;
            
            int width = event.getWidth();
            boolean newMobileMode = width <= 768;
            
            if (newMobileMode != isMobileMode) {
                isMobileMode = newMobileMode;
                ui.access(() -> updateResponsiveLayout());
            }
        });
    }
    
    private void setupResponsiveMode() {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            int screenWidth = details.getScreenWidth();
            isMobileMode = screenWidth <= 768;
            
            getUI().ifPresent(ui -> ui.access(() -> {
                updateResponsiveLayout();
                responsiveSetupDone.set(true);
            }));
        });
    }
    
    private void updateResponsiveLayout() {
        if (isMobileMode) {
            // Switch to mobile layout
            if (pageSize.getParent().isPresent()) {
                remove(pageSize);
            }
            if (rangeInfo.getParent().isPresent()) {
                remove(rangeInfo);
            }
            
            if (responsivePageSize == null) {
                responsivePageSize = new ComboBox<>();
                responsivePageSize.setItems("25", "50", "100", "All");
                responsivePageSize.setValue(showAll ? "All" : String.valueOf(size));
                responsivePageSize.setWidth("70px");
                responsivePageSize.addThemeVariants(ComboBoxVariant.LUMO_SMALL);
                responsivePageSize.setPlaceholder("Size");
                
                responsivePageSize.addValueChangeListener(e -> {
                    String val = e.getValue();
                    if (val == null) return;
                    
                    if ("All".equals(val)) {
                        showAll = true;
                        size = ALL_RECORDS_PAGE_SIZE;
                    } else {
                        showAll = false;
                        size = Integer.parseInt(val);
                    }
                    
                    // Sync with desktop combobox
                    pageSize.setValue(showAll ? -1 : size);
                    
                    if (grid != null) grid.setPageSize(size);
                    currentPage = 0;
                    navigateTo(currentPage);
                });
            }
            
            if (!responsivePageSize.getParent().isPresent()) {
                add(responsivePageSize);
            }
            
        } else {
            // Switch to desktop layout
            if (responsivePageSize != null && responsivePageSize.getParent().isPresent()) {
                remove(responsivePageSize);
            }
            
            if (!pageSize.getParent().isPresent()) {
                add(pageSize);
            }
            if (!rangeInfo.getParent().isPresent()) {
                add(rangeInfo);
            }
        }
    }
    
    private Integer parsePositiveInt(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            return v > 0 ? v : null;
        } catch (Exception ex) {
            return null;
        }
    }

    public void bind(Grid<?> grid) {
        this.grid = grid;
        if (showAll) {
            // Keep page size reasonable for virtual scrolling
            // Vaadin can only fetch 500 items at a time
            this.grid.setPageSize(500); // Match Vaadin's DataCommunicator limit
        } else {
            this.grid.setPageSize(size);
        }
        updateUi();
    }

    public void setTotal(long total) {
        this.total = Math.max(0, total);
        if (currentPage > getMaxPageIndex()) currentPage = getMaxPageIndex();
        updateUi();
    }

    private void goToPageFromField() {
        Integer v = pageField.getValue();
        if (v == null) {
            pageField.setValue(currentPage + 1);
            return;
        }
        navigateTo(v - 1);
    }

    private void navigateTo(int pageIndex) {
        currentPage = clamp(pageIndex, 0, getMaxPageIndex());
        if (grid == null) {
            updateUi();
            return;
        }

        if (showAll) {
            currentPage = 0;
        }
        
        if (!showAll) {
            grid.scrollToIndex(0);
        }
        
        grid.getDataProvider().refreshAll();
        updateUi();
    }

    private int getMaxPageIndex() {
        if (showAll || total <= 0 || size == 0) return 0;
        int maxPageIndex = (int) ((total - 1) / size);
        return Math.max(0, maxPageIndex);
    }

    private void updateUi() {
        int pageCount = showAll ? 1 : Math.max(1, getMaxPageIndex() + 1);

        pageField.setMax(pageCount);
        pageField.setEnabled(total > 0 && !showAll);

        int safePage = (total == 0 || showAll) ? 0 : Math.max(0, Math.min(currentPage, getMaxPageIndex()));

        pageField.setValue(safePage + 1);
        pageInfo.setText(String.valueOf(pageCount));

        long start, end;
        if (showAll) {
            start = (total == 0) ? 0 : 1;
            end = total;
        } else {
            start = (total == 0) ? 0 : Math.max(1L, (long) safePage * size + 1);
            end = (total == 0) ? 0 : Math.min(total, (long) (safePage + 1) * size);
        }
        
        if (total == 0) {
            rangeInfo.setText("No records");
        } else {
            rangeInfo.setText(start + "–" + end + " / " + total);
        }

        boolean hasData = total > 0;
        boolean paginationEnabled = hasData && !showAll;
        
        btnFirst.setEnabled(paginationEnabled && safePage > 0);
        btnPrev.setEnabled(paginationEnabled && safePage > 0);
        btnNext.setEnabled(paginationEnabled && safePage < getMaxPageIndex());
        btnLast.setEnabled(paginationEnabled && safePage < getMaxPageIndex());
        
        if (showAll) {
            pageField.setValue(1);
            pageInfo.setText("1");
        }
        
        // Sync mobile combobox if it exists
        if (responsivePageSize != null && !responsivePageSize.isReadOnly()) {
            responsivePageSize.setValue(showAll ? "All" : String.valueOf(size));
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public int getCurrentPageIndex() {
        return currentPage;
    }

    public int getPageSizeValue() {
        return showAll ? -1 : size;
    }
    
    public boolean isShowingAll() {
        return showAll;
    }

    public void resetToFirstPage() {
        currentPage = 0;
        if (grid != null) {
            if (!showAll) {
                grid.scrollToIndex(0);
            }
            grid.getDataProvider().refreshAll();
        }
        updateUi();
    }
    
    private void addTooltip(Button button, String text) {
        Tooltip tip = Tooltip.forComponent(button);
        tip.setText(text);
        tip.setPosition(TooltipPosition.TOP);
        tip.setHideDelay(200);
    }
    
    // Public method to manually trigger responsive update (optional)
    public void refreshResponsiveLayout() {
        if (getUI().isPresent()) {
            getUI().get().access(this::updateResponsiveLayout);
        }
    }
}