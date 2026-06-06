package com.example.eventanalysisplatform.record;


public record IncidentEvent(
        String incidentId,
        String source,
        String severity,
        String message,
        String correlationId
) {}