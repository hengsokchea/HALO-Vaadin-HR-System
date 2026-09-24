package org.halocambodia.upload.streaming;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HttpRangeParser {

    public Optional<HttpByteRange> parse(String header, long fileSize) {
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        if (fileSize <= 0 || !header.regionMatches(true, 0, "bytes=", 0, 6)) {
            throw new UnsatisfiedRangeException(fileSize);
        }

        String value = header.substring(6).trim();
        if (value.contains(",")) {
            throw new UnsatisfiedRangeException(fileSize);
        }

        int dash = value.indexOf('-');
        if (dash < 0) {
            throw new UnsatisfiedRangeException(fileSize);
        }

        String startText = value.substring(0, dash).trim();
        String endText = value.substring(dash + 1).trim();

        try {
            if (startText.isEmpty()) {
                long suffixLength = Long.parseLong(endText);
                if (suffixLength <= 0) {
                    throw new UnsatisfiedRangeException(fileSize);
                }
                long length = Math.min(suffixLength, fileSize);
                return Optional.of(new HttpByteRange(fileSize - length, fileSize - 1));
            }

            long start = Long.parseLong(startText);
            if (start < 0 || start >= fileSize) {
                throw new UnsatisfiedRangeException(fileSize);
            }

            long end = endText.isEmpty() ? fileSize - 1 : Long.parseLong(endText);
            if (end < start) {
                throw new UnsatisfiedRangeException(fileSize);
            }

            return Optional.of(new HttpByteRange(start, Math.min(end, fileSize - 1)));
        } catch (NumberFormatException ex) {
            throw new UnsatisfiedRangeException(fileSize);
        }
    }
}
