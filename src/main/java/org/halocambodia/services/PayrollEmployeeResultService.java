package org.halocambodia.services;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.halocambodia.data.PayrollEmployeeResultRepository;
import org.halocambodia.data.PayrollEmployeeResultRow;
import org.halocambodia.data.PayrollPayslipRepository;
import org.halocambodia.data.PayrollPayslipRow;
import org.halocambodia.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Employee self-service payroll result service.
 *
 * Security rules:
 * - Employee identity always comes from the authenticated user.
 * - The UI never supplies an employee id.
 * - Payment ownership is checked again before payroll detail or payslip access.
 */
@Service
public class PayrollEmployeeResultService {

    private static final String TEMPLATE =
            "report_embed/payroll_payslip.jasper";

    private final PayrollEmployeeResultRepository resultRepository;
    private final PayrollPayslipRepository payslipRepository;
    private final AuthenticatedUser authenticatedUser;

    public PayrollEmployeeResultService(
            PayrollEmployeeResultRepository resultRepository,
            PayrollPayslipRepository payslipRepository,
            AuthenticatedUser authenticatedUser) {

        this.resultRepository = resultRepository;
        this.payslipRepository = payslipRepository;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Returns only PAID payroll results belonging to the
     * currently authenticated employee.
     */
    public List<PayrollEmployeeResultRow> findMyResults() {

        return authenticatedUser.get()
                .map(user ->
                        resultRepository.findPaidResults(
                                user.getInsurance()
                        )
                )
                .orElseGet(List::of);
    }

    /**
     * Returns the detailed paid payroll rows for the selected
     * employee payment.
     *
     * Ownership is validated before any payroll detail is returned.
     */
    public List<PayrollPayslipRow> findMyPayslipRows(
            PayrollEmployeeResultRow selectedRow) {

        validateOwnership(selectedRow);

        List<PayrollPayslipRow> rows =
                payslipRepository.findPaidBatchRows(
                        selectedRow.getPaymentBatchId(),
                        selectedRow.getEmployeePaymentId()
                );

        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "This payroll result has no available payslip. "
                            + "| លទ្ធផលប្រាក់បៀវត្សនេះមិនមានបង្កាន់ដៃទេ។"
            );
        }

        return rows;
    }

    /**
     * Generates the employee's own payslip.
     */
    public PayslipReport generatePayslip(
            PayrollEmployeeResultRow selectedRow) {

        List<PayrollPayslipRow> rows =
                findMyPayslipRows(selectedRow);

        PayrollPayslipRow first = rows.getFirst();

        String month = "%04d-%02d".formatted(
                first.getPayrollYear(),
                first.getPayrollMonth()
        );

        String type =
                first.getInstallmentType() == null
                        ? "PAYROLL"
                        : first.getInstallmentType()
                                .toUpperCase(Locale.ROOT);

        HashMap<String, Object> parameters =
                new HashMap<>();

        parameters.put(
                "REPORT_TITLE",
                "PAYSLIP | បង្កាន់ដៃប្រាក់បៀវត្ស"
        );

        parameters.put(
                JRParameter.REPORT_DATA_SOURCE,
                new JRBeanCollectionDataSource(
                        rows,
                        false
                )
        );

        String insurance =
                first.getInsuranceNo() == null
                        ? String.valueOf(first.getEmployeeId())
                        : first.getInsuranceNo().toString();

        String fileName =
                "Payslip_"
                        + month
                        + "_"
                        + type
                        + "_"
                        + insurance
                        + ".pdf";

        return new PayslipReport(
                TEMPLATE,
                fileName,
                parameters
        );
    }

    /**
     * Security boundary.
     *
     * The selected payment identifiers are accepted only when
     * they are present in the current authenticated employee's
     * own PAID payroll results.
     */
    private void validateOwnership(
            PayrollEmployeeResultRow selectedRow) {

        if (selectedRow == null
                || selectedRow.getPaymentBatchId() == null
                || selectedRow.getEmployeePaymentId() == null) {

            throw new IllegalArgumentException(
                    "Select a payroll result first. "
                            + "| សូមជ្រើសលទ្ធផលប្រាក់បៀវត្សជាមុនសិន។"
            );
        }

        boolean owned =
                findMyResults()
                        .stream()
                        .anyMatch(item ->
                                selectedRow
                                        .getPaymentBatchId()
                                        .equals(
                                                item.getPaymentBatchId()
                                        )
                                        &&
                                selectedRow
                                        .getEmployeePaymentId()
                                        .equals(
                                                item.getEmployeePaymentId()
                                        )
                        );

        if (!owned) {
            throw new IllegalArgumentException(
                    "You can only view your own payroll result. "
                            + "| អ្នកអាចមើលបានតែលទ្ធផលប្រាក់បៀវត្សរបស់អ្នកប៉ុណ្ណោះ។"
            );
        }
    }

    public record PayslipReport(
            String reportPath,
            String fileName,
            HashMap<String, Object> parameters) {
    }
}