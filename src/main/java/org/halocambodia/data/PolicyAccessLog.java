package org.halocambodia.data;

import java.time.OffsetDateTime;

import org.halocambodia.data.*;

import org.halocambodia.fileattachment.data.FileAttachment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "policy_access_log", schema = "public")
@Getter
@Setter
public class PolicyAccessLog extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policy_access_log_id_generator")
    @SequenceGenerator(
            name = "policy_access_log_id_generator",
            sequenceName = "public.policy_access_log_policy_access_log_id_seq",
            allocationSize = 1)
    @Column(name = "policy_access_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private PolicyAccessAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_attachment_id")
    private FileAttachment fileAttachment;

    @Column(name = "accessed_at", nullable = false)
    private OffsetDateTime accessedAt;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;
    
   @Override
	public Long getId() {
	    return id;
}

}
