package org.halocambodia.upload.thumbnail;

import org.halocambodia.upload.component.event.UploadCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UploadCompletedThumbnailListener {

    private final ThumbnailBackgroundService backgroundService;

    @EventListener
    public void onUploadCompleted(UploadCompletedEvent event) {
        /*
         * Adapt the UUID accessor below if your UploadCompletedEvent uses
         * getUploadUuid() instead of uploadUuid().
         */
        backgroundService.generate(
            new ThumbnailGenerationJob(
            		event.getUploadId(),
                320,
                240,
                0
            )
        );
    }
}
