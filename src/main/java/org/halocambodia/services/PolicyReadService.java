package org.halocambodia.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.halocambodia.data.Policy;
import org.halocambodia.data.PolicyAcknowledge;
import org.halocambodia.data.PolicyAttachmentDto;
import org.halocambodia.data.PolicyCategory;
import org.halocambodia.data.PolicyReadDto;
import org.halocambodia.data.PolicyReadRepository;
import org.halocambodia.fileattachment.data.FileAttachment;
import org.halocambodia.fileattachment.data.FileAttachmentOwnerType;
import org.halocambodia.fileattachment.repository.FileAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyReadService {

    private final PolicyReadRepository policyRepository;
    private final FileAttachmentRepository attachmentRepository;
    private final PolicyAcknowledgeService acknowledgeService;
    private final CurrentEmployeeService currentEmployeeService;

    /**
     * Finds one policy that the current authenticated user is allowed to read.
     *
     * The acknowledgement status must not affect whether the policy is
     * available. A policy should remain readable after acknowledgement.
     */
    @Transactional(readOnly = true)
    public Optional<PolicyReadDto> findAvailableByPublicToken(
            UUID publicToken) {

        currentEmployeeService.requireUser();

        if (publicToken == null) {
            return Optional.empty();
        }

        LocalDate today = LocalDate.now();

        return policyRepository
                .findAvailableByPublicToken(publicToken, today)
                .map(this::toDto);
    }

    /**
     * Returns all currently available policies for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<PolicyReadDto> findAllAvailable() {

        currentEmployeeService.requireUser();

        LocalDate today = LocalDate.now();

        List<Policy> policies =
                policyRepository.findAllAvailable(today);

        if (policies == null || policies.isEmpty()) {
            return Collections.emptyList();
        }

        return policies.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Returns the policy entity for internal actions such as acknowledgement,
     * printing and access logging.
     */
    @Transactional(readOnly = true)
    public Optional<Policy> findAvailableEntity(
            UUID publicToken) {

        currentEmployeeService.requireUser();

        if (publicToken == null) {
            return Optional.empty();
        }

        return policyRepository.findAvailableByPublicToken(
                publicToken,
                LocalDate.now()
        );
    }

    /**
     * Finds an attachment only when:
     *
     * 1. The policy is currently available.
     * 2. The file belongs to the POLICY owner type.
     * 3. The file owner ID matches the policy ID.
     */
    @Transactional(readOnly = true)
    public Optional<FileAttachment> findOwnedAttachment(
            UUID publicToken,
            UUID fileUuid) {

        currentEmployeeService.requireUser();

        if (publicToken == null || fileUuid == null) {
            return Optional.empty();
        }

        return policyRepository
                .findAvailableByPublicToken(
                        publicToken,
                        LocalDate.now()
                )
                .flatMap(policy ->
                        attachmentRepository
                                .findByFileUuidAndDeletedFalse(fileUuid)
                                .filter(this::isPolicyAttachment)
                                .filter(file ->
                                        policy.getId() != null
                                                && policy.getId().equals(
                                                        file.getOwnerId()
                                                )
                                )
                );
    }

    /**
     * Converts the Policy entity to the employee-facing read DTO.
     */
    private PolicyReadDto toDto(Policy policy) {

        if (policy == null) {
            throw new IllegalArgumentException(
                    "Policy cannot be null."
            );
        }

        List<PolicyAttachmentDto> attachments =
                loadAttachments(policy);

        Optional<PolicyAcknowledge> acknowledgement =
                acknowledgeService.findCurrent(policy);

        PolicyCategory category =
                policy.getPolicyCategory();

        String categoryCode =
                category == null
                        ? null
                        : category.getCode();

        String categoryNameEn =
                category == null
                        ? null
                        : category.getNameEn();

        String categoryNameKh =
                category == null
                        ? null
                        : category.getNameKh();

        boolean acknowledgedCurrentVersion =
                acknowledgement.isPresent();

        return new PolicyReadDto(
                policy.getId(),
                policy.getPublicToken(),
                policy.getCode(),
                policy.getTitleEn(),
                policy.getTitleKh(),
                policy.getSummary(),
                policy.getContent(),
                policy.getEffectiveDate(),
                policy.getExpiryDate(),
                policy.getVersion(),
                policy.isRequiredAcknowledge(),
                categoryCode,
                categoryNameEn,
                categoryNameKh,
                acknowledgedCurrentVersion,
                acknowledgement
                        .map(PolicyAcknowledge::getAcknowledgedAt)
                        .orElse(null),
                attachments
        );
    }

    /**
     * Loads attachments belonging to the policy.
     */
    private List<PolicyAttachmentDto> loadAttachments(
            Policy policy) {

        if (policy.getId() == null) {
            return Collections.emptyList();
        }

        List<FileAttachment> files =
                attachmentRepository
                        .findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAscIdAsc(
                                FileAttachmentOwnerType.POLICY,
                                policy.getId()
                        );

        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        return files.stream()
                .filter(this::isPolicyAttachment)
                .map(this::toAttachmentDto)
                .toList();
    }

    /**
     * Converts a file attachment entity into the policy attachment DTO.
     */
    private PolicyAttachmentDto toAttachmentDto(
            FileAttachment file) {

        return new PolicyAttachmentDto(
                file.getFileUuid(),
                file.getFileName(),
                file.getMimeType(),
                file.getFileSize(),
                file.getDescription(),
                file.getSortOrder()
        );
    }

    /**
     * Confirms that the attachment belongs to the policy module.
     */
    private boolean isPolicyAttachment(
            FileAttachment file) {

        return file != null
                && file.getOwnerType()
                        == FileAttachmentOwnerType.POLICY;
    }
}