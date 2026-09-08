package fr.pace.ai.domain;

import java.time.LocalDate;
import java.util.List;

public record WorkoutGenerationResult(String title, LocalDate plannedDate, String objective,
                                      Integer durationMinutes, String intensity, List<String> steps,
                                      String rationale, ConfidenceLevel confidence) {
    public WorkoutGenerationResult {
        if (title == null || title.isBlank() || plannedDate == null || objective == null || objective.isBlank()
                || durationMinutes == null || durationMinutes < 1 || intensity == null || intensity.isBlank()
                || rationale == null || rationale.isBlank() || confidence == null)
            throw new IllegalArgumentException("La séance générée est incomplète");
        steps = steps == null ? List.of() : List.copyOf(steps);
        if (steps.isEmpty()) throw new IllegalArgumentException("La séance doit contenir au moins une étape");
    }
}
