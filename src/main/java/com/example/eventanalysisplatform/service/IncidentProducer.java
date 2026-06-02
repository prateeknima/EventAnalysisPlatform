package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentProducer {

    private final KafkaTemplate<String, IncidentRequest> kafkaTemplate;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentProducer.class);

    public IncidentProducer(
            KafkaTemplate<String, IncidentRequest> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(IncidentRequest incidentRequest) {

        try {

            log.info(
                    "Publishing incident: {}",
                    incidentRequest.incidentId()
            );

            // block until broker acknowledges message
            kafkaTemplate
                    .send("incidents", incidentRequest)
                    .get();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}