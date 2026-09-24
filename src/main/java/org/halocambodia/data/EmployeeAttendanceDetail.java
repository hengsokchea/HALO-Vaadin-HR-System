package org.halocambodia.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.halocambodia.data.enums.*;
import org.halocambodia.enums.AttendanceEntryStatus;
import org.halocambodia.enums.AttendanceQcStatus;

@Entity
@Table(name = "emp_attendance_detail",schema  = "public")
@Getter
@Setter
public class EmployeeAttendanceDetail  extends AbstractEntity{

	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_attendance_detail_emp_attendance_detail_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_attendance_detail_emp_attendance_detail_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "attendance_detail_id")
	    private Long id; 
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "attendance_emp_id",  nullable = false)
	    @NotNull(message = "Attendance Name is required")
	    private Employee employee;
	    
	    @Column(name = "attendance_date", nullable = false)
	    @NotNull(message = "Attendance date cannot be null or empty")
	    private LocalDate attendanceDate;
	    
	    
	    @Column(name = "first_in")
	    private LocalTime firstIn;
	    
	    @Column(name = "last_out")
	    private LocalTime lastOut;
	    
	    @Column(name = "break_minutes")
	    private Integer breakMinutes;
	    
	    @Column(name = "total_hours", precision = 5, scale = 2, insertable = false, updatable = false)
	    private BigDecimal totalHours;
	    
	    
	    @Column(name = "normal_hours", precision = 5, scale = 2)
	    private BigDecimal  normalHours;
	    
	    @Column(name = "overtime_hours", precision = 5, scale = 2, insertable = false, updatable = false)
	    private BigDecimal overtimeHours;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "leave_type_id")
	    @NotNull(message = "Leave Type is required")
	    private LeaveType leaveType;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "leave_sub_type_id")
	    private LeaveTypeSubType leaveTypeSubType;	
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "leave_duration", nullable = false)
	    @NotNull(message = "Leave duration is required")
	    private LeaveDuration leaveDuration;
	    
	    @Column(name = "number_of_day", insertable = false, updatable = false)
	    private Float numberOfDay;
	    
	    
	    @Column(name = "source")
	    private String source;
	    
	    @Column(name = "remark")
	    private String remark;
	    
		@Version
		@Column(name = "version")
		private Long version;
	    
		
		@Override
		public Long getId() {
		    return id;
		}
		
	    //@ManyToOne(fetch = FetchType.LAZY)
	    //@JoinColumn(name = "emp_attendance_id", nullable = false)
	    //@NotNull(message = "Attendance is required")
	    //private EmployeeAttendance attendance;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "emp_attendance_id")
	    private EmployeeAttendance attendance;
	    
	    
	    @Column(name = "hr_verification_date")
	    private LocalDate hrVerificationDate;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "hr_verification_by_emp_id")
	    private Employee hrVerificationBy;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "hr_verification_position_id")
	    private Positions hrVerificationPosition;

	    
	    
	    @OneToMany(mappedBy = "attendanceDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	    private List<EmployeeAttendanceDetailTeam> attendanceDetailTeams= new ArrayList<>();
	    
	    @OneToMany(mappedBy = "attendanceDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	    private List<EmployeeAttendanceDetailTimeLog> attendanceDetailTimeLogs= new ArrayList<>();
	    
	    public void calculateNumberOfDays() {
	        if ( leaveDuration != null) {
	            switch (leaveDuration) {
	                case FULL_DAY:
	                    this.numberOfDay = (float) 1;
	                    break;
	                case MORNING_ONLY:
	                case AFTERNOON_ONLY:
	                     this.numberOfDay = 0.5f;
	                    break;
	            }
	        }
	    }
	    
	    public void recalculateHours() {
	        calculateTotalHours();
	        calculateOvertimeHours();
	    }

	    private void calculateTotalHours() {
	        if (firstIn != null && lastOut != null) {
	            long minutes = Duration.between(firstIn, lastOut).toMinutes();

	            // Subtract break minutes if provided
	            if (breakMinutes != null && breakMinutes > 0) {
	                minutes -= breakMinutes;
	            }

	            // No negative / zero duration
	            if (minutes <= 0) {
	                this.totalHours = null;
	                return;
	            }

	            this.totalHours = BigDecimal.valueOf(minutes)
	                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
	        } else {
	            this.totalHours = null;
	        }
	    }

	    private void calculateOvertimeHours() {
	        if (totalHours != null && normalHours != null) {
	            BigDecimal overtime = totalHours.subtract(normalHours);
	            this.overtimeHours = overtime.compareTo(BigDecimal.ZERO) > 0
	                    ? overtime
	                    : BigDecimal.ZERO;
	        } else {
	            this.overtimeHours = BigDecimal.ZERO; // or BigDecimal.ZERO if you prefer
	        }
	    }


	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "reported_by_emp_id", nullable = false)
	    @NotNull(message = "Reported by employee is required")
	    private Employee reportedByEmployee;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "reported_by_position_id")
	    private Positions reportedByPosition;

	    @Column(name = "reported_date", nullable = false)
	    @NotNull(message = "Reported date is required")
	    private LocalDate reportedDate;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "report_location_id", nullable = false)
	    @NotNull(message = "Report Location is required")
	    private Branch reportLocation;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "qc_by_emp_id")
	    private Employee qcByEmployee;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "qc_by_position_id")
	    private Positions qcByPosition;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "qc_location_id")
	    private Branch qcLocation;

	    @Column(name = "qc_date")
	    private LocalDate qcDate;
	    
	    @Column(name = "survey123_id",updatable = false,nullable = false)
	    private UUID survey123Id=UUID.randomUUID();
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "data_entry_by_emp_id")
	    private Employee dataEntryBy;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "data_entry_position_id")
	    private Positions dataEntryPosition;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "data_entry_location_id")
	    private Branch dataEntryLocation;

	    @Column(name = "data_entry_date")
	    private LocalDate dataEntryDate;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "entry_status", nullable = false)
	    private AttendanceEntryStatus entryStatus = AttendanceEntryStatus.DRAFT;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "qc_status", nullable = false)
	    private AttendanceQcStatus qcStatus = AttendanceQcStatus.PENDING;

	    @Column(name = "qc_return_reason", length = 1000)
	    private String qcReturnReason;

	    @Column(name = "qc_return_date")
	    private LocalDate qcReturnDate;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "qc_return_by_emp_id")
	    private Employee qcReturnBy;

}
