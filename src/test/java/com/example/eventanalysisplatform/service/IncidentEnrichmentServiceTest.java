package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentEnrichmentResult;
import com.example.eventanalysisplatform.record.IncidentEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentEnrichmentServiceTest {

    private final IncidentEnrichmentService service =
            new IncidentEnrichmentService(
                    new ServiceDependencyGraph(),
                    new IncidentPriorityService()
            );

    @Test
    void enrichBuildsIncidentEnrichmentResult() {
        IncidentEvent event = new IncidentEvent(
                "INC-ENRICH-1",
                "payment",
                "HIGH",
                "payment timeout",
                "corr-1"
        );

        IncidentEnrichmentResult result = service.enrich(event);

        assertThat(result.incidentId()).isEqualTo("INC-ENRICH-1");
        assertThat(result.priorityScore()).isEqualTo(81);
        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.affectedServices())
                .contains(
                        "payment",
                        "fraud",
                        "checkout",
                        "notification",
                        "risk-engine",
                        "inventory",
                        "shipping",
                        "email",
                        "sms",
                        "warehouse",
                        "carrier"
                )
                .doesNotHaveDuplicates();
        assertThat(result.recommendedAction())
                .isEqualTo("Escalate to service owner and monitor downstream impact");
    }

    @Test
    void enrichHandlesUnknownSource() {
        IncidentEvent event = new IncidentEvent(
                "INC-ENRICH-2",
                "unknown-service",
                "MEDIUM",
                "unknown issue",
                "corr-2"
        );

        IncidentEnrichmentResult result = service.enrich(event);

        assertThat(result.affectedServices())
                .containsExactly("unknown-service");
        assertThat(result.priorityScore()).isEqualTo(41);
        assertThat(result.riskLevel()).isEqualTo("MEDIUM");
        assertThat(result.recommendedAction())
                .isEqualTo("Create incident ticket and monitor error trends");
    }
}