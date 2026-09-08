package fr.pace.ai.domain;

import java.time.LocalDate;
import java.util.List;

/** Port réservé à la phase 7 : aucun appel n'est effectué en phase 6. */
public record WorkoutGenerationRequest(LocalDate targetDate, String goalSummary,
                                       String lastActivitySummary, List<String> constraints) {
    public WorkoutGenerationRequest { constraints = constraints == null ? List.of() : List.copyOf(constraints); }
}
