package org.halocambodia.fileattachment.component;

import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Reusable file preview dialog.
 *
 * Supported inline preview:
 * - PDF
 * - Images
 * - Video
 * - Audio
 * - Text, CSV, JSON, XML and log files
 *
 * Unsupported file types are opened in a new browser tab.
 */
public final class FilePreviewDialog extends Dialog {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MIME_TYPE ="application/octet-stream";

    private static final String DEFAULT_WIDTH = "92vw";
    private static final String DEFAULT_HEIGHT = "88vh";
    private static final String DEFAULT_MAX_WIDTH = "1100px";
    private static final String DEFAULT_MAX_HEIGHT = "95vh";
    private static final String MIN_WIDTH = "360px";
    private static final String MIN_HEIGHT = "260px";
    private static final String MINIMIZED_WIDTH = "420px";

    private final String fileName;
    private final String mimeType;
    private final String previewUrl;

    private VerticalLayout contentLayout;
    private Button minimizeButton;
    private Button maximizeButton;

    private boolean minimized;
    private boolean maximized;
    
    private final HorizontalLayout footerContent = new HorizontalLayout();

    private FilePreviewDialog(String fileName,String mimeType,String previewUrl) {

        this.fileName = sanitizeFileName(fileName);
        this.mimeType = normalizeMimeType(mimeType);
        this.previewUrl = previewUrl;

        configureDialog();
        buildHeader();
        buildDialogContent();

    }

    /**
     * Opens a file preview.
     *
     * Supported types open inside a dialog.
     * Unsupported types open in a new browser tab.
     */
    public static void openPreview(String fileName, String mimeType,
            String previewUrl) {

        if (previewUrl == null || previewUrl.isBlank()) {
            return;
        }

        if (!isInlinePreviewSupported(fileName, mimeType)) {
            openInNewTab(previewUrl);
            return;
        }

        FilePreviewDialog dialog =new FilePreviewDialog(fileName, mimeType,previewUrl);

        dialog.open();
    }

    /**
     * Checks whether the file can be displayed inside the preview dialog.
     */
    public static boolean isInlinePreviewSupported(String fileName,String mimeType) {

        String normalizedMimeType =normalizeMimeType(mimeType);
        String normalizedFileName =sanitizeFileName(fileName).toLowerCase(Locale.ROOT);

        return isPdf(normalizedMimeType, normalizedFileName) || isImage(normalizedMimeType, normalizedFileName) || isVideo(normalizedMimeType, normalizedFileName) || isAudio(normalizedMimeType, normalizedFileName) || isText(normalizedMimeType, normalizedFileName);
    }

    private void configureDialog() {



        setWidth(DEFAULT_WIDTH);
        setHeight(DEFAULT_HEIGHT);
        setMaxWidth(DEFAULT_MAX_WIDTH);
        setMaxHeight(DEFAULT_MAX_HEIGHT);
        setMinWidth(MIN_WIDTH);
        setMinHeight(MIN_HEIGHT);

        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        setDraggable(true);
        setResizable(true);
        setKeepInViewport(true);

        getElement().setAttribute(
                "aria-label",
                "File preview: " + fileName
        );
    }

    private void buildHeader() {

    	Span title = new Span( "Preview | មើលឯកសារ: " + fileName);

    	title.addClassName("draggable");
    	title.getStyle()
    	        .set("font-weight", "700")
    	        .set("font-size", "var(--lumo-font-size-l)")
    	        .set("overflow", "hidden")
    	        .set("text-overflow", "ellipsis")
    	        .set("white-space", "nowrap")
    	        .set("cursor", "move")
    	        .set("user-select", "none")
    	        .set("flex", "1 1 auto")
    	        .set("min-width", "0");

    	title.getElement().addEventListener("dblclick", event -> toggleMaximize());

        minimizeButton = createHeaderButton( VaadinIcon.MINUS.create(),"Minimize", this::toggleMinimize);
        maximizeButton = createHeaderButton( VaadinIcon.EXPAND_SQUARE.create(), "Maximize",this::toggleMaximize );
        Button closeButton = createHeaderButton(VaadinIcon.CLOSE_SMALL.create(), "Close", this::close);
        closeButton.getStyle().set("color", "var(--lumo-error-text-color)");
        HorizontalLayout controls = new HorizontalLayout(minimizeButton,maximizeButton,closeButton);
        
        minimizeButton.getStyle().setColor("White");
        maximizeButton.getStyle().setColor("White");
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
      

        controls.setPadding(false);
        controls.setMargin(false);
        controls.setSpacing(false);
        controls.setAlignItems(Alignment.CENTER);
        controls.getStyle().set("flex-shrink", "0");

        HorizontalLayout header = new HorizontalLayout( title, controls);

        header.setWidthFull();
        header.setPadding(false);
        header.setMargin(false);
        header.setSpacing(true);
        header.setAlignItems(Alignment.CENTER);

        getHeader().removeAll();
        getHeader().add(header);
    }

