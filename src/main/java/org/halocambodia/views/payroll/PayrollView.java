package org.halocambodia.views.payroll;

import java.util.LinkedHashMap;
import java.util.Map;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.LeaveTypeRepository;
import org.halocambodia.data.LeaveTypeSubTypeRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollAttendanceDayEditService;
import org.halocambodia.services.PayrollPaymentService;
import org.halocambodia.services.PayrollPayslipService;
import org.halocambodia.services.PayrollService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.ProgressDialog;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "payroll", layout = MainLayout.class)
@PageTitle("Payroll Operations | ប្រតិបត្តិការប្រាក់បៀវត្ស")
@PermitAll
public class PayrollView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthenticatedUser authenticatedUser;
    private final PayrollRefreshCoordinator refreshCoordinator = new PayrollRefreshCoordinator();

    private final PayrollDashboardPanel dashboardPanel;
    private final PayrollPeriodsPanel periodsPanel;
    private final PayrollAttendancePanel attendancePanel;
    private final PayrollProcessingPanel processingPanel;
    private final PayrollPaymentsPanel paymentsPanel;

    private final Div pageContainer = new Div();
    private final Map<Tab, Component> tabPages = new LinkedHashMap<>();
    private final Tabs payrollTabs = new Tabs();

    private final Tab dashboardTab = new Tab(VaadinIcon.DASHBOARD.create(), new Span("Dashboard"));
    private final Tab periodsTab = new Tab(VaadinIcon.CALENDAR.create(), new Span("Periods"));
    private final Tab attendanceTab = new Tab(
            VaadinIcon.CLIPBOARD_CHECK.create(),
            new Span("Attendance Check & Lock"));
    private final Tab processingTab = new Tab(VaadinIcon.MONEY.create(), new Span("Processing"));
    private final Tab paymentsTab = new Tab(VaadinIcon.CREDIT_CARD.create(), new Span("Payments"));

    private boolean initialDashboardLoadStarted;

    public PayrollView(PayrollService service, PayrollPaymentService paymentService,
            PayrollPayslipService payslipService, AuthenticatedUser authenticatedUser,
            LeaveTypeRepository leaveTypeRepository,
            LeaveTypeSubTypeRepository leaveTypeSubTypeRepository,
            PayrollAttendanceDayEditService attendanceDayEditService) {

        this.authenticatedUser = authenticatedUser;

        dashboardPanel = new PayrollDashboardPanel(service);
        periodsPanel = new PayrollPeriodsPanel(service, authenticatedUser, refreshCoordinator);
        attendancePanel = new PayrollAttendancePanel(service,authenticatedUser, leaveTypeRepository,leaveTypeSubTypeRepository, attendanceDayEditService,refreshCoordinator);
        processingPanel = new PayrollProcessingPanel(service, authenticatedUser, refreshCoordinator);
        paymentsPanel = new PayrollPaymentsPanel(service, paymentService, payslipService, authenticatedUser, refreshCoordinator);

        wireRefreshCoordinator();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("payroll-view");

        buildShell();

        // Load the initially selected Dashboard with the same progress
        // experience used when switching Payroll tabs.
        addAttachListener(event -> {
            if (!initialDashboardLoadStarted) {
                initialDashboardLoadStarted = true;
                loadTabWithProgress(dashboardTab);
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE)) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    private void wireRefreshCoordinator() {
        refreshCoordinator.registerDashboard(dashboardPanel::refresh);
        refreshCoordinator.registerPeriods(periodsPanel::refresh);
        refreshCoordinator.registerAttendance(attendancePanel::refresh);
        refreshCoordinator.registerProcessing(processingPanel::refresh);
        refreshCoordinator.registerPayments(paymentsPanel::refresh);
        refreshCoordinator.registerPaymentPeriodRefresh(paymentsPanel::refreshPaymentPeriodFilter);
        refreshCoordinator.registerPaymentScheduleSupplier(paymentsPanel::paymentSchedule);
        refreshCoordinator.registerProcessingPeriodIdSupplier(processingPanel::selectedPeriodId);
    }

    private void buildShell() {
        payrollTabs.add(dashboardTab, periodsTab, attendanceTab, processingTab, paymentsTab);
        payrollTabs.setWidthFull();
        payrollTabs.addClassName("payroll-tabs");

        tabPages.put(dashboardTab, dashboardPanel);
        tabPages.put(periodsTab, periodsPanel);
        tabPages.put(attendanceTab, attendancePanel);
        tabPages.put(processingTab, processingPanel);
        tabPages.put(paymentsTab, paymentsPanel);

        pageContainer.setSizeFull();
        pageContainer.addClassName("payroll-page-container");
        showTab(dashboardTab);

        payrollTabs.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();

            if (selected == periodsTab) {
                // Periods is intentionally kept immediate; the requested
                // progress experience applies to Dashboard, Attendance,
                // Processing and Payments.
                showTab(selected);
                periodsPanel.refresh();
                return;
            }

            loadTabWithProgress(selected);
        });

        add(payrollTabs, pageContainer);
        setFlexGrow(1, pageContainer);
    }

    private void showTab(Tab tab) {
        pageContainer.removeAll();
        pageContainer.add(tabPages.get(tab));
    }

    private void loadTabWithProgress(Tab tab) {
        Runnable refreshTask;
        String title;
        String message;

        if (tab == dashboardTab) {
            refreshTask = dashboardPanel::refresh;
            title = "Loading Dashboard | កំពុងផ្ទុកផ្ទាំងសង្ខេប";
            message = "Loading payroll dashboard... | កំពុងផ្ទុកទិន្នន័យផ្ទាំងសង្ខេបប្រាក់បៀវត្ស...";
        } else if (tab == attendanceTab) {
            refreshTask = attendancePanel::refresh;
            title = "Loading Attendance | កំពុងផ្ទុកវត្តមាន";
            message = "Loading attendance check & lock... | កំពុងផ្ទុកការត្រួតពិនិត្យ និងចាក់សោវត្តមាន...";
        } else if (tab == processingTab) {
            refreshTask = processingPanel::refresh;
            title = "Loading Processing | កំពុងផ្ទុកដំណើរការ";
            message = "Loading payroll processing... | កំពុងផ្ទុកដំណើរការប្រាក់បៀវត្ស...";
        } else if (tab == paymentsTab) {
            refreshTask = paymentsPanel::refresh;
            title = "Loading Payments | កំពុងផ្ទុកការបើកប្រាក់";
            message = "Loading payroll payments... | កំពុងផ្ទុកទិន្នន័យការបើកប្រាក់...";
        } else {
            showTab(tab);
            return;
        }

        ProgressDialog.runUiTask(
                title,
                message,
                refreshTask,
                () -> showTab(tab),
                ex -> {
                    showTab(tab);
                    PayrollViewSupport.error(PayrollViewSupport.databaseMessage(ex));
                });
    }
}
