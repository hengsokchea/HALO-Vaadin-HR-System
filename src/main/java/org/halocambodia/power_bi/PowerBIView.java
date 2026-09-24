package org.halocambodia.power_bi;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route( value = "powerbi",layout = MainLayout.class
)
@PageTitle("PowerBI Dashboard")
@PermitAll
public class PowerBIView
        extends VerticalLayout
        implements BeforeEnterObserver {

    private static final String POWER_BI_URL =
            "https://app.powerbi.com/reportEmbed"
                    + "?reportId=4efe9636-94d9-41a8-aec2-f1ed092239ee"
                    + "&autoAuth=true"
                    + "&ctid=54d709f2-6115-44f4-bcb7-15fc524cd70a"
                    + "&actionBarEnabled=false";

    private final AuthenticatedUser authenticatedUser;

    public PowerBIView( AuthenticatedUser authenticatedUser) {

        this.authenticatedUser =authenticatedUser;

        configureLayout();
        createPowerBiFrame();
    }

    private void configureLayout() {
        setSizeFull();

        setPadding(false);
        setMargin(false);
        setSpacing(false);

        getStyle()
                .set("overflow", "hidden")
                .set("background-color", "var(--halo-page-background)");
    }

    private void createPowerBiFrame() {
        IFrame frame =new IFrame(POWER_BI_URL);

        frame.setSizeFull();

        frame.setTitle( "PowerBI Dashboard");

        frame.getElement().setAttribute("allowfullscreen","true" );

        frame.getElement().setAttribute("allow", "fullscreen");

        frame.getStyle()
                .set("border", "0")
                .set("display", "block");

        add(frame);

        setFlexGrow( 1, frame);
    }

    /*
     * Check database page permission before opening the view.
     */
    @Override
    public void beforeEnter( BeforeEnterEvent event) {

        boolean allowed = authenticatedUser.hasPage(event.getNavigationTarget(),AccessPageType.SELECTED_PAGE );

        if (!allowed) { event.rerouteTo( AccessDeniedView.class );
        }
    }
}