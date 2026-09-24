package org.halocambodia.upload.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.UUID;

public class UploadedFileActions extends HorizontalLayout {

    public UploadedFileActions(UUID uploadId, boolean allowPreview, boolean allowDownload) {
        setPadding(false);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        if (allowPreview) {
            Button preview = new Button("Preview", VaadinIcon.EYE.create());
            preview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            preview.addClickListener(event -> UI.getCurrent().getPage()
                    .open("/api/uploads/" + uploadId + "/preview", "_blank"));
            add(preview);
        }

        if (allowDownload) {
            Button download = new Button("Download", VaadinIcon.DOWNLOAD.create());
            download.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            download.addClickListener(event -> UI.getCurrent().getPage()
                    .open("/api/uploads/" + uploadId + "/download", "_blank"));
            add(download);
        }
    }
}
