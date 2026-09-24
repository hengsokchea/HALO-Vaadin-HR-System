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

@Entity
@Table(name = "payroll_nssf_config", schema = "public")
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
public class PayrollNssfConfig extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_nssf_config_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "eligible_contract_type_id")
    private ContractType eligibleContractType;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "KHR";

    @Column(name = "health_employee_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal healthEmployeeRate = BigDecimal.ZERO;

    @Column(name = "health_employer_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal healthEmployerRate = BigDecimal.ZERO;

    @Column(name = "risk_employee_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal riskEmployeeRate = BigDecimal.ZERO;

    @Column(name = "risk_employer_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal riskEmployerRate = BigDecimal.ZERO;

    @Column(name = "pension_employee_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal pensionEmployeeRate = BigDecimal.ZERO;

    @Column(name = "pension_employer_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal pensionEmployerRate = BigDecimal.ZERO;

    @Column(name = "pension_min_wage", nullable = false, precision = 18, scale = 2)
    private BigDecimal pensionMinWage = BigDecimal.ZERO;

    @Column(name = "pension_max_wage", nullable = false, precision = 18, scale = 2)
    private BigDecimal pensionMaxWage;

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
        return eligibleContractType == null
                ? "Not selected | មិនបានជ្រើសរើស"
                : eligibleContractType.getContractTypeName();
    }
}
