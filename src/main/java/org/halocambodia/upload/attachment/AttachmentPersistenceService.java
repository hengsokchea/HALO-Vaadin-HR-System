package org.halocambodia.upload.attachment;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.exception.UploadException;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentPersistenceService {

    private final UploadSessionRepository uploadSessionRepository;
    private final List<AttachmentPersistenceAdapter> adapters;

    @Transactional(readOnly = true)
    public Object attach(UUID uploadUuid, AttachmentTarget target) {
        UploadSession session = uploadSessionRepository.findByUploadUuid(uploadUuid)
                .orElseThrow(() -> new UploadException(HttpStatus.NOT_FOUND, "Upload session not found"));

        if (!session.isCompleted()) {
            throw new UploadException(HttpStatus.CONFLICT, "Upload is not completed");
        }
        if (session.getUploadType() != target.uploadType()) {
            throw new UploadException(HttpStatus.BAD_REQUEST,
                    "Upload type does not match the attachment target");
        }

        AttachmentPersistenceAdapter adapter = adapters.stream()
                .filter(candidate -> candidate.supports(target))
                .findFirst()
                .orElseThrow(() -> new UploadException(HttpStatus.NOT_IMPLEMENTED,
                        "No attachment persistence adapter is registered for " + target.entityType()));

        AttachmentMetadata metadata = new AttachmentMetadata(
                session.getUploadUuid(),
                session.getOriginalFileName(),
                session.getStoredFileName(),
                session.getMimeType(),
                session.getTotalSize(),
                session.getChecksumSha256(),
                session.getUploadType(),
                session.getOwnerUsername(),
                session.getFinalPath());

        return adapter.persist(target, metadata);
    }
}
