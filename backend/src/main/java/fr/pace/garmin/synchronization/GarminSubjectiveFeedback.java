package fr.pace.garmin.synchronization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.ExternalActivityDetails;

import java.util.Set;

/** Ressenti Garmin extrait sans déduire de sémantique absente du contrat source. */
record GarminSubjectiveFeedback(Double rpe, Double feelingScore, String source) {
    private static final Set<String> RPE_FIELDS = Set.of("perceivedExertion", "perceived_exertion");
    private static final Set<String> FEELING_FIELDS = Set.of("activityFeel", "activity_feel");

    static GarminSubjectiveFeedback from(ExternalActivityDetails details, ObjectMapper mapper) {
        JsonNode metrics = mapper.valueToTree(details.availableMetrics());
        Double rawRpe = findNumber(metrics, RPE_FIELDS);
        Double rawFeeling = findNumber(metrics, FEELING_FIELDS);
        Double rpe = normalizeRpe(rawRpe);
        Double feeling = inRange(rawFeeling, 0, 100) ? rawFeeling : null;
        return new GarminSubjectiveFeedback(rpe, feeling, rpe != null || feeling != null ? "GARMIN" : null);
    }

    private static Double findNumber(JsonNode node, Set<String> fields) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isObject()) {
            for (String field : fields) {
                JsonNode value = node.get(field);
                if (value != null && value.isNumber()) return value.doubleValue();
            }
            var children = node.elements();
            while (children.hasNext()) {
                Double value = findNumber(children.next(), fields);
                if (value != null) return value;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                Double value = findNumber(child, fields);
                if (value != null) return value;
            }
        }
        return null;
    }

    private static Double normalizeRpe(Double value) {
        if (!inRange(value, 0, 100)) return null;
        return value > 10 ? value / 10.0 : value;
    }

    private static boolean inRange(Double value, double minimum, double maximum) {
        return value != null && Double.isFinite(value) && value >= minimum && value <= maximum;
    }
}
