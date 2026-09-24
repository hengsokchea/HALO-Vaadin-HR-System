package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payroll_attendance_day_snapshot", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class PayrollAttendanceDaySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_attendance_day_snapshot_id")
    private Long id;

    @Column(name = "payroll_employee_id", nullable = false)
    private Long payrollEmployeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "leave_type_id")
    private Long leaveTypeId;

    @Column(name = "payroll_policy_rule_id")
    private Long payrollPolicyRuleId;

    @Column(name = "holiday_id")
    private Long holidayId;

    @Column(name = "day_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal dayValue = BigDecimal.ONE;

    @Column(name = "overtime_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "normal_working_hours", precision = 8, scale = 2)
    private BigDecimal normalWorkingHours;

    @Column(name = "scheduled_workday", nullable = false)
    private boolean scheduledWorkday;

    @Column(name = "leave_period_start")
    private LocalDate leavePeriodStart;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
