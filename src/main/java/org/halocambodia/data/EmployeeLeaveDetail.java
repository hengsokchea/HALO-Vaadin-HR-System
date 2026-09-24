package org.halocambodia.data;

import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "emp_leave_detail",schema  = "public")
@Getter
@Setter


public class EmployeeLeaveDetail  extends AbstractEntity{
	@Version
	@Column(name = "version")
	private Long version;
	
	

    public enum LeaveDuration {
        MORNING_ONLY("Morning only"),
        AFTERNOON_ONLY("Afternoon only"),
        FULL_DAY("Full day");

        private final String label;

        LeaveDuration(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
    
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_leave_detail_emp_leave_detail_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_leave_detail_emp_leave_detail_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_leave_detail_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_leave_id")
	    @NotNull(message = "Leave is required")
	    private EmployeeLeave employeeLeave;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "leave_type_id")
	    @NotNull(message = "Leave type is required")
	    private LeaveType leaveType;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "leave_type_sub_type_id")
	    private LeaveTypeSubType leaveTypeSubType ;
	    
	    
	    @NotNull(message = "From date is required")
	    @Column(name = "from_date", nullable = false)
	    private LocalDate fromDate;
	    
	    @NotNull(message = "To date is required")
	    @Column(name = "to_date", nullable = false)
	    private LocalDate toDate;
	    

	    @Column(name = "number_of_day", insertable = false, updatable = false)
	    private Float numberOfDay;

	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "leave_duration", nullable = false)
	    @NotNull(message = "Leave duration is required")
	    private LeaveDuration leaveDuration;


	  
	    @Column(name = "remark")
	    private String remark;
	    
	    @Column(name = "survey123_id")
	    private UUID  survey123ID;
	    
	    @Transient
	    public String getDateRangeDisplay() {
	        if (fromDate != null && toDate != null) {
	            return fromDate.equals(toDate)
	                    ? fromDate.toString()
	                    : fromDate + " → " + toDate;
	        }
	        return "";
	    }
	    
	    public void calculateNumberOfDays() {
	        if (fromDate != null && toDate != null && leaveDuration != null) {
	            long daysBetween = toDate.toEpochDay() - fromDate.toEpochDay() + 1;

	            switch (leaveDuration) {
	                case FULL_DAY:
	                    this.numberOfDay = (float) daysBetween;
	                    break;

	                case MORNING_ONLY:
	                case AFTERNOON_ONLY:
	                    if (daysBetween == 1) {
	                        this.numberOfDay = 0.5f;
	                    } else {
	                        this.numberOfDay = (float) (daysBetween - 1 + 0.5);
	                    }
	                    break;
	            }
	        }
	    }

}
