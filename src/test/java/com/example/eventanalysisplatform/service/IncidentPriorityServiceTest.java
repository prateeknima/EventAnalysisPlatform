package com.example.eventanalysisplatform.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentPriorityServiceTest {

    private final IncidentPriorityService service = new IncidentPriorityService();

    @Test
    void calculatePriorityScoreKeepsHighIncidentInsideHighBand() {
        int score = service.calculatePriorityScore("HIGH", 100);

        assertThat(score).isEqualTo(89);
        assertThat(service.determineRiskLevel(score)).isEqualTo("HIGH");
    }

    @Test
    void calculatePriorityScoreKeepsMediumIncidentInsideMediumBand() {
        int score = service.calculatePriorityScore("MEDIUM", 100);

        assertThat(score).isEqualTo(69);
        assertThat(service.determineRiskLevel(score)).isEqualTo("MEDIUM");
    }

    @Test
    void calculatePriorityScoreUsesAffectedServiceCountWithinBand() {
        int lowerImpactScore = service.calculatePriorityScore("HIGH", 2);
        int higherImpactScore = service.calculatePriorityScore("HIGH", 8);

        assertThat(lowerImpactScore).isEqualTo(72);
        assertThat(higherImpactScore).isEqualTo(78);
    }

    @Test
    void calculatePriorityScoreCapsCriticalAtOneHundred() {
        int score = service.calculatePriorityScore("CRITICAL", 100);

        assertThat(score).isEqualTo(100);
        assertThat(service.determineRiskLevel(score)).isEqualTo("CRITICAL");
    }

    @Test
    void determineRiskLevelUsesNearestLowerThreshold() {
        assertThat(service.determineRiskLevel(10)).isEqualTo("LOW");
        assertThat(service.determineRiskLevel(45)).isEqualTo("MEDIUM");
        assertThat(service.determineRiskLevel(80)).isEqualTo("HIGH");
        assertThat(service.determineRiskLevel(95)).isEqualTo("CRITICAL");
    }

    @Test
    void calculatePriorityScoreRejectsUnsupportedSeverity() {
        assertThatThrownBy(() -> service.calculatePriorityScore("UNKNOWN", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported severity: UNKNOWN");
    }
}