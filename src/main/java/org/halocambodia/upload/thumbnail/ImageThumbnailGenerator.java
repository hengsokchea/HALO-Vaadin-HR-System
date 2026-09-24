package org.halocambodia.upload.thumbnail;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

@Component
public class ImageThumbnailGenerator {

    public boolean supports(String mimeType, String fileName) {
        return mimeType != null && mimeType.toLowerCase().startsWith("image/");
    }

    public ThumbnailResult generate(
            Path source,
            Path destination,
            int maxWidth,
            int maxHeight) throws IOException {

        BufferedImage input = ImageIO.read(source.toFile());
        if (input == null) return ThumbnailResult.skipped("Unsupported image");

        double ratio = Math.min(
            (double) maxWidth / input.getWidth(),
            (double) maxHeight / input.getHeight()
        );
        ratio = Math.min(1D, ratio);

        int width = Math.max(1, (int) Math.round(input.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(input.getHeight() * ratio));

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.drawImage(input, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        Files.createDirectories(destination.getParent());
        if (!ImageIO.write(output, "jpg", destination.toFile())) {
            throw new IOException("JPEG writer unavailable");
        }

        return ThumbnailResult.generated(destination, "image/jpeg");
    }
}
