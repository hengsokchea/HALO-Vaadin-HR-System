package org.halocambodia.upload.reporting;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.retention.UploadRetentionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','UPLOAD_ADMIN')")
public class UploadGovernanceController {
    private final UploadRetentionService retentionService;
    private final UploadAuditCsvService csvService;

    @DeleteMapping("/{uploadUuid}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID uploadUuid, Authentication auth) {
        retentionService.softDelete(uploadUuid, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uploadUuid}/restore")
    public ResponseEntity<Void> restore(@PathVariable UUID uploadUuid, Authentication auth) {
        retentionService.restore(uploadUuid, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{uploadUuid}/purge")
    public ResponseEntity<Void> purge(@PathVariable UUID uploadUuid, Authentication auth) {
        retentionService.purge(uploadUuid, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/audit.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportAudit(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        byte[] data = csvService.export(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=upload-audit.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }
}
