package org.halocambodia.upload.thumbnail;

import java.nio.file.Path;

public record ThumbnailDescriptor(
        boolean available,
        Path path,
        String mimeType,
        String etag,
        long lastModified) {

    public static ThumbnailDescriptor unavailable() {
        return new ThumbnailDescriptor(false, null, null, null, 0L);
    }
}
