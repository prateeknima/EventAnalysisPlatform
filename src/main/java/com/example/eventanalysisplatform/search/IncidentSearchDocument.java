package com.example.eventanalysisplatform.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "incidents")
public record IncidentSearchDocument (
    @Id String incidentId,
    String source,
    String severity,
    String message
) {}
