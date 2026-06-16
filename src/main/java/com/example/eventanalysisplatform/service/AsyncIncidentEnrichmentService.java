package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentEnrichmentResult;
import com.example.eventanalysisplatform.record.IncidentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runs incident enrichment asynchronously after the main incident processing path.
 *
 * The executor controls how many enrichment tasks can run or wait, AtomicLong
 * counters track task outcomes safely across threads, and the volatile flag allows
 * enrichment to be enabled or disabled across worker threads.
 */
@Service
public class AsyncIncidentEnrichmentService {

    private static final Logger log =
            LoggerFactory.getLogger(AsyncIncidentEnrichmentService.class);

    private final IncidentEnrichmentService incidentEnrichmentService;
    private final Executor enrichmentExecutor;

    private final AtomicLong submittedCount = new AtomicLong();
    private final AtomicLong completedCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();

    private volatile boolean enrichmentEnabled = true;

    public AsyncIncidentEnrichmentService(
            IncidentEnrichmentService incidentEnrichmentService,
            Executor enrichmentExecutor
    ) {
        this.incidentEnrichmentService = incidentEnrichmentService;
        this.enrichmentExecutor = enrichmentExecutor;
    }

    public CompletableFuture<IncidentEnrichmentResult> enrichAsync(IncidentEvent incidentEvent) {
        if (!enrichmentEnabled) {
            log.info("Incident enrichment skipped because enrichment is disabled");
            return CompletableFuture.completedFuture(null);
        }

        submittedCount.incrementAndGet();

        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    IncidentEnrichmentResult result =
                            incidentEnrichmentService.enrich(incidentEvent);

                    completedCount.incrementAndGet();

                    log.info(
                            "Incident enrichment completed incidentId={} priorityScore={} riskLevel={}",
                            result.incidentId(),
                            result.priorityScore(),
                            result.riskLevel()
                    );

                    return result;
                } catch (RuntimeException exception) {
                    failedCount.incrementAndGet();
                    throw exception;
                }
            }, enrichmentExecutor);
        } catch (RejectedExecutionException exception) {
            failedCount.incrementAndGet();

            log.warn(
                    "Incident enrichment rejected incidentId={}",
                    incidentEvent.incidentId(),
                    exception
            );

            return CompletableFuture.failedFuture(exception);
        }
    }

    public long submittedCount() {
        return submittedCount.get();
    }

    public long completedCount() {
        return completedCount.get();
    }

    public long failedCount() {
        return failedCount.get();
    }

    public void setEnrichmentEnabled(boolean enrichmentEnabled) {
        this.enrichmentEnabled = enrichmentEnabled;
    }
}