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
@Table(name = "leave_status",schema  = "public")
@Getter
@Setter
public class LeaveStatus  {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.leave_status_leave_status_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.leave_status_leave_status_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "leave_status_id")
	    private Long id; 
	 
	    @Column(name = "leave_status_name")
	    private String leaveStatusName;
	    
	    @Column(name = "leave_status_name_kh")
	    private String leaveStatusNameKh;
	    
	    @Column(name = "description")
	    private String description;
	    
	    @Column(name = "is_active")
	    private Boolean isActive=true;
	    
	    @Column(name = "sort_order")
	    private Integer sortOrder;
	

}
