package com.example.eventanalysisplatform.record;

public record IncidentRequest(
        String incidentId,
        String source,
        String severity,
        String message
) {}
