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
@Table(name = "contract_type",schema  = "public")
@Getter
@Setter
public class ContractType extends AbstractEntity {
	
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.contract_type_contract_type_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.contract_type_contract_type_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "contract_type_id")
	    private Long id; 
	    
	    @Column(name = "contract_type_name", nullable = false, unique = true)
	    @NotNull(message = "Contract Type Name is required")
	    @Size(min = 1, message = "Contract Type Name cannot be empty")
	    private String contractTypeName;
	    

	    @Version
	    @Column(name = "version")
	    private Long version;
	    
		@Override
		public Long getId() {
		    return id;
		}
}
