package org.halocambodia.upload.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.exception.UploadException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadStorageService {

    private static final int COPY_BUFFER_SIZE = 1024 * 1024;

    private final UploadProperties properties;

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(tempRoot());
            Files.createDirectories(finalRoot());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize upload directories", exception);
        }
    }

    public Path createSessionDirectory(UUID uploadId) {
        Path directory = resolveInside(tempRoot(), uploadId.toString());
        try {
            Files.createDirectories(directory);
            return directory;
        } catch (IOException exception) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot create upload session directory", exception);
        }
    }

    public long writeChunk(UUID uploadId, int chunkIndex, InputStream inputStream, long maximumBytes) {
        Path sessionDirectory = createSessionDirectory(uploadId);
        Path target = resolveInside(sessionDirectory, chunkFileName(chunkIndex));
        Path partial = resolveInside(sessionDirectory, chunkFileName(chunkIndex) + ".partial");

        try {
            long written = copyLimited(inputStream, partial, maximumBytes);
            moveReplacing(partial, target);
            return written;
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(partial);
            } catch (IOException ignored) {
                log.warn("Could not delete partial chunk {}", partial);
            }
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot store upload chunk " + chunkIndex, exception);
        }
    }

    public boolean chunkExists(UUID uploadId, int chunkIndex) {
        return Files.isRegularFile(resolveInside(sessionDirectory(uploadId), chunkFileName(chunkIndex)));
    }

    public Path chunkPath(UUID uploadId, int chunkIndex) {
        return resolveInside(sessionDirectory(uploadId), chunkFileName(chunkIndex));
    }

    public long chunkSize(UUID uploadId, int chunkIndex) {
        Path path = chunkPath(uploadId, chunkIndex);
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot read chunk size", exception);
        }
    }

    public Path mergeChunks(UploadSession session) {
        Path sessionDirectory = sessionDirectory(session.getUploadUuid());
        if (!Files.isDirectory(sessionDirectory)) {
            throw new UploadException(HttpStatus.CONFLICT, "Upload session directory does not exist");
        }

        String typeFolder =
                session.getUploadType()
                        .name()
                        .toLowerCase(Locale.ROOT);

        Path destinationDirectory =
                resolveInside(
                        finalRoot(),
                        typeFolder
                );
        
        
        String storedName = buildStoredFileName(session.getUploadUuid(), session.getOriginalFileName());
        Path finalPath = resolveInside(destinationDirectory, storedName);
        Path partialPath = resolveInside(destinationDirectory, storedName + ".partial");

        try {
            Files.createDirectories(destinationDirectory);
            try (OutputStream output = Files.newOutputStream(partialPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[COPY_BUFFER_SIZE];
                for (int index = 0; index < session.getTotalChunks(); index++) {
                    Path chunk = resolveInside(sessionDirectory, chunkFileName(index));
                    if (!Files.isRegularFile(chunk)) {
                        throw new UploadException(HttpStatus.CONFLICT,
                                "Missing chunk " + index + " of " + session.getTotalChunks());
                    }
                    try (InputStream input = Files.newInputStream(chunk)) {
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                    }
                }
            }

            long mergedSize = Files.size(partialPath);
            if (mergedSize != session.getTotalSize()) {
                Files.deleteIfExists(partialPath);
                throw new UploadException(HttpStatus.CONFLICT,
                        "Merged file size does not match the expected size");
            }

            moveReplacing(partialPath, finalPath);
            return finalPath;
        } catch (UploadException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot merge upload chunks", exception);
        }
    }

    public void deleteSessionFiles(UUID uploadId) {
        deleteRecursively(sessionDirectory(uploadId));
    }

    public void deleteFinalFile(String finalPath) {
        if (finalPath == null || finalPath.isBlank()) {
            return;
        }
        Path path = Path.of(finalPath).toAbsolutePath().normalize();
        if (!path.startsWith(finalRoot())) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Invalid final file path");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot delete final upload file", exception);
        }
    }

    public String storedFileName(Path finalPath) {
        return finalPath.getFileName().toString();
    }

    private long copyLimited(InputStream input, Path target, long maximumBytes) throws IOException {
        long total = 0;
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        try (OutputStream output = Files.newOutputStream(target,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maximumBytes) {
                    throw new UploadException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "Chunk is larger than the expected chunk size");
                }
                output.write(buffer, 0, read);
            }
        }
        return total;
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    log.warn("Could not delete {}", path, exception);
                }
            });
        } catch (IOException exception) {
            log.warn("Could not clean upload directory {}", directory, exception);
        }
    }

    private Path tempRoot() {
        return properties.getRootDirectory().resolve("temp").toAbsolutePath().normalize();
    }

    private Path finalRoot() {
        return properties.getRootDirectory().resolve("files").toAbsolutePath().normalize();
    }

    private Path sessionDirectory(UUID uploadId) {
        return resolveInside(tempRoot(), uploadId.toString());
    }

    private Path resolveInside(Path root, String... children) {
        Path resolved = root;
        for (String child : children) {
            resolved = resolved.resolve(child);
        }
        resolved = resolved.toAbsolutePath().normalize();
        if (!resolved.startsWith(root.toAbsolutePath().normalize())) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Invalid upload path");
        }
        return resolved;
    }

    private String chunkFileName(int chunkIndex) {
        return String.format("chunk-%08d.part", chunkIndex);
    }

    private String buildStoredFileName(UUID uploadId, String originalFileName) {
        String extension = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot >= 0 && dot < originalFileName.length() - 1) {
            extension = sanitizeExtension(originalFileName.substring(dot + 1));
        }
        return extension.isBlank() ? uploadId.toString() : uploadId + "." + extension;
    }

    private String sanitizeExtension(String extension) {
        return extension.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
