package org.halocambodia.upload.recovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.halocambodia.upload.audit.UploadAuditAction;
import org.halocambodia.upload.audit.UploadAuditService;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.halocambodia.upload.service.UploadStorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadRecoveryService {
    private final UploadSessionRepository repository;
    private final UploadStorageService storageService;
    private final UploadAuditService auditService;

    @Scheduled(initialDelayString = "${halo.upload.recovery-initial-delay-ms:30000}",
            fixedDelayString = "${halo.upload.recovery-interval-ms:300000}")
    @Transactional
    public void reconcileActiveSessions() {
        List<UploadSession> sessions = repository.findByStatusIn(List.of(
                UploadStatus.PENDING, UploadStatus.UPLOADING, UploadStatus.PAUSED));

        for (UploadSession session : sessions) {
            long uploaded = 0L;
            int received = 0;
            for (int i = 0; i < session.getTotalChunks(); i++) {
                if (storageService.chunkExists(session.getUploadUuid(), i)) {
                    uploaded += storageService.chunkSize(session.getUploadUuid(), i);
                    received++;
                }
            }

            if (!uploadedEquals(session, uploaded, received)) {
                session.setUploadedSize(uploaded);
                session.setReceivedChunks(received);
                session.setUpdatedAt(OffsetDateTime.now());
                repository.save(session);
                auditService.record(session, UploadAuditAction.RECOVERED,
                        uploaded, true, "Upload progress reconciled from disk");
                log.info("Recovered upload {}: {}/{} chunks", session.getUploadUuid(),
                        received, session.getTotalChunks());
            }
        }
    }

    private boolean uploadedEquals(UploadSession session, long uploaded, int received) {
        return session.getUploadedSize() != null
                && session.getUploadedSize() == uploaded
                && session.getReceivedChunks() != null
                && session.getReceivedChunks() == received;
    }
}
