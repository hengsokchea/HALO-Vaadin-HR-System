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
@Table(name = "payroll_tax_bracket", schema = "public")
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
public class PayrollTaxBracket extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_tax_bracket_id")
    private Long id;

    @Column(name = "payroll_tax_config_id", nullable = false)
    private Long payrollTaxConfigId;

    @Column(name = "bracket_order", nullable = false)
    private Integer bracketOrder;

    @Column(name = "min_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 18, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "deduction_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public Long getId() {
        return id;
    }
}
