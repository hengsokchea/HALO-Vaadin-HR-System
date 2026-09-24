package org.halocambodia.upload.exception;

import java.time.OffsetDateTime;

public record UploadErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
