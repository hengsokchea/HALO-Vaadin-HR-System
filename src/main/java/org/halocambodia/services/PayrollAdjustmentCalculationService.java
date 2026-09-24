package org.halocambodia.services;

import static org.halocambodia.data.PayrollModels.RunRow;

import org.halocambodia.data.PayrollAdjustmentCalculationRepository;
import org.halocambodia.enums.PayrollRunType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class PayrollAdjustmentCalculationService {

    private final PayrollAdjustmentCalculationRepository repository;
    private final PayrollBaseSalaryCalculationService baseSalaryService;

    public PayrollAdjustmentCalculationService(
            PayrollAdjustmentCalculationRepository repository,
            PayrollBaseSalaryCalculationService baseSalaryService) {
        this.repository = repository;
        this.baseSalaryService = baseSalaryService;
    }

    public void ensureComponents(Long userId) {
        repository.ensureAdjustmentComponents(userId);
    }

    public void prepareRun(RunRow run, Long userId) {
        requireAdjustmentRun(run);
        ensureComponents(userId);
        repository.deleteResultItems(run.id(), null);
        repository.deleteBaselineItems(run.id(), null);
        baseSalaryService.insertMonthlyRun(run.id(), userId);
        repository.copyBaselineItems(run.periodId(), run.id(), null, userId);
    }

    public void prepareEmployee(RunRow run, Long payrollEmployeeId, Long userId) {
        requireAdjustmentRun(run);
        ensureComponents(userId);
        repository.deleteResultItems(run.id(), payrollEmployeeId);
        repository.deleteBaselineItems(run.id(), payrollEmployeeId);
        baseSalaryService.ensureMonthlyEmployee(payrollEmployeeId, userId);
        repository.copyBaselineItems(
                run.periodId(), run.id(), payrollEmployeeId, userId);
    }

    public void calculateDifferences(
            RunRow run,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        requireAdjustmentRun(run);

        long missing = repository.countMissingApprovedRegularEmployees(
                run.periodId(), run.id(), payrollEmployeeId);
        if (missing > 0) {
            throw new IllegalStateException(
                    "One or more selected employees are not in the approved regular run. "
                            + "| បុគ្គលិកម្នាក់ ឬច្រើននាក់មិនមានក្នុងដំណើរការប្រចាំខែដែលបានអនុម័តទេ។");
        }

        repository.insertDifferenceItems(
                run.periodId(), run.id(), payrollEmployeeId, userId);

        // Adjustment results must contain only the difference plus any intentional
        // current manual/OTHER items. Temporary current automatic items and the
        // copied regular-run baseline are removed after the difference is captured.
        repository.deleteCurrentCalculatedItems(run.id(), payrollEmployeeId);
        repository.deleteBaselineItems(run.id(), payrollEmployeeId);
    }

    private static void requireAdjustmentRun(RunRow run) {
        if (run == null || !PayrollRunType.ADJUSTMENT.matches(run.runType())) {
            throw new IllegalArgumentException("An ADJUSTMENT payroll run is required.");
        }
    }
}
