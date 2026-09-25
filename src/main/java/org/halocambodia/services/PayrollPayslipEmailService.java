package org.halocambodia.services;

import static org.halocambodia.data.PayrollModels.EmployeePaymentRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollPaymentBatch;
import org.halocambodia.data.PayrollPaymentBatchRepository;
import org.halocambodia.data.ReportService;
import org.halocambodia.enums.PayrollInstallmentType;
import org.halocambodia.enums.PayrollPaymentBatchStatus;
import org.halocambodia.gmail.EmailService;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.PayrollActionPermissions;
import org.halocambodia.services.PayrollPayslipService.PayslipReport;
import org.springframework.stereotype.Service;

/**
 * Coordinates payslip-email delivery outside the Vaadin view.
 *
 * <p>The service deliberately sends sequentially. Payroll email is a controlled
 * business action and we prefer predictable SMTP load and per-employee audit
 * results over uncontrolled parallel delivery.</p>
 */
@Service
public class PayrollPayslipEmailService {

    public static final String EMAIL_SENT = "PAYSLIP_EMAIL_SENT";
    public static final String EMAIL_FAILED = "PAYSLIP_EMAIL_FAILED";
    public static final String EMAIL_SKIPPED = "PAYSLIP_EMAIL_SKIPPED";

    private final PayrollPaymentService paymentService;
    private final PayrollPaymentBatchRepository paymentBatchRepository;
    private final PayrollPayslipService payslipService;
    private final EmailService emailService;
    private final AuthenticatedUser authenticatedUser;

