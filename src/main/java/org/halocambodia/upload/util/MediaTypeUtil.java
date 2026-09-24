package org.halocambodia.upload.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;

public final class MediaTypeUtil {
    private static final Map<String, String> TYPES = Map.ofEntries(
        Map.entry("pdf", "application/pdf"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("png", "image/png"),
        Map.entry("gif", "image/gif"),
        Map.entry("webp", "image/webp"),
        Map.entry("mp4", "video/mp4"),
        Map.entry("webm", "video/webm"),
        Map.entry("mov", "video/quicktime"),
        Map.entry("mp3", "audio/mpeg"),
        Map.entry("m4a", "audio/mp4"),
        Map.entry("wav", "audio/wav"),
        Map.entry("ogg", "audio/ogg"),
        Map.entry("txt", "text/plain"),
        Map.entry("csv", "text/csv"),
        Map.entry("zip", "application/zip")
    );

    private MediaTypeUtil() {}

    public static MediaType resolve(Path path, String declared, String name) {
        if (declared != null && !declared.isBlank()
                && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(declared)) {
            try {
                return MediaType.parseMediaType(declared.split(";", 2)[0].trim());
            } catch (IllegalArgumentException ignored) {}
        }

        try {
            String detected = Files.probeContentType(path);
            if (detected != null && !detected.isBlank()) {
                return MediaType.parseMediaType(detected);
            }
        } catch (IOException | IllegalArgumentException ignored) {}

        String ext = extension(name);
        if (ext.isBlank()) {
            ext = extension(path.getFileName().toString());
        }
        String type = TYPES.get(ext);
        return type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type);
    }

    public static boolean previewable(MediaType type) {
        return "image".equals(type.getType())
            || "video".equals(type.getType())
            || "audio".equals(type.getType())
            || MediaType.APPLICATION_PDF.includes(type)
            || "text".equals(type.getType());
    }

    private static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1
            ? ""
            : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
