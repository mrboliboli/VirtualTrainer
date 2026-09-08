package fr.pace.ai.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.ai.domain.ActivityAnalysisRequest;
import fr.pace.ai.domain.AiInvalidResponseException;
import fr.pace.ai.domain.ConfidenceLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
