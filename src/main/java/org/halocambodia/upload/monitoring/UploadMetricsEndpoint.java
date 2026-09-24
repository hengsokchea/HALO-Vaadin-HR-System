package org.halocambodia.upload.monitoring;

import lombok.RequiredArgsConstructor;
import org.halocambodia.upload.operations.UploadStatisticsService;
import org.halocambodia.upload.operations.UploadSystemSnapshot;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "haloUpload")
@RequiredArgsConstructor
public class UploadMetricsEndpoint {

    private final UploadStatisticsService statisticsService;

    @ReadOperation
    public UploadSystemSnapshot uploadStatus() {
        return statisticsService.snapshot();
    }
}
