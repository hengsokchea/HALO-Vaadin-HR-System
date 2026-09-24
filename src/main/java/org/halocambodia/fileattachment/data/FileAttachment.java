package org.halocambodia.fileattachment.data;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.halocambodia.data.AbstractEntity;
import org.halocambodia.data.AttachmentType;
import org.halocambodia.data.User;
import org.halocambodia.upload.domain.UploadType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * New disk-based attachment metadata.
 *
 * This entity intentionally uses table public.file_attachment so the existing
 * production public.attachment table and legacy Attachment entity remain
 * untouched and can continue operating during gradual migration.
 */
@Entity
@Table(
    name = "file_attachment",
    schema = "public",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_file_attachment_file_uuid",
        columnNames = "file_uuid"
    ),
    indexes = {
        @Index(
            name = "idx_file_attachment_owner",
            columnList = "owner_type, owner_id, sort_order"
        ),
        @Index(
            name = "idx_file_attachment_upload_type",
            columnList = "upload_type"
        ),
        @Index(
            name = "idx_file_attachment_category",
            columnList = "file_category"
        )
    }
)
@Getter
@Setter
public class FileAttachment extends AbstractEntity {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "file_attachment_id_generator"
    )
    @SequenceGenerator(
        name = "file_attachment_id_generator",
        sequenceName = "public.file_attachment_file_attachment_id_seq",
        allocationSize = 1
    )
    @Column(name = "file_attachment_id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 50)
    private FileAttachmentOwnerType ownerType;

    @NotNull
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", nullable = false, length = 50)
    private UploadType uploadType = UploadType.OTHER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", nullable = false, length = 30)
    private FileAttachmentType fileCategory = FileAttachmentType.OTHER;

    @NotNull
    @Column(name = "file_uuid", nullable = false, columnDefinition = "uuid")
    private UUID fileUuid;

    @NotBlank
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @NotBlank
    @Column(name = "mime_type", nullable = false, length = 255)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** Existing business attachment type lookup used by the legacy system too. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_type_id")
    private AttachmentType attachmentType;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public Long getId() {
        return id;
    }
    
    @Column(name = "deleted",nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at",columnDefinition = "TIMESTAMP(0) WITH TIME ZONE")
    private ZonedDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;
}
