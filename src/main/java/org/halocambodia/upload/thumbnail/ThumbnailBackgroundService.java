package org.halocambodia.upload.thumbnail;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailBackgroundService {

    private final ThumbnailService thumbnailService;
    private final ThumbnailJobProperties properties;

    @Async("thumbnailExecutor")
    public CompletableFuture<Boolean> generate(ThumbnailGenerationJob job) {
        if (!properties.enabled()) {
            return CompletableFuture.completedFuture(false);
        }

        ThumbnailGenerationJob current = job;

        while (true) {
            try {
                ThumbnailDescriptor result = thumbnailService.getOrCreate(
                    current.uploadUuid(),
                    current.width(),
                    current.height()
                );
                return CompletableFuture.completedFuture(result.available());

            } catch (RuntimeException exception) {
                if (current.attempt() >= properties.maxRetries()) {
                    log.error(
                        "Thumbnail generation failed uploadUuid={} attempts={}",
                        current.uploadUuid(),
                        current.attempt() + 1,
                        exception
                    );
                    return CompletableFuture.completedFuture(false);
                }

                log.warn(
                    "Retrying thumbnail generation uploadUuid={} attempt={}",
                    current.uploadUuid(),
                    current.attempt() + 2
                );

                try {
                    Thread.sleep(properties.retryDelay().toMillis());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return CompletableFuture.completedFuture(false);
                }

                current = current.retry();
            }
        }
    }
}
