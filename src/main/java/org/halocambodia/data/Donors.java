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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "donors",schema  = "public")
@Getter
@Setter
public class Donors extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.donors_donors_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.donors_donors_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "donors_id")
	    private Long id; 
	    
	    @Column(name = "donor_long_name", nullable = false)
	    @NotBlank(message = "Donor long name cannot be null or empty")
	    private String donorLongName;
	    
	    @Column(name = "donor_short_name", nullable = false)
	    @NotBlank(message = "Donor short name cannot be null or empty")
	    private String donorShortName;
	    
	    @Column(name = "donor_country", nullable = false)	    
	    private String donorCountry;
	
	    
	    @OneToMany(mappedBy = "donor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<Contracts> contracts = new ArrayList<>();

	    
		@Override
		public Long getId() {
		    return id;
		}
}
