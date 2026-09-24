package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable audit detail generated with a seniority payroll line. */
@Entity
@Table(name = "payroll_seniority_calculation", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollSeniorityCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_seniority_calculation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "payroll_employee_id", nullable = false)
    private PayrollEmployee payrollEmployee;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "payroll_seniority_rule_id", nullable = false)
    private PayrollSeniorityRule seniorityRule;

    @Column(name = "seniority_year", nullable = false)
    private Integer seniorityYear;

    @Column(name = "semester_no", nullable = false)
    private Integer semesterNo;

    @Column(name = "semester_start", nullable = false)
    private LocalDate semesterStart;

    @Column(name = "semester_end", nullable = false)
    private LocalDate semesterEnd;

    @Column(name = "eligibility_start", nullable = false)
    private LocalDate eligibilityStart;

    @Column(name = "eligibility_end", nullable = false)
    private LocalDate eligibilityEnd;

    @Column(name = "eligible_calendar_days", nullable = false)
    private Integer eligibleCalendarDays;

    @Column(name = "eligible_month_count", nullable = false)
    private Integer eligibleMonthCount;

    @Column(name = "total_eligible_earnings", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEligibleEarnings;

    @Column(name = "average_monthly_earnings", nullable = false, precision = 18, scale = 2)
    private BigDecimal averageMonthlyEarnings;

    @Column(name = "workday_divisor", nullable = false)
    private Integer workdayDivisor;

    @Column(name = "average_daily_earnings", nullable = false, precision = 18, scale = 6)
    private BigDecimal averageDailyEarnings;

    @Column(name = "entitlement_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal entitlementDays;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    public Integer insuranceNo() {
        return payrollEmployee == null ? null : payrollEmployee.getInsuranceNo();
    }

    public String employeeNameEn() {
        return payrollEmployee == null ? "" : payrollEmployee.getEmployeeNameEn();
    }

    public String employeeNameKh() {
        return payrollEmployee == null ? "" : payrollEmployee.getEmployeeNameKh();
    }

    public Long payrollRunId() {
        return payrollEmployee == null ? null : payrollEmployee.getPayrollRunId();
    }
}
