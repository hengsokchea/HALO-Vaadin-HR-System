package org.halocambodia.upload.attachment;

import org.halocambodia.upload.domain.UploadType;

import java.util.UUID;

/**
 * Framework-neutral metadata returned after a completed upload is attached
 * to a business record such as a policy, employee, asset, or procurement item.
 */
public record AttachmentMetadata(
        UUID uploadUuid,
        String originalFileName,
        String storedFileName,
        String mimeType,
        long fileSize,
        String checksumSha256,
        UploadType uploadType,
        String ownerUsername,
        String finalPath) {
}
