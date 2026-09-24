package org.halocambodia.upload.scan;

import java.nio.file.Path;

/** Extension point for ClamAV or another malware scanning engine. */
public interface VirusScanner {
    VirusScanResult scan(Path file);
}
