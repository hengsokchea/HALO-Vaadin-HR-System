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
@Table(name = "list_emp_category",schema  = "public")
@Getter
@Setter
public class Category  {
	
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_emp_category_category_id_seq")
    // The initial value is to account for data.sql demo data ids
    @SequenceGenerator(name = "public.list_emp_category_category_id_seq", initialValue = 1,allocationSize = 1)
    @Column(name = "category_id")
    private Long id;

	@Column(name = "emp_category", nullable = false, unique = true)
	@NotBlank(message = "category cannot be null or empty")
	private String category;
	    
	    
	 //@OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	 //private List<ShiftCycle> shiftCycles = new ArrayList<>();

}
