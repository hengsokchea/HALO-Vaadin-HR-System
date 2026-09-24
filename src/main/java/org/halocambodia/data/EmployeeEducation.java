package org.halocambodia.data;

import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.halocambodia.enums.EducationStatusEnum;
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
@Table(name = "emp_education",schema  = "public")
@Getter
@Setter
public class EmployeeEducation  extends AbstractEntity{
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_education_eduction_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_education_eduction_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "eduction_id")
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
	    @JoinColumn(name = "education_type",nullable = false)	    
	    @NotNull(message = "Degree cannot be null or empty. Please select only one.")
	    private EducationType educationType;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "education_center_id",nullable = false)	    
	    @NotNull(message = "Institute cannot be null or empty. Please select only one.")
	    private EducationCenter educationCenter;

	    
	    @Column(name = "major", nullable = false)
	    @NotNull(message = "Major/Subject is required")
	    @Size(min = 1, message = "Major/Subject cannot be empty")
	    private String major;
	    
	    
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "education_status")
	    private EducationStatusEnum status;
	    

	    
	    @Column(name = "end_date")
	    private LocalDate EndDate;
	    
		@Version
		@Column(name = "version")
		private Long version;
	

}
