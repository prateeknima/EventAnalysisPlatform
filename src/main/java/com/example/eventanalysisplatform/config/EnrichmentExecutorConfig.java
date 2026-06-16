package com.example.eventanalysisplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Defines the dedicated executor used for incident enrichment work.
 *
 * Keeping enrichment on its own thread pool prevents it from using the JVM common
 * pool and gives us explicit control over concurrency, queue size, thread names,
 * and overload behavior.
 */
@Configuration
public class EnrichmentExecutorConfig {

    @Bean
    public Executor enrichmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("incident-enrichment-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}