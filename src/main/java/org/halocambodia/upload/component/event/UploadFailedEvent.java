package org.halocambodia.upload.component.event;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

@DomEvent("upload-failed")
public class UploadFailedEvent extends ComponentEvent<Component> {
    private final String uploadId;
    private final String fileName;
    private final String message;

    public UploadFailedEvent(Component source, boolean fromClient,
                             @EventData("event.detail.uploadId") String uploadId,
                             @EventData("event.detail.fileName") String fileName,
                             @EventData("event.detail.message") String message) {
        super(source, fromClient);
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.message = message;
    }

    public String getUploadId() { return uploadId; }
    public String getFileName() { return fileName; }
    public String getMessage() { return message; }
}
