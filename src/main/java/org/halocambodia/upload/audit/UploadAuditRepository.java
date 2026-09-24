package org.halocambodia.upload.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface UploadAuditRepository extends JpaRepository<UploadAuditEvent, Long> {
    List<UploadAuditEvent> findTop100ByUploadUuidOrderByCreatedAtDesc(UUID uploadUuid);
    Page<UploadAuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
    List<UploadAuditEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime from, OffsetDateTime to);
}
