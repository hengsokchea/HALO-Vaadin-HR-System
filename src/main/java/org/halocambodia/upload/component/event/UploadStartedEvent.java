package org.halocambodia.upload.component.event;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

@DomEvent("upload-started")
public class UploadStartedEvent extends ComponentEvent<Component> {
    private final String uploadId;
    private final String fileName;
    private final long fileSize;

    public UploadStartedEvent(Component source, boolean fromClient,
                              @EventData("event.detail.uploadId") String uploadId,
                              @EventData("event.detail.fileName") String fileName,
                              @EventData("event.detail.fileSize") double fileSize) {
        super(source, fromClient);
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.fileSize = (long) fileSize;
    }

    public String getUploadId() { return uploadId; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
}
