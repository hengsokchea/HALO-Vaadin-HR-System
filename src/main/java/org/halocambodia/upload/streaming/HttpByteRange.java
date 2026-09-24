package org.halocambodia.upload.streaming;

/** Inclusive HTTP byte range. */
public record HttpByteRange(long start, long end) {
    public HttpByteRange {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid HTTP byte range");
        }
    }

    public long length() {
        return end - start + 1;
    }
}
