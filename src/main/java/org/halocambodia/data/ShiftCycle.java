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
@Table(name = "shift_cycle",schema  = "public")
@Getter
@Setter
public class ShiftCycle extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.shift_cycle_shift_cycle_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.shift_cycle_shift_cycle_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "shift_cycle_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "shift_id",nullable = false)
	    @NotNull(message = "Shift cannot be null or empty. Please select only one.")
	    private Shift shift;
	 
	    @Column(name = "year_number",  nullable = false)
	    @NotNull(message = "Year cannot be null")
	    private Integer year;
	    
	    @Column(name = "month_number")
	    private Integer month;
	    
	    @Column(name = "cycle_start_date")	  
	    private LocalDate cycleStartDate;	
	    
	    @Column(name = "cycle_end_date")	  
	    private LocalDate cycleEndDate;	
	    
	    @OneToMany(mappedBy = "shiftCycle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<ShiftCycleDetail> shiftCycleDetails = new ArrayList<>();
	    
	    @Column(name = "include_weekends", nullable = false)
	    @NotNull(message = "Include Weekends cannot be null.")
	    private Boolean includeWeekends = Boolean.FALSE;
	    
	    
	   
}
