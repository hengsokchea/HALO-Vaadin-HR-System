package org.halocambodia.data;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only audit record for payroll workflow and payment events.
 *
 * Database UPDATE/DELETE is also blocked by the migration trigger, so these
 * records remain immutable even outside Hibernate.
 */
@Entity
@Immutable
@Table(name = "payroll_run_event", schema = "public")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollRunEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_run_event_id")
    private Long id;

    @Column(name = "payroll_period_id", nullable = false, updatable = false)
    private Long payrollPeriodId;

    @Column(name = "payroll_run_id", updatable = false)
    private Long payrollRunId;

    @Column(name = "payroll_employee_id", updatable = false)
    private Long payrollEmployeeId;

    @Column(name = "payroll_payment_batch_id", updatable = false)
    private Long payrollPaymentBatchId;

    /** Direct link for employee-payment events such as payslip email delivery. */
    @Column(name = "payroll_employee_payment_id", updatable = false)
    private Long payrollEmployeePaymentId;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @Column(name = "from_status", length = 20, updatable = false)
    private String fromStatus;

    @Column(name = "to_status", length = 20, updatable = false)
    private String toStatus;

    @Column(name = "reason", columnDefinition = "text", updatable = false)
    private String reason;

    @Column(name = "event_detail", columnDefinition = "text", updatable = false)
    private String eventDetail;

    @Column(name = "event_at", nullable = false, updatable = false)
    private OffsetDateTime eventAt;

    @Column(name = "event_by", nullable = false, updatable = false)
    private Long eventBy;

    public PayrollRunEvent(
            Long payrollPeriodId,
            Long payrollRunId,
            Long payrollEmployeeId,
            Long payrollPaymentBatchId,
            PayrollRunEventType eventType,
            String fromStatus,
            String toStatus,
            String reason,
            String eventDetail,
            OffsetDateTime eventAt,
            Long eventBy) {
        this(
                payrollPeriodId,
                payrollRunId,
                payrollEmployeeId,
                payrollPaymentBatchId,
                null,
                eventType,
                fromStatus,
                toStatus,
                reason,
                eventDetail,
                eventAt,
                eventBy);
    }

    public PayrollRunEvent(
            Long payrollPeriodId,
            Long payrollRunId,
            Long payrollEmployeeId,
            Long payrollPaymentBatchId,
            Long payrollEmployeePaymentId,
            PayrollRunEventType eventType,
            String fromStatus,
            String toStatus,
            String reason,
            String eventDetail,
            OffsetDateTime eventAt,
            Long eventBy) {
        this.payrollPeriodId = payrollPeriodId;
        this.payrollRunId = payrollRunId;
        this.payrollEmployeeId = payrollEmployeeId;
        this.payrollPaymentBatchId = payrollPaymentBatchId;
        this.payrollEmployeePaymentId = payrollEmployeePaymentId;
        this.eventType = eventType.name();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.eventDetail = eventDetail;
        this.eventAt = eventAt;
        this.eventBy = eventBy;
    }
}
