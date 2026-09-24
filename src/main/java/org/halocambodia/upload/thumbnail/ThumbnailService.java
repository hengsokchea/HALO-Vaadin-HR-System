package org.halocambodia.upload.thumbnail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import org.halocambodia.upload.model.CompletedFileMetadata;
import org.halocambodia.upload.service.CompletedFileService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final CompletedFileService completedFileService;
    private final ThumbnailProperties properties;
    private final ImageThumbnailGenerator imageGenerator;
    private final PdfThumbnailGenerator pdfGenerator;
    private final VideoThumbnailGenerator videoGenerator;

   

    public ThumbnailDescriptor getOrCreate(UUID uploadUuid, Integer width, Integer height) {
        if (!properties.enabled()) {
            return ThumbnailDescriptor.unavailable();
        }

        CompletedFileMetadata file = completedFileService.metadata(uploadUuid);

        int targetWidth = normalize(width, properties.defaultWidth(), properties.maxWidth());
        int targetHeight = normalize(height, properties.defaultHeight(), properties.maxHeight());

        String fingerprint = fingerprint(file, targetWidth, targetHeight);
        Path destination = resolveThumbnailPath(file, targetWidth, targetHeight);

        try {
            if (Files.isRegularFile(destination)) {
                return descriptor(destination, fingerprint);
            }

            Files.createDirectories(destination.getParent());
            Path temporary = Files.createTempFile(
            	    destination.getParent(),
            	    fingerprint + "-",
            	    ".jpg"
            	);

            ThumbnailResult result;
            try {
                if (imageGenerator.supports(file.mimeType(), file.originalFileName())) {
                    result = imageGenerator.generate(file.path(), temporary, targetWidth, targetHeight);
                } else if (pdfGenerator.supports(file.mimeType(), file.originalFileName())) {
                    result = pdfGenerator.generate(file.path(), temporary, targetWidth, targetHeight);
                } else if (videoGenerator.supports(file.mimeType(), file.originalFileName())) {
                    result = videoGenerator.generate(file.path(), temporary, targetWidth, targetHeight);
                } else {
                    return ThumbnailDescriptor.unavailable();
                }

                if (!result.generated()) {
                    return ThumbnailDescriptor.unavailable();
                }

                try {
                    Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    );
                } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                    Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                    );
                }
            } finally {
                Files.deleteIfExists(temporary);
            }

            return descriptor(destination, fingerprint);

        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Cannot generate thumbnail",
                exception
            );
        }
    }


    public Path resolveThumbnailPath(UUID uploadUuid, Integer width, Integer height) {
        CompletedFileMetadata file = completedFileService.metadata(uploadUuid);
        int targetWidth = normalize(width, properties.defaultWidth(), properties.maxWidth());
        int targetHeight = normalize(height, properties.defaultHeight(), properties.maxHeight());
        return resolveThumbnailPath(file, targetWidth, targetHeight);
    }

    private Path resolveThumbnailPath(CompletedFileMetadata file, int width, int height) {
        String fingerprint = fingerprint(file, width, height);
        Path root = properties.directory().toAbsolutePath().normalize();
        Path destination = root
                .resolve(fingerprint.substring(0, 2))
                .resolve(fingerprint + ".jpg")
                .toAbsolutePath()
                .normalize();
        if (!destination.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid thumbnail path");
        }
        return destination;
    }

    private ThumbnailDescriptor descriptor( Path path,String etag) throws IOException {

        return new ThumbnailDescriptor(
            true,
            path,
            "image/jpeg",
            "\"" + etag + "\"",
            Files.getLastModifiedTime(path).toMillis()
        );
    }

    private int normalize(Integer requested, int defaultValue, int maximum) {
        int value = requested == null ? defaultValue : requested;
        if (value <= 0 || value > maximum) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Thumbnail dimension must be between 1 and " + maximum
            );
        }
        return value;
    }

    private String fingerprint(
            CompletedFileMetadata file,
            int width,
            int height) {

        try {
            String source = file.uploadId() + "|"
                + file.checksumSha256() + "|"
                + file.fileSize() + "|"
                + file.completedAt() + "|"
                + width + "x" + height;

            return HexFormat.of().formatHex(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                        source.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                        )
                    )
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Cannot create thumbnail fingerprint",
                exception
            );
        }
    }
    
    @PostConstruct
    public void initialize() throws IOException {
        Files.createDirectories(properties.directory());
        log.info("================================================");
        log.info("Thumbnail storage");
        log.info("Directory : {}", properties.directory().toAbsolutePath().normalize());
        log.info("Enabled   : {}", properties.enabled());
        log.info("Default   : {} x {}", properties.defaultWidth(), properties.defaultHeight());
        log.info("Maximum   : {} x {}", properties.maxWidth(), properties.maxHeight());
        log.info("================================================");
    }

}
