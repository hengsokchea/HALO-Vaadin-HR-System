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
@Table(name = "emp_disciplinary",schema  = "public")
@Getter
@Setter
public class EmployeeDisciplinary extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_disciplinary_discipinary_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_disciplinary_discipinary_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "discipinary_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id",nullable = false)	    
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "position_id",nullable = false)	    
	    @NotNull(message = "Position cannot be null or empty. Please select only one.")
	    private Positions positions;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "disciplinary_action",nullable = false)	    
	    @NotNull(message = "Disciplinary cannot be null or empty. Please select only one.")
	    private Disciplinary disciplinary;

	
	    @Column(name = "description")	    
	    private String description;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "team_id",nullable = false)	    
	    @NotNull(message = "Teams cannot be null or empty. Please select only one.")
	    private Teams teams;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "location_id",nullable = false)	    
	    @NotNull(message = "Teams cannot be null or empty. Please select only one.")
	    private Branch branch;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "reported_by_emp_id")	    	    
	    private Employee reportedBy;
	    	    
	    @Column(name = "fine_usd", nullable = false, precision = 14, scale = 2)
	    @NotNull(message = "Fine (USD) cannot be null or empty")
	    private BigDecimal fineUsd = BigDecimal.ZERO;
	    
	    @Column(name = "start_date", nullable = false)
	    @NotNull(message = "Start Date cannot be null or empty")
	    private LocalDate startDate;
	    
	    @Column(name = "expire_date", nullable = false)
	    @NotNull(message = "Expire Date cannot be null or empty")
	    private LocalDate expireDate;
	    


	    @Version
	    @Column(name = "version")
	    private Long version;
	    

}
