package fr.pace.goal;

import fr.pace.common.ApiExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({GoalController.class, ApiExceptionHandler.class})
class GoalControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private GoalService service;

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
}
