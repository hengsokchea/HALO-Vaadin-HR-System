package org.halocambodia.data;

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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "emp_personnel_allocation",schema  = "public")
@Getter
@Setter
public class EmployeeAllocation extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_personnel_allocation_emp_personnel_allocation_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_personnel_allocation_emp_personnel_allocation_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_personnel_allocation_id")
	    private Long id; 
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id",nullable = false)
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    @Column(name = "year_number", nullable = false)
	    @NotNull(message = "Year cannot be null or empty")
	    private Integer yearNum;
	    	    
	    @Min(value = 1, message = "Month must be at least 1")
	    @Max(value = 12, message = "Month must be at most 12")
	    @NotNull(message = "Month cannot be null")
	    @Column(name = "month_number", nullable = false)
	    private Integer monthNum;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)	
	    @JoinColumn(name = "position_id",nullable = false)
	    @NotNull(message = "Position cannot be null or empty. Please select only one.")
	    private Positions positions;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "team_id",nullable = false)
	    @NotNull(message = "Team cannot be null or empty. Please select only one.")
	    private Teams teams;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "branch_id",nullable = false)
	    @NotNull(message = "Location cannot be null or empty. Please select only one.")
	    private Branch branch;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_category_id",nullable = false)
	    @NotNull(message = "Category cannot be null or empty. Please select only one.")
	    private Category category;
	    

	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "contract_id",nullable = false)
	    @NotNull(message = "Contract cannot be null or empty. Please select only one.")
	    private Contracts contracts;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "po_grade_id",nullable = false)
	    @NotNull(message = "Po Grade cannot be null or empty. Please select only one.")
	    private PoGrade poGrade;
	    

	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "shift_cycle_id",nullable = false)
	    @NotNull(message = "Shilf cannot be null or empty. Please select only one.")
	    private ShiftCycle shiftCycle;
	    
	    @ManyToOne(fetch = FetchType.LAZY)	
	    @JoinColumn(name = "duty_leave_type_id")	    
	    private LeaveType duty;
	    
	    @ManyToOne(fetch = FetchType.LAZY)	
	    @JoinColumn(name = "task_id")	    
	    private Tasks minefield;
	    
	
	    
		@Override
		public Long getId() {
		    return id;
		}

}
