package org.halocambodia.data;

import java.util.UUID;

public record PolicyAttachmentDto(
        UUID fileUuid,
        String fileName,
        String mimeType,
        Long fileSize,
        String description,
        Integer sortOrder) {

    public boolean previewable() {
        if (mimeType == null) return false;
        String value = mimeType.toLowerCase();
        return value.equals("application/pdf")
                || value.startsWith("image/")
                || value.startsWith("video/")
                || value.startsWith("text/");
    }
}
