package org.halocambodia.upload.exception;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.halocambodia.upload.util.ClientDisconnectUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice(basePackages = "org.halocambodia.upload")
@Slf4j
public class UploadExceptionHandler {

    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Map<String, Object>> handleUploadException(
            UploadException exception,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (response.isCommitted()) {
            logAfterCommit(exception, request);
            return null;
        }

        return jsonError(
            exception.getStatus(),
            exception.getMessage(),
            request
        );
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncDisconnect(
            AsyncRequestNotUsableException exception,
            HttpServletRequest request) {

        log.debug(
            "Client disconnected: method={}, uri={}, message={}",
            request.getMethod(), request.getRequestURI(), exception.getMessage()
        );
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMissingFile(
            FileNotFoundException exception,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (response.isCommitted()) {
            logAfterCommit(exception, request);
            return null;
        }

        return jsonError(HttpStatus.NOT_FOUND, "Stored file was not found", request);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIoException(
            IOException exception,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (ClientDisconnectUtil.isClientDisconnect(exception)) {
            log.debug(
                "Client disconnected during transfer: method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage()
            );
            return null;
        }

        if (response.isCommitted()) {
            logAfterCommit(exception, request);
            return null;
        }

        log.error(
            "File transfer failed: method={}, uri={}",
            request.getMethod(), request.getRequestURI(), exception
        );

        return jsonError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "File transfer failed",
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception exception,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (ClientDisconnectUtil.isClientDisconnect(exception)) {
            log.debug(
                "Client disconnected during request: method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage()
            );
            return null;
        }

        /*
         * Never serialize JSON into an already-started video, audio, image or
         * PDF response. This fixes the previous HttpMessageNotWritableException
         * with preset Content-Type video/mp4.
         */
        if (response.isCommitted()) {
            logAfterCommit(exception, request);
            return null;
        }

        log.error(
            "Unexpected upload error: method={}, uri={}",
            request.getMethod(), request.getRequestURI(), exception
        );

        return jsonError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected upload error",
            request
        );
    }

    private ResponseEntity<Map<String, Object>> jsonError(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("requestId", requestId);

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Request-ID", requestId)
            .body(body);
    }

    private void logAfterCommit(Throwable exception, HttpServletRequest request) {
        if (ClientDisconnectUtil.isClientDisconnect(exception)) {
            log.debug(
                "Client disconnected after response commit: method={}, uri={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getMessage()
            );
        } else {
            log.warn(
                "Response already committed; no JSON error body written. method={}, uri={}, exception={}",
                request.getMethod(), request.getRequestURI(), exception.toString()
            );
        }
    }
}
