package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.exception.IncidentConflictException;
import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentStatus;
import com.example.eventanalysisplatform.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IncidentEventConsumerTest {

    private RedisService redisService;
    private IncidentRepository incidentRepository;
    private KafkaTemplate<String, IncidentEvent> kafkaTemplate;
    private AsyncIncidentEnrichmentService asyncIncidentEnrichmentService;
    private IncidentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        incidentRepository = mock(IncidentRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        asyncIncidentEnrichmentService = mock(AsyncIncidentEnrichmentService.class);

        consumer = new IncidentEventConsumer(
                redisService,
                incidentRepository,
                kafkaTemplate,
                asyncIncidentEnrichmentService
        );
    }

    @Test
    void consumeProcessesIncidentSuccessfully() {
        IncidentEvent event = new IncidentEvent(
                "INC-CONSUMER-1",
                "payment",
                "HIGH",
                "timeout",
                "corr-123"
        );

        when(asyncIncidentEnrichmentService.enrichAsync(any(IncidentEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        consumer.consume(event);

        verify(redisService).saveStatus(
                "INC-CONSUMER-1",
                IncidentStatus.PROCESSING
        );

        verify(incidentRepository).save(event);

        verify(redisService).saveStatus(
                "INC-CONSUMER-1",
                IncidentStatus.PROCESSED
        );

        verify(asyncIncidentEnrichmentService).enrichAsync(event);

        verify(kafkaTemplate, never()).send(eq("incidents-dlt"), any(IncidentEvent.class));
    }

    @Test
    void consumeSendsConflictingIncidentToDlt() {
        IncidentEvent event = new IncidentEvent(
                "INC-CONSUMER-CONFLICT-1",
                "checkout",
                "LOW",
                "conflict",
                "corr-conflict"
        );

        doThrow(new IncidentConflictException("INC-CONSUMER-CONFLICT-1"))
                .when(incidentRepository)
                .save(any(IncidentEvent.class));

        consumer.consume(event);

        verify(redisService).saveStatus(
                "INC-CONSUMER-CONFLICT-1",
                IncidentStatus.PROCESSING
        );

        verify(redisService).saveStatus(
                "INC-CONSUMER-CONFLICT-1",
                IncidentStatus.CONFLICT
        );

        verify(kafkaTemplate).send("incidents-dlt", event);

        verify(redisService, never()).saveStatus(
                "INC-CONSUMER-CONFLICT-1",
                IncidentStatus.PROCESSED
        );

        verifyNoInteractions(asyncIncidentEnrichmentService);
    }
}