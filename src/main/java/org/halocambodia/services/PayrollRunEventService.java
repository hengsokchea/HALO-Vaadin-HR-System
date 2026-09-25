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
import org.halocambodia.data.PayrollEmployeePayment;
import org.halocambodia.data.PayrollEmployeePaymentRepository;
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
    private final PayrollEmployeePaymentRepository employeePaymentRepository;

    public PayrollRunEventService(
            PayrollRunEventRepository repository,
            UserRepository userRepository,
            PayrollEmployeeRepository payrollEmployeeRepository,
            PayrollEmployeePaymentRepository employeePaymentRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.payrollEmployeeRepository = payrollEmployeeRepository;
        this.employeePaymentRepository = employeePaymentRepository;
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
     * Records one append-only audit event linked directly to an employee payment.
     * New payslip-email events use this structured link instead of encoding the
     * payment ID in free-text reason data.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordWithEmployeePayment(
            Long payrollPeriodId,
            Long payrollRunId,
            Long payrollEmployeeId,
            Long payrollPaymentBatchId,
            Long payrollEmployeePaymentId,
            PayrollRunEventType eventType,
            String fromStatus,
            String toStatus,
            String reason,
            String detail,
            Long userId) {
        if (payrollPeriodId == null) {
            throw new IllegalArgumentException("Payroll period is required for audit history.");
        }
        if (payrollEmployeePaymentId == null) {
            throw new IllegalArgumentException("Employee payment is required for audit history.");
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
                payrollEmployeePaymentId,
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

        Set<Long> employeePaymentIds = events.stream()
                .map(PayrollRunEventService::employeePaymentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PayrollEmployeePayment> paymentsById = employeePaymentIds.isEmpty()
                ? Map.of()
                : employeePaymentRepository.findAllById(employeePaymentIds).stream()
                        .collect(Collectors.toMap(PayrollEmployeePayment::getId, payment -> payment,
                                (first, ignored) -> first, LinkedHashMap::new));

        return events.stream().map(event -> {
            Long eventBy = event.getEventBy();
            Long payrollEmployeeId = event.getPayrollEmployeeId();

            User user = eventBy == null ? null : usersById.get(eventBy);
            PayrollEmployee employee = payrollEmployeeId == null
                    ? null
                    : employeesById.get(payrollEmployeeId);
            PayrollEmployeePayment payment = paymentsById.get(employeePaymentId(event));

            Integer insuranceNo = employee != null
                    ? employee.getInsuranceNo()
                    : payment == null ? null : payment.getInsuranceNoSnapshot();
            String employeeNameEn = employee != null
                    ? employee.getEmployeeNameEn()
                    : payment == null ? null : payment.getEmployeeNameEnSnapshot();
            String employeeNameKh = employee != null
                    ? employee.getEmployeeNameKh()
                    : payment == null ? null : payment.getEmployeeNameKhSnapshot();

            return new PayrollAuditEventRow(
                    event.getId(),
                    event.getEventAt(),
                    event.getEventType(),
                    eventBy,
                    user == null ? null : user.getName(),
                    user == null ? null : user.getUsername(),
                    payrollEmployeeId,
                    insuranceNo,
                    employeeNameEn,
                    employeeNameKh,
                    event.getPayrollPaymentBatchId(),
                    event.getFromStatus(),
                    event.getToStatus(),
                    event.getReason(),
                    event.getEventDetail());
        }).toList();
    }

    private static Long employeePaymentId(PayrollRunEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getPayrollEmployeePaymentId() != null) {
            return event.getPayrollEmployeePaymentId();
        }
        return legacyPaymentIdFromReason(event.getReason());
    }

    /** Compatibility only for email-audit rows created before the structured column existed. */
    private static Long legacyPaymentIdFromReason(String reason) {
        if (reason == null || !reason.startsWith("Payment ID: ")) {
            return null;
        }
        int start = "Payment ID: ".length();
        int end = reason.indexOf(" · ", start);
        String value = (end < 0 ? reason.substring(start) : reason.substring(start, end)).trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
