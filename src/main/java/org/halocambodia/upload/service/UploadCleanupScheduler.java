package org.halocambodia.upload.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadCleanupScheduler {

    private final UploadCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${halo.upload.cleanup-interval:PT1H}")
    public void cleanExpiredUploads() {
        int count = cleanupService.expireAbandonedUploads();
        if (count > 0) {
            log.info("Expired {} abandoned upload session(s)", count);
        }
    }
}
