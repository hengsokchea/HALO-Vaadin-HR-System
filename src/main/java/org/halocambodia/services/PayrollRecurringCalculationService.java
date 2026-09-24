package org.halocambodia.services;

import static org.halocambodia.data.PayrollModels.RunRow;

import org.halocambodia.data.PayrollEmployeeAdjustmentRepository;
import org.halocambodia.data.PayrollRecurringCalculationRepository;
import org.halocambodia.enums.PayrollRunType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies recurring employee payroll configuration and company-loan deductions.
 * Reservations are safe to rebuild during recalculation and are financially
 * settled only when the REGULAR Final Settlement is marked PAID.
 */
@Service
public class PayrollRecurringCalculationService {

    private final PayrollEmployeeAdjustmentRepository adjustmentRepository;
    private final PayrollRecurringCalculationRepository calculationRepository;

    public PayrollRecurringCalculationService(
            PayrollEmployeeAdjustmentRepository adjustmentRepository,
            PayrollRecurringCalculationRepository calculationRepository) {
        this.adjustmentRepository = adjustmentRepository;
        this.calculationRepository = calculationRepository;
    }

    @Transactional
    public void calculate(
            RunRow run,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        if (run == null || !PayrollRunType.REGULAR.matches(run.runType())) {
            return;
        }

        adjustmentRepository.ensureSystemComponents(userId);

        // Synchronize positive carry-forwards before rebuilding recurring
        // applications. This repairs older finalized adjustments whose
        // carry-forward row was not created and makes them available to the
        // next eligible REGULAR payroll on recalculation.
        calculationRepository.syncPositiveAdjustmentCarryForwards(run.id(), userId);

        // Recalculation replaces only RESERVED/generated data. Settled history is
        // immutable and is never changed here.
        calculationRepository.deleteGeneratedItems(run.id(), payrollEmployeeId);
        calculationRepository.deleteReservedRecurringApplications(run.id(), payrollEmployeeId);
        calculationRepository.deleteReservedLoanRepayments(run.id(), payrollEmployeeId);

        calculationRepository.insertRecurringApplications(run.id(), payrollEmployeeId, userId);
        calculationRepository.insertRecurringItems(run.id(), payrollEmployeeId, userId);

        calculationRepository.insertLoanReservations(run.id(), payrollEmployeeId, userId);
        calculationRepository.insertLoanItems(run.id(), payrollEmployeeId, userId);
    }

    @Transactional
    public void settlePaidRegularRun(Long runId, Long userId) {
        if (runId == null) {
            return;
        }
        long conflicts = calculationRepository.countLoanSettlementConflicts(runId);
        if (conflicts > 0) {
            throw new IllegalStateException(
                    "One or more company-loan balances changed after payroll approval. "
                            + "Reconcile the loan before confirming Final Settlement. "
                            + "| សមតុល្យប្រាក់កម្ចីបុគ្គលិកបានផ្លាស់ប្តូរបន្ទាប់ពីអនុម័តប្រាក់បៀវត្ស។");
        }

        calculationRepository.settleLoans(runId, userId);
        calculationRepository.settleLoanRepayments(runId, userId);
        calculationRepository.settleRecurringApplications(runId, userId);

        // ONE_TIME earnings/deductions are complete only after the Final
        // Settlement is really PAID. Deactivate them at that point so the
        // employee payroll setup shows INACTIVE and they cannot be reused.
        calculationRepository.deactivateSettledOneTimeRecurring(runId, userId);
    }
}
