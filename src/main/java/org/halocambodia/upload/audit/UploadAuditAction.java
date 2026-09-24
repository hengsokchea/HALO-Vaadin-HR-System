package org.halocambodia.upload.audit;

public enum UploadAuditAction {
    INITIALIZED,
    CHUNK_RECEIVED,
    COMPLETED,
    CANCELLED,
    FAILED,
    EXPIRED,
    RECOVERED,
    DOWNLOADED,
    PREVIEWED,
    SOFT_DELETED,
    RESTORED,
    PURGED,
    AUDIT_EXPORTED
}
