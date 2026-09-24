package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.*;
import static org.halocambodia.views.payroll.PayrollViewSupport.*;

import org.halocambodia.services.PayrollService;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Payroll Dashboard.
 *
 * <p>Shows the main payroll KPIs together with recent payroll runs. Payment
 * figures such as First Paid and Final Paid remain available in the Recent
 * Payroll Runs grid rather than the top KPI cards.</p>
 */
final class PayrollDashboardPanel extends VerticalLayout {

    private final PayrollService service;

    private final Grid<RunRow> recentRunsGrid = new Grid<>();
    private final Div overviewCards = new Div();

    PayrollDashboardPanel(PayrollService service) {
        this.service = service;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        VerticalLayout page = page(
                "Payroll Dashboard | ផ្ទាំងសង្ខេបប្រាក់បៀវត្ស",
                "Payroll overview, payment reconciliation and recent processing activity. "
                        + "| ទិដ្ឋភាពទូទៅប្រាក់បៀវត្ស ការផ្ទៀងផ្ទាត់ការទូទាត់ និងសកម្មភាពថ្មីៗ។");

        overviewCards.addClassName("payroll-kpi-grid");

        configureRecentRunsGrid();

        page.add(
                overviewCards,
                sectionTitle("Recent Payroll Runs | ដំណើរការប្រាក់បៀវត្សថ្មីៗ"),
                recentRunsGrid);

        page.setFlexGrow(1, recentRunsGrid);

        add(page);
        setFlexGrow(1, page);
    }

    /**
     * Refreshes the dashboard summary and recent payroll runs.
     */
    void refresh() {
        try {
            DashboardStats stats = service.getDashboardStats();
            overviewCards.removeAll();
            overviewCards.add(
                    kpi(
                            "Net Pay | ប្រាក់សុទ្ធ",
                            usd(stats.netPay()),
                            "Amount payable | ចំនួនត្រូវបើក",
                            VaadinIcon.MONEY,
                            "primary"),

                    kpi(
                            "Employees | បុគ្គលិក",
                            wholeNumber(stats.employeeCount()),
                            "Latest payroll run | ដំណើរការចុងក្រោយ",
                            VaadinIcon.USERS,
                            "neutral"),

                    kpi(
                            "Gross Pay | ប្រាក់សរុប",
                            usd(stats.totalEarnings()),
                            "Total earnings | ចំណូលសរុប",
                            VaadinIcon.CHART,
                            "success"),

                    kpi(
                            "Deductions | ការកាត់",
                            usd(stats.totalDeductions()),
                            "Tax and deductions | ពន្ធ និងការកាត់",
                            VaadinIcon.CALC,
                            "error"),

                    kpi(
                            "Latest Period | រយៈពេលចុងក្រោយ",
                            stats.latestPeriod(),
                            "Payroll cycle | វដ្តប្រាក់បៀវត្ស",
                            VaadinIcon.CALENDAR,
                            "info"),

                    kpi(
                            "Active Runs | ដំណើរការសកម្ម",
                            wholeNumber(stats.activeRuns()),
                            wholeNumber(stats.openPeriods())
                                    + " open period(s) | "
                                    + wholeNumber(stats.openPeriods())
                                    + " រយៈពេលកំពុងបើក",
                            VaadinIcon.REFRESH,
                            stats.activeRuns() > 0 ? "warning" : "neutral"));

            recentRunsGrid.setItems(service.findRecentRuns(10));
        } catch (Exception ex) {
            error(databaseMessage(ex));
        }
    }

    private void configureRecentRunsGrid() {
        configureGrid(recentRunsGrid);
        recentRunsGrid.addClassName("payroll-recent-runs-grid");

        recentRunsGrid
                .addColumn(r -> "%04d-%02d".formatted(r.year(), r.month()))
                .setHeader("Period")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);

        recentRunsGrid
                .addColumn(RunRow::runNumber)
                .setHeader("Run")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);

        recentRunsGrid
                .addColumn(r -> runTypeLabel(r.runType()))
                .setHeader("Type")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);

        recentRunsGrid
                .addComponentColumn(r -> statusBadge(r.status()))
                .setHeader("Status")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);

        recentRunsGrid
                .addColumn(r -> wholeNumber(r.employeeCount()))
                .setHeader("Employees")
                .setComparator(RunRow::employeeCount)
                .setTextAlign(ColumnTextAlign.END)
                .setAutoWidth(true);

        moneyColumn(recentRunsGrid, "Gross Pay", RunRow::totalEarnings);
        moneyColumn(recentRunsGrid, "Deductions", RunRow::totalDeductions);
        moneyColumn(recentRunsGrid, "Net Pay", RunRow::netPay);
        moneyColumn(recentRunsGrid, "First Paid | បើកលើកទី", RunRow::firstPaid);
        moneyColumn(recentRunsGrid, "Final Paid | បើកចុង", RunRow::finalPaid);
    }
}
