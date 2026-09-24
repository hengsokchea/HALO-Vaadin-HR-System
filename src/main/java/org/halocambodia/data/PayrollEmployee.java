package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.halocambodia.enums.PayrollEmployeeStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payroll_employee", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_employee_id")
    private Long id;

    @Column(name = "payroll_run_id", nullable = false)
    private Long payrollRunId;

    @Column(name = "emp_id", nullable = false)
    private Long employeeId;

    @Column(name = "insurance_no", nullable = false)
    private Integer insuranceNo;

    @Column(name = "employee_name_en", length = 250)
    private String employeeNameEn;

    @Column(name = "employee_name_kh", length = 250)
    private String employeeNameKh;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "bank_account", length = 100)
    private String bankAccount;

    @Column(name = "spouse_count", nullable = false)
    private Integer spouseCount = 0;

    @Column(name = "tax_dependent_count", nullable = false)
    private Integer taxDependentCount = 0;

    @Column(name = "salary_currency", nullable = false, length = 3)
    private String salaryCurrency = "USD";

    @Column(name = "basic_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "total_earnings", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "salary_tax", nullable = false, precision = 14, scale = 2)
    private BigDecimal salaryTax = BigDecimal.ZERO;

    @Column(name = "employee_contribution", nullable = false, precision = 14, scale = 2)
    private BigDecimal employeeContribution = BigDecimal.ZERO;

    @Column(name = "employer_contribution", nullable = false, precision = 14, scale = 2)
    private BigDecimal employerContribution = BigDecimal.ZERO;

    // Read-only Payroll Results breakdown populated from payroll_employee_item.
    @Transient
    private BigDecimal nssfHealthEmployee = BigDecimal.ZERO;

    @Transient
    private BigDecimal nssfHealthEmployer = BigDecimal.ZERO;

    @Transient
    private BigDecimal nssfRiskEmployee = BigDecimal.ZERO;

    @Transient
    private BigDecimal nssfRiskEmployer = BigDecimal.ZERO;

    @Transient
    private BigDecimal nssfPensionEmployee = BigDecimal.ZERO;

    @Transient
    private BigDecimal nssfPensionEmployer = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(name = "payroll_status", nullable = false, length = 20)
    private String payrollStatus = PayrollEmployeeStatus.INCLUDED.code();

    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

    @Column(name = "correction_required", nullable = false)
    private Boolean correctionRequired = false;

    @Column(name = "correction_reason", columnDefinition = "text")
    private String correctionReason;

    @Column(name = "correction_returned_at")
    private OffsetDateTime correctionReturnedAt;

    @Column(name = "correction_returned_by")
    private Long correctionReturnedBy;

    @Column(name = "correction_resolved_at")
    private OffsetDateTime correctionResolvedAt;

    @Column(name = "correction_resolved_by")
    private Long correctionResolvedBy;

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
    private Integer version = 0;

    public PayrollEmployeeStatus payrollStatusEnum() {
        return PayrollEmployeeStatus.from(payrollStatus);
    }

    public void setPayrollStatus(PayrollEmployeeStatus payrollStatus) {
        this.payrollStatus = java.util.Objects.requireNonNull(payrollStatus, "payrollStatus").code();
    }

    public String displayName() {
        return insuranceNo + " · " + (employeeNameEn == null ? "" : employeeNameEn);
    }
}
