package org.halocambodia.upload.model;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompletedFileMetadata(
        UUID uploadId,
        String originalFileName,
        String storedFileName,
        String mimeType,
        long fileSize,
        String checksumSha256,
        Path path,
        String ownerUsername,
        OffsetDateTime completedAt
) {
}
