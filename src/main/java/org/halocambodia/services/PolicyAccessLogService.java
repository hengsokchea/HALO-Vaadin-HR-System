package org.halocambodia.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.halocambodia.data.Policy;
import org.halocambodia.data.PolicyAccessAction;
import org.halocambodia.data.PolicyAccessLog;
import org.halocambodia.data.PolicyAccessLogRepository;
import org.halocambodia.data.PolicyRepository;
import org.halocambodia.fileattachment.data.FileAttachment;
import org.halocambodia.fileattachment.data.FileAttachmentOwnerType;
import org.halocambodia.fileattachment.repository.FileAttachmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Stores policy access history.
 */
@Service
@RequiredArgsConstructor
public class PolicyAccessLogService   implements GenericService<PolicyAccessLog> {

    private final PolicyAccessLogRepository repository;
    private final CurrentEmployeeService currentEmployeeService;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final PolicyRepository policyRepository;

    /**
     * Records an action when the Policy and optional FileAttachment are already
     * known by the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Policy policy,
            PolicyAccessAction action,
            FileAttachment attachment,
            HttpServletRequest request) {

        if (policy == null || action == null) {
            return;
        }

        saveLog(
                policy,
                action,
                attachment,
                request
        );
    }

    /**
     * Records preview/download activity received through the generic endpoint:
     *
     * /api/uploads/{fileUuid}/preview
     * /api/uploads/{fileUuid}/download
     *
     * The method records a row only when:
     * 1. An active FileAttachment exists for the UUID.
     * 2. The attachment owner type is POLICY.
     * 3. The owning Policy still exists.
     *
     * Attachments belonging to other modules are ignored.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttachmentAccess(
            UUID fileUuid,
            PolicyAccessAction action,
            HttpServletRequest request) {

        if (fileUuid == null || action == null) {
            return;
        }

        if (action != PolicyAccessAction.PREVIEWED_ATTACHMENT
                && action != PolicyAccessAction.DOWNLOADED_ATTACHMENT) {

            return;
        }

        FileAttachment attachment =fileAttachmentRepository.findByFileUuidAndDeletedFalse(fileUuid) .orElse(null);

        if (attachment == null || attachment.getOwnerType()  != FileAttachmentOwnerType.POLICY || attachment.getOwnerId() == null) {

            return;
        }

        Policy policy = policyRepository .findById(attachment.getOwnerId()) .orElse(null);

        if (policy == null) {
            return;
        }

        saveLog(
                policy,
                action,
                attachment,
                request
        );
    }

    private void saveLog( Policy policy, PolicyAccessAction action, FileAttachment attachment, HttpServletRequest request) {

        PolicyAccessLog accessLog = new PolicyAccessLog();

        accessLog.setPolicy(policy);
        accessLog.setUser(currentEmployeeService.requireUser());
        accessLog.setAction(action);
        accessLog.setFileAttachment(attachment);
        accessLog.setAccessedAt(OffsetDateTime.now());
        accessLog.setIpAddress(resolveIp(request));
        accessLog.setUserAgent(resolveUserAgent(request));

        repository.save(accessLog);
    }

    @Transactional(readOnly = true)
    public List<PolicyAccessLog> findLatest() {
        return repository
                .findTop500ByOrderByAccessedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PolicyAccessLog> list(
            Pageable pageable,
            Specification<PolicyAccessLog> specification) {

        return repository.findAll(
                specification,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long count(
            Specification<PolicyAccessLog> specification) {

        return repository.count(specification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyAccessLog> findAll(
            Specification<PolicyAccessLog> specification) {

        return repository.findAll(specification);
    }

    /**
     * Access logs are immutable audit records.
     */
    @Override
    public PolicyAccessLog update(
            PolicyAccessLog entity) {

        throw new UnsupportedOperationException(
                "Policy access logs are read-only."
        );
    }

    /**
     * Access logs must not be deleted from the normal application UI.
     */
    @Override
    public void delete(
            Set<PolicyAccessLog> entities) {

        throw new UnsupportedOperationException(
                "Policy access logs cannot be deleted."
        );
    }

    public String resolveIp(
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

    public String resolveUserAgent(
            HttpServletRequest request) {

        return trim(
                request == null
                        ? null
                        : request.getHeader("User-Agent"),
                1000
        );
    }

    private String trim(
            String value,
            int max) {

        if (value == null) {
            return null;
        }

        return value.length() <= max
                ? value
                : value.substring(0, max);
    }
}
