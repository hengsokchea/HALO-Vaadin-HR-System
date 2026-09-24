package org.halocambodia.upload.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record UploadChunkRequest(
        @NotNull UUID uploadId,
        @PositiveOrZero int chunkIndex,
        @PositiveOrZero long chunkStart,
        @PositiveOrZero long chunkEnd,
        String chunkChecksumSha256
) {
}
