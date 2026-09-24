package org.halocambodia.upload.monitoring;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.config.UploadProperties;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads/health")
@RequiredArgsConstructor
public class UploadHealthController {
    private final UploadProperties properties;
    private final UploadSessionRepository repository;

    @GetMapping
    public UploadHealthResponse health() throws IOException {
        Path root = properties.getRootDirectory();
        Files.createDirectories(root);
        FileStore store = Files.getFileStore(root);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (UploadStatus status : UploadStatus.values()) {
            byStatus.put(status.name(), repository.countByStatus(status));
        }
        long active = repository.countByStatusIn(List.of(
                UploadStatus.PENDING, UploadStatus.UPLOADING, UploadStatus.PAUSED));
        String status = store.getUsableSpace() >= properties.getMinimumFreeDiskBytes()
                ? "UP" : "DEGRADED";
        return new UploadHealthResponse(status, root, store.getUsableSpace(),
                store.getTotalSpace(), active, byStatus);
    }
}
