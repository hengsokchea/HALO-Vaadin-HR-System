package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "emp_leave_balance",schema  = "public")
@Getter
@Setter
public class EmployeeLeaveBalance  extends AbstractEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_leave_balance_emp_leave_balance_id_seq")
	    // The initial value is to account for data.sql demo data ids
	@SequenceGenerator(name = "public.emp_leave_balance_emp_leave_balance_id_seq", initialValue = 1,allocationSize = 1)
	@Column(name = "emp_leave_balance_id")
	private Long id; 
	
	@Override
	public Long getId() {
	    return id;
	}
	 
	@ManyToOne(fetch = FetchType.LAZY)		
	@JoinColumn(name = "emp_id")
	@NotNull(message = "Employee is required")
	private Employee employee;
	    
	@ManyToOne(fetch = FetchType.LAZY)		
	@JoinColumn(name = "leave_type_id")
	@NotNull(message = "Leave Type is required")
	private LeaveType leaveType;
	    
	@Column(name = "employment_start_date", nullable = false)
	@NotNull(message = "Start date cannot be null or empty")
	private LocalDate employmentStartDate;
	    
	@Column(name = "service_years", nullable = false)
	@NotNull(message = "Service years cannot be null or empty")
	private Integer serviceYears;
	    
	@Column(name = "service_months", nullable = false)
	@NotNull(message = "Service months cannot be null or empty")
	private Integer service_months;
	    
	@Column(name = "entitled_days", precision = 5, scale = 2)
	private BigDecimal  entitledDays;
	    
	@Column(name = "carried_over_days", precision = 5, scale = 2)
	private BigDecimal  carriedOverDays;
	    
	@Column(name = "additional_days", precision = 5, scale = 2)
	private BigDecimal  additionalDays;
	    
	@Column(name = "taken_days", precision = 5, scale = 2, insertable=false, updatable=false)
	private BigDecimal  takenDays;
	    
	@Column(name = "total_days", precision = 5, scale = 2,insertable=false, updatable=false)
	private BigDecimal  totalDays;
	
	@Column(name = "remaining_days_calc", precision = 5, scale = 2,insertable=false, updatable=false)
	private BigDecimal  remainingDaysCalc;
	    
	@Column(name = "year", nullable = false)
	@NotNull(message = "Year cannot be null or empty")
	private Integer year;
	    	    
	@Column(name = "effective_date", nullable = false)
	@NotNull(message = "Effective date cannot be null or empty")
	private LocalDate effectiveDate;
	    
	@Column(name = "expiry_date", nullable = false)
	@NotNull(message = "Expiry date cannot be null or empty")
	private LocalDate expiryDate;

	@Column(name = "note")
	private String note;
	
	@Column(name = "available_days", precision = 5, scale = 2 ,insertable=false, updatable=false)
	private BigDecimal availableDays; // Available balance (remaining - pending requests)

	@Column(name = "pending_days", precision = 5, scale = 2,insertable=false, updatable=false)
	private BigDecimal pendingDays; // Days pending approval
}
