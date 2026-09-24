package org.halocambodia.data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.halocambodia.enums.PayrollInstallmentType;
import org.halocambodia.enums.PayrollPaymentBatchStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "payroll_payment_batch", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollPaymentBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_payment_batch_id")
    private Long id;

    @Column(name = "payroll_period_id", nullable = false)
    private Long payrollPeriodId;

    /** Populated for FINAL_SETTLEMENT and null for FIRST_INSTALLMENT. */
    @Column(name = "payroll_run_id")
    private Long payrollRunId;

    @Column(name = "installment_type", nullable = false, length = 30)
    private String installmentType;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = PayrollPaymentBatchStatus.DRAFT.code();

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 150)
    private String paymentReference;

    @Column(name = "bank_reference", length = 150)
    private String bankReference;

    @Column(name = "payment_file_name", length = 255)
    private String paymentFileName;

    @Column(name = "payment_exported_at")
    private OffsetDateTime paymentExportedAt;

    @Column(name = "payment_exported_by")
    private Long paymentExportedBy;

    @Column(name = "payment_confirmed_at")
    private OffsetDateTime paymentConfirmedAt;

    @Column(name = "payment_confirmed_by")
    private Long paymentConfirmedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    public PayrollInstallmentType installmentTypeEnum() {
        return PayrollInstallmentType.from(installmentType);
    }

    public void setInstallmentType(PayrollInstallmentType installmentType) {
        this.installmentType = java.util.Objects.requireNonNull(installmentType, "installmentType").code();
    }

    public PayrollPaymentBatchStatus statusEnum() {
        return PayrollPaymentBatchStatus.from(status);
    }

    public void setStatus(PayrollPaymentBatchStatus status) {
        this.status = java.util.Objects.requireNonNull(status, "status").code();
    }

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
