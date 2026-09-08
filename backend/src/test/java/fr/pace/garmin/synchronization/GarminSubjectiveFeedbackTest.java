package fr.pace.garmin.synchronization;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.ExternalActivityDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GarminSubjectiveFeedbackTest {
    @Test
    @DisplayName("Devrait extraire le ressenti Garmin imbriqué et normaliser le RPE sur dix")
    void from_shouldExtractNestedGarminFeedback() {
        ExternalActivityDetails details = new ExternalActivityDetails(
                "123", Instant.EPOCH, "running",
                Map.of("summaryDTO", Map.of("perceivedExertion", 70, "activityFeel", 75))
        );

        GarminSubjectiveFeedback feedback = GarminSubjectiveFeedback.from(details, new ObjectMapper());

        assertThat(feedback.rpe()).isEqualTo(7);
        assertThat(feedback.feelingScore()).isEqualTo(75);
        assertThat(feedback.source()).isEqualTo("GARMIN");
    }

    @Test
    @DisplayName("Devrait reconnaître les vrais champs directWorkout renvoyés par Garmin")
    void from_shouldExtractDirectWorkoutFeedback() {
        ExternalActivityDetails details = new ExternalActivityDetails(
                "456", Instant.EPOCH, "running",
                Map.of("summaryDTO", Map.of("directWorkoutRpe", 50, "directWorkoutFeel", 25))
        );

        GarminSubjectiveFeedback feedback = GarminSubjectiveFeedback.from(details, new ObjectMapper());

        assertThat(feedback.rpe()).isEqualTo(5);
        assertThat(feedback.feelingScore()).isEqualTo(25);
        assertThat(feedback.source()).isEqualTo("GARMIN");
    }

    @Test
    @DisplayName("Devrait interpréter la valeur Garmin 10 comme un effort de 1 sur 10")
    void from_shouldNormalizeLowestGarminRpe() {
        ExternalActivityDetails details = new ExternalActivityDetails(
                "789", Instant.EPOCH, "running",
                Map.of("summaryDTO", Map.of("directWorkoutRpe", 10))
        );

        GarminSubjectiveFeedback feedback = GarminSubjectiveFeedback.from(details, new ObjectMapper());

        assertThat(feedback.rpe()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ne devrait pas inventer un ressenti lorsque Garmin ne le fournit pas")
    void from_shouldRemainEmpty_whenFeedbackIsMissing() {
        ExternalActivityDetails details = new ExternalActivityDetails(
                "123", Instant.EPOCH, "running", Map.of("summaryDTO", Map.of())
        );

        GarminSubjectiveFeedback feedback = GarminSubjectiveFeedback.from(details, new ObjectMapper());

        assertThat(feedback.rpe()).isNull();
        assertThat(feedback.feelingScore()).isNull();
        assertThat(feedback.source()).isNull();
    }
}
