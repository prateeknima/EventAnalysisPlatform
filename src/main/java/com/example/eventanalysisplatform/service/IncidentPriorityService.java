package com.example.eventanalysisplatform.service;

import org.springframework.stereotype.Service;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Calculates a priority score inside the incident's severity band and maps the
 * score to a risk level using ordered thresholds.
 */
@Service
public class IncidentPriorityService {

    private final NavigableMap<Integer, String> riskLevels = new TreeMap<>();

    public IncidentPriorityService() {
        riskLevels.put(0, "LOW");
        riskLevels.put(40, "MEDIUM");
        riskLevels.put(70, "HIGH");
        riskLevels.put(90, "CRITICAL");
    }

    public int calculatePriorityScore(String severity, int affectedServiceCount) {
        SeverityBand severityBand = severityBand(severity);

        return Math.min(
                severityBand.baseScore() + affectedServiceCount,
                severityBand.maxScore()
        );
    }

    public String determineRiskLevel(int priorityScore) {
        return riskLevels.floorEntry(priorityScore).getValue();
    }

    private SeverityBand severityBand(String severity) {
        return switch (severity) {
            case "CRITICAL" -> new SeverityBand(90, 100);
            case "HIGH" -> new SeverityBand(70, 89);
            case "MEDIUM" -> new SeverityBand(40, 69);
            case "LOW" -> new SeverityBand(10, 39);
            default -> throw new IllegalArgumentException("Unsupported severity: " + severity);
        };
    }

    private record SeverityBand(
            int baseScore,
            int maxScore
    ) {
    }
}