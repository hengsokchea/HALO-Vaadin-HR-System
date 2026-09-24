package org.halocambodia.upload.thumbnail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThumbnailAdminService {

    private final ThumbnailService thumbnailService;

    public ThumbnailDescriptor regenerate(UUID uploadUuid, Integer width, Integer height) {
        delete(uploadUuid, width, height);
        return thumbnailService.getOrCreate(uploadUuid, width, height);
    }

    public boolean delete(UUID uploadUuid, Integer width, Integer height) {
        try {
            return Files.deleteIfExists(thumbnailService.resolveThumbnailPath(uploadUuid, width, height));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot delete thumbnail", exception);
        }
    }
}