    private Button createHeaderButton(Icon icon, String tooltip, Runnable action) {
        icon.setSize("18px");
        Button button = new Button( icon, event -> action.run());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.TERTIARY);
        button.getElement().setAttribute("title", tooltip);
        button.getElement().setAttribute("aria-label", tooltip);
        return button;
    }

    private void toggleMinimize() {

        if (minimized) {
            restoreNormalSize();
            return;
        }

        minimized = true;
        maximized = false;

        contentLayout.setVisible(false);
        footerContent.setVisible(false);

        setWidth("min(420px, calc(100vw - 24px))");
        setHeight("48px");

        setMinWidth("280px");
        setMinHeight("48px");
        setMaxWidth("calc(100vw - 24px)");
        setMaxHeight("48px");

        minimizeButton.setIcon(VaadinIcon.PLUS.create());
        minimizeButton.getElement().setAttribute("title", "Restore");
        minimizeButton.getElement().setAttribute("aria-label", "Restore");
        maximizeButton.setIcon(VaadinIcon.EXPAND_SQUARE.create());
        maximizeButton.getElement().setAttribute("title", "Maximize");
        maximizeButton.getElement().setAttribute("aria-label", "Maximize");
    }

    private void toggleMaximize() {

        if (maximized) {
            restoreNormalSize();
            return;
        }
        maximizeDialog();
    }

    private void maximizeDialog() {

        minimized = false;
        maximized = true;

        contentLayout.setVisible(true);
        footerContent.setVisible(true);

        setMinWidth("0px");
        setMinHeight("0px");
        setMaxWidth("100vw");
        setMaxHeight("100vh");

        setWidth("100vw");
        setHeight("100vh");

        setTop("0px");
        setLeft("0px");

        minimizeButton.setIcon( VaadinIcon.MINUS.create());
        minimizeButton.getElement().setAttribute("title", "Minimize");
        minimizeButton.getElement().setAttribute("aria-label", "Minimize");
        maximizeButton.setIcon(VaadinIcon.COMPRESS_SQUARE.create());
        maximizeButton.getElement().setAttribute("title", "Restore");
        maximizeButton.getElement().setAttribute("aria-label", "Restore");
    }

    private void restoreNormalSize() {

        minimized = false;
        maximized = false;

        contentLayout.setVisible(true);
        footerContent.setVisible(true);

        getElement().removeProperty("top");
        getElement().removeProperty("left");

        setMinWidth(MIN_WIDTH);
        setMinHeight(MIN_HEIGHT);
        setMaxWidth(DEFAULT_MAX_WIDTH);
        setMaxHeight(DEFAULT_MAX_HEIGHT);

        setWidth(DEFAULT_WIDTH);
        setHeight(DEFAULT_HEIGHT);

        minimizeButton.setIcon(VaadinIcon.MINUS.create());
        minimizeButton.getElement().setAttribute("title", "Minimize");
        minimizeButton.getElement().setAttribute("aria-label", "Minimize");
        maximizeButton.setIcon(VaadinIcon.EXPAND_SQUARE.create());
        maximizeButton.getElement().setAttribute("title", "Maximize");
        maximizeButton.getElement().setAttribute("aria-label", "Maximize");
    }

    private void buildDialogContent() {

        Component previewContent =createPreviewContent();
        Div previewContainer =new Div(previewContent);
        previewContainer.setSizeFull();
        previewContainer.getStyle()
                .set("min-height", "180px")
                .set("overflow", "auto")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("box-sizing", "border-box");

        contentLayout =new VerticalLayout(previewContainer);
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);
        add(contentLayout);
        Button openNewTabButton = new Button("Open in New Tab | បើកផ្ទាំងថ្មី",VaadinIcon.EXTERNAL_LINK.create(), event -> openInNewTab(previewUrl));
        openNewTabButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button closeButton = new Button("Close | បិទ",VaadinIcon.CLOSE_SMALL.create(),event -> close());
        closeButton.addThemeVariants( ButtonVariant.LUMO_PRIMARY);

        footerContent.add( openNewTabButton, closeButton);
        
        footerContent.setWidthFull();
        footerContent.setPadding(false);
        footerContent.setMargin(false);
        footerContent.setSpacing(true);
        footerContent.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        getFooter().add(footerContent);
    }

    private Component createPreviewContent() {

        String lowerFileName =fileName.toLowerCase(Locale.ROOT);

        if (isImage(mimeType, lowerFileName)) {
            return createImagePreview();
        }

        if (isAudio(mimeType, lowerFileName)) {
            return createFramePreview("180px");
        }

        return createFramePreview("100%");
    }

    private Component createImagePreview() {

        Image image = new Image(previewUrl,fileName );
        image.setWidthFull();
        image.setHeightFull();
        image.getStyle()
                .set("display", "block")
                .set("object-fit", "contain")
                .set("margin", "auto")
                .set("padding", "8px")
                .set("box-sizing", "border-box");

        return image;
    }

    private Component createFramePreview(
            String height) {

        IFrame frame = new IFrame(previewUrl);

        frame.setWidth("100%");
        frame.setHeight(height);

        frame.getStyle()
                .set("display", "block")
                .set("border", "none")
                .set("background", "var(--lumo-base-color)");

        frame.getElement() .setAttribute("title", "Preview of " + fileName );

        return frame;
    }

    private static boolean isPdf(String mimeType,String fileName) {
        return "application/pdf".equals(mimeType) || fileName.endsWith(".pdf");
    }

    private static boolean isImage( String mimeType,String fileName) {
        return mimeType.startsWith("image/") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")  || fileName.endsWith(".gif")  || fileName.endsWith(".webp") || fileName.endsWith(".bmp") || fileName.endsWith(".svg");
    }

    private static boolean isVideo( String mimeType, String fileName) {
        return mimeType.startsWith("video/") || fileName.endsWith(".mp4") || fileName.endsWith(".webm") || fileName.endsWith(".ogg") || fileName.endsWith(".mov");
    }

    private static boolean isAudio(String mimeType, String fileName) {
        return mimeType.startsWith("audio/") || fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".m4a")  || fileName.endsWith(".aac")  || fileName.endsWith(".oga");
    }

    private static boolean isText( String mimeType, String fileName) {
        return mimeType.startsWith("text/") || "application/json".equals(mimeType) || "application/xml".equals(mimeType) || "application/javascript".equals(mimeType)  || fileName.endsWith(".txt") || fileName.endsWith(".log")  || fileName.endsWith(".csv") || fileName.endsWith(".json") || fileName.endsWith(".xml") || fileName.endsWith(".sql")  || fileName.endsWith(".java") || fileName.endsWith(".properties") || fileName.endsWith(".md");
    }

    private static void openInNewTab(String url) {

        UI ui = UI.getCurrent();
        if (ui == null || url == null || url.isBlank()) {
            return;
        }

        ui.getPage().executeJs(
                """
                const previewWindow = window.open($0, '_blank');

                if (previewWindow) {
                    previewWindow.opener = null;
                }
                """,
                url
        );
    }

    private static String sanitizeFileName(String fileName) {

        if (fileName == null|| fileName.isBlank()) {
            return "Unnamed attachment";
        }
        return fileName.trim();
    }

    private static String normalizeMimeType(String mimeType) {

        if (mimeType == null || mimeType.isBlank()) {
            return DEFAULT_MIME_TYPE;
        }
        int separatorIndex =mimeType.indexOf(';');
        String normalized = separatorIndex >= 0  ? mimeType.substring(0, separatorIndex) : mimeType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }
}