package org.halocambodia.data;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "emp_goal_setting",schema  = "public")
@Getter
@Setter
public class GoalSetting extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_goal_setting_emp_goal_setting_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_goal_setting_emp_goal_setting_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_goal_setting_id")
	    private Long id;
	 
		@Override
		public Long getId() {
		    return id;
		}
		
		
	    @Column(name = "from_date", nullable = false)
	    @NotNull(message = "From Date cannot be null or empty")
	    private LocalDate fromDate;
	    
	    @Column(name = "to_date", nullable = false)
	    @NotNull(message = "To Date cannot be null or empty")
	    private LocalDate toDate;

	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "reported_by_emp_id",nullable = false)
	    @NotNull(message = "Reported by cannot be null or empty. Please select only one.")
	    private HREmployeeData reportedBy;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "reported_by_position_id",nullable = false)
	    @NotNull(message = "Reported Positions cannot be null or empty. Please select only one.")
	    private Positions reportedPosition;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "branch_id",nullable = false)
	    @NotNull(message = "Branch cannot be null or empty. Please select only one.")
	    private Branch branch;	    
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "department_id",nullable = false)
	    @NotNull(message = "Department cannot be null or empty. Please select only one.")
	    private Department department;
	    
	    
	    
	    @Column(name = "reported_date", nullable = false)
	    @NotNull(message = "Reported Date cannot be null or empty")
	    private LocalDate reportedDate;
	    
	    @Column(name = "development_goal", nullable = false)
	    @NotBlank(message = "Development goal cannot be null or empty")
	    private String developmentGoal;
	    
	    @Column(name = "development_due_date", nullable = false)
	    @NotNull(message = "Development due Date cannot be null or empty")
	    private LocalDate developmentDueDate;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "supervisor_emp_id",nullable = false)
	    @NotNull(message = "Supervisor by cannot be null or empty. Please select only one.")
	    private HREmployeeData supervisor;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "supervisor_position_id",nullable = false)
	    @NotNull(message = "Supervisor Positions cannot be null or empty. Please select only one.")
	    private Positions supervisorPosition;
	    
	    @Column(name = "manager_qc_at")
	    private LocalDate supervisorQcAt;
	    
	    @Column(name = "manager_qc_yn", nullable = false)
	    @NotNull(message = "Supervisor Qc Yes/No cannot be null")
	    private Boolean supervisorQcYesNo = false;


	    @Column(name = "survey123_id")
	    private UUID  survey123ID;
	    
	    @OneToMany(mappedBy = "goalSetting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<GoalSettingDetail> goalSettingDetails= new ArrayList<>();
	    

	    @Transient
	    private List<Attachment> attachments = new ArrayList<>();

	
}
