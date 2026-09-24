package org.halocambodia.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class CustomDialog extends Dialog {

    private static final String NORMAL_WIDTH =
            "min(900px, calc(100vw - 32px))";

    private static final String NORMAL_MAX_WIDTH =
            "calc(100vw - 32px)";

    private static final String NORMAL_MAX_HEIGHT =
            "calc(100dvh - 32px)";

    private static final String MINIMIZED_WIDTH = "300px";
    private static final String MINIMIZED_HEIGHT = "48px";

    private String dialogTitle;

    private boolean minimized;
    private boolean maximized;

    /*
     * Normal dialog dimensions before minimizing or maximizing.
     */
    private String previousWidth;
    private String previousHeight;
    private String previousTop;
    private String previousLeft;

    private final Button minimizeButton;
    private final Button maximizeButton;
    private final Button closeButton;

    public CustomDialog(String dialogTitle) {
        this.dialogTitle = dialogTitle != null
                ? dialogTitle
                : "";

        configureDialog();

        minimizeButton = createMinimizeButton();
        maximizeButton = createMaximizeButton();
        closeButton = createCloseButton();

        getHeader().add(
                minimizeButton,
                maximizeButton,
                closeButton
        );

        updateHeaderButtons();
    }

    /*
     * Configure the dialog.
     */
    private void configureDialog() {
        setHeaderTitle(dialogTitle);

        setCloseOnOutsideClick(false);
        setCloseOnEsc(false);

        setDraggable(true);
        setResizable(true);
        setKeepInViewport(true);

        addThemeVariants(DialogVariant.NO_PADDING);
        addClassName("custom-dialog");

        setWidth(NORMAL_WIDTH);
        setMaxWidth(NORMAL_MAX_WIDTH);
        setMaxHeight(NORMAL_MAX_HEIGHT);
    }

    /*
     * Create the Minimize button.
     */
    private Button createMinimizeButton() {
        Button button = new Button(
                createHeaderIcon(VaadinIcon.MINUS),
                event -> toggleMinimize()
        );

        configureHeaderButton(
                button,
                "Minimize",
                "Minimize dialog"
        );

        return button;
    }

    /*
     * Create the Maximize button.
     */
    private Button createMaximizeButton() {
        Button button = new Button(
                createHeaderIcon(VaadinIcon.EXPAND_FULL),
                event -> toggleMaximize()
        );

        configureHeaderButton(
                button,
                "Maximize",
                "Maximize dialog"
        );

        return button;
    }

    /*
     * Create the Close button.
     */
    private Button createCloseButton() {
        Button button = new Button(
                createHeaderIcon(VaadinIcon.CLOSE_SMALL),
                event -> closeDialog()
        );

        button.addThemeVariants(
                ButtonVariant.TERTIARY,
                ButtonVariant.LUMO_ERROR
        );

        button.addClassNames(
                "dialog-header-button",
                "dialog-close-button"
        );

        /*
         * Java-side colour fallback.
         */
        button.getStyle()
        	.set("color","var(--halo-dialog-header-text)")
                .set("background-color", "transparent")
                .set("opacity", "1");

        button.setTooltipText("Close");
        button.setAriaLabel("Close dialog");

        return button;
    }

    /*
     * Common Minimize and Maximize button configuration.
     */
    private void configureHeaderButton(
            Button button,
            String tooltip,
            String ariaLabel) {

        button.addThemeVariants(ButtonVariant.TERTIARY);
        button.addClassName("dialog-header-button");

        button.getStyle()
                .set(
                        "color",
                        "var(--halo-dialog-header-text)"
                )
                .set("background-color", "transparent")
                .set("opacity", "1");

        button.setTooltipText(tooltip);
        button.setAriaLabel(ariaLabel);
    }

    /*
     * Create a visible white dialog-header icon.
     */
    private Icon createHeaderIcon(VaadinIcon iconType) {
        Icon icon = iconType.create();

        icon.addClassName("dialog-header-icon");
        icon.setSize("18px");

        icon.getStyle()
                .set(
                        "color",
                        "var(--halo-dialog-header-text)"
                )
                .set("fill", "currentColor")
                .set("opacity", "1");

        return icon;
    }

    /*
     * Minimize or restore the dialog.
     */
    private void toggleMinimize() {
        if (minimized) {
            restoreNormalState();

            minimized = false;
            maximized = false;

            removeDialogStateClasses();
        } else {
            /*
             * Do not overwrite normal dimensions when switching
             * from maximized to minimized.
             */
            if (!maximized) {
                saveNormalState();
            }

            minimized = true;
            maximized = false;

            removeDialogStateClasses();
            addClassName("dialog-minimized");

            setResizable(false);
            setDraggable(true);

            setWidth(MINIMIZED_WIDTH);
            setMaxWidth(MINIMIZED_WIDTH);

            setHeight(MINIMIZED_HEIGHT);
            setMaxHeight(MINIMIZED_HEIGHT);

            setTop("calc(100dvh - 58px)");
            setLeft("calc(50vw - 150px)");
        }

        updateHeaderButtons();
    }

    /*
     * Maximize or restore the dialog.
     */
    private void toggleMaximize() {
        if (maximized) {
            restoreNormalState();

            minimized = false;
            maximized = false;

            removeDialogStateClasses();
        } else {
            /*
             * Do not overwrite normal dimensions when switching
             * from minimized to maximized.
             */
            if (!minimized) {
                saveNormalState();
            }

            minimized = false;
            maximized = true;

            removeDialogStateClasses();
            addClassName("dialog-maximized");

            setDraggable(false);
            setResizable(false);

            setWidth("100vw");
            setMaxWidth("100vw");

            setHeight("100dvh");
            setMaxHeight("100dvh");

            setTop("0");
            setLeft("0");
        }

        updateHeaderButtons();
    }

    /*
     * Save the current normal dialog state.
     */
    private void saveNormalState() {
        previousWidth = getWidth();
        previousHeight = getHeight();
        previousTop = getTop();
        previousLeft = getLeft();
    }

    /*
     * Restore the dialog to its previous normal state.
     */
    private void restoreNormalState() {
        setDraggable(true);
        setResizable(true);

        setWidth(valueOrDefault(
                previousWidth,
                NORMAL_WIDTH
        ));

        setMaxWidth(NORMAL_MAX_WIDTH);
        setMaxHeight(NORMAL_MAX_HEIGHT);

        restoreStyleProperty(
                "height",
                previousHeight
        );

        restoreStyleProperty(
                "top",
                previousTop
        );

        restoreStyleProperty(
                "left",
                previousLeft
        );
    }

    /*
     * Restore a CSS property or remove it when it had no
     * original value.
     */
    private void restoreStyleProperty(
            String property,
            String value) {

        if (value == null || value.isBlank()) {
            getElement()
                    .getStyle()
                    .remove(property);
        } else {
            getElement()
                    .getStyle()
                    .set(property, value);
        }
    }

    /*
     * Update button icons, tooltips and accessibility labels.
     */
    private void updateHeaderButtons() {
        if (minimized) {
            minimizeButton.setIcon(
                    createHeaderIcon(VaadinIcon.PLUS)
            );

            minimizeButton.setTooltipText("Restore");
            minimizeButton.setAriaLabel("Restore dialog");
        } else {
            minimizeButton.setIcon(
                    createHeaderIcon(VaadinIcon.MINUS)
            );

            minimizeButton.setTooltipText("Minimize");
            minimizeButton.setAriaLabel("Minimize dialog");
        }

        if (maximized) {
            maximizeButton.setIcon(
                    createHeaderIcon(VaadinIcon.COMPRESS)
            );

            maximizeButton.setTooltipText("Restore");
            maximizeButton.setAriaLabel("Restore dialog");
        } else {
            maximizeButton.setIcon(
                    createHeaderIcon(VaadinIcon.EXPAND_FULL)
            );

            maximizeButton.setTooltipText("Maximize");
            maximizeButton.setAriaLabel("Maximize dialog");
        }
    }

    /*
     * Close the dialog and reset it to normal state so that
     * it opens normally the next time it is used.
     */
    private void closeDialog() {
        if (minimized || maximized) {
            restoreNormalState();
        }

        minimized = false;
        maximized = false;

        removeDialogStateClasses();
        updateHeaderButtons();

        close();
    }

    /*
     * Remove minimized and maximized CSS classes.
     */
    private void removeDialogStateClasses() {
        removeClassName("dialog-minimized");
        removeClassName("dialog-maximized");
    }

    private String valueOrDefault(
            String value,
            String defaultValue) {

        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle != null
                ? dialogTitle
                : "";

        setHeaderTitle(this.dialogTitle);
    }

    public boolean isMinimized() {
        return minimized;
    }

    public boolean isMaximized() {
        return maximized;
    }
}