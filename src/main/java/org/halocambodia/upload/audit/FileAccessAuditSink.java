package org.halocambodia.upload.audit;

@FunctionalInterface
public interface FileAccessAuditSink {

    void record(FileAccessEvent event);
}
