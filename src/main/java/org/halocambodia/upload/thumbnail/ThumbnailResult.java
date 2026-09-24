package org.halocambodia.upload.thumbnail;

import java.nio.file.Path;

public record ThumbnailResult(
        boolean generated,
        Path path,
        String mimeType,
        String message) {

    public static ThumbnailResult generated(Path path, String mimeType) {
        return new ThumbnailResult(true, path, mimeType, null);
    }

    public static ThumbnailResult skipped(String message) {
        return new ThumbnailResult(false, null, null, message);
    }
}
