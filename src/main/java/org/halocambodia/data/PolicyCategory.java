package org.halocambodia.data;

import java.time.LocalDate;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "policy_category",schema  = "public")
@Getter
@Setter
public class PolicyCategory extends AbstractEntity{

	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.policy_category_policy_category_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.policy_category_policy_category_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "policy_category_id")
	    private Long id;


	    
	    @Column(name = "code", nullable = false, unique = true)
	    @NotBlank(message = "Code is required")
	    @Size(min = 1, message = "Code cannot be empty")
	    private String code;

	    @Column(name = "name_en", nullable = false, unique = true)
	    @NotBlank(message = "English name is required")
	    @Size(min = 1, message = "English name cannot be empty")
	    private String nameEn;

	    @Column(name = "name_kh", nullable = false, unique = true)
	    @NotBlank(message = "Khmer name is required")
	    @Size(min = 1, message = "Khmer name cannot be empty")
	    private String nameKh;

	    @Column(name = "description")
	    private String description;

	    @Column(name = "obsolete_date")
	    private LocalDate obsoleteDate;

	    @OneToMany(mappedBy = "policyCategory", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	    @OrderBy("code ASC")
	    private List<Policy> policies = new ArrayList<>();



	    // Helper method to maintain bidirectional relationship
	    public void addPolicy(Policy policy) {
	        policies.add(policy);
	        policy.setPolicyCategory(this);
	    }

	    public void removePolicy(Policy policy) {
	        policies.remove(policy);
	        policy.setPolicyCategory(null);
	    }
	    
	    @Version
	    @Column(name = "version")
	    private Long version;
	    
		@Override
		public Long getId() {
		    return id;
		}

}
