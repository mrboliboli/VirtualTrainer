package fr.pace.activity;

import java.time.Instant;
import java.util.Map;

public record ExternalActivityDetails(
        String sourceActivityId,
        Instant startedAt,
        String sport,
        Map<String, Object> availableMetrics
) {
}
