package org.halocambodia.upload.admin;

public record UploadAdminSummary(
        long total,
        long active,
        long completed,
        long failed,
        long cancelled,
        long expired
) {
}
