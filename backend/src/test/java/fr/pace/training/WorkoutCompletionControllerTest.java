package fr.pace.training;

import fr.pace.common.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({WorkoutCompletionController.class, ApiExceptionHandler.class})
class WorkoutCompletionControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private WorkoutCompletionService service;

    @Test
    void exposesLatestPlannedAndCompletedWorkoutComparison() throws Exception {
        UUID workoutId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        when(service.latestCompletion()).thenReturn(Optional.of(new WorkoutCompletionResponse(
                workoutId, "Endurance facile", LocalDate.parse("2026-09-08"), 45, "REALISEE",
                activityId, Instant.parse("2026-09-08T08:00:00Z"), "running", 10_000d, 3_600d,
                "DATE_SPORT_FENETRE_1J", Instant.parse("2026-09-08T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/seances/derniere-realisation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seanceId").value(workoutId.toString()))
                .andExpect(jsonPath("$.titre").value("Endurance facile"))
                .andExpect(jsonPath("$.dureePrevueMinutes").value(45))
                .andExpect(jsonPath("$.distanceMetres").value(10_000));
    }

    @Test
    void returnsNotFoundWhenNoWorkoutHasBeenCompleted() throws Exception {
        when(service.latestCompletion()).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/seances/derniere-realisation"))
                .andExpect(status().isNotFound());
    }
}
