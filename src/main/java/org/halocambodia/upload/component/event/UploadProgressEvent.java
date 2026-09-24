package org.halocambodia.upload.component.event;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

@DomEvent("upload-progress")
public class UploadProgressEvent extends ComponentEvent<Component> {
    private final String uploadId;
    private final String fileName;
    private final long uploadedBytes;
    private final long totalBytes;
    private final double progressPercent;
    private final double speedBytesPerSecond;
    private final long etaSeconds;

    public UploadProgressEvent(Component source, boolean fromClient,
                               @EventData("event.detail.uploadId") String uploadId,
                               @EventData("event.detail.fileName") String fileName,
                               @EventData("event.detail.uploadedBytes") double uploadedBytes,
                               @EventData("event.detail.totalBytes") double totalBytes,
                               @EventData("event.detail.progressPercent") double progressPercent,
                               @EventData("event.detail.speedBytesPerSecond") double speedBytesPerSecond,
                               @EventData("event.detail.etaSeconds") double etaSeconds) {
        super(source, fromClient);
        this.uploadId = uploadId;
        this.fileName = fileName;
        this.uploadedBytes = (long) uploadedBytes;
        this.totalBytes = (long) totalBytes;
        this.progressPercent = progressPercent;
        this.speedBytesPerSecond = speedBytesPerSecond;
        this.etaSeconds = (long) etaSeconds;
    }

    public String getUploadId() { return uploadId; }
    public String getFileName() { return fileName; }
    public long getUploadedBytes() { return uploadedBytes; }
    public long getTotalBytes() { return totalBytes; }
    public double getProgressPercent() { return progressPercent; }
    public double getSpeedBytesPerSecond() { return speedBytesPerSecond; }
    public long getEtaSeconds() { return etaSeconds; }
}
