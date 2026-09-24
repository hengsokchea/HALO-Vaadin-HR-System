package org.halocambodia.views.access_denied;

import com.google.common.base.Optional;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.data.*;
import org.halocambodia.views.MainLayout;

@PageTitle("Access Denied")
@Route(value = "access-denied", layout = MainLayout.class)
@AnonymousAllowed
public class AccessDeniedView extends VerticalLayout {

    private final AuthenticatedUser authenticatedUser;

    public AccessDeniedView(AuthenticatedUser authenticatedUser) {
    	this.authenticatedUser=authenticatedUser;

        setSizeFull();
        setSpacing(false);
        setPadding(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Background styling
        getStyle()
            .set("background", "linear-gradient(135deg, var(--lumo-primary-color-10pct), white)");

        // Big lock icon
        Icon lockIcon = VaadinIcon.LOCK.create();
        lockIcon.setSize("96px");
        lockIcon.getStyle().set("color", "var(--lumo-primary-color)");

        // Title
        H1 title = new H1("Access Denied");
        title.getStyle().set("margin-top", "0.5rem");

        // Subtitle
        Paragraph subtitle = new Paragraph("You don’t have permission to access this page.");
        subtitle.getStyle().set("margin-top", "0").set("font-size", "1.1rem");

        // Khmer help text
        Paragraph kh = new Paragraph("អ្នកមិនមានសិទ្ធិមើលទំព័រនេះទេ។ សូមទាក់ទងអ្នកគ្រប់គ្រងប្រព័ន្ធ ប្រសិនបើអ្នកត្រូវការចូលប្រើ។");
        kh.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("max-width", "480px")
            .set("text-align", "center");

        // Action buttons
        Button back = new Button("Go back", VaadinIcon.ARROW_LEFT.create(), e ->
            UI.getCurrent().getPage().getHistory().back());
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button home = new Button("Home", VaadinIcon.HOME.create(), e ->
            UI.getCurrent().navigate(""));
        home.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button signOut = new Button("Sign out", VaadinIcon.SIGN_OUT.create(), e ->authenticatedUser.logout());
        signOut.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout actions = new HorizontalLayout(back, home, signOut);
        actions.setSpacing(true);
        actions.setPadding(false);

        // Assemble
        add(lockIcon, title, subtitle, kh, actions);
    }
}
