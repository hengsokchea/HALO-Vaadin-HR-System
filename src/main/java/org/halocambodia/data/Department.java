package org.halocambodia.data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "list_department",schema  = "public")
@Getter
@Setter
public class Department extends AbstractEntity{
	
	@Override
	public Long getId() {
	    return id;
	}
	
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_department_department_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.list_department_department_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "department_id")
	    private Long id;

	    
	    @Column(name = "department_name", nullable = false, unique = true)
	    @NotNull(message = "Department EN is required")
	    @Size(min = 1, message = "Department EN cannot be empty")
	    private String name;
	    
	    @Column(name = "department_name_kh", nullable = false, unique = true)
	    @NotNull(message = "Department KH is required")
	    @Size(min = 1, message = "Department KH cannot be empty")
	    private String nameKH;
	    
	    @Column(name = "remarks")
	    private String remarks;
	    
		@Version
		@Column(name = "version")
		private Long version;


}
