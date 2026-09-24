package org.halocambodia.upload.quota;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.exception.UploadException;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadQuotaService {
    private static final List<UploadStatus> ACTIVE = List.of(
            UploadStatus.PENDING, UploadStatus.UPLOADING, UploadStatus.PAUSED);

    private final UploadSessionRepository repository;
    private final UploadProperties properties;

    public void assertCanInitialize(String username, long requestedBytes) {
        long activeSessions = repository.countByOwnerUsernameAndStatusIn(username, ACTIVE);
        if (activeSessions >= properties.getMaxActiveSessionsPerUser()) {
            throw new UploadException(HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum active upload sessions reached");
        }

        long reservedBytes = repository.sumTotalSizeByOwnerAndStatuses(username, ACTIVE);
        if (reservedBytes + requestedBytes > properties.getMaxReservedBytesPerUser()) {
            throw new UploadException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Per-user upload quota would be exceeded");
        }
    }
}
