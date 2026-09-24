package org.halocambodia.upload.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.halocambodia.upload.config.UploadProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fails fast when the configured upload directory cannot be created or written.
 * This prevents users from discovering a storage configuration problem only
 * after a large upload has already started.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UploadStartupValidator implements ApplicationRunner {

    private final UploadProperties properties;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Path root = properties.getRootDirectory().toAbsolutePath().normalize();
        Path chunks = root.resolve("chunks");
        Path completed = root.resolve("completed");
        Path thumbnails = root.resolve("thumbnails");

        Files.createDirectories(chunks);
        Files.createDirectories(completed);
        Files.createDirectories(thumbnails);

        verifyWritable(root);
        verifyWritable(chunks);
        verifyWritable(completed);
        verifyWritable(thumbnails);

        if (properties.getDefaultChunkSize() <= 0
                || properties.getDefaultChunkSize() > properties.getMaxChunkSize()) {
            throw new IllegalStateException(
                    "halo.upload.default-chunk-size must be greater than zero and not exceed halo.upload.max-chunk-size");
        }

        if (properties.getMaxFileSize() < properties.getDefaultChunkSize()) {
            throw new IllegalStateException(
                    "halo.upload.max-file-size must be greater than or equal to halo.upload.default-chunk-size");
        }

        log.info("HALO upload storage ready: root={}, maxFileSize={}, defaultChunkSize={}, maxActiveSessionsPerUser={}",
                root,
                properties.getMaxFileSize(),
                properties.getDefaultChunkSize(),
                properties.getMaxActiveSessionsPerUser());
    }

    private void verifyWritable(Path directory) throws IOException {
        Path probe = Files.createTempFile(directory, ".halo-upload-write-test-", ".tmp");
        try {
            Files.writeString(probe, "ok");
        } finally {
            Files.deleteIfExists(probe);
        }
    }
}
