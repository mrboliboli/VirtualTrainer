package fr.pace.ai.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityAnalysisResultTest {
    @Test
    void devraitAccepterUneInterpretationQuiReferenceUnFaitConnu() {
        ActivityAnalysisResult result = result(List.of("allure_moitie_2"));

        assertDoesNotThrow(() -> result.validateFactReferences(Set.of("allure_moitie_2")));
    }

    @Test
    void devraitRefuserUneInterpretationQuiReferenceUnFaitInconnu() {
        ActivityAnalysisResult result = result(List.of("donnee_inventee"));

        assertThrows(IllegalArgumentException.class,
                () -> result.validateFactReferences(Set.of("allure_moitie_2")));
    }

    @Test
    void devraitExigerUneConfianceEtAuMoinsUnFaitSource() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActivityAnalysisResult.Interpretation("Allure", "Stable", ConfidenceLevel.ELEVE, List.of()));
    }

    private ActivityAnalysisResult result(List<String> sourceIds) {
        return new ActivityAnalysisResult(
                "Sortie régulière",
                List.of(new ActivityAnalysisResult.Interpretation("Allure", "Allure stable", ConfidenceLevel.ELEVE, sourceIds)),
                List.of("Régularité"), List.of(),
                new ActivityAnalysisResult.RecoveryRecommendation("Récupération légère", "Charge modérée"),
                List.of(), List.of(), "Pas de conclusion sans séance planifiée", null);
    }
}
