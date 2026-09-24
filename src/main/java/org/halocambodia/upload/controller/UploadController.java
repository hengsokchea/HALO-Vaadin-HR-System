package org.halocambodia.upload.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.dto.UploadCompleteResponse;
import org.halocambodia.upload.dto.UploadInitRequest;
import org.halocambodia.upload.dto.UploadInitResponse;
import org.halocambodia.upload.dto.UploadStatusResponse;
import org.halocambodia.upload.service.UploadSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadSessionService uploadSessionService;

    @PostMapping("/init")
    public ResponseEntity<UploadInitResponse> initialize(
            @Valid @RequestBody UploadInitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploadSessionService.initialize(request));
    }

    @PostMapping(path = "/{uploadId}/chunks/{chunkIndex}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadStatusResponse uploadChunk(
            @PathVariable UUID uploadId,
            @PathVariable int chunkIndex,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Chunk-SHA256", required = false) String chunkChecksum)
            throws IOException {
        return uploadSessionService.receiveChunk(
                uploadId,
                chunkIndex,
                file.getInputStream(),
                file.getSize(),
                chunkChecksum
        );
    }

    @GetMapping("/{uploadId}/status")
    public UploadStatusResponse status(@PathVariable UUID uploadId) {
        return uploadSessionService.status(uploadId);
    }

    @PostMapping("/{uploadId}/complete")
    public UploadCompleteResponse complete(@PathVariable UUID uploadId) {
        return uploadSessionService.complete(uploadId);
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID uploadId) {
        uploadSessionService.cancel(uploadId);
        return ResponseEntity.noContent().build();
    }
}
