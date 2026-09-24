package org.halocambodia.views;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.halocambodia.data.ReportService;
import org.halocambodia.utility.ReportExecutorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

/**
 * PreviewReport - Enhanced report preview dialog with full feature set
 */
public class PreviewReport extends CustomDialog {
    
    private static final Logger log = LoggerFactory.getLogger(PreviewReport.class);
    
    // Constants
    private static final String DEFAULT_WIDTH = "90%";
    private static final String DEFAULT_HEIGHT = "90%";
    private static final String DEFAULT_TITLE_EN = "Report Preview";
    private static final String DEFAULT_TITLE_KH = "មើលរបាយការណ៍";
    
    // State variables
    private double currentZoom = 1.0;
    private final AtomicBoolean released = new AtomicBoolean(false);
    private StreamRegistration streamRegistration;
    private String currentReportUrl;
    private final String reportPath;
    private final HashMap<String, Object> params;
    private final String titleEn;
    private final String titleKh;
    private final String downloadFileName;
    private final boolean openInNewWindow;
    
    // UI Components
    private IFrame frame;
    private VerticalLayout loadingLayout;
    private VerticalLayout contentLayout;
    private ProgressBar progressBar;
    private Span loadingText;
    private Span zoomDisplay;
    private Span statusText;
    private Button zoomInBtn;
    private Button zoomOutBtn;
    private Button fitWidthBtn;
    private Button printBtn;
    private Button downloadBtn;
    private Button openBtn;
    private Div zoomContainer;
    
    // Callbacks
    private Consumer<byte[]> onReportGenerated;
    private Runnable onReportError;
    
    /**
     * Constructor with default title and filename
     */
    public PreviewReport(String reportPath, HashMap<String, Object> params) {
        this(reportPath, params, false, DEFAULT_TITLE_EN, DEFAULT_TITLE_KH, null);
    }
    
    /**
     * Constructor with openInNewWindow flag
     */
    public PreviewReport(String reportPath, HashMap<String, Object> params, boolean openInNewWindow) {
        this(reportPath, params, openInNewWindow, DEFAULT_TITLE_EN, DEFAULT_TITLE_KH, null);
    }
    
    /**
     * Preview an already-generated PDF using the same report viewer used by
     * Jasper-based reports. This is useful when a service already produced
     * the final immutable PDF bytes (for example, payroll payslips).
     */
    public PreviewReport(byte[] pdfData, String titleEn, String titleKh, String downloadFileName) {
        super(localizedTitle(titleEn, titleKh));

        this.reportPath = null;
        this.params = new HashMap<>();
        this.openInNewWindow = false;
        this.titleEn = titleEn != null ? titleEn : DEFAULT_TITLE_EN;
        this.titleKh = titleKh != null ? titleKh : DEFAULT_TITLE_KH;
        this.downloadFileName = downloadFileName != null ? downloadFileName : "Report.pdf";

        configurePreviewDialog();
        buildUI();

        setLoadingState(true);
        updateStatus("Loading report... | កំពុងផ្ទុករបាយការណ៍...");
        handleReportGenerated(pdfData, this::displayReport);
    }

    /**
     * Full constructor
     */
    public PreviewReport(String reportPath, HashMap<String, Object> params, 
                         boolean openInNewWindow, String titleEn, String titleKh, 
                         String downloadFileName) {
        super(localizedTitle(titleEn, titleKh));

        this.reportPath = reportPath;
        this.params = params != null ? params : new HashMap<>();
        this.openInNewWindow = openInNewWindow;
        this.titleEn = titleEn != null ? titleEn : DEFAULT_TITLE_EN;
        this.titleKh = titleKh != null ? titleKh : DEFAULT_TITLE_KH;
        this.downloadFileName = downloadFileName != null ? downloadFileName : "Report.pdf";
        
        configurePreviewDialog();
        buildUI();
        
        
        if (openInNewWindow) {
            generateReportInBackground(url -> {
                UI.getCurrent().getPage().open(currentReportUrl, "_blank");
                close();
            });
        } else {
            generateReportInBackground(this::displayReport);
        }
    }
    
    // ======================== SETUP METHODS ========================

