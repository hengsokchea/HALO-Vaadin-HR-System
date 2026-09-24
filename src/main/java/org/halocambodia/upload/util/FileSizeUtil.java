package org.halocambodia.upload.util;

import java.util.Locale;

public final class FileSizeUtil {

    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

    private FileSizeUtil() {
    }

    public static String format(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < UNITS.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format(Locale.ROOT, unitIndex == 0 ? "%.0f %s" : "%.2f %s", value, UNITS[unitIndex]);
    }
}
