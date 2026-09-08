package fr.pace.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.OptionalLong;

class OpenAiResponsesClient {
    private static final int MAX_RESPONSE_CHARACTERS = 200_000;
    private static final int MAX_RETRIES = 2;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties properties;
    private final Sleeper sleeper;

    OpenAiResponsesClient(ObjectMapper objectMapper, OpenAiProperties properties) {
        this(HttpClient.newBuilder().connectTimeout(properties.connectionTimeout()).build(),
                objectMapper, properties, Thread::sleep);
    }

    OpenAiResponsesClient(HttpClient httpClient, ObjectMapper objectMapper, OpenAiProperties properties, Sleeper sleeper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.sleeper = sleeper;
    }

    Response execute(String instructions, JsonNode input, String schemaName, JsonNode schema, String correlationId) {
        requireConfigured();
        String serializedInput = serialize(input);
        if (serializedInput.length() > properties.maxInputCharacters()) {
            throw new IllegalArgumentException("Les données envoyées à l'IA dépassent la limite autorisée.");
        }
        ObjectNode payload = payload(instructions, serializedInput, schemaName, schema);
        HttpRequest request = request(serialize(payload), correlationId);
        for (int attempt = 0; ; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.body() != null && response.body().length() > MAX_RESPONSE_CHARACTERS) {
                    throw new IllegalStateException("La réponse OpenAI dépasse la limite autorisée.");
                }
                if (response.statusCode() >= 200 && response.statusCode() < 300) return parse(response.body());
                if (retryable(response.statusCode()) && attempt < MAX_RETRIES) {
                    pause(response, attempt);
                    continue;
                }
                throw new OpenAiHttpException(response.statusCode(), "OpenAI a refusé la requête (HTTP " + response.statusCode() + ").");
            } catch (IOException exception) {
                throw new OpenAiHttpException(503, "Le service OpenAI est inaccessible.");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new OpenAiHttpException(503, "L'appel OpenAI a été interrompu.");
            }
        }
    }

    private ObjectNode payload(String instructions, String input, String schemaName, JsonNode schema) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.model());
        root.put("store", false);
        root.put("instructions", instructions);
        root.put("input", input);
        root.put("max_output_tokens", properties.maxOutputTokens());
        root.putArray("tools");
        root.put("tool_choice", "none");
        ObjectNode format = root.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("strict", true);
        format.set("schema", schema.deepCopy());
        return root;
    }

    private HttpRequest request(String body, String correlationId) {
        URI endpoint = properties.baseUrl().resolve("/v1/responses");
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(properties.requestTimeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (correlationId != null && correlationId.matches("[A-Za-z0-9._-]{1,100}")) {
            builder.header("X-Client-Request-Id", correlationId);
        }
        return builder.build();
    }

    private Response parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String id = requiredText(root, "id");
            String outputText = findOutputText(root);
            JsonNode structured = objectMapper.readTree(outputText);
            if (!structured.isObject()) throw new IllegalArgumentException("La sortie structurée n'est pas un objet.");
            JsonNode usage = root.path("usage");
            return new Response(id, structured,
                    integerOrNull(usage, "input_tokens"), integerOrNull(usage, "output_tokens"));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("La réponse OpenAI est invalide.", exception);
        }
    }

    private String findOutputText(JsonNode root) {
        JsonNode output = root.path("output");
        if (!output.isArray()) throw new IllegalArgumentException("Sortie absente.");
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) continue;
            for (JsonNode part : content) {
                if ("output_text".equals(part.path("type").asText()) && part.path("text").isTextual()) {
                    return part.path("text").textValue();
                }
                if ("refusal".equals(part.path("type").asText())) {
                    throw new IllegalArgumentException("Le modèle a refusé la demande.");
                }
            }
        }
        throw new IllegalArgumentException("Texte de sortie absent.");
    }

    private void pause(HttpResponse<String> response, int attempt) throws InterruptedException {
        long fallback = 250L * (1L << attempt);
        OptionalLong retryAfter = response.headers().firstValue("Retry-After").stream()
                .mapToLong(OpenAiResponsesClient::secondsToMillis).filter(value -> value >= 0).findFirst();
        sleeper.sleep(Math.min(retryAfter.orElse(fallback), Duration.ofSeconds(10).toMillis()));
    }

    private static long secondsToMillis(String value) {
        try {
            return Math.multiplyExact(Long.parseLong(value.trim()), 1_000L);
        } catch (RuntimeException exception) {
            return -1;
        }
    }

    private static boolean retryable(int status) {
        return status == 408 || status == 429 || status >= 500;
    }

    private String serialize(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Les données IA ne peuvent pas être sérialisées.", exception);
        }
    }

    private void requireConfigured() {
        if (!properties.configured()) throw new IllegalStateException("OpenAI n'est pas configuré.");
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException(field + " absent.");
        return value.textValue();
    }

    private static Integer integerOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToInt() ? value.intValue() : null;
    }

    record Response(String providerResponseId, JsonNode output, Integer inputTokens, Integer outputTokens) {
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long milliseconds) throws InterruptedException;
    }
}
