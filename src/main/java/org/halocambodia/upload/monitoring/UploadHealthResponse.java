package org.halocambodia.upload.monitoring;

import java.nio.file.Path;
import java.util.Map;

public record UploadHealthResponse(
        String status,
        Path rootDirectory,
        long usableDiskBytes,
        long totalDiskBytes,
        long activeSessions,
        Map<String, Long> sessionsByStatus
) {}
