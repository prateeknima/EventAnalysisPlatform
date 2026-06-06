package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.record.IncidentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final IncidentProducer producer;
    private final RedisService redisService;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentService.class);

    public IncidentService(
            IncidentProducer producer,
            RedisService redisService
    ) {
        this.producer = producer;
        this.redisService = redisService;
    }

    public void handle(IncidentRequest incidentRequest) {
        log.info("Received incident: {}", incidentRequest.incidentId());
        redisService.saveStatus(
                incidentRequest.incidentId(),
                IncidentStatus.RECEIVED
        );
        producer.publish(incidentRequest);
    }
}