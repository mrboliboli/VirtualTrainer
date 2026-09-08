package fr.pace.ai.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NextWorkoutResponse(UUID id, String titre, LocalDate datePrevue, String type,
                                  Integer dureeMinutes, Double distanceKilometres, String intensite,
                                  String explicationCoach, List<String> etapes, String confiance,
                                  String statut, Integer version, UUID versionPrecedenteId) { }
