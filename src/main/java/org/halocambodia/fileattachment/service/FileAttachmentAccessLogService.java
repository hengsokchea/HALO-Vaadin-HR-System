package org.halocambodia.fileattachment.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.User;
import org.halocambodia.fileattachment.data.FileAttachment;
import org.halocambodia.fileattachment.data.FileAttachmentAccessAction;
import org.halocambodia.fileattachment.data.FileAttachmentAccessLog;
import org.halocambodia.fileattachment.repository.FileAttachmentAccessLogRepository;
import org.halocambodia.fileattachment.repository.FileAttachmentRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileAttachmentAccessLogService {



    private final FileAttachmentAccessLogRepository repository;
    private final FileAttachmentRepository attachmentRepository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID fileUuid,
            FileAttachmentAccessAction action,
            HttpServletRequest request,
            Integer responseStatus) {

        if (fileUuid == null || action == null) {
            return;
        }

        FileAttachment attachment =
                attachmentRepository
                        .findByFileUuidAndDeletedFalse(fileUuid)
                        .orElse(null);

        if (attachment == null) {
            return;
        }

        FileAttachmentAccessLog accessLog =
                new FileAttachmentAccessLog();

        accessLog.setFileAttachment(attachment);

        accessLog.setOwnerType(
                attachment.getOwnerType().name()
        );

        accessLog.setOwnerId(
                attachment.getOwnerId()
        );

        accessLog.setAction(action);

        User user = authenticatedUser
                .get()
                .orElse(null);

        accessLog.setUser(user);

        accessLog.setAccessedAt(ZonedDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE));

        accessLog.setIpAddress(
                resolveIp(request)
        );

        accessLog.setUserAgent(
                trim(
                    request == null
                        ? null
                        : request.getHeader("User-Agent"),
                    1000
                )
        );

        accessLog.setRequestUri(
                trim(
                    request == null
                        ? null
                        : request.getRequestURI(),
                    1000
                )
        );

        accessLog.setHttpMethod(
                request == null
                    ? null
                    : request.getMethod()
        );

        accessLog.setResponseStatus(responseStatus);

        repository.save(accessLog);
    }

    @Transactional(readOnly = true)
    public List<FileAttachmentAccessLog> findLatest() {
        return repository
                .findTop500ByOrderByAccessedAtDesc();
    }

    private String resolveIp(
            HttpServletRequest request) {

        if (request == null) {
            return null;
        }

        String forwarded =
                request.getHeader("X-Forwarded-For");

        if (forwarded != null
                && !forwarded.isBlank()) {

            return trim(
                    forwarded.split(",")[0].trim(),
                    100
            );
        }

        return trim(
                request.getRemoteAddr(),
                100
        );
    }

    private String trim(
            String value,
            int maximumLength) {

        if (value == null) {
            return null;
        }

        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }
}