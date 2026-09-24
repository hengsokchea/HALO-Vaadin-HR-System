package org.halocambodia.fileattachment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.halocambodia.fileattachment.data.FileAttachment;
import org.halocambodia.fileattachment.data.FileAttachmentOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FileAttachmentRepository
        extends JpaRepository<FileAttachment, Long>,
                JpaSpecificationExecutor<FileAttachment> {

    /**
     * Normal application query. Deleted attachments are excluded.
     */
    List<FileAttachment>
            findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAscIdAsc(
                    FileAttachmentOwnerType ownerType,
                    Long ownerId
            );

    /**
     * Used when serving a preview or download.
     * Deleted attachments must not be accessible.
     */
    Optional<FileAttachment> findByFileUuidAndDeletedFalse(
            UUID fileUuid
    );

    /**
     * Administrative query that includes deleted attachments.
     */
    Optional<FileAttachment> findByFileUuid(
            UUID fileUuid
    );

    /*
     * Keep this checking all records because file_uuid remains globally unique,
     * including soft-deleted records.
     */
    boolean existsByFileUuid(
            UUID fileUuid
    );

    long countByOwnerTypeAndOwnerIdAndDeletedFalse(
            FileAttachmentOwnerType ownerType,
            Long ownerId
    );

    /**
     * Finds only active records belonging to the specified owner.
     * This prevents deleting another module's attachment by passing its ID.
     */
    List<FileAttachment>
            findAllByOwnerTypeAndOwnerIdAndIdInAndDeletedFalse(
                    FileAttachmentOwnerType ownerType,
                    Long ownerId,
                    Collection<Long> ids
            );

    List<FileAttachment> findAllByDeletedFalseOrderByCreatedAtDesc();

    List<FileAttachment> findAllByDeletedTrueOrderByDeletedAtDesc();
}