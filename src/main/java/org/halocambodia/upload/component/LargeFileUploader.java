package org.halocambodia.upload.component;

import org.halocambodia.upload.component.event.UploadCancelledEvent;
import org.halocambodia.upload.component.event.UploadCompletedEvent;
import org.halocambodia.upload.component.event.UploadFailedEvent;
import org.halocambodia.upload.component.event.UploadProgressEvent;
import org.halocambodia.upload.component.event.UploadStartedEvent;
import org.halocambodia.upload.domain.UploadType;
import org.halocambodia.utility.ApplicationUrlUtil;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

@Tag("halo-large-file-uploader")
@JsModule("./uploader/halo-large-file-uploader.js")
public class LargeFileUploader extends Component implements HasSize {

    private static final long ONE_MB = 1024L * 1024L;
    private static final long DEFAULT_CHUNK_SIZE = 20L * ONE_MB;
    private static final long DEFAULT_MAX_FILE_SIZE =
            20L * 1024L * 1024L * 1024L;

    public LargeFileUploader() {
        setWidthFull();

        /*
         * Development:
         * /api/uploads
         *
         * Production under /hr:
         * /hr/api/uploads
         */
        setEndpoint(
                ApplicationUrlUtil.contextUrl("/api/uploads")
        );

        setUploadType(UploadType.OTHER);
        setMaxFiles(10);
        setRequestedChunkSize(DEFAULT_CHUNK_SIZE);
        setMaxFileSize(DEFAULT_MAX_FILE_SIZE);
        setVerifyChunkChecksum(false);
        setDisabled(false);
    }

    public void setEndpoint(String endpoint) {
        String normalizedEndpoint =
                endpoint == null || endpoint.isBlank()
                        ? "api/uploads"
                        : endpoint.trim();

        while (normalizedEndpoint.length() > 1
                && normalizedEndpoint.endsWith("/")) {

            normalizedEndpoint =
                    normalizedEndpoint.substring(
                            0,
                            normalizedEndpoint.length() - 1
                    );
        }

        getElement().setProperty(
                "endpoint",
                normalizedEndpoint
        );
    }

    public String getEndpoint() {
        return getElement().getProperty(
                "endpoint",
                "api/uploads"
        );
    }

    public void setUploadType(UploadType uploadType) {
        getElement().setProperty(
                "uploadType",
                uploadType != null
                        ? uploadType.name()
                        : UploadType.OTHER.name()
        );
    }

    public void setAccept(String accept) {
        getElement().setProperty(
                "accept",
                accept != null ? accept : ""
        );
    }

    public void setMaxFiles(int maxFiles) {
        getElement().setProperty(
                "maxFiles",
                Math.max(1, maxFiles)
        );
    }

    public void setRequestedChunkSize(long bytes) {
        getElement().setProperty(
                "requestedChunkSize",
                Math.max(ONE_MB, bytes)
        );
    }

    public void setMaxFileSize(long bytes) {
        getElement().setProperty(
                "maxFileSize",
                Math.max(1L, bytes)
        );
    }

    public void setVerifyChunkChecksum(boolean enabled) {
        getElement().setProperty(
                "verifyChunkChecksum",
                enabled
        );
    }

    public void setDisabled(boolean disabled) {
        getElement().setProperty(
                "disabled",
                disabled
        );
    }

    public boolean isDisabled() {
        return getElement().getProperty(
                "disabled",
                false
        );
    }

    public void openFilePicker() {
        getElement().callJsFunction(
                "openFilePicker"
        );
    }

    public void pauseAll() {
        getElement().callJsFunction(
                "pauseAll"
        );
    }

    public void resumeAll() {
        getElement().callJsFunction(
                "resumeAll"
        );
    }

    public void cancelAll() {
        getElement().callJsFunction(
                "cancelAll"
        );
    }

    public Registration addStartedListener(
            ComponentEventListener<UploadStartedEvent> listener) {

        return ComponentUtil.addListener(
                this,
                UploadStartedEvent.class,
                listener
        );
    }

    public Registration addProgressListener(
            ComponentEventListener<UploadProgressEvent> listener) {

        return ComponentUtil.addListener(
                this,
                UploadProgressEvent.class,
                listener
        );
    }

    public Registration addCompletedListener(
            ComponentEventListener<UploadCompletedEvent> listener) {

        return ComponentUtil.addListener(
                this,
                UploadCompletedEvent.class,
                listener
        );
    }

    public Registration addFailedListener(
            ComponentEventListener<UploadFailedEvent> listener) {

        return ComponentUtil.addListener(
                this,
                UploadFailedEvent.class,
                listener
        );
    }

    public Registration addCancelledListener(
            ComponentEventListener<UploadCancelledEvent> listener) {

        return ComponentUtil.addListener(
                this,
                UploadCancelledEvent.class,
                listener
        );
    }
}