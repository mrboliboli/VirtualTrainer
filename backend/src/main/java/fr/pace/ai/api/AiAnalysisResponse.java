package fr.pace.ai.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.ai.domain.ActivityAnalysisResult;
import fr.pace.ai.persistence.AiCall;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiAnalysisResponse(UUID id, String statut, int version, String erreur,
                                 Resultat resultat, Instant dateCreation, Instant dateMiseAJour) {
    public static AiAnalysisResponse from(AiCall call, ObjectMapper mapper) {
        ActivityAnalysisResult result = null;
        if (call.getResponseJson() != null) {
            try { result = mapper.readValue(call.getResponseJson(), ActivityAnalysisResult.class); }
            catch (Exception ignored) { /* Une réponse persistée illisible n'est jamais exposée. */ }
        }
        return new AiAnalysisResponse(call.getId(), call.getStatus().name(), call.getAnalysisVersion(),
                call.getControlledError(), result == null ? null : Resultat.from(result),
                call.getCreatedAt(), call.getUpdatedAt());
    }

    public record Resultat(String resume, List<Interpretation> interpretations, List<String> pointsPositifs,
                           List<String> pointsVigilance, Recuperation recuperation, List<String> hypotheses,
                           List<String> donneesManquantes, String impactProchaineSeance, String avertissementSante) {
        static Resultat from(ActivityAnalysisResult value) {
            return new Resultat(value.summary(), value.interpretations().stream().map(Interpretation::from).toList(),
                    value.positivePoints(), value.vigilancePoints(),
                    new Recuperation(value.recovery().recommendation(), value.recovery().rationale()),
                    value.hypotheses(), value.missingData(), value.nextWorkoutImpact(), value.healthWarning());
        }
    }
    public record Interpretation(String titre, String texte, String confiance, List<String> faitsSources) {
        static Interpretation from(ActivityAnalysisResult.Interpretation value) {
            String confidence = switch (value.confidence()) { case ELEVE -> "ELEVEE"; case MOYEN -> "MOYENNE"; case FAIBLE -> "FAIBLE"; };
            return new Interpretation(value.title(), value.text(), confidence, value.sourceFactIds());
        }
    }
    public record Recuperation(String recommandation, String justification) { }
}
