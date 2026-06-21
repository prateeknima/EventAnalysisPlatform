package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.exception.IncidentConflictException;
import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.record.IncidentStatus;
import com.example.eventanalysisplatform.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "app.kafka.listeners.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class IncidentEventConsumer {
    private final RedisService redisService;
    private final IncidentRepository incidentRepository;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentEventConsumer.class);
    private final KafkaTemplate<String, IncidentEvent> kafkaTemplate;
    private final AsyncIncidentEnrichmentService asyncIncidentEnrichmentService;

    public IncidentEventConsumer(RedisService redisService, IncidentRepository incidentRepository,
                                 KafkaTemplate<String, IncidentEvent> kafkaTemplate, AsyncIncidentEnrichmentService asyncIncidentEnrichmentService) {
        this.redisService = redisService;
        this.incidentRepository = incidentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.asyncIncidentEnrichmentService = asyncIncidentEnrichmentService;
    }

    @KafkaListener(
            topics = "incidents",
            groupId = "incident-group"
    )
    public void consume(IncidentEvent incidentEvent) {
        MDC.put("correlationId", incidentEvent.correlationId());
        try {
            redisService.saveStatus(
                    incidentEvent.incidentId(),
                    IncidentStatus.PROCESSING
            );

            incidentRepository.save(incidentEvent);

            redisService.saveStatus(
                    incidentEvent.incidentId(),
                    IncidentStatus.PROCESSED
            );

            asyncIncidentEnrichmentService.enrichAsync(incidentEvent)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn(
                                    "Async enrichment failed incidentId={} correlationId={}",
                                    incidentEvent.incidentId(),
                                    incidentEvent.correlationId(),
                                    exception
                            );
                        }
                    });

            log.info(
                    "Consumed incident {} correlationId={}",
                    incidentEvent.incidentId(),
                    incidentEvent.correlationId()
            );
        } catch (IncidentConflictException exception) {

            redisService.saveStatus(
                    incidentEvent.incidentId(),
                    IncidentStatus.CONFLICT
            );

            log.error(
                    "Incident conflict while consuming event: {} - {}",
                    incidentEvent.incidentId(),
                    exception.getMessage()
            );
            kafkaTemplate.send("incidents-dlt", incidentEvent);
        }
        finally {
            MDC.remove("correlationId");
        }
    }
}
