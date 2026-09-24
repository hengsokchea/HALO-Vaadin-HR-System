package org.halocambodia.upload.util;

import org.halocambodia.upload.constants.UploadConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class MimeTypeUtil {

    private MimeTypeUtil() {
    }

    public static String normalize(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "application/octet-stream";
        }
        int semicolon = mimeType.indexOf(';');
        String normalized = semicolon >= 0 ? mimeType.substring(0, semicolon) : mimeType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    public static String detect(Path path, String clientMimeType) throws IOException {
        String detected = Files.probeContentType(path);
        if (detected != null && !detected.isBlank()) {
            return normalize(detected);
        }
        return normalize(clientMimeType);
    }

    public static boolean isAllowedByDefault(String mimeType) {
        return UploadConstants.DEFAULT_ALLOWED_MIME_TYPES.contains(normalize(mimeType));
    }
}
