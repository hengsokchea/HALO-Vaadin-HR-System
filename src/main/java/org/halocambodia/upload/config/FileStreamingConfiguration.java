package org.halocambodia.upload.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileStreamingProperties.class)
public class FileStreamingConfiguration {
}
