package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
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
import jakarta.persistence.Version;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "list_leave_type_sub_type",schema  = "public")
@AssociationOverrides({
    @AssociationOverride(name = "userCreated", joinColumns = @JoinColumn(name = "created_by", nullable = true, updatable = false)),
    @AssociationOverride(name = "userUpdated", joinColumns = @JoinColumn(name = "updated_by", nullable = true))
})
@Getter
@Setter
public class LeaveTypeSubType extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.list_leave_type_sub_type_list_leave_type_sub_type_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.list_leave_type_sub_type_list_leave_type_sub_type_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "list_leave_type_sub_type_id")
	    private Long id; 
	 
	    
	    @Column(name = "leave_sub_type_name_en")
	    private String leaveSubTypeNameEn;
	    
	    @Column(name = "leave_sub_type_name_kh")
	    private String leaveSubTypeNameKh;
	    
	    @Column(name = "remarks")
	    private String remarks;
	    
	    @Column(name = "obsolete_date")
	    private LocalDate obsoleteDate;
	    
	    @Column(name = "entitled_day", precision = 5, scale = 2)
	    private BigDecimal  entitledDay;
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "list_leave_type_id")
	    private LeaveType leaveType;

	@Version
	@Column(name = "version", nullable = false)
	private Long version = 0L;

	@Override
	public Long getId() { return id; }
	    

	

}
