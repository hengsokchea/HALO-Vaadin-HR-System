package org.halocambodia.upload.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_audit_event", schema = "public", indexes = {
        @Index(name = "idx_upload_audit_upload_uuid", columnList = "upload_uuid"),
        @Index(name = "idx_upload_audit_username", columnList = "username"),
        @Index(name = "idx_upload_audit_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_uuid")
    private UUID uploadUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private UploadAuditAction action;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "bytes_processed")
    private Long bytesProcessed;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "remote_address", length = 100)
    private String remoteAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
