package org.halocambodia.views.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.halocambodia.data.PayrollEmployeeResultRow;
import org.halocambodia.data.PayrollPayslipRow;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PayrollEmployeeResultService;
import org.halocambodia.views.PreviewReport;
import org.halocambodia.views.access_denied.AccessDeniedView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

/**
 * Employee self-service payslip page.
 *
 * Only PAID payroll results belonging to the authenticated employee
 * are exposed by the service.
 */
@Route(value = "my-payroll-result", layout = org.halocambodia.views.MainLayout.class)
@PageTitle("Payslips | បង្កាន់ដៃប្រាក់ខែ")
@PermitAll
public class PayrollEmployeeResultView  extends VerticalLayout  implements BeforeEnterObserver {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH );

    private final PayrollEmployeeResultService service;
    private final AuthenticatedUser authenticatedUser;

    private final Grid<PayrollEmployeeResultRow> grid =new Grid<>();

    private final VerticalLayout detailPanel = new VerticalLayout();

    private final VerticalLayout emptyState = new VerticalLayout();

    private final Span resultCount = new Span();

    public PayrollEmployeeResultView(PayrollEmployeeResultService service, AuthenticatedUser authenticatedUser) {

        this.service = service;
        this.authenticatedUser = authenticatedUser;

        configureLayout();
        configureHeader();
        configureGrid();
        configureDetailPanel();
        configureEmptyState();

        addAttachListener(event -> refresh());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        if (authenticatedUser.get().isEmpty()) {
            event.rerouteTo(AccessDeniedView.class);
        }
    }

    private void configureLayout() {

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        getStyle()
                .set("max-width", "1400px")
                .set("margin", "0 auto");
    }

    private void configureHeader() {

        H2 title =
                new H2(
                        "Payslips | បង្កាន់ដៃប្រាក់ខែ"
                );

        title.getStyle()
                .set("margin", "0");

        Paragraph description =
                new Paragraph(
                        "View your completed salary payments and payslips. "
                                + "| មើលការទូទាត់ប្រាក់ខែ និងបង្កាន់ដៃប្រាក់ខែរបស់អ្នក។"
                );

        description.getStyle()
                .set(
                        "color",
                        "var(--lumo-secondary-text-color)"
                )
                .set("margin-top", "0");

        Button refresh =
                new Button(
                        "Refresh | ផ្ទុកឡើងវិញ",
                        VaadinIcon.REFRESH.create(),
                        event -> refresh()
                );

        refresh.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY
        );

        HorizontalLayout actions =
                new HorizontalLayout(
                        resultCount,
                        refresh
                );

        actions.setDefaultVerticalComponentAlignment(
                Alignment.CENTER
        );

        actions.getStyle()
                .set("margin-left", "auto");

        HorizontalLayout header =
                new HorizontalLayout();

        VerticalLayout titleArea =
                new VerticalLayout(
                        title,
                        description
                );

        titleArea.setPadding(false);
        titleArea.setSpacing(false);

        header.add(
                titleArea,
                actions
        );

        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(
                Alignment.CENTER
        );

        header.expand(titleArea);

        add(header);
    }

    private void configureGrid() {

        grid.setWidthFull();
        grid.setHeight("340px");

        grid.setSelectionMode(
                Grid.SelectionMode.SINGLE
        );

        grid.addColumn(this::formatPeriod)
                .setHeader(
                        "Period | រយៈពេល"
                )
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        grid.addColumn(
                        row ->
                                formatDate(
                                        row.getPaymentDate()
                                )
                )
                .setHeader(
                        "Payment Date | ថ្ងៃបើកប្រាក់"
                )
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true);

        grid.addColumn(
                        row ->
                                paymentType(
                                        row.getInstallmentType()
                                )
                )
                .setHeader(
                        "Payment | ការទូទាត់"
                )
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(
                        PayrollEmployeeResultRow::getCurrency
                )
                .setHeader(
                        "Currency | រូបិយប័ណ្ណ"
                )
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(
                        row ->
                                money(
                                        row.getPaymentAmount()
                                )
                )
                .setHeader(
                        "Amount | ចំនួនទឹកប្រាក់"
                )
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(row -> {

            Button payslip = new Button(
                    "Payslip | បង្កាន់ដៃ",
                    VaadinIcon.FILE_TEXT.create()
            );

            payslip.addThemeVariants(
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_SMALL
            );

            payslip.addClickListener(
                    event -> openPayslip(row)
            );

            return payslip;

        })
        .setHeader("Payslip | បង្កាន់ដៃ")
        .setAutoWidth(true)
        .setFlexGrow(0);

        grid.addSelectionListener(event ->
                event.getFirstSelectedItem()
                        .ifPresent(this::showDetail)
        );

        add(grid);
    }

    private void configureDetailPanel() {

        detailPanel.setWidthFull();
        detailPanel.setPadding(true);
        detailPanel.setSpacing(true);

        detailPanel.getStyle()
                .set(
                        "border",
                        "1px solid var(--lumo-contrast-10pct)"
                )
                .set(
                        "border-radius",
                        "var(--lumo-border-radius-l)"
                )
                .set(
                        "background",
                        "var(--lumo-base-color)"
                );

        detailPanel.setVisible(false);

        add(detailPanel);
    }

    private void configureEmptyState() {

        emptyState.setWidthFull();
        emptyState.setAlignItems(
                Alignment.CENTER
        );

        emptyState.setPadding(true);
        emptyState.setSpacing(false);

        emptyState.getStyle()
                .set("padding", "48px 16px")
                .set(
                        "border",
                        "1px dashed var(--lumo-contrast-20pct)"
                )
                .set(
                        "border-radius",
                        "var(--lumo-border-radius-l)"
                );

        var icon =
                VaadinIcon.FILE_TEXT.create();

        icon.setSize("48px");

        icon.getStyle()
                .set(
                        "color",
                        "var(--lumo-secondary-text-color)"
                );

        H3 title =
                new H3(
                        "No payslips available "
                                + "| មិនទាន់មានបង្កាន់ដៃប្រាក់ខែ"
                );

        Paragraph message =
                new Paragraph(
                        "Your payslips will appear here after payroll payment is completed. "
                                + "| បង្កាន់ដៃប្រាក់ខែរបស់អ្នកនឹងបង្ហាញនៅទីនេះ បន្ទាប់ពីការទូទាត់ប្រាក់ខែបានបញ្ចប់។"
                );

        message.getStyle()
                .set(
                        "color",
                        "var(--lumo-secondary-text-color)"
                )
                .set(
                        "text-align",
                        "center"
                );

        emptyState.add(
                icon,
                title,
                message
        );

        emptyState.setVisible(false);

        add(emptyState);
    }

    private void refresh() {

        try {

            List<PayrollEmployeeResultRow> rows =
                    service.findMyResults();

            grid.setItems(rows);

            resultCount.setText(
                    rows.isEmpty()
                            ? ""
                            : rows.size()
                                    + " payslip(s) | "
                                    + rows.size()
                                    + " បង្កាន់ដៃ"
            );

            boolean hasResults =
                    !rows.isEmpty();

            grid.setVisible(hasResults);
            emptyState.setVisible(!hasResults);

            detailPanel.removeAll();
            detailPanel.setVisible(false);

            if (hasResults) {

                PayrollEmployeeResultRow first =
                        rows.getFirst();

                grid.select(first);
                showDetail(first);
            }

        } catch (Exception ex) {

            grid.setItems(List.of());
            grid.setVisible(false);

            detailPanel.removeAll();
            detailPanel.setVisible(false);

            emptyState.setVisible(true);

            resultCount.setText("");

            showError(
                    ex.getMessage() == null
                            ? "Unable to load payslips. | មិនអាចផ្ទុកបង្កាន់ដៃប្រាក់ខែបានទេ។"
                            : ex.getMessage()
            );
        }
    }

    private void showDetail(PayrollEmployeeResultRow selectedRow) {

        try {

            List<PayrollPayslipRow> rows =
                    service.findMyPayslipRows(selectedRow);

            if (rows.isEmpty()) {
                return;
            }

            PayrollPayslipRow payroll = rows.getFirst();

            detailPanel.removeAll();

            H3 title = new H3(
                    formatPeriod(selectedRow)
                            + " — "
                            + paymentType(selectedRow.getInstallmentType())
            );

            title.getStyle()
                    .set("margin", "0");

            Span paymentDate = new Span(
                    "Payment Date | ថ្ងៃបើកប្រាក់: "
                            + formatDate(selectedRow.getPaymentDate())
            );

            paymentDate.getStyle()
                    .set(
                            "color",
                            "var(--lumo-secondary-text-color)"
                    );

            VerticalLayout heading = new VerticalLayout(
                    title,
                    paymentDate
            );

            heading.setPadding(false);
            heading.setSpacing(false);

            detailPanel.add(heading);

            detailPanel.add(
                    createSummary(payroll)
            );

            detailPanel.setVisible(true);

        } catch (Exception ex) {

            detailPanel.removeAll();
            detailPanel.setVisible(false);

            showError(ex.getMessage());
        }
    }

    private Component createSummary(
            PayrollPayslipRow payroll) {

        VerticalLayout summary =
                new VerticalLayout();

        summary.setPadding(false);
        summary.setSpacing(false);
        summary.setWidthFull();

        BigDecimal basicSalary =
                zero(payroll.getBasicSalary());

        BigDecimal totalEarnings =
                zero(payroll.getTotalEarnings());

        BigDecimal totalDeductions =
                zero(payroll.getTotalDeductions());

        BigDecimal nssf =
                zero(
                        payroll.getEmployeeContribution()
                );

        BigDecimal salaryTax =
                zero(payroll.getSalaryTax());

        BigDecimal netSalary =
                zero(payroll.getNetSalary());

        BigDecimal paymentAmount =
                zero(payroll.getPaymentAmount());

        /*
         * totalEarnings normally includes the basic salary.
         * Display the remaining earnings separately.
         */
        BigDecimal otherEarnings =
                totalEarnings.subtract(
                        basicSalary
                );

        if (otherEarnings.signum() < 0) {
            otherEarnings = BigDecimal.ZERO;
        }

        /*
         * totalDeductions may include tax/NSSF depending on the
         * payroll snapshot. Calculate only the remaining amount
         * for the employee-facing "Other Deductions" line.
         */
        BigDecimal otherDeductions =
                totalDeductions
                        .subtract(salaryTax)
                        .subtract(nssf);

        if (otherDeductions.signum() < 0) {
            otherDeductions =
                    BigDecimal.ZERO;
        }

        summary.add(
                summaryRow(
                        "Basic Salary | ប្រាក់ខែគោល",
                        moneyWithCurrency(
                                basicSalary,
                                payroll.getCurrency()
                        ),
                        false
                )
        );

        if (otherEarnings.signum() != 0) {
            summary.add(
                    summaryRow(
                            "Other Earnings | ចំណូលផ្សេងៗ",
                            moneyWithCurrency(
                                    otherEarnings,
                                    payroll.getCurrency()
                            ),
                            false
                    )
            );
        }

        summary.add(
                summaryRow(
                        "Gross Earnings | ចំណូលសរុប",
                        moneyWithCurrency(
                                totalEarnings,
                                payroll.getCurrency()
                        ),
                        true
                )
        );

        summary.add(
                separator()
        );

        if (nssf.signum() != 0) {
            summary.add(
                    summaryRow(
                            "NSSF | ប.ស.ស.",
                            moneyWithCurrency(
                                    nssf,
                                    payroll.getCurrency()
                            ),
                            false
                    )
            );
        }

        if (salaryTax.signum() != 0) {
            summary.add(
                    summaryRow(
                            "Salary Tax | ពន្ធលើប្រាក់បៀវត្ស",
                            moneyWithCurrency(
                                    salaryTax,
                                    payroll.getCurrency()
                            ),
                            false
                    )
            );
        }

        if (otherDeductions.signum() != 0) {
            summary.add(
                    summaryRow(
                            "Other Deductions | ការកាត់ផ្សេងៗ",
                            moneyWithCurrency(
                                    otherDeductions,
                                    payroll.getCurrency()
                            ),
                            false
                    )
            );
        }

        summary.add(
                summaryRow(
                        "Total Deductions | ការកាត់សរុប",
                        moneyWithCurrency(
                                totalDeductions,
                                payroll.getCurrency()
                        ),
                        true
                )
        );

        summary.add(
                separator()
        );

        /*
         * Net Salary represents the complete payroll result.
         * Payment Amount is the amount actually paid in this
         * particular installment/payment.
         */
        summary.add(
                summaryRow(
                        "Net Salary | ប្រាក់ខែសុទ្ធ",
                        moneyWithCurrency(
                                netSalary,
                                payroll.getCurrency()
                        ),
                        true
                )
        );

        if (paymentAmount.compareTo(netSalary) != 0) {

            summary.add(
                    summaryRow(
                            "Paid This Time | បានបើកលើកនេះ",
                            moneyWithCurrency(
                                    paymentAmount,
                                    payroll.getCurrency()
                            ),
                            true
                    )
            );
        }

        if (zero(payroll.getPreviousPaidAmount())
                .signum() != 0) {

            summary.add(
                    summaryRow(
                            "Previously Paid | បានបើកមុន",
                            moneyWithCurrency(
                                    payroll.getPreviousPaidAmount(),
                                    payroll.getCurrency()
                            ),
                            false
                    )
            );
        }

        if (zero(payroll.getCarryForwardAmount())
                .signum() != 0) {

            summary.add(
                    summaryRow(
                            "Carry Forward | បន្តទូទាត់",
                            moneyWithCurrency(
                                    payroll.getCarryForwardAmount(),
                                    payroll.getCurrency()
                            ),
                            false
                    )
            );
        }

        return summary;
    }

    private Component summaryRow(
            String label,
            String value,
            boolean emphasized) {

        Span labelComponent =
                new Span(label);

        Span valueComponent =
                new Span(value);

        valueComponent.getStyle()
                .set("font-variant-numeric", "tabular-nums")
                .set("text-align", "right");

        if (emphasized) {

            labelComponent.getStyle()
                    .set("font-weight", "600");

            valueComponent.getStyle()
                    .set("font-weight", "700");
        }

        HorizontalLayout row =
                new HorizontalLayout(
                        labelComponent,
                        valueComponent
                );

        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);
        row.setDefaultVerticalComponentAlignment(
                Alignment.CENTER
        );

        row.expand(labelComponent);

        row.getStyle()
                .set("padding", "8px 0");

        return row;
    }

    private Component separator() {

        Div line =
                new Div();

        line.setWidthFull();

        line.getStyle()
                .set(
                        "border-top",
                        "1px solid var(--lumo-contrast-10pct)"
                )
                .set(
                        "margin",
                        "6px 0"
                );

        return line;
    }

    private void openPayslip(
            PayrollEmployeeResultRow row) {

        try {

            var report =
                    service.generatePayslip(row);

            new PreviewReport(
                    report.reportPath(),
                    report.parameters(),
                    false,
                    "Payslip | បង្កាន់ដៃប្រាក់ខែ",
                    "បង្កាន់ដៃប្រាក់ខែ",
                    report.fileName()
            ).open();

        } catch (Exception ex) {

            showError(
                    ex.getMessage()
            );
        }
    }

    private void showError(String message) {

        Notification notification =
                Notification.show(
                        message == null
                                ? "Unable to load payroll information. "
                                        + "| មិនអាចផ្ទុកព័ត៌មានប្រាក់បៀវត្សបានទេ។"
                                : message,
                        5000,
                        Notification.Position.MIDDLE
                );

        notification.addThemeVariants(
                NotificationVariant.LUMO_ERROR
        );
    }

    private String formatPeriod(
            PayrollEmployeeResultRow row) {

        if (row.getPayrollYear() != null
                && row.getPayrollMonth() != null
                && row.getPayrollMonth() >= 1
                && row.getPayrollMonth() <= 12) {

            String month =
                    Month.of(
                            row.getPayrollMonth()
                    ).getDisplayName(
                            TextStyle.FULL,
                            Locale.ENGLISH
                    );

            return month
                    + " "
                    + row.getPayrollYear();
        }

        if (row.getPeriodStart() != null
                && row.getPeriodEnd() != null) {

            return formatDate(
                    row.getPeriodStart()
            )
                    + " → "
                    + formatDate(
                            row.getPeriodEnd()
                    );
        }

        return "-";
    }

    private static String formatDate(
            LocalDate date) {

        return date == null
                ? "-"
                : DATE.format(date);
    }

    private static String paymentType(
            String value) {

        if (value == null
                || value.isBlank()) {
            return "-";
        }

        return switch (
                value.trim()
                        .toUpperCase(Locale.ROOT)
        ) {

            case "FIRST_INSTALLMENT" ->
                    "First Salary | ប្រាក់ខែលើកទី១";

            case "FINAL_SETTLEMENT" ->
                    "Final Salary | ប្រាក់ខែចុងក្រោយ";

            case "ADJUSTMENT_SETTLEMENT" ->
                    "Adjustment | ការកែតម្រូវ";

            default ->
                    value.replace(
                            '_',
                            ' '
                    );
        };
    }

    private static String moneyWithCurrency(
            BigDecimal value,
            String currency) {

        String amount =
                money(value);

        if (currency == null
                || currency.isBlank()) {
            return amount;
        }

        if ("USD".equalsIgnoreCase(currency)) {
            return "$" + amount;
        }

        if ("KHR".equalsIgnoreCase(currency)) {
            return amount + " KHR";
        }

        return amount
                + " "
                + currency;
    }

    private static String money(
            BigDecimal value) {

        return zero(value)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }

    private static BigDecimal zero(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}