package org.halocambodia.views.payroll;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollEmployeeAdjustmentService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.ProgressDialog;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

/**
 * Payroll master/setup page for employee recurring earnings, deductions,
 * company loans and employee recoveries.
 *
 * This is intentionally separate from Payroll Operations because these
 * configurations are maintained independently of a specific payroll run.
 */
@Route(value = "payroll-employee-adjustments", layout = MainLayout.class)
@PageTitle("Recurring & Loans | ចំណូល ការកាត់ ប្រាក់កម្ចី និងការសងសំណង")
@PermitAll
public class PayrollEmployeeAdjustmentsView extends VerticalLayout
        implements BeforeEnterObserver {

    private final AuthenticatedUser authenticatedUser;
    private final PayrollEmployeeAdjustmentsPanel panel;

    private boolean initialLoadStarted;

    public PayrollEmployeeAdjustmentsView(
            PayrollEmployeeAdjustmentService service,
            AuthenticatedUser authenticatedUser) {

        this.authenticatedUser = authenticatedUser;
        this.panel = new PayrollEmployeeAdjustmentsPanel(service);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassNames("payroll-view", "payroll-employee-adjustments-view");

        add(panel);
        setFlexGrow(1, panel);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean allowed = authenticatedUser.hasPage(
                PayrollEmployeeAdjustmentsView.class,
                AccessPageType.SELECTED_PAGE)
                || authenticatedUser.hasPage(
                        PayrollView.class,
                        AccessPageType.SELECTED_PAGE);

        if (!allowed) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        if (initialLoadStarted) {
            return;
        }
        initialLoadStarted = true;

        ProgressDialog.runUiTask(
                "Loading Recurring & Loans | កំពុងផ្ទុកចំណូល ការកាត់ និងប្រាក់កម្ចី",
                "Loading recurring earnings, deductions, loans and employee recoveries... "
                        + "| កំពុងផ្ទុកចំណូល ការកាត់ ប្រាក់កម្ចី និងការសងសំណងបុគ្គលិក...",
                panel::refresh,
                () -> {
                    // The panel is already mounted; data is now ready.
                },
                ex -> PayrollViewSupport.error(
                        PayrollViewSupport.databaseMessage(ex)));
    }
}
