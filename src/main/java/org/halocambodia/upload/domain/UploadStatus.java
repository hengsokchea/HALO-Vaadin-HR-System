package org.halocambodia.upload.domain;

public enum UploadStatus {
    PENDING,
    UPLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
    DELETED
}
