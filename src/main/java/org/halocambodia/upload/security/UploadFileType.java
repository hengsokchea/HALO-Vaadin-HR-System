package org.halocambodia.upload.security;

import org.halocambodia.data.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "upload_file_type",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_upload_file_type_extension_mime",
                        columnNames = {
                                "extension",
                                "mime_type"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_upload_file_type_enabled",
                        columnList = "enabled"
                ),
                @Index(
                        name = "idx_upload_file_type_sort_order",
                        columnList = "sort_order"
                )
        }
)
@Getter
@Setter
public class UploadFileType extends AbstractEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "public.upload_file_type_upload_file_type_id_seq"
    )
    @SequenceGenerator(
            name = "public.upload_file_type_upload_file_type_id_seq",
            sequenceName = "public.upload_file_type_upload_file_type_id_seq",
            allocationSize = 1
    )
    @Column(name = "upload_file_type_id")
    private Long id;

    @NotBlank(message = "File extension is required")
    @Size(max = 20)
    @Column(
            name = "extension",
            nullable = false,
            length = 20
    )
    private String extension;

    @NotBlank(message = "MIME type is required")
    @Size(max = 255)
    @Column(
            name = "mime_type",
            nullable = false,
            length = 255
    )
    private String mimeType;

    @NotBlank(message = "Display name is required")
    @Size(max = 100)
    @Column(
            name = "display_name",
            nullable = false,
            length = 100
    )
    private String displayName;

    @NotNull
    @Column(
            name = "enabled",
            nullable = false
    )
    private Boolean enabled = true;

    @Column(name = "max_file_size")
    private Long maxFileSize;

    @Column(name = "description")
    private String description;

    @NotNull
    @Column(
            name = "sort_order",
            nullable = false
    )
    private Integer sortOrder = 0;

    @Version
    @Column(name = "version")
    private Long version;

    @Override
    public Long getId() {
        return id;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}