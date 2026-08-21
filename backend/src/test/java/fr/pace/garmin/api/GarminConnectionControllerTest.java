package fr.pace.garmin.api;

import fr.pace.common.ApiExceptionHandler;
import fr.pace.garmin.GarminActivityClient;
import fr.pace.garmin.GarminSession;
import fr.pace.garmin.GarminSessionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({GarminConnectionController.class, ApiExceptionHandler.class})
class GarminConnectionControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private GarminActivityClient client;

    @Test
    @DisplayName("Devrait renvoyer un défi quand Garmin demande le code MFA")
    void connect_shouldReturnMfaChallenge_whenMfaIsRequired() throws Exception {
        // GIVEN
        when(client.connect(anyString(), anyString(), anyString()))
                .thenReturn(new GarminSession(GarminSessionState.MFA_REQUIRED, "Code requis"));

        // WHEN / THEN
        mockMvc.perform(post("/api/v1/garmin/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifiant":"coureur@example.org","motDePasse":"secret-ephemere",
                                "consentementRisques":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("MFA_REQUIS"))
                .andExpect(jsonPath("$.mode").value("PERSONNEL"))
                .andExpect(jsonPath("$.defiMfaId").isNotEmpty());
    }

    @Test
    @DisplayName("Devrait refuser une connexion sans consentement aux risques")
    void connect_shouldRejectRequest_whenRiskConsentIsMissing() throws Exception {
        // WHEN / THEN
        mockMvc.perform(post("/api/v1/garmin/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifiant":"coureur@example.org","motDePasse":"secret-ephemere",
                                "consentementRisques":false}
                                """))
                .andExpect(status().isBadRequest());
    }
}
