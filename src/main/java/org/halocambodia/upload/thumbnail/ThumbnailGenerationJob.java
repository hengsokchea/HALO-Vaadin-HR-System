package org.halocambodia.upload.thumbnail;

import java.util.UUID;

public record ThumbnailGenerationJob(
        UUID uploadUuid,
        int width,
        int height,
        int attempt) {

    public ThumbnailGenerationJob retry() {
        return new ThumbnailGenerationJob(
            uploadUuid,
            width,
            height,
            attempt + 1
        );
    }
}
