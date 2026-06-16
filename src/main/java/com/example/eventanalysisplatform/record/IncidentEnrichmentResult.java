package com.example.eventanalysisplatform.record;

import java.util.List;

public record IncidentEnrichmentResult(
        String incidentId,
        int priorityScore,
        String riskLevel,
        List<String> affectedServices,
        String recommendedAction
) {
}