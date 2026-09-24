package org.halocambodia.data;

import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.halocambodia.enums.GenderEnum;

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
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "emp_dependency_family",schema  = "public")
@Getter
@Setter
public class EmployeeDependencyFamily  extends AbstractEntity{
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_dependency_family_de_f_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_dependency_family_de_f_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "de_f_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id",nullable = false)	    
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    @Column(name = "df_name", nullable = false)
	    @NotNull(message = "Name is required")
	    @Size(min = 1, message = "Name cannot be empty")
	    private String dependencyName;

	    @Enumerated(EnumType.STRING)
	    @Column(name = "df_gender", nullable = false)
	    @NotNull(message = "Gender is required")
	    private GenderEnum gender;
	    
	   // @Column(name = "df_dob", nullable = false)
	    //@NotNull(message = "Date of birth cannot be null or empty")
	    @Column(name = "df_dob")
	    private LocalDate dob;
	    
	    @Column(name = "df_id_card")
	    private String nid;
	    
	    @Column(name = "df_phone")
	    private String phone;
	    
	    @Column(name = "occupation")
	    private String occupation;
	    
	    @Column(name = "include_tax")
	    private Boolean includeTax=Boolean.FALSE;
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "df_relationship",nullable = false)	    
	    @NotNull(message = "Relationship cannot be null or empty. Please select only one.")
	    private Relationship relationship;
	    
	    
		@Version
		@Column(name = "version")
		private Long version;
	

}
