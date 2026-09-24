package org.halocambodia.data;

import java.text.DecimalFormat;

public class FileSizeFormatter {

    private static final String[] UNITS = {"Bytes", "KB", "MB", "GB", "TB", "PB", "EB"};
    private static final long KILOBYTE = 1024;
    private static final long MEGABYTE = KILOBYTE * 1024;
    private static final long GIGABYTE = MEGABYTE * 1024;
    private static final long TERABYTE = GIGABYTE * 1024;

    /**
     * Converts a byte count to a human-readable string (e.g., "17.4 KB", "2.5 MB").
     * Uses binary prefixes (powers of 1024).
     *
     * @param bytes The size in bytes.
     * @return A formatted string representing the file size.
     */
    public static String formatBytes(Long bytes) {
        if (bytes == null) {
            return "";
        }
        if (bytes < KILOBYTE) {
            return bytes + " Bytes";
        }

        int unitIndex = (int) (Math.log(bytes) / Math.log(KILOBYTE));
        double size = (double) bytes / Math.pow(KILOBYTE, unitIndex);

        DecimalFormat df = new DecimalFormat("#.##"); // Format to 2 decimal places
        return df.format(size) + " " + UNITS[unitIndex];
    }
    
    // Alternative for more granular control, less common for simple display
    public static String formatBytesAlternative(long bytes) {
        if (bytes < 0) {
            return "N/A"; // Or throw an IllegalArgumentException
        }
        if (bytes < KILOBYTE) {
            return bytes + " Bytes";
        }
        int exp = (int) (Math.log(bytes) / Math.log(KILOBYTE));
        return String.format("%.2f %s", bytes / Math.pow(KILOBYTE, exp), UNITS[exp]);
    }
}
