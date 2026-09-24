package org.halocambodia.data;

import java.math.BigDecimal;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payroll_tax_config", schema = "public")
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
public class PayrollTaxConfig extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_tax_config_id")
    private Long id;

    @Column(name = "tax_year", nullable = false, unique = true)
    private Integer taxYear;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "KHR";

    @Column(name = "dependent_allowance", nullable = false, precision = 18, scale = 2)
    private BigDecimal dependentAllowance = BigDecimal.ZERO;

    @Column(name = "non_resident_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal nonResidentRate = new BigDecimal("20");

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "source_reference", columnDefinition = "text")
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
}
