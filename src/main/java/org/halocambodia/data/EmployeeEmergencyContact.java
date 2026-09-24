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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "emp_emergency_contact",schema  = "public")
@Getter
@Setter
public class EmployeeEmergencyContact extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_emergency_contact_emp_emergency_contact_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_emergency_contact_emp_emergency_contact_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_emergency_contact_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_id", nullable = false)
	    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
	    private Employee employee;
	    
	    
	    @Column(name = "emergency_contact_name", nullable = false)
	    @NotNull(message = "Emergency Contact Name is required")
	    @Size(min = 1, message = "Emergency Contact Name cannot be empty")
	    private String emergencyContactName;
	    
	    @Column(name = "emergency_contact_phone")
	    private String emergencyContactPhone;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "relationship_id", nullable = false)
	    @NotNull(message = "Relationship cannot be null or empty. Please select only one.")
	    private Relationship relationship;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "address_id", nullable = false)
	    @NotNull(message = "Gazetteer cannot be null or empty. Please select only one.")
	    private Gazetteer gazetteer;
	    
	    
	    @Version
	    @Column(name = "version")
	    private Long version;

}
