package fr.pace.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.ai.domain.ActivityAnalysisRequest;
import fr.pace.ai.domain.AiInvalidResponseException;
import fr.pace.ai.domain.ConfidenceLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import fr.pace.ai.domain.WorkoutGenerationRequest;
import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiProviderTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void rejectsUnknownFactReferencesEvenAfterStructuredOutput() throws Exception {
        var output = mapper.readTree("""
                {"summary":"Sortie régulière","interpretations":[{
                  "title":"Allure","text":"Allure stable","confidence":"ELEVE","sourceFactIds":["unknown"]
                }],"positivePoints":[],"vigilancePoints":[],
                "recovery":{"recommendation":"Récupération légère","rationale":"Charge modérée"},
                "hypotheses":[],"missingData":[],"nextWorkoutImpact":"À considérer","healthWarning":null}
                """);
        OpenAiResponsesClient fakeClient = new OpenAiResponsesClient(null, mapper, null, millis -> { }) {
            @Override Response execute(String instructions, com.fasterxml.jackson.databind.JsonNode input,
                                       String schemaName, com.fasterxml.jackson.databind.JsonNode schema, String correlationId) {
                return new Response("resp", output, 1, 1);
            }
        };
        OpenAiProvider provider = new OpenAiProvider(mapper, fakeClient);

        assertThatThrownBy(() -> provider.analyzeActivity(request()))
                .isInstanceOf(AiInvalidResponseException.class)
                .hasMessageContaining("contrat");
    }

    @Test
    void generatesAndValidatesStructuredWorkout() throws Exception {
        var output = mapper.readTree("""
                {"title":"Footing facile","plannedDate":"2026-09-09","objective":"Récupérer",
                "durationMinutes":35,"intensity":"FACILE","steps":["10 min faciles","20 min régulières","5 min retour au calme"],
                "rationale":"Après la sortie récente","confidence":"MOYEN"}
                """);
        OpenAiResponsesClient fakeClient = new OpenAiResponsesClient(null, mapper, null, millis -> { }) {
            @Override Response execute(String instructions, com.fasterxml.jackson.databind.JsonNode input,
                                       String schemaName, com.fasterxml.jackson.databind.JsonNode schema, String correlationId) {
                return new Response("resp", output, 1, 1);
            }
        };
        var result = new OpenAiProvider(mapper, fakeClient).generateWorkout(new WorkoutGenerationRequest(
                LocalDate.parse("2026-09-09"), "Course de 20 km", List.of(), List.of(), List.of(), 60));
        assertThat(result.durationMinutes()).isEqualTo(35);
    }

    private ActivityAnalysisRequest request() {
        return new ActivityAnalysisRequest(
                new ActivityAnalysisRequest.AthleteContext(40, null, 3, List.of("route")), null,
                new ActivityAnalysisRequest.ActivityContext(Instant.parse("2026-09-01T08:00:00Z"),
                        "running", 10_000d, 3_600d, 3_500d, 50, Map.of()),
                null, List.of(), List.of(),
                List.of(new ActivityAnalysisRequest.AnalysisFact("pace", "Allure", "6:00/km", ConfidenceLevel.ELEVE)),
                List.of(), List.of());
    }
}
