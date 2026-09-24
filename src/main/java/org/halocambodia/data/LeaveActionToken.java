package org.halocambodia.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "leave_action_token",schema  = "public")
@Getter
@Setter
public class LeaveActionToken  extends AbstractEntity {
	
	@Override
	public Long getId() {
	    return id;
	}

	@Version
	@Column(name = "version")
	private Long version;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.emp_leave_action_token_emp_leave_action_token_id_seq")
	@SequenceGenerator(name = "public.emp_leave_action_token_emp_leave_action_token_id_seq", initialValue = 1,allocationSize = 1)
	@Column(name = "leave_action_token_id")
	private Long id; 

	@Column(name = "token")
    private String tokenHash;
	
	@Column(name = "action_type")
    private String actionType;

	@Column(name = "used")
    private boolean used = false;
	
	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "expires_at")
    private LocalDateTime expiresAt;


    
    @ManyToOne(fetch = FetchType.LAZY)		
    @JoinColumn(name = "emp_leave_id")
    @NotNull(message = "Leave is required")
    private EmployeeLeave employeeLeave;
    
    @OneToMany(mappedBy = "leaveActionToken", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<LeaveActionAudit> leaveActionAudits= new ArrayList<>();

}