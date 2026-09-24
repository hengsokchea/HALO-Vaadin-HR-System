package org.halocambodia.upload.component;

import java.util.UUID;

import org.halocambodia.utility.ApplicationUrlUtil;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class FileThumbnail extends Div {

    private final Image image = new Image();
    private final Icon fallback = VaadinIcon.FILE.create();

    public FileThumbnail() {
        setWidth(96, Unit.PIXELS);
        setHeight(72, Unit.PIXELS);

        getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("overflow", "hidden")
            .set("border-radius", "8px")
            .set("background", "var(--lumo-contrast-5pct)");

        image.setAlt("File thumbnail");
        image.setWidthFull();
        image.setHeightFull();
        image.getStyle().set("object-fit", "cover");

        fallback.setSize("32px");
        fallback.getStyle().set(
            "color",
            "var(--lumo-secondary-text-color)"
        );

        // Image successfully loaded
        image.getElement().addEventListener(
            "load",
            event -> showImage()
        );

        // Image failed to load
        image.getElement().addEventListener(
            "error",
            event -> showFallback()
        );

        showFallback();
    }

    public void setUploadUuid(UUID uploadUuid) {
        showFallback();

        if (uploadUuid == null) {
            image.getElement().removeAttribute("src");
            return;
        }

        String thumbnailUrl = ApplicationUrlUtil.contextUrl(
            "/api/uploads/"
                + uploadUuid
                + "/thumbnail?width=192&height=144"
        );

        image.setSrc(thumbnailUrl);
    }

    private void showImage() {
        if (getChildren().noneMatch(component -> component == image)) {
            removeAll();
            add(image);
        }
    }

    private void showFallback() {
        if (getChildren().noneMatch(component -> component == fallback)) {
            removeAll();
            add(fallback);
        }
    }
}