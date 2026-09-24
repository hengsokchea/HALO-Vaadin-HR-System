package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payroll_attendance_summary", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollAttendanceSummary {

    @Id
    @Column(name = "payroll_employee_id")
    private Long payrollEmployeeId;

    @Column(name = "scheduled_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal scheduledDays = BigDecimal.ZERO;

    @Column(name = "worked_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal workedDays = BigDecimal.ZERO;

    @Column(name = "holiday_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal holidayDays = BigDecimal.ZERO;

    @Column(name = "absent_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal absentDays = BigDecimal.ZERO;

    @Column(name = "annual_leave_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal annualLeaveDays = BigDecimal.ZERO;

    @Column(name = "special_leave_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal specialLeaveDays = BigDecimal.ZERO;

    @Column(name = "sick_leave_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal sickLeaveDays = BigDecimal.ZERO;

    @Column(name = "maternity_leave_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal maternityLeaveDays = BigDecimal.ZERO;

    @Column(name = "other_paid_leave_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal otherPaidLeaveDays = BigDecimal.ZERO;

    @Column(name = "unpaid_leave_days", nullable = false, precision = 8, scale = 2)
    private BigDecimal unpaidLeaveDays = BigDecimal.ZERO;

    @Column(name = "overtime_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "annual_leave_remaining", nullable = false, precision = 8, scale = 2)
    private BigDecimal annualLeaveRemaining = BigDecimal.ZERO;

    @Column(name = "overused_annual_leave", nullable = false, precision = 8, scale = 2)
    private BigDecimal overusedAnnualLeave = BigDecimal.ZERO;

    @Column(name = "special_leave_remaining", nullable = false, precision = 8, scale = 2)
    private BigDecimal specialLeaveRemaining = BigDecimal.ZERO;

    @Column(name = "overused_special_leave", nullable = false, precision = 8, scale = 2)
    private BigDecimal overusedSpecialLeave = BigDecimal.ZERO;

    @Column(name = "source_start_date", nullable = false)
    private LocalDate sourceStartDate;

    @Column(name = "source_end_date", nullable = false)
    private LocalDate sourceEndDate;

    @UpdateTimestamp
    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;
}
