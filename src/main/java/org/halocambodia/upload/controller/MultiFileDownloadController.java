package org.halocambodia.upload.controller;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.exception.UploadException;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Streams selected completed uploads as one ZIP archive.
 *
 * The response is generated directly to the browser and does not create a
 * temporary ZIP file on the server.
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class MultiFileDownloadController {

    private static final int MAX_FILES_PER_ARCHIVE = 100;
    private static final int BUFFER_SIZE = 64 * 1024;

    private final UploadSessionRepository repository;

    @GetMapping("/download-zip")
    @Transactional(readOnly = true)
    public void downloadZip(
            @RequestParam("uploadUuid") List<UUID> uploadUuids,
            HttpServletResponse response) throws IOException {

        if (uploadUuids == null || uploadUuids.isEmpty()) {
            throw new UploadException(HttpStatus.BAD_REQUEST, "Select at least one file");
        }
        if (uploadUuids.size() > MAX_FILES_PER_ARCHIVE) {
            throw new UploadException(
                HttpStatus.BAD_REQUEST,
                "A maximum of " + MAX_FILES_PER_ARCHIVE + " files can be downloaded at once"
            );
        }

        List<UploadSession> sessions = uploadUuids.stream()
            .distinct()
            .map(this::resolveCompletedUpload)
            .toList();

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/zip");
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=attachments.zip"
        );
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        Map<String, Integer> usedNames = new HashMap<>();
        byte[] buffer = new byte[BUFFER_SIZE];

        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream())) {
            for (UploadSession session : sessions) {
                Path path = Path.of(session.getFinalPath()).toAbsolutePath().normalize();
                if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                    throw new UploadException(
                        HttpStatus.NOT_FOUND,
                        "Physical file was not found for upload " + session.getUploadUuid()
                    );
                }

                String entryName = uniqueEntryName(
                    safeEntryName(session.getOriginalFileName()),
                    usedNames
                );

                ZipEntry entry = new ZipEntry(entryName);
                entry.setSize(Files.size(path));
                entry.setTime(Files.getLastModifiedTime(path).toMillis());
                zip.putNextEntry(entry);

                try (InputStream input = new BufferedInputStream(
                        Files.newInputStream(path), BUFFER_SIZE)) {
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        zip.write(buffer, 0, read);
                    }
                }

                zip.closeEntry();
            }
            zip.finish();
        }
    }

    private UploadSession resolveCompletedUpload(UUID uploadUuid) {
        UploadSession session = repository.findByUploadUuid(uploadUuid)
            .orElseThrow(() -> new UploadException(
                HttpStatus.NOT_FOUND,
                "Upload was not found: " + uploadUuid
            ));

        if (session.getStatus() != UploadStatus.COMPLETED) {
            throw new UploadException(
                HttpStatus.CONFLICT,
                "File is not ready: " + uploadUuid
            );
        }
        if (session.getFinalPath() == null || session.getFinalPath().isBlank()) {
            throw new UploadException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Completed upload does not contain a final path: " + uploadUuid
            );
        }
        return session;
    }

    private String safeEntryName(String originalName) {
        String name = originalName == null || originalName.isBlank()
            ? "attachment"
            : originalName.trim();

        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }

        name = name.replaceAll("[\\r\\n\\t]", "_");
        return name.isBlank() ? "attachment" : name;
    }

    private String uniqueEntryName(String requestedName, Map<String, Integer> usedNames) {
        String key = requestedName.toLowerCase(Locale.ROOT);
        int occurrence = usedNames.merge(key, 1, Integer::sum);
        if (occurrence == 1) {
            return requestedName;
        }

        int dot = requestedName.lastIndexOf('.');
        if (dot > 0) {
            return requestedName.substring(0, dot)
                + " (" + occurrence + ")"
                + requestedName.substring(dot);
        }
        return requestedName + " (" + occurrence + ")";
    }
}
