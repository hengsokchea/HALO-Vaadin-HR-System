package org.halocambodia.data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "policy_acknowledge", schema = "public")
@Getter
@Setter
public class PolicyAcknowledge extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public.policy_acknowledge_policy_acknowledge_id_seq")
    @SequenceGenerator(name = "public.policy_acknowledge_policy_acknowledge_id_seq", initialValue = 1, allocationSize = 1)
    @Column(name = "policy_acknowledge_id")
    private Long id;

    @Column(name = "acknowledged_at",  nullable = false)
    private ZonedDateTime acknowledgedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "remarks")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    @NotNull(message = "Policy cannot be null or empty. Please select only one.")
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @NotNull(message = "Employee cannot be null or empty. Please select only one.")
    private Employee employee;
    
    @Version
    @Column(name = "version")
    private Long version;

    @Override
    public Long getId() {
        return id;
    }
    
    @Column(name = "policy_version", nullable = false)
    private Long policyVersion;
}