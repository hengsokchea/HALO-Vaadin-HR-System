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
@Table(name = "payroll_nssf_wage_band", schema = "public")
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
public class PayrollNssfWageBand extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_nssf_wage_band_id")
    private Long id;

    @Column(name = "payroll_nssf_config_id", nullable = false)
    private Long payrollNssfConfigId;

    @Column(name = "band_order", nullable = false)
    private Integer bandOrder;

    @Column(name = "max_salary", precision = 18, scale = 2)
    private BigDecimal maxSalary;

    @Column(name = "contributory_wage", nullable = false, precision = 18, scale = 2)
    private BigDecimal contributoryWage;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public Long getId() {
        return id;
    }
}
