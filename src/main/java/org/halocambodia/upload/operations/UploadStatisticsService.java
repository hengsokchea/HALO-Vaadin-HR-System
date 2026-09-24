package org.halocambodia.upload.operations;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.halocambodia.upload.thumbnail.ThumbnailProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class UploadStatisticsService {

    private static final List<UploadStatus> ACTIVE = List.of(
            UploadStatus.PENDING,
            UploadStatus.UPLOADING,
            UploadStatus.PAUSED);

    private final UploadSessionRepository repository;
    private final UploadProperties uploadProperties;
    private final ThumbnailProperties thumbnailProperties;
    private final ThreadPoolTaskExecutor thumbnailExecutor;

    public UploadStatisticsService(
            UploadSessionRepository repository,
            UploadProperties uploadProperties,
            ThumbnailProperties thumbnailProperties,
            @Qualifier("thumbnailExecutor") ThreadPoolTaskExecutor thumbnailExecutor) {
        this.repository = repository;
        this.uploadProperties = uploadProperties;
        this.thumbnailProperties = thumbnailProperties;
        this.thumbnailExecutor = thumbnailExecutor;
    }

    public UploadSystemSnapshot snapshot() {
        DirectoryStats files = directoryStats(uploadProperties.getRootDirectory().resolve("files"));
        DirectoryStats thumbnails = directoryStats(thumbnailProperties.directory());
        FileStore store = fileStore(uploadProperties.getRootDirectory());

        return new UploadSystemSnapshot(
                repository.count(),
                repository.countByStatusIn(ACTIVE),
                repository.countByStatus(UploadStatus.COMPLETED),
                repository.countByStatus(UploadStatus.FAILED),
                repository.countByStatus(UploadStatus.DELETED),
                repository.sumTotalSizeByStatus(UploadStatus.COMPLETED),
                files.count(),
                files.bytes(),
                thumbnails.count(),
                thumbnails.bytes(),
                safeUnallocated(store),
                safeUsable(store),
                thumbnailExecutor.getThreadPoolExecutor() == null
                        ? 0 : thumbnailExecutor.getThreadPoolExecutor().getQueue().size(),
                thumbnailExecutor.getActiveCount(),
                OffsetDateTime.now());
    }

    private DirectoryStats directoryStats(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return new DirectoryStats(0, 0);
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            long[] values = paths.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ignored) {
                    return 0L;
                }
            }).collect(() -> new long[2], (a, size) -> {
                a[0]++;
                a[1] += size;
            }, (a, b) -> {
                a[0] += b[0];
                a[1] += b[1];
            });
            return new DirectoryStats(values[0], values[1]);
        } catch (IOException ignored) {
            return new DirectoryStats(0, 0);
        }
    }

    private FileStore fileStore(Path path) {
        try {
            Files.createDirectories(path);
            return Files.getFileStore(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect upload filesystem", exception);
        }
    }

    private long safeUsable(FileStore store) {
        try {
            return store.getUsableSpace();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private long safeUnallocated(FileStore store) {
        try {
            return store.getUnallocatedSpace();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private record DirectoryStats(long count, long bytes) {
    }
}
