package org.halocambodia.fileattachment.component.event;

import org.halocambodia.fileattachment.component.FileAttachmentComponent;

import com.vaadin.flow.component.ComponentEvent;

public class FileAttachmentChangedEvent
        extends ComponentEvent<FileAttachmentComponent> {

    private final int attachmentCount;
    private final boolean pendingChanges;

    public FileAttachmentChangedEvent(
            FileAttachmentComponent source,
            int attachmentCount,
            boolean pendingChanges) {
        super(source, false);
        this.attachmentCount = attachmentCount;
        this.pendingChanges = pendingChanges;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public boolean hasPendingChanges() {
        return pendingChanges;
    }
}
