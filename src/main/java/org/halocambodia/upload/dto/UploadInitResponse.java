package org.halocambodia.upload.dto;

import org.halocambodia.upload.domain.UploadStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UploadInitResponse(
        UUID uploadId,
        long chunkSize,
        int totalChunks,
        long uploadedSize,
        int nextChunkIndex,
        UploadStatus status,
        OffsetDateTime expiresAt
) {
}
