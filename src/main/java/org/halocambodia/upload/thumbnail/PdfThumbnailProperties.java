package org.halocambodia.upload.thumbnail;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.thumbnail.pdf")
public record PdfThumbnailProperties(
        boolean enabled,
        float dpi,
        String format,
        float jpegQuality,
        Duration timeout) {

    public PdfThumbnailProperties {
        if (dpi <= 0) dpi = 144F;
        if (format == null || format.isBlank()) format = "jpg";
        format = format.toLowerCase();
        if (jpegQuality <= 0 || jpegQuality > 1) jpegQuality = 0.88F;
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = Duration.ofSeconds(30);
        }
    }
}
