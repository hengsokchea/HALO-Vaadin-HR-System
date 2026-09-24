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

/** Effective-dated Cambodia seniority-payment configuration. */
@Entity
@Table(name = "payroll_seniority_rule", schema = "public")
@AssociationOverrides({
        @AssociationOverride(
                name = "userCreated",
                joinColumns = @JoinColumn(name = "created_by", nullable = true, updatable = false)),
        @AssociationOverride(
                name = "userUpdated",
                joinColumns = @JoinColumn(name = "updated_by", nullable = true))
})
@Getter
@Setter
@NoArgsConstructor
public class PayrollSeniorityRule extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_seniority_rule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "eligible_contract_type_id", nullable = false)
    private ContractType eligibleContractType;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "first_payment_month", nullable = false)
    private Integer firstPaymentMonth = 6;

    @Column(name = "second_payment_month", nullable = false)
    private Integer secondPaymentMonth = 12;

    @Column(name = "days_per_payment", nullable = false, precision = 8, scale = 2)
    private BigDecimal daysPerPayment = new BigDecimal("7.50");

    @Column(name = "minimum_eligible_days", nullable = false)
    private Integer minimumEligibleDays = 21;

    @Column(name = "workday_divisor", nullable = false)
    private Integer workdayDivisor = 26;

    @Column(name = "require_employed_at_semester_end", nullable = false)
    private boolean requireEmployedAtSemesterEnd = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "source_reference", length = 1000)
    private String sourceReference;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public Long getId() {
        return id;
    }

    public String contractTypeName() {
        return eligibleContractType == null ? "" : eligibleContractType.getContractTypeName();
    }
}
