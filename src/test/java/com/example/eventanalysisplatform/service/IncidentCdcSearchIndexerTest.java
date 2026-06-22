package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IncidentCdcSearchIndexerTest {

    private ObjectMapper objectMapper;
    private IncidentSearchService incidentSearchService;
    private IncidentEnrichmentService incidentEnrichmentService;
    private IncidentCdcSearchIndexer indexer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        incidentSearchService = mock(IncidentSearchService.class);

        incidentEnrichmentService = new IncidentEnrichmentService(
                new ServiceDependencyGraph(),
                new IncidentPriorityService()
        );

        indexer = new IncidentCdcSearchIndexer(
                objectMapper,
                incidentSearchService,
                incidentEnrichmentService
        );
    }

    @Test
    void consumeCdcIndexesEnrichedIncidentDocument() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "eventanalysis.public.incidents",
                0,
                0,
                null,
                """
                {
                  "payload": {
                    "op": "c",
                    "after": {
                      "incident_id": "INC-CDC-TEST-1",
                      "source": "payment",
                      "severity": "HIGH",
                      "message": "cdc test message"
                    }
                  }
                }
                """
        );

        indexer.consumeCdc(record);

        ArgumentCaptor<IncidentSearchDocument> captor =
                ArgumentCaptor.forClass(IncidentSearchDocument.class);

        verify(incidentSearchService).index(captor.capture());

        IncidentSearchDocument document = captor.getValue();

        assertThat(document.incidentId()).isEqualTo("INC-CDC-TEST-1");
        assertThat(document.source()).isEqualTo("payment");
        assertThat(document.severity()).isEqualTo("HIGH");
        assertThat(document.message()).isEqualTo("cdc test message");
        assertThat(document.priorityScore()).isEqualTo(81);
        assertThat(document.riskLevel()).isEqualTo("HIGH");
        assertThat(document.affectedServiceCount()).isEqualTo(11);
        assertThat(document.affectedServices())
                .contains("payment", "fraud", "checkout", "notification");
    }

    @Test
    void consumeCdcIgnoresDeleteEventsWithoutAfterPayload() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "eventanalysis.public.incidents",
                0,
                0,
                null,
                """
                {
                  "payload": {
                    "op": "d",
                    "after": null
                  }
                }
                """
        );

        indexer.consumeCdc(record);

        verifyNoInteractions(incidentSearchService);
    }
}