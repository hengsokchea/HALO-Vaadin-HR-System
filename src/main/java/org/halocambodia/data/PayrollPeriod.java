package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.halocambodia.enums.PayrollPeriodStatus;
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
@Table(name = "payroll_period", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_period_id")
    private Long id;

    @Column(name = "payroll_year", nullable = false)
    private Integer payrollYear;

    @Column(name = "payroll_month", nullable = false)
    private Integer payrollMonth;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payroll_currency", nullable = false, length = 3)
    private String payrollCurrency = "USD";

    @Column(name = "usd_to_khr_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal usdToKhrRate;

    @Column(name = "nssf_usd_to_khr_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal nssfUsdToKhrRate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = PayrollPeriodStatus.OPEN.code();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public PayrollPeriodStatus statusEnum() {
        return PayrollPeriodStatus.from(status);
    }

    public void setStatus(PayrollPeriodStatus status) {
        this.status = java.util.Objects.requireNonNull(status, "status").code();
    }

    public String displayName() {
        return "%04d-%02d · %s".formatted(payrollYear, payrollMonth, status);
    }
}
