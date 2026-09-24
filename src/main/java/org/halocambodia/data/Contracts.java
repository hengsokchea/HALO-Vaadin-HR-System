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
import lombok.*;

@Entity
@Table(name = "contracts",schema  = "public")
@Getter
@Setter
public class Contracts extends AbstractEntity {
	
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.contracts_contracts_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.contracts_contracts_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "contracts_id")
	    private Long id; 
	    
	    @Column(name = "contract_code", nullable = false)
	    @NotBlank(message = "Contract code cannot be null or empty")
	    private String contractCode;
	    
	    @Column(name = "start_date")
	    private LocalDate startDate;
	    
	    @Column(name = "end_date")
	    private LocalDate endDate;
	    
	    @Column(name = "amount_prog_currency")
	    private Integer amountUSD;
	    
	    @Version
	    @Column(name = "version")
	    private Long version;
	    
		@Override
		public Long getId() {
		    return id;
		}

	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "donors_id",nullable = false)
	    @NotNull(message = "Donor cannot be null or empty. Please select only one.")
	    private Donors donor;

}
