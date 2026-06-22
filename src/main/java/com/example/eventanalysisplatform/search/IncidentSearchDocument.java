package com.example.eventanalysisplatform.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

@Document(indexName = "incidents")
public record IncidentSearchDocument(
        @Id String incidentId,
        String source,
        String severity,
        String message,
        int priorityScore,
        String riskLevel,
        int affectedServiceCount,
        List<String> affectedServices
) {
    public IncidentSearchDocument(
            String incidentId,
            String source,
            String severity,
            String message
    ) {
        this(
                incidentId,
                source,
                severity,
                message,
                0,
                "UNKNOWN",
                0,
                List.of()
        );
    }
}