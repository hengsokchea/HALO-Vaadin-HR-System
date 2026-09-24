package org.halocambodia.upload.streaming;

import java.nio.file.Path;
import java.util.UUID;

public record StreamedFile(
        UUID uploadUuid,
        Path path,
        String originalFileName,
        String declaredMimeType,
        String checksumSha256) {
}
