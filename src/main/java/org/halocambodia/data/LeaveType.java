package org.halocambodia.data;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
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
import jakarta.persistence.Version;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "list_leave_type",schema  = "public")
@AssociationOverrides({
    @AssociationOverride(name = "userCreated", joinColumns = @JoinColumn(name = "created_by", nullable = true, updatable = false)),
    @AssociationOverride(name = "userUpdated", joinColumns = @JoinColumn(name = "updated_by", nullable = true))
})
@Getter
@Setter
public class LeaveType extends AbstractEntity {
	 @Id
	    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.leave_type_leave_type_id_seq")
	    // The initial value is to account for data.sql demo data ids
	    @SequenceGenerator(name = "public.leave_type_leave_type_id_seq", initialValue = 1,allocationSize = 1)
	    @Column(name = "leave_type_id")
	    private Long id; 
	 
	    @Column(name = "leav_type_code")
	    private String leavTypeCode;
	    
	    @Column(name = "leave_name_en")
	    private String leaveNameEn;
	    
	    @Column(name = "leave_name_local")
	    private String leaveNameKh;
	    
	    @Column(name = "remarks")
	    private String remarks;

	    @Column(name = "legend_color", nullable = false, length = 7)
	    private String legendColor = "#78716C";
	    
	    @Column(name = "leave_rate")
	    private Float leaveRate;
	    
	    @Column(name = "paid")
	    private Boolean paid=true;
	    
	    @Column(name = "obsolete_date")
	    private LocalDate obsoleteDate;
	    
	    @OneToMany(mappedBy = "leaveType", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	    private List<LeaveTypeSubType> leaveTypeSubTypes= new ArrayList<>();
	    
	    
	    @ManyToOne(fetch = FetchType.LAZY)		
	    @JoinColumn(name = "list_leave_type_group_id",nullable = false)
	    @NotNull(message = "Leave group cannot be null or empty. Please select only one.")
	    private LeaveTypeGroup  leaveTypeGroup;
	    
	    
		@Column(name = "entitled_days", precision = 5, scale = 2)
		private BigDecimal  entitledDays;

	@Version
	@Column(name = "version", nullable = false)
	private Long version = 0L;

	@Override
	public Long getId() { return id; }
	

}
