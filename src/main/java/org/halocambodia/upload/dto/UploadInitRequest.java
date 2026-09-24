package org.halocambodia.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.halocambodia.upload.domain.UploadType;

public record UploadInitRequest(
        @NotBlank String fileName,
        String mimeType,
        @NotNull @Positive Long fileSize,
        Long requestedChunkSize,
        UploadType uploadType,
        String checksumSha256
) {
}
