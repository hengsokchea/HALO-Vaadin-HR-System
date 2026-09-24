package org.halocambodia.data;

import java.time.LocalDate;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "list_position",schema  = "public")
@Getter
@Setter
public class Positions extends AbstractEntity{
	
	@Override
	public Long getId() {
	    return id;
	}
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_position_position_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.list_position_position_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "position_id")
	    private Long id;

	    
	    @Column(name = "emp_position", nullable = false, unique = true)
	    @NotBlank(message = "position  cannot be null or empty")
	    private String position;
	    
	    @Column(name = "emp_position_kh", nullable = false, unique = true)
	    @NotBlank(message = "Positions KH cannot be null or empty")
	    private String positionKh;
	    
	    @Column(name = "obsolate_date")
	    private LocalDate obsolateDate;
	    
	    @Version
	    @Column(name = "version")
	    private Long version;

}
