package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.service.IncidentProducer;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final IncidentProducer producer;

    public IncidentService(
            IncidentProducer producer
    ) {
        this.producer = producer;
    }

    public void handle(IncidentRequest incidentRequest) {

        producer.publish(incidentRequest);
    }
}