package org.halocambodia.data;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "data_explorer",schema  = "core_system")
@Getter
@Setter
public class DataExplorer extends AbstractEntity{
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.data_explorer_data_explorer_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "core_system.data_explorer_data_explorer_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "data_explorer_id")
	    private Long id;
	 
	    @Column(name = "data_explorer_name", nullable = false, unique = true)
	    @NotBlank(message = "Data explorer name cannot be null or empty")
	    private String dataExplorerName;
	
	    
	    @Column(name = "query")	   
	    private String query;
	    
	    @Column(name = "notes")	   
	    private String notes;
	    
	    @OneToMany(mappedBy = "dataExplorer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<DataExplorerUser> dataExplorerUsers= new ArrayList<>();
	    
	    @NotEmpty(message = "At least one role must be selected")
	    @ManyToMany(fetch = FetchType.LAZY)
	    @JoinTable(
	        name = "data_explorer_role",schema  = "core_system",
	        joinColumns = @JoinColumn(name = "data_explorer_id"),
	        inverseJoinColumns = @JoinColumn(name = "roles_id")
	    )
	    private Set<Role> roles;
	    
		@Override
		public Long getId() {
		    return id;
		}

}
