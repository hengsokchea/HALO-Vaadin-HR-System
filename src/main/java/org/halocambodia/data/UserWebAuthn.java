package org.halocambodia.data;


import java.time.LocalDateTime;

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
@Table(name = "user_webauthn",schema  = "core_system")
@Getter
@Setter

public class UserWebAuthn {
	
	 @Id
	 @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_system.user_webauthn_user_webauthn_id_seq")
	 @SequenceGenerator(name = "core_system.user_webauthn_user_webauthn_id_seq", initialValue = 1,allocationSize = 1)
	 @Column(name = "user_webauthn_id")
	 private Long id; 


    @Column(name = "credential_id",  nullable = false, unique = true)
    private String credentialId;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;
    
    @Column(name = "credential_public_key", columnDefinition = "TEXT")
    private String credentialPublicKey;

    @Column(name = "sign_count")
    private Long signCount= 0L;

    @Column(name = "transports")
    private String transports;
    
    @Column(name = "raw_id", columnDefinition = "TEXT")
    private String rawId;

    @Column(name = "client_data_json", columnDefinition = "TEXT")
    private String clientDataJson;

    @Column(name = "attestation_object", columnDefinition = "TEXT")
    private String attestationObject;
    
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Version
    private Long version;
    
    @Column(name = "aaguid")
    private String aaguid;

    @Column(name = "credential_type")
    private String credentialType;

    @Column(name = "last_ip")
    private String lastIpAddress;
    
    @Column(name = "user_handle")
    private String userHandle;

    @Column(name = "user_agent")
    private String userAgent;
    
    
    
    @ManyToOne(fetch = FetchType.LAZY)		
    @JoinColumn(name = "user_id")
    @NotNull(message = "User is required")
    private User user;


}
