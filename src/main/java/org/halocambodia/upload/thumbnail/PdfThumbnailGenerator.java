package org.halocambodia.upload.thumbnail;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PdfThumbnailGenerator {

    private final PdfThumbnailProperties properties;

    public boolean supports(String mimeType, String fileName) {
        if ("application/pdf".equalsIgnoreCase(mimeType)) return true;
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    public ThumbnailResult generate(
            Path source,
            Path destination,
            int maxWidth,
            int maxHeight) throws IOException {

        if (!properties.enabled()) {
            return ThumbnailResult.skipped("PDF thumbnail generation is disabled");
        }
        if (!Files.isRegularFile(source)) {
            return ThumbnailResult.skipped("PDF source file does not exist");
        }

        Files.createDirectories(destination.getParent());

        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            if (document.getNumberOfPages() == 0) {
                return ThumbnailResult.skipped("PDF contains no pages");
            }

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage rendered = renderer.renderImageWithDPI(
                0,
                properties.dpi(),
                ImageType.RGB
            );

            BufferedImage scaled = scale(rendered, maxWidth, maxHeight);
            write(scaled, destination, properties.format(), properties.jpegQuality());

            return ThumbnailResult.generated(
                destination,
                "png".equals(properties.format()) ? "image/png" : "image/jpeg"
            );
        }
    }

    private BufferedImage scale(BufferedImage source, int maxWidth, int maxHeight) {
        int targetWidth = maxWidth > 0 ? maxWidth : source.getWidth();
        int targetHeight = maxHeight > 0 ? maxHeight : source.getHeight();

        double ratio = Math.min(
            (double) targetWidth / source.getWidth(),
            (double) targetHeight / source.getHeight()
        );
        ratio = Math.min(1D, ratio);

        int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));

        BufferedImage target = new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private void write(
            BufferedImage image,
            Path destination,
            String format,
            float jpegQuality) throws IOException {

        if ("png".equals(format)) {
            if (!ImageIO.write(image, "png", destination.toFile())) {
                throw new IOException("PNG writer is unavailable");
            }
            return;
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg")
            .next();

        try (ImageOutputStream output =
                 ImageIO.createImageOutputStream(destination.toFile())) {

            writer.setOutput(output);
            ImageWriteParam parameter = writer.getDefaultWriteParam();
            parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameter.setCompressionQuality(jpegQuality);
            writer.write(null, new IIOImage(image, null, null), parameter);
        } finally {
            writer.dispose();
        }
    }
}
