package org.halocambodia.upload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file-streaming")
public record FileStreamingProperties(
        long previewBytesPerSecond,
        long downloadBytesPerSecond,
        int maxRanges,
        int bufferSize,
        boolean accessLoggingEnabled,
        boolean thumbnailsEnabled,
        int thumbnailWidth,
        int thumbnailHeight) {

    public FileStreamingProperties {
        if (previewBytesPerSecond < 0) previewBytesPerSecond = 0;
        if (downloadBytesPerSecond < 0) downloadBytesPerSecond = 0;
        if (maxRanges <= 0) maxRanges = 8;
        if (bufferSize <= 0) bufferSize = 256 * 1024;
        if (thumbnailWidth <= 0) thumbnailWidth = 320;
        if (thumbnailHeight <= 0) thumbnailHeight = 240;
    }
}
