package org.halocambodia.upload.retention;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.audit.UploadAuditAction;
import org.halocambodia.upload.audit.UploadAuditService;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.exception.UploadException;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.halocambodia.upload.service.UploadStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadRetentionService {
    private final UploadSessionRepository repository;
    private final UploadStorageService storageService;
    private final UploadAuditService auditService;
    private final UploadProperties properties;

    @Transactional
    public void softDelete(UUID uploadUuid, String username) {
        UploadSession session = required(uploadUuid);
        if (session.getStatus() != UploadStatus.COMPLETED) {
            throw new UploadException(HttpStatus.CONFLICT, "Only completed uploads can be deleted");
        }
        session.setStatus(UploadStatus.DELETED);
        session.setDeletedAt(OffsetDateTime.now());
        session.setDeletedBy(username);
        session.setRetentionUntil(OffsetDateTime.now().plus(properties.getDeletedFileGracePeriod()));
        repository.save(session);
        auditService.record(session, UploadAuditAction.SOFT_DELETED, 0L, true,
                "Completed file soft-deleted by " + username);
    }

    @Transactional
    public void restore(UUID uploadUuid, String username) {
        UploadSession session = required(uploadUuid);
        if (session.getStatus() != UploadStatus.DELETED) {
            throw new UploadException(HttpStatus.CONFLICT, "Only deleted uploads can be restored");
        }
        Path file = Path.of(session.getFinalPath()).toAbsolutePath().normalize();
        if (!Files.exists(file)) {
            throw new UploadException(HttpStatus.GONE, "Stored file no longer exists");
        }
        session.setStatus(UploadStatus.COMPLETED);
        session.setDeletedAt(null);
        session.setDeletedBy(null);
        session.setRetentionUntil(null);
        repository.save(session);
        auditService.record(session, UploadAuditAction.RESTORED, 0L, true,
                "Deleted file restored by " + username);
    }

    @Transactional
    public void purge(UUID uploadUuid, String username) {
        UploadSession session = required(uploadUuid);
        if (session.getStatus() != UploadStatus.DELETED) {
            throw new UploadException(HttpStatus.CONFLICT, "Only soft-deleted uploads can be purged");
        }
        storageService.deleteSessionFiles(uploadUuid);
        if (session.getFinalPath() != null) {
            try { Files.deleteIfExists(Path.of(session.getFinalPath())); }
            catch (Exception ex) { throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete final file"); }
        }
        auditService.record(session, UploadAuditAction.PURGED, session.getTotalSize(), true,
                "File permanently purged by " + username);
        repository.delete(session);
    }

    @Transactional
    public int purgeExpiredDeletedFiles() {
        List<UploadSession> expired = repository.findByStatusAndDeletedAtBefore(
                UploadStatus.DELETED, OffsetDateTime.now().minus(properties.getDeletedFileGracePeriod()));
        expired.forEach(s -> purge(s.getUploadUuid(), "SYSTEM"));
        return expired.size();
    }

    @Transactional
    public int applyCompletedRetention() {
        if (!properties.isAutomaticCompletedFileRetentionEnabled()) return 0;
        List<UploadSession> old = repository.findByStatusAndUpdatedAtBefore(
                UploadStatus.COMPLETED, OffsetDateTime.now().minus(properties.getCompletedFileRetention()));
        old.forEach(s -> softDelete(s.getUploadUuid(), "SYSTEM"));
        return old.size();
    }

    private UploadSession required(UUID uploadUuid) {
        return repository.findByUploadUuid(uploadUuid)
                .orElseThrow(() -> new UploadException(HttpStatus.NOT_FOUND, "Upload session not found"));
    }
}
