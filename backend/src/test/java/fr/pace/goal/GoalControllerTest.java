package fr.pace.goal;

import fr.pace.common.ApiExceptionHandler;
import fr.pace.profile.AthleteProfile;
import fr.pace.profile.AthleteProfileData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({GoalController.class, ApiExceptionHandler.class})
class GoalControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private GoalService service;

    @Test
    @DisplayName("Devrait créer l'objectif avec le contrat JSON français du formulaire")
    void create_shouldCreateGoal_whenFrontendPayloadUsesFrenchContract() throws Exception {
        // GIVEN
        UUID goalId = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-21T10:00:00Z");
        AthleteProfile profile = AthleteProfile.create(UUID.randomUUID(), profileData(), now);
        Goal goal = Goal.create(
                goalId,
                profile,
                new GoalData(
                        "Course des quais",
                        LocalDate.of(2026, 10, 10),
                        new BigDecimal("10"),
                        DistanceUnit.KILOMETER,
                        GoalType.TEN_KILOMETERS,
                        "https://example.test/course",
                        2,
                        null,
                        null,
                        null,
                        GoalStatus.PLANNED,
                        true
                ),
                now
        );
        when(service.create(any(GoalData.class))).thenReturn(goal);

        // WHEN / THEN
        mockMvc.perform(post("/api/v1/objectifs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Course des quais",
                                  "date": "2026-10-10",
                                  "distance": 10,
                                  "unite": "KM",
                                  "type": "10_KM",
                                  "url": "https://example.test/course",
                                  "priorite": 2,
                                  "statut": "PREVU",
                                  "principal": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(goalId.toString()))
                .andExpect(jsonPath("$.unite").value("KM"))
                .andExpect(jsonPath("$.type").value("10_KM"))
                .andExpect(jsonPath("$.statut").value("PREVU"));

        verify(service).create(org.mockito.ArgumentMatchers.argThat(data -> {
            assertThat(data.distanceUnit()).isEqualTo(DistanceUnit.KILOMETER);
            assertThat(data.type()).isEqualTo(GoalType.TEN_KILOMETERS);
            assertThat(data.status()).isEqualTo(GoalStatus.PLANNED);
            return true;
        }));
    }

    @Test
    @DisplayName("Devrait expliquer en français une valeur JSON inconnue")
    void create_shouldReturnReadableError_whenJsonValueIsUnknown() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post("/api/v1/objectifs")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Course des quais",
                                  "date": "2026-10-10",
                                  "distance": 10,
                                  "unite": "KM",
                                  "type": "TYPE_INCONNU",
                                  "priorite": 2,
                                  "statut": "PREVU",
                                  "principal": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Saisie illisible"))
                .andExpect(jsonPath("$.detail").value("Le format d'une information saisie n'est pas reconnu."));
    }

    @Test
    @DisplayName("Devrait archiver l'objectif avec le contrat HTTP convenu")
    void archive_shouldReturnNoContent_whenGoalExists() throws Exception {
        // GIVEN
        UUID goalId = UUID.randomUUID();

        // WHEN
        mockMvc.perform(post("/api/v1/objectifs/{id}/archivage", goalId))
                .andExpect(status().isNoContent());

        // THEN
        verify(service).archive(goalId);
    }

    private static AthleteProfileData profileData() {
        return new AthleteProfileData(
                "Fabien",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
