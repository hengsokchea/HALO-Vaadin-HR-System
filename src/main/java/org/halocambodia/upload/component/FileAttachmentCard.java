package org.halocambodia.upload.component;

import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class FileAttachmentCard extends HorizontalLayout {

    private final FileThumbnail thumbnail = new FileThumbnail();
    private final Span name = new Span();
    private final Span details = new Span();
    private final Div actions = new Div();

    public FileAttachmentCard() {
        setWidthFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(FlexComponent.Alignment.CENTER);

        getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-l)");

        name.getStyle().set("font-weight", "600");
        details.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout text = new VerticalLayout(name, details);
        text.setPadding(false);
        text.setSpacing(false);
        text.setMinWidth(0, Unit.PIXELS);

        add(thumbnail, text, actions);
        expand(text);
    }

    public void setFile(
            UUID uploadUuid,
            String fileName,
            String mimeType,
            long fileSize) {

        thumbnail.setUploadUuid(uploadUuid);
        name.setText(fileName == null ? "Unnamed file" : fileName);
        details.setText(
            (mimeType == null ? "Unknown type" : mimeType)
                + " • " + humanSize(fileSize)
        );
    }

    public void setActions(Component... components) {
        actions.removeAll();
        actions.add(components);
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024D;
        if (kb < 1024) return "%.1f KB".formatted(kb);
        double mb = kb / 1024D;
        if (mb < 1024) return "%.1f MB".formatted(mb);
        return "%.1f GB".formatted(mb / 1024D);
    }
}
