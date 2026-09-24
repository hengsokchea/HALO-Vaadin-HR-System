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
@Table(name = "list_education_type",schema  = "public")
@Getter
@Setter
public class EducationType extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.education_type_education_type_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.education_type_education_type_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "education_type_id")
	    private Long id; 
	    
	    @Column(name = "education_name", nullable = false, unique = true)
	    @NotNull(message = "Education Type Name(EN) is required")
	    @Size(min = 1, message = "Education Type Name(EN) cannot be empty")
	    private String educationTypeEN;
	    
	    @Column(name = "education_name_kh")
	    private String educationTypeKh;
	    
	    @Column(name = "education_level")
	    private Integer educationLevel;


	    @Version
	    @Column(name = "version")
	    private Long version;
	    
		@Override
		public Long getId() {
		    return id;
		}
}
