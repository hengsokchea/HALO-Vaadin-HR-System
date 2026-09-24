package org.halocambodia.upload.admin;

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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadAdminOperationService {

    private static final List<UploadStatus> ACTIVE = List.of(
            UploadStatus.PENDING, UploadStatus.UPLOADING, UploadStatus.PAUSED);

    private final UploadSessionRepository repository;
    private final UploadStorageService storageService;
    private final UploadAuditService auditService;
    private final UploadProperties properties;

    @Transactional(readOnly = true)
    public UploadAdminSummary summary() {
        return new UploadAdminSummary(
                repository.count(),
                repository.countByStatusIn(ACTIVE),
                repository.countByStatus(UploadStatus.COMPLETED),
                repository.countByStatus(UploadStatus.FAILED),
                repository.countByStatus(UploadStatus.CANCELLED),
                repository.countByStatus(UploadStatus.EXPIRED));
    }

    @Transactional
    public void cancel(UUID uploadUuid, boolean deleteTemporaryFiles) {
        UploadSession session = required(uploadUuid);
        if (session.getStatus() == UploadStatus.COMPLETED) {
            throw new UploadException(HttpStatus.CONFLICT,
                    "Completed uploads cannot be cancelled from the administration page");
        }
        session.setStatus(UploadStatus.CANCELLED);
        session.setFailureMessage("Cancelled by upload administrator");
        session.setExpiresAt(OffsetDateTime.now());
        repository.save(session);
        if (deleteTemporaryFiles) {
            storageService.deleteSessionFiles(uploadUuid);
        }
        auditService.record(session, UploadAuditAction.CANCELLED, 0L, true,
                "Upload cancelled by administrator");
    }

    @Transactional
    public void reactivate(UUID uploadUuid) {
        UploadSession session = required(uploadUuid);
        if (session.getStatus() == UploadStatus.COMPLETED) {
            throw new UploadException(HttpStatus.CONFLICT, "Completed uploads cannot be reactivated");
        }
        if (session.getStatus() != UploadStatus.FAILED
                && session.getStatus() != UploadStatus.CANCELLED
                && session.getStatus() != UploadStatus.EXPIRED
                && session.getStatus() != UploadStatus.PAUSED) {
            throw new UploadException(HttpStatus.CONFLICT,
                    "Only failed, cancelled, expired, or paused uploads can be reactivated");
        }
        session.setStatus(session.getReceivedChunks() != null && session.getReceivedChunks() > 0
                ? UploadStatus.UPLOADING : UploadStatus.PENDING);
        session.setFailureMessage(null);
        session.setExpiresAt(OffsetDateTime.now().plus(properties.getSessionTtl()));
        repository.save(session);
        auditService.record(session, UploadAuditAction.RECOVERED, 0L, true,
                "Upload reactivated by administrator");
    }

    private UploadSession required(UUID uploadUuid) {
        return repository.findByUploadUuid(uploadUuid)
                .orElseThrow(() -> new UploadException(HttpStatus.NOT_FOUND, "Upload session not found"));
    }
}
