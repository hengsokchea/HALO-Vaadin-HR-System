package org.halocambodia.upload.audit;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.domain.UploadSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadAuditService {
    private final UploadAuditRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UploadSession session, UploadAuditAction action,
                       Long bytesProcessed, boolean success, String message) {
        record(session == null ? null : session.getUploadUuid(),
                session == null ? null : session.getOriginalFileName(),
                action, bytesProcessed, success, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID uploadId, String fileName, UploadAuditAction action,
                       Long bytesProcessed, boolean success, String message) {
        repository.save(UploadAuditEvent.builder()
                .uploadUuid(uploadId)
                .fileName(fileName)
                .action(action)
                .username(currentUsername())
                .bytesProcessed(bytesProcessed)
                .success(success)
                .message(truncate(message, 2000))
                .build());
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
