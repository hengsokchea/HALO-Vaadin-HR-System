package org.halocambodia.upload.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.halocambodia.data.PolicyAccessAction;
import org.halocambodia.fileattachment.data.FileAttachmentAccessAction;
import org.halocambodia.fileattachment.repository.FileAttachmentRepository;
import org.halocambodia.fileattachment.service.FileAttachmentAccessLogService;
import org.halocambodia.services.PolicyAccessLogService;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.domain.UploadStatus;
import org.halocambodia.upload.exception.UploadException;
import org.halocambodia.upload.repository.UploadSessionRepository;
import org.halocambodia.upload.service.FileStreamingService;
import org.halocambodia.upload.streaming.StreamedFile;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serves completed uploads through the generic /api/uploads endpoints.
 *
 * Policy attachment access logging is handled here so PolicyReadView can keep
 * using:
 *
 * /api/uploads/{uploadUuid}/preview
 * /api/uploads/{uploadUuid}/download
 *
 * HEAD requests are not logged because browsers and PDF viewers may issue
 * several HEAD requests automatically.
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Slf4j
public class CompletedFileController {

    private final UploadSessionRepository uploadSessionRepository;
    private final FileStreamingService streamingService;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileAttachmentAccessLogService    fileAttachmentAccessLogService;

    @GetMapping("/{uploadUuid}/preview")
    @Transactional(readOnly = true)
    public void preview(
            @PathVariable UUID uploadUuid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        StreamedFile streamedFile = resolve(uploadUuid);

        
        fileAttachmentAccessLogService.record(
                uploadUuid,
                FileAttachmentAccessAction.PREVIEWED,
                request,
                HttpServletResponse.SC_OK
        );
        

        streamingService.streamPreview(
                streamedFile,
                request,
                response,
                false
        );
    }

    @RequestMapping(
            value = "/{uploadUuid}/preview",
            method = RequestMethod.HEAD
    )
    @Transactional(readOnly = true)
    public void previewHead(
            @PathVariable UUID uploadUuid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        streamingService.streamPreview(
                resolve(uploadUuid),
                request,
                response,
                true
        );
    }

    @GetMapping("/{uploadUuid}/download")
    @Transactional(readOnly = true)
    public void download(
            @PathVariable UUID uploadUuid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        StreamedFile streamedFile = resolve(uploadUuid);

        fileAttachmentAccessLogService.record(
                uploadUuid,
                FileAttachmentAccessAction.DOWNLOADED,
                request,
                HttpServletResponse.SC_OK
        );

        streamingService.streamDownload(
                streamedFile,
                request,
                response,
                false
        );
    }

    @RequestMapping(
            value = "/{uploadUuid}/download",
            method = RequestMethod.HEAD
    )
    @Transactional(readOnly = true)
    public void downloadHead(
            @PathVariable UUID uploadUuid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        streamingService.streamDownload(
                resolve(uploadUuid),
                request,
                response,
                true
        );
    }

    /**
     * Resolves the completed upload and blocks access when an associated
     * FileAttachment record has been soft deleted.
     *
     * A completed upload that has not yet been attached to a business record is
     * still allowed. This preserves preview support during an upload/edit flow.
     */
    private StreamedFile resolve(UUID uploadUuid) {

        if (uploadUuid == null) {
            throw new UploadException(
                    HttpStatus.BAD_REQUEST,
                    "Upload UUID is required"
            );
        }

        fileAttachmentRepository
                .findByFileUuid(uploadUuid)
                .ifPresent(attachment -> {
                    if (attachment.isDeleted()) {
                        throw new UploadException(
                                HttpStatus.NOT_FOUND,
                                "File attachment was not found"
                        );
                    }
                });

        UploadSession session = uploadSessionRepository
                .findByUploadUuid(uploadUuid)
                .orElseThrow(() -> new UploadException(
                        HttpStatus.NOT_FOUND,
                        "Completed upload was not found"
                ));

        if (session.getStatus() != UploadStatus.COMPLETED) {
            throw new UploadException(
                    HttpStatus.CONFLICT,
                    "File is not ready. Upload status is "
                            + session.getStatus()
            );
        }

        if (session.getFinalPath() == null
                || session.getFinalPath().isBlank()) {

            throw new UploadException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Completed upload does not contain a final path"
            );
        }

        return new StreamedFile(
                session.getUploadUuid(),
                Path.of(session.getFinalPath())
                        .toAbsolutePath()
                        .normalize(),
                session.getOriginalFileName(),
                session.getMimeType(),
                session.getChecksumSha256()
        );
    }

    
    private void recordAttachmentAccessSafely(
            UUID uploadUuid,
            FileAttachmentAccessAction action,
            HttpServletRequest request,
            int responseStatus) {

        try {
            fileAttachmentAccessLogService.record(
                    uploadUuid,
                    action,
                    request,
                    responseStatus
            );
        } catch (Exception exception) {
            log.warn(
                    "Unable to record file attachment access. "
                            + "uploadUuid={}, action={}",
                    uploadUuid,
                    action,
                    exception
            );
        }
    }
}