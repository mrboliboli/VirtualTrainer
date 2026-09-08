package fr.pace.training;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkoutCompletionResponse(UUID seanceId, LocalDate datePrevue, String statut, UUID activiteId,
                                        Instant dateActivite, String sport, Double distanceMetres,
                                        Double dureeSecondes, String methodeRapprochement, Instant rapprocheLe) { }
