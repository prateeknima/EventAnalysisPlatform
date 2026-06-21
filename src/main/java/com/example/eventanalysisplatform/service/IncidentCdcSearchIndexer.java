package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(
        name = "app.kafka.listeners.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class IncidentCdcSearchIndexer {

    private static final Logger log =
            LoggerFactory.getLogger(IncidentCdcSearchIndexer.class);

    private final ObjectMapper objectMapper;
    private final IncidentSearchService incidentSearchService;

    public IncidentCdcSearchIndexer(ObjectMapper objectMapper, IncidentSearchService incidentSearchService) {
        this.objectMapper = objectMapper;
        this.incidentSearchService = incidentSearchService;
    }

    @KafkaListener(
            topics = "eventanalysis.public.incidents",
            groupId = "cdc-debug-group",
            containerFactory = "cdcKafkaListenerContainerFactory"
    )
    public void consumeCdc(ConsumerRecord<String, String> record) {
        try {


            JsonNode root = objectMapper.readTree(record.value());
            JsonNode payload = root.get("payload");
            if (payload == null || payload.isNull()) {
                log.warn("CDC event missing payload: {}", record.value());
                return;
            }

            JsonNode after = payload.get("after");
            if (after == null || after.isNull()) {
                log.info("CDC event has no after payload. op={}", payload.get("op"));
                return;
            }

            String incidentId = after.get("incident_id").asString();
            String source = after.get("source").asString();
            String severity = after.get("severity").asString();
            String message = after.get("message").asString();

            log.info(
                    "Extracted CDC incident for search indexing: incidentId={}, source={}, severity={}, message={}",
                    incidentId,
                    source,
                    severity,
                    message
            );

            IncidentSearchDocument document = new IncidentSearchDocument(
                    incidentId,
                    source,
                    severity,
                    message
            );

            incidentSearchService.index(document);

            log.info("Indexed incident into Elasticsearch: {}", incidentId);

        } catch (JacksonException e) {
            log.error("Failed to parse CDC event: {}", record.value(), e);
        }
    }
}
