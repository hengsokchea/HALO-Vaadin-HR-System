package org.halocambodia.upload.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.halocambodia.upload.audit.FileAccessAuditSink;
import org.halocambodia.upload.audit.FileAccessEvent;
import org.halocambodia.upload.audit.RequestIdentityResolver;
import org.halocambodia.upload.config.FileStreamingProperties;
import org.halocambodia.upload.streaming.ByteRange;
import org.halocambodia.upload.streaming.MultiRangeParser;
import org.halocambodia.upload.streaming.StreamedFile;
import org.halocambodia.upload.util.ClientDisconnectUtil;
import org.halocambodia.upload.util.MediaTypeUtil;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStreamingService {

    private static final String CRLF = "\r\n";

    private final MultiRangeParser rangeParser;
    private final TransferRateLimiter rateLimiter;
    private final FileAccessAuditSink auditSink;
    private final RequestIdentityResolver identityResolver;
    private final FileStreamingProperties properties;

    public void streamPreview(
            StreamedFile file,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean headOnly) throws IOException {

        stream(file, request, response, headOnly, true);
    }

    public void streamDownload(
            StreamedFile file,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean headOnly) throws IOException {

        stream(file, request, response, headOnly, false);
    }

    private void stream(
            StreamedFile file,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean headOnly,
            boolean preview) throws IOException {

        long started = System.nanoTime();
        long transferred = 0L;
        boolean disconnected = false;
        int status = HttpStatus.OK.value();

        try {
            validateFile(file);

            long size = Files.size(file.path());
            long modified = Files.getLastModifiedTime(file.path()).toMillis();

            MediaType mediaType = MediaTypeUtil.resolve(
                file.path(),
                file.declaredMimeType(),
                file.originalFileName()
            );

            String etag = createEtag(file, size, modified);

            setCommonHeaders(
                response,
                file,
                mediaType,
                preview,
                etag,
                modified
            );

            if (notModified(request, etag, modified)) {
                status = HttpStatus.NOT_MODIFIED.value();
                response.setStatus(status);
                return;
            }

            List<ByteRange> ranges;

            try {
                ranges = parseEffectiveRanges(request, etag, modified, size);
            } catch (IllegalArgumentException exception) {
                status = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value();
                response.resetBuffer();
                response.setStatus(status);
                response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + size);
                response.setContentLengthLong(0L);
                return;
            }

            long bytesPerSecond = preview
                ? properties.previewBytesPerSecond()
                : properties.downloadBytesPerSecond();

            if (ranges.isEmpty()) {
                status = HttpStatus.OK.value();
                response.setStatus(status);
                response.setContentLengthLong(size);

                if (!headOnly && size > 0L) {
                    transferred = transfer(
                        file,
                        response.getOutputStream(),
                        0L,
                        size,
                        bytesPerSecond
                    );
                }
                return;
            }

            if (ranges.size() == 1) {
                ByteRange range = ranges.getFirst();

                status = HttpStatus.PARTIAL_CONTENT.value();
                response.setStatus(status);
                response.setHeader(
                    HttpHeaders.CONTENT_RANGE,
                    "bytes " + range.start() + "-" + range.end() + "/" + size
                );
                response.setContentLengthLong(range.length());

                if (!headOnly && range.length() > 0L) {
                    transferred = transfer(
                        file,
                        response.getOutputStream(),
                        range.start(),
                        range.length(),
                        bytesPerSecond
                    );
                }
                return;
            }

            status = HttpStatus.PARTIAL_CONTENT.value();
            response.setStatus(status);

            String boundary = "HALO_" + UUID.randomUUID().toString().replace("-", "");
            response.setContentType("multipart/byteranges; boundary=" + boundary);

            if (!headOnly) {
                transferred = writeMultipleRanges(
                    file,
                    response.getOutputStream(),
                    ranges,
                    size,
                    mediaType,
                    boundary,
                    bytesPerSecond
                );
            }

        } catch (Throwable exception) {
            if (ClientDisconnectUtil.isClientDisconnect(exception)) {
                disconnected = true;
                log.debug(
                    "Client disconnected while streaming uploadUuid={}, file={}: {}",
                    file == null ? null : file.uploadUuid(),
                    file == null ? null : file.originalFileName(),
                    exception.getMessage()
                );
                return;
            }

            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException("File streaming failed", exception);

        } finally {
            if (properties.accessLoggingEnabled()) {
                long durationMillis = Duration.ofNanos(
                    System.nanoTime() - started
                ).toMillis();

                auditSink.record(
                    new FileAccessEvent(
                        Instant.now(),
                        identityResolver.username(request),
                        identityResolver.clientIp(request),
                        request.getHeader(HttpHeaders.USER_AGENT),
                        file == null ? null : file.uploadUuid(),
                        file == null ? null : file.originalFileName(),
                        preview ? "PREVIEW" : "DOWNLOAD",
                        status,
                        transferred,
                        durationMillis,
                        disconnected,
                        request.getHeader(HttpHeaders.RANGE)
                    )
                );
            }
        }
    }

    private List<ByteRange> parseEffectiveRanges(
            HttpServletRequest request,
            String etag,
            long modified,
            long size) {

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return List.of();
        }

        String ifRange = request.getHeader(HttpHeaders.IF_RANGE);
        if (ifRange != null && !ifRange.isBlank() && !ifRangeMatches(ifRange, etag, modified)) {
            return List.of();
        }

        return rangeParser.parse(
            rangeHeader,
            size,
            properties.maxRanges()
        );
    }

    private boolean ifRangeMatches(
            String ifRange,
            String etag,
            long modified) {

        String value = ifRange.trim();

        if (value.startsWith("\"") || value.startsWith("W/\"")) {
            return value.equals(etag);
        }

        try {
            long date = java.time.ZonedDateTime.parse(
                value,
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
            ).toInstant().toEpochMilli();

            return modified / 1000L <= date / 1000L;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void setCommonHeaders(
            HttpServletResponse response,
            StreamedFile file,
            MediaType mediaType,
            boolean preview,
            String etag,
            long modified) {

        response.setBufferSize(properties.bufferSize());
        response.setContentType(mediaType.toString());
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setHeader(HttpHeaders.ETAG, etag);
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, modified);
        response.setHeader(
            HttpHeaders.CACHE_CONTROL,
            CacheControl.maxAge(Duration.ofHours(1))
                .cachePrivate()
                .mustRevalidate()
                .getHeaderValue()
        );
        response.setHeader("X-Content-Type-Options", "nosniff");

        boolean inline = preview && MediaTypeUtil.previewable(mediaType);

        if (inline) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
        } else {
            response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                disposition("attachment", file.originalFileName())
            );
        }
    }

    private long writeMultipleRanges(
            StreamedFile file,
            OutputStream output,
            List<ByteRange> ranges,
            long size,
            MediaType mediaType,
            String boundary,
            long bytesPerSecond) throws IOException {

        long total = 0L;

        for (ByteRange range : ranges) {
            String header =
                "--" + boundary + CRLF
                + HttpHeaders.CONTENT_TYPE + ": " + mediaType + CRLF
                + HttpHeaders.CONTENT_RANGE + ": bytes "
                + range.start() + "-" + range.end() + "/" + size + CRLF
                + CRLF;

            byte[] headerBytes = header.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            output.write(headerBytes);
            total += headerBytes.length;

            total += transfer(
                file,
                output,
                range.start(),
                range.length(),
                bytesPerSecond
            );

            byte[] separator = CRLF.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            output.write(separator);
            total += separator.length;
        }

        byte[] closing = (
            "--" + boundary + "--" + CRLF
        ).getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        output.write(closing);
        output.flush();
        return total + closing.length;
    }

    private long transfer(
            StreamedFile file,
            OutputStream output,
            long start,
            long length,
            long bytesPerSecond) throws IOException {

        TransferRateLimiter.Session limiter = rateLimiter.open(bytesPerSecond);

        try (FileChannel source = FileChannel.open(
                file.path(),
                StandardOpenOption.READ);
             WritableByteChannel target = Channels.newChannel(output)) {

            source.position(start);

            long remaining = length;
            long transferred = 0L;

            while (remaining > 0L) {
                long chunk = Math.min(remaining, 8L * 1024L * 1024L);
                long written = source.transferTo(source.position(), chunk, target);

                if (written > 0L) {
                    source.position(source.position() + written);
                    remaining -= written;
                    transferred += written;
                    limiter.afterWrite((int) Math.min(written, Integer.MAX_VALUE));
                    continue;
                }

                int fallbackSize = (int) Math.min(properties.bufferSize(), remaining);
                ByteBuffer buffer = ByteBuffer.allocateDirect(fallbackSize);
                int read = source.read(buffer);

                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    Thread.onSpinWait();
                    continue;
                }

                buffer.flip();
                while (buffer.hasRemaining()) {
                    target.write(buffer);
                }

                remaining -= read;
                transferred += read;
                limiter.afterWrite(read);
            }

            output.flush();
            return transferred;
        }
    }

    private void validateFile(StreamedFile file) throws IOException {
        if (file == null || file.path() == null || !Files.exists(file.path())) {
            throw new FileNotFoundException("Stored file was not found");
        }
        if (!Files.isRegularFile(file.path()) || !Files.isReadable(file.path())) {
            throw new IOException("Stored path is not a readable regular file");
        }
    }

    private boolean notModified(
            HttpServletRequest request,
            String etag,
            long modified) {

        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);

        if (ifNoneMatch != null) {
            for (String candidate : ifNoneMatch.split(",")) {
                String value = candidate.trim();
                if ("*".equals(value) || etag.equals(value)) {
                    return true;
                }
            }
            return false;
        }

        long ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);

        return ifModifiedSince >= 0L
            && modified / 1000L <= ifModifiedSince / 1000L;
    }

    private String createEtag(StreamedFile file, long size, long modified) {
        if (file.checksumSha256() != null && !file.checksumSha256().isBlank()) {
            return "\"sha256-" + file.checksumSha256() + "\"";
        }
        return "\"" + Long.toHexString(size) + "-" + Long.toHexString(modified) + "\"";
    }

    private String disposition(String type, String fileName) {
        String name = (fileName == null || fileName.isBlank())
            ? "download"
            : fileName
                .replace("\\", "_")
                .replace("\"", "")
                .replace("\r", "")
                .replace("\n", "");

        String encoded = UriUtils.encode(
            name,
            java.nio.charset.StandardCharsets.UTF_8
        );

        if (isAscii(name)) {
            return type + "; filename=\"" + name + "\"";
        }

        return type + "; filename*=UTF-8''" + encoded;
    }

    private boolean isAscii(String value) {
        return value.chars().allMatch(character -> character <= 0x7F);
    }
}
