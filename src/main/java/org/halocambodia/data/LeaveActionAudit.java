package org.halocambodia.data;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "leave_action_audit",schema  = "public")
@Getter
@Setter
public class LeaveActionAudit extends AbstractEntity{
	
	@Override
	public Long getId() {
	    return id;
	}

	@Version
	@Column(name = "version")
	private Long version;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.leave_action_audit_leave_action_audit_id_seq")
	@SequenceGenerator(name = "public.leave_action_audit_leave_action_audit_id_seq", initialValue = 1,allocationSize = 1)
	@Column(name = "leave_action_audit_id")
	private Long id;

	@Column(name = "action_type")
    private String actionType;

	@Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 4000)
    private String userAgent;

    @Column(name = "browser")
    private String browser;

    @Column(name = "operating_system")
    private String operatingSystem;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "success")
    private boolean success;

    @Column(name = "action_time")
    private LocalDateTime actionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_action_token_id")
    @NotNull(message = "Leave Action Token is required")
    private LeaveActionToken leaveActionToken;


    
    @ManyToOne(fetch = FetchType.LAZY)		
    @JoinColumn(name = "emp_leave_id")
    @NotNull(message = "Leave is required")
    private EmployeeLeave employeeLeave;
}