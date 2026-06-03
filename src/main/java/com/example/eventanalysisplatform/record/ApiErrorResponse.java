package com.example.eventanalysisplatform.record;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String path,
    Map<String, String> validationErrors
) {}
