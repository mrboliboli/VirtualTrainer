package fr.pace.garmin.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.pace.garmin.synchronization.ActivitySynchronization;
import fr.pace.garmin.synchronization.SynchronizationCandidate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SynchronizationResponse(
        UUID id,
        String statut,
        String messageUtilisateur,
        List<CandidateResponse> candidates
) {
    static SynchronizationResponse from(ActivitySynchronization synchronization) {
        String status = switch (synchronization.getStatus()) {
            case IN_PROGRESS -> "EN_COURS";
            case COMPLETED -> "TERMINEE";
            case TEMPORARY_ERROR -> "ERREUR_TEMPORAIRE";
            case PERMANENT_ERROR -> "ERREUR_DEFINITIVE";
        };
        List<CandidateResponse> candidates = synchronization.getCandidates()
                .stream()
                .map(CandidateResponse::from)
                .toList();
        return new SynchronizationResponse(
                synchronization.getId(),
                status,
                synchronization.getErrorMessage(),
                candidates
        );
    }

    public record CandidateResponse(
            String idExterne,
            Instant dateHeure,
            String sport,
            Long distanceMetres,
            Long dureeSecondes,
            String confiance
    ) {
        static CandidateResponse from(SynchronizationCandidate candidate) {
            return new CandidateResponse(
                    candidate.getExternalId(), candidate.getStartedAt(), candidate.getSport(),
                    candidate.getDistanceMeters(), candidate.getDurationSeconds(),
                    switch (candidate.getConfidence()) {
                        case HIGH -> "ELEVEE";
                        case MEDIUM -> "MOYENNE";
                        case LOW -> "FAIBLE";
                    }
            );
        }
    }
}
