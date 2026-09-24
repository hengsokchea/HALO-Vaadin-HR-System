package org.halocambodia.upload.service;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.exception.UploadException;
import org.halocambodia.upload.model.CompletedFileMetadata;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.halocambodia.upload.security.UploadAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompletedFileService {

    private final UploadSessionRepository repository;
    private final UploadProperties properties;
    private final UploadAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public CompletedFileMetadata metadata(UUID uploadId) {
        UploadSession session = repository.findByUploadUuid(uploadId)
                .orElseThrow(() -> new UploadException(HttpStatus.NOT_FOUND, "Uploaded file not found"));
        authorizationService.requireOwnerOrAdministrator(session);

        if (session.getStatus() != UploadStatus.COMPLETED || session.getFinalPath() == null) {
            throw new UploadException(HttpStatus.CONFLICT, "Upload has not completed");
        }

        Path path = Path.of(session.getFinalPath()).toAbsolutePath().normalize();
        Path finalRoot = properties.getRootDirectory().resolve("files").toAbsolutePath().normalize();
        if (!path.startsWith(finalRoot)) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Invalid stored file path");
        }
        if (!Files.isRegularFile(path)) {
            throw new UploadException(HttpStatus.NOT_FOUND, "Stored file is missing");
        }

        long actualSize;
        try {
            actualSize = Files.size(path);
        } catch (IOException exception) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot read stored file size", exception);
        }

        return new CompletedFileMetadata(
                session.getUploadUuid(),
                session.getOriginalFileName(),
                session.getStoredFileName(),
                session.getMimeType(),
                actualSize,
                session.getChecksumSha256(),
                path,
                session.getOwnerUsername(),
                session.getUpdatedAt()
        );
    }

    public InputStream open(CompletedFileMetadata metadata) {
        try {
            return Files.newInputStream(metadata.path());
        } catch (IOException exception) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot open stored file", exception);
        }
    }
}
