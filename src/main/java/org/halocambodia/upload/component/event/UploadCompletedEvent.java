package org.halocambodia.upload.component.event;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

import java.util.UUID;

@DomEvent("upload-completed")
public class UploadCompletedEvent extends ComponentEvent<Component> {
    private final UUID uploadId;
    private final String originalFileName;
    private final String storedFileName;
    private final String mimeType;
    private final long fileSize;
    private final String checksumSha256;
    private final String downloadUrl;
    private final String previewUrl;

    public UploadCompletedEvent(Component source, boolean fromClient,
                                @EventData("event.detail.uploadId") String uploadId,
                                @EventData("event.detail.originalFileName") String originalFileName,
                                @EventData("event.detail.storedFileName") String storedFileName,
                                @EventData("event.detail.mimeType") String mimeType,
                                @EventData("event.detail.fileSize") double fileSize,
                                @EventData("event.detail.checksumSha256") String checksumSha256,
                                @EventData("event.detail.downloadUrl") String downloadUrl,
                                @EventData("event.detail.previewUrl") String previewUrl) {
        super(source, fromClient);
        this.uploadId = UUID.fromString(uploadId);
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.mimeType = mimeType;
        this.fileSize = (long) fileSize;
        this.checksumSha256 = checksumSha256;
        this.downloadUrl = downloadUrl;
        this.previewUrl = previewUrl;
    }

    public UUID getUploadId() { return uploadId; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStoredFileName() { return storedFileName; }
    public String getMimeType() { return mimeType; }
    public long getFileSize() { return fileSize; }
    public String getChecksumSha256() { return checksumSha256; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getPreviewUrl() { return previewUrl; }
}
