package fr.pace.garmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.ActivitySourceStatus;
import fr.pace.activity.ExternalActivityDetails;
import fr.pace.activity.ExternalActivitySummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class HttpGarminActivityClient implements GarminActivityClient {
    private static final String SERVICE_KEY_HEADER = "X-Cle-Service";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public HttpGarminActivityClient(GarminConnectorProperties properties, ObjectMapper objectMapper) {
        this(configuredBuilder(properties), properties, objectMapper);
    }

    HttpGarminActivityClient(
            RestClient.Builder builder,
            GarminConnectorProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(SERVICE_KEY_HEADER, properties.token())
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    ConnectorError error = objectMapper.readValue(response.getBody(), ConnectorError.class);
                    throw mapError(response.getStatusCode(), error);
                })
                .build();
        this.objectMapper = objectMapper;
    }

    private static RestClient.Builder configuredBuilder(GarminConnectorProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectionTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Override
    public GarminSession connect(String email, String password, String correlationId) {
        return call(() -> restClient.post()
                .uri("/interne/v1/session/connexion")
                .header(CORRELATION_HEADER, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ConnectionRequest(email, password))
                .exchange((request, response) -> {
                    if (response.getStatusCode().value() == 202) {
                        ConnectorError error = objectMapper.readValue(response.getBody(), ConnectorError.class);
                        return new GarminSession(GarminSessionState.MFA_REQUIRED, error.message());
                    }
                    if (response.getStatusCode().isError()) {
                        ConnectorError error = objectMapper.readValue(response.getBody(), ConnectorError.class);
                        throw mapError(response.getStatusCode(), error);
                    }
                    return toSession(objectMapper.readValue(response.getBody(), SessionResponse.class));
                }));
    }

    @Override
    public GarminSession completeMfa(String code, String correlationId) {
        SessionResponse response = call(() -> restClient.post()
                .uri("/interne/v1/session/mfa")
                .header(CORRELATION_HEADER, correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MfaRequest(code))
                .retrieve()
                .body(SessionResponse.class));
        return toSession(response);
    }

    @Override
    public GarminSession sessionStatus(String correlationId) {
        return toSession(call(() -> restClient.get()
                .uri("/interne/v1/session")
                .header(CORRELATION_HEADER, correlationId)
                .retrieve()
                .body(SessionResponse.class)));
    }

    @Override
    public ActivitySourceStatus connectionStatus() {
        return switch (sessionStatus("").state()) {
            case CONNECTED -> ActivitySourceStatus.CONNECTED;
            case ABSENT -> ActivitySourceStatus.DISCONNECTED;
            case MFA_REQUIRED -> ActivitySourceStatus.EXPIRED;
        };
    }

    @Override
    public List<ExternalActivitySummary> pullActivities(
            LocalDate from,
            LocalDate to,
            int limit,
            String correlationId
    ) {
        ActivityListResponse response = call(() -> restClient.get()
                .uri(builder -> builder
                        .path("/interne/v1/activites")
                        .queryParam("limite", limit)
                        .queryParam("debut", from)
                        .queryParam("fin", to)
                        .build())
                .header(CORRELATION_HEADER, correlationId)
                .retrieve()
                .body(ActivityListResponse.class));
        return response == null ? List.of() : response.activities().stream().map(this::toSummary).toList();
    }

    @Override
    public ExternalActivityDetails getActivity(String garminActivityId) {
        ActivityResponse response = call(() -> restClient.get()
                .uri("/interne/v1/activites/{id}", garminActivityId)
                .retrieve()
                .body(ActivityResponse.class));
        if (response == null) throw new TemporaryGarminConnectorException("La réponse Garmin est vide.");
        return new ExternalActivityDetails(
                response.identifier(),
                instant(response.data()),
                text(response.data(), "activityType", "typeKey", "sport"),
                objectMapper.convertValue(response.data(), Map.class)
        );
    }

    @Override
    public byte[] downloadFit(String garminActivityId) {
        return call(() -> restClient.get()
                .uri("/interne/v1/activites/{id}/fit", garminActivityId)
                .accept(MediaType.parseMediaType("application/vnd.ant.fit"))
                .retrieve()
                .body(byte[].class));
    }

    @Override
    public void disconnect() {
        call(() -> restClient.delete().uri("/interne/v1/session").retrieve().toBodilessEntity());
    }

    private ExternalActivitySummary toSummary(ActivityResponse response) {
        JsonNode data = response.data();
        return new ExternalActivitySummary(
                response.identifier(),
                instant(data),
                text(data, "activityType", "typeKey", "sport"),
                longValue(data, "distance"),
                longValue(data, "duration")
        );
    }

    private Instant instant(JsonNode data) {
        String value = text(data, "startTimeGMT", "startTimeLocal", "dateHeure");
        if (value == null) return Instant.EPOCH;
        try {
            return Instant.parse(value.endsWith("Z") ? value : value + "Z");
        } catch (RuntimeException exception) {
            throw new PermanentGarminConnectorException("La date de l'activité Garmin est invalide.");
        }
    }

    private static String text(JsonNode data, String... fields) {
        for (String field : fields) {
            JsonNode value = data.path(field);
            if (value.isTextual()) return value.asText();
            if (value.isObject() && value.path("typeKey").isTextual()) return value.path("typeKey").asText();
        }
        return null;
    }

    private static Long longValue(JsonNode data, String field) {
        return data.path(field).isNumber() ? data.path(field).longValue() : null;
    }

    private static GarminSession toSession(SessionResponse response) {
        if (response == null) throw new TemporaryGarminConnectorException("Le service Garmin n'a pas répondu.");
        GarminSessionState state = switch (response.state()) {
            case "CONNECTEE" -> GarminSessionState.CONNECTED;
            case "MFA_REQUISE" -> GarminSessionState.MFA_REQUIRED;
            default -> GarminSessionState.ABSENT;
        };
        return new GarminSession(state, response.message());
    }

    private static GarminConnectorException mapError(HttpStatusCode status, ConnectorError error) {
        String message = error == null || error.message() == null
                ? "Le service Garmin est temporairement indisponible."
                : error.message();
        if (status.value() == 401 || hasType(error, "AUTHENTIFICATION")) {
            return new GarminConnectionExpiredException();
        }
        if (hasType(error, "DEFINITIVE", "PROTECTION")) {
            return new PermanentGarminConnectorException(message);
        }
        if (status.value() == 429 || status.is5xxServerError()
                || hasType(error, "TEMPORAIRE", "LIMITE_FREQUENCE", "INDISPONIBLE")) {
            return new TemporaryGarminConnectorException(message);
        }
        return new PermanentGarminConnectorException(message);
    }

    private static boolean hasType(ConnectorError error, String... types) {
        return error != null && List.of(types).contains(error.type());
    }

    private static <T> T call(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (ResourceAccessException exception) {
            throw new TemporaryGarminConnectorException(
                    "Le service Garmin est temporairement inaccessible.",
                    exception
            );
        }
    }

    private record ConnectionRequest(@JsonProperty("courriel") String email,
                                     @JsonProperty("mot_de_passe") String password) { }
    private record MfaRequest(String code) { }
    private record SessionResponse(@JsonProperty("etat") String state, String message) { }
    private record ActivityListResponse(@JsonProperty("activites") List<ActivityResponse> activities) { }
    private record ActivityResponse(@JsonProperty("identifiant") String identifier,
                                    @JsonProperty("donnees") JsonNode data) { }
    private record ConnectorError(String code, String type, String message,
                                  @JsonProperty("correlation_id") String correlationId) { }
}
