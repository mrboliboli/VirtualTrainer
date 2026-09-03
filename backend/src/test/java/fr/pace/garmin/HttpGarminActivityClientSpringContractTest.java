package fr.pace.garmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@JsonTest
class HttpGarminActivityClientSpringContractTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Devrait sérialiser la connexion selon le contrat Python avec la configuration Spring")
    void connect_shouldSerializePythonContract_whenUsingSpringConfiguration() {
        // GIVEN
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("http://connecteur/interne/v1/session/connexion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"courriel":"personne@example.invalid","mot_de_passe":"secret-ephemere"}
                        """, true))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"etat":"CONNECTEE","message":"Session Garmin créée"}
                                """));
        GarminConnectorProperties properties = new GarminConnectorProperties(
                "http://connecteur",
                "cle-service-test",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );
        HttpGarminActivityClient client = new HttpGarminActivityClient(
                restClientBuilder, properties, objectMapper
        );

        // WHEN
        client.connect("personne@example.invalid", "secret-ephemere", "correlation-test");

        // THEN
        server.verify();
    }
}
