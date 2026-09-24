package org.halocambodia.upload.audit;

import java.time.Instant;
import java.util.UUID;

public record FileAccessEvent(
        Instant occurredAt,
        String username,
        String clientIp,
        String userAgent,
        UUID uploadUuid,
        String fileName,
        String action,
        int httpStatus,
        long bytesTransferred,
        long durationMillis,
        boolean clientDisconnected,
        String rangeHeader) {
}
