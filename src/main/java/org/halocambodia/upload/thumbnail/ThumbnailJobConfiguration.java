package org.halocambodia.upload.thumbnail;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(ThumbnailJobProperties.class)
public class ThumbnailJobConfiguration {

    @Bean(name = "thumbnailExecutor")
    public ThreadPoolTaskExecutor thumbnailExecutor(ThumbnailJobProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("thumbnail-");
        executor.setCorePoolSize(properties.corePoolSize());
        executor.setMaxPoolSize(properties.corePoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
