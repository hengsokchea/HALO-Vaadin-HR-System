package org.halocambodia.upload.thumbnail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VideoThumbnailGenerator {

    private static final Set<String> EXTENSIONS = Set.of(
        "mp4", "mov", "mkv", "avi", "webm", "m4v", "mpeg", "mpg", "wmv"
    );

    private final VideoThumbnailProperties properties;

    public boolean supports(String mimeType, String fileName) {
        if (mimeType != null && mimeType.toLowerCase().startsWith("video/")) {
            return true;
        }
        if (fileName == null) return false;
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(
            fileName.substring(dot + 1).toLowerCase()
        );
    }

    public ThumbnailResult generate(
            Path source,
            Path destination,
            int width,
            int height) throws IOException {

        if (!properties.enabled()) {
            return ThumbnailResult.skipped("Video thumbnail generation is disabled");
        }
        if (!Files.isRegularFile(source)) {
            return ThumbnailResult.skipped("Video source file does not exist");
        }

        Files.createDirectories(destination.getParent());

        String scale = buildScale(width, height);

        List<String> command = List.of(
            properties.ffmpegPath().toString(),
            "-hide_banner",
            "-loglevel", "error",
            "-y",
            "-ss", Double.toString(properties.captureSecond()),
            "-i", source.toString(),
            "-frames:v", "1",
            "-vf", scale,
            "-q:v", Integer.toString(properties.jpegQuality()),
            destination.toString()
        );

        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();

        byte[] output;
        try (var stream = process.getInputStream()) {
            output = stream.readAllBytes();
        }

        boolean finished;
        try {
            finished = process.waitFor(
                properties.timeout().toMillis(),
                TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted while waiting for FFmpeg", exception);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("FFmpeg timed out after " + properties.timeout());
        }

        if (process.exitValue() != 0 || !Files.isRegularFile(destination)) {
            String error = new String(output, StandardCharsets.UTF_8);
            throw new IOException(
                "FFmpeg thumbnail generation failed with exit code "
                + process.exitValue() + ": " + error
            );
        }

        return ThumbnailResult.generated(destination, "image/jpeg");
    }

    public void verifyInstalled() throws IOException {
        Process process = new ProcessBuilder(
            properties.ffmpegPath().toString(),
            "-version"
        ).redirectErrorStream(true).start();

        try {
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
                process.destroyForcibly();
                throw new IOException("FFmpeg is not available: " + properties.ffmpegPath());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while verifying FFmpeg", exception);
        }
    }

    private String buildScale(int width, int height) {
        int w = width > 0 ? width : 320;
        int h = height > 0 ? height : 240;
        return "scale='min(" + w + ",iw)':'min(" + h
            + ",ih)':force_original_aspect_ratio=decrease";
    }
}
