package fr.pace.ai.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityAnalysisRequestTest {
    @Test
    void devraitFaireUneCopieDesAgregats() {
        List<String> missing = new ArrayList<>(List.of("puissance"));
        ActivityAnalysisRequest request = request(missing);
        missing.add("cadence");

        assertEquals(List.of("puissance"), request.missingData());
        assertThrows(UnsupportedOperationException.class, () -> request.missingData().add("FC"));
    }

    @Test
    void devraitRefuserUneChargeAvecTropDeTours() {
        List<ActivityAnalysisRequest.LapAggregate> laps = java.util.stream.IntStream.range(0, 101)
                .mapToObj(i -> new ActivityAnalysisRequest.LapAggregate(i, 1d, 1d, null, null, null)).toList();

        assertThrows(IllegalArgumentException.class, () -> new ActivityAnalysisRequest(
                new ActivityAnalysisRequest.AthleteContext(null, null, null, List.of()), null,
                new ActivityAnalysisRequest.ActivityContext(Instant.now(), "RUNNING", 1d, 1d, 1d, 0, Map.of()),
                null, laps, List.of(), List.of(), List.of(), List.of()));
    }

    private ActivityAnalysisRequest request(List<String> missing) {
        return new ActivityAnalysisRequest(
                new ActivityAnalysisRequest.AthleteContext(40, null, 3, List.of("ROUTE")), null,
                new ActivityAnalysisRequest.ActivityContext(Instant.now(), "RUNNING", 10_000d, 3_600d, 3_500d, 50, Map.of("allure", 360d)),
                new ActivityAnalysisRequest.SubjectiveFeedback(5d, 75d, "GARMIN"),
                List.of(), List.of(),
                List.of(new ActivityAnalysisRequest.AnalysisFact("allure", "Allure", "6 min/km", ConfidenceLevel.ELEVE)),
                missing, List.of());
    }
}
