package org.halocambodia.data;

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
@Table(name = "holiday",schema  = "public")
@Getter
@Setter
public class Holiday extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.holiday_holiday_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.holiday_holiday_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "holiday_id")
	    private Long id;
	 
	 	@NotNull(message = "Holiday date is required | ត្រូវបញ្ចូលកាលបរិច្ឆេទថ្ងៃឈប់សម្រាក")
	    @Column(name = "holiday_date", nullable = false)
	    private LocalDate holidayDate;

	    @NotBlank(message = "Holiday name (EN) is required | ត្រូវបញ្ចូលឈ្មោះថ្ងៃឈប់សម្រាក (អង់គ្លេស)")
	    @Column(name = "holiday_name", nullable = false)
	    private String holidayName;

	    @NotBlank(message = "Holiday name (KH) is required | ត្រូវបញ្ចូលឈ្មោះថ្ងៃឈប់សម្រាក (ខ្មែរ)")
	    @Column(name = "holiday_name_kh", nullable = false)
	    private String holidayNameKh;

	    @NotBlank(message = "Holiday code is required | ត្រូវបញ្ចូលកូដថ្ងៃឈប់សម្រាក")
	    @Column(name = "holiday_code", nullable = false)
	    private String holidayCode;
	    

	    
	    @Column(name = "remark")
	    private String remark;
	    
	    
	    @OneToMany(mappedBy = "holiday", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<ShiftCycleDetail> shiftCycleDetails = new ArrayList<>();
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "holiday_group_id",nullable = false)
	    @NotNull(message = "Holiday group cannot be null or empty. Please select only one.")
	    private HolidayGroup holidayGroup;

	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "leave_type_id")
	    private LeaveType leaveType;
	    
	    
		@Override
		public Long getId() {
		    return id;
		}
}
