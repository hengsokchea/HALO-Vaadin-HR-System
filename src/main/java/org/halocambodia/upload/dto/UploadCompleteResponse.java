package org.halocambodia.upload.dto;

import org.halocambodia.upload.domain.UploadStatus;

import java.util.UUID;

public record UploadCompleteResponse(
        UUID uploadId,
        String originalFileName,
        String storedFileName,
        String mimeType,
        long fileSize,
        String checksumSha256,
        UploadStatus status,
        String downloadUrl,
        String previewUrl
) {
}
