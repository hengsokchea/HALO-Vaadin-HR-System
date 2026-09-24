package org.halocambodia.upload.thumbnail;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.thumbnail")
public record ThumbnailProperties(
        boolean enabled,
        Path directory,
        int defaultWidth,
        int defaultHeight,
        int maxWidth,
        int maxHeight,
        Duration cacheMaxAge) {

    public ThumbnailProperties {
        if (directory == null) directory = Path.of("./data/hr/uploads/thumbnails");
        if (defaultWidth <= 0) defaultWidth = 320;
        if (defaultHeight <= 0) defaultHeight = 240;
        if (maxWidth <= 0) maxWidth = 1600;
        if (maxHeight <= 0) maxHeight = 1200;
        if (cacheMaxAge == null) cacheMaxAge = Duration.ofDays(7);
    }
}
