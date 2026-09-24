package org.halocambodia.upload.component;

import java.util.UUID;
import java.util.function.Function;

import com.vaadin.flow.data.renderer.ComponentRenderer;

public final class ThumbnailRendererFactory {

    private ThumbnailRendererFactory() {
    }

    public static <T> ComponentRenderer<FileThumbnail, T> create(
            Function<T, UUID> uploadUuidProvider) {

        return new ComponentRenderer<>(item -> {
            FileThumbnail thumbnail = new FileThumbnail();
            thumbnail.setUploadUuid(uploadUuidProvider.apply(item));
            return thumbnail;
        });
    }
}
