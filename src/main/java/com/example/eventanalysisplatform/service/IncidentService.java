package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final IncidentProducer producer;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentService.class);

    public IncidentService(
            IncidentProducer producer
    ) {
        this.producer = producer;
    }

    public void handle(IncidentRequest incidentRequest) {
        log.info("Received incident: {}", incidentRequest.incidentId());
        producer.publish(incidentRequest);
    }
}