package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.record.IncidentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentProducer producer;
    private final RedisService redisService;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentService.class);
    private final RateLimiterService rateLimiterService;

    public IncidentService(
            IncidentProducer producer,
            RedisService redisService,
            RateLimiterService rateLimiterService
    ) {
        this.producer = producer;
        this.redisService = redisService;
        this.rateLimiterService = rateLimiterService;
    }

    public void handle(IncidentRequest incidentRequest, String incomingCorrelationId) {
        rateLimiterService.checkLimit(incidentRequest.source());
        String correlationId = incomingCorrelationId != null && !incomingCorrelationId.isBlank()
                ? incomingCorrelationId
                : UUID.randomUUID().toString();
        IncidentEvent incidentEvent = new IncidentEvent(
                incidentRequest.incidentId(),
                incidentRequest.source(),
                incidentRequest.severity(),
                incidentRequest.message(),
                correlationId
        );
        log.info(
                "Received incident: {} correlationId={}",
                incidentEvent.incidentId(),
                incidentEvent.correlationId()
        );
        redisService.saveStatus(
                incidentEvent.incidentId(),
                IncidentStatus.RECEIVED
        );
        producer.publish(incidentEvent);
    }
}