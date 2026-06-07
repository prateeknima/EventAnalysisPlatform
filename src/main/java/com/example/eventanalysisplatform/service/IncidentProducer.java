package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class IncidentProducer {

    private final KafkaTemplate<String, IncidentEvent> kafkaTemplate;
    private static final Logger log =
            LoggerFactory.getLogger(IncidentProducer.class);

    public IncidentProducer(
            KafkaTemplate<String, IncidentEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(IncidentEvent incidentEvent) {
        MDC.put("correlationId", incidentEvent.correlationId());
        try {

            log.info(
                    "Publishing incident: {}",
                    incidentEvent.incidentId()
            );

            // block until broker acknowledges message
            kafkaTemplate
                    .send("incidents", incidentEvent)
                    .get();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
        finally {
            MDC.remove("correlationId");
        }
    }
}