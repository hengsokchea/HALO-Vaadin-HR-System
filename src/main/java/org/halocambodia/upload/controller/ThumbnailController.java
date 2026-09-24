package org.halocambodia.upload.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.halocambodia.upload.thumbnail.ThumbnailDescriptor;
import org.halocambodia.upload.thumbnail.ThumbnailProperties;
import org.halocambodia.upload.thumbnail.ThumbnailService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class ThumbnailController {

    private final ThumbnailService thumbnailService;
    private final ThumbnailProperties properties;

    @GetMapping("/{uploadUuid}/thumbnail")
    public void thumbnail(
            @PathVariable UUID uploadUuid,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ThumbnailDescriptor thumbnail =
            thumbnailService.getOrCreate(
                uploadUuid,
                width,
                height
            );

        if (!thumbnail.available()) {
            response.sendError(
                HttpStatus.NOT_FOUND.value(),
                "Thumbnail is not available"
            );
            return;
        }

        String ifNoneMatch =
            request.getHeader(HttpHeaders.IF_NONE_MATCH);

        if (thumbnail.etag() != null
                && thumbnail.etag().equals(ifNoneMatch)) {

            response.setStatus(
                HttpStatus.NOT_MODIFIED.value()
            );
            return;
        }

        response.setContentType(thumbnail.mimeType());

        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            "inline"
        );

        if (thumbnail.etag() != null) {
            response.setHeader(
                HttpHeaders.ETAG,
                thumbnail.etag()
            );
        }

        response.setDateHeader(
            HttpHeaders.LAST_MODIFIED,
            thumbnail.lastModified()
        );

        response.setHeader(
            HttpHeaders.CACHE_CONTROL,
            CacheControl
                .maxAge(properties.cacheMaxAge())
                .cachePrivate()
                .mustRevalidate()
                .getHeaderValue()
        );

        long fileSize = Files.size(thumbnail.path());
        response.setContentLengthLong(fileSize);

        /*
         * Spring automatically handles HEAD requests mapped
         * through @GetMapping. The response body is suppressed.
         */
        Files.copy(
            thumbnail.path(),
            response.getOutputStream()
        );

        response.getOutputStream().flush();
    }
}