package com.example.eventanalysisplatform.search;

import static java.lang.Math.clamp;

/**
 * Compact immutable ranking value used while scoring Elasticsearch candidates.
 * Packing the score into one primitive keeps the hot ranking path allocation-friendly.
 */
public record IncidentRankScore(
        int sortKey
) {
    private static final int MAX_PRIORITY_SCORE = 100;
    private static final int MAX_AFFECTED_SERVICE_COUNT = 255;
    private static final int MAX_SEARCH_SCORE_BASIS_POINTS = 65_535;

    public static IncidentRankScore from(
            int priorityScore,
            int affectedServiceCount,
            float searchScore
    ) {
        int safePriorityScore = clamp(priorityScore, 0, MAX_PRIORITY_SCORE);
        int safeAffectedServiceCount =
                clamp(affectedServiceCount, 0, MAX_AFFECTED_SERVICE_COUNT);
        int searchScoreBasisPoints =
                clamp(Math.round(searchScore * 100), 0, MAX_SEARCH_SCORE_BASIS_POINTS);

        int sortKey =
                (safePriorityScore << 24)
                        | (safeAffectedServiceCount << 16)
                        | searchScoreBasisPoints;

        return new IncidentRankScore(sortKey);
    }

    public int priorityScore() {
        return (sortKey >>> 24) & 0x7F;
    }

    public int affectedServiceCount() {
        return (sortKey >>> 16) & 0xFF;
    }

    public float searchScore() {
        return (sortKey & 0xFFFF) / 100.0f;
    }
}
