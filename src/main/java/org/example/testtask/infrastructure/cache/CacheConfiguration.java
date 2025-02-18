package org.example.testtask.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheConfiguration {
    private long timeoutHours = 24;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
}