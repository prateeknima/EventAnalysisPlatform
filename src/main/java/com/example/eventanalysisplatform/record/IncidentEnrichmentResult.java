package com.example.eventanalysisplatform.record;

import java.util.List;

/**
 * Enriched operational context derived from an incident event.
 */
public record IncidentEnrichmentResult(
        String incidentId,
        int priorityScore,
        String riskLevel,
        List<String> affectedServices,
        String recommendedAction
) {
}