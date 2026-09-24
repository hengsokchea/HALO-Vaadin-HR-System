package org.halocambodia.upload.scan;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.exception.UploadException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class VirusScanService {
    private final VirusScanner virusScanner;

    public VirusScanResult requireClean(Path file) {
        VirusScanResult result = virusScanner.scan(file);
        if (!result.clean()) {
            throw new UploadException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Uploaded file failed malware scanning: " + result.message());
        }
        return result;
    }
}
