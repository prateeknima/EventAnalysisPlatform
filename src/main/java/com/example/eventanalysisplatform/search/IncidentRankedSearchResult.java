package com.example.eventanalysisplatform.search;

import java.util.List;

public record IncidentRankedSearchResult(
        String incidentId,
        String source,
        String severity,
        String message,
        int priorityScore,
        String riskLevel,
        int affectedServiceCount,
        float searchScore,
        List<String> affectedServices
) {
}
