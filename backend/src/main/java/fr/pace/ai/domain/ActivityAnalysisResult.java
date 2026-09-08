package fr.pace.ai.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ActivityAnalysisResult(
        String summary,
        List<Interpretation> interpretations,
        List<String> positivePoints,
        List<String> vigilancePoints,
        RecoveryRecommendation recovery,
        List<String> hypotheses,
        List<String> missingData,
        String nextWorkoutImpact,
        String healthWarning
) {
    public ActivityAnalysisResult {
        requireText(summary, "résumé", 1_500);
        interpretations = copy(interpretations); positivePoints = copy(positivePoints);
        vigilancePoints = copy(vigilancePoints); hypotheses = copy(hypotheses); missingData = copy(missingData);
        requireText(nextWorkoutImpact, "impact sur la prochaine séance", 1_500);
        if (recovery == null) throw new IllegalArgumentException("La récupération est obligatoire");
        checkTexts(positivePoints); checkTexts(vigilancePoints); checkTexts(hypotheses); checkTexts(missingData);
        if (healthWarning != null && healthWarning.length() > 1_500) throw new IllegalArgumentException("Avertissement santé trop long");
    }

    public void validateFactReferences(Set<String> knownFactIds) {
        Set<String> allowed = knownFactIds == null ? Set.of() : new HashSet<>(knownFactIds);
        interpretations.stream().flatMap(value -> value.sourceFactIds().stream())
                .filter(id -> !allowed.contains(id)).findFirst()
                .ifPresent(id -> { throw new IllegalArgumentException("Référence de fait inconnue : " + id); });
    }

    private static void checkTexts(List<String> values) { values.forEach(value -> requireText(value, "élément", 1_500)); }
    private static <T> List<T> copy(List<T> value) { return value == null ? List.of() : List.copyOf(value); }
    private static void requireText(String value, String name, int max) {
        if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException(name + " invalide");
    }

    public record Interpretation(String title, String text, ConfidenceLevel confidence, List<String> sourceFactIds) {
        public Interpretation {
            requireText(title, "titre", 200); requireText(text, "interprétation", 1_500);
            if (confidence == null) throw new IllegalArgumentException("La confiance est obligatoire");
            sourceFactIds = sourceFactIds == null ? List.of() : List.copyOf(sourceFactIds);
            if (sourceFactIds.isEmpty()) throw new IllegalArgumentException("Une interprétation doit citer au moins un fait");
        }
    }
    public record RecoveryRecommendation(String recommendation, String rationale) {
        public RecoveryRecommendation { requireText(recommendation, "récupération", 1_000); requireText(rationale, "justification", 1_000); }
    }
}
