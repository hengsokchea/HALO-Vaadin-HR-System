package org.halocambodia.views.payroll;

import org.halocambodia.views.CustomDialog;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

/**
 * Payroll confirmation dialog built on the application's standard CustomDialog.
 */
final class PayrollConfirmDialog extends CustomDialog {

    private final Paragraph message = new Paragraph();
    private final Button cancelButton = new Button(
            "Cancel | បោះបង់",
            VaadinIcon.CLOSE_SMALL.create());
    private final Button confirmButton = new Button(
            "Confirm | បញ្ជាក់",
            VaadinIcon.CHECK.create());

    PayrollConfirmDialog(String title, String text) {
        super(title);
        addClassName("payroll-dialog");

        setWidth("min(620px, calc(100vw - 32px))");

        message.setText(text == null ? "" : text);
        message.getStyle()
                .set("white-space", "pre-wrap")
                .set("margin", "0");

        VerticalLayout content = new VerticalLayout(message);
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(false);
        add(content);

        cancelButton.addClickListener(event -> close());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(cancelButton, confirmButton);
    }

    void setText(String text) {
        message.setText(text == null ? "" : text);
    }

    void setCancelable(boolean cancelable) {
        cancelButton.setVisible(cancelable);
    }

    void setCancelText(String text) {
        cancelButton.setText(text == null ? "" : text);
    }

    void setConfirmText(String text) {
        confirmButton.setText(text == null ? "" : text);
    }

    void setConfirmButtonTheme(String themeNames) {
        confirmButton.getElement().getThemeList().clear();

        if (themeNames == null || themeNames.isBlank()) {
            return;
        }

        for (String theme : themeNames.trim().split("\\s+")) {
            if (!theme.isBlank()) {
                confirmButton.getElement().getThemeList().add(theme);
            }
        }
    }

    Registration addConfirmListener(
            ComponentEventListener<com.vaadin.flow.component.ClickEvent<Button>> listener) {

        return confirmButton.addClickListener(event -> {
            close();
            listener.onComponentEvent(event);
        });
    }
}
