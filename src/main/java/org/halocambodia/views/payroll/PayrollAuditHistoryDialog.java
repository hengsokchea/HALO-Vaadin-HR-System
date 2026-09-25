package org.halocambodia.views.payroll;

import static org.halocambodia.views.payroll.PayrollViewSupport.configureGrid;
import static org.halocambodia.views.payroll.PayrollViewSupport.nvl;
import static org.halocambodia.views.payroll.PayrollViewSupport.statusLabel;

import java.util.List;
import java.util.Locale;

import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.PayrollModels.PayrollAuditEventRow;
import org.halocambodia.views.CustomDialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

/** Read-only view of immutable payroll audit events. */
final class PayrollAuditHistoryDialog extends CustomDialog {

    private final List<PayrollAuditEventRow> allRows;
    private final Grid<PayrollAuditEventRow> grid = new Grid<>();
    private final TextField search = new TextField();
    private final Span summary = new Span();

    PayrollAuditHistoryDialog(String title, List<PayrollAuditEventRow> rows) {
        super(title);
        addClassName("payroll-dialog");
        this.allRows = rows == null ? List.of() : List.copyOf(rows);
        setWidth("min(1250px, 96vw)");
        setHeight("min(760px, 94vh)");
        setResizable(true);
        setDraggable(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        configureGrid(grid);
        configureColumns();

        search.setPlaceholder(
                "Search event, user, employee, status or reason... | ស្វែងរកសកម្មភាព អ្នកប្រើ បុគ្គលិក ស្ថានភាព ឬមូលហេតុ...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setWidth("min(620px, 100%)");
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(event -> refreshRows());

        summary.addClassName("payroll-processing-summary");

        Paragraph help = new Paragraph(
                "This history is read-only and comes from the immutable payroll audit log. "
                        + "| ប្រវត្តិនេះអាចមើលបានតែប៉ុណ្ណោះ និងមកពីកំណត់ត្រាសវនកម្មប្រាក់បៀវត្សដែលមិនអាចកែប្រែបាន។");
        help.addClassName("payroll-page-subtitle");

        VerticalLayout content = new VerticalLayout(help, search, summary, grid);
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(true);
        content.setFlexGrow(1, grid);
        add(content);

        Button close = new Button("Close | បិទ", event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(close);

        refreshRows();
    }

    private void configureColumns() {
        grid.addColumn(row -> row.eventAt() == null ? "-" : DateTimeUtilFormart.DATE_TIME_FORMATTER.format (row.eventAt()))
                .setHeader("Date / Time | កាលបរិច្ឆេទ / ម៉ោង")
                .setFrozen(true)
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true);

        grid.addComponentColumn(row -> auditEventBadge(row.eventType()))
                .setHeader("Event | សកម្មភាព")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(PayrollAuditEventRow::actorDisplay)
                .setHeader("User | អ្នកប្រើ")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(PayrollAuditEventRow::employeeDisplay)
                .setHeader("Employee | បុគ្គលិក")
                .setAutoWidth(true);

        grid.addColumn(PayrollAuditHistoryDialog::statusTransition)
                .setHeader("Status Change | ការប្តូរស្ថានភាព")
                .setAutoWidth(true);

        grid.addColumn(row -> blankDash(row.reason()))
                .setHeader("Reason | មូលហេតុ")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(row -> blankDash(row.eventDetail()))
                .setHeader("Detail | ព័ត៌មានលម្អិត")
                .setAutoWidth(true)
                .setFlexGrow(1);
    }

    private void refreshRows() {
        String term = nvl(search.getValue()).trim().toLowerCase(Locale.ROOT);
        List<PayrollAuditEventRow> filtered = term.isBlank()
                ? allRows
                : allRows.stream().filter(row -> searchableText(row).contains(term)).toList();
        grid.setItems(filtered);
        summary.setText(filtered.size() + " event(s) | សកម្មភាព " + filtered.size()
                + (filtered.size() == allRows.size() ? "" : " · Total | សរុប " + allRows.size()));
    }

    private static String searchableText(PayrollAuditEventRow row) {
        return String.join(" ",
                nvl(row.eventType()),
                auditEventLabel(row.eventType()),
                row.actorDisplay(),
                row.employeeDisplay(),
                nvl(row.fromStatus()),
                nvl(row.toStatus()),
                statusTransition(row),
                nvl(row.reason()),
                nvl(row.eventDetail()))
                .toLowerCase(Locale.ROOT);
    }

    private static Span auditEventBadge(String eventType) {
        Span badge = new Span(auditEventLabel(eventType));
        badge.getElement().getThemeList().add(switch (nvl(eventType)) {
            case "RUN_APPROVED", "PAYMENT_APPROVED", "PAYMENT_PAID", "RUN_PAID", "PERIOD_CLOSED",
                    "PAYSLIP_EMAIL_SENT" -> "badge success pill";
            case "RETURNED_FOR_CORRECTION", "PAYMENT_CANCELLED", "PAYSLIP_EMAIL_FAILED" ->
                    "badge error pill";
            case "PAYSLIP_EMAIL_SKIPPED" -> "badge contrast pill";
            case "SENT_FOR_REVIEW", "RUN_REVIEWED", "RUN_CALCULATED", "RUN_RECALCULATED", "EMPLOYEE_RECALCULATED" ->
                    "badge primary pill";
            default -> "badge contrast pill";
        });
        return badge;
    }

    private static String auditEventLabel(String eventType) {
        return switch (nvl(eventType)) {
            case "RUN_CREATED" -> "Run Created | បង្កើតដំណើរការ";
            case "RUN_CALCULATED" -> "Run Calculated | គណនាដំណើរការ";
            case "RUN_RECALCULATED" -> "Run Recalculated | គណនាដំណើរការឡើងវិញ";
            case "SENT_FOR_REVIEW" -> "Sent for Review | ផ្ញើទៅពិនិត្យ";
            case "RUN_REVIEWED" -> "Review Completed | បានបញ្ចប់ការពិនិត្យ";
            case "RETURNED_FOR_CORRECTION" -> "Returned for Correction | បញ្ជូនត្រឡប់ទៅកែតម្រូវ";
            case "EMPLOYEE_RECALCULATED" -> "Employee Recalculated | គណនាបុគ្គលិកឡើងវិញ";
            case "RUN_APPROVED" -> "Run Approved | អនុម័តដំណើរការ";
            case "PAYMENT_GENERATED" -> "Payment Generated | បង្កើតការបើកប្រាក់";
            case "PAYMENT_APPROVED" -> "Payment Approved | អនុម័តការបើកប្រាក់";
            case "PAYMENT_PAID" -> "Payment Paid | បានបើកប្រាក់";
            case "PAYMENT_CANCELLED" -> "Payment Cancelled | បោះបង់ការបើកប្រាក់";
            case "PAYSLIP_EMAIL_SENT" -> "Payslip Email Sent | បានផ្ញើអ៊ីមែលបង្កាន់ដៃ";
            case "PAYSLIP_EMAIL_FAILED" -> "Payslip Email Failed | ផ្ញើអ៊ីមែលបង្កាន់ដៃបរាជ័យ";
            case "PAYSLIP_EMAIL_SKIPPED" -> "Payslip Email Skipped | បានរំលងអ៊ីមែលបង្កាន់ដៃ";
            case "RUN_PAID" -> "Payroll Paid | ប្រាក់បៀវត្សបានបើក";
            case "PERIOD_CLOSED" -> "Period Closed | បិទរយៈពេល";
            default -> nvl(eventType).replace('_', ' ');
        };
    }

    private static String statusTransition(PayrollAuditEventRow row) {
        String from = row.fromStatus();
        String to = row.toStatus();
        if ((from == null || from.isBlank()) && (to == null || to.isBlank())) {
            return "-";
        }
        if (from == null || from.isBlank()) {
            return statusLabel(to);
        }
        if (to == null || to.isBlank()) {
            return statusLabel(from);
        }
        return statusLabel(from) + " → " + statusLabel(to);
    }

    private static String blankDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
