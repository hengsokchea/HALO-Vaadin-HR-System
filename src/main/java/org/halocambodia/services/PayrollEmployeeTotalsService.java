package org.halocambodia.services;

import java.util.Collection;
import java.util.List;

import org.halocambodia.data.PayrollEmployeeRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds payroll employee monetary totals with set-based PostgreSQL updates.
 *
 * The service contains no SQL and no JdbcTemplate. Database work is owned by
 * {@link PayrollEmployeeRepository}.
 */
@Service
public class PayrollEmployeeTotalsService {

    private final PayrollEmployeeRepository payrollEmployeeRepository;
    private final PayrollRecoveryService payrollRecoveryService;
    private final AuthenticatedUser authenticatedUser;

    public PayrollEmployeeTotalsService(
            PayrollEmployeeRepository payrollEmployeeRepository,
            PayrollRecoveryService payrollRecoveryService,
            AuthenticatedUser authenticatedUser) {
        this.payrollEmployeeRepository = payrollEmployeeRepository;
        this.payrollRecoveryService = payrollRecoveryService;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional
    public int refreshRun(Long runId) {
        if (runId == null) {
            throw new IllegalArgumentException("Payroll run is required.");
        }
        Long userId = currentUserId();
        payrollRecoveryService.applyRun(runId, userId);
        return payrollEmployeeRepository.refreshTotalsForRun(runId, userId);
    }

    @Transactional
    public int refreshEmployees(Long runId, Collection<Long> payrollEmployeeIds) {
        if (runId == null) {
            throw new IllegalArgumentException("Payroll run is required.");
        }
        if (payrollEmployeeIds == null || payrollEmployeeIds.isEmpty()) {
            return refreshRun(runId);
        }

        List<Long> ids = payrollEmployeeIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return refreshRun(runId);
        }

        Long userId = currentUserId();
        payrollRecoveryService.applyEmployees(runId, ids, userId);
        return payrollEmployeeRepository.refreshTotalsForEmployees(runId, ids, userId);
    }

    @Transactional
    public int refreshEmployee(Long payrollEmployeeId) {
        if (payrollEmployeeId == null) {
            throw new IllegalArgumentException("Payroll employee is required.");
        }
        Long userId = currentUserId();
        payrollRecoveryService.applyEmployee(payrollEmployeeId, userId);
        return payrollEmployeeRepository.refreshTotalsForEmployee(payrollEmployeeId, userId);
    }
    private Long currentUserId() {
        return authenticatedUser.get()
                .orElseThrow(() -> new IllegalStateException("User is not logged in."))
                .getId();
    }

}
