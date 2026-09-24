package org.halocambodia.services;

import static org.halocambodia.data.PayrollModels.EmployeePaymentRow;
import static org.halocambodia.data.PayrollModels.NssfPaymentRow;
import static org.halocambodia.data.PayrollModels.PaymentBatchRow;
import static org.halocambodia.data.PayrollModels.PaymentScheduleSummary;
import static org.halocambodia.data.PayrollModels.PayrollAuditEventRow;
import static org.halocambodia.data.PayrollModels.PaymentExecutionInput;
import static org.halocambodia.data.PayrollModels.PaymentFileExport;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.PayrollEmployeePaymentRepository;
import org.halocambodia.data.PayrollPaymentBatch;
import org.halocambodia.data.PayrollPaymentBatchRepository;
import org.halocambodia.data.PayrollPaymentDataRepository;
import org.halocambodia.data.PayrollPeriod;
import org.halocambodia.data.PayrollPeriodRepository;
import org.halocambodia.data.PayrollRun;
import org.halocambodia.data.PayrollRunEventType;
import org.halocambodia.data.PayrollRunRepository;
import org.halocambodia.enums.PayrollInstallmentType;
import org.halocambodia.enums.PayrollPaymentBatchStatus;
import org.halocambodia.enums.PayrollPeriodStatus;
import org.halocambodia.enums.PayrollRunStatus;
import org.halocambodia.enums.PayrollRunType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.PayrollActionPermissions;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollPaymentService {

    private static final Set<String> PAYMENT_METHODS = Set.of(
            "BANK_TRANSFER", "CASH", "CHEQUE", "OTHER", "NO_CASH_MOVEMENT");

    private final PayrollPaymentDataRepository paymentDataRepository;
    private final PayrollPaymentBatchRepository paymentBatchRepository;
    private final PayrollEmployeePaymentRepository employeePaymentRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunEventService payrollRunEventService;
    private final PayrollRecoveryService payrollRecoveryService;
    private final PayrollRecurringCalculationService recurringCalculationService;
    private final PayrollPeriodStatusService payrollPeriodStatusService;
    private final AuthenticatedUser authenticatedUser;

    public PayrollPaymentService(
            PayrollPaymentDataRepository paymentDataRepository,
            PayrollPaymentBatchRepository paymentBatchRepository,
            PayrollEmployeePaymentRepository employeePaymentRepository,
            PayrollPeriodRepository payrollPeriodRepository,
            PayrollRunRepository payrollRunRepository,
            PayrollRunEventService payrollRunEventService,
            PayrollRecoveryService payrollRecoveryService,
            PayrollRecurringCalculationService recurringCalculationService,
            PayrollPeriodStatusService payrollPeriodStatusService,
            AuthenticatedUser authenticatedUser) {
        this.paymentDataRepository = paymentDataRepository;
        this.paymentBatchRepository = paymentBatchRepository;
        this.employeePaymentRepository = employeePaymentRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunEventService = payrollRunEventService;
        this.payrollRecoveryService = payrollRecoveryService;
        this.recurringCalculationService = recurringCalculationService;
        this.payrollPeriodStatusService = payrollPeriodStatusService;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Detects the effective company payment rule and optional MONTHLY Shift
     * exception for a payroll period. Legacy employee-level settings are ignored.
     * The period payment date is the reference date; period end is used
     * only when payment date is null.
     */
    @Transactional(readOnly = true)
    public PaymentScheduleSummary detectPaymentSchedule(Long periodId) {
        if (periodId == null) {
            throw new IllegalArgumentException(
                    "Select a payroll period. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្ស។");
        }

        PaymentScheduleSummary summary = paymentDataRepository.detectPaymentSchedule(periodId);
        if (summary == null) {
            throw new IllegalArgumentException(
                    "Payroll period not found. | រកមិនឃើញរយៈពេលប្រាក់បៀវត្ស។");
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public List<PaymentBatchRow> findBatches(Long periodId) {
        if (periodId == null) {
            return List.of();
        }
        return paymentDataRepository.findBatches(periodId);
    }

    @Transactional(readOnly = true)
    public List<EmployeePaymentRow> findEmployeePayments(Long batchId, String search) {
        if (batchId == null) {
            return List.of();
        }
        return paymentDataRepository.findEmployeePayments(batchId, search);
    }

    @Transactional(readOnly = true)
    public List<PayrollAuditEventRow> findPaymentAuditHistory(Long batchId) {
        return payrollRunEventService.findPaymentBatchHistory(batchId);
    }

    /**
     * Exports the selected payment batch as a real Excel XLSX workbook for
     * Finance / bank upload preparation. Export is allowed for any batch status.
     * Only positive Pay Now rows are exported; recovery carry-forward rows never
     * become negative bank transactions.
     */
    @Transactional
    public PaymentFileExport exportBankPaymentFile(Long batchId) {
        requireActionPermission(PayrollActionPermissions.BANK_EXPORT);
        PayrollPaymentBatch batch = requireBatch(batchId);

        List<EmployeePaymentRow> payableRows = paymentDataRepository.findEmployeePayments(batchId, "")
                .stream()
                .filter(row -> row.paymentAmount() != null && row.paymentAmount().signum() > 0)
                .sorted(Comparator.comparing(
                        EmployeePaymentRow::insuranceNo,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (payableRows.isEmpty()) {
            throw new IllegalStateException(
                    "This batch has no positive payment amount to export. Recovery-only adjustments do not need a bank file. "
                            + "| កញ្ចប់នេះមិនមានចំនួនវិជ្ជមានសម្រាប់បើកប្រាក់ទេ។");
        }

        String fileName = "Payroll_" + batch.getInstallmentType() + "_"
                + batch.getPaymentDate() + "_Batch_" + batch.getId() + ".xlsx";

        PayrollPeriod period = payrollPeriodRepository.findById(batch.getPayrollPeriodId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll period not found. | រកមិនឃើញរយៈពេលប្រាក់បៀវត្ស។"));

        byte[] content = createBankPaymentWorkbook(batch, period, payableRows);

        Long userId = currentUserId();
        OffsetDateTime now = OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        batch.setPaymentFileName(fileName);
        batch.setPaymentExportedAt(now);
        batch.setPaymentExportedBy(userId);
        batch.setUpdatedBy(userId);
        paymentBatchRepository.saveAndFlush(batch);

        payrollRunEventService.record(
                batch.getPayrollPeriodId(), batch.getPayrollRunId(), null, batch.getId(),
                PayrollRunEventType.PAYMENT_FILE_EXPORTED,
                batch.getStatus(), batch.getStatus(), null,
                fileName + " · " + payableRows.size() + " payable employee(s)",
                userId);

        return new PaymentFileExport(fileName, content);
    }

    /**
     * Exports the NSSF remittance detail for the REGULAR payroll behind an
     * APPROVED/PAID FINAL_SETTLEMENT batch.
     *
     * The statutory NSSF contribution values are exported in KHR from the
     * original payroll_employee_item.amount values, not from their converted
     * payroll-currency amounts.
     */
    @Transactional(readOnly = true)
    public PaymentFileExport exportNssfPaymentFile(Long batchId) {
        requireActionPermission(PayrollActionPermissions.BANK_EXPORT);

        PayrollPaymentBatch batch = requireBatch(batchId);
        if (!PayrollInstallmentType.FINAL_SETTLEMENT.matches(batch.getInstallmentType())) {
            throw new IllegalStateException(
                    "NSSF payment Excel is available only for Final Settlement. "
                            + "| អាចនាំចេញ Excel ប.ស.ស. បានតែសម្រាប់ការទូទាត់ចុងក្រោយ។");
        }

        PayrollPaymentBatchStatus batchStatus = batch.statusEnum();
        if (batchStatus != PayrollPaymentBatchStatus.APPROVED
                && batchStatus != PayrollPaymentBatchStatus.PAID) {
            throw new IllegalStateException(
                    "Approve the Final Settlement batch before exporting NSSF payment. "
                            + "| សូមអនុម័តកញ្ចប់ទូទាត់ចុងក្រោយ មុននាំចេញការទូទាត់ ប.ស.ស.។");
        }

        if (batch.getPayrollRunId() == null) {
            throw new IllegalStateException(
                    "Final Settlement is not linked to a payroll run. "
                            + "| ការទូទាត់ចុងក្រោយមិនបានភ្ជាប់ទៅដំណើរការប្រាក់បៀវត្ស។");
        }

        PayrollRun run = payrollRunRepository.findById(batch.getPayrollRunId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll run not found. | រកមិនឃើញដំណើរការប្រាក់បៀវត្ស។"));
        if (run.runTypeEnum() != PayrollRunType.REGULAR) {
            throw new IllegalStateException(
                    "NSSF payment Excel must use the REGULAR monthly payroll run. "
                            + "| Excel ប.ស.ស. ត្រូវប្រើដំណើរការប្រាក់បៀវត្សប្រចាំខែ REGULAR។");
        }

        PayrollPeriod period = payrollPeriodRepository.findById(batch.getPayrollPeriodId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll period not found. | រកមិនឃើញរយៈពេលប្រាក់បៀវត្ស។"));

        List<NssfPaymentRow> rows =
                paymentDataRepository.findNssfPaymentRows(run.getId());
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "This payroll run has no NSSF contribution to export. "
                            + "| ដំណើរការប្រាក់បៀវត្សនេះមិនមានភាគទាន ប.ស.ស. សម្រាប់នាំចេញទេ។");
        }

        validateNssfExportData(rows);

        String fileName = "NSSF_Payment_%04d_%02d_Run_%d.xlsx".formatted(
                period.getPayrollYear(),
                period.getPayrollMonth(),
                run.getRunNumber());

        return new PaymentFileExport(
                fileName,
                createNssfPaymentWorkbook(period, run, batch, rows));
    }

    private static byte[] createNssfPaymentWorkbook(
            PayrollPeriod period,
            PayrollRun run,
            PayrollPaymentBatch batch,
            List<NssfPaymentRow> rows) {

        BigDecimal nssfExchangeRate = resolveNssfExchangeRate(period, rows);
        BigDecimal grossWageUsdTotal = sum(rows, NssfPaymentRow::grossWageUsd);
        BigDecimal grossWageKhrTotal = sum(rows, NssfPaymentRow::grossWageKhr);
        BigDecimal contributionWageTotal = sum(rows, NssfPaymentRow::contributionWageKhr);
        BigDecimal nssfPaymentTotal = sum(rows, NssfPaymentRow::totalNssfKhr);
        // Keep the summary USD total exactly aligned with the employee table total:
        // convert and round each employee to 2 decimals first, then sum the rows.
        BigDecimal nssfPaymentTotalUsd = rows.stream()
                .map(payment -> payment.totalNssfKhr()
                        .divide(nssfExchangeRate, 2, java.math.RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("NSSF Payment");
            DataFormat dataFormat = workbook.createDataFormat();

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            Font totalFont = workbook.createFont();
            totalFont.setBold(true);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setWrapText(true);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(headerFont);

            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(dataFormat.getFormat("@"));

            CellStyle longStyle = workbook.createCellStyle();
            longStyle.setDataFormat(dataFormat.getFormat("0"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));

            CellStyle usdAmountStyle = workbook.createCellStyle();
            usdAmountStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            CellStyle khrAmountStyle = workbook.createCellStyle();
            khrAmountStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            CellStyle exchangeRateStyle = workbook.createCellStyle();
            exchangeRateStyle.setDataFormat(dataFormat.getFormat("#,##0"));

            CellStyle totalUsdAmountStyle = workbook.createCellStyle();
            totalUsdAmountStyle.cloneStyleFrom(usdAmountStyle);
            totalUsdAmountStyle.setFont(totalFont);

            CellStyle totalAmountStyle = workbook.createCellStyle();
            totalAmountStyle.cloneStyleFrom(khrAmountStyle);
            totalAmountStyle.setFont(totalFont);

            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(totalFont);

            String[] headers = {
                    "ល.រ\nNo.",
                    "លេខធានារ៉ាប់រង\nInsurance No",
                    "លេខ ប.ស.ស.\nNSSF No",
                    "លេខអត្តសញ្ញាណប័ណ្ណ\nNational ID",
                    "ឈ្មោះបុគ្គលិក (អង់គ្លេស)\nEmployee Name EN",
                    "ឈ្មោះបុគ្គលិក (ខ្មែរ)\nEmployee Name KH",
                    "ភេទ\nSex",
                    "សញ្ជាតិ\nNationality",
                    "ថ្ងៃខែឆ្នាំកំណើត\nDate of Birth",
                    "ថ្ងៃចូលបម្រើការងារ\nDate of Join",
                    "ប្រាក់ឈ្នួលសរុប (USD)\nGross Wage (USD)",
                    "ប្រាក់ឈ្នួលសរុប (រៀល)\nGross Wage (KHR)",
                    "ប្រាក់ឈ្នួលជាប់ភាគទាន (រៀល)\nContribution Wage (KHR)",
                    "ថែទាំសុខភាព - និយោជិត (រៀល)\nHealth - Employee (KHR)",
                    "ថែទាំសុខភាព - និយោជក (រៀល)\nHealth - Employer (KHR)",
                    "ហានិភ័យការងារ - និយោជិត (រៀល)\nOccupational Risk - Employee (KHR)",
                    "ហានិភ័យការងារ - និយោជក (រៀល)\nOccupational Risk - Employer (KHR)",
                    "សោធន - និយោជិត (រៀល)\nPension - Employee (KHR)",
                    "សោធន - និយោជក (រៀល)\nPension - Employer (KHR)",
                    "សរុបនិយោជិត (រៀល)\nEmployee Total (KHR)",
                    "សរុបនិយោជក (រៀល)\nEmployer Total (KHR)",
                    "សរុបការទូទាត់ ប.ស.ស. (រៀល)\nTotal NSSF Payment (KHR)",
                    "សរុបការទូទាត់ ប.ស.ស. (ដុល្លារ)\nTotal NSSF Payment (USD)"
            };

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("NSSF Payment | ការទូទាត់ ប.ស.ស.");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                    0, 0, 0, headers.length - 1));

            writeTextCell(sheet.createRow(2), 0,
                    "Payroll Period | រយៈពេលប្រាក់បៀវត្ស", labelStyle);
            writeTextCell(sheet.getRow(2), 1,
                    "%04d-%02d".formatted(
                            period.getPayrollYear(), period.getPayrollMonth()),
                    textStyle);

            writeTextCell(sheet.createRow(3), 0,
                    "Payroll Run | ដំណើរការប្រាក់បៀវត្ស", labelStyle);
            writeTextCell(sheet.getRow(3), 1,
                    "Run " + run.getRunNumber() + " · " + run.getRunType(),
                    textStyle);

            writeTextCell(sheet.createRow(4), 0,
                    "Settlement Date | ថ្ងៃទូទាត់", labelStyle);
            Cell settlementDateCell = sheet.getRow(4).createCell(1);
            if (batch.getPaymentDate() != null) {
                settlementDateCell.setCellValue(batch.getPaymentDate());
            }
            settlementDateCell.setCellStyle(dateStyle);

            writeTextCell(sheet.createRow(5), 0,
                    "Currency | រូបិយប័ណ្ណ", labelStyle);
            writeTextCell(sheet.getRow(5), 1, "KHR | រៀល", textStyle);

            writeTextCell(sheet.createRow(6), 0,
                    "NSSF Exchange Rate (KHR/USD) | អត្រាប្តូរប្រាក់ ប.ស.ស. (រៀល/ដុល្លារ)",
                    labelStyle);
            writeAmountCell(sheet.getRow(6), 1, nssfExchangeRate, exchangeRateStyle);

            writeTextCell(sheet.createRow(7), 0,
                    "Employees | ចំនួនបុគ្គលិក", labelStyle);
            writeLongCell(sheet.getRow(7), 1, rows.size(), longStyle);

            writeTextCell(sheet.createRow(8), 0,
                    "Total NSSF Payment (KHR) | ការទូទាត់ ប.ស.ស. សរុប (រៀល)",
                    labelStyle);
            writeAmountCell(sheet.getRow(8), 1, nssfPaymentTotal, khrAmountStyle);

            writeTextCell(sheet.createRow(9), 0,
                    "Total NSSF Payment (USD) | ការទូទាត់ ប.ស.ស. សរុប (ដុល្លារ)",
                    labelStyle);
            writeAmountCell(sheet.getRow(9), 1, nssfPaymentTotalUsd, usdAmountStyle);

            int headerRowIndex = 11;
            Row headerRow = sheet.createRow(headerRowIndex);
            headerRow.setHeightInPoints(42);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal healthEmployeeTotal = BigDecimal.ZERO;
            BigDecimal healthEmployerTotal = BigDecimal.ZERO;
            BigDecimal riskEmployeeTotal = BigDecimal.ZERO;
            BigDecimal riskEmployerTotal = BigDecimal.ZERO;
            BigDecimal pensionEmployeeTotal = BigDecimal.ZERO;
            BigDecimal pensionEmployerTotal = BigDecimal.ZERO;
            BigDecimal employeeTotal = BigDecimal.ZERO;
            BigDecimal employerTotal = BigDecimal.ZERO;
            BigDecimal grandTotal = BigDecimal.ZERO;
            BigDecimal grandTotalUsd = BigDecimal.ZERO;

            int rowIndex = headerRowIndex + 1;
            int sequence = 1;
            for (NssfPaymentRow payment : rows) {
                Row row = sheet.createRow(rowIndex++);

                writeLongCell(row, 0, sequence++, longStyle);
                writeLongCell(row, 1, payment.insuranceNo(), longStyle);
                writeTextCell(row, 2, payment.nssfNo(), textStyle);
                writeTextCell(row, 3, payment.nationalId(), textStyle);
                writeTextCell(row, 4, payment.nameEn(), textStyle);
                writeTextCell(row, 5, payment.nameKh(), textStyle);
                writeTextCell(row, 6, payment.sex(), textStyle);
                writeTextCell(row, 7, payment.nationality(), textStyle);
                writeDateCell(row, 8, payment.dateOfBirth(), dateStyle);
                writeDateCell(row, 9, payment.dateOfJoin(), dateStyle);
                writeAmountCell(row, 10, payment.grossWageUsd(), usdAmountStyle);
                writeAmountCell(row, 11, payment.grossWageKhr(), khrAmountStyle);
                writeAmountCell(row, 12, payment.contributionWageKhr(), khrAmountStyle);
                writeAmountCell(row, 13, payment.healthEmployeeKhr(), khrAmountStyle);
                writeAmountCell(row, 14, payment.healthEmployerKhr(), khrAmountStyle);
                writeAmountCell(row, 15, payment.riskEmployeeKhr(), khrAmountStyle);
                writeAmountCell(row, 16, payment.riskEmployerKhr(), khrAmountStyle);
                writeAmountCell(row, 17, payment.pensionEmployeeKhr(), khrAmountStyle);
                writeAmountCell(row, 18, payment.pensionEmployerKhr(), khrAmountStyle);
                writeAmountCell(row, 19, payment.employeeTotalKhr(), khrAmountStyle);
                writeAmountCell(row, 20, payment.employerTotalKhr(), khrAmountStyle);
                writeAmountCell(row, 21, payment.totalNssfKhr(), khrAmountStyle);
                BigDecimal totalNssfUsd = payment.totalNssfKhr()
                        .divide(nssfExchangeRate, 2, java.math.RoundingMode.HALF_UP);
                writeAmountCell(row, 22, totalNssfUsd, usdAmountStyle);

                healthEmployeeTotal = healthEmployeeTotal.add(payment.healthEmployeeKhr());
                healthEmployerTotal = healthEmployerTotal.add(payment.healthEmployerKhr());
                riskEmployeeTotal = riskEmployeeTotal.add(payment.riskEmployeeKhr());
                riskEmployerTotal = riskEmployerTotal.add(payment.riskEmployerKhr());
                pensionEmployeeTotal = pensionEmployeeTotal.add(payment.pensionEmployeeKhr());
                pensionEmployerTotal = pensionEmployerTotal.add(payment.pensionEmployerKhr());
                employeeTotal = employeeTotal.add(payment.employeeTotalKhr());
                employerTotal = employerTotal.add(payment.employerTotalKhr());
                grandTotal = grandTotal.add(payment.totalNssfKhr());
                grandTotalUsd = grandTotalUsd.add(totalNssfUsd);
            }

            Row totalRow = sheet.createRow(rowIndex);
            Cell totalLabel = totalRow.createCell(9);
            totalLabel.setCellValue("TOTAL | សរុប");
            totalLabel.setCellStyle(totalLabelStyle);

            writeAmountCell(totalRow, 10, grossWageUsdTotal, totalUsdAmountStyle);
            writeAmountCell(totalRow, 11, grossWageKhrTotal, totalAmountStyle);
            writeAmountCell(totalRow, 12, contributionWageTotal, totalAmountStyle);
            writeAmountCell(totalRow, 13, healthEmployeeTotal, totalAmountStyle);
            writeAmountCell(totalRow, 14, healthEmployerTotal, totalAmountStyle);
            writeAmountCell(totalRow, 15, riskEmployeeTotal, totalAmountStyle);
            writeAmountCell(totalRow, 16, riskEmployerTotal, totalAmountStyle);
            writeAmountCell(totalRow, 17, pensionEmployeeTotal, totalAmountStyle);
            writeAmountCell(totalRow, 18, pensionEmployerTotal, totalAmountStyle);
            writeAmountCell(totalRow, 19, employeeTotal, totalAmountStyle);
            writeAmountCell(totalRow, 20, employerTotal, totalAmountStyle);
            writeAmountCell(totalRow, 21, grandTotal, totalAmountStyle);
            writeAmountCell(totalRow, 22, grandTotalUsd, totalUsdAmountStyle);

            sheet.createFreezePane(0, headerRowIndex + 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    headerRowIndex,
                    Math.max(headerRowIndex, rowIndex - 1),
                    0,
                    headers.length - 1));

            int[] widths = {
                    8, 18, 20, 22, 30, 30, 18, 28, 18, 18,
                    22, 22, 26,
                    28, 28, 34, 34, 28, 28, 26, 26, 30, 30
            };
            for (int column = 0; column < widths.length; column++) {
                sheet.setColumnWidth(column, widths[column] * 256);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to create NSSF payment Excel file. "
                            + "| មិនអាចបង្កើតឯកសារ Excel សម្រាប់ការទូទាត់ ប.ស.ស. បានទេ។",
                    ex);
        }
    }

    private static void validateNssfExportData(List<NssfPaymentRow> rows) {
        List<String> issues = new java.util.ArrayList<>();

        for (NssfPaymentRow payment : rows) {
            List<String> missing = new java.util.ArrayList<>();
            if (payment.insuranceNo() == null) {
                missing.add("Insurance No | លេខធានារ៉ាប់រង");
            }
            // NSSF No is optional for export. If it is missing, keep the Excel cell blank
            // and continue exporting the employee.
            if (isBlank(payment.nationalId())) {
                missing.add("National ID | លេខអត្តសញ្ញាណប័ណ្ណ");
            }
            if (isBlank(payment.sex())) {
                missing.add("Sex | ភេទ");
            }
            if (isBlank(payment.nationality())) {
                missing.add("Nationality | សញ្ជាតិ");
            }
            if (payment.dateOfBirth() == null) {
                missing.add("Date of Birth | ថ្ងៃខែឆ្នាំកំណើត");
            }
            if (payment.dateOfJoin() == null) {
                missing.add("Date of Join | ថ្ងៃចូលបម្រើការងារ");
            }

            if (!missing.isEmpty()) {
                String employee = payment.insuranceNo() == null
                        ? displayEmployeeName(payment)
                        : payment.insuranceNo() + " - " + displayEmployeeName(payment);
                issues.add(employee + " [" + String.join(", ", missing) + "]");
            }
        }

        if (!issues.isEmpty()) {
            int shown = Math.min(issues.size(), 10);
            String details = String.join("; ", issues.subList(0, shown));
            String more = issues.size() > shown
                    ? "; +" + (issues.size() - shown) + " more employee(s)"
                    : "";
            throw new IllegalStateException(
                    "NSSF export cannot continue because required employee information is missing: "
                            + details + more
                            + ". | មិនអាចនាំចេញ Excel ប.ស.ស. បានទេ ព្រោះព័ត៌មានចាំបាច់របស់បុគ្គលិកមិនគ្រប់គ្រាន់។ "
                            + "សូមបំពេញព័ត៌មានដែលខ្វះ ហើយនាំចេញម្តងទៀត។");
        }
    }

    private static BigDecimal resolveNssfExchangeRate(
            PayrollPeriod period,
            List<NssfPaymentRow> rows) {

        BigDecimal snapshotRate = null;
        boolean missingSnapshot = false;

        for (NssfPaymentRow payment : rows) {
            BigDecimal rate = payment.nssfExchangeRate();
            if (rate == null || rate.signum() <= 0) {
                missingSnapshot = true;
                continue;
            }

            if (snapshotRate == null) {
                snapshotRate = rate;
            } else if (snapshotRate.compareTo(rate) != 0) {
                throw new IllegalStateException(
                        "NSSF export cannot continue because this payroll run contains different "
                                + "NSSF exchange-rate snapshots. Recalculate or review the payroll run first. "
                                + "| មិនអាចនាំចេញ Excel ប.ស.ស. បានទេ ព្រោះដំណើរការប្រាក់បៀវត្សនេះមាន "
                                + "អត្រាប្តូរប្រាក់ ប.ស.ស. ដែលបានរក្សាទុកខុសគ្នា។ "
                                + "សូមគណនា ឬពិនិត្យដំណើរការប្រាក់បៀវត្សឡើងវិញជាមុន។");
            }
        }

        BigDecimal periodRate = period.getNssfUsdToKhrRate();
        if (snapshotRate != null) {
            if (missingSnapshot
                    && (periodRate == null
                    || periodRate.signum() <= 0
                    || snapshotRate.compareTo(periodRate) != 0)) {
                throw new IllegalStateException(
                        "NSSF export cannot continue because some employees do not have an NSSF "
                                + "exchange-rate snapshot and the current period rate does not match the saved snapshot. "
                                + "| មិនអាចនាំចេញ Excel ប.ស.ស. បានទេ ព្រោះបុគ្គលិកខ្លះមិនមាន "
                                + "អត្រាប្តូរប្រាក់ ប.ស.ស. ដែលបានរក្សាទុក ហើយអត្រាបច្ចុប្បន្នរបស់រយៈពេល "
                                + "មិនត្រូវនឹងអត្រាដែលបានរក្សាទុក។");
            }
            return snapshotRate;
        }

        if (periodRate == null || periodRate.signum() <= 0) {
            throw new IllegalStateException(
                    "NSSF exchange rate is missing or invalid for this payroll run. "
                            + "| អត្រាប្តូរប្រាក់ ប.ស.ស. សម្រាប់ដំណើរការប្រាក់បៀវត្សនេះមិនមាន ឬមិនត្រឹមត្រូវ។");
        }

        // Legacy fallback for payroll rows created before NSSF rate snapshots were stored.
        return periodRate;
    }

    private static BigDecimal sum(
            List<NssfPaymentRow> rows,
            java.util.function.Function<NssfPaymentRow, BigDecimal> extractor) {

        BigDecimal total = BigDecimal.ZERO;
        for (NssfPaymentRow row : rows) {
            BigDecimal value = extractor.apply(row);
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private static String displayEmployeeName(NssfPaymentRow payment) {
        if (!isBlank(payment.nameEn())) {
            return payment.nameEn().trim();
        }
        if (!isBlank(payment.nameKh())) {
            return payment.nameKh().trim();
        }
        return "Employee ID " + payment.employeeId();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void writeDateCell(
            Row row,
            int column,
            LocalDate value,
            CellStyle dateStyle) {

        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(dateStyle);
    }

    private static void writeAmountCell(
            Row row,
            int column,
            BigDecimal value,
            CellStyle amountStyle) {

        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(amountStyle);
    }

    private static byte[] createBankPaymentWorkbook(
            PayrollPaymentBatch batch,
            PayrollPeriod period,
            List<EmployeePaymentRow> payableRows) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Bank Payment");

            DataFormat dataFormat = workbook.createDataFormat();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(dataFormat.getFormat("@"));

            CellStyle longStyle = workbook.createCellStyle();
            longStyle.setDataFormat(dataFormat.getFormat("0"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"));

            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

            String[] headers = {
                    "No",
                    "Insurance No",
                    "Employee Name EN",
                    "Account No.",
                    "Currency",
                    "Salary",
                    "Sign",
                    "Transaction Reference",
                    "Value Date"
            };

            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            int sequenceNo = 1;
            for (EmployeePaymentRow payment : payableRows) {
                Row row = sheet.createRow(rowIndex++);

                writeLongCell(row, 0, sequenceNo++, longStyle);
                writeLongCell(row, 1, payment.insuranceNo(), longStyle);
                writeTextCell(row, 2, payment.nameEn(), textStyle);
                writeTextCell(row, 3, payment.bankAccount(), textStyle);
                writeTextCell(row, 4, payment.currency(), textStyle);

                Cell salaryCell = row.createCell(5);
                if (payment.paymentAmount() != null) {
                    salaryCell.setCellValue(payment.paymentAmount().doubleValue());
                }
                salaryCell.setCellStyle(amountStyle);

                writeTextCell(row, 6, "", textStyle);
                writeTextCell(row, 7, bankTransactionReference(batch, period, payment), textStyle);

                Cell valueDateCell = row.createCell(8);
                if (batch.getPaymentDate() != null) {
                    valueDateCell.setCellValue(batch.getPaymentDate());
                }
                valueDateCell.setCellStyle(dateStyle);
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(0, rowIndex - 1), 0, headers.length - 1));

            int[] widths = {
                    8, 18, 28, 24, 12, 16, 10, 42, 14
            };
            for (int column = 0; column < widths.length; column++) {
                sheet.setColumnWidth(column, widths[column] * 256);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Unable to create Excel bank payment file. "
                            + "| មិនអាចបង្កើតឯកសារ Excel សម្រាប់ធនាគារបានទេ។",
                    ex);
        }
    }

    private static String bankTransactionReference(
            PayrollPaymentBatch batch,
            PayrollPeriod period,
            EmployeePaymentRow payment) {

        String payrollMonth = java.time.LocalDate.of(
                        period.getPayrollYear(), period.getPayrollMonth(), 1)
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

        PayrollInstallmentType installmentType = batch.installmentTypeEnum();
        if (installmentType == PayrollInstallmentType.FIRST_INSTALLMENT) {
            String percent = payment.firstPaymentPercent() == null
                    ? ""
                    : payment.firstPaymentPercent().stripTrailingZeros().toPlainString() + "% ";
            return "First Salary " + percent + "for " + payrollMonth;
        }
        if (installmentType == PayrollInstallmentType.FINAL_SETTLEMENT) {
            return "Final Salary for " + payrollMonth;
        }
        if (installmentType == PayrollInstallmentType.ADJUSTMENT_SETTLEMENT) {
            return "Salary Adjustment for " + payrollMonth;
        }
        return "Salary for " + payrollMonth;
    }

    private static void writeTextCell(
            Row row,
            int column,
            Object value,
            CellStyle textStyle) {

        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value.toString());
        cell.setCellStyle(textStyle);
    }

    private static void writeLongCell(
            Row row,
            int column,
            Number value,
            CellStyle longStyle) {

        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.longValue());
        }
        cell.setCellStyle(longStyle);
    }

    /**
     * Records the real-world payment execution evidence before financially
     * finalizing an APPROVED batch. BANK_TRANSFER requires an exported file and
     * bank reference. Recovery-only batches use NO_CASH_MOVEMENT.
     */
    @Transactional
    public void confirmBatchPaid(Long batchId, PaymentExecutionInput input) {
        requireActionPermission(PayrollActionPermissions.PAYMENT_RECONCILE);
        PayrollPaymentBatch batch = requireBatch(batchId);
        if (batch.statusEnum() != PayrollPaymentBatchStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only an APPROVED payment batch can be confirmed as paid. "
                            + "| អាចបញ្ជាក់ការបើកប្រាក់បានតែកញ្ចប់ APPROVED ប៉ុណ្ណោះ។");
        }

        List<EmployeePaymentRow> rows = paymentDataRepository.findEmployeePayments(batchId, "");
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "The payment batch has no employee payment records. "
                            + "| កញ្ចប់បើកប្រាក់មិនមានទិន្នន័យបុគ្គលិកទេ។");
        }

        BigDecimal payableTotal = rows.stream()
                .map(EmployeePaymentRow::paymentAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String method = normalizePaymentMethod(input == null ? null : input.paymentMethod());
        String reference = requiredText(input == null ? null : input.paymentReference(),
                "Payment reference is required. | ត្រូវបញ្ចូលលេខយោងការបើកប្រាក់។");
        String bankReference = blankToNull(input == null ? null : input.bankReference());

        if (payableTotal.signum() == 0) {
            method = "NO_CASH_MOVEMENT";
        } else if ("NO_CASH_MOVEMENT".equals(method)) {
            throw new IllegalArgumentException(
                    "NO_CASH_MOVEMENT is allowed only when the batch Pay Now total is zero. "
                            + "| អាចប្រើ NO_CASH_MOVEMENT បានតែពេលចំនួនត្រូវបើកសរុបស្មើសូន្យ។");
        }

        if ("BANK_TRANSFER".equals(method)) {
            if (batch.getApprovedAt() == null
                    || batch.getPaymentExportedAt() == null
                    || batch.getPaymentFileName() == null
                    || batch.getPaymentExportedAt().isBefore(batch.getApprovedAt())) {
                throw new IllegalStateException(
                        "Export the bank payment file after the batch is approved before confirming a bank transfer. "
                                + "A draft or stale export cannot be used for payment confirmation. "
                                + "| សូមទាញយកឯកសារបើកប្រាក់ធនាគារបន្ទាប់ពីកញ្ចប់ត្រូវបានអនុម័ត "
                                + "មុនបញ្ជាក់ការផ្ទេរ។ ឯកសារ Draft ឬឯកសារចាស់មិនអាចប្រើបានទេ។");
            }
            if (bankReference == null) {
                throw new IllegalArgumentException(
                        "Bank reference is required for BANK_TRANSFER. "
                                + "| ត្រូវបញ្ចូលលេខយោងធនាគារសម្រាប់ការផ្ទេរប្រាក់។");
            }
        }

        Long userId = currentUserId();
        OffsetDateTime now = OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        batch.setPaymentMethod(method);
        batch.setPaymentReference(reference);
        batch.setBankReference(bankReference);
        batch.setPaymentConfirmedAt(now);
        batch.setPaymentConfirmedBy(userId);
        batch.setUpdatedBy(userId);
        paymentBatchRepository.saveAndFlush(batch);

        payrollRunEventService.record(
                batch.getPayrollPeriodId(), batch.getPayrollRunId(), null, batch.getId(),
                PayrollRunEventType.PAYMENT_RECONCILED,
                PayrollPaymentBatchStatus.APPROVED.code(), PayrollPaymentBatchStatus.APPROVED.code(), null,
                "Method=" + method + "; PaymentRef=" + reference
                        + (bankReference == null ? "" : "; BankRef=" + bankReference),
                userId);

        markPaid(batch, userId, now);
    }

    /**
     * Finalizes an approved adjustment settlement without collecting payment
     * execution details. Adjustment settlements never create an immediate cash
     * payment: positive and negative balances are carried into the next REGULAR
     * payroll.
     */
    @Transactional
    public void finalizeAdjustmentSettlement(Long batchId) {
        requireActionPermission(PayrollActionPermissions.PAYMENT_RECONCILE);
        PayrollPaymentBatch batch = requireBatch(batchId);
        if (batch.installmentTypeEnum() != PayrollInstallmentType.ADJUSTMENT_SETTLEMENT) {
            throw new IllegalArgumentException(
                    "Only an adjustment settlement can be finalized without payment details. "
                            + "| អាចបញ្ចប់ដោយគ្មានព័ត៌មានការបើកប្រាក់បានតែការទូទាត់កែតម្រូវប៉ុណ្ណោះ។");
        }
        if (batch.statusEnum() != PayrollPaymentBatchStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only an APPROVED adjustment settlement can be finalized. "
                            + "| អាចបញ្ចប់ការកែតម្រូវបានតែកញ្ចប់ APPROVED ប៉ុណ្ណោះ។");
        }

        List<EmployeePaymentRow> rows = paymentDataRepository.findEmployeePayments(batchId, "");
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "The adjustment settlement has no employee records. "
                            + "| កញ្ចប់កែតម្រូវមិនមានទិន្នន័យបុគ្គលិកទេ។");
        }

        Long userId = currentUserId();
        OffsetDateTime now = OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        batch.setPaymentMethod("NO_CASH_MOVEMENT");
        batch.setPaymentReference("ADJUSTMENT-CARRY-FORWARD-" + batch.getId());
        batch.setBankReference(null);
        batch.setPaymentConfirmedAt(now);
        batch.setPaymentConfirmedBy(userId);
        batch.setUpdatedBy(userId);
        paymentBatchRepository.saveAndFlush(batch);

        payrollRunEventService.record(
                batch.getPayrollPeriodId(), batch.getPayrollRunId(), null, batch.getId(),
                PayrollRunEventType.PAYMENT_RECONCILED,
                PayrollPaymentBatchStatus.APPROVED.code(), PayrollPaymentBatchStatus.APPROVED.code(), null,
                "Adjustment finalized with no cash movement; all balances carried forward to next regular payroll",
                userId);

        markPaid(batch, userId, now);
    }

    @Transactional
    public int generateFirstInstallment(Long periodId, LocalDate paymentDate, String notes) {
        requireActionPermission(PayrollActionPermissions.PAYMENT_GENERATE);
        PayrollPeriod period = requireOpenPeriod(periodId);
        LocalDate referenceDate = referenceDate(period);

        requireEffectiveCompanyPaymentSetting(referenceDate);
        validatePaymentDate(period, paymentDate);
        requireSupportedCurrency(period);

        PayrollPaymentBatch batch = prepareDraftBatch(
                periodId, null, PayrollInstallmentType.FIRST_INSTALLMENT, paymentDate, notes);
        Long userId = currentUserId();

        paymentDataRepository.deleteEmployeePayments(batch.getId());
        int inserted = paymentDataRepository.insertFirstInstallment(
                periodId, referenceDate, batch.getId(), userId);

        if (inserted == 0) {
            throw new IllegalStateException(
                    "No rostered employee is scheduled for SEMI_MONTHLY payment on payroll reference date "
                            + referenceDate + ". For MONTHLY choose a Shift exception, or choose SEMI_MONTHLY for all staff. "
                            + "| មិនមានបុគ្គលិកក្នុងតារាងវេនដែលមានការកំណត់បើកប្រាក់ពីរដងសម្រាប់ថ្ងៃនេះទេ។");
        }
        payrollRunEventService.record(
                periodId, null, null, batch.getId(),
                PayrollRunEventType.PAYMENT_GENERATED,
                null, PayrollPaymentBatchStatus.DRAFT.code(), notes,
                PayrollInstallmentType.FIRST_INSTALLMENT.code() + " · " + inserted + " employee payment(s)",
                userId);
        return inserted;
    }

    @Transactional
    public int generateFinalSettlement(Long periodId, LocalDate paymentDate, String notes) {
        requireActionPermission(PayrollActionPermissions.PAYMENT_GENERATE);
        PayrollPeriod period = requireOpenPeriod(periodId);
        LocalDate referenceDate = referenceDate(period);

        requireEffectiveCompanyPaymentSetting(referenceDate);
        validatePaymentDate(period, paymentDate);
        requireSupportedCurrency(period);

        long incompleteFirst = paymentBatchRepository
                .countByPayrollPeriodIdAndInstallmentTypeAndStatusIn(
                        periodId,
                        PayrollInstallmentType.FIRST_INSTALLMENT.code(),
                        Set.of(PayrollPaymentBatchStatus.DRAFT.code(),
                                PayrollPaymentBatchStatus.APPROVED.code()));
        if (incompleteFirst > 0) {
            throw new IllegalStateException(
                    "Complete or cancel the first installment before generating final settlement. "
                            + "| សូមបញ្ចប់ ឬបោះបង់ការបើកប្រាក់លើកទីមួយ មុនបង្កើតការទូទាត់ចុងក្រោយ។");
        }

        PayrollRun run = payrollRunRepository
                .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                        periodId, PayrollRunType.REGULAR.code(), PayrollRunStatus.APPROVED.code())
                .orElseThrow(() -> new IllegalStateException(
                        "Approve the regular monthly payroll before generating final settlement. "
                                + "| សូមអនុម័តប្រាក់បៀវត្សប្រចាំខែ មុនបង្កើតការទូទាត់ចុងក្រោយ។"));

        PayrollPaymentBatch batch = prepareDraftBatch(
                periodId, run.getId(), PayrollInstallmentType.FINAL_SETTLEMENT, paymentDate, notes);
        Long userId = currentUserId();

        paymentDataRepository.deleteEmployeePayments(batch.getId());
        int inserted = paymentDataRepository.insertFinalSettlement(
                periodId, referenceDate, run.getId(), batch.getId(), userId);

        if (inserted == 0) {
            throw new IllegalStateException(
                    "The approved regular payroll has no INCLUDED employees. "
                            + "| ប្រាក់បៀវត្សប្រចាំខែដែលបានអនុម័តមិនមានបុគ្គលិក INCLUDED ទេ។");
        }
        payrollRunEventService.record(
                periodId, run.getId(), null, batch.getId(),
                PayrollRunEventType.PAYMENT_GENERATED,
                null, PayrollPaymentBatchStatus.DRAFT.code(), notes,
                PayrollInstallmentType.FINAL_SETTLEMENT.code() + " · " + inserted + " employee payment(s)",
                userId);
        return inserted;
    }

    @Transactional
    public int generateAdjustmentSettlement(Long periodId, LocalDate paymentDate, String notes) {
        requireActionPermission(PayrollActionPermissions.PAYMENT_GENERATE);
        PayrollPeriod period = requireOpenPeriod(periodId);

        validateAdjustmentPaymentDate(period, paymentDate);
        requireSupportedCurrency(period);

        payrollRunRepository
                .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                        periodId, PayrollRunType.REGULAR.code(), PayrollRunStatus.PAID.code())
                .orElseThrow(() -> new IllegalStateException(
                        "The regular payroll must already be PAID before creating a post-paid adjustment settlement. "
                                + "| ប្រាក់បៀវត្សប្រចាំខែត្រូវបានបើករួចជាមុនសិន មុនបង្កើតការទូទាត់កែតម្រូវ។"));

        PayrollRun adjustmentRun = payrollRunRepository
                .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                        periodId, PayrollRunType.ADJUSTMENT.code(), PayrollRunStatus.APPROVED.code())
                .orElseThrow(() -> new IllegalStateException(
                        "Approve the adjustment payroll before generating adjustment settlement. "
                                + "| សូមអនុម័តប្រាក់បៀវត្សកែតម្រូវ មុនបង្កើតការទូទាត់កែតម្រូវ។"));

        PayrollPaymentBatch batch = prepareDraftBatch(
                periodId, adjustmentRun.getId(), PayrollInstallmentType.ADJUSTMENT_SETTLEMENT,
                paymentDate, notes);
        Long userId = currentUserId();

        paymentDataRepository.deleteEmployeePayments(batch.getId());
        int inserted = paymentDataRepository.insertAdjustmentSettlement(
                periodId, adjustmentRun.getId(), batch.getId(), userId);

        if (inserted == 0) {
            throw new IllegalStateException(
                    "The approved adjustment has no non-zero employee balance to settle. "
                            + "| ការកែតម្រូវដែលបានអនុម័តមិនមានសមតុល្យបុគ្គលិកសម្រាប់ទូទាត់ទេ។");
        }

        payrollRunEventService.record(
                periodId, adjustmentRun.getId(), null, batch.getId(),
                PayrollRunEventType.PAYMENT_GENERATED,
                null, PayrollPaymentBatchStatus.DRAFT.code(), notes,
                PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.code()
                        + " · " + inserted + " employee settlement(s)",
                userId);
        return inserted;
    }

    @Transactional
    public void moveBatchTo(Long batchId, String targetStatus) {
        requireActionPermission(PayrollActionPermissions.PAYMENT_APPROVE);
        PayrollPaymentBatchStatus target = PayrollPaymentBatchStatus.from(targetStatus);

        PayrollPaymentBatch batch = requireBatch(batchId);
        if (target == PayrollPaymentBatchStatus.PAID) {
            throw new IllegalArgumentException(
                    "Use payment execution confirmation before marking a batch PAID. "
                            + "| សូមបញ្ជាក់ព័ត៌មានការបើកប្រាក់ មុនសម្គាល់កញ្ចប់ជា PAID។");
        }

        Map<PayrollPaymentBatchStatus, Set<PayrollPaymentBatchStatus>> allowed = Map.of(
                PayrollPaymentBatchStatus.DRAFT,
                Set.of(PayrollPaymentBatchStatus.APPROVED, PayrollPaymentBatchStatus.CANCELLED),
                PayrollPaymentBatchStatus.APPROVED,
                Set.of(PayrollPaymentBatchStatus.CANCELLED));

        if (!allowed.getOrDefault(batch.statusEnum(), Set.of()).contains(target)) {
            throw new IllegalStateException(
                    "Cannot change payment batch from "
                            + batch.getStatus() + " to " + target.code() + ".");
        }

        long detailCount = employeePaymentRepository.countByPayrollPaymentBatchId(batchId);
        if (target != PayrollPaymentBatchStatus.CANCELLED && detailCount == 0) {
            throw new IllegalStateException(
                    "Generate employee payments before changing the batch status. "
                            + "| សូមបង្កើតការបើកប្រាក់បុគ្គលិកមុនប្តូរស្ថានភាព។");
        }

        Long userId = currentUserId();
        OffsetDateTime now = OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE);

        switch (target) {
            case APPROVED -> {
                // A file exported while the batch was still DRAFT is only a preview.
                // Invalidate it at approval so Finance must export the immutable
                // approved version before confirming a BANK_TRANSFER.
                invalidatePaymentExport(batch);
                batch.setStatus(PayrollPaymentBatchStatus.APPROVED);
                batch.setApprovedAt(now);
                batch.setApprovedBy(userId);
                batch.setUpdatedBy(userId);
                paymentBatchRepository.saveAndFlush(batch);
                payrollRunEventService.record(
                        batch.getPayrollPeriodId(), batch.getPayrollRunId(), null, batch.getId(),
                        PayrollRunEventType.PAYMENT_APPROVED,
                        PayrollPaymentBatchStatus.DRAFT.code(), PayrollPaymentBatchStatus.APPROVED.code(), null,
                        batch.getInstallmentType(), userId);
            }
            case PAID -> throw new IllegalArgumentException(
                    "Use payment execution confirmation before marking a batch PAID.");
            case CANCELLED -> {
                PayrollPaymentBatchStatus fromStatus = batch.statusEnum();
                batch.setStatus(PayrollPaymentBatchStatus.CANCELLED);
                batch.setUpdatedBy(userId);
                paymentBatchRepository.saveAndFlush(batch);
                payrollRunEventService.record(
                        batch.getPayrollPeriodId(), batch.getPayrollRunId(), null, batch.getId(),
                        PayrollRunEventType.PAYMENT_CANCELLED,
                        fromStatus.code(), PayrollPaymentBatchStatus.CANCELLED.code(), null,
                        batch.getInstallmentType(), userId);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported payment batch status.");
        }
    }

    @Transactional(readOnly = true)
    public boolean hasPaidFinalSettlement(Long payrollRunId) {
        if (payrollRunId == null) {
            return false;
        }
        return paymentBatchRepository.existsByPayrollRunIdAndInstallmentTypeAndStatus(
                payrollRunId,
                PayrollInstallmentType.FINAL_SETTLEMENT.code(),
                PayrollPaymentBatchStatus.PAID.code());
    }

    @Transactional(readOnly = true)
    public boolean hasPaidRegularPayroll(Long periodId) {
        if (periodId == null) {
            return false;
        }
        return payrollRunRepository
                .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                        periodId, PayrollRunType.REGULAR.code(), PayrollRunStatus.PAID.code())
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean canGenerateAdjustmentSettlement(Long periodId) {
        if (periodId == null) {
            return false;
        }
        boolean regularPaid = payrollRunRepository
                .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                        periodId, PayrollRunType.REGULAR.code(), PayrollRunStatus.PAID.code())
                .isPresent();
        if (!regularPaid) {
            return false;
        }
        boolean adjustmentApproved = payrollRunRepository
                .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                        periodId, PayrollRunType.ADJUSTMENT.code(), PayrollRunStatus.APPROVED.code())
                .isPresent();
        if (!adjustmentApproved) {
            return false;
        }

        PayrollRun adjustmentRun = payrollRunRepository
                .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                        periodId, PayrollRunType.ADJUSTMENT.code(), PayrollRunStatus.APPROVED.code())
                .orElse(null);
        if (adjustmentRun == null) {
            return false;
        }

        return paymentBatchRepository
                .findTopByPayrollRunIdAndInstallmentTypeAndStatusNotOrderByIdDesc(
                        adjustmentRun.getId(),
                        PayrollInstallmentType.ADJUSTMENT_SETTLEMENT.code(),
                        PayrollPaymentBatchStatus.CANCELLED.code())
                .map(batch -> batch.statusEnum() == PayrollPaymentBatchStatus.DRAFT)
                .orElse(true);
    }

    private PayrollPaymentBatch prepareDraftBatch(
            Long periodId,
            Long runId,
            PayrollInstallmentType installmentType,
            LocalDate paymentDate,
            String notes) {
        PayrollPaymentBatch existing;
        if (installmentType == PayrollInstallmentType.ADJUSTMENT_SETTLEMENT) {
            existing = paymentBatchRepository
                    .findTopByPayrollRunIdAndInstallmentTypeAndStatusNotOrderByIdDesc(
                            runId,
                            installmentType.code(),
                            PayrollPaymentBatchStatus.CANCELLED.code())
                    .orElse(null);
        } else {
            existing = paymentBatchRepository
                    .findTopByPayrollPeriodIdAndInstallmentTypeAndStatusNotOrderByIdDesc(
                            periodId,
                            installmentType.code(),
                            PayrollPaymentBatchStatus.CANCELLED.code())
                    .orElse(null);
        }

        Long userId = currentUserId();
        if (existing == null) {
            PayrollPaymentBatch batch = new PayrollPaymentBatch();
            batch.setPayrollPeriodId(periodId);
            batch.setPayrollRunId(runId);
            batch.setInstallmentType(installmentType);
            batch.setPaymentDate(paymentDate);
            batch.setStatus(PayrollPaymentBatchStatus.DRAFT);
            batch.setNotes(blankToNull(notes));
            batch.setCreatedBy(userId);
            batch.setUpdatedBy(userId);
            return paymentBatchRepository.saveAndFlush(batch);
        }

        if (existing.statusEnum() != PayrollPaymentBatchStatus.DRAFT) {
            throw new IllegalStateException(
                    "The existing "
                            + installmentType.code().replace('_', ' ').toLowerCase(Locale.ROOT)
                            + " batch is already " + existing.getStatus()
                            + ". Cancel it before creating another one. "
                            + "| កញ្ចប់បើកប្រាក់នេះបានប្តូរស្ថានភាពរួចហើយ។");
        }

        // Regenerating a DRAFT replaces its employee-payment detail. Any file
        // exported from the previous detail set is stale and must not remain as
        // payment evidence.
        invalidatePaymentExport(existing);
        existing.setPayrollRunId(runId);
        existing.setPaymentDate(paymentDate);
        existing.setNotes(blankToNull(notes));
        existing.setUpdatedBy(userId);
        return paymentBatchRepository.saveAndFlush(existing);
    }

    private static void invalidatePaymentExport(PayrollPaymentBatch batch) {
        batch.setPaymentFileName(null);
        batch.setPaymentExportedAt(null);
        batch.setPaymentExportedBy(null);
    }

    private void markPaid(
            PayrollPaymentBatch batch,
            Long userId,
            OffsetDateTime now) {
        PayrollInstallmentType installmentType = batch.installmentTypeEnum();

        if (installmentType == PayrollInstallmentType.FIRST_INSTALLMENT) {
            boolean finalExists = paymentBatchRepository
                    .existsByPayrollPeriodIdAndInstallmentTypeAndStatusNot(
                            batch.getPayrollPeriodId(),
                            PayrollInstallmentType.FINAL_SETTLEMENT.code(),
                            PayrollPaymentBatchStatus.CANCELLED.code());
            if (finalExists) {
                throw new IllegalStateException(
                        "The final settlement already exists. Cancel and regenerate it before marking the first installment paid. "
                                + "| ការទូទាត់ចុងក្រោយមានរួចហើយ។ សូមបោះបង់ និងបង្កើតឡើងវិញជាមុន។");
            }
        }

        PayrollRun linkedRun = null;
        if (installmentType == PayrollInstallmentType.FINAL_SETTLEMENT) {
            linkedRun = requireLinkedRun(batch, PayrollRunType.REGULAR);
            if (linkedRun.statusEnum() != PayrollRunStatus.APPROVED) {
                throw new IllegalStateException(
                        "The linked regular payroll must still be APPROVED. "
                                + "| ប្រាក់បៀវត្សប្រចាំខែដែលភ្ជាប់ត្រូវនៅស្ថានភាព APPROVED។");
            }
        } else if (installmentType == PayrollInstallmentType.ADJUSTMENT_SETTLEMENT) {
            linkedRun = requireLinkedRun(batch, PayrollRunType.ADJUSTMENT);
            if (linkedRun.statusEnum() != PayrollRunStatus.APPROVED) {
                throw new IllegalStateException(
                        "The linked adjustment payroll must still be APPROVED. "
                                + "| ប្រាក់បៀវត្សកែតម្រូវដែលភ្ជាប់ត្រូវនៅស្ថានភាព APPROVED។");
            }
            payrollRunRepository
                    .findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
                            batch.getPayrollPeriodId(),
                            PayrollRunType.REGULAR.code(),
                            PayrollRunStatus.PAID.code())
                    .orElseThrow(() -> new IllegalStateException(
                            "The regular payroll must remain PAID before the adjustment settlement can be finalized. "
                                    + "| ប្រាក់បៀវត្សប្រចាំខែត្រូវនៅស្ថានភាព PAID មុនបញ្ចប់ការទូទាត់កែតម្រូវ។"));
        }

        batch.setStatus(PayrollPaymentBatchStatus.PAID);
        batch.setPaidAt(now);
        batch.setPaidBy(userId);
        batch.setUpdatedBy(userId);
        paymentBatchRepository.saveAndFlush(batch);
        payrollRunEventService.record(
                batch.getPayrollPeriodId(), batch.getPayrollRunId(), null, batch.getId(),
                PayrollRunEventType.PAYMENT_PAID,
                PayrollPaymentBatchStatus.APPROVED.code(), PayrollPaymentBatchStatus.PAID.code(), null,
                batch.getInstallmentType(), userId);

        if (linkedRun == null) {
            return;
        }

        linkedRun.setStatus(PayrollRunStatus.PAID);
        linkedRun.setPaidAt(now);
        linkedRun.setPaidBy(userId);
        payrollRunRepository.saveAndFlush(linkedRun);

        if (installmentType == PayrollInstallmentType.FINAL_SETTLEMENT) {
            // Commit company-loan repayments and one-time recurring items only
            // after the real Final Settlement is confirmed PAID. Recalculation
            // before this point only creates replaceable reservations.
            recurringCalculationService.settlePaidRegularRun(linkedRun.getId(), userId);
            payrollRecoveryService.settlePaidRegularRun(linkedRun.getId(), userId);
            payrollRecoveryService.createFromPaidBatch(
                    batch.getId(), batch.getPayrollPeriodId(), linkedRun.getId(), userId);
        } else if (installmentType == PayrollInstallmentType.ADJUSTMENT_SETTLEMENT) {
            paymentDataRepository.createPositiveAdjustmentCarryForwards(batch.getId(), userId);
            payrollRecoveryService.createFromPaidBatch(
                    batch.getId(), batch.getPayrollPeriodId(), linkedRun.getId(), userId);
        }

        String runDetail = installmentType == PayrollInstallmentType.FINAL_SETTLEMENT
                ? "Final settlement paid"
                : "Adjustment settlement finalized; positive balances carried forward as earnings and negative balances carried forward for recovery";
        payrollRunEventService.record(
                batch.getPayrollPeriodId(), linkedRun.getId(), null, batch.getId(),
                PayrollRunEventType.RUN_PAID,
                PayrollRunStatus.APPROVED.code(), PayrollRunStatus.PAID.code(), null,
                runDetail, userId);

        PayrollPeriodStatusService.StatusChange periodStatus =
                payrollPeriodStatusService.refresh(batch.getPayrollPeriodId(), userId);
        if (periodStatus.changed() && periodStatus.to() == PayrollPeriodStatus.CLOSED) {
            String closeDetail = installmentType == PayrollInstallmentType.FINAL_SETTLEMENT
                    ? "Closed after paid final settlement; no unfinished adjustment remains"
                    : "Closed after finalized adjustment settlement; no unfinished adjustment remains";
            payrollRunEventService.record(
                    batch.getPayrollPeriodId(), linkedRun.getId(), null, batch.getId(),
                    PayrollRunEventType.PERIOD_CLOSED,
                    periodStatus.from().code(), periodStatus.to().code(), null,
                    closeDetail, userId);
        }
    }

    private PayrollRun requireLinkedRun(
            PayrollPaymentBatch batch,
            PayrollRunType expectedRunType) {
        if (batch.getPayrollRunId() == null) {
            throw new IllegalStateException(
                    installmentTypeDescription(batch.installmentTypeEnum())
                            + " is not linked to a payroll run.");
        }

        PayrollRun run = payrollRunRepository.findById(batch.getPayrollRunId())
                .orElseThrow(() -> new IllegalStateException(
                        "The linked payroll run no longer exists."));
        if (run.runTypeEnum() != expectedRunType) {
            throw new IllegalStateException(
                    "The payment batch is linked to the wrong payroll run type. Expected "
                            + expectedRunType.code() + " but found " + run.getRunType() + ".");
        }
        if (!batch.getPayrollPeriodId().equals(run.getPayrollPeriodId())) {
            throw new IllegalStateException(
                    "The payment batch and linked payroll run belong to different payroll periods.");
        }
        return run;
    }

    private static String installmentTypeDescription(PayrollInstallmentType type) {
        return type.code().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private PayrollPeriod requireOpenPeriod(Long periodId) {
        if (periodId == null) {
            throw new IllegalArgumentException(
                    "Select a payroll period. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្ស។");
        }

        PayrollPeriod period = payrollPeriodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payroll period not found."));
        if (period.statusEnum() == PayrollPeriodStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed payroll periods cannot create payment batches. "
                            + "| មិនអាចបង្កើតកញ្ចប់បើកប្រាក់សម្រាប់រយៈពេលដែលបានបិទទេ។");
        }
        return period;
    }

    private PayrollPaymentBatch requireBatch(Long batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException(
                    "Select a payment batch. | សូមជ្រើសរើសកញ្ចប់បើកប្រាក់។");
        }
        return paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment batch not found."));
    }

    private void validatePaymentDate(PayrollPeriod period, LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new IllegalArgumentException(
                    "Payment date is required. | ត្រូវបញ្ចូលថ្ងៃបើកប្រាក់។");
        }
        if (paymentDate.isBefore(period.getPeriodStart())
                || paymentDate.isAfter(period.getPeriodEnd())) {
            throw new IllegalArgumentException(
                    "Payment date must be inside the payroll period "
                            + period.getPeriodStart() + " to " + period.getPeriodEnd() + ". "
                            + "| ថ្ងៃបើកប្រាក់ត្រូវស្ថិតក្នុងរយៈពេលប្រាក់បៀវត្ស។");
        }
    }

    private void validateAdjustmentPaymentDate(PayrollPeriod period, LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new IllegalArgumentException(
                    "Payment date is required. | ត្រូវបញ្ចូលថ្ងៃបើកប្រាក់។");
        }

        LocalDate originalPaymentDate = referenceDate(period);
        if (paymentDate.isBefore(originalPaymentDate)) {
            throw new IllegalArgumentException(
                    "Adjustment settlement date cannot be before the original payroll payment date "
                            + originalPaymentDate + ". "
                            + "| ថ្ងៃទូទាត់កែតម្រូវមិនអាចមុនថ្ងៃបើកប្រាក់ដើមបានទេ។");
        }
    }

    private void requireSupportedCurrency(PayrollPeriod period) {
        if (!"USD".equalsIgnoreCase(period.getPayrollCurrency())) {
            throw new IllegalStateException(
                    "Split payments currently require payroll currency USD because Salary Tax and NSSF "
                            + "calculation is USD-based. "
                            + "| ការបើកប្រាក់ពីរដងបច្ចុប្បន្នតម្រូវឱ្យរូបិយប័ណ្ណប្រាក់បៀវត្សជា USD។");
        }
    }

    private void requireEffectiveCompanyPaymentSetting(LocalDate referenceDate) {
        if (!paymentDataRepository.hasEffectiveCompanyPaymentSetting(referenceDate)) {
            throw new IllegalStateException(
                    "Payment generation is blocked. No active company payment rule covers payroll reference date "
                            + referenceDate
                            + ". Configure Payroll Payment Settings first. "
                            + "| មិនអាចបង្កើតការបើកប្រាក់បានទេ។ "
                            + "មិនមានច្បាប់ការបើកប្រាក់របស់ក្រុមហ៊ុនសម្រាប់ថ្ងៃ "
                            + referenceDate
                            + " ទេ។ សូមកំណត់ការបើកប្រាក់ជាមុន។");
        }
    }

    private void requireActionPermission(String routeValue) {
        if (!authenticatedUser.hasPermissionRoute(routeValue, AccessPageType.UPDATED_PAGE)) {
            throw new IllegalArgumentException(
                    "You do not have permission for this payroll payment action. "
                            + "| អ្នកមិនមានសិទ្ធិសម្រាប់សកម្មភាពបើកប្រាក់នេះទេ។");
        }
    }

    private void requirePermission(AccessPageType action) {
        if (!authenticatedUser.hasPage(PayrollView.class, action)) {
            throw new IllegalArgumentException(
                    "You do not have permission for this payroll payment operation. "
                            + "| អ្នកមិនមានសិទ្ធិសម្រាប់ប្រតិបត្តិការបើកប្រាក់នេះទេ។");
        }
    }

    private Long currentUserId() {
        return authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not logged in."))
                .getId();
    }

    private static LocalDate referenceDate(PayrollPeriod period) {
        return period.getPaymentDate() == null
                ? period.getPeriodEnd()
                : period.getPaymentDate();
    }

    private static String normalizePaymentMethod(String value) {
        String method = blankToNull(value);
        if (method == null) {
            throw new IllegalArgumentException(
                    "Payment method is required. | ត្រូវជ្រើសរើសវិធីបើកប្រាក់។");
        }
        method = method.toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(method)) {
            throw new IllegalArgumentException(
                    "Unsupported payment method: " + method);
        }
        return method;
    }

    private static String requiredText(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }


    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    @Transactional(readOnly = true)
    public boolean hasPendingAdjustmentCarryForward(Long runId) {
        return paymentDataRepository.hasPendingAdjustmentCarryForward(runId);
    }

}
