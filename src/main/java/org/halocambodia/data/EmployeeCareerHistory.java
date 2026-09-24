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
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "emp_career_history",schema  = "public")
@Getter
@Setter
public class EmployeeCareerHistory extends AbstractEntity {
	
	@Override
	public Long getId() {
	    return id;
	}
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_career_history_career_history_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_career_history_career_history_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "career_history_id")
	    private Long id; 

	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id",nullable = false)	    
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "position_id",nullable = false)	    
	    @NotNull(message = "Position cannot be null or empty. Please select only one.")
	    private Positions position;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "career_type",nullable = false)	    
	    @NotNull(message = "Career Type cannot be null or empty. Please select only one.")
	    private CareerType careerType;
	    
	    @Column(name = "salary", nullable = false, precision = 14, scale = 2)
	    @NotNull(message = "Salary cannot be null or empty")
	    private BigDecimal salary = BigDecimal.ZERO;
	    
	    @Column(name = "from_date", nullable = false)
	    @NotNull(message = "From Date cannot be null or empty")
	    private LocalDate fromDate;
	    
	    @Column(name = "to_date")
	    private LocalDate toDate;
	    
	    
	   /* @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "approve_by_emp_id")	    
	    private Employee approvedBy;
	    
	    @Column(name = "approve_date")
	    private LocalDate approveDate;
	    */
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "location_id",nullable = false)	    
	    @NotNull(message = "Location cannot be null or empty. Please select only one.")
	    private Branch branch;
	    
	

	    @Version
	    @Column(name = "version")
	    private Long version;
	    

}
