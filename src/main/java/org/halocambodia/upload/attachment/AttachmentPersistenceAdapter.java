package org.halocambodia.upload.attachment;

/**
 * Implement this interface in the host HRMIS application to convert completed
 * uploads into existing domain attachment entities such as PolicyAttachment.
 */
public interface AttachmentPersistenceAdapter {

    boolean supports(AttachmentTarget target);

    Object persist(AttachmentTarget target, AttachmentMetadata metadata);
}
