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
@Table(name = "nationality",schema  = "public")
@Getter
@Setter
public class Nationality extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.nationality_nationality_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.nationality_nationality_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "nationality_id")
	    private Long id; 
	    
	    @Column(name = "nationality_en", nullable = false, unique = true)
	    @NotNull(message = "Nationality Name(EN) is required")
	    @Size(min = 1, message = "Nationality Name(EN) cannot be empty")
	    private String nationalityEn;
	    
	    @Column(name = "nationality_kh")
	    private String nationalityKh;
	    
	    @Column(name = "is_default")
	    private Boolean isDefault=false;

	    @Version
	    @Column(name = "version")
	    private Long version;
	    
		@Override
		public Long getId() {
		    return id;
		}
}
