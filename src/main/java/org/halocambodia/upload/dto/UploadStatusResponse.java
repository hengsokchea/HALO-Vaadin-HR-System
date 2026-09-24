package org.halocambodia.upload.dto;

import org.halocambodia.upload.domain.UploadStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UploadStatusResponse(
        UUID uploadId,
        String fileName,
        String mimeType,
        long totalSize,
        long uploadedSize,
        long chunkSize,
        int totalChunks,
        int receivedChunks,
        int nextChunkIndex,
        double progressPercent,
        UploadStatus status,
        String failureMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime expiresAt
) {
}
