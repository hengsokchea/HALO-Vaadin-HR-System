package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "emp_attendance_detail_time_log",schema  = "public")
@Getter
@Setter
public class EmployeeAttendanceDetailTimeLog  extends AbstractEntity{

	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_attendance_detail_time_log_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.emp_attendance_detail_time_log_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "emp_attendance_detail_time_log_id")
	    private Long id; 
	 
		@Override
		public Long getId() {
		    return id;
		}
	 
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "emp_attendance_id")
	    @NotNull(message = "Attendance is required")
	    private EmployeeAttendanceDetail attendanceDetail;	   
	    
	    @Column(name = "log_time")
	    private LocalTime logTime;
	    
	
	    
	    @Column(name = "direction")
	    private String direction;
	    
	    @Column(name = "source")
	    private String source;
	    
	    @Column(name = "device_id")
	    private UUID  deviceId;
	    
		@Version
		@Column(name = "version")
		private Long version;


}
