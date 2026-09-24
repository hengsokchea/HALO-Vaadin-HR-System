package org.halocambodia.upload.streaming;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MultiRangeParser {

    public List<ByteRange> parse(String rangeHeader, long fileSize, int maxRanges) {
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return List.of();
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must be >= 0");
        }
        if (!rangeHeader.regionMatches(true, 0, "bytes=", 0, 6)) {
            throw new IllegalArgumentException("Only byte ranges are supported");
        }

        String value = rangeHeader.substring(6).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty Range header");
        }

        String[] parts = value.split(",");
        if (parts.length > maxRanges) {
            throw new IllegalArgumentException("Too many ranges requested");
        }

        List<ByteRange> ranges = new ArrayList<>(parts.length);

        for (String raw : parts) {
            String item = raw.trim();
            int dash = item.indexOf('-');
            if (dash < 0) {
                throw new IllegalArgumentException("Invalid byte range: " + item);
            }

            String startText = item.substring(0, dash).trim();
            String endText = item.substring(dash + 1).trim();

            long start;
            long end;

            if (startText.isEmpty()) {
                long suffixLength = parseLong(endText, item);
                if (suffixLength <= 0 || fileSize == 0) {
                    throw new IllegalArgumentException("Invalid suffix range: " + item);
                }
                suffixLength = Math.min(suffixLength, fileSize);
                start = fileSize - suffixLength;
                end = fileSize - 1;
            } else {
                start = parseLong(startText, item);
                if (start >= fileSize) {
                    throw new IllegalArgumentException("Unsatisfied range: " + item);
                }

                if (endText.isEmpty()) {
                    end = fileSize - 1;
                } else {
                    end = parseLong(endText, item);
                    end = Math.min(end, fileSize - 1);
                }

                if (end < start) {
                    throw new IllegalArgumentException("Invalid range: " + item);
                }
            }

            ranges.add(new ByteRange(start, end));
        }

        return List.copyOf(ranges);
    }

    private long parseLong(String value, String original) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid byte range: " + original, exception);
        }
    }
}
