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
@Table(name = "emp_leave",schema  = "public")
@Getter
@Setter
public class EmployeeLeave  extends AbstractEntity{
	@Version
	@Column(name = "version")
	private Long version;

	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_leave_request_emp_leave_request_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_leave_request_emp_leave_request_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_leave_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id")
	    @NotNull(message = "Employee is required")
	    private Employee employee;
	    
	    @Column(name = "request_date", nullable = false)
	    @NotNull(message = "request date cannot be null or empty")
	    private LocalDate requestDate;
	    
	    @Column(name = "contact_number")
	    private String contractNumber;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "department_id")
	    @NotNull(message = "Department is required")
	    private Department department;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "position_id")
	    @NotNull(message = "Position is required")
	    private Positions positions;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_line_manager_id")
	    @NotNull(message = "Line manager is required")
	    private Employee lineManager;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "line_manager_position_id")
	    @NotNull(message = "Line manager position is required")
	    private Positions lineManagerPositions;
	    
	    @Column(name = "reason")
	    private String reason;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		   
	    @JoinColumn(name = "leave_status_id")
	    @NotNull(message = "Leave status is required")
	    private LeaveStatus leaveStatus;
	    

	    
	    @Column(name = "line_manager_check_date")
	    private LocalDate lineManagerCheckDate;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "line_manager_check_by_emp_id")
	    private Employee lineManagerChecked;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "line_manager_check_position_id")
	    private Positions lineManagerCheckedPositions;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "line_manager_check_status_id")
	    private LeaveStatus lineManagerCheckedStatus;
	    
	    @Column(name = "line_manager_check_comments")
	    private String lineManagerCheckComments;
	    
	    
	    @Column(name = "hr_verification_date")
	    private LocalDate hrVerificationDate;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "hr_verification_by_emp_id")
	    private Employee hrVerificationBy;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "hr_verification_position_id")
	    private Positions hrVerificationPosition;
	    
	    @Column(name = "hr_verification_comments")
	    private String hrVerificationComments;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "hr_verification_status_id")
	    private LeaveStatus hrVerificationStatus;
	    
	    @Column(name = "survey123_id")
	    private UUID  survey123ID;
	    
	 // === LEAVE SUMMARY FIELDS ===
	    @Column(name = "annual_leave_remaining", precision = 5, scale = 2)
	    private BigDecimal  annualLeaveAvailableBalance;

	    @Column(name = "sick_leave_used", precision = 5, scale = 2)
	    private BigDecimal  sickLeaveUsed;

	    @Column(name = "special_leave_used", precision = 5, scale = 2)
	    private BigDecimal  specialLeaveUsed;

	    @Column(name = "unpaid_leave_used", precision = 5, scale = 2)
	    private BigDecimal  unpaidLeaveUsed;

	    @Column(name = "paternitiy_leave_used", precision = 5, scale = 2)
	    private BigDecimal  paternityLeaveUsed;

	    @Column(name = "mapernity_leave_used", precision = 5, scale = 2)
	    private BigDecimal  maternityLeaveUsed;

	    @Column(name = "compensatory_leave_remaining", precision = 5, scale = 2)
	    private BigDecimal  compensatoryLeaveRemaining;
	    
	    
	    @OneToMany(mappedBy = "employeeLeave", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	    private List<EmployeeLeaveDetail> employeeLeaveDetails= new ArrayList<>();
	    
	    
	    @OneToMany(mappedBy = "employeeLeave", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	    private List<LeaveActionToken> leaveActionTokens= new ArrayList<>();
	    
	    @Transient
	    private List<Attachment> attachments = new ArrayList<>();
}
