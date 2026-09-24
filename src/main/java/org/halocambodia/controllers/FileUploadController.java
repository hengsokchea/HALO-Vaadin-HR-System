package org.halocambodia.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.halocambodia.services.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/files")
@Slf4j
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    // In-memory session store (replace with Redis for production)
    private final Map<String, UploadSession> uploadSessions = new ConcurrentHashMap<>();

    private static class UploadSession {
        Path tempPath;
        String originalFileName;
        long totalSize;
        int totalChunks;
        int receivedChunks = 0;
    }

    @PostMapping("/upload/init")
    public ResponseEntity<Map<String, Object>> initUpload(@RequestBody Map<String, Object> request) {
        String fileName = (String) request.get("fileName");
        long fileSize = Long.parseLong(request.get("fileSize").toString());

        UUID fileUuid = UUID.randomUUID();
        String tempFileName = fileUuid + "_temp";
        Path tempPath = Paths.get(fileStorageService.getTempDir(), tempFileName);

        UploadSession session = new UploadSession();
        session.tempPath = tempPath;
        session.originalFileName = fileName;
        session.totalSize = fileSize;
        session.totalChunks = (int) Math.ceil((double) fileSize / (200 * 1024 * 1024L)); // 200MB chunks

        uploadSessions.put(fileUuid.toString(), session);

        Map<String, Object> response = new HashMap<>();
        response.put("fileUuid", fileUuid.toString());
        response.put("uploadId", fileUuid.toString());
        response.put("chunkSize", 200 * 1024 * 1024);
        response.put("totalChunks", session.totalChunks);

        log.info("Upload initialized: {} ({} bytes, {} chunks)", fileName, fileSize, session.totalChunks);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload/chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("file") MultipartFile chunk) throws IOException {

        UploadSession session = uploadSessions.get(uploadId);
        if (session == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid upload session"));
        }

        // Append chunk to temp file
        try (var fos = new java.io.FileOutputStream(session.tempPath.toFile(), true)) {
            chunk.getInputStream().transferTo(fos);
        }

        session.receivedChunks++;

        Map<String, Object> response = new HashMap<>();
        response.put("chunkIndex", chunkIndex);
        response.put("received", session.receivedChunks);
        response.put("total", session.totalChunks);

        log.debug("Chunk {}/{} received for {}", chunkIndex + 1, session.totalChunks, uploadId);

        // Finalize when all chunks received
        if (session.receivedChunks == session.totalChunks) {
            finalizeUpload(uploadId, session);
        }

        return ResponseEntity.ok(response);
    }

    private void finalizeUpload(String uploadId, UploadSession session) throws IOException {
        UUID fileUuid = UUID.fromString(uploadId);
        String storedFileName = fileStorageService.finalizeLargeUpload(
                session.tempPath, session.originalFileName, fileUuid);

        log.info("Large file upload completed: {} -> {}", session.originalFileName, storedFileName);
        uploadSessions.remove(uploadId);
    }

    @GetMapping("/upload/status/{uploadId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String uploadId) {
        UploadSession session = uploadSessions.get(uploadId);
        if (session == null) return ResponseEntity.notFound().build();

        Map<String, Object> status = new HashMap<>();
        status.put("receivedChunks", session.receivedChunks);
        status.put("totalChunks", session.totalChunks);
        status.put("progress", (session.receivedChunks * 100.0) / session.totalChunks);
        return ResponseEntity.ok(status);
    }
}