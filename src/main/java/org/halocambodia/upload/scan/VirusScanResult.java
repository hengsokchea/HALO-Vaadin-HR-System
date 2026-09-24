package org.halocambodia.upload.scan;

public record VirusScanResult(boolean clean, String engine, String message) {
    public static VirusScanResult clean(String engine) {
        return new VirusScanResult(true, engine, "Clean");
    }

    public static VirusScanResult infected(String engine, String message) {
        return new VirusScanResult(false, engine, message);
    }
}
