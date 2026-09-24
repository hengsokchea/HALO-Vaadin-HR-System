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
@Table(name = "shift_cycle_detail",schema  = "public")
@Getter
@Setter
public class ShiftCycleDetail extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.shift_cycle_detail_shift_cycle_detail_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.shift_cycle_detail_shift_cycle_detail_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "shift_cycle_detail_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
		
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "shift_cycle_id",nullable = false)
	    @NotNull(message = "Shift cycle cannot be null or empty. Please select only one.")
	    private ShiftCycle shiftCycle;
	    
	    @Column(name = "cycle_date")	  
	    private LocalDate cycleDate;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "holiday_id")
	    private Holiday holiday;
	 
	   
}
