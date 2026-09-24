package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.halocambodia.enums.EmployeeTypeEnum;
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
@Table(name = "list_disability_type_option",schema  = "public")
@Getter
@Setter
public class DisabilityTypeOption extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_disability_type_option_disability_type_option_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.list_disability_type_option_disability_type_option_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "disability_type_option_id")
	    private Long id; 
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "disability_type_id",nullable = false)	    
	    @NotNull(message = "Disability Type cannot be null or empty. Please select only one.")
	    private DisabilityType disabilityType;
	    
	    @Column(name = "disability_type_option_name", nullable = false,unique = true)
	    @NotNull(message = "Disability Type Option cannot be null or empty")
	    @Size(min = 1, message = "Disability Type Option cannot be empty")
	    private String disabilityTypeOptionName;

	    
	    @Column(name = "note")
	    private String note;
	    
		@Version
		@Column(name = "version")
		private Long version;
	
		@Override
		public Long getId() {
		    return id;
		}

}