    public PayrollPayslipEmailService(
            PayrollPaymentService paymentService,
            PayrollPaymentBatchRepository paymentBatchRepository,
            PayrollPayslipService payslipService,
            EmailService emailService,
            AuthenticatedUser authenticatedUser) {
        this.paymentService = paymentService;
        this.paymentBatchRepository = paymentBatchRepository;
        this.payslipService = payslipService;
        this.emailService = emailService;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Builds the confirmation summary. With selected IDs, all selected employees
     * are resend targets. With no selection, only employees that still need an
     * email attempt are targets; successful sends are not duplicated.
     */
    public PayslipEmailPlan plan(Long batchId, Set<Long> selectedPaymentIds) {
        requireSendPermission();
        requirePaidPayslipBatch(batchId);

        List<EmployeePaymentRow> rows = paymentService.findEmployeePayments(batchId, "");
        Set<Long> selectedIds = normalizeIds(selectedPaymentIds);
        boolean selectedMode = !selectedIds.isEmpty();
        List<EmployeePaymentRow> scope = selectedMode
                ? selectedRows(rows, selectedIds)
                : rows;

        long alreadySent = scope.stream().filter(PayrollPayslipEmailService::isSent).count();
        long failed = scope.stream().filter(row -> EMAIL_FAILED.equals(row.emailStatus())).count();
        long skipped = scope.stream().filter(row -> EMAIL_SKIPPED.equals(row.emailStatus())).count();
        long notSent = scope.stream().filter(row -> row.emailStatus() == null || row.emailStatus().isBlank()).count();
        long missingEmail = scope.stream().filter(row -> blank(row.personalEmail())).count();

        long targetCount = selectedMode
                ? scope.size()
                : scope.stream().filter(PayrollPayslipEmailService::needsUnsentAttempt).count();

        return new PayslipEmailPlan(
                scope.size(),
                targetCount,
                alreadySent,
                failed,
                skipped,
                notSent,
                missingEmail,
                selectedMode);
    }

    /**
     * Sends selected rows as an explicit resend, or sends only rows that still
     * need an attempt when no employee is selected.
     */
    public PayslipEmailResult send(
            Long batchId,
            Set<Long> selectedPaymentIds,
            String payrollPeriod,
            String installmentLabel) {

        requireSendPermission();
        requirePaidPayslipBatch(batchId);

        List<EmployeePaymentRow> allRows = paymentService.findEmployeePayments(batchId, "");
        Set<Long> selectedIds = normalizeIds(selectedPaymentIds);
        boolean selectedMode = !selectedIds.isEmpty();

        List<EmployeePaymentRow> scope = selectedMode
                ? selectedRows(allRows, selectedIds)
                : allRows;
        List<EmployeePaymentRow> targets = (selectedMode
                ? scope.stream()
                : scope.stream().filter(PayrollPayslipEmailService::needsUnsentAttempt))
                .sorted(Comparator.comparing(
                        EmployeePaymentRow::insuranceNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        int sent = 0;
        int skippedNoEmail = 0;
        int failed = 0;
        int auditFailed = 0;
        List<PayslipEmailIssue> issues = new ArrayList<>();

        for (EmployeePaymentRow employee : targets) {
            String employeeLabel = employeeLabel(employee);
            String recipient = trimToNull(employee.personalEmail());

            if (recipient == null) {
                skippedNoEmail++;
                String detail = "No personal email is configured.";
                issues.add(issue(employee, null, "SKIPPED", detail));
                if (!recordAuditSafely(
                        batchId,
                        employee,
                        "SKIPPED",
                        null,
                        "Payslip email skipped because no personal email is configured.",
                        issues)) {
                    auditFailed++;
                }
                continue;
            }

            if (employee.id() == null) {
                failed++;
                String detail = "Employee payment ID is missing.";
                issues.add(issue(employee, recipient, "FAILED", detail));
                continue;
            }

            try {
                PayslipReport report = payslipService.generateEmployeePayslip(batchId, employee.id());
                byte[] pdf = ReportService.generateReport(report.reportPath(), report.parameters());

                emailService.sendPayrollPayslip(
                        recipient,
                        employeeLabel,
                        payrollPeriod,
                        installmentLabel,
                        report.fileName(),
                        pdf);

                sent++;
                String detail = "Payslip accepted by the outgoing mail server"
                        + suffix("Period", payrollPeriod)
                        + suffix(null, installmentLabel)
                        + suffix("PDF", report.fileName());
                if (!recordAuditSafely(
                        batchId,
                        employee,
                        "SENT",
                        recipient,
                        detail,
                        issues)) {
                    auditFailed++;
                }
            } catch (Exception ex) {
                failed++;
                String detail = safeMessage(ex);
                issues.add(issue(employee, recipient, "FAILED", detail));
                if (!recordAuditSafely(
                        batchId,
                        employee,
                        "FAILED",
                        recipient,
                        detail,
                        issues)) {
                    auditFailed++;
                }
            }
        }

        int alreadySentNotRetried = selectedMode
                ? 0
                : (int) scope.stream().filter(PayrollPayslipEmailService::isSent).count();
        int previouslySkippedWithoutEmail = selectedMode
                ? 0
                : (int) scope.stream()
                        .filter(row -> EMAIL_SKIPPED.equals(row.emailStatus()) && blank(row.personalEmail()))
                        .count();

        return new PayslipEmailResult(
                scope.size(),
                targets.size(),
                sent,
                skippedNoEmail,
                failed,
                auditFailed,
                alreadySentNotRetried,
                previouslySkippedWithoutEmail,
                selectedMode,
                List.copyOf(issues));
    }

    private PayrollPaymentBatch requirePaidPayslipBatch(Long batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException(
                    "Select a paid payment batch first. | សូមជ្រើសរើសកញ្ចប់ដែលបានបើកប្រាក់ជាមុនសិន។");
        }
        PayrollPaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment batch not found. | រកមិនឃើញកញ្ចប់បើកប្រាក់។"));

        boolean firstInstallment = PayrollInstallmentType.FIRST_INSTALLMENT.matches(batch.getInstallmentType());
        boolean settledPayslip = batch.getPayrollRunId() != null
                && (PayrollInstallmentType.FINAL_SETTLEMENT.matches(batch.getInstallmentType())
                        || PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.matches(batch.getInstallmentType()));

        if (!PayrollPaymentBatchStatus.PAID.matches(batch.getStatus())
                || !(firstInstallment || settledPayslip)) {
            throw new IllegalStateException(
                    "Payslip email is available only for a PAID first/final/adjustment batch. "
                            + "| អាចផ្ញើអ៊ីមែលបង្កាន់ដៃបានតែសម្រាប់កញ្ចប់លើកទីមួយ/ចុងក្រោយ/កែតម្រូវដែលបាន PAID។");
        }
        return batch;
    }

    private void requireSendPermission() {
        if (!authenticatedUser.hasPermissionRoute(
                PayrollActionPermissions.PAYSLIP_EMAIL_SEND,
                AccessPageType.UPDATED_PAGE)) {
            throw new IllegalArgumentException(
                    "You do not have permission to send payroll payslip email. "
                            + "| អ្នកមិនមានសិទ្ធិផ្ញើអ៊ីមែលបង្កាន់ដៃប្រាក់បៀវត្សទេ។");
        }
    }

    private boolean recordAuditSafely(
            Long batchId,
            EmployeePaymentRow employee,
            String status,
            String recipient,
            String detail,
            List<PayslipEmailIssue> issues) {
        try {
            paymentService.recordPayslipEmailResult(
                    batchId,
                    employee.id(),
                    employee.payrollEmployeeId(),
                    status,
                    recipient,
                    detail);
            return true;
        } catch (Exception auditException) {
            issues.add(issue(
                    employee,
                    recipient,
                    "AUDIT FAILED",
                    "Email result was not recorded in audit history: " + safeMessage(auditException)));
            return false;
        }
    }

    private static List<EmployeePaymentRow> selectedRows(
            List<EmployeePaymentRow> allRows,
            Set<Long> selectedIds) {
        List<EmployeePaymentRow> selected = allRows.stream()
                .filter(row -> row.id() != null && selectedIds.contains(row.id()))
                .toList();
        if (selected.size() != selectedIds.size()) {
            throw new IllegalArgumentException(
                    "One or more selected employee payments are no longer available. Refresh and try again. "
                            + "| ការបើកប្រាក់បុគ្គលិកមួយចំនួនដែលបានជ្រើសលែងមាន។ សូម Refresh ហើយសាកល្បងម្ដងទៀត។");
        }
        return selected;
    }

    private static Set<Long> normalizeIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean isSent(EmployeePaymentRow row) {
        return row != null && EMAIL_SENT.equals(row.emailStatus());
    }

    /**
     * For batch "Send Unsent", keep successful rows untouched. A row that was
     * previously skipped for no email is also left untouched until a personal
     * email is added; once an address exists it automatically becomes retryable.
     */
    private static boolean needsUnsentAttempt(EmployeePaymentRow row) {
        if (row == null || isSent(row)) {
            return false;
        }
        return !EMAIL_SKIPPED.equals(row.emailStatus()) || !blank(row.personalEmail());
    }

    private static String employeeLabel(EmployeePaymentRow employee) {
        if (employee == null) {
            return "Employee";
        }
        if (employee.nameEn() != null && !employee.nameEn().isBlank()) {
            return employee.nameEn().trim();
        }
        if (employee.insuranceNo() != null) {
            return "Employee " + employee.insuranceNo();
        }
        return employee.employeeId() == null ? "Employee" : "Employee " + employee.employeeId();
    }

    private static PayslipEmailIssue issue(
            EmployeePaymentRow employee,
            String recipient,
            String status,
            String detail) {
        return new PayslipEmailIssue(
                employee == null ? null : employee.id(),
                employee == null ? null : employee.insuranceNo(),
                employeeLabel(employee),
                recipient,
                status,
                detail);
    }

    private static String suffix(String label, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "";
        }
        return label == null || label.isBlank()
                ? " · " + normalized
                : " · " + label + ": " + normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String value = current.getMessage();
        if (value == null || value.isBlank()) {
            value = throwable.getMessage();
        }
        return value == null || value.isBlank()
                ? throwable.getClass().getSimpleName()
                : value.trim();
    }

    public record PayslipEmailPlan(
            long scopeCount,
            long targetCount,
            long sentCount,
            long failedCount,
            long skippedCount,
            long notSentCount,
            long missingEmailCount,
            boolean selectedMode) {
    }

    public record PayslipEmailResult(
            int scopeCount,
            int targetCount,
            int sent,
            int skippedNoEmail,
            int failed,
            int auditFailed,
            int alreadySentNotRetried,
            int previouslySkippedWithoutEmail,
            boolean selectedMode,
            List<PayslipEmailIssue> issues) {
    }

    public record PayslipEmailIssue(
            Long employeePaymentId,
            Integer insuranceNo,
            String employeeName,
            String recipient,
            String status,
            String detail) {
    }
}
