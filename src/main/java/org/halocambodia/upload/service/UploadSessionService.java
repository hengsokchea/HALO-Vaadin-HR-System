package org.halocambodia.upload.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.halocambodia.upload.audit.UploadAuditAction;
import org.halocambodia.upload.audit.UploadAuditService;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.domain.UploadType;
import org.halocambodia.upload.dto.UploadCompleteResponse;
import org.halocambodia.upload.dto.UploadInitRequest;
import org.halocambodia.upload.dto.UploadInitResponse;
import org.halocambodia.upload.dto.UploadStatusResponse;
import org.halocambodia.upload.exception.UploadException;
import org.halocambodia.upload.quota.UploadQuotaService;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.halocambodia.upload.security.MimeAndExtensionValidator;
import org.halocambodia.upload.util.ChecksumUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadSessionService {

    private final UploadSessionRepository repository;
    private final UploadStorageService storageService;
    private final UploadProperties properties;
    private final UploadQuotaService quotaService;
    private final UploadAuditService auditService;
    private final MimeAndExtensionValidator mimeAndExtensionValidator;

    /*
     * Prevent two HTTP requests from writing/finalizing the same upload session
     * concurrently inside this application instance.
     */
    private final Map<UUID, Object> uploadLocks = new ConcurrentHashMap<>();

    @Transactional
    public UploadInitResponse initialize(UploadInitRequest request) {
        validateInitRequest(request);

        /*
         * Central file-type validation.
         * This validator checks the extension and declared MIME type together.
         * Keep this as the single source of truth to avoid duplicate allowlists.
         */
        mimeAndExtensionValidator.validate(
                request.fileName(),
                request.mimeType(),
                request.fileSize()
        );

        String ownerUsername = currentUsername();
        quotaService.assertCanInitialize(ownerUsername, request.fileSize());

        long chunkSize = normalizeChunkSize(request.requestedChunkSize());
        int totalChunks = Math.toIntExact(
                (request.fileSize() + chunkSize - 1L) / chunkSize
        );

        UUID uploadId = UUID.randomUUID();
        Path temporaryDirectory = storageService.createSessionDirectory(uploadId);

        UploadSession session = UploadSession.builder()
                .uploadUuid(uploadId)
                .originalFileName(sanitizeOriginalFileName(request.fileName()))
                .mimeType(normalizeMimeType(request.mimeType()))
                .totalSize(request.fileSize())
                .uploadedSize(0L)
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .receivedChunks(0)
                .status(UploadStatus.PENDING)
                .uploadType(
                        request.uploadType() == null
                                ? UploadType.OTHER
                                : request.uploadType()
                )
                .checksumSha256(normalizeChecksum(request.checksumSha256()))
                .temporaryPath(temporaryDirectory.toString())
                .ownerUsername(ownerUsername)
                .expiresAt(OffsetDateTime.now().plus(properties.getSessionTtl()))
                .build();

        UploadSession saved = repository.save(session);

        auditService.record(
                saved,
                UploadAuditAction.INITIALIZED,
                0L,
                true,
                "Upload session initialized"
        );

        log.info(
                "Upload initialized: uploadId={}, fileName={}, totalSize={}, "
                        + "chunkSize={}, totalChunks={}, owner={}",
                saved.getUploadUuid(),
                saved.getOriginalFileName(),
                saved.getTotalSize(),
                saved.getChunkSize(),
                saved.getTotalChunks(),
                saved.getOwnerUsername()
        );

        return toInitResponse(saved);
    }

    @Transactional
    public UploadStatusResponse receiveChunk(
            UUID uploadId,
            int chunkIndex,
            InputStream inputStream,
            long declaredContentLength,
            String chunkChecksum) {

        Object lock = uploadLocks.computeIfAbsent(uploadId, ignored -> new Object());

        synchronized (lock) {
            UploadSession session = getOwnedSession(uploadId);

            validateWritableSession(session);
            validateChunkIndex(session, chunkIndex);

            long expectedSize = expectedChunkSize(session, chunkIndex);

            /*
             * The controller should pass MultipartFile#getSize(), not the entire
             * multipart HTTP request Content-Length.
             */
            if (declaredContentLength > 0L
                    && declaredContentLength != expectedSize) {
                throw new UploadException(
                        HttpStatus.BAD_REQUEST,
                        "Chunk content length does not match the expected size. "
                                + "Expected " + expectedSize
                                + " bytes but received "
                                + declaredContentLength + " bytes"
                );
            }

            /*
             * Idempotent retry: if the same valid chunk already exists, do not
             * append or overwrite unnecessarily.
             */
            if (storageService.chunkExists(uploadId, chunkIndex)) {
                long existingSize = storageService.chunkSize(uploadId, chunkIndex);

                if (existingSize == expectedSize) {
                    session.setStatus(UploadStatus.UPLOADING);
                    session.setFailureMessage(null);
                    session.setExpiresAt(
                            OffsetDateTime.now().plus(properties.getSessionTtl())
                    );

                    recalculateProgress(session);
                    UploadSession saved = repository.save(session);

                    log.debug(
                            "Chunk already exists and is valid: uploadId={}, "
                                    + "chunkIndex={}, chunkSize={}",
                            uploadId,
                            chunkIndex,
                            existingSize
                    );

                    return toStatusResponse(saved);
                }

                log.warn(
                        "Existing chunk has invalid size and will be replaced: "
                                + "uploadId={}, chunkIndex={}, existingSize={}, "
                                + "expectedSize={}",
                        uploadId,
                        chunkIndex,
                        existingSize,
                        expectedSize
                );
            }

            long written = storageService.writeChunk(
                    uploadId,
                    chunkIndex,
                    inputStream,
                    expectedSize
            );

            if (written != expectedSize) {
                throw new UploadException(
                        HttpStatus.BAD_REQUEST,
                        "Received chunk size does not match the expected size. "
                                + "Expected " + expectedSize
                                + " bytes but wrote " + written + " bytes"
                );
            }

            verifyChunkChecksum(uploadId, chunkIndex, chunkChecksum);

            session.setStatus(UploadStatus.UPLOADING);
            session.setFailureMessage(null);
            session.setExpiresAt(
                    OffsetDateTime.now().plus(properties.getSessionTtl())
            );

            recalculateProgress(session);

            UploadSession saved = repository.save(session);

            auditService.record(
                    saved,
                    UploadAuditAction.CHUNK_RECEIVED,
                    written,
                    true,
                    "Chunk " + chunkIndex + " stored"
            );

            log.debug(
                    "Chunk stored: uploadId={}, chunkIndex={}, written={}, "
                            + "receivedChunks={}/{}, uploadedSize={}/{}",
                    uploadId,
                    chunkIndex,
                    written,
                    intValue(saved.getReceivedChunks()),
                    intValue(saved.getTotalChunks()),
                    longValue(saved.getUploadedSize()),
                    longValue(saved.getTotalSize())
            );

            return toStatusResponse(saved);
        }
    }

    @Transactional(readOnly = true)
    public UploadStatusResponse status(UUID uploadId) {
        UploadSession session = getOwnedSession(uploadId);
        return toStatusResponse(session);
    }

    @Transactional
    public UploadCompleteResponse complete(UUID uploadId) {
        Object lock = uploadLocks.computeIfAbsent(uploadId, ignored -> new Object());

        synchronized (lock) {
            UploadSession session = getOwnedSession(uploadId);

            if (session.getStatus() == UploadStatus.COMPLETED) {
                return toCompleteResponse(session);
            }

            validateWritableSession(session);
            recalculateProgress(session);

            /*
             * IMPORTANT:
             * UploadSession uses Integer and Long wrapper fields. Comparing those
             * wrappers directly with != may compare object references. Convert
             * them to primitives first.
             */
            int receivedChunks = intValue(session.getReceivedChunks());
            int totalChunks = intValue(session.getTotalChunks());
            long uploadedSize = longValue(session.getUploadedSize());
            long totalSize = longValue(session.getTotalSize());

            int firstMissingChunk = firstMissingChunkIndex(session);

            if (receivedChunks != totalChunks
                    || uploadedSize != totalSize
                    || firstMissingChunk < totalChunks) {

                String message = String.format(
                        "Upload is incomplete: %d/%d chunks, %d/%d bytes; "
                                + "first missing chunk index: %d",
                        receivedChunks,
                        totalChunks,
                        uploadedSize,
                        totalSize,
                        firstMissingChunk
                );

                log.warn(
                        "{}; uploadId={}, fileName={}",
                        message,
                        uploadId,
                        session.getOriginalFileName()
                );

                session.setFailureMessage(message);
                repository.save(session);

                auditService.record(
                        session,
                        UploadAuditAction.FAILED,
                        uploadedSize,
                        false,
                        message
                );

                throw new UploadException(HttpStatus.CONFLICT, message);
            }

            Path finalPath = storageService.mergeChunks(session);

            try {
                String actualChecksum = ChecksumUtil.sha256(finalPath);

                if (session.getChecksumSha256() != null
                        && !actualChecksum.equalsIgnoreCase(
                                session.getChecksumSha256())) {

                    storageService.deleteFinalFile(finalPath.toString());

                    session.setStatus(UploadStatus.FAILED);
                    session.setFailureMessage(
                            "SHA-256 checksum verification failed"
                    );
                    repository.save(session);

                    auditService.record(
                            session,
                            UploadAuditAction.FAILED,
                            uploadedSize,
                            false,
                            "SHA-256 checksum verification failed"
                    );

                    throw new UploadException(
                            HttpStatus.CONFLICT,
                            "SHA-256 checksum verification failed"
                    );
                }

                if (properties.isChecksumRequired()
                        && session.getChecksumSha256() == null) {

                    storageService.deleteFinalFile(finalPath.toString());

                    throw new UploadException(
                            HttpStatus.BAD_REQUEST,
                            "A SHA-256 checksum is required for this server"
                    );
                }

                session.setChecksumSha256(actualChecksum);

            } catch (IOException exception) {
                try {
                    storageService.deleteFinalFile(finalPath.toString());
                } catch (Exception cleanupException) {
                    log.warn(
                            "Could not delete failed final file: {}",
                            finalPath,
                            cleanupException
                    );
                }

                throw new UploadException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Cannot calculate final file checksum",
                        exception
                );
            }

            session.setStoredFileName(storageService.storedFileName(finalPath));
            session.setFinalPath(finalPath.toString());
            session.setStatus(UploadStatus.COMPLETED);
            session.setUploadedSize(totalSize);
            session.setReceivedChunks(totalChunks);
            session.setFailureMessage(null);

            UploadSession saved = repository.save(session);

            /*
             * Delete temporary chunk files only after the database marks the
             * final file as completed.
             */
            storageService.deleteSessionFiles(uploadId);
            uploadLocks.remove(uploadId);

            auditService.record(
                    saved,
                    UploadAuditAction.COMPLETED,
                    totalSize,
                    true,
                    "Upload completed"
            );

            log.info(
                    "Upload completed: uploadId={}, fileName={}, storedFileName={}, "
                            + "size={}, checksum={}",
                    saved.getUploadUuid(),
                    saved.getOriginalFileName(),
                    saved.getStoredFileName(),
                    saved.getTotalSize(),
                    saved.getChecksumSha256()
            );

            return toCompleteResponse(saved);
        }
    }

    @Transactional
    public void cancel(UUID uploadId) {
        Object lock = uploadLocks.computeIfAbsent(uploadId, ignored -> new Object());

        synchronized (lock) {
            UploadSession session = getOwnedSession(uploadId);

            if (session.getStatus() == UploadStatus.COMPLETED) {
                throw new UploadException(
                        HttpStatus.CONFLICT,
                        "A completed upload cannot be cancelled"
                );
            }

            storageService.deleteSessionFiles(uploadId);

            session.setStatus(UploadStatus.CANCELLED);
            session.setFailureMessage("Cancelled by user");

            UploadSession saved = repository.save(session);

            auditService.record(
                    saved,
                    UploadAuditAction.CANCELLED,
                    longValue(saved.getUploadedSize()),
                    true,
                    "Upload cancelled"
            );

            uploadLocks.remove(uploadId);

            log.info(
                    "Upload cancelled: uploadId={}, fileName={}, owner={}",
                    uploadId,
                    saved.getOriginalFileName(),
                    saved.getOwnerUsername()
            );
        }
    }

    @Transactional
    public void expire(UploadSession session) {
        if (session == null || session.getUploadUuid() == null) {
            return;
        }

        UUID uploadId = session.getUploadUuid();

        storageService.deleteSessionFiles(uploadId);

        session.setStatus(UploadStatus.EXPIRED);
        session.setFailureMessage("Upload session expired");

        UploadSession saved = repository.save(session);

        auditService.record(
                saved,
                UploadAuditAction.EXPIRED,
                longValue(saved.getUploadedSize()),
                true,
                "Upload session expired"
        );

        uploadLocks.remove(uploadId);
    }

    private void recalculateProgress(UploadSession session) {
        long uploadedSize = 0L;
        int receivedChunks = 0;

        int totalChunks = intValue(session.getTotalChunks());

        for (int chunkIndex = 0;
                chunkIndex < totalChunks;
                chunkIndex++) {

            if (storageService.chunkExists(
                    session.getUploadUuid(),
                    chunkIndex)) {

                long size = storageService.chunkSize(
                        session.getUploadUuid(),
                        chunkIndex
                );

                uploadedSize += size;
                receivedChunks++;
            }
        }

        session.setUploadedSize(uploadedSize);
        session.setReceivedChunks(receivedChunks);
    }

    private void verifyChunkChecksum(
            UUID uploadId,
            int chunkIndex,
            String chunkChecksum) {

        if (chunkChecksum == null || chunkChecksum.isBlank()) {
            return;
        }

        String normalizedChunkChecksum = normalizeChecksum(chunkChecksum);

        try {
            String actualChunkChecksum = ChecksumUtil.sha256(
                    storageService.chunkPath(uploadId, chunkIndex)
            );

            if (!actualChunkChecksum.equalsIgnoreCase(
                    normalizedChunkChecksum)) {

                throw new UploadException(
                        HttpStatus.CONFLICT,
                        "Chunk SHA-256 checksum verification failed"
                );
            }

        } catch (IOException exception) {
            throw new UploadException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot calculate chunk checksum",
                    exception
            );
        }
    }

    private void validateInitRequest(UploadInitRequest request) {
        if (request == null) {
            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "Upload initialization request is required"
            );
        }

        if (request.fileName() == null || request.fileName().isBlank()) {
            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "File name is required"
            );
        }

        if (request.fileSize() <= 0L) {
            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "File size must be greater than zero"
            );
        }

        if (request.fileSize() > properties.getMaxFileSize()) {
            throw new UploadException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds the configured maximum upload size"
            );
        }

        if (properties.isChecksumRequired()
                && (request.checksumSha256() == null
                || request.checksumSha256().isBlank())) {

            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "SHA-256 checksum is required"
            );
        }
    }

    private long normalizeChunkSize(Long requestedChunkSize) {
        long size = requestedChunkSize == null
                ? properties.getDefaultChunkSize()
                : requestedChunkSize.longValue();

        if (size <= 0L || size > properties.getMaxChunkSize()) {
            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid requested chunk size"
            );
        }

        return size;
    }

    private UploadSession getOwnedSession(UUID uploadId) {
        UploadSession session = repository.findByUploadUuid(uploadId)
                .orElseThrow(() -> new UploadException(
                        HttpStatus.NOT_FOUND,
                        "Upload session not found"
                ));

        String username = currentUsername();

        if (session.getOwnerUsername() != null
                && !session.getOwnerUsername().equals(username)) {

            throw new UploadException(
                    HttpStatus.FORBIDDEN,
                    "You do not own this upload session"
            );
        }

        return session;
    }

    private void validateWritableSession(UploadSession session) {
        if (session.getExpiresAt() != null
                && session.getExpiresAt().isBefore(OffsetDateTime.now())) {

            throw new UploadException(
                    HttpStatus.GONE,
                    "Upload session has expired"
            );
        }

        if (session.getStatus() == UploadStatus.COMPLETED) {
            throw new UploadException(
                    HttpStatus.CONFLICT,
                    "Upload session is already completed"
            );
        }

        if (session.getStatus() == UploadStatus.CANCELLED
                || session.getStatus() == UploadStatus.EXPIRED
                || session.getStatus() == UploadStatus.FAILED
                || session.getStatus() == UploadStatus.DELETED) {

            throw new UploadException(
                    HttpStatus.CONFLICT,
                    "Upload session is not writable: "
                            + session.getStatus()
            );
        }
    }

    private void validateChunkIndex(
            UploadSession session,
            int chunkIndex) {

        int totalChunks = intValue(session.getTotalChunks());

        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "Chunk index is outside the valid range. "
                            + "Valid indexes are 0 to "
                            + Math.max(0, totalChunks - 1)
            );
        }
    }

    private long expectedChunkSize(
            UploadSession session,
            int chunkIndex) {

        long chunkSize = longValue(session.getChunkSize());
        long totalSize = longValue(session.getTotalSize());
        long start = Math.multiplyExact((long) chunkIndex, chunkSize);

        return Math.min(chunkSize, totalSize - start);
    }

    private String sanitizeOriginalFileName(String fileName) {
        String normalized = fileName.replace('\\', '/');
        String baseName = normalized.substring(
                normalized.lastIndexOf('/') + 1
        ).trim();

        if (baseName.isBlank()
                || baseName.equals(".")
                || baseName.equals("..")) {

            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file name"
            );
        }

        return baseName.length() > 500
                ? baseName.substring(0, 500)
                : baseName;
    }

    private String normalizeChecksum(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return null;
        }

        String value = checksum.trim().toLowerCase();

        if (!value.matches("[0-9a-f]{64}")) {
            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "SHA-256 checksum must contain exactly "
                            + "64 hexadecimal characters"
            );
        }

        return value;
    }

    private int firstMissingChunkIndex(UploadSession session) {
        int totalChunks = intValue(session.getTotalChunks());

        for (int chunkIndex = 0;
                chunkIndex < totalChunks;
                chunkIndex++) {

            if (!storageService.chunkExists(
                    session.getUploadUuid(),
                    chunkIndex)) {

                return chunkIndex;
            }
        }

        return totalChunks;
    }

    private String currentUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return "anonymous";
        }

        return authentication.getName();
    }

    private UploadInitResponse toInitResponse(UploadSession session) {
        return new UploadInitResponse(
                session.getUploadUuid(),
                longValue(session.getChunkSize()),
                intValue(session.getTotalChunks()),
                longValue(session.getUploadedSize()),
                firstMissingChunkIndex(session),
                session.getStatus(),
                session.getExpiresAt()
        );
    }

    private UploadStatusResponse toStatusResponse(UploadSession session) {
        return new UploadStatusResponse(
                session.getUploadUuid(),
                session.getOriginalFileName(),
                session.getMimeType(),
                longValue(session.getTotalSize()),
                longValue(session.getUploadedSize()),
                longValue(session.getChunkSize()),
                intValue(session.getTotalChunks()),
                intValue(session.getReceivedChunks()),
                firstMissingChunkIndex(session),
                session.getProgressPercent(),
                session.getStatus(),
                session.getFailureMessage(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getExpiresAt()
        );
    }

    private UploadCompleteResponse toCompleteResponse(UploadSession session) {
        String uploadId = session.getUploadUuid().toString();

        return new UploadCompleteResponse(
                session.getUploadUuid(),
                session.getOriginalFileName(),
                session.getStoredFileName(),
                session.getMimeType(),
                longValue(session.getTotalSize()),
                session.getChecksumSha256(),
                session.getStatus(),
                "/api/uploads/" + uploadId + "/download",
                "/api/uploads/" + uploadId + "/preview"
        );
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }

        return mimeType.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private static long longValue(Long value) {
        return value == null ? 0L : value.longValue();
    }
}