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
@Table(name = "payroll_policy_rule", schema = "public")
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
public class PayrollPolicyRule extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_policy_rule_id")
    private Long id;

    @Column(name = "rule_year", nullable = false)
    private Integer ruleYear;

    @Column(name = "rule_code", nullable = false, length = 30)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 200)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 30)
    private String ruleType;

    @Column(name = "rate_amount", precision = 18, scale = 4)
    private BigDecimal rateAmount;

    @Column(name = "rate_currency", length = 3)
    private String rateCurrency;

    @Column(name = "rate_unit", length = 20)
    private String rateUnit;

    @Column(name = "multiplier", precision = 10, scale = 4)
    private BigDecimal multiplier;

    /**
     * Monthly divisor used when this rule converts basic salary to a daily/hourly rate.
     * This is independent from payroll_seniority_rule.workday_divisor.
     */
    @Column(name = "workday_divisor", nullable = false)
    private Integer workdayDivisor = 22;

    @Column(name = "pay_percentage", precision = 7, scale = 4)
    private BigDecimal payPercentage;

    @Column(name = "entitlement_days", precision = 8, scale = 2)
    private BigDecimal entitlementDays;

    @Column(name = "service_years_per_extra_day")
    private Integer serviceYearsPerExtraDay;

    @Column(name = "initial_full_pay_months")
    private Integer initialFullPayMonths;

    @Column(name = "max_paid_months")
    private Integer maxPaidMonths;

    @Column(name = "calculation_source", nullable = false, length = 20)
    private String calculationSource = "MANUAL";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "payroll_component_id")
    private Long payrollComponentId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public Long getId() {
        return id;
    }
}
