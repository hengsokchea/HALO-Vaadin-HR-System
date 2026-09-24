package org.halocambodia.upload.exception;

import org.springframework.http.HttpStatus;

public class UploadException extends RuntimeException {

    private final HttpStatus status;

    public UploadException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public UploadException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
