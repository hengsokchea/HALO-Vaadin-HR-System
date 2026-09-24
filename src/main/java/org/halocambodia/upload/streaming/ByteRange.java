package org.halocambodia.upload.streaming;

public record ByteRange(long start, long end) {

    public ByteRange {
        if (start < 0) {
            throw new IllegalArgumentException("Range start must be >= 0");
        }
        if (end < start) {
            throw new IllegalArgumentException("Range end must be >= start");
        }
    }

    public long length() {
        return end - start + 1;
    }
}
