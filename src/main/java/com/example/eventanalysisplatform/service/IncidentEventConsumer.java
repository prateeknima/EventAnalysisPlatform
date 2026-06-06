package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.exception.IncidentConflictException;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.record.IncidentStatus;
import com.example.eventanalysisplatform.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentEventConsumer {
    private final RedisService redisService;
    private final IncidentRepository incidentRepository;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentEventConsumer.class);
    private final KafkaTemplate<String, IncidentRequest> kafkaTemplate;

    public IncidentEventConsumer(RedisService redisService, IncidentRepository incidentRepository, KafkaTemplate<String, IncidentRequest> kafkaTemplate) {
        this.redisService = redisService;
        this.incidentRepository = incidentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "incidents",
            groupId = "incident-group"
    )
    public void consume(IncidentRequest incidentRequest) {
        try {
            redisService.saveStatus(
                    incidentRequest.incidentId(),
                    IncidentStatus.PROCESSING
            );

            incidentRepository.save(incidentRequest);

            redisService.saveStatus(
                    incidentRequest.incidentId(),
                    IncidentStatus.PROCESSED
            );
            log.info(
                    "Consumed incident {}",
                    incidentRequest.incidentId()
            );
        } catch (IncidentConflictException exception) {

            redisService.saveStatus(
                    incidentRequest.incidentId(),
                    IncidentStatus.CONFLICT
            );

            log.error(
                    "Incident conflict while consuming event: {} - {}",
                    incidentRequest.incidentId(),
                    exception.getMessage()
            );
            kafkaTemplate.send("incidents-dlt", incidentRequest);
        }
    }
}