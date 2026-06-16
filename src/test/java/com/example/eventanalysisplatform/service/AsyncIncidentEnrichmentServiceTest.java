package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentEnrichmentResult;
import com.example.eventanalysisplatform.record.IncidentEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncIncidentEnrichmentServiceTest {

    private final IncidentEnrichmentService incidentEnrichmentService =
            mock(IncidentEnrichmentService.class);

    private final Executor directExecutor = Runnable::run;

    @Test
    void enrichAsyncCompletesSuccessfully() {
        AsyncIncidentEnrichmentService service =
                new AsyncIncidentEnrichmentService(
                        incidentEnrichmentService,
                        directExecutor
                );

        IncidentEvent event = incidentEvent();

        IncidentEnrichmentResult expectedResult = new IncidentEnrichmentResult(
                "INC-ASYNC-1",
                81,
                "HIGH",
                List.of("payment", "checkout"),
                "Investigate affected service"
        );

        when(incidentEnrichmentService.enrich(event))
                .thenReturn(expectedResult);

        IncidentEnrichmentResult result =
                service.enrichAsync(event).join();

        assertThat(result).isEqualTo(expectedResult);
        assertThat(service.submittedCount()).isEqualTo(1);
        assertThat(service.completedCount()).isEqualTo(1);
        assertThat(service.failedCount()).isZero();
    }

    @Test
    void enrichAsyncTracksFailures() {
        AsyncIncidentEnrichmentService service =
                new AsyncIncidentEnrichmentService(
                        incidentEnrichmentService,
                        directExecutor
                );

        IncidentEvent event = incidentEvent();

        when(incidentEnrichmentService.enrich(event))
                .thenThrow(new IllegalStateException("enrichment failed"));

        assertThatThrownBy(() -> service.enrichAsync(event).join())
                .hasCauseInstanceOf(IllegalStateException.class);

        assertThat(service.submittedCount()).isEqualTo(1);
        assertThat(service.completedCount()).isZero();
        assertThat(service.failedCount()).isEqualTo(1);
    }

    @Test
    void enrichAsyncReturnsCompletedNullWhenDisabled() {
        AsyncIncidentEnrichmentService service =
                new AsyncIncidentEnrichmentService(
                        incidentEnrichmentService,
                        directExecutor
                );

        service.setEnrichmentEnabled(false);

        IncidentEnrichmentResult result =
                service.enrichAsync(incidentEvent()).join();

        assertThat(result).isNull();
        assertThat(service.submittedCount()).isZero();
        assertThat(service.completedCount()).isZero();
        assertThat(service.failedCount()).isZero();
    }

    @Test
    void enrichAsyncTracksRejectedTasks() {
        // Simulates a full executor queue rejecting new enrichment tasks.
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("queue full");
        };

        AsyncIncidentEnrichmentService service =
                new AsyncIncidentEnrichmentService(
                        incidentEnrichmentService,
                        rejectingExecutor
                );

        assertThatThrownBy(() -> service.enrichAsync(incidentEvent()).join())
                .hasCauseInstanceOf(RejectedExecutionException.class);

        assertThat(service.submittedCount()).isEqualTo(1);
        assertThat(service.completedCount()).isZero();
        assertThat(service.failedCount()).isEqualTo(1);
    }

    private IncidentEvent incidentEvent() {
        return new IncidentEvent(
                "INC-ASYNC-1",
                "payment",
                "HIGH",
                "timeout",
                "corr-async-1"
        );
    }
}