package org.halocambodia.upload.retention;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadRetentionScheduler {
    private final UploadRetentionService retentionService;

    @Scheduled(cron = "${halo.upload.retention-cron:0 30 2 * * *}")
    public void enforceRetention() {
        int softDeleted = retentionService.applyCompletedRetention();
        int purged = retentionService.purgeExpiredDeletedFiles();
        if (softDeleted > 0 || purged > 0) {
            log.info("Upload retention completed: softDeleted={}, purged={}", softDeleted, purged);
        }
    }
}
