package org.halocambodia.data;

import java.time.OffsetDateTime;

import org.halocambodia.enums.PayrollRunStatus;
import org.halocambodia.enums.PayrollRunType;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payroll_run", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_run_id")
    private Long id;

    @Column(name = "payroll_period_id", nullable = false)
    private Long payrollPeriodId;

    @Column(name = "run_number", nullable = false)
    private Integer runNumber = 1;

    @Column(name = "run_type", nullable = false, length = 20)
    private String runType = PayrollRunType.REGULAR.code();

    @Column(name = "status", nullable = false, length = 20)
    private String status = PayrollRunStatus.DRAFT.code();

    @Column(name = "correction_mode", nullable = false)
    private Boolean correctionMode = false;

    @Column(name = "calculated_at")
    private OffsetDateTime calculatedAt;

    @Column(name = "calculated_by")
    private Long calculatedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public PayrollRunType runTypeEnum() {
        return PayrollRunType.from(runType);
    }

    public void setRunType(PayrollRunType runType) {
        this.runType = java.util.Objects.requireNonNull(runType, "runType").code();
    }

    public PayrollRunStatus statusEnum() {
        return PayrollRunStatus.from(status);
    }

    public void setStatus(PayrollRunStatus status) {
        this.status = java.util.Objects.requireNonNull(status, "status").code();
    }

    public String displayName() {
        return "Run %d · %s · %s".formatted(runNumber, runType, status);
    }
}
