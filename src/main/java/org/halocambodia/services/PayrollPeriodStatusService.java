package org.halocambodia.services;

import java.util.Set;

import org.halocambodia.data.PayrollPeriod;
import org.halocambodia.data.PayrollPeriodRepository;
import org.halocambodia.data.PayrollRunRepository;
import org.halocambodia.enums.PayrollPeriodStatus;
import org.halocambodia.enums.PayrollRunStatus;
import org.halocambodia.enums.PayrollRunType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single source of truth for the derived payroll-period workflow status.
 *
 * A period is CLOSED only when its regular payroll has been paid and there is
 * no unfinished adjustment. Creating a new adjustment reopens the period to
 * PROCESSING through the normal run-creation flow.
 */
@Service
public class PayrollPeriodStatusService {

    private static final Set<String> FINISHED_ADJUSTMENT_STATUSES = Set.of(
            PayrollRunStatus.PAID.code(),
            PayrollRunStatus.CANCELLED.code());

    private final PayrollPeriodRepository periodRepository;
    private final PayrollRunRepository runRepository;

    public PayrollPeriodStatusService(
            PayrollPeriodRepository periodRepository,
            PayrollRunRepository runRepository) {
        this.periodRepository = periodRepository;
        this.runRepository = runRepository;
    }

    @Transactional
    public StatusChange refresh(Long periodId, Long userId) {
        if (periodId == null) {
            throw new IllegalArgumentException("Payroll period is required.");
        }

        PayrollPeriod period = periodRepository.findByIdForUpdate(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found."));

        PayrollPeriodStatus from = period.statusEnum();
        PayrollPeriodStatus target = derive(periodId, from);
        if (from == target) {
            return new StatusChange(from, target, false);
        }

        period.setStatus(target);
        period.setUpdatedBy(userId);
        periodRepository.saveAndFlush(period);
        return new StatusChange(from, target, true);
    }

    private PayrollPeriodStatus derive(Long periodId, PayrollPeriodStatus current) {
        long paidRegular = runRepository.countByPayrollPeriodIdAndRunTypeAndStatusIn(
                periodId,
                PayrollRunType.REGULAR.code(),
                Set.of(PayrollRunStatus.PAID.code()));

        if (paidRegular == 0) {
            return current == PayrollPeriodStatus.CLOSED
                    ? PayrollPeriodStatus.PROCESSING
                    : current;
        }

        long unfinishedAdjustments = runRepository.countByPayrollPeriodIdAndRunTypeAndStatusNotIn(
                periodId,
                PayrollRunType.ADJUSTMENT.code(),
                FINISHED_ADJUSTMENT_STATUSES);

        return unfinishedAdjustments == 0
                ? PayrollPeriodStatus.CLOSED
                : PayrollPeriodStatus.PROCESSING;
    }

    public record StatusChange(
            PayrollPeriodStatus from,
            PayrollPeriodStatus to,
            boolean changed) {
    }
}
