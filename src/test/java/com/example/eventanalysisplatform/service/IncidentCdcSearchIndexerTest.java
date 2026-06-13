package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;

class IncidentCdcSearchIndexerTest {

    private IncidentSearchService incidentSearchService;
    private IncidentCdcSearchIndexer indexer;

    @BeforeEach
    void setUp() {
        incidentSearchService = mock(IncidentSearchService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        indexer = new IncidentCdcSearchIndexer(
                objectMapper,
                incidentSearchService
        );
    }

    @Test
    void consumeCdcIndexesAfterPayload() {
        String cdcEvent = """
                {
                  "payload": {
                    "before": null,
                    "after": {
                      "incident_id": "INC-CDC-TEST-1",
                      "source": "payment",
                      "severity": "HIGH",
                      "message": "cdc test message"
                    },
                    "op": "c"
                  }
                }
                """;

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "eventanalysis.public.incidents",
                0,
                0,
                null,
                cdcEvent
        );

        indexer.consumeCdc(record);

        verify(incidentSearchService).index(new IncidentSearchDocument(
                "INC-CDC-TEST-1",
                "payment",
                "HIGH",
                "cdc test message"
        ));
    }

    @Test
    void consumeCdcIgnoresEventWithoutAfterPayload() {
        String cdcEvent = """
                {
                  "payload": {
                    "before": {
                      "incident_id": "INC-CDC-TEST-1"
                    },
                    "after": null,
                    "op": "d"
                  }
                }
                """;

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "eventanalysis.public.incidents",
                0,
                0,
                null,
                cdcEvent
        );

        indexer.consumeCdc(record);

        verifyNoInteractions(incidentSearchService);
    }
}