package fr.pace.garmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class HttpGarminActivityClientTest {
    @Test
    @DisplayName("Devrait traduire la réponse MFA en respectant le contrat interne")
    void connect_shouldMapMfaResponse_whenConnectorReturnsAccepted() {
        // GIVEN
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://connecteur/interne/v1/session/connexion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Cle-Service", "cle-service-test"))
                .andExpect(header("X-Correlation-Id", "correlation-test"))
                .andExpect(jsonPath("$.courriel").value("coureur@example.org"))
                .andExpect(jsonPath("$.mot_de_passe").value("secret"))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code":"MFA_REQUISE","type":"MFA_REQUISE","message":"Code requis",
                                "correlation_id":"correlation-test"}
                                """));
        HttpGarminActivityClient client = new HttpGarminActivityClient(builder, properties(), new ObjectMapper());

        // WHEN
        GarminSession result = client.connect("coureur@example.org", "secret", "correlation-test");

        // THEN
        assertThat(result.state()).isEqualTo(GarminSessionState.MFA_REQUIRED);
        server.verify();
    }

    @Test
    @DisplayName("Devrait arrêter définitivement quand une protection Garmin répond en erreur serveur")
    void sessionStatus_shouldMapPermanentError_whenProtectionReturnsServerError() {
        // GIVEN
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://connecteur/interne/v1/session"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"code":"PROTECTION_GARMIN","type":"DEFINITIVE",
                                "message":"Protection Garmin détectée","correlation_id":"correlation-test"}
                                """));
        HttpGarminActivityClient client = new HttpGarminActivityClient(builder, properties(), new ObjectMapper());

        // WHEN / THEN
        assertThatThrownBy(() -> client.sessionStatus("correlation-test"))
                .isInstanceOf(PermanentGarminConnectorException.class)
                .hasMessage("Protection Garmin détectée");
        server.verify();
    }

    private static GarminConnectorProperties properties() {
        return new GarminConnectorProperties(
                "http://connecteur",
                "cle-service-test",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );
    }
}
