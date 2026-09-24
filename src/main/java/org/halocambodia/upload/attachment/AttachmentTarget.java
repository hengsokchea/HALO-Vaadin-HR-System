package org.halocambodia.upload.attachment;

import org.halocambodia.upload.domain.UploadType;

/** Identifies the business record receiving an uploaded file. */
public record AttachmentTarget(
        UploadType uploadType,
        String entityType,
        String entityId) {

    public AttachmentTarget {
        if (uploadType == null) {
            throw new IllegalArgumentException("uploadType is required");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType is required");
        }
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entityId is required");
        }
    }
}
