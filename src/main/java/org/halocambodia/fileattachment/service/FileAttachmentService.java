package org.halocambodia.fileattachment.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.halocambodia.data.User;
import org.halocambodia.fileattachment.data.FileAttachment;
import org.halocambodia.fileattachment.data.FileAttachmentOwnerType;
import org.halocambodia.fileattachment.data.FileAttachmentType;
import org.halocambodia.fileattachment.repository.FileAttachmentRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileAttachmentService {

    private final FileAttachmentRepository repository;
    private final AuthenticatedUser authenticatedUser;

    @Transactional(readOnly = true)
    public List<FileAttachment> findByOwner(
            FileAttachmentOwnerType ownerType,
            Long ownerId) {

        if (ownerType == null || ownerId == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
            repository
                .findByOwnerTypeAndOwnerIdAndDeletedFalseOrderBySortOrderAscIdAsc(
                    ownerType,
                    ownerId
                )
        );
    }

    @Transactional(readOnly = true)
    public List<FileAttachment> findAll() {
        return repository.findAll().stream()
            .sorted(
                Comparator.comparing(
                    FileAttachment::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
                )
            )
            .toList();
    }

    @Transactional
    public List<FileAttachment> saveOwnerAttachments(
            FileAttachmentOwnerType ownerType,
            Long ownerId,
            Collection<FileAttachment> attachments,
            Collection<Long> deletedIds) {

        Objects.requireNonNull(ownerType, "File attachment owner type is required");
        Objects.requireNonNull(ownerId, "File attachment owner ID is required");

        User currentUser = authenticatedUser.get()
            .orElseThrow(() -> new IllegalStateException("User is not logged in"));

        softDeleteOwnedAttachments(
        	    ownerType,
        	    ownerId,
        	    deletedIds,
        	    currentUser
        	);

        List<FileAttachment> ordered = attachments == null
            ? new ArrayList<>()
            : new ArrayList<>(attachments);

        ordered.sort(
            Comparator.comparing(
                FileAttachment::getSortOrder,
                Comparator.nullsLast(Integer::compareTo)
            ).thenComparing(
                FileAttachment::getFileName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            )
        );

        for (int index = 0; index < ordered.size(); index++) {
            FileAttachment attachment = ordered.get(index);
            validate(attachment);

            attachment.setOwnerType(ownerType);
            attachment.setOwnerId(ownerId);
            attachment.setSortOrder(index + 1);
            attachment.setFileCategory(
                FileAttachmentType.detect(
                    attachment.getMimeType(),
                    attachment.getFileName()
                )
            );

            if (attachment.getId() == null) {
                attachment.setUserCreated(currentUser);
            }
            attachment.setUserUpdated(currentUser);
        }

        return repository.saveAll(ordered);
    }

    private void softDeleteOwnedAttachments(
            FileAttachmentOwnerType ownerType,
            Long ownerId,
            Collection<Long> deletedIds,
            User currentUser) {

        if (deletedIds == null || deletedIds.isEmpty()) {
            return;
        }

        List<Long> validIds = deletedIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (validIds.isEmpty()) {
            return;
        }

        List<FileAttachment> attachmentsToDelete =
            repository.findAllByOwnerTypeAndOwnerIdAndIdInAndDeletedFalse(
                ownerType,
                ownerId,
                validIds
            );

        if (attachmentsToDelete.isEmpty()) {
            return;
        }

        ZonedDateTime deletedAt =ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        for (FileAttachment attachment : attachmentsToDelete) {
            attachment.setDeleted(true);
            attachment.setDeletedAt(deletedAt);
            attachment.setDeletedBy(currentUser);

            attachment.setUserUpdated(currentUser);
            attachment.setUpdatedAt(deletedAt);
        }

        repository.saveAll(attachmentsToDelete);
    }
    
    private void validate(FileAttachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("File attachment cannot be null");
        }
        if (attachment.getFileUuid() == null) {
            throw new IllegalArgumentException("File UUID is required");
        }
        if (attachment.getFileName() == null || attachment.getFileName().isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        if (attachment.getMimeType() == null || attachment.getMimeType().isBlank()) {
            attachment.setMimeType("application/octet-stream");
        }
        if (attachment.getAttachmentType() == null) {
            throw new IllegalArgumentException(
                "Attachment type is required for file: " + attachment.getFileName()
            );
        }
    }
    
    @Transactional
    public void restoreAttachment(
            FileAttachmentOwnerType ownerType,
            Long ownerId,
            Long attachmentId) {

        Objects.requireNonNull(ownerType);
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(attachmentId);

        User currentUser = authenticatedUser.get()
            .orElseThrow(() ->
                new IllegalStateException("User is not logged in")
            );

        FileAttachment attachment = repository.findById(attachmentId)
            .filter(item -> item.getOwnerType() == ownerType)
            .filter(item -> ownerId.equals(item.getOwnerId()))
            .filter(FileAttachment::isDeleted)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Deleted attachment was not found."
                )
            );

        attachment.setDeleted(false);
        attachment.setDeletedAt(null);
        attachment.setDeletedBy(null);
        attachment.setUserUpdated(currentUser);

        repository.save(attachment);
    }
}
