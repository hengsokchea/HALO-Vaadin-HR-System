package org.halocambodia.upload.monitoring;


import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.operations.UploadStatisticsService;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class UploadMetricsConfiguration {

    private final MeterRegistry meterRegistry;
    private final UploadStatisticsService statisticsService;

    @PostConstruct
    void registerGauges() {
        Gauge.builder("halo.upload.active", statisticsService,
                        service -> service.snapshot().activeUploads())
                .description("Active HALO upload sessions")
                .register(meterRegistry);
        Gauge.builder("halo.upload.completed", statisticsService,
                        service -> service.snapshot().completedUploads())
                .description("Completed HALO upload sessions")
                .register(meterRegistry);
        Gauge.builder("halo.upload.storage.bytes", statisticsService,
                        service -> service.snapshot().physicalFileBytes())
                .description("Bytes used by completed HALO upload files")
                .register(meterRegistry);
        Gauge.builder("halo.thumbnail.files", statisticsService,
                        service -> service.snapshot().thumbnailCount())
                .description("Cached HALO thumbnail files")
                .register(meterRegistry);
        Gauge.builder("halo.thumbnail.queue", statisticsService,
                        service -> service.snapshot().thumbnailQueueSize())
                .description("Queued thumbnail generation jobs")
                .register(meterRegistry);
        Gauge.builder("halo.upload.disk.usable.bytes", statisticsService,
                        service -> service.snapshot().usableDiskBytes())
                .description("Usable bytes on the upload filesystem")
                .register(meterRegistry);
    }
}
