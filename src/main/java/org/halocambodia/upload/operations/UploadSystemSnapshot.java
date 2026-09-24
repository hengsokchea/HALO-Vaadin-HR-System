package org.halocambodia.upload.operations;

import java.time.OffsetDateTime;

public record UploadSystemSnapshot(
        long totalUploads,
        long activeUploads,
        long completedUploads,
        long failedUploads,
        long deletedUploads,
        long completedBytes,
        long physicalFileCount,
        long physicalFileBytes,
        long thumbnailCount,
        long thumbnailBytes,
        long freeDiskBytes,
        long usableDiskBytes,
        int thumbnailQueueSize,
        int thumbnailActiveJobs,
        OffsetDateTime generatedAt) {
}
