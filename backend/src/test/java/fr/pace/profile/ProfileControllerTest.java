package fr.pace.profile;

import fr.pace.common.ApiExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ProfileController.class, ApiExceptionHandler.class})
class ProfileControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AthleteProfileService service;

    @Test
    @DisplayName("Devrait accepter le contrat français du profil")
    void save_shouldAcceptFrenchContract_whenPayloadIsValid() throws Exception {
        // GIVEN
        AthleteProfile profile = AthleteProfile.create(
                UUID.randomUUID(),
                new AthleteProfileData("Fabien", 1980, 180, null, null, null, null, null, null,
                        "Mardi|Dimanche", null, "Route|Sentier", null, null, null),
                Instant.parse("2026-08-21T10:00:00Z")
        );
        when(service.save(any(AthleteProfileData.class))).thenReturn(profile);

        // WHEN / THEN
        mockMvc.perform(put("/api/v1/profil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prenom":"Fabien","anneeNaissance":1980,"tailleCentimetres":180,
                                "joursDisponibles":["Mardi","Dimanche"],
                                "terrainsAccessibles":["Route","Sentier"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tailleCentimetres").value(180))
                .andExpect(jsonPath("$.joursDisponibles[1]").value("Dimanche"));
    }
}
