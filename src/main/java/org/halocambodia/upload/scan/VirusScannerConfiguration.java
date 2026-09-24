package org.halocambodia.upload.scan;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirusScannerConfiguration {

    @Bean
    @ConditionalOnMissingBean(VirusScanner.class)
    VirusScanner noOpVirusScanner() {
        return file -> VirusScanResult.clean("disabled");
    }
}