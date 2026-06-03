package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class IncidentDltConsumer {
    private static final Logger log =
            LoggerFactory.getLogger(IncidentConsumer.class);

    @KafkaListener(
            topics = "incidents-dlt",
            groupId = "incident-dlt-debug-group"
    )
    public void consumeDlt(IncidentRequest incidentRequest) {
        log.warn(
                "Received incident from DLT: {}",
                incidentRequest
        );
    }

}
