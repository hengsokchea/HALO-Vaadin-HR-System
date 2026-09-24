package org.halocambodia.upload.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_session", schema = "public", indexes = {
        @Index(name = "idx_upload_session_uuid", columnList = "upload_uuid", unique = true),
        @Index(name = "idx_upload_session_status", columnList = "status"),
        @Index(name = "idx_upload_session_expires_at", columnList = "expires_at"),
        @Index(name = "idx_upload_session_type", columnList = "upload_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_uuid", nullable = false, unique = true, updatable = false)
    private UUID uploadUuid;

    @Column(name = "original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Column(name = "stored_file_name", length = 500)
    private String storedFileName;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "total_size", nullable = false)
    private Long totalSize;

    @Column(name = "uploaded_size", nullable = false)
    @Builder.Default
    private Long uploadedSize = 0L;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "total_chunks", nullable = false)
    private Integer totalChunks;

    @Column(name = "received_chunks", nullable = false)
    @Builder.Default
    private Integer receivedChunks = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private UploadStatus status = UploadStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", nullable = false, length = 50)
    @Builder.Default
    private UploadType uploadType = UploadType.OTHER;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "temporary_path", length = 1000)
    private String temporaryPath;

    @Column(name = "final_path", length = 1000)
    private String finalPath;

    @Column(name = "owner_username", length = 255)
    private String ownerUsername;

    @Column(name = "failure_message", length = 2000)
    private String failureMessage;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;

    @Column(name = "retention_until")
    private OffsetDateTime retentionUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public boolean isCompleted() {
        return status == UploadStatus.COMPLETED && finalPath != null && !finalPath.isBlank();
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (uploadUuid == null) {
            uploadUuid = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public double getProgressPercent() {
        if (totalSize == null || totalSize <= 0) {
            return 0.0;
        }
        return Math.min(100.0, uploadedSize * 100.0 / totalSize);
    }
}
