package fr.pace.activity;

import java.time.Instant;
import java.util.UUID;

public record SortieResponse(
        UUID id,
        Instant dateHeure,
        String sport,
        Long distanceMetres,
        Long dureeSecondes,
        Integer frequenceCardiaqueMoyenne,
        String typeEntrainement,
        String source
) { }
