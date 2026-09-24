package org.halocambodia.services;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.PayrollPayslipRepository;
import org.halocambodia.data.PayrollPayslipRow;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.stereotype.Service;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class PayrollPayslipService {

    private static final String TEMPLATE = "report_embed/payroll_payslip.jrxml";

    private final PayrollPayslipRepository payslipRepository;
    private final AuthenticatedUser authenticatedUser;

    public PayrollPayslipService(
            PayrollPayslipRepository payslipRepository,
            AuthenticatedUser authenticatedUser) {
        this.payslipRepository = payslipRepository;
        this.authenticatedUser = authenticatedUser;
    }

    public PayslipReport generateEmployeePayslip(Long batchId, Long employeePaymentId) {
        if (employeePaymentId == null) {
            throw new IllegalArgumentException(
                    "Select a paid employee payment first. | សូមជ្រើសរើសការបើកប្រាក់បុគ្គលិកដែលបាន PAID ជាមុនសិន។");
        }
        return generatePayslips(batchId, Set.of(employeePaymentId));
    }

    public PayslipReport generateBatchPayslips(Long batchId) {
        return generatePayslips(batchId, Set.of());
    }

    /**
     * Prepares the Jasper report reference and data source for payslip preview.
     *
     * <p>No PDF byte array is generated here. The returned report reference is
     * opened by {@code PreviewReport}, the same report-preview flow used by
     * National Staff ID Card.</p>
     *
     * <p>When {@code employeePaymentIds} is null/empty, all eligible employees
     * in the paid batch are included. Otherwise, only the selected employee
     * payment rows are included.</p>
     */
    public PayslipReport generatePayslips(Long batchId, Set<Long> employeePaymentIds) {
        requireViewPermission();
        if (batchId == null) {
            throw new IllegalArgumentException(
                    "Select a paid payment batch first. | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់ដែលបាន PAID ជាមុនសិន។");
        }

        Set<Long> selectedIds = employeePaymentIds == null
                ? Set.of()
                : employeePaymentIds.stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<PayrollPayslipRow> allRows = payslipRepository.findPaidBatchRows(batchId, null);
        List<PayrollPayslipRow> rows = selectedIds.isEmpty()
                ? allRows
                : allRows.stream()
                        .filter(row -> selectedIds.contains(row.getPayrollEmployeePaymentId()))
                        .toList();

        if (rows.isEmpty()) {
            throw new IllegalStateException(selectedIds.isEmpty()
                    ? "Payslips are available only for a PAID first installment, final settlement, or adjustment settlement. "
                            + "| បង្កាន់ដៃប្រាក់បៀវត្សមានសម្រាប់កញ្ចប់លើកទីមួយ ចុងក្រោយ ឬកែតម្រូវដែលបាន PAID ប៉ុណ្ណោះ។"
                    : "No selected employee has an available payslip in this PAID batch. "
                            + "| មិនមានបង្កាន់ដៃសម្រាប់បុគ្គលិកដែលបានជ្រើសនៅក្នុងកញ្ចប់ PAID នេះទេ។");
        }

        PayrollPayslipRow first = rows.getFirst();
        String month = "%04d-%02d".formatted(first.getPayrollYear(), first.getPayrollMonth());
        String type = first.getInstallmentType() == null
                ? "PAYROLL"
                : first.getInstallmentType().toUpperCase(Locale.ROOT);
        boolean firstInstallment = "FIRST_INSTALLMENT".equals(type);

        String fileName;
        if (selectedIds.size() == 1) {
            String safeInsurance = first.getInsuranceNo() == null
                    ? String.valueOf(first.getEmployeeId())
                    : first.getInsuranceNo().toString();
            fileName = "Payslip_" + month + "_" + type + "_" + safeInsurance + ".pdf";
        } else {
            fileName = selectedIds.isEmpty()
                    ? "Payslips_" + month + "_" + type + ".pdf"
                    : "Payslips_" + month + "_" + type + "_Selected_" + selectedIds.size() + ".pdf";
        }

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(
                "REPORT_TITLE",
                firstInstallment
                        ? "FIRST INSTALLMENT PAYSLIP | បង្កាន់ដៃបើកប្រាក់លើកទីមួយ"
                        : "PAYSLIP | បង្កាន់ដៃប្រាក់បៀវត្ស");

        // ReportService detects this Jasper built-in parameter and fills the
        // Jasper template using the bean data source instead of a JDBC query.
        parameters.put(
                JRParameter.REPORT_DATA_SOURCE,
                new JRBeanCollectionDataSource(rows, false));

        return new PayslipReport(TEMPLATE, fileName, parameters);
    }

    private void requireViewPermission() {
        if (!authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE)) {
            throw new IllegalArgumentException(
                    "You do not have permission to view payroll payslips. "
                            + "| អ្នកមិនមានសិទ្ធិមើលបង្កាន់ដៃប្រាក់បៀវត្សទេ។");
        }
    }

    /**
     * Report reference passed to PreviewReport. This intentionally contains no
     * generated PDF bytes.
     */
    public record PayslipReport(
            String reportPath,
            String fileName,
            HashMap<String, Object> parameters) {
    }
}
