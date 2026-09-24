package org.halocambodia.data;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payroll_component", schema = "public")
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
public class PayrollComponent extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_component_id")
    private Long id;

    @Column(name = "component_code", nullable = false, unique = true, length = 50)
    private String componentCode;

    @Column(name = "component_name_en", nullable = false, length = 200)
    private String componentNameEn;

    @Column(name = "component_name_kh", length = 200)
    private String componentNameKh;

    @Column(name = "component_type", nullable = false, length = 30)
    private String componentType;

    @Column(name = "calculation_method", nullable = false, length = 20)
    private String calculationMethod = "MANUAL";

    @Column(name = "allow_manual_entry", nullable = false)
    private boolean allowManualEntry;

    @Column(name = "taxable", nullable = false)
    private boolean taxable;

    @Column(name = "subject_to_nssf", nullable = false)
    private boolean subjectToNssf;

    @Column(name = "subject_to_seniority", nullable = false)
    private boolean subjectToSeniority;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "gl_account_code", length = 50)
    private String glAccountCode;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    /** Read-only summary populated by PayrollComponentService for the grid. */
    @Transient
    private String usedByPayrollRules = "-";

    @Override
    public Long getId() {
        return id;
    }

    public String displayName() {
        return componentCode + " · " + componentNameEn;
    }
}
