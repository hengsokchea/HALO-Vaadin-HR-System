package org.halocambodia.views.payroll;

import static org.halocambodia.views.payroll.PayrollViewSupport.configureGrid;

import java.util.List;

import org.halocambodia.services.PayrollPayslipEmailService.PayslipEmailIssue;
import org.halocambodia.services.PayrollPayslipEmailService.PayslipEmailResult;
import org.halocambodia.views.CustomDialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/** Read-only result summary for one payslip-email batch action. */
final class PayrollPayslipEmailResultDialog extends CustomDialog {

    PayrollPayslipEmailResultDialog(PayslipEmailResult result) {
        super("Payslip Email Result | លទ្ធផលផ្ញើអ៊ីមែលបង្កាន់ដៃ");
        addClassName("payroll-dialog");
        setWidth("min(1050px, 96vw)");
        setHeight("min(680px, 92vh)");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        PayslipEmailResult safe = result == null
                ? new PayslipEmailResult(0, 0, 0, 0, 0, 0, 0, 0, false, List.of())
                : result;

        HorizontalLayout metrics = new HorizontalLayout(
                metric("Target", safe.targetCount()),
                metric("Sent", safe.sent()),
                metric("Failed", safe.failed()),
                metric("Skipped", safe.skippedNoEmail()),
                metric("Audit Failed", safe.auditFailed()));
        metrics.setWidthFull();
        metrics.setWrap(true);
        metrics.setSpacing(true);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(true);
        content.add(metrics);

        if (!safe.selectedMode()
                && (safe.alreadySentNotRetried() > 0 || safe.previouslySkippedWithoutEmail() > 0)) {
            Span protectedRows = new Span(
                    "Not resent: " + safe.alreadySentNotRetried() + " already sent"
                            + (safe.previouslySkippedWithoutEmail() > 0
                                    ? " · " + safe.previouslySkippedWithoutEmail()
                                            + " previously skipped with no personal email"
                                    : "")
                            + " | មិនបានផ្ញើស្ទួនអ្នកដែលបានផ្ញើរួច។");
            protectedRows.addClassName("payroll-processing-summary");
            content.add(protectedRows);
        }

        List<PayslipEmailIssue> issues = safe.issues() == null ? List.of() : safe.issues();
        if (!issues.isEmpty()) {
            content.add(new H4("Issues / Skipped Employees | បញ្ហា / បុគ្គលិកដែលបានរំលង"));

            Grid<PayslipEmailIssue> grid = new Grid<>();
            configureGrid(grid);
            grid.setSizeFull();
            grid.addColumn(issue -> issue.insuranceNo() == null ? "-" : issue.insuranceNo().toString())
                    .setHeader("Insurance")
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setSortable(true);
            grid.addColumn(PayslipEmailIssue::employeeName)
                    .setHeader("Employee")
                    .setAutoWidth(true)
                    .setSortable(true);
            grid.addColumn(issue -> blankDash(issue.recipient()))
                    .setHeader("Personal Email")
                    .setAutoWidth(true);
            grid.addColumn(PayslipEmailIssue::status)
                    .setHeader("Result")
                    .setAutoWidth(true)
                    .setFlexGrow(0);
            grid.addColumn(PayslipEmailIssue::detail)
                    .setHeader("Detail")
                    .setAutoWidth(true)
                    .setFlexGrow(1);
            grid.setItems(issues);
            content.add(grid);
            content.setFlexGrow(1, grid);
        } else {
            Span success = new Span(
                    "All targeted payslip emails were accepted by the outgoing mail server and audited successfully. "
                            + "| អ៊ីមែលបង្កាន់ដៃគោលដៅទាំងអស់ត្រូវបានម៉ាស៊ីនមេអ៊ីមែលទទួល និងកត់ត្រាសវនកម្មដោយជោគជ័យ។");
            success.getElement().getThemeList().add("badge success pill");
            content.add(success);
        }

        add(content);

        Button close = new Button("Close | បិទ", event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(close);
    }

    private static Div metric(String label, int value) {
        Div box = new Div();
        box.addClassName("payroll-attendance-control");
        box.getStyle().set("min-width", "135px").set("padding", "10px 14px");

        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("display", "block").set("font-size", "var(--lumo-font-size-s)");
        Span valueSpan = new Span(Integer.toString(value));
        valueSpan.getStyle().set("display", "block").set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "700");
        box.add(labelSpan, valueSpan);
        return box;
    }

    private static String blankDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
