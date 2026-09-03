package fr.pace.garmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class HttpGarminActivityClientTest {
    @Test
    @DisplayName("Devrait imposer HTTP 1.1 pour communiquer avec le connecteur Garmin")
    void configuredHttpClient_shouldUseHttp11_whenCallingGarminConnector() {
        // WHEN
        HttpClient httpClient = HttpGarminActivityClient.configuredHttpClient(properties());

        // THEN
        assertThat(httpClient.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
        assertThat(httpClient.connectTimeout()).contains(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Devrait accepter le format de date réel de Garmin et ignorer un résumé isolé invalide")
    void pullActivities_shouldKeepValidActivities_whenGarminUsesSpaceSeparatedDate() {
        // GIVEN
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "http://connecteur/interne/v1/activites?limite=20&debut=2026-08-18&fin=2026-08-21"
                ))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"activites":[
                                  {"identifiant":"123","donnees":{"startTimeGMT":"2026-08-20 06:30:00",
                                  "activityType":{"typeKey":"running"},"distance":10000,"duration":3600}},
                                  {"identifiant":"invalide","donnees":{"startTimeGMT":"date-invalide"}}
                                ],"limite":20}
                                """));
        HttpGarminActivityClient client = new HttpGarminActivityClient(builder, properties(), new ObjectMapper());

        // WHEN
        var result = client.pullActivities(
                java.time.LocalDate.parse("2026-08-18"),
                java.time.LocalDate.parse("2026-08-21"),
                20,
                "correlation-test"
        );

        // THEN
        assertThat(result).singleElement().satisfies(activity -> {
            assertThat(activity.sourceActivityId()).isEqualTo("123");
            assertThat(activity.startedAt()).isEqualTo(Instant.parse("2026-08-20T06:30:00Z"));
            assertThat(activity.sport()).isEqualTo("running");
        });
        server.verify();
    }

    @Test
    @DisplayName("Devrait conserver la date candidate quand le détail Garmin place son résumé dans un sous-objet")
    void getActivity_shouldUseCandidateMetadata_whenGarminDetailUsesSummaryDto() {
        // GIVEN
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://connecteur/interne/v1/activites/123"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"identifiant":"123","donnees":{"activityId":123,
                                "activityTypeDTO":{"typeKey":"running"},
                                "summaryDTO":{"startTimeGMT":"2026-08-20 06:30:00"}}}
                                """));
        HttpGarminActivityClient client = new HttpGarminActivityClient(builder, properties(), new ObjectMapper());
        Instant candidateDate = Instant.parse("2026-08-20T06:30:00Z");

        // WHEN
        var result = client.getActivity("123", candidateDate, "running");

        // THEN
        assertThat(result.sourceActivityId()).isEqualTo("123");
        assertThat(result.startedAt()).isEqualTo(candidateDate);
        assertThat(result.sport()).isEqualTo("running");
        assertThat(result.availableMetrics()).containsKey("summaryDTO");
        server.verify();
    }

    @Test
    @DisplayName("Devrait refuser un FIT qui dépasse dix mégaoctets")
    void downloadFit_shouldRejectResponse_whenPayloadIsTooLarge() {
        // GIVEN
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://connecteur/interne/v1/activites/123/fit"))
                .andRespond(withStatus(HttpStatus.OK).body(new byte[10 * 1024 * 1024 + 1]));
        HttpGarminActivityClient client = new HttpGarminActivityClient(builder, properties(), new ObjectMapper());

        // WHEN / THEN
        assertThatThrownBy(() -> client.downloadFit("123"))
                .isInstanceOf(PermanentGarminConnectorException.class)
                .hasMessageContaining("taille autorisée");
        server.verify();
    }

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
