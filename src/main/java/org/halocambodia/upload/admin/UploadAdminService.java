package org.halocambodia.upload.admin;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.audit.UploadAuditEvent;
import org.halocambodia.upload.audit.UploadAuditRepository;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UploadAdminService {

    private final UploadSessionRepository sessionRepository;
    private final UploadAuditRepository auditRepository;
    private final UploadAdminOperationService operationService;

    public Page<UploadSession> findSessions(String searchText, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(pageSize, 1));
        if (searchText == null || searchText.isBlank()) {
            return sessionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        String search = searchText.trim();
        return sessionRepository
                .findByOwnerUsernameContainingIgnoreCaseOrOriginalFileNameContainingIgnoreCaseOrderByCreatedAtDesc(
                        search, search, pageable);
    }

    public List<UploadAuditEvent> findAuditEvents(UUID uploadUuid) {
        return auditRepository.findTop100ByUploadUuidOrderByCreatedAtDesc(uploadUuid);
    }

    public UploadAdminSummary summary() {
        return operationService.summary();
    }

    @Transactional
    public void cancel(UUID uploadUuid, boolean deleteTemporaryFiles) {
        operationService.cancel(uploadUuid, deleteTemporaryFiles);
    }

    @Transactional
    public void reactivate(UUID uploadUuid) {
        operationService.reactivate(uploadUuid);
    }
}
