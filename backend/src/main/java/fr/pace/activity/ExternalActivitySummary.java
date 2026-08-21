package fr.pace.activity;

import java.time.Instant;

public record ExternalActivitySummary(
        String sourceActivityId,
        Instant startedAt,
        String sport,
        Long distanceMeters,
        Long durationSeconds
) {
}
