package org.halocambodia.upload.reporting;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.audit.UploadAuditEvent;
import org.halocambodia.upload.audit.UploadAuditRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadAuditCsvService {
    private final UploadAuditRepository repository;

    public byte[] export(OffsetDateTime from, OffsetDateTime to) {
        List<UploadAuditEvent> events = repository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        StringBuilder csv = new StringBuilder("created_at,upload_uuid,action,username,file_name,bytes_processed,success,message,remote_address\n");
        for (UploadAuditEvent e : events) {
            csv.append(cell(e.getCreatedAt())).append(',').append(cell(e.getUploadUuid())).append(',')
               .append(cell(e.getAction())).append(',').append(cell(e.getUsername())).append(',')
               .append(cell(e.getFileName())).append(',').append(cell(e.getBytesProcessed())).append(',')
               .append(e.isSuccess()).append(',').append(cell(e.getMessage())).append(',')
               .append(cell(e.getRemoteAddress())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String cell(Object value) {
        String s = value == null ? "" : value.toString();
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