    /**
     * PreviewReport uses CustomDialog for the common window behavior:
     * title/header, minimize, maximize/restore, close, drag and resize.
     * This method only applies report-viewer-specific sizing.
     */
    private void configurePreviewDialog() {
        setWidth(DEFAULT_WIDTH);
        setHeight(DEFAULT_HEIGHT);
        setMaxWidth("calc(100vw - 32px)");
        setMaxHeight("calc(100dvh - 32px)");
        addClassName("preview-report-dialog");

        // Keep the report preview comfortable on small screens without
        // replacing CustomDialog's normal window controls.
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            if (details.getBodyClientWidth() < 768) {
                setWidth("calc(100vw - 16px)");
                setMaxWidth("calc(100vw - 16px)");
                setHeight("calc(100dvh - 16px)");
                setMaxHeight("calc(100dvh - 16px)");
            }
        });
    }

    private static String localizedTitle(String titleEn, String titleKh) {
        String resolvedEn = titleEn != null ? titleEn : DEFAULT_TITLE_EN;
        String resolvedKh = titleKh != null ? titleKh : DEFAULT_TITLE_KH;
        return resolvedEn + " | " + resolvedKh;
    }

    // ======================== UI BUILDING ========================
    
    private void buildUI() {
        // Build content
        buildContent();
        
        // Build footer
        HorizontalLayout footer = buildFooter();
        
        // Create main layout with content and footer (header is already in dialog header)
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.setMargin(false);
        
        // Content - expands to fill space
        VerticalLayout contentWrapper = new VerticalLayout();
        contentWrapper.setSizeFull();
        contentWrapper.setPadding(false);
        contentWrapper.setSpacing(false);
        contentWrapper.setMargin(false);
        contentWrapper.add(loadingLayout, contentLayout);
        contentWrapper.expand(contentLayout);
        contentWrapper.expand(loadingLayout);
        
        // Footer - fixed height
        footer.setWidthFull();
        footer.setFlexShrink(0);
        
        mainLayout.add(contentWrapper, footer);
        mainLayout.expand(contentWrapper);
        
        add(mainLayout);
    }
    
    private void buildContent() {
        // Loading layout
        loadingLayout = new VerticalLayout();
        loadingLayout.setSizeFull();
        loadingLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        loadingLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loadingLayout.setPadding(true);
        loadingLayout.setSpacing(true);
        loadingLayout.getStyle()
            .set("background", "var(--lumo-base-color)");
        
        // Progress bar (indeterminate)
        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("400px");
        progressBar.setHeight("20px");
        
        // Loading text
        loadingText = new Span("Loading report... | កំពុងបង្កើតរបាយការណ៍...");
        loadingText.getStyle()
            .set("font-size", "var(--lumo-font-size-m)")
            .set("color", "var(--lumo-secondary-text-color)");
        
        // Spinner icon
        Icon spinner = VaadinIcon.SPINNER.create();
        spinner.getStyle()
            .set("animation", "spin 1s linear infinite")
            .set("width", "70px")
            .set("height", "70px")
            .set("color", "var(--lumo-primary-color)");
        
        // Add CSS animation
        UI.getCurrent().getPage().executeJs(
            "if (!document.getElementById('spin-animation')) {" +
            "  const style = document.createElement('style');" +
            "  style.id = 'spin-animation';" +
            "  style.textContent = '@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }';" +
            "  document.head.appendChild(style);" +
            "}"
        );
        
        loadingLayout.add(spinner, progressBar, loadingText);
        
        // Content layout (for report display)
        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);
        contentLayout.setMargin(false);
        contentLayout.setVisible(false);
        contentLayout.getStyle()
            .set("background", "var(--lumo-base-color)");
        
        // Frame
        frame = new IFrame();
        frame.setSizeFull();
        frame.getStyle()
            .set("border", "none")
            .set("background", "var(--lumo-base-color)")
            .set("width", "100%")
            .set("height", "100%");
        
        // Zoom container
        zoomContainer = new Div(frame);
        zoomContainer.setSizeFull();
        zoomContainer.getStyle()
            .set("overflow", "auto")
            .set("display", "flex")
            .set("justify-content", "center")
            .set("align-items", "flex-start")
            .set("background", "var(--lumo-base-color)");
           // .set("padding", "10px");
        
        contentLayout.add(zoomContainer);
        contentLayout.expand(zoomContainer);
        
        // Set initial visibility
        loadingLayout.setVisible(true);
        contentLayout.setVisible(false);
    }
    
    private HorizontalLayout buildFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setPadding(false);
        footer.setSpacing(false);
        footer.setMargin(false);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.getStyle().setPaddingLeft("15px");
        footer.getStyle().setPaddingRight("15px");

        
        // Zoom controls group
        HorizontalLayout zoomGroup = new HorizontalLayout();
        zoomGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        zoomGroup.setSpacing(true);
        zoomGroup.getStyle().set("gap", "2px");
        
        zoomOutBtn = new Button(VaadinIcon.SEARCH_MINUS.create());
        zoomOutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        zoomOutBtn.setTooltipText("Zoom Out | ពង្រីកចេញ");
        zoomOutBtn.addClickListener(e -> zoomOut());
        zoomOutBtn.setEnabled(false);
        
        zoomDisplay = new Span("100%");
        zoomDisplay.getStyle()
            .set("min-width", "45px")
            .set("text-align", "center")
            .set("font-size", "var(--lumo-font-size-s)")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-weight", "500");
        
        zoomInBtn = new Button(VaadinIcon.SEARCH_PLUS.create());
        zoomInBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        zoomInBtn.setTooltipText("Zoom In | ពង្រីកចូល");
        zoomInBtn.addClickListener(e -> zoomIn());
        zoomInBtn.setEnabled(false);
        
        fitWidthBtn = new Button(VaadinIcon.RESIZE_H.create());
        fitWidthBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        fitWidthBtn.setTooltipText("Fit Width | សម្រួលទទឹង");
        fitWidthBtn.addClickListener(e -> fitWidth());
        fitWidthBtn.setEnabled(false);
        
        zoomGroup.add(zoomOutBtn, zoomDisplay, zoomInBtn, fitWidthBtn);
        
        // Separator
        Span sep1 = new Span("|");
        sep1.getStyle()
            .set("color", "var(--lumo-contrast-20pct)")
            .set("padding", "0 4px");
        
        // Action buttons group
        HorizontalLayout actionGroup = new HorizontalLayout();
        actionGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        actionGroup.setSpacing(true);
        actionGroup.getStyle().set("gap", "2px");
        
        printBtn = new Button(VaadinIcon.PRINT.create());
        printBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        printBtn.setTooltipText("Print | បោះពុម្ព");
        printBtn.addClickListener(e -> print());
        printBtn.setEnabled(false);
        
        downloadBtn = new Button(VaadinIcon.DOWNLOAD.create());
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        downloadBtn.setTooltipText("Download PDF | ទាញយក PDF");
        downloadBtn.addClickListener(e -> download());
        downloadBtn.setEnabled(false);
        
        openBtn = new Button(VaadinIcon.EXTERNAL_LINK.create());
        openBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        openBtn.setTooltipText("Open in New Window | បើកក្នុងផ្ទាំងថ្មី");
        openBtn.addClickListener(e -> openInNewWindowAction());
        openBtn.setEnabled(false);
        
        actionGroup.add(printBtn, downloadBtn, openBtn);
        
        // Spacer
        Span spacer = new Span();
        spacer.getStyle().set("flex", "1");
        
        // Status text
        statusText = new Span("Ready | រួចរាល់");
        statusText.getStyle()
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("text-align", "right")
            .set("white-space", "nowrap");
        
        footer.add( statusText,spacer,zoomGroup, sep1, actionGroup);
       
        
        return footer;
    }
    
    // ======================== REPORT GENERATION ========================
    
    private void generateReportInBackground(Consumer<String> onSuccess) {
        setLoadingState(true);
        updateStatus("Generating report... | កំពុងបង្កើតរបាយការណ៍...");
        
        UI currentUI = UI.getCurrent();
        if (currentUI == null) {
            showError("No UI context available");
            return;
        }
        
        final java.util.concurrent.ExecutorService reportExecutor;
        try {
            reportExecutor = ReportExecutorManager.executor();
        } catch (RejectedExecutionException ex) {
            log.warn("Report generation rejected because the application is stopping", ex);
            handleReportError(ex);
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        log.info("Generating report: {}", reportPath);
                        return ReportService.generateReport(reportPath, params);
                    } catch (Throwable e) {
                        log.error("Report generation failed", e);
                        throw new RuntimeException("Failed to generate report", e);
                    }
                }, reportExecutor)
                .whenComplete((pdfData, failure) -> {
                    try {
                        currentUI.access(() -> {
                            if (failure != null) {
                                handleReportError(failure);
                            } else {
                                handleReportGenerated(pdfData, onSuccess);
                            }
                        });
                    } catch (Exception ex) {
                        // The UI/session may already be gone during logout or server
                        // shutdown. Do not schedule more work on another common pool.
                        log.debug("Report completed after UI became unavailable", ex);
                    }
                });
    }
    
    private void handleReportGenerated(byte[] pdfData, Consumer<String> onSuccess) {
        if (pdfData == null || pdfData.length == 0) {
            showError("Empty report generated | របាយការណ៍ទទេ");
            setLoadingState(false);
            return;
        }
        
        log.info("Report generated: {} bytes", pdfData.length);
        
        try {
            String fileName = generateFileName();
            StreamResource resource = new StreamResource(fileName, 
                () -> new ByteArrayInputStream(pdfData));
            resource.setContentType("application/pdf");
            resource.setCacheTime(0);
            
            if (streamRegistration != null) {
                streamRegistration.unregister();
            }
            streamRegistration = VaadinSession.getCurrent()
                .getResourceRegistry()
                .registerResource(resource);
            
            currentReportUrl = streamRegistration.getResourceUri().toString();
            
            if (onSuccess != null) {
                onSuccess.accept(currentReportUrl);
            }
            
            setLoadingState(false);
            updateStatus("Report ready | របាយការណ៍រួចរាល់");
            enableControls(true);
            
            if (onReportGenerated != null) {
                onReportGenerated.accept(pdfData);
            }
            
        } catch (Exception e) {
            log.error("Error displaying report", e);
            showError("Error displaying report: " + e.getMessage());
            setLoadingState(false);
        }
    }
    
    private void handleReportError(Throwable ex) {
        log.error("Report generation error", ex);

        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String msg = root.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = root.getClass().getSimpleName();
        }

        showError("Error generating report: " + msg + " | កំហុសបង្កើតរបាយការណ៍");
        setLoadingState(false);
        
        if (onReportError != null) {
            onReportError.run();
        }
    }
    
    private String generateFileName() {
        if (downloadFileName != null && !downloadFileName.isEmpty()) {
            return downloadFileName;
        }
        
        String name = reportPath != null ? 
            reportPath.replaceAll(".*[/\\\\]", "").replaceAll("\\.[^.]+$", "") : 
            "report";
        
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        return name + "_" + timestamp + ".pdf";
    }
    
    // ======================== DISPLAY METHODS ========================
    
    private void displayReport(String url) {
        contentLayout.setVisible(true);
        loadingLayout.setVisible(false);
        
        frame.getElement().setAttribute("src", url);
        frame.setSizeFull();
        
        enableControls(true);
        updateStatus("Report loaded | របាយការណ៍បានផ្ទុក");
    }
    
    private void setLoadingState(boolean loading) {
        loadingLayout.setVisible(loading);
        contentLayout.setVisible(!loading);
        progressBar.setIndeterminate(loading);
        
        if (loading) {
            loadingText.setText("Loading report... | កំពុងបង្កើតរបាយការណ៍...");
        }
        
        enableControls(!loading);
    }
    
    private void enableControls(boolean enabled) {
        zoomInBtn.setEnabled(enabled);
        zoomOutBtn.setEnabled(enabled);
        fitWidthBtn.setEnabled(enabled);
        printBtn.setEnabled(enabled);
        downloadBtn.setEnabled(enabled);
        openBtn.setEnabled(enabled);
    }
    
    private void updateStatus(String text) {
        if (statusText != null) {
            statusText.setText(text);
        }
    }
    
    // ======================== ZOOM CONTROLS ========================
    
    private void zoomIn() {
        if (currentZoom < 2.0) {
            currentZoom = Math.min(2.0, currentZoom + 0.1);
            applyZoom();
        }
    }
    
    private void zoomOut() {
        if (currentZoom > 0.5) {
            currentZoom = Math.max(0.5, currentZoom - 0.1);
            applyZoom();
        }
    }
    
    private void fitWidth() {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            double frameWidth = details.getBodyClientWidth() - 60;
            double contentWidth = 1200;
            double zoom = Math.min(1.0, frameWidth / contentWidth);
            currentZoom = Math.max(0.5, Math.min(2.0, zoom));
            applyZoom();
        });
    }
    
    private void applyZoom() {
        if (frame != null) {
            frame.getStyle().set("transform", "scale(" + currentZoom + ")");
            frame.getStyle().set("transform-origin", "top center");
            
            if (zoomContainer != null) {
                if (currentZoom < 1.0) {
                    zoomContainer.getStyle().set("padding", "20px");
                } else {
                    zoomContainer.getStyle().set("padding", "10px");
                }
            }
            
            if (zoomDisplay != null) {
                zoomDisplay.setText(String.format("%d%%", (int) (currentZoom * 100)));
            }
        }
    }
    
    // ======================== DIALOG LIFECYCLE ========================

    /**
     * CustomDialog owns the header Close button. Overriding close() keeps
     * PreviewReport resource cleanup guaranteed no matter how the dialog
     * is closed (header button, programmatically, or after opening a new
     * window).
     */
    @Override
    public void close() {
        cleanup();
        super.close();
    }

    private void cleanup() {
        if (streamRegistration != null && released.compareAndSet(false, true)) {
            try {
                streamRegistration.unregister();
                log.debug("StreamRegistration unregistered");
            } catch (Exception e) {
                log.warn("Error unregistering stream", e);
            }
        }
        
        if (frame != null) {
            frame.getElement().setAttribute("src", "about:blank");
        }
    }
    
    // ======================== ACTION METHODS ========================
    
    private void print() {
        if (currentReportUrl != null) {
            UI.getCurrent().getPage().executeJs(
                "const win = window.open($0, '_blank');" +
                "win.onload = function() { win.print(); };",
                currentReportUrl
            );
        }
    }
    
    private void download() {
        if (currentReportUrl != null) {
            UI.getCurrent().getPage().open(currentReportUrl, "_blank");
        }
    }
    
    private void openInNewWindowAction() {
        if (currentReportUrl != null) {
            UI.getCurrent().getPage().open(currentReportUrl, "_blank");
        }
    }
    
    private void showError(String message) {
        Notification.show(message, 6000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
    
    // ======================== SETTERS FOR CALLBACKS ========================
    
    public void setOnReportGenerated(Consumer<byte[]> onReportGenerated) {
        this.onReportGenerated = onReportGenerated;
    }
    
    public void setOnReportError(Runnable onReportError) {
        this.onReportError = onReportError;
    }
    
    // ======================== LIFECYCLE METHODS ========================
    
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        cleanup();
        super.onDetach(detachEvent);
    }
    
    // ======================== BUILDER PATTERN ========================
    
    public static class Builder {
        private String reportPath;
        private HashMap<String, Object> params = new HashMap<>();
        private boolean openInNewWindow = false;
        private String titleEn = DEFAULT_TITLE_EN;
        private String titleKh = DEFAULT_TITLE_KH;
        private String downloadFileName;
        private Consumer<byte[]> onReportGenerated;
        private Runnable onReportError;
        
        public Builder(String reportPath) {
            this.reportPath = reportPath;
        }
        
        public Builder params(HashMap<String, Object> params) {
            this.params = params;
            return this;
        }
        
        public Builder param(String key, Object value) {
            this.params.put(key, value);
            return this;
        }
        
        public Builder openInNewWindow(boolean openInNewWindow) {
            this.openInNewWindow = openInNewWindow;
            return this;
        }
        
        public Builder title(String en, String kh) {
            this.titleEn = en;
            this.titleKh = kh;
            return this;
        }
        
        public Builder downloadFileName(String downloadFileName) {
            this.downloadFileName = downloadFileName;
            return this;
        }
        
        public Builder onReportGenerated(Consumer<byte[]> callback) {
            this.onReportGenerated = callback;
            return this;
        }
        
        public Builder onReportError(Runnable callback) {
            this.onReportError = callback;
            return this;
        }
        
        public PreviewReport build() {
            PreviewReport report = new PreviewReport(
                reportPath, params, openInNewWindow, titleEn, titleKh, downloadFileName
            );
            
            if (onReportGenerated != null) {
                report.setOnReportGenerated(onReportGenerated);
            }
            if (onReportError != null) {
                report.setOnReportError(onReportError);
            }
            
            return report;
        }
        
        public void open() {
            build().open();
        }
    }
    
    // ======================== STATIC FACTORY METHODS ========================
    
    public static PreviewReport create(String reportPath, HashMap<String, Object> params) {
        return new PreviewReport(reportPath, params);
    }
    
    public static PreviewReport create(String reportPath, HashMap<String, Object> params, boolean openInNewWindow) {
        return new PreviewReport(reportPath, params, openInNewWindow);
    }
    
    public static PreviewReport create(String reportPath, HashMap<String, Object> params, 
                                       String titleEn, String titleKh) {
        return new PreviewReport(reportPath, params, false, titleEn, titleKh, null);
    }
    
    public static PreviewReport.Builder builder(String reportPath) {
        return new Builder(reportPath);
    }
}