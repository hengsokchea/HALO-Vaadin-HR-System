package org.halocambodia.upload.scan;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

/**
 * Default virus scanner used when no real malware-scanning engine is configured.
 *
 * WARNING:
 * This implementation does not scan the file. It only allows the upload
 * framework to operate until ClamAV or another scanner is configured.
 */
@Component
public class NoOpVirusScanner implements VirusScanner {

    @Override
    public VirusScanResult scan(Path file) {
        return VirusScanResult.clean("disabled");
    }
}