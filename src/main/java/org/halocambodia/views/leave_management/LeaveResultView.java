package org.halocambodia.views.leave_management;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

@Route("leave-result/:status")
@PageTitle("Leave Result")
public class LeaveResultView        extends VerticalLayout        implements BeforeEnterObserver {

    private final H2 title = new H2();

    private final Span message = new Span();

    public LeaveResultView() {

        setSizeFull();

        setAlignItems(Alignment.CENTER);

        setJustifyContentMode(JustifyContentMode.CENTER);

        add(title, message);
    }

    @Override
    public void beforeEnter(
            BeforeEnterEvent event
    ) {

        String status =
                event.getRouteParameters()
                        .get("status")
                        .orElse("unknown");

        switch (status.toLowerCase()) {

            case "approve":

                title.setText("✅ Leave Approved");

                message.setText(
                        "The leave request was approved successfully."
                );

                break;

            case "reject":

                title.setText("❌ Leave Rejected");

                message.setText(
                        "The leave request was rejected."
                );

                break;

            case "expired":

                title.setText("⏰ Token Expired");

                message.setText(
                        "This approval link has expired."
                );

                break;

            case "used":

                title.setText("⚠ Already Processed");

                message.setText(
                        "This approval link was already used."
                );

                break;

            default:

                title.setText("🚫 Invalid Token");

                message.setText(
                        "Invalid approval link."
                );
        }
    }
}