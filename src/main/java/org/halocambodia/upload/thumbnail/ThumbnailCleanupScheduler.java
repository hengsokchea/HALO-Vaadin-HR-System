package org.halocambodia.upload.thumbnail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThumbnailCleanupScheduler {

    private final ThumbnailProperties thumbnailProperties;
    private final ThumbnailJobProperties jobProperties;

    @Scheduled(
        fixedDelayString =
            "${app.thumbnail.jobs.cleanup-interval:PT6H}"
    )
    public void cleanup() {
        if (!jobProperties.enabled()) {
            return;
        }

        Path root = thumbnailProperties.directory()
            .toAbsolutePath()
            .normalize();

        if (!Files.isDirectory(root)) {
            return;
        }

        Instant cutoff =
            Instant.now().minus(jobProperties.retention());

        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> olderThan(path, cutoff))
                .forEach(this::deleteQuietly);
        } catch (IOException exception) {
            log.error(
                "Thumbnail cleanup failed for {}",
                root,
                exception
            );
        }

        removeEmptyDirectories(root);
    }

    private boolean olderThan(
            Path path,
            Instant cutoff) {

        try {
            return Files.getLastModifiedTime(path)
                .toInstant()
                .isBefore(cutoff);
        } catch (IOException exception) {
            log.warn(
                "Cannot inspect thumbnail {}",
                path,
                exception
            );
            return false;
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn(
                "Cannot delete old thumbnail {}",
                path,
                exception
            );
        }
    }

    private void removeEmptyDirectories(Path root) {
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                .sorted(Comparator.reverseOrder())
                .filter(path -> !path.equals(root))
                .forEach(path -> {
                    try (var children = Files.list(path)) {
                        if (children.findAny().isEmpty()) {
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException exception) {
                        log.debug(
                            "Cannot remove directory {}",
                            path,
                            exception
                        );
                    }
                });
        } catch (IOException exception) {
            log.debug(
                "Cannot scan thumbnail directories",
                exception
            );
        }
    }
}