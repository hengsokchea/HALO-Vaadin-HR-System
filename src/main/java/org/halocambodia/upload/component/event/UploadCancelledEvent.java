package org.halocambodia.upload.component.event;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

@DomEvent("upload-cancelled")
public class UploadCancelledEvent extends ComponentEvent<Component> {
    private final String uploadId;
    private final String fileName;

    public UploadCancelledEvent(Component source, boolean fromClient,
                                @EventData("event.detail.uploadId") String uploadId,
                                @EventData("event.detail.fileName") String fileName) {
        super(source, fromClient);
        this.uploadId = uploadId;
        this.fileName = fileName;
    }

    public String getUploadId() { return uploadId; }
    public String getFileName() { return fileName; }
}
