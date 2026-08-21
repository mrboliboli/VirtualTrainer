package fr.pace.goal;

import java.util.UUID;

/** Indique qu'un objectif demandé n'existe pas. */
public class GoalNotFoundException extends RuntimeException {
    public GoalNotFoundException(UUID id) {
        super("L'objectif %s est introuvable.".formatted(id));
    }
}
