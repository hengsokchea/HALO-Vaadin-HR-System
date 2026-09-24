package org.halocambodia.upload.repository;

import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, Long> {

    Optional<UploadSession> findByUploadUuid(UUID uploadUuid);

    Page<UploadSession> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UploadSession> findByOwnerUsernameContainingIgnoreCaseOrOriginalFileNameContainingIgnoreCaseOrderByCreatedAtDesc(
            String ownerUsername, String originalFileName, Pageable pageable);

    Optional<UploadSession> findByUploadUuidAndOwnerUsername(UUID uploadUuid, String ownerUsername);

    List<UploadSession> findByStatus(UploadStatus status);

    List<UploadSession> findByStatusAndExpiresAtBefore(UploadStatus status, OffsetDateTime expiresAt);

    List<UploadSession> findByExpiresAtBeforeAndStatusNotIn(
            OffsetDateTime expiresAt,
            List<UploadStatus> excludedStatuses);

    boolean existsByUploadUuid(UUID uploadUuid);

    List<UploadSession> findByStatusIn(List<UploadStatus> statuses);

    long countByStatus(UploadStatus status);

    long countByStatusIn(List<UploadStatus> statuses);

    List<UploadSession> findByStatusAndUpdatedAtBefore(UploadStatus status, OffsetDateTime cutoff);

    List<UploadSession> findByStatusAndDeletedAtBefore(UploadStatus status, OffsetDateTime cutoff);

    long countByOwnerUsernameAndStatusIn(String ownerUsername, List<UploadStatus> statuses);

    @Query("select coalesce(sum(u.totalSize), 0) from UploadSession u where u.status = :status")
    long sumTotalSizeByStatus(@Param("status") UploadStatus status);

    @Query("""
            select coalesce(sum(u.totalSize), 0)
              from UploadSession u
             where u.ownerUsername = :ownerUsername
               and u.status in :statuses
            """)
    long sumTotalSizeByOwnerAndStatuses(
            @Param("ownerUsername") String ownerUsername,
            @Param("statuses") List<UploadStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UploadSession u
               set u.status = :status,
                   u.failureMessage = :failureMessage,
                   u.updatedAt = :updatedAt
             where u.uploadUuid = :uploadUuid
            """)
    int updateStatus(
            @Param("uploadUuid") UUID uploadUuid,
            @Param("status") UploadStatus status,
            @Param("failureMessage") String failureMessage,
            @Param("updatedAt") OffsetDateTime updatedAt);
}
