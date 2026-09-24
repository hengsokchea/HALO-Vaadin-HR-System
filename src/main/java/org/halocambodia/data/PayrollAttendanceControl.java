package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.halocambodia.enums.PayrollAttendanceControlStatus;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "payroll_attendance_control",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_payroll_attendance_control_period",
                columnNames = "payroll_period_id"))
@Getter
@Setter
@NoArgsConstructor
public class PayrollAttendanceControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_attendance_control_id")
    private Long payrollAttendanceControlId;

    @Column(name = "payroll_period_id", nullable = false)
    private Long payrollPeriodId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = PayrollAttendanceControlStatus.DRAFT.code();

    @Column(name = "roster_employee_count", nullable = false)
    private Long rosterEmployeeCount = 0L;

    @Column(name = "roster_day_count", nullable = false)
    private Long rosterDayCount = 0L;

    @Column(name = "attendance_record_count", nullable = false)
    private Long attendanceRecordCount = 0L;

    @Column(name = "roster_fallback_day_count", nullable = false)
    private Long rosterFallbackDayCount = 0L;

    @Column(name = "unverified_attendance_count", nullable = false)
    private Long unverifiedAttendanceCount = 0L;

    @Column(name = "duplicate_attendance_count", nullable = false)
    private Long duplicateAttendanceCount = 0L;

    @Column(name = "overtime_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "source_last_modified_at")
    private OffsetDateTime sourceLastModifiedAt;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;

    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "attendance_cutoff_date")
    private LocalDate attendanceCutoffDate;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    public PayrollAttendanceControlStatus statusEnum() {
        return PayrollAttendanceControlStatus.from(status);
    }

    public void setStatus(PayrollAttendanceControlStatus status) {
        this.status = java.util.Objects.requireNonNull(status, "status").code();
    }

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;
}
