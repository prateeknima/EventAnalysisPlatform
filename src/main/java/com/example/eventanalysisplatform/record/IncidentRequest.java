package com.example.eventanalysisplatform.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IncidentRequest(
        @NotBlank(message = "incidentId is required")
        String incidentId,

        @NotBlank(message = "source is required")
        String source,

        @Pattern(
                regexp = "LOW|MEDIUM|HIGH|CRITICAL",
                message = "severity must be one of LOW, MEDIUM, HIGH, CRITICAL"
        )
        String severity,

        @NotBlank(message = "message is required")
        String message
) {}
