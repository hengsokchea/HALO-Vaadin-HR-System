package org.halocambodia.upload.thumbnail;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.thumbnail.video")
public record VideoThumbnailProperties(
        boolean enabled,
        Path ffmpegPath,
        Duration timeout,
        double captureSecond,
        String format,
        int jpegQuality) {

    public VideoThumbnailProperties {
        if (ffmpegPath == null) ffmpegPath = Path.of("ffmpeg");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            timeout = Duration.ofSeconds(30);
        }
        if (captureSecond < 0) captureSecond = 2D;
        if (format == null || format.isBlank()) format = "jpg";
        if (jpegQuality < 2 || jpegQuality > 31) jpegQuality = 3;
    }
}
