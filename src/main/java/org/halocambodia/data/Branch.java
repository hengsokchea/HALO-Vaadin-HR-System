package org.halocambodia.data;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "list_location",schema  = "public")
@Getter
@Setter
public class Branch extends AbstractEntity{
	
	@Override
	public Long getId() {
	    return id;
	}
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_location_location_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.list_location_location_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "location_id")
	    private Long id;

	    
	    @Column(name = "location_full_name", nullable = false, unique = true)
	    @NotBlank(message = "Branch full name cannot be null or empty")
	    private String branchFullName;
	    
	    @Column(name = "location_short_name", nullable = false, unique = true)
	    @NotBlank(message = "Branch short name cannot be null or empty")
	    private String branchShortName;
	    
        @Column(name = "is_active", nullable = false)
        private boolean isActive = true;
        
        @OneToMany(mappedBy = "branch", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("sortOrder ASC")
        private List<BranchManagerAssignment> branchManagerAssignments = new ArrayList<>();

}
