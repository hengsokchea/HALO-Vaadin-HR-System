package org.halocambodia.services;

import static org.halocambodia.data.PayrollModels.PayrollAuditEventRow;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.PayrollEmployee;
import org.halocambodia.data.PayrollEmployeeRepository;
import org.halocambodia.data.PayrollRunEvent;
import org.halocambodia.data.PayrollRunEventRepository;
import org.halocambodia.data.PayrollRunEventType;
import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollRunEventService {

    private final PayrollRunEventRepository repository;
    private final UserRepository userRepository;
    private final PayrollEmployeeRepository payrollEmployeeRepository;

    public PayrollRunEventService(
            PayrollRunEventRepository repository,
            UserRepository userRepository,
            PayrollEmployeeRepository payrollEmployeeRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.payrollEmployeeRepository = payrollEmployeeRepository;
    }

    /** Records one append-only audit event inside the caller's transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            Long payrollPeriodId,
            Long payrollRunId,
            Long payrollEmployeeId,
            Long payrollPaymentBatchId,
            PayrollRunEventType eventType,
            String fromStatus,
            String toStatus,
            String reason,
            String detail,
            Long userId) {
        if (payrollPeriodId == null) {
            throw new IllegalArgumentException("Payroll period is required for audit history.");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("Payroll audit event type is required.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Payroll audit user is required.");
        }

        repository.save(new PayrollRunEvent(
                payrollPeriodId,
                payrollRunId,
                payrollEmployeeId,
                payrollPaymentBatchId,
                eventType,
                blankToNull(fromStatus),
                blankToNull(toStatus),
                blankToNull(reason),
                blankToNull(detail),
                OffsetDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE),
                userId));
    }

    /**
     * Records the same employee-level recalculation audit as record(...), but
     * for every open returned employee using one INSERT ... SELECT statement.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int recordReturnedEmployeesRecalculatedBulk(
            Long payrollPeriodId,
            Long payrollRunId,
            String detail,
            Long userId) {
        if (payrollPeriodId == null) {
            throw new IllegalArgumentException("Payroll period is required for audit history.");
        }
        if (payrollRunId == null) {
            throw new IllegalArgumentException("Payroll run is required for audit history.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Payroll audit user is required.");
        }
        return repository.insertReturnedEmployeeRecalculatedEvents(
                payrollPeriodId, payrollRunId, blankToNull(detail), userId);
    }

    @Transactional(readOnly = true)
    public List<PayrollAuditEventRow> findRunHistory(Long payrollRunId) {
        if (payrollRunId == null) {
            return List.of();
        }
        return toRows(repository.findByPayrollRunIdOrderByEventAtDescIdDesc(payrollRunId));
    }

    @Transactional(readOnly = true)
    public List<PayrollAuditEventRow> findPaymentBatchHistory(Long payrollPaymentBatchId) {
        if (payrollPaymentBatchId == null) {
            return List.of();
        }
        return toRows(repository.findByPayrollPaymentBatchIdOrderByEventAtDescIdDesc(payrollPaymentBatchId));
    }

    private List<PayrollAuditEventRow> toRows(List<PayrollRunEvent> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = events.stream()
                .map(PayrollRunEvent::getEventBy)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> usersById = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (first, ignored) -> first,
                                LinkedHashMap::new));

        Set<Long> payrollEmployeeIds = events.stream()
                .map(PayrollRunEvent::getPayrollEmployeeId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PayrollEmployee> employeesById = payrollEmployeeIds.isEmpty()
                ? Map.of()
                : payrollEmployeeRepository.findAllById(payrollEmployeeIds).stream()
                        .collect(Collectors.toMap(PayrollEmployee::getId, employee -> employee,
                                (first, ignored) -> first, LinkedHashMap::new));

        return events.stream().map(event -> {
            Long eventBy = event.getEventBy();
            Long payrollEmployeeId = event.getPayrollEmployeeId();

            User user = eventBy == null ? null : usersById.get(eventBy);
            PayrollEmployee employee = payrollEmployeeId == null
                    ? null
                    : employeesById.get(payrollEmployeeId);

            return new PayrollAuditEventRow(
                    event.getId(),
                    event.getEventAt(),
                    event.getEventType(),
                    eventBy,
                    user == null ? null : user.getName(),
                    user == null ? null : user.getUsername(),
                    payrollEmployeeId,
                    employee == null ? null : employee.getInsuranceNo(),
                    employee == null ? null : employee.getEmployeeNameEn(),
                    employee == null ? null : employee.getEmployeeNameKh(),
                    event.getPayrollPaymentBatchId(),
                    event.getFromStatus(),
                    event.getToStatus(),
                    event.getReason(),
                    event.getEventDetail());
        }).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
