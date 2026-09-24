package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.halocambodia.data.enums.*;

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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "emp_roster",schema  = "public")
@Getter
@Setter
public class EmployeeRoster extends AbstractEntity {
	
   
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.roster_roster_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.roster_roster_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "roster_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "shift_cycle_id",nullable = false)
	    @NotNull(message = "Shift Cycle be null or empty. Please select only one.")
	    private ShiftCycle shiftCycle;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id",nullable = false)
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "holiday_id",nullable = false)
	    @NotNull(message = "Holiday cannot be null or empty. Please select only one.")
	    private Holiday holiday;
	    
	    
	    @Column(name = "roster_date", nullable = false)
	    @NotNull(message = "Roster date cannot be null or empty")
	    private LocalDate rosterDate;
	    
	    @Column(name = "start_time")
	    private LocalTime startTime;

	    @Column(name = "end_time")
	    private LocalTime endTime;

	    @Column(name = "break_minutes")
	    private Integer breakMinutes;
	    
	    @Column(name = "total_working_hour", insertable = false, updatable = false)
	    private BigDecimal totalWorkingHour;

	
	    @Column(name = "remark", columnDefinition = "text")
	    private String remark;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_personnel_allocation_id")	    
	    private EmployeeAllocation employeeAllocation;
	    
	    @Column(name = "source_type", length = 20)
	    private String sourceType = "AUTO_GENERATED";
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_leave_detail_id")
	    private EmployeeLeaveDetail employeeLeaveDetail;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "leave_type_id")
	    private LeaveType leaveType;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "leave_type_sub_type_id")
	    private LeaveTypeSubType leaveTypeSubType ;
	    
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "leave_duration", nullable = false)
	    private LeaveDuration leaveDuration;
	    


}
