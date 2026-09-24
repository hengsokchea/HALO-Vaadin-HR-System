package org.halocambodia.upload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadCleanupService {

    private final UploadSessionRepository repository;
    private final UploadSessionService sessionService;

    @Transactional
    public int expireAbandonedUploads() {
        List<UploadSession> expired = repository.findByExpiresAtBeforeAndStatusNotIn(
                OffsetDateTime.now(),
                List.of(UploadStatus.COMPLETED, UploadStatus.CANCELLED, UploadStatus.EXPIRED)
        );
        expired.forEach(session -> {
            try {
                sessionService.expire(session);
            } catch (Exception exception) {
                log.warn("Could not expire upload {}", session.getUploadUuid(), exception);
            }
        });
        return expired.size();
    }
}
