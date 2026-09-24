package org.halocambodia.upload.operations;

import org.halocambodia.upload.domain.UploadSession;

import java.util.List;

public record DuplicateFileGroup(
        String checksumSha256,
        long fileSize,
        long reclaimableBytes,
        List<UploadSession> uploads) {
}
