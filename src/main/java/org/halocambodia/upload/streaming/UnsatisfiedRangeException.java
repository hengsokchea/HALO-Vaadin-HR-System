package org.halocambodia.upload.streaming;

public class UnsatisfiedRangeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final long fileSize;

    public UnsatisfiedRangeException(long fileSize) {
        super("Requested byte range is not satisfiable");
        this.fileSize = Math.max(0L, fileSize);
    }

    public long getFileSize() {
        return fileSize;
    }
}
