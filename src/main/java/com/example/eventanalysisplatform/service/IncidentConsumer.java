package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class IncidentConsumer {
    private final RedisService redisService;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentConsumer.class);

    public IncidentConsumer(RedisService redisService) {
        this.redisService = redisService;
    }

    @KafkaListener(
            topics = "incidents",
            groupId = "incident-group"
    )
    public void consume(IncidentRequest incidentRequest) {

        redisService.save(
                "incident:" + incidentRequest.incidentId(),
                "processed"
        );
        log.info(
                "Consumed incident {}",
                incidentRequest.incidentId()
        );
    }
}