package org.halocambodia.fileattachment.data;

/**
 * Identifies the business record that owns a new disk-based file attachment.
 * Add new values when another module adopts FileAttachmentComponent.
 */
public enum FileAttachmentOwnerType {
    POLICY,
    EMPLOYEE,
    LEAVE,
    TRAINING,
    PERFORMANCE,
    DISCIPLINARY,
    MEDICAL,
    ASSET,
    FLEET,
    PROCUREMENT,
    FINANCE,
    OTHER
}
