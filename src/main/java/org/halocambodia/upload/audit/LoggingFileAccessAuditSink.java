package org.halocambodia.upload.audit;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoggingFileAccessAuditSink implements FileAccessAuditSink {

    @Override
    public void record(FileAccessEvent event) {
        log.info(
            "file_access action={} user={} ip={} uploadUuid={} file={} status={} bytes={} durationMs={} disconnected={} range={}",
            event.action(),
            event.username(),
            event.clientIp(),
            event.uploadUuid(),
            event.fileName(),
            event.httpStatus(),
            event.bytesTransferred(),
            event.durationMillis(),
            event.clientDisconnected(),
            event.rangeHeader()
        );
    }
}
