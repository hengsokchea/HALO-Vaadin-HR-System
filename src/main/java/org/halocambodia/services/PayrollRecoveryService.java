package org.halocambodia.services;

import java.util.Collection;
import java.util.List;

import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollRecoveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages negative post-paid adjustment balances carried into future regular
 * payrolls as automatic deductions.
 */
@Service
public class PayrollRecoveryService {

    public static final String COMPONENT_CODE = "PRIOR_PAYROLL_RECOVERY";

    private final PayrollRecoveryRepository recoveryRepository;
    private final PayrollComponentRepository componentRepository;

    public PayrollRecoveryService(
            PayrollRecoveryRepository recoveryRepository,
            PayrollComponentRepository componentRepository) {
        this.recoveryRepository = recoveryRepository;
        this.componentRepository = componentRepository;
    }

    /**
     * Creates one recovery ledger row per PAID settlement detail with a positive
     * carry-forward amount. This covers negative adjustment settlements and any
     * final-settlement overpayment caused by an earlier first installment. The
     * source payment detail is unique, so the same balance cannot be created twice.
     */
    @Transactional
    public int createFromPaidBatch(
            Long batchId,
            Long periodId,
            Long sourceRunId,
            Long userId) {
        if (batchId == null || periodId == null || sourceRunId == null || userId == null) {
            throw new IllegalArgumentException("Payroll recovery source is incomplete.");
        }
        return recoveryRepository.createRecoveriesFromPaidBatch(
                batchId, periodId, sourceRunId, userId);
    }

    /**
     * Rebuilds all recovery reservations/items for an editable REGULAR run.
     * Recovery is capped by each employee's current net before recovery, so the
     * deduction cannot make the payroll net negative. Any remainder stays open.
     */
    @Transactional
    public int applyRun(Long runId, Long userId) {
        if (!recoveryRepository.isEditableRegularRun(runId)) {
            return 0;
        }
        Long componentId = requireRecoveryComponent(userId).getId();
        recoveryRepository.deleteReservedApplicationsForRun(runId);
        recoveryRepository.deleteRecoveryItemsForRun(runId, componentId);
        int reserved = recoveryRepository.reserveRecoveriesForRun(runId, userId);
        recoveryRepository.insertRecoveryItemsForRun(runId, componentId, userId);
        return reserved;
    }

    /** Rebuilds recovery reservations/items for selected employees only. */
    @Transactional
    public int applyEmployees(Long runId, Collection<Long> payrollEmployeeIds, Long userId) {
        if (!recoveryRepository.isEditableRegularRun(runId)) {
            return 0;
        }
        if (payrollEmployeeIds == null || payrollEmployeeIds.isEmpty()) {
            return applyRun(runId, userId);
        }

        List<Long> ids = payrollEmployeeIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return 0;
        }

        Long componentId = requireRecoveryComponent(userId).getId();
        recoveryRepository.deleteReservedApplicationsForEmployees(runId, ids);
        recoveryRepository.deleteRecoveryItemsForEmployees(ids, componentId);
        int reserved = recoveryRepository.reserveRecoveriesForEmployees(runId, ids, userId);
        recoveryRepository.insertRecoveryItemsForEmployees(runId, ids, componentId, userId);
        return reserved;
    }

    /** Rebuilds recovery for one payroll employee when its totals are refreshed. */
    @Transactional
    public int applyEmployee(Long payrollEmployeeId, Long userId) {
        if (payrollEmployeeId == null) {
            return 0;
        }
        Long runId = recoveryRepository.findRunIdByPayrollEmployeeId(payrollEmployeeId)
                .orElse(null);
        if (runId == null || !recoveryRepository.isEditableRegularRun(runId)) {
            return 0;
        }
        Long componentId = requireRecoveryComponent(userId).getId();
        recoveryRepository.deleteReservedApplicationsForEmployee(runId, payrollEmployeeId);
        recoveryRepository.deleteRecoveryItemsForEmployee(payrollEmployeeId, componentId);
        int reserved = recoveryRepository.reserveRecoveriesForEmployee(runId, payrollEmployeeId, userId);
        recoveryRepository.insertRecoveryItemsForEmployee(
                runId, payrollEmployeeId, componentId, userId);
        return reserved;
    }

    /**
     * Finalizes recovery reservations when a regular FINAL_SETTLEMENT is PAID.
     * INCLUDED employees are settled; reservations belonging to employees not
     * paid in that run are released and remain recoverable in a later payroll.
     */
    @Transactional
    public int settlePaidRegularRun(Long runId, Long userId) {
        if (runId == null || userId == null) {
            return 0;
        }
        int settled = recoveryRepository.settleIncludedReservations(runId, userId);
        recoveryRepository.releaseUnpaidReservations(runId, userId);
        recoveryRepository.refreshRecoveryStatuses(userId);
        return settled;
    }

    private PayrollComponent requireRecoveryComponent(Long userId) {
        recoveryRepository.ensureRecoveryComponent(userId);
        PayrollComponent component = componentRepository
                .findByComponentCodeIgnoreCase(COMPONENT_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "PRIOR_PAYROLL_RECOVERY payroll component could not be created. "
                                + "| មិនអាចបង្កើតធាតុ PRIOR_PAYROLL_RECOVERY បានទេ។"));
        if (!component.isActive() || !"DEDUCTION".equalsIgnoreCase(component.getComponentType())) {
            throw new IllegalStateException(
                    "PRIOR_PAYROLL_RECOVERY must be an active DEDUCTION payroll component. "
                            + "| PRIOR_PAYROLL_RECOVERY ត្រូវតែជាធាតុកាត់ប្រាក់ DEDUCTION ដែលសកម្ម។");
        }
        return component;
    }
}
