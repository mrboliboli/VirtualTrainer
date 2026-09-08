package fr.pace.ai.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Données agrégées autorisées à sortir de Pace pour analyser une activité. */
public record ActivityAnalysisRequest(
        AthleteContext athlete,
        GoalContext primaryGoal,
        ActivityContext activity,
        SubjectiveFeedback feedback,
        List<LapAggregate> laps,
        List<ZoneAggregate> zones,
        List<AnalysisFact> facts,
        List<String> missingData,
        List<String> localWarnings
) {
    public ActivityAnalysisRequest {
        if (athlete == null || activity == null) throw new IllegalArgumentException("Le contexte athlète et l'activité sont obligatoires");
        laps = copy(laps); zones = copy(zones); facts = copy(facts);
        missingData = copy(missingData); localWarnings = copy(localWarnings);
        if (laps.size() > 100 || zones.size() > 50 || facts.size() > 100) {
            throw new IllegalArgumentException("La requête d'analyse dépasse les agrégats autorisés");
        }
    }

    private static <T> List<T> copy(List<T> value) { return value == null ? List.of() : List.copyOf(value); }

    public record AthleteContext(Integer age, String sex, Integer trainingDaysPerWeek, List<String> availableTerrains) {
        public AthleteContext { availableTerrains = availableTerrains == null ? List.of() : List.copyOf(availableTerrains); }
    }
    public record GoalContext(String type, Instant targetDate, Double targetDistanceMeters, Integer targetDurationSeconds) { }
    public record ActivityContext(Instant startedAt, String sport, Double distanceMeters, Double elapsedSeconds,
                                  Double activeSeconds, Integer ascentMeters, Map<String, Double> metrics) {
        public ActivityContext { metrics = metrics == null ? Map.of() : Map.copyOf(metrics); }
    }
    public record SubjectiveFeedback(Double rpe, Double feelingScore, String source) { }
    public record LapAggregate(int index, Double distanceMeters, Double activeSeconds,
                               Double averageHeartRate, Double averagePower, Double ascentMeters) { }
    public record ZoneAggregate(String type, int index, Double percentage) { }
    public record AnalysisFact(String id, String label, String value, ConfidenceLevel confidence) {
        public AnalysisFact {
            if (id == null || id.isBlank() || label == null || label.isBlank() || value == null || value.isBlank()) {
                throw new IllegalArgumentException("Un fait doit posséder un identifiant, un libellé et une valeur");
            }
        }
    }
}
