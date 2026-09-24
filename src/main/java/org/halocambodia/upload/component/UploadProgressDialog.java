package org.halocambodia.upload.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.halocambodia.upload.domain.UploadType;

public class UploadProgressDialog extends Dialog {

    private final LargeFileUploader uploader = new LargeFileUploader();
    private final Span status = new Span("Select or drop files to begin.");

    public UploadProgressDialog(UploadType uploadType) {
        setHeaderTitle("Upload Files | បញ្ចូលឯកសារ");
        setWidth("760px");
        setMaxWidth("95vw");
        setCloseOnOutsideClick(false);

        uploader.setUploadType(uploadType);
        uploader.addStartedListener(e -> status.setText("Uploading: " + e.getFileName()));
        uploader.addProgressListener(e -> status.setText(String.format(
                "%s — %.1f%%", e.getFileName(), e.getProgressPercent())));
        uploader.addCompletedListener(e -> status.setText("Completed: " + e.getOriginalFileName()));
        uploader.addFailedListener(e -> status.setText("Failed: " + e.getMessage()));
        uploader.addCancelledListener(e -> status.setText("Cancelled: " + e.getFileName()));

        Button pause = new Button("Pause", VaadinIcon.PAUSE.create(), e -> uploader.pauseAll());
        Button resume = new Button("Resume", VaadinIcon.PLAY.create(), e -> uploader.resumeAll());
        Button cancel = new Button("Cancel All", VaadinIcon.CLOSE.create(), e -> uploader.cancelAll());
        Button close = new Button("Close", e -> close());

        HorizontalLayout actions = new HorizontalLayout(pause, resume, cancel, close);
        VerticalLayout body = new VerticalLayout(uploader, status, actions);
        body.setPadding(false);
        body.setWidthFull();
        add(body);
    }

    public LargeFileUploader getUploader() {
        return uploader;
    }
}
