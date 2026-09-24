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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "emp_supervision",schema  = "public")
@Getter
@Setter
public class HREmployeeWithSupervisor{

		 @Id
		 @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_supervision_emp_supervision_id_seq")
		    // The initial value is to account for data.sql demo data ids
		 @SequenceGenerator(name = "public.emp_supervision_emp_supervision_id_seq", initialValue = 1,allocationSize = 1)
		 @Column(name = "emp_supervision_id")
		 private Long id;
		 
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id",nullable = false)
	    @NotNull(message = "Employee cannot be null or empty")
	    private HREmployeeData  employee;	    

	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "manager_id",nullable = false)
	    @NotNull(message = "Supervisor cannot be null or empty")
	    private HREmployeeData  supervisor;
	    
	    @Column(name = "sort_order")
	    private Integer sortOrder;
	    
}
