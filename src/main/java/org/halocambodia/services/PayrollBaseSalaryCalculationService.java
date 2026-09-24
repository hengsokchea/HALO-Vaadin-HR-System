package org.halocambodia.services;

import org.halocambodia.data.PayrollBaseSalaryCalculationRepository;
import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class PayrollBaseSalaryCalculationService {

    private final PayrollBaseSalaryCalculationRepository repository;
    private final PayrollComponentRepository componentRepository;

    public PayrollBaseSalaryCalculationService(
            PayrollBaseSalaryCalculationRepository repository,
            PayrollComponentRepository componentRepository) {
        this.repository = repository;
        this.componentRepository = componentRepository;
    }

    public int insertMonthlyRun(Long runId, Long userId) {
        requireBasicSalaryComponent();
        return repository.insertMonthlyItems(runId, userId);
    }

    public int ensureMonthlyEmployee(Long payrollEmployeeId, Long userId) {
        Long componentId = requireBasicSalaryComponent().getId();
        return repository.ensureMonthlyItem(payrollEmployeeId, componentId, userId);
    }

    public int replaceMonthlyEmployee(
            Long runId,
            Long payrollEmployeeId,
            Long userId) {
        Long componentId = requireBasicSalaryComponent().getId();
        repository.deleteItems(runId, payrollEmployeeId);
        return repository.ensureMonthlyItem(payrollEmployeeId, componentId, userId);
    }

    /**
     * Replaces BASIC_SALARY for every INCLUDED employee in the run using the
     * current payroll_employee salary snapshot. Used by the full correction
     * bulk path after the snapshots have been refreshed from Employee Master.
     */
    public int replaceMonthlyRun(Long runId, Long userId) {
        requireBasicSalaryComponent();
        repository.deleteItems(runId, null);
        return repository.insertMonthlyItems(runId, userId);
    }

    public int replaceFinalPayment(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        Long componentId = requireBasicSalaryComponent().getId();
        repository.deleteItems(runId, payrollEmployeeId);
        return repository.insertFinalPaymentItems(
                runId, payrollEmployeeId, componentId, userId);
    }

    public int insertFinalPayment(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        Long componentId = requireBasicSalaryComponent().getId();
        return repository.insertFinalPaymentItems(
                runId, payrollEmployeeId, componentId, userId);
    }

    public int delete(Long runId, @Nullable Long payrollEmployeeId) {
        return repository.deleteItems(runId, payrollEmployeeId);
    }

    private PayrollComponent requireBasicSalaryComponent() {
        PayrollComponent component = componentRepository
                .findByComponentCodeIgnoreCase("BASIC_SALARY")
                .orElseThrow(() -> new IllegalStateException(
                        "Active BASIC_SALARY EARNING payroll component is required for automatic calculation. "
                                + "| ត្រូវមានសមាសភាគប្រាក់បៀវត្ស BASIC_SALARY EARNING ដែលសកម្ម "
                                + "សម្រាប់ការគណនាដោយស្វ័យប្រវត្តិ។"));
        if (!component.isActive() || !"EARNING".equalsIgnoreCase(component.getComponentType())) {
            throw new IllegalStateException(
                    "Active BASIC_SALARY EARNING payroll component is required for automatic calculation. "
                            + "| ត្រូវមានសមាសភាគប្រាក់បៀវត្ស BASIC_SALARY EARNING ដែលសកម្ម "
                            + "សម្រាប់ការគណនាដោយស្វ័យប្រវត្តិ។");
        }
        return component;
    }
}
