package org.halocambodia.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.ElementConstants;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class CardComponent extends Div {

    private final HorizontalLayout header = new HorizontalLayout();
    private final Div content = new Div();
    private final HorizontalLayout footer = new HorizontalLayout();
    private Icon toggleIcon = VaadinIcon.ANGLE_DOWN.create();

    private boolean expanded = true;

    public CardComponent(String headerContent, Component... bodyContent) {
        // Card base style
        getStyle()
            .set("border", "1px solid #ccc")
            .set("border-radius", "12px")
            .set("padding", "0")
            .set("background-color", "#fff")
            .set("box-shadow", "0 2px 6px rgba(0, 0, 0, 0.05)")
            .set("width", "100%")  // Adjust width for better visibility
            .set("margin-bottom", "1rem")
            .set("color", "#000");

        // Header layout
        header.setPadding(true);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("cursor", "pointer")
            .set("padding", "1rem")
            //.set("font-weight", "bold")
            .set("border-bottom", "1px solid #eee");

        toggleIcon.getStyle().set("margin-left", "auto");
        header.add(new Span(headerContent), toggleIcon);

        // Content
        content.add(bodyContent);
        content.getStyle()
            .set("padding", "1rem")
            .set("transition", "max-height 0.3s ease")
            .set(ElementConstants.STYLE_OVERFLOW, "hidden");

        // Footer
        footer.setPadding(true);
        footer.setSpacing(true);
        footer.setWidthFull();
        footer.getStyle()
            .set("border-top", "1px solid #eee")
            .set("padding", "0.75rem 1rem");

        // Toggle
        header.addClickListener(e -> toggle());

        // Listen for theme changes and update the card theme
        UI.getCurrent().addBeforeEnterListener(event -> updateCardTheme());
        
        add(header, content, footer);
    }

    public void setFooter(Component... footerContent) {
        footer.removeAll();
        footer.add(footerContent);
    }

    public void toggle() {
        expanded = !expanded;
        content.setVisible(expanded);

        Icon newIcon = expanded ? VaadinIcon.ANGLE_DOWN.create() : VaadinIcon.ANGLE_RIGHT.create();
        header.replace(toggleIcon, newIcon);
        toggleIcon = newIcon;
        toggleIcon.getStyle().set("margin-left", "auto");
    }

    public void collapse() {
        if (expanded) toggle();
    }

    public void expand() {
        if (!expanded) toggle();
    }

    private void updateCardTheme() {
        boolean isDark = UI.getCurrent().getElement().getThemeList().contains("dark");
        setTheme(isDark ? "dark" : "light");
    }

    public void setTheme(String theme) {
        boolean isDark = "dark".equalsIgnoreCase(theme);

        getStyle()
            .set("background-color", isDark ? "#1e1e1e" : "#fff")
            .set("color", isDark ? "#f0f0f0" : "#000")
            .set("border", isDark ? "1px solid #444" : "1px solid #ccc");

        header.getStyle()
            .set("border-bottom", isDark ? "1px solid #444" : "1px solid #eee");

        footer.getStyle()
            .set("border-top", isDark ? "1px solid #444" : "1px solid #eee");
    }
}
