package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.record.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IncidentServiceTest {

    private IncidentProducer producer;
    private RedisService redisService;
    private IncidentService incidentService;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        producer = mock(IncidentProducer.class);
        redisService = mock(RedisService.class);
        rateLimiterService = mock(RateLimiterService.class);

        incidentService = new IncidentService(
                producer,
                redisService,
                rateLimiterService
        );
    }

    @Test
    void handleUsesIncomingCorrelationId() {
        IncidentRequest request = new IncidentRequest(
                "INC-SERVICE-1",
                "payment",
                "HIGH",
                "timeout"
        );

        incidentService.handle(request, "corr-123");

        verify(redisService).saveStatus(
                "INC-SERVICE-1",
                IncidentStatus.RECEIVED
        );

        verify(producer).publish(new IncidentEvent(
                "INC-SERVICE-1",
                "payment",
                "HIGH",
                "timeout",
                "corr-123"
        ));
    }

    @Test
    void handleGeneratesCorrelationIdWhenMissing() {
        IncidentRequest request = new IncidentRequest(
                "INC-SERVICE-2",
                "checkout",
                "LOW",
                "slow response"
        );

        incidentService.handle(request, null);

        ArgumentCaptor<IncidentEvent> eventCaptor =
                ArgumentCaptor.forClass(IncidentEvent.class);

        verify(producer).publish(eventCaptor.capture());

        IncidentEvent event = eventCaptor.getValue();

        assertThat(event.incidentId()).isEqualTo("INC-SERVICE-2");
        assertThat(event.source()).isEqualTo("checkout");
        assertThat(event.severity()).isEqualTo("LOW");
        assertThat(event.message()).isEqualTo("slow response");
        assertThat(event.correlationId()).isNotBlank();
    }

    @Test
    void handleGeneratesCorrelationIdWhenBlank() {
        IncidentRequest request = new IncidentRequest(
                "INC-SERVICE-3",
                "checkout",
                "LOW",
                "blank correlation"
        );

        incidentService.handle(request, "   ");

        ArgumentCaptor<IncidentEvent> eventCaptor =
                ArgumentCaptor.forClass(IncidentEvent.class);

        verify(producer).publish(eventCaptor.capture());

        IncidentEvent event = eventCaptor.getValue();

        assertThat(event.incidentId()).isEqualTo("INC-SERVICE-3");
        assertThat(event.correlationId()).isNotBlank();
    }
}