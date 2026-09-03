package fr.pace.activity;

import java.time.Instant;
import java.util.UUID;

public record SortieResponse(
        UUID id,
        Instant dateHeure,
        String sport,
        Long distanceMetres,
        Long dureeSecondes,
        String source
) { }
