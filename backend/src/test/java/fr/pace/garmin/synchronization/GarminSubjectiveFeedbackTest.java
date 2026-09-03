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
