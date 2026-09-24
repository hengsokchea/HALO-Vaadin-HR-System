package org.halocambodia.fileattachment.data;

import java.time.ZonedDateTime;

import org.halocambodia.data.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "file_attachment_access_log",
    schema = "public",
    indexes = {
        @Index(
            name = "idx_file_attachment_access_log_attachment_time",
            columnList = "file_attachment_id, accessed_at"
        ),
        @Index(
            name = "idx_file_attachment_access_log_owner_time",
            columnList = "owner_type, owner_id, accessed_at"
        ),
        @Index(
            name = "idx_file_attachment_access_log_user_time",
            columnList = "user_id, accessed_at"
        )
    }
)
@Getter
@Setter
public class FileAttachmentAccessLog {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "file_attachment_access_log_seq"
    )
    @SequenceGenerator(
        name = "file_attachment_access_log_seq",
        sequenceName =
            "public.file_attachment_access_log_file_attachment_access_log_id_seq",
        allocationSize = 1
    )
    @Column(name = "file_attachment_access_log_id")
    private Long id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "file_attachment_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_file_attachment_access_log_attachment"
        )
    )
    private FileAttachment fileAttachment;

    @Column(
        name = "owner_type",
        nullable = false,
        length = 50
    )
    private String ownerType;

    @Column(
        name = "owner_id",
        nullable = false
    )
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "action",
        nullable = false,
        length = 50
    )
    private FileAttachmentAccessAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        foreignKey = @ForeignKey(
            name = "fk_file_attachment_access_log_user"
        )
    )
    private User user;

    @Column(
        name = "accessed_at",
        nullable = false
    )
    private ZonedDateTime accessedAt;

    @Column(
        name = "ip_address",
        length = 100
    )
    private String ipAddress;

    @Column(
        name = "user_agent",
        length = 1000
    )
    private String userAgent;

    @Column(
        name = "request_uri",
        length = 1000
    )
    private String requestUri;

    @Column(
        name = "http_method",
        length = 10
    )
    private String httpMethod;

    @Column(name = "response_status")
    private Integer responseStatus;
}