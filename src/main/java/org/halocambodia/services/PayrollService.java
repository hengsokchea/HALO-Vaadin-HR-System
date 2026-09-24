package org.halocambodia.services;

import static org.halocambodia.data.PayrollModels.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.PayrollApprovedAttendanceDayRepository;
import org.halocambodia.data.PayrollAttendanceDataRepository;
import org.halocambodia.data.PayrollAttendanceSummaryRepository;
import org.halocambodia.data.PayrollPeriodDataRepository;
import org.halocambodia.data.PayrollEmployeeDataRepository;
import org.halocambodia.data.PayrollEmployeeItemDataRepository;
import org.halocambodia.data.PayrollRun;
import org.halocambodia.data.PayrollRunDataRepository;
import org.halocambodia.data.PayrollRunRepository;
import org.halocambodia.data.PayrollRunEventType;
import org.halocambodia.data.PayrollAttendanceControl;
import org.halocambodia.data.PayrollAttendanceControlRepository;
import org.halocambodia.data.PayrollPeriod;
import org.halocambodia.data.PayrollPeriodRepository;
import org.halocambodia.data.PayrollPaymentSettingRepository;
import org.halocambodia.data.PayrollPaymentDataRepository;
import org.halocambodia.data.User;
import org.halocambodia.enums.PayrollAttendanceControlStatus;
import org.halocambodia.enums.PayrollEmployeeStatus;
import org.halocambodia.enums.PayrollPeriodStatus;
import org.halocambodia.enums.PayrollRunStatus;
import org.halocambodia.enums.PayrollRunType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.PayrollActionPermissions;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollService {

    private static final Set<String> AUTOMATIC_POLICY_COMPONENT_CODES = Set.of(
            "ABSENCE_DEDUCTION", "SICK_LEAVE_DEDUCTION", "MATERNITY_DEDUCTION", "OVERTIME_PAY");
    private static final Set<String> AUTOMATIC_NSSF_COMPONENT_CODES = Set.of(
            // Legacy combined components remain protected so historical items
            // cannot be edited or deleted manually.
            "NSSF_EMPLOYEE", "NSSF_EMPLOYER",
            "NSSF_HEALTH_EMPLOYEE", "NSSF_HEALTH_EMPLOYER",
            "NSSF_RISK_EMPLOYEE", "NSSF_RISK_EMPLOYER",
            "NSSF_PENSION_EMPLOYEE", "NSSF_PENSION_EMPLOYER");
    private static final Set<String> AUTOMATIC_ADJUSTMENT_COMPONENT_CODES = Set.of(
            "PAYROLL_ADJUSTMENT_EARNING", "PAYROLL_ADJUSTMENT_DEDUCTION");
    private static final Set<String> AUTOMATIC_SENIORITY_COMPONENT_CODES = Set.of(
            "SENIORITY_PAYMENT");
    private static final Set<String> AUTOMATIC_RECOVERY_COMPONENT_CODES = Set.of(
            "PRIOR_PAYROLL_RECOVERY");
    private static final Set<String> AUTOMATIC_EMPLOYEE_ADJUSTMENT_COMPONENT_CODES = Set.of(
            "FIXED_INCOME", "OTHER_DEDUCTION", "COMPANY_LOAN_REPAYMENT");
    private static final Set<String> SYSTEM_COMPONENT_CODES = Set.of(
            "BASIC_SALARY", "SALARY_TAX",
            "NSSF_EMPLOYEE", "NSSF_EMPLOYER",
            "NSSF_HEALTH_EMPLOYEE", "NSSF_HEALTH_EMPLOYER",
            "NSSF_RISK_EMPLOYEE", "NSSF_RISK_EMPLOYER",
            "NSSF_PENSION_EMPLOYEE", "NSSF_PENSION_EMPLOYER",
            "ABSENCE_DEDUCTION", "SICK_LEAVE_DEDUCTION", "MATERNITY_DEDUCTION", "OVERTIME_PAY",
            "SENIORITY_PAYMENT",
            "PAYROLL_ADJUSTMENT_EARNING", "PAYROLL_ADJUSTMENT_DEDUCTION",
            "PRIOR_PAYROLL_RECOVERY",
            "FIXED_INCOME", "OTHER_DEDUCTION", "COMPANY_LOAN_REPAYMENT");

    private final AuthenticatedUser authenticatedUser;
    private final PayrollBaseSalaryCalculationService baseSalaryCalculationService;
    private final PayrollAdjustmentCalculationService adjustmentCalculationService;
    private final PayrollRunDataRepository payrollRunDataRepository;
    private final PayrollEmployeeDataRepository payrollEmployeeDataRepository;
    private final PayrollEmployeeItemDataRepository payrollEmployeeItemDataRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollSeniorityService seniorityService;
    private final PayrollApprovedAttendanceDayRepository approvedAttendanceRepository;
    private final PayrollAttendanceDataRepository payrollAttendanceDataRepository;
    private final PayrollAttendanceSummaryRepository attendanceSummaryRepository;
    private final PayrollPeriodDataRepository payrollPeriodDataRepository;
    private final PayrollAttendanceControlRepository attendanceControlRepository;
    private final PayrollAttendanceSnapshotService attendanceSnapshotService;
    private final PayrollPolicyCalculationService policyCalculationService;
    private final PayrollNssfCalculationService nssfCalculationService;
    private final PayrollSalaryTaxCalculationService salaryTaxCalculationService;
    private final PayrollRecurringCalculationService recurringCalculationService;
    private final PayrollEmployeeTotalsService employeeTotalsService;
    private final PayrollPeriodRepository periodRepository;
    private final PayrollConfigurationAdminService configurationAdminService;
    private final PayrollPaymentSettingRepository paymentSettingRepository;
    private final PayrollPaymentDataRepository paymentDataRepository;
    private final PayrollRunEventService payrollRunEventService;

    public PayrollService(
            AuthenticatedUser authenticatedUser,
            PayrollBaseSalaryCalculationService baseSalaryCalculationService,
            PayrollAdjustmentCalculationService adjustmentCalculationService,
            PayrollRunDataRepository payrollRunDataRepository,
            PayrollEmployeeDataRepository payrollEmployeeDataRepository,
            PayrollEmployeeItemDataRepository payrollEmployeeItemDataRepository,
            PayrollRunRepository payrollRunRepository,
            PayrollSeniorityService seniorityService,
            PayrollApprovedAttendanceDayRepository approvedAttendanceRepository,
            PayrollAttendanceDataRepository payrollAttendanceDataRepository,
            PayrollAttendanceSummaryRepository attendanceSummaryRepository,
            PayrollPeriodDataRepository payrollPeriodDataRepository,
            PayrollAttendanceControlRepository attendanceControlRepository,
            PayrollAttendanceSnapshotService attendanceSnapshotService,
            PayrollPolicyCalculationService policyCalculationService,
            PayrollNssfCalculationService nssfCalculationService,
            PayrollSalaryTaxCalculationService salaryTaxCalculationService,
            PayrollRecurringCalculationService recurringCalculationService,
            PayrollEmployeeTotalsService employeeTotalsService,
            PayrollPeriodRepository periodRepository,
            PayrollConfigurationAdminService configurationAdminService,
            PayrollPaymentSettingRepository paymentSettingRepository,
            PayrollPaymentDataRepository paymentDataRepository,
            PayrollRunEventService payrollRunEventService) {
        this.authenticatedUser = authenticatedUser;
        this.baseSalaryCalculationService = baseSalaryCalculationService;
        this.adjustmentCalculationService = adjustmentCalculationService;
        this.payrollRunDataRepository = payrollRunDataRepository;
        this.payrollEmployeeDataRepository = payrollEmployeeDataRepository;
        this.payrollEmployeeItemDataRepository = payrollEmployeeItemDataRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.seniorityService = seniorityService;
        this.approvedAttendanceRepository = approvedAttendanceRepository;
        this.payrollAttendanceDataRepository = payrollAttendanceDataRepository;
        this.attendanceSummaryRepository = attendanceSummaryRepository;
        this.payrollPeriodDataRepository = payrollPeriodDataRepository;
        this.attendanceControlRepository = attendanceControlRepository;
        this.attendanceSnapshotService = attendanceSnapshotService;
        this.policyCalculationService = policyCalculationService;
        this.nssfCalculationService = nssfCalculationService;
        this.salaryTaxCalculationService = salaryTaxCalculationService;
        this.recurringCalculationService = recurringCalculationService;
        this.employeeTotalsService = employeeTotalsService;
        this.periodRepository = periodRepository;
        this.configurationAdminService = configurationAdminService;
        this.paymentSettingRepository = paymentSettingRepository;
        this.paymentDataRepository = paymentDataRepository;
        this.payrollRunEventService = payrollRunEventService;
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        return payrollRunDataRepository.getDashboardStats();
    }

    @Transactional(readOnly = true)
    public PayrollReconciliationSummary getLatestReconciliation() {
        return payrollRunDataRepository.getLatestReconciliation();
    }

    @Transactional(readOnly = true)
    public List<PeriodRow> findPeriods() {
        return payrollPeriodDataRepository.findPeriodRows();
    }

    @Transactional
    public Long savePeriod(PeriodInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Payroll period is required.");
        }
        validatePeriod(input);
        requireEffectiveCompanyPaymentSetting(input.paymentDate());
        Long userId = currentUserId();
        PayrollPeriodStatus requestedStatus = PayrollPeriodStatus.from(input.status());

        if (input.id() == null) {
            requireActionPermission(PayrollActionPermissions.PERIOD_MANAGEMENT, AccessPageType.INSERTED_PAGE);
            if (requestedStatus != PayrollPeriodStatus.OPEN) {
                throw new IllegalArgumentException("A new payroll period must start with OPEN status.");
            }

            PayrollPeriod period = new PayrollPeriod();
            period.setPayrollYear(input.year());
            period.setPayrollMonth(input.month());
            period.setPeriodStart(input.startDate());
            period.setPeriodEnd(input.endDate());
            period.setPaymentDate(input.paymentDate());
            period.setPayrollCurrency(input.currency());
            period.setUsdToKhrRate(input.usdToKhrRate());
            period.setNssfUsdToKhrRate(input.nssfUsdToKhrRate());
            period.setStatus(requestedStatus);
            period.setCreatedBy(userId);
            period.setUpdatedBy(userId);
            return periodRepository.saveAndFlush(period).getId();
        }

        requireActionPermission(PayrollActionPermissions.PERIOD_MANAGEMENT, AccessPageType.UPDATED_PAGE);
        assertPeriodEditable(input.id());

        if (payrollRunRepository.countByPayrollPeriodIdAndStatusIn(
                input.id(), Set.of(
                        PayrollRunStatus.PENDING_REVIEW.code(),
                        PayrollRunStatus.REVIEWED.code(),
                        PayrollRunStatus.APPROVED.code(),
                        PayrollRunStatus.PAID.code())) > 0) {
            throw new IllegalStateException(
                    "Payroll-period configuration cannot change after a run is sent for review, reviewed, approved, or paid.");
        }

        PayrollPeriod period = periodRepository.findByIdForUpdate(input.id())
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found."));

        if (period.statusEnum() != requestedStatus) {
            throw new IllegalStateException(
                    "Payroll-period status is controlled by the payroll workflow and cannot be edited manually.");
        }

        period.setPayrollYear(input.year());
        period.setPayrollMonth(input.month());
        period.setPeriodStart(input.startDate());
        period.setPeriodEnd(input.endDate());
        period.setPaymentDate(input.paymentDate());
        period.setPayrollCurrency(input.currency());
        period.setUsdToKhrRate(input.usdToKhrRate());
        period.setNssfUsdToKhrRate(input.nssfUsdToKhrRate());
        period.setUpdatedBy(userId);
        periodRepository.saveAndFlush(period);

        attendanceControlRepository.findByPayrollPeriodId(input.id()).ifPresent(control -> {
            control.setStatus(PayrollAttendanceControlStatus.DRAFT);
            control.setSourceLastModifiedAt(null);
            control.setCheckedAt(null);
            control.setCheckedBy(null);
            control.setApprovedAt(null);
            control.setApprovedBy(null);
            control.setUpdatedBy(userId);
            attendanceControlRepository.saveAndFlush(control);
        });

        invalidateCalculatedRunsForPeriod(input.id());
        return input.id();
    }

    @Transactional
    public void deletePeriod(Long periodId) {
        requireActionPermission(PayrollActionPermissions.PERIOD_MANAGEMENT, AccessPageType.DELETED_PAGE);
        assertPeriodEditable(periodId);

        if (payrollRunRepository.countByPayrollPeriodId(periodId) > 0) {
            throw new IllegalStateException("Delete the payroll runs before deleting this period.");
        }

        periodRepository.deleteById(periodId);
        periodRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<ComponentRow> findComponents(boolean activeOnly) {
        return configurationAdminService.findComponents(activeOnly);
    }

    @Transactional(readOnly = true)
    public List<ComponentRow> findManualEntryComponents() {
        return configurationAdminService.findComponents(true).stream()
                .filter(ComponentRow::allowManualEntry)
                .toList();
    }

    @Transactional
    public Long saveComponent(ComponentInput input) {
        return configurationAdminService.saveComponent(input);
    }

    @Transactional
    public void deleteComponent(Long componentId) {
        configurationAdminService.deleteComponent(componentId);
    }

    private void invalidateCalculatedRunsForPeriod(Long periodId) {
        payrollRunRepository.resetCalculatedRunsForPeriod(periodId);
    }



    @Transactional(readOnly = true)
    public List<TaxConfigRow> findTaxConfigs() {
        return configurationAdminService.findTaxConfigs();
    }

    @Transactional(readOnly = true)
    public List<TaxBracketRow> findTaxBrackets(Long taxConfigId) {
        return configurationAdminService.findTaxBrackets(taxConfigId);
    }

    @Transactional
    public Long saveTaxConfig(TaxConfigInput input) {
        return configurationAdminService.saveTaxConfig(input);
    }

    /**
     * Saves the Tax Year and its complete progressive bracket collection in a
     * single transaction. A null bracket list keeps the legacy parent-only
     * behaviour; a non-null list replaces the complete child collection.
     */
    @Transactional
    public Long saveTaxConfigWithBrackets(
            TaxConfigInput input,
            List<TaxBracketInput> bracketInputs) {
        return configurationAdminService.saveTaxConfigWithBrackets(input, bracketInputs);
    }





    @Transactional
    public void deleteTaxConfig(Long taxConfigId) {
        configurationAdminService.deleteTaxConfig(taxConfigId);
    }

    @Transactional
    public Long saveTaxBracket(TaxBracketInput input) {
        return configurationAdminService.saveTaxBracket(input);
    }

    @Transactional
    public void deleteTaxBracket(Long taxBracketId) {
        configurationAdminService.deleteTaxBracket(taxBracketId);
    }



    @Transactional(readOnly = true)
    public List<NssfConfigRow> findNssfConfigs() {
        return configurationAdminService.findNssfConfigs();
    }

    @Transactional(readOnly = true)
    public List<NssfBandRow> findNssfBands(Long nssfConfigId) {
        return configurationAdminService.findNssfBands(nssfConfigId);
    }

    /**
     * Validates only NSSF rules that exactly match contract types rostered in
     * the payroll period. A missing match is allowed: that contract type is
     * outside NSSF calculation and is skipped.
     */
    @Transactional(readOnly = true)
    public List<String> findNssfConfigurationIssues(Long payrollPeriodId) {
        return configurationAdminService.findNssfConfigurationIssues(payrollPeriodId);
    }

    @Transactional
    public Long saveNssfConfig(NssfConfigInput input) {
        return configurationAdminService.saveNssfConfig(input);
    }

    /** Saves an NSSF rule and its complete wage-band collection atomically. */
    @Transactional
    public Long saveNssfConfigWithBands(
            NssfConfigInput input,
            List<NssfBandInput> bandInputs) {
        return configurationAdminService.saveNssfConfigWithBands(input, bandInputs);
    }





    @Transactional
    public void deleteNssfConfig(Long nssfConfigId) {
        configurationAdminService.deleteNssfConfig(nssfConfigId);
    }

    @Transactional
    public Long saveNssfBand(NssfBandInput input) {
        return configurationAdminService.saveNssfBand(input);
    }

    @Transactional
    public void deleteNssfBand(Long nssfBandId) {
        configurationAdminService.deleteNssfBand(nssfBandId);
    }



    @Transactional(readOnly = true)
    public List<Integer> findPayrollRuleYears() {
        return configurationAdminService.findPayrollRuleYears();
    }

    @Transactional(readOnly = true)
    public List<PayrollRuleRow> findPayrollRules(Integer ruleYear) {
        return configurationAdminService.findPayrollRules(ruleYear);
    }

    @Transactional
    public Long savePayrollRule(PayrollRuleInput input) {
        return configurationAdminService.savePayrollRule(input);
    }

    @Transactional
    public void deletePayrollRule(Long ruleId) {
        configurationAdminService.deletePayrollRule(ruleId);
    }



    @Transactional(readOnly = true)
    public List<RunRow> findRuns(Long periodId) {
        return payrollRunDataRepository.findByPeriodId(periodId);
    }

    @Transactional(readOnly = true)
    public List<RunRow> findRecentRuns(int limit) {
        return payrollRunDataRepository.findRecent(limit);
    }

    @Transactional(readOnly = true)
    public Optional<RunRow> findRun(Long runId) {
        return payrollRunDataRepository.findRow(runId);
    }

    @Transactional(readOnly = true)
    public List<PayrollAuditEventRow> findRunAuditHistory(Long runId) {
        return payrollRunEventService.findRunHistory(runId);
    }

    @Transactional(readOnly = true)
    public AttendanceControlRow findAttendanceControl(Long periodId) {
        if (periodId == null) {
            throw new IllegalArgumentException("Select a payroll period first.");
        }
        return payrollAttendanceDataRepository.findControl(periodId);
    }

    @Transactional(readOnly = true)
    public List<AttendanceReviewRow> findAttendanceReview(
            Long periodId, String search, boolean issuesOnly) {
        if (periodId == null) {
            return List.of();
        }

        PayrollPeriod period = periodRepository.findById(periodId).orElse(null);
        if (period == null) {
            return List.of();
        }

        return findAttendanceSummary(
                period.getPeriodStart(), period.getPeriodEnd(), search, issuesOnly);
    }

    @Transactional(readOnly = true)
    public List<AttendanceReviewRow> findAttendanceSummary(
            LocalDate startDate, LocalDate endDate, String search, boolean issuesOnly) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End Date must be on or after Start Date. "
                            + "| ថ្ងៃបញ្ចប់ត្រូវតែស្មើ ឬក្រោយថ្ងៃចាប់ផ្ដើម។");
        }

        return payrollAttendanceDataRepository.findSummary(
                startDate, endDate, search, issuesOnly);
    }

    @Transactional
    public AttendanceControlRow checkAttendance(Long periodId, String notes) {
        requireActionPermission(PayrollActionPermissions.ATTENDANCE_LOCK, AccessPageType.UPDATED_PAGE);
        assertAttendanceControlEditable(periodId);

        Long userId = currentUserId();
        LocalDate attendanceCutoffDate = LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE);

        PayrollAttendanceControl control = attendanceControlRepository.findByPayrollPeriodId(periodId)
                .orElseGet(() -> {
                    PayrollAttendanceControl created = new PayrollAttendanceControl();
                    created.setPayrollPeriodId(periodId);
                    created.setVersion(null);
                    return created;
                });

        control.setAttendanceCutoffDate(attendanceCutoffDate);
        control.setUpdatedBy(userId);
        attendanceControlRepository.saveAndFlush(control);

        AttendanceControlRow live = findAttendanceControl(periodId);

        control.setStatus(PayrollAttendanceControlStatus.CHECKED);
        control.setRosterEmployeeCount(live.rosterEmployeeCount());
        control.setRosterDayCount(live.rosterDayCount());
        control.setAttendanceRecordCount(live.attendanceRecordCount());
        control.setRosterFallbackDayCount(live.rosterFallbackDayCount());
        control.setUnverifiedAttendanceCount(live.unverifiedAttendanceCount());
        control.setDuplicateAttendanceCount(0L);
        control.setOvertimeHours(live.overtimeHours());
        control.setSourceLastModifiedAt(live.sourceLastModifiedAt());
        control.setAttendanceCutoffDate(attendanceCutoffDate);
        control.setCheckedAt(OffsetDateTime.now());
        control.setCheckedBy(userId);
        control.setApprovedAt(null);
        control.setApprovedBy(null);
        control.setNotes(blankToNull(notes));
        control.setUpdatedBy(userId);
        attendanceControlRepository.saveAndFlush(control);

        return findAttendanceControl(periodId);
    }

    @Transactional(readOnly = true)
    private boolean hasDuplicateAttendance(Long periodId) {
        return payrollAttendanceDataRepository.hasDuplicateAttendance(periodId);
    }

    @Transactional
    public AttendanceControlRow approveAttendance(Long periodId, String notes) {
        requireActionPermission(PayrollActionPermissions.ATTENDANCE_LOCK, AccessPageType.UPDATED_PAGE);
        assertAttendanceControlEditable(periodId);

        // Approve & Lock is intentionally a single action. Refresh the attendance
        // check/snapshot first so approval always validates the latest source data.
        checkAttendance(periodId, notes);
        AttendanceControlRow control = findAttendanceControl(periodId);
        if (control.rosterEmployeeCount() == 0 || control.rosterDayCount() == 0) {
            throw new IllegalStateException("No roster data exists for this payroll period.");
        }
        if (control.unverifiedAttendanceCount() > 0) {
            throw new IllegalStateException(control.unverifiedAttendanceCount()
                    + " attendance record(s) on or before the attendance cutoff date "
                    + "are waiting for HR final verification. Future dates do not block approval.");
        }
        if (hasDuplicateAttendance(periodId)) {
            throw new IllegalStateException(
                    "Duplicate attendance records must be resolved before approval.");
        }
        if (control.stale()) {
            throw new IllegalStateException("Attendance changed after checking. Check attendance again.");
        }

        Long userId = currentUserId();
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalStateException("Payroll period was not found."));
        if (period.getPaymentDate() == null) {
            throw new IllegalStateException(
                    "Set the payroll payment date before approving attendance.");
        }

        LocalDate today = LocalDate.now(DateTimeUtilFormart.CAMBODIA_ZONE);
        if (today.isBefore(period.getPeriodStart())) {
            throw new IllegalStateException(
                    "Attendance cannot be approved before the payroll period starts.");
        }
        approvedAttendanceRepository.deleteByPayrollPeriodId(periodId);
        approvedAttendanceRepository.flush();
        int capturedDays = approvedAttendanceRepository.captureApprovedAttendance(periodId, userId);
        if (capturedDays != control.rosterDayCount()) {
            throw new IllegalStateException(
                    "Approved attendance snapshot saved " + capturedDays
                    + " day(s), but the attendance check found " + control.rosterDayCount()
                    + ". Check attendance again before approval.");
        }

        PayrollAttendanceControl attendanceControl = attendanceControlRepository.findByPayrollPeriodId(periodId)
                .orElseThrow(() -> new IllegalStateException(
                        "Attendance has not been checked for this period."));
        LocalDate attendanceCutoffDate = attendanceControl.getAttendanceCutoffDate();
        if (attendanceCutoffDate == null) {
            // Backward compatibility for a record checked before the cutoff column
            // was populated. New checks always store Cambodia's current date.
            attendanceCutoffDate = today;
        }
        attendanceControl.setStatus(PayrollAttendanceControlStatus.APPROVED);
        attendanceControl.setApprovedAt(OffsetDateTime.now());
        attendanceControl.setApprovedBy(userId);
        attendanceControl.setAttendanceCutoffDate(attendanceCutoffDate);
        if (blankToNull(notes) != null) attendanceControl.setNotes(notes.trim());
        attendanceControl.setUpdatedBy(userId);
        attendanceControlRepository.saveAndFlush(attendanceControl);
        return findAttendanceControl(periodId);
    }

    @Transactional
    public AttendanceControlRow reopenAttendance(Long periodId, String notes) {
        requireActionPermission(PayrollActionPermissions.ATTENDANCE_LOCK, AccessPageType.UPDATED_PAGE);
        assertAttendanceControlEditable(periodId);
        Long userId = currentUserId();
        PayrollAttendanceControl attendanceControl = attendanceControlRepository.findByPayrollPeriodId(periodId)
                .orElseThrow(() -> new IllegalStateException(
                        "Attendance has not been checked for this period."));
        attendanceControl.setStatus(PayrollAttendanceControlStatus.DRAFT);
        attendanceControl.setApprovedAt(null);
        attendanceControl.setApprovedBy(null);
        attendanceControl.setAttendanceCutoffDate(null);
        if (blankToNull(notes) != null) attendanceControl.setNotes(notes.trim());
        attendanceControl.setUpdatedBy(userId);
        attendanceControlRepository.saveAndFlush(attendanceControl);

        approvedAttendanceRepository.deleteByPayrollPeriodId(periodId);
        return findAttendanceControl(periodId);
    }

    @Transactional
    public Long createRun(Long periodId, String runType, String notes) {
        requireActionPermission(PayrollActionPermissions.RUN_MANAGEMENT, AccessPageType.UPDATED_PAGE);
        if (periodId == null) {
            throw new IllegalArgumentException("A valid payroll period and run type are required.");
        }
        PayrollRunType normalizedRunType;
        try {
            normalizedRunType = PayrollRunType.from(runType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("A valid payroll period and run type are required.", ex);
        }
        if (normalizedRunType == PayrollRunType.FINAL_PAYMENT) {
            throw new IllegalArgumentException(
                    "FINAL_PAYMENT is no longer available. Leaving employees are processed in REGULAR payroll. "
                            + "| FINAL_PAYMENT លែងប្រើទៀតហើយ។ បុគ្គលិកចាកចេញត្រូវដំណើរការក្នុង REGULAR។");
        }

        PayrollPeriod period = periodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found."));

        // A closed period may be reopened only by a controlled adjustment run.
        if (normalizedRunType != PayrollRunType.ADJUSTMENT) {
            assertPeriodEditable(periodId);
        }
        if (normalizedRunType != PayrollRunType.ADJUSTMENT) {
            requireApprovedAttendance(periodId);
        }
        validateRunTypeCreation(periodId, normalizedRunType);

        Long userId = currentUserId();
        if (normalizedRunType == PayrollRunType.ADJUSTMENT) {
            adjustmentCalculationService.ensureComponents(userId);
        }

        int nextRun = payrollRunRepository
                .findTopByPayrollPeriodIdOrderByRunNumberDesc(periodId)
                .map(PayrollRun::getRunNumber)
                .orElse(0) + 1;

        PayrollRun run = new PayrollRun();
        run.setPayrollPeriodId(periodId);
        run.setRunNumber(nextRun);
        run.setRunType(normalizedRunType);
        run.setStatus(PayrollRunStatus.DRAFT);
        run.setCorrectionMode(false);
        run.setNotes(blankToNull(notes));
        run.setCreatedBy(userId);
        run = payrollRunRepository.saveAndFlush(run);
        payrollRunEventService.record(
                periodId, run.getId(), null, null,
                PayrollRunEventType.RUN_CREATED,
                null, PayrollRunStatus.DRAFT.code(), notes,
                normalizedRunType.code() + " · Run " + nextRun,
                userId);

        period.setStatus(PayrollPeriodStatus.PROCESSING);
        period.setUpdatedBy(userId);
        periodRepository.saveAndFlush(period);
        return run.getId();
    }

    @Transactional(readOnly = true)
    public List<String> findAllowedRunTypes(Long periodId) {
        if (periodId == null) {
            return List.of();
        }
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found."));

        List<String> result = new java.util.ArrayList<>();
        long regularRuns = payrollRunRepository.countByPayrollPeriodIdAndRunTypeAndStatusNot(
                periodId, PayrollRunType.REGULAR.code(), PayrollRunStatus.CANCELLED.code());
        if (period.statusEnum() != PayrollPeriodStatus.CLOSED && regularRuns == 0) {
            result.add(PayrollRunType.REGULAR.code());
        }

        long approvedRegular = payrollRunRepository.countByPayrollPeriodIdAndRunTypeAndStatusIn(
                periodId, PayrollRunType.REGULAR.code(), Set.of(PayrollRunStatus.APPROVED.code(), PayrollRunStatus.PAID.code()));
        long openAdjustmentRuns = payrollRunRepository.countByPayrollPeriodIdAndRunTypeAndStatusNotIn(
                periodId, PayrollRunType.ADJUSTMENT.code(),
                Set.of(PayrollRunStatus.PAID.code(), PayrollRunStatus.CANCELLED.code()));
        if (approvedRegular > 0 && openAdjustmentRuns == 0) {
            result.add(PayrollRunType.ADJUSTMENT.code());
        }
        return List.copyOf(result);
    }

    private void validateRunTypeCreation(Long periodId, PayrollRunType runType) {
        if (runType == PayrollRunType.REGULAR) {
            long existing = payrollRunRepository.countByPayrollPeriodIdAndRunTypeAndStatusNot(
                    periodId, PayrollRunType.REGULAR.code(), PayrollRunStatus.CANCELLED.code());
            if (existing > 0) {
                throw new IllegalStateException(
                        "This payroll period already has a regular run. Use an adjustment run for corrections. "
                                + "| រយៈពេលនេះមានដំណើរការប្រចាំខែរួចហើយ។ សូមប្រើដំណើរការកែតម្រូវ។");
            }
        }
        if (runType == PayrollRunType.ADJUSTMENT) {
            long approvedRegular = payrollRunRepository.countByPayrollPeriodIdAndRunTypeAndStatusIn(
                    periodId, PayrollRunType.REGULAR.code(), Set.of(PayrollRunStatus.APPROVED.code(), PayrollRunStatus.PAID.code()));
            if (approvedRegular == 0) {
                throw new IllegalStateException(
                        "An adjustment run requires an approved regular payroll for the same period. "
                                + "| ដំណើរការកែតម្រូវត្រូវការដំណើរការប្រចាំខែដែលបានអនុម័តសម្រាប់រយៈពេលដូចគ្នា។");
            }
            long openAdjustment = payrollRunRepository.countByPayrollPeriodIdAndRunTypeAndStatusNotIn(
                    periodId, PayrollRunType.ADJUSTMENT.code(),
                    Set.of(PayrollRunStatus.PAID.code(), PayrollRunStatus.CANCELLED.code()));
            if (openAdjustment > 0) {
                throw new IllegalStateException(
                        "Pay/finalize or cancel the current adjustment run before creating another adjustment. "
                                + "| សូមបញ្ចប់ ឬបោះបង់ដំណើរការកែតម្រូវបច្ចុប្បន្ន មុនបង្កើតការកែតម្រូវថ្មី។");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PayrollCandidateRow> findPayrollCandidates(Long runId) {
        RunRow run = findRun(runId).orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));
        return switch (PayrollRunType.from(run.runType())) {
            case ADJUSTMENT -> payrollEmployeeDataRepository
                    .findAdjustmentCandidates(run.periodId(), runId);
            case FINAL_PAYMENT -> payrollEmployeeDataRepository
                    .findFinalPaymentCandidates(run.periodId(), runId);
            case REGULAR -> payrollEmployeeDataRepository
                    .findRegularCandidates(run.periodId(), runId);
        };
    }

    @Transactional
    public int generatePayrollEmployees(Long runId) {
        return generatePayrollEmployees(runId, Set.of());
    }

    @Transactional
    public int generatePayrollEmployees(Long runId, Set<Long> selectedEmpIds) {
        requireActionPermission(PayrollActionPermissions.RUN_MANAGEMENT, AccessPageType.UPDATED_PAGE);
        RunRow run = requireEditableRun(runId);
        requireNoOpenCorrectionsForRunLevelChange(runId,"Generate Employees is disabled while employee corrections are open. "  + "Recalculate the returned employee(s) and send the run for review again first. " + "| មិនអាចបង្កើតបុគ្គលិកបន្ថែម ខណៈពេលមានបុគ្គលិកត្រូវកែតម្រូវទេ។");
        requireApprovedAttendanceForRun(run);
        Long userId = currentUserId();

        Set<Long> requested = selectedEmpIds == null ? Set.of() : selectedEmpIds.stream().filter(java.util.Objects::nonNull) .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (PayrollRunType.REGULAR.matches(run.runType()) && !requested.isEmpty()) {
            throw new IllegalArgumentException("A regular run generates all rostered employees. Do not pass an employee selection.");
        }
        if (PayrollRunType.ADJUSTMENT.matches(run.runType()) && requested.isEmpty()) {
            // Attendance reconciliation is automatic for ADJUSTMENT runs. Load every
            // remaining employee from the paid/approved REGULAR baseline; employees
            // whose post-payment attendance did not change simply produce zero difference.
            requested = findPayrollCandidates(runId).stream()
                    .map(PayrollCandidateRow::empId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        if (!PayrollRunType.REGULAR.matches(run.runType()) && requested.isEmpty()) {
            throw new IllegalArgumentException("No eligible employee remains for this payroll run. | មិនមានបុគ្គលិកដែលមានសិទ្ធិនៅសល់សម្រាប់ដំណើរការនេះទេ។");
        }
        if (!requested.isEmpty()) {
            Set<Long> allowed = findPayrollCandidates(runId).stream()
                    .map(PayrollCandidateRow::empId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!allowed.containsAll(requested)) {
                throw new IllegalArgumentException("One or more selected employees are not eligible for this payroll run. " + "| បុគ្គលិកម្នាក់ ឬច្រើននាក់មិនមានសិទ្ធិសម្រាប់ដំណើរការនេះ។");
            }
        }
        int inserted = PayrollRunType.REGULAR.matches(run.runType()) ? payrollEmployeeDataRepository.insertRegularPayrollEmployees( runId, run.periodId(), userId)  : payrollEmployeeDataRepository.insertSelectedPayrollEmployees( runId, run.periodId(), requested, userId);

        snapshotAttendance(runId, run.periodId(), null);
        if (PayrollRunType.REGULAR.matches(run.runType())) {
            baseSalaryCalculationService.insertMonthlyRun(runId, userId);
        } else if (PayrollRunType.FINAL_PAYMENT.matches(run.runType())) {
            baseSalaryCalculationService.insertFinalPayment(runId, null, userId);
        }

        if (PayrollRunType.ADJUSTMENT.matches(run.runType())) {
            // First compare the rebuilt Adjustment attendance snapshot with the frozen
            // Regular snapshot. Remove unchanged employees BEFORE the expensive payroll
            // calculation pipeline so recurring/policy/seniority/NSSF/tax calculations
            // run only for employees whose post-payment payroll inputs actually changed.
            payrollEmployeeDataRepository.excludeAdjustmentEmployeesWithoutAttendanceChange(runId, userId);
            clearExcludedPayrollData(runId);
            payrollEmployeeDataRepository.deleteExcludedAdjustmentEmployees(runId);

            int changedEmployees = payrollEmployeeDataRepository.countIncludedEmployees(runId);
            if (changedEmployees == 0) {
                employeeTotalsService.refreshRun(runId);
                return 0;
            }

            // Calculate only the attendance-changed employees, then apply the final
            // financial-difference filter. An attendance change that has no monetary
            // effect is removed from the Adjustment run as well.
            recalculateRunInternal(runId, true);
            payrollEmployeeDataRepository.excludeAdjustmentEmployeesWithoutDifference(runId, userId);
            clearExcludedPayrollData(runId);
            payrollEmployeeDataRepository.deleteExcludedAdjustmentEmployees(runId);
            employeeTotalsService.refreshRun(runId);
            return payrollEmployeeDataRepository.countIncludedEmployees(runId);
        }

        recalculateRunInternal(runId, false);
        return inserted;
    }

    @Transactional(readOnly = true)
    public List<EmployeePayrollRow> findEmployees(Long runId, String search) {
        return payrollEmployeeDataRepository.findEmployees(runId, search);
    }

    @Transactional(readOnly = true)
    public int countCorrectionRequiredEmployees(Long runId) {
        if (runId == null) {
            return 0;
        }
        return payrollEmployeeDataRepository.countCorrectionRequired(runId);
    }

    @Transactional
    public int changePayrollEmployeeStatus(
            Long runId, Set<Long> payrollEmployeeIds, String targetStatus, String notes) {
        requireActionPermission(PayrollActionPermissions.EMPLOYEE_STATUS, AccessPageType.UPDATED_PAGE);
        RunRow run = requireEditableRun(runId);

        PayrollEmployeeStatus status;
        try {
            status = PayrollEmployeeStatus.from(targetStatus);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Payroll status must be INCLUDED, ON_HOLD, or EXCLUDED. "
                            + "| ស្ថានភាពប្រាក់បៀវត្សត្រូវតែជា INCLUDED, ON_HOLD ឬ EXCLUDED។", ex);
        }
        if (payrollEmployeeIds == null || payrollEmployeeIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one payroll employee. "
                            + "| សូមជ្រើសរើសបុគ្គលិកប្រាក់បៀវត្សយ៉ាងតិចម្នាក់។");
        }

        Set<Long> employeeIds = payrollEmployeeIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (employeeIds.size() != payrollEmployeeIds.size()) {
            throw new IllegalArgumentException(
                    "Selected payroll employee is invalid. "
                            + "| បុគ្គលិកប្រាក់បៀវត្សដែលបានជ្រើសរើសមិនត្រឹមត្រូវ។");
        }
        int belongsToRun = payrollEmployeeDataRepository.countEmployeesInRun(runId, employeeIds);
        if (belongsToRun != employeeIds.size()) {
            throw new IllegalArgumentException(
                    "One selected employee does not belong to this payroll run. "
                            + "| បុគ្គលិកម្នាក់ដែលបានជ្រើសរើសមិនស្ថិតក្នុងដំណើរការប្រាក់បៀវត្សនេះទេ។");
        }

        boolean correctionMode = run.correctionMode();
        requireEmployeesEditableInCorrectionMode(runId, employeeIds);

        String reason = blankToNull(notes);
        String statusNote = reason == null ? null
                : "Payroll status " + status.code() + " | ស្ថានភាពប្រាក់បៀវត្ស "
                        + status.code() + ": " + reason;
        Long userId = currentUserId();

        int updated = payrollEmployeeDataRepository.updatePayrollStatus(
                runId, employeeIds, status.code(), statusNote, userId);
        if (updated != employeeIds.size()) {
            throw new IllegalStateException(
                    "Payroll employee changed while the status update was running. Refresh and try again.");
        }

        if (status == PayrollEmployeeStatus.EXCLUDED) {
            clearExcludedPayrollData(runId);
        } else if (status == PayrollEmployeeStatus.INCLUDED) {
            if (PayrollRunStatus.CALCULATED.matches(run.status())) {
                requireApprovedAttendanceForRun(run);
            }
            for (Long payrollEmployeeId : employeeIds) {
                if (PayrollRunStatus.CALCULATED.matches(run.status())) {
                    snapshotAttendance(runId, run.periodId(), payrollEmployeeId);
                    calculateEmployeeForRunType(run, payrollEmployeeId);
                } else if (PayrollRunType.REGULAR.matches(run.runType())) {
                    baseSalaryCalculationService.ensureMonthlyEmployee(payrollEmployeeId, userId);
                    recalculateEmployee(payrollEmployeeId);
                } else if (PayrollRunType.FINAL_PAYMENT.matches(run.runType())) {
                    baseSalaryCalculationService.insertFinalPayment(runId, payrollEmployeeId, userId);
                    recalculateEmployee(payrollEmployeeId);
                } else {
                    recalculateEmployee(payrollEmployeeId);
                }
            }
        }

        if (correctionMode) {
            for (Long payrollEmployeeId : employeeIds) {
                markCorrectionResolved(payrollEmployeeId, userId);
            }
        }

        if (PayrollRunStatus.CALCULATED.matches(run.status())) {
            touchRunCalculated(runId, userId);
        }

        return updated;
    }

    @Transactional(readOnly = true)
    public Optional<AttendanceRow> findAttendance(Long payrollEmployeeId) {
        if (payrollEmployeeId == null) {
            return Optional.empty();
        }

        return attendanceSummaryRepository.findById(payrollEmployeeId)
                .map(summary -> new AttendanceRow(
                        summary.getScheduledDays(),
                        summary.getWorkedDays(),
                        summary.getHolidayDays(),
                        summary.getAbsentDays(),
                        summary.getAnnualLeaveDays(),
                        summary.getSpecialLeaveDays(),
                        summary.getSickLeaveDays(),
                        summary.getMaternityLeaveDays(),
                        summary.getOtherPaidLeaveDays(),
                        summary.getUnpaidLeaveDays(),
                        summary.getOvertimeHours(),
                        summary.getAnnualLeaveRemaining(),
                        summary.getOverusedAnnualLeave(),
                        summary.getSpecialLeaveRemaining(),
                        summary.getOverusedSpecialLeave(),
                        summary.getSourceStartDate(),
                        summary.getSourceEndDate()));
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> findAttendancePolicyQuantities(Long payrollEmployeeId) {
        if (payrollEmployeeId == null) {
            return Map.of();
        }
        return payrollAttendanceDataRepository.findPolicyQuantities(payrollEmployeeId);
    }

    @Transactional(readOnly = true)
    public Set<String> findAttendanceRuleCodesForPeriod(Long periodId) {
        if (periodId == null) {
            return Set.of();
        }
        return payrollAttendanceDataRepository.findRuleCodesForPeriod(periodId);
    }

    @Transactional(readOnly = true)
    public Set<String> findAttendanceRuleCodesForDateRange(
            LocalDate startDate, LocalDate endDate) {
        return payrollAttendanceDataRepository.findRuleCodesForDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ItemRow> findItems(Long payrollEmployeeId) {
        return payrollEmployeeItemDataRepository.findItems(payrollEmployeeId);
    }

    @Transactional
    public Long saveItem(ItemInput input) {
        if (input == null || input.payrollEmployeeId() == null || input.componentId() == null) {
            throw new IllegalArgumentException("Employee and payroll component are required.");
        }
        if (input.amount() == null || input.amount().signum() < 0) {
            throw new IllegalArgumentException("Amount must be zero or greater.");
        }

        ComponentRow selectedComponent = configurationAdminService.findComponents(false).stream()
                .filter(component -> component.id().equals(input.componentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payroll component was not found."));
        requireManualEntryComponent(selectedComponent);
        String componentCode = selectedComponent.code();

        if (input.id() != null) {
            String existingCode = payrollEmployeeItemDataRepository
                    .findExistingItemComponentCode(input.id(), input.payrollEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Payroll item was not found."));
            if (payrollEmployeeItemDataRepository
                    .isRecurringOrLoanItem(input.id(), input.payrollEmployeeId())) {
                throw new IllegalStateException(
                        "This payroll item is generated from Payroll > Recurring & Loans. "
                                + "Change it there and recalculate payroll instead. "
                                + "| ធាតុប្រាក់បៀវត្សនេះត្រូវបានបង្កើតពី Payroll > Recurring & Loans។ "
                                + "សូមកែប្រែនៅទីនោះ ហើយគណនាប្រាក់បៀវត្សឡើងវិញ។");
            }
            requireManualComponent(existingCode);
            if (payrollEmployeeItemDataRepository
                    .isAutomaticPolicyItem(input.id(), input.payrollEmployeeId())) {
                throw new IllegalStateException(
                        "This payroll item is calculated automatically. Change attendance or the yearly payroll rule instead. "
                                + "| ប្រព័ន្ធគណនាធាតុនេះដោយស្វ័យប្រវត្តិ។ "
                                + "សូមកែវត្តមាន ឬច្បាប់ប្រាក់បៀវត្សប្រចាំឆ្នាំ។");
            }
        }

        Long runId = payrollEmployeeItemDataRepository
                .findRunIdByPayrollEmployeeId(input.payrollEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Payroll employee was not found."));
        RunRow run = requireEditableRun(runId);
        requireEmployeeEditableInCorrectionMode(runId, input.payrollEmployeeId());
        requireIncludedEmployee(input.payrollEmployeeId());

        Long userId = currentUserId();
        BigDecimal exchangeRate = input.exchangeRate() == null ? BigDecimal.ONE : input.exchangeRate();
        BigDecimal payrollAmount = input.amount().multiply(exchangeRate);
        BigDecimal quantity = input.quantity() == null ? BigDecimal.ONE : input.quantity();

        Long id;
        if (input.id() == null) {
            requireActionPermission(PayrollActionPermissions.MANUAL_ITEM, AccessPageType.INSERTED_PAGE);
            id = payrollEmployeeItemDataRepository.insert(
                    input.payrollEmployeeId(), input.componentId(), blankToNull(input.description()),
                    quantity, input.rate(), input.amount(), input.currency(), exchangeRate, payrollAmount,
                    input.sourceType(), blankToNull(input.remarks()), userId);
        } else {
            requireActionPermission(PayrollActionPermissions.MANUAL_ITEM, AccessPageType.UPDATED_PAGE);
            int updated = payrollEmployeeItemDataRepository.update(
                    input.id(), input.payrollEmployeeId(), input.componentId(),
                    blankToNull(input.description()), quantity, input.rate(), input.amount(),
                    input.currency(), exchangeRate, payrollAmount, input.sourceType(),
                    blankToNull(input.remarks()), userId);
            if (updated != 1) {
                throw new IllegalStateException(
                        "Payroll item changed while the update was running. Refresh and try again.");
            }
            id = input.id();
        }

        if (PayrollRunStatus.CALCULATED.matches(run.status())) {
            calculateEmployeeForRunType(run, input.payrollEmployeeId());
        } else {
            recalculateEmployee(input.payrollEmployeeId());
        }
        return id;
    }

    @Transactional
    public void deleteItem(ItemRow item) {
        requireActionPermission(PayrollActionPermissions.MANUAL_ITEM, AccessPageType.DELETED_PAGE);
        if (item == null || item.id() == null) {
            return;
        }
        if ("BASIC_SALARY".equals(item.componentCode()) || "SALARY_TAX".equals(item.componentCode())
                || AUTOMATIC_POLICY_COMPONENT_CODES.contains(item.componentCode())
                || AUTOMATIC_NSSF_COMPONENT_CODES.contains(item.componentCode())
                || AUTOMATIC_ADJUSTMENT_COMPONENT_CODES.contains(item.componentCode())
                || AUTOMATIC_SENIORITY_COMPONENT_CODES.contains(item.componentCode())
                || AUTOMATIC_RECOVERY_COMPONENT_CODES.contains(item.componentCode())
                || payrollEmployeeItemDataRepository.isRecurringOrLoanItem(item.id(), item.payrollEmployeeId())
                || isAutomaticPolicyItem(item)) {
            throw new IllegalStateException("System-calculated payroll items cannot be deleted manually.");
        }

        Long runId = payrollEmployeeItemDataRepository.findRunIdByItemId(item.id())
                .orElseThrow(() -> new IllegalArgumentException("Payroll item was not found."));
        RunRow run = requireEditableRun(runId);
        requireEmployeeEditableInCorrectionMode(runId, item.payrollEmployeeId());
        requireIncludedEmployee(item.payrollEmployeeId());

        int deleted = payrollEmployeeItemDataRepository.delete(item.id());
        if (deleted != 1) {
            throw new IllegalStateException(
                    "Payroll item changed while the delete was running. Refresh and try again.");
        }
        if (PayrollRunStatus.CALCULATED.matches(run.status())) {
            calculateEmployeeForRunType(run, item.payrollEmployeeId());
        } else {
            recalculateEmployee(item.payrollEmployeeId());
        }
    }

    @Transactional
    public void recalculateRun(Long runId) {
        requireActionPermission(PayrollActionPermissions.CALCULATE, AccessPageType.UPDATED_PAGE);
        RunRow run = requireEditableRun(runId);
        requireApprovedAttendanceForRun(run);

        List<Long> correctionEmployeeIds = findOpenCorrectionEmployeeIds(runId);
        if (run.correctionMode()) {
            if (correctionEmployeeIds.isEmpty()) {
                throw new IllegalStateException(
                        "All returned employees are already recalculated. Send the payroll for review again. "
                                + "| បុគ្គលិកដែលត្រូវកែតម្រូវទាំងអស់បានគណនាឡើងវិញរួចហើយ។ "
                                + "សូមផ្ញើប្រាក់បៀវត្សទៅពិនិត្យម្តងទៀត។");
            }
            Long userId = currentUserId();
            for (Long payrollEmployeeId : correctionEmployeeIds) {
                snapshotAttendance(runId, run.periodId(), payrollEmployeeId);
                calculateEmployeeItemsForRunType(run, payrollEmployeeId);
                markCorrectionResolved(payrollEmployeeId, userId);
                payrollRunEventService.record(
                        run.periodId(), runId, payrollEmployeeId, null,
                        PayrollRunEventType.EMPLOYEE_RECALCULATED,
                        "CORRECTION_REQUIRED", "CORRECTED", null,
                        "Returned employee recalculated", userId);
            }
            employeeTotalsService.refreshEmployees(runId, correctionEmployeeIds);
            touchRunCalculated(runId, userId);
            return;
        }

        snapshotAttendance(runId, run.periodId(), null);
        recalculateRunInternal(runId, true);
    }

    @Transactional
    public int recalculateEmployees(Long runId, Set<Long> payrollEmployeeIds) {
        if (payrollEmployeeIds == null || payrollEmployeeIds.isEmpty()) {
            int correctionCount = countCorrectionRequiredEmployees(runId);
            recalculateRun(runId);
            if (correctionCount > 0) {
                return correctionCount;
            }
            return payrollEmployeeDataRepository.countIncluded(runId);
        }

        requireActionPermission(PayrollActionPermissions.CALCULATE, AccessPageType.UPDATED_PAGE);
        RunRow run = requireEditableRun(runId);
        requireApprovedAttendanceForRun(run);

        boolean correctionMode = run.correctionMode();

        for (Long payrollEmployeeId : payrollEmployeeIds) {
            if (payrollEmployeeId == null) {
                throw new IllegalArgumentException("Selected payroll employee is invalid.");
            }
            if (!payrollEmployeeDataRepository.isIncludedInRun(runId, payrollEmployeeId)) {
                throw new IllegalArgumentException(
                        "Only INCLUDED employees can be recalculated. Change the employee payroll status first. "
                                + "| អាចគណនាឡើងវិញបានតែបុគ្គលិកដែលមានស្ថានភាព INCLUDED ប៉ុណ្ណោះ។ "
                                + "សូមប្តូរស្ថានភាពប្រាក់បៀវត្សរបស់បុគ្គលិកជាមុនសិន។");
            }
        }

        if (correctionMode) {
            requireEmployeesEditableInCorrectionMode(runId, payrollEmployeeIds);
        }

        Long userId = currentUserId();
        for (Long payrollEmployeeId : payrollEmployeeIds) {
            // Refresh only selected employees. Unselected employees keep the attendance
            // snapshot that belongs to their existing calculated result.
            snapshotAttendance(runId, run.periodId(), payrollEmployeeId);
            calculateEmployeeItemsForRunType(run, payrollEmployeeId);
            if (correctionMode) {
                markCorrectionResolved(payrollEmployeeId, userId);
                payrollRunEventService.record(
                        run.periodId(), runId, payrollEmployeeId, null,
                        PayrollRunEventType.EMPLOYEE_RECALCULATED,
                        "CORRECTION_REQUIRED", "CORRECTED", null,
                        "Returned employee recalculated", userId);
            }
        }
        employeeTotalsService.refreshEmployees(runId, payrollEmployeeIds);

        /*
         * A REGULAR run stays DRAFT after a selected-only calculation because
         * the unselected monthly employees may still be uncalculated. An
         * ADJUSTMENT run intentionally contains only affected employees, so it
         * may become CALCULATED when the selection covers every INCLUDED
         * employee in that adjustment run. Correction mode is already CALCULATED
         * and never recalculates employees that were not returned.
         */
        int includedEmployeeCount = payrollEmployeeDataRepository.countIncluded(runId);
        boolean completeDraftAdjustment = !correctionMode
                && PayrollRunStatus.DRAFT.matches(run.status())
                && PayrollRunType.ADJUSTMENT.matches(run.runType())
                && includedEmployeeCount > 0
                && includedEmployeeCount == payrollEmployeeIds.size();

        if (completeDraftAdjustment) {
            markRunCalculated(runId, userId);
        } else if (PayrollRunStatus.CALCULATED.matches(run.status())) {
            touchRunCalculated(runId, userId);
        }
        return payrollEmployeeIds.size();
    }

    @Transactional
    public int returnRunForCorrection(
            Long runId, Set<Long> selectedPayrollEmployeeIds, String reason) {
        requireActionPermission(PayrollActionPermissions.RETURN_CORRECTION, AccessPageType.UPDATED_PAGE);

        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));
        PayrollRunStatus returnFromStatus = run.statusEnum();
        if (!Set.of(PayrollRunStatus.PENDING_REVIEW, PayrollRunStatus.REVIEWED).contains(returnFromStatus)) {
            throw new IllegalStateException(
                    "Only a payroll run that is pending review or already reviewed can be returned for correction. "
                            + "Current status: " + run.getStatus() + ". "
                            + "| អាចបញ្ជូនត្រឡប់ទៅកែតម្រូវបានតែដំណើរការដែលកំពុងរង់ចាំពិនិត្យ "
                            + "ឬបានពិនិត្យរួចប៉ុណ្ណោះ។");
        }

        String correctionReason = blankToNull(reason);
        if (correctionReason == null) {
            throw new IllegalArgumentException(
                    "Enter a reason before returning payroll for correction. "
                            + "| សូមបញ្ចូលមូលហេតុមុនបញ្ជូនប្រាក់បៀវត្សត្រឡប់ទៅកែតម្រូវ។");
        }

        Set<Long> requested = selectedPayrollEmployeeIds == null
                ? new LinkedHashSet<>()
                : selectedPayrollEmployeeIds.stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean returnAll = requested.isEmpty();

        Set<Long> targetEmployeeIds = new LinkedHashSet<>();
        if (returnAll) {
            targetEmployeeIds.addAll(
                    payrollEmployeeDataRepository.findIncludedEmployeeIds(runId));
        } else {
            for (Long payrollEmployeeId : requested) {
                if (!payrollEmployeeDataRepository.isIncludedInRun(runId, payrollEmployeeId)) {
                    throw new IllegalArgumentException(
                            "Only INCLUDED employees from this payroll run can be returned for correction. "
                                    + "| អាចបញ្ជូនត្រឡប់ទៅកែតម្រូវបានតែបុគ្គលិក INCLUDED "
                                    + "ក្នុងដំណើរការប្រាក់បៀវត្សនេះប៉ុណ្ណោះ។");
                }
                targetEmployeeIds.add(payrollEmployeeId);
            }
        }

        if (targetEmployeeIds.isEmpty()) {
            throw new IllegalStateException(
                    "There are no INCLUDED employees to return for correction. "
                            + "| មិនមានបុគ្គលិក INCLUDED សម្រាប់បញ្ជូនត្រឡប់ទៅកែតម្រូវទេ។");
        }

        User currentUser = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in."));
        Long returnedById = currentUser.getId();
        String returnedBy = blankToNull(currentUser.getUsername());
        if (returnedBy == null) {
            returnedBy = "User " + returnedById;
        }

        payrollEmployeeDataRepository.resetCorrectionScope(runId, returnedById);
        int returned = payrollEmployeeDataRepository.markReturnedForCorrection(
                runId, targetEmployeeIds, correctionReason, returnedById);
        if (returned != targetEmployeeIds.size()) {
            throw new IllegalStateException(
                    "Payroll employee correction scope changed while the request was being saved. Refresh and try again.");
        }

        String scope = returnAll
                ? "ALL INCLUDED EMPLOYEES (" + targetEmployeeIds.size() + ")"
                : "SELECTED EMPLOYEES (" + targetEmployeeIds.size() + ")";
        String timestamp = OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String auditLine = "[RETURN FOR CORRECTION " + timestamp + "] "
                + returnedBy + " · " + scope + ": " + correctionReason;

        String existingNotes = blankToNull(run.getNotes());
        run.setNotes(existingNotes == null ? auditLine : existingNotes + "\n" + auditLine);
        run.setStatus(PayrollRunStatus.CALCULATED);
        run.setCorrectionMode(true);
        run.setReviewedAt(null);
        run.setReviewedBy(null);
        payrollRunRepository.saveAndFlush(run);
        payrollRunEventService.record(
                run.getPayrollPeriodId(), runId, null, null,
                PayrollRunEventType.RETURNED_FOR_CORRECTION,
                returnFromStatus.code(), PayrollRunStatus.CALCULATED.code(), correctionReason,
                scope, returnedById);

        return returned;
    }

    @Transactional
    public void moveRunTo(Long runId, String targetStatus) {
        // Action permission is checked after the requested target status is parsed.

        PayrollRun runEntity = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));
        RunRow run = findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));

        PayrollRunStatus target = PayrollRunStatus.from(targetStatus);
        if (target == PayrollRunStatus.PENDING_REVIEW) {
            requireActionPermission(PayrollActionPermissions.SEND_REVIEW, AccessPageType.UPDATED_PAGE);
        } else if (target == PayrollRunStatus.REVIEWED) {
            requireActionPermission(PayrollActionPermissions.REVIEW, AccessPageType.UPDATED_PAGE);
        } else if (target == PayrollRunStatus.APPROVED) {
            requireActionPermission(PayrollActionPermissions.APPROVE, AccessPageType.UPDATED_PAGE);
        }
        Map<PayrollRunStatus, Set<PayrollRunStatus>> allowed = Map.of(
                PayrollRunStatus.CALCULATED, Set.of(PayrollRunStatus.PENDING_REVIEW),
                PayrollRunStatus.PENDING_REVIEW, Set.of(PayrollRunStatus.REVIEWED),
                PayrollRunStatus.REVIEWED, Set.of(PayrollRunStatus.APPROVED));
        if (!allowed.getOrDefault(runEntity.statusEnum(), Set.of()).contains(target)) {
            throw new IllegalStateException(
                    "Cannot change payroll from " + runEntity.getStatus() + " to " + target.code() + ". "
                            + "A run must be CALCULATED before it is sent for review, "
                            + "PENDING_REVIEW before it is marked REVIEWED, "
                            + "and REVIEWED before approval. "
                            + "| មិនអាចប្តូរស្ថានភាពប្រាក់បៀវត្សពី " + runEntity.getStatus()
                            + " ទៅ " + target.code() + " បានទេ។");
        }
        if (run.employeeCount() == 0) {
            throw new IllegalStateException("Generate payroll employees before changing the status.");
        }

        if (target == PayrollRunStatus.PENDING_REVIEW) {
            int openCorrections = countCorrectionRequiredEmployees(runId);
            if (openCorrections > 0) {
                throw new IllegalStateException(
                        openCorrections + " returned employee(s) still require correction. "
                                + "Recalculate them before sending payroll for review again. "
                                + "| នៅមានបុគ្គលិក " + openCorrections
                                + " នាក់ត្រូវកែតម្រូវ។ សូមគណនាពួកគេឡើងវិញមុនផ្ញើទៅពិនិត្យម្តងទៀត។");
            }
        }

        if (Set.of(
                PayrollRunStatus.PENDING_REVIEW,
                PayrollRunStatus.REVIEWED,
                PayrollRunStatus.APPROVED).contains(target)) {
            requireApprovedAttendanceForRun(run);
        }

        Long userId = currentUserId();
        OffsetDateTime now = OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE);

        PayrollRunStatus fromStatus = runEntity.statusEnum();

        switch (target) {
            case PENDING_REVIEW -> {
                runEntity.setStatus(PayrollRunStatus.PENDING_REVIEW);
                runEntity.setCorrectionMode(false);
                runEntity.setReviewedAt(null);
                runEntity.setReviewedBy(null);
                payrollEmployeeDataRepository.clearCorrectionScope(runId, userId);
            }
            case REVIEWED -> {
                runEntity.setStatus(PayrollRunStatus.REVIEWED);
                runEntity.setReviewedAt(now);
                runEntity.setReviewedBy(userId);
            }
            case APPROVED -> {
                runEntity.setStatus(PayrollRunStatus.APPROVED);
                runEntity.setApprovedAt(now);
                runEntity.setApprovedBy(userId);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported payroll status. Payroll payment must be completed through the Payments tab.");
        }
        payrollRunRepository.saveAndFlush(runEntity);

        if (target == PayrollRunStatus.PENDING_REVIEW) {
            payrollRunEventService.record(
                    run.periodId(), runId, null, null,
                    PayrollRunEventType.SENT_FOR_REVIEW,
                    fromStatus.code(), PayrollRunStatus.PENDING_REVIEW.code(), null,
                    "Payroll submitted for review", userId);
        } else if (target == PayrollRunStatus.REVIEWED) {
            payrollRunEventService.record(
                    run.periodId(), runId, null, null,
                    PayrollRunEventType.RUN_REVIEWED,
                    fromStatus.code(), PayrollRunStatus.REVIEWED.code(), null,
                    "Payroll review completed", userId);
        } else if (target == PayrollRunStatus.APPROVED) {
            payrollRunEventService.record(
                    run.periodId(), runId, null, null,
                    PayrollRunEventType.RUN_APPROVED,
                    fromStatus.code(), PayrollRunStatus.APPROVED.code(), null,
                    null, userId);
        }
    }

    private void touchRunCalculated(Long runId, Long userId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));
        run.setCalculatedAt(OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE));
        run.setCalculatedBy(userId);
        payrollRunRepository.saveAndFlush(run);
        payrollRunEventService.record(
                run.getPayrollPeriodId(), runId, null, null,
                PayrollRunEventType.RUN_RECALCULATED,
                PayrollRunStatus.CALCULATED.code(), PayrollRunStatus.CALCULATED.code(), null,
                null, userId);
    }

    private void markRunCalculated(Long runId, Long userId) {
        PayrollRun run = payrollRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));
        if (!Set.of(PayrollRunStatus.DRAFT, PayrollRunStatus.CALCULATED).contains(run.statusEnum())) {
            throw new IllegalStateException(
                    "Payroll status changed while calculation was running. Refresh and try again.");
        }
        PayrollRunStatus fromStatus = run.statusEnum();
        run.setStatus(PayrollRunStatus.CALCULATED);
        run.setCalculatedAt(OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE));
        run.setCalculatedBy(userId);
        payrollRunRepository.saveAndFlush(run);
        payrollRunEventService.record(
                run.getPayrollPeriodId(), runId, null, null,
                fromStatus == PayrollRunStatus.CALCULATED
                        ? PayrollRunEventType.RUN_RECALCULATED
                        : PayrollRunEventType.RUN_CALCULATED,
                fromStatus.code(), PayrollRunStatus.CALCULATED.code(), null,
                null, userId);
    }

    private void snapshotAttendance(Long runId, Long periodId, @Nullable Long payrollEmployeeId) {
        attendanceSnapshotService.refresh(runId, periodId, payrollEmployeeId);
    }

    private void recalculateRunInternal(Long runId, boolean markCalculated) {
        clearExcludedPayrollData(runId);
        RunRow run = findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));

        if (markCalculated) {
            prepareRunForCalculation(run);
            recurringCalculationService.calculate(run, null, currentUserId());
            calculateAutomaticPolicyItems(runId, null);
            seniorityService.calculate(runId, null);
            nssfCalculationService.calculate(runId, null, currentUserId());
            salaryTaxCalculationService.calculate(runId, null, currentUserId());

            if (PayrollRunType.ADJUSTMENT.matches(run.runType())) {
                // Bulk: one PostgreSQL calculation for all INCLUDED adjustment employees.
                adjustmentCalculationService.calculateDifferences(
                        run, null, currentUserId());
            }
        }

        employeeTotalsService.refreshRun(runId);

        if (markCalculated) {
            markRunCalculated(runId, currentUserId());
        }
    }

    private void prepareRunForCalculation(RunRow run) {
        Long userId = currentUserId();
        switch (PayrollRunType.from(run.runType())) {
            case ADJUSTMENT -> adjustmentCalculationService.prepareRun(run, userId);
            case FINAL_PAYMENT -> baseSalaryCalculationService.replaceFinalPayment(
                    run.id(), null, userId);
            case REGULAR -> baseSalaryCalculationService.insertMonthlyRun(run.id(), userId);
        }
    }

    private void prepareEmployeeForCalculation(RunRow run, Long payrollEmployeeId) {
        Long userId = currentUserId();
        switch (PayrollRunType.from(run.runType())) {
            case ADJUSTMENT -> adjustmentCalculationService.prepareEmployee(
                    run, payrollEmployeeId, userId);
            case FINAL_PAYMENT -> baseSalaryCalculationService.replaceFinalPayment(
                    run.id(), payrollEmployeeId, userId);
            case REGULAR -> baseSalaryCalculationService.ensureMonthlyEmployee(
                    payrollEmployeeId, userId);
        }
    }

    private void calculateEmployeeForRunType(RunRow run, Long payrollEmployeeId) {
        calculateEmployeeItemsForRunType(run, payrollEmployeeId);
        employeeTotalsService.refreshEmployee(payrollEmployeeId);
    }

    private void calculateEmployeeItemsForRunType(RunRow run, Long payrollEmployeeId) {
        prepareEmployeeForCalculation(run, payrollEmployeeId);
        recurringCalculationService.calculate(run, payrollEmployeeId, currentUserId());
        calculateAutomaticPolicyItems(run.id(), payrollEmployeeId);
        seniorityService.calculate(run.id(), payrollEmployeeId);
        nssfCalculationService.calculate(run.id(), payrollEmployeeId, currentUserId());
        salaryTaxCalculationService.calculate(run.id(), payrollEmployeeId, currentUserId());
        if (PayrollRunType.ADJUSTMENT.matches(run.runType())) {
            adjustmentCalculationService.calculateDifferences(
                    run, payrollEmployeeId, currentUserId());
        }
    }

    /**
     * Defensive cleanup for legacy rows whose status may previously have been edited
     * directly in SQL. Normal status changes use changePayrollEmployeeStatus(...).
     */
    private void clearExcludedPayrollData(Long runId) {
        payrollEmployeeDataRepository.clearExcludedCalculatedData(runId, currentUserId());
    }

    private void calculateAutomaticPolicyItems(Long runId, Long payrollEmployeeId) {
        policyCalculationService.calculate(runId, payrollEmployeeId, currentUserId());
    }

    /** Automatic rules use their Rule Code as the attendance trigger code. */
    private static String attendanceTriggerCode(String ruleCode) {
        return upperOrNull(ruleCode);
    }

    private static void requireManualEntryComponent(ComponentRow component) {
        if (component == null || !component.active() || !component.allowManualEntry()) {
            throw new IllegalStateException(
                    "This payroll component is not enabled for direct manual entry. "
                            + "Enable Manual Entry in Payroll Components when appropriate. "
                            + "| ធាតុប្រាក់បៀវត្សនេះមិនត្រូវបានអនុញ្ញាតឱ្យបញ្ចូលដោយដៃទេ។");
        }
    }

    private static void requireManualComponent(String componentCode) {
        if ("BASIC_SALARY".equals(componentCode)) {
            throw new IllegalStateException(
                    "BASIC_SALARY is generated from the employee salary source and cannot be edited manually. "
                            + "Update the salary source, then generate employees again. "
                            + "| BASIC_SALARY ត្រូវបានបង្កើតពីប្រភពប្រាក់បៀវត្សរបស់និយោជិត "
                            + "ហើយមិនអាចកែដោយដៃបានទេ។");
        }
        if ("SALARY_TAX".equals(componentCode)) {
            throw new IllegalStateException(
                    "SALARY_TAX is calculated automatically. Change taxable earnings or the yearly tax rules instead.");
        }
        if (AUTOMATIC_NSSF_COMPONENT_CODES.contains(componentCode)) {
            throw new IllegalStateException(
                    componentCode + " is calculated automatically. Change the effective NSSF rules instead. "
                            + "| ប្រព័ន្ធគណនា " + componentCode
                            + " ដោយស្វ័យប្រវត្តិ។ សូមកែប្រែច្បាប់ ប.ស.ស. ដែលមានសុពលភាព។");
        }
        if (AUTOMATIC_POLICY_COMPONENT_CODES.contains(componentCode)) {
            throw new IllegalStateException(
                    componentCode + " is calculated automatically. Change attendance or the yearly payroll rules instead.");
        }
        if (AUTOMATIC_ADJUSTMENT_COMPONENT_CODES.contains(componentCode)) {
            throw new IllegalStateException(
                    componentCode + " is calculated automatically from the approved regular run. "
                            + "| ប្រព័ន្ធគណនាការកែតម្រូវនេះដោយស្វ័យប្រវត្តិពីដំណើរការប្រចាំខែដែលបានអនុម័ត។");
        }
        if (AUTOMATIC_SENIORITY_COMPONENT_CODES.contains(componentCode)) {
            throw new IllegalStateException(
                    componentCode + " is calculated automatically from the effective Seniority Payment Rule. "
                            + "| ប្រព័ន្ធគណនា " + componentCode
                            + " ដោយស្វ័យប្រវត្តិពីច្បាប់ប្រាក់បំណាច់អតីតភាព។");
        }
        if (AUTOMATIC_RECOVERY_COMPONENT_CODES.contains(componentCode)) {
            throw new IllegalStateException(
                    componentCode + " is generated automatically from unpaid post-paid adjustment recovery. "
                            + "Finalize the source adjustment and recalculate the next regular payroll instead. "
                            + "| ប្រព័ន្ធបង្កើតការកាត់ប្រាក់សងត្រឡប់នេះដោយស្វ័យប្រវត្តិ។");
        }
        if (AUTOMATIC_EMPLOYEE_ADJUSTMENT_COMPONENT_CODES.contains(componentCode)) {
            throw new IllegalStateException(
                    componentCode + " is managed from Payroll > Recurring & Loans and cannot be edited manually. "
                            + "| សូមកំណត់ធាតុនេះតាម Payroll > Recurring & Loans។");
        }
    }

    private static boolean isAutomaticPolicyItem(ItemRow item) {
        if (item == null || item.sourceType() == null || item.remarks() == null
                || !Set.of("ATTENDANCE", "LEAVE", "OVERTIME").contains(item.sourceType())) {
            return false;
        }
        return item.remarks().matches("^Rule [A-Z0-9_]+;.*");
    }

    private void recalculateEmployee(Long payrollEmployeeId) {
        employeeTotalsService.refreshEmployee(payrollEmployeeId);
    }

    private boolean isRunCorrectionMode(Long runId) {
        return payrollRunDataRepository.isCorrectionMode(runId);
    }

    private List<Long> findOpenCorrectionEmployeeIds(Long runId) {
        return payrollEmployeeDataRepository.findOpenCorrectionEmployeeIds(runId);
    }

    private void requireNoOpenCorrectionsForRunLevelChange(Long runId, String message) {
        if (isRunCorrectionMode(runId)) {
            throw new IllegalStateException(message);
        }
    }

    private void requireEmployeeEditableInCorrectionMode(Long runId, Long payrollEmployeeId) {
        if (!isRunCorrectionMode(runId)) {
            return;
        }
        if (!payrollEmployeeDataRepository.isReturnedForCorrection(runId, payrollEmployeeId)) {
            throw new IllegalStateException(
                    "This employee was not returned for correction and remains locked. "
                            + "Only returned employees can be changed during this correction cycle. "
                            + "| បុគ្គលិកនេះមិនត្រូវបានបញ្ជូនត្រឡប់មកកែតម្រូវទេ ហើយនៅតែត្រូវបានចាក់សោ។");
        }
    }

    private void requireEmployeesEditableInCorrectionMode(Long runId, Set<Long> payrollEmployeeIds) {
        if (!isRunCorrectionMode(runId)) {
            return;
        }
        for (Long payrollEmployeeId : payrollEmployeeIds) {
            requireEmployeeEditableInCorrectionMode(runId, payrollEmployeeId);
        }
    }

    private void markCorrectionResolved(Long payrollEmployeeId, Long userId) {
        payrollEmployeeDataRepository.markCorrectionResolved(payrollEmployeeId, userId);
    }

    private RunRow requireEditableRun(Long runId) {
        RunRow run = findRun(runId).orElseThrow(() -> new IllegalArgumentException("Payroll run not found."));
        if (!Set.of(PayrollRunStatus.DRAFT, PayrollRunStatus.CALCULATED).contains(PayrollRunStatus.from(run.status()))) {
            throw new IllegalStateException("Payroll run is read-only because its status is " + run.status() + ".");
        }
        return run;
    }

    private void requireIncludedEmployee(Long payrollEmployeeId) {
        if (!payrollEmployeeDataRepository.isIncluded(payrollEmployeeId)) {
            throw new IllegalStateException(
                    "Payroll items can be changed only while the employee status is INCLUDED. "
                            + "| អាចកែប្រែធាតុប្រាក់បៀវត្សបានតែនៅពេលស្ថានភាពបុគ្គលិកជា INCLUDED ប៉ុណ្ណោះ។");
        }
    }

    /**
     * ADJUSTMENT runs operate against an already approved/paid regular payroll.
     * They must not be blocked by the current attendance approval state of the
     * closed/reopened period. REGULAR and FINAL_PAYMENT continue to require an
     * approved, current attendance snapshot.
     */
    private void requireApprovedAttendanceForRun(RunRow run) {
        if (run == null) {
            throw new IllegalArgumentException("Payroll run is required.");
        }
        if (!PayrollRunType.ADJUSTMENT.matches(run.runType())) {
            requireApprovedAttendance(run.periodId());
        }
    }

    private void requireApprovedAttendance(Long periodId) {
        AttendanceControlRow control = findAttendanceControl(periodId);
        if (!control.approvedAndCurrent()) {
            if (control.stale()) {
                throw new IllegalStateException("Attendance or roster data changed after approval. Check and approve attendance again.");
            }
            throw new IllegalStateException("Attendance must be checked and approved before generating or calculating payroll.");
        }
    }

    private void assertAttendanceControlEditable(Long periodId) {
        assertPeriodEditable(periodId);
        if (payrollRunRepository.countByPayrollPeriodIdAndStatusIn(
                periodId, Set.of(
                        PayrollRunStatus.PENDING_REVIEW.code(),
                        PayrollRunStatus.REVIEWED.code(),
                        PayrollRunStatus.APPROVED.code(),
                        PayrollRunStatus.PAID.code())) > 0) {
            throw new IllegalStateException(
                    "Attendance cannot be changed because this period has a payroll run that is pending review, reviewed, approved, or paid.");
        }
    }

    private void assertPeriodEditable(Long periodId) {
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found."));
        if (period.statusEnum() == PayrollPeriodStatus.CLOSED) {
            throw new IllegalStateException("Closed payroll periods cannot be changed.");
        }
    }

    private void validatePeriod(PeriodInput input) {
        if (input.year() < 2000 || input.year() > 2100 || input.month() < 1 || input.month() > 12) {
            throw new IllegalArgumentException("Enter a valid payroll year and month.");
        }
        if (input.startDate() == null || input.endDate() == null || input.endDate().isBefore(input.startDate())) {
            throw new IllegalArgumentException("Enter a valid period start and end date.");
        }
        if (input.paymentDate() == null) {
            throw new IllegalArgumentException(
                    "Payment date is required because tax and NSSF rules use it. "
                            + "| ត្រូវបញ្ចូលថ្ងៃបើកប្រាក់ ព្រោះច្បាប់ពន្ធ និង ប.ស.ស. ប្រើកាលបរិច្ឆេទនេះ។");
        }
        if (input.currency() == null || input.currency().length() != 3) {
            throw new IllegalArgumentException("Currency must contain three characters, for example USD.");
        }
        if (input.usdToKhrRate() == null || input.usdToKhrRate().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Payroll/Tax USD to KHR exchange rate must be greater than zero. "
                            + "| អត្រាប្តូរ USD ទៅ KHR សម្រាប់ប្រាក់បៀវត្ស/ពន្ធ ត្រូវតែធំជាងសូន្យ។");
        }
        if (input.nssfUsdToKhrRate() == null || input.nssfUsdToKhrRate().signum() <= 0) {
            throw new IllegalArgumentException(
                    "NSSF USD to KHR exchange rate must be greater than zero. "
                            + "| អត្រាប្តូរ USD ទៅ KHR សម្រាប់ ប.ស.ស. ត្រូវតែធំជាងសូន្យ។");
        }
        try {
            PayrollPeriodStatus.from(input.status());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid payroll-period status.", ex);
        }
    }

    /**
     * A payroll period must never be saved without an active company payment
     * rule that is effective on its payment date. The rule may optionally carry
     * a Shift exception when its frequency is MONTHLY.
     */
    private void requireEffectiveCompanyPaymentSetting(LocalDate paymentDate) {
        boolean configured = paymentSettingRepository
                .countActiveCompanySettingsForDate(paymentDate) > 0;

        if (!configured) {
            throw new IllegalStateException(
                    "Cannot save payroll period. No active company payment rule covers payment date "
                            + paymentDate
                            + ". Configure Payroll Payment Settings first. "
                            + "| មិនអាចរក្សាទុករយៈពេលប្រាក់បៀវត្សបានទេ។ "
                            + "មិនមានច្បាប់ការបើកប្រាក់របស់ក្រុមហ៊ុនដែលមានប្រសិទ្ធភាពសម្រាប់ថ្ងៃ "
                            + paymentDate
                            + " ទេ។ សូមកំណត់ការបើកប្រាក់ជាមុន។");
        }
    }















    private void requireActionPermission(String routeValue, AccessPageType type) {
        if (!authenticatedUser.hasPermissionRoute(routeValue, type)) {
            throw new IllegalArgumentException(
                    "You don't have permission for this payroll action. | អ្នកមិនមានសិទ្ធិសម្រាប់សកម្មភាពប្រាក់បៀវត្សនេះទេ។");
        }
    }

    private void requirePermission(AccessPageType type) {
        if (!authenticatedUser.hasPage(PayrollView.class, type)) {
            throw new IllegalArgumentException("You don't have permission to perform this payroll operation.");
        }
    }

    private Long currentUserId() {
        User user = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in."));
        return user.getId();
    }


    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
    @Transactional(readOnly = true)
    public boolean hasPendingAdjustmentCarryForward(Long runId) {
        return paymentDataRepository.hasPendingAdjustmentCarryForward(runId);
    }

}
