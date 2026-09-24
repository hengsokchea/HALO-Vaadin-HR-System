package org.halocambodia.upload.constants;

import java.time.Duration;
import java.util.Set;

public final class UploadConstants {

    private UploadConstants() {
    }

    public static final long DEFAULT_CHUNK_SIZE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_CHUNK_SIZE_BYTES = 100L * 1024 * 1024;
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024 * 1024;
    public static final Duration DEFAULT_SESSION_TTL = Duration.ofHours(24);

    public static final String TEMP_DIRECTORY_NAME = "temp";
    public static final String FINAL_DIRECTORY_NAME = "files";

    public static final Set<String> DEFAULT_ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "video/mp4",
            "audio/mpeg",
            "application/zip",
            "application/octet-stream"
    );
}
