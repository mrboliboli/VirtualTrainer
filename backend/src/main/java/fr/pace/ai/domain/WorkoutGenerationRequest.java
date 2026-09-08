package fr.pace.ai.domain;

import java.time.LocalDate;
import java.util.List;

public record WorkoutGenerationRequest(LocalDate targetDate, String goalSummary,
                                       List<String> recentActivitySummaries, List<String> constraints,
                                       List<String> availableDays, Integer maximumDurationMinutes) {
    public WorkoutGenerationRequest {
        if (targetDate == null || goalSummary == null || goalSummary.isBlank())
            throw new IllegalArgumentException("La date et l'objectif sont obligatoires");
        recentActivitySummaries = recentActivitySummaries == null ? List.of() : List.copyOf(recentActivitySummaries);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        availableDays = availableDays == null ? List.of() : List.copyOf(availableDays);
        if (maximumDurationMinutes != null && maximumDurationMinutes < 1)
            throw new IllegalArgumentException("La durée maximale doit être positive");
    }
}
