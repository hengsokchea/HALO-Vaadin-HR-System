package org.halocambodia.upload.thumbnail;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.thumbnail.jobs")
public record ThumbnailJobProperties(
        boolean enabled,
        int corePoolSize,
        int queueCapacity,
        int maxRetries,
        Duration retryDelay,
        Duration cleanupInterval,
        Duration retention) {

    public ThumbnailJobProperties {
        if (corePoolSize <= 0) corePoolSize = 2;
        if (queueCapacity <= 0) queueCapacity = 500;
        if (maxRetries < 0) maxRetries = 2;
        if (retryDelay == null) retryDelay = Duration.ofSeconds(5);
        if (cleanupInterval == null) cleanupInterval = Duration.ofHours(6);
        if (retention == null) retention = Duration.ofDays(30);
    }
}
