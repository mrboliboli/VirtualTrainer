package fr.pace.ai.domain;

import java.util.List;

/** Résultat réservé à la phase 7. */
public record WorkoutGenerationResult(String title, String objective, List<String> steps,
                                      String rationale, ConfidenceLevel confidence) {
    public WorkoutGenerationResult { steps = steps == null ? List.of() : List.copyOf(steps); }
}
