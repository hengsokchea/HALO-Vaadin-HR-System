package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Effective-dated company payroll payment rule.
 * <p>
 * New rules are company-wide (employee is null). For MONTHLY, a null Shift means
 * all staff are monthly; a selected Shift is the SEMI_MONTHLY exception. For
 * SEMI_MONTHLY, Shift is null/disabled and all staff are semi-monthly. The legacy
 * employee column is retained only for database/history compatibility and is ignored
 * by current payroll payment resolution.
 */
@Entity
@Table(name = "payroll_payment_setting", schema = "public")
@AssociationOverrides({
        @AssociationOverride(
                name = "userCreated",
                joinColumns = @JoinColumn(name = "created_by", nullable = false, updatable = false)),
        @AssociationOverride(
                name = "userUpdated",
                joinColumns = @JoinColumn(name = "updated_by", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
public class PayrollPaymentSetting extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_payment_setting_id")
    private Long id;

    /** Legacy database/history field. Current payroll resolution ignores it. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "emp_id")
    private Employee employee;

    /** Optional SEMI_MONTHLY exception Shift when paymentFrequency is MONTHLY. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(name = "payment_frequency", nullable = false, length = 20)
    private String paymentFrequency = "MONTHLY";

    @Column(name = "first_payment_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal firstPaymentPercent = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public Long getId() {
        return id;
    }

    public String scopeLabel() {
        if (employee != null) {
            return employee.getInsuranceNo() + " · " + employee.getNameEn();
        }
        if ("SEMI_MONTHLY".equals(paymentFrequency)) {
            return "All Staff Semi-Monthly | បុគ្គលិកទាំងអស់បើកពីរដង";
        }
        if (shift != null) {
            return "Monthly + " + shift.getShiftName() + " Semi-Monthly";
        }
        return "All Staff Monthly | បុគ្គលិកទាំងអស់បើកប្រចាំខែ";
    }
}
