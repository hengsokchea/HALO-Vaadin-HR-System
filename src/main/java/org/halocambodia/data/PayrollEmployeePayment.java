package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
@Table(name = "payroll_employee_payment", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollEmployeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_employee_payment_id")
    private Long id;

    @Column(name = "payroll_payment_batch_id", nullable = false)
    private Long payrollPaymentBatchId;

    @Column(name = "emp_id", nullable = false)
    private Long employeeId;

    @Column(name = "payroll_employee_id")
    private Long payrollEmployeeId;

    @Column(name = "insurance_no_snapshot")
    private Integer insuranceNoSnapshot;

    @Column(name = "employee_name_en_snapshot", length = 250)
    private String employeeNameEnSnapshot;

    @Column(name = "employee_name_kh_snapshot", length = 250)
    private String employeeNameKhSnapshot;

    @Column(name = "bank_name_snapshot", length = 200)
    private String bankNameSnapshot;

    @Column(name = "bank_account_snapshot", length = 100)
    private String bankAccountSnapshot;

    @Column(name = "payment_frequency", nullable = false, length = 20)
    private String paymentFrequency;

    @Column(name = "first_payment_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal firstPaymentPercent = BigDecimal.ZERO;

    @Column(name = "basic_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "full_net_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal fullNetAmount = BigDecimal.ZERO;

    @Column(name = "previous_paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal previousPaidAmount = BigDecimal.ZERO;

    @Column(name = "payment_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paymentAmount = BigDecimal.ZERO;

    @Column(name = "carry_forward_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal carryForwardAmount = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
