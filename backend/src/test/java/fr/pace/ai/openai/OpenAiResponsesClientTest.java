package fr.pace.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiResponsesClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsPrivateStrictBoundedRequestAndFindsOutputTextInAllItems() throws Exception {
        CapturingHandler handler = start(new StubResponse(200, """
                {"id":"resp_42","output":[
                  {"type":"reasoning","content":[]},
                  {"type":"message","content":[{"type":"output_text","text":"{\\"summary\\":\\"bien\\"}"}]}
                ],"usage":{"input_tokens":12,"output_tokens":8}}
                """));
        OpenAiResponsesClient client = client(1000, millis -> { });

        OpenAiResponsesClient.Response result = client.execute(
                "instruction", mapper.readTree("{\"sport\":\"running\"}"),
                "activity_analysis", mapper.readTree("{\"type\":\"object\"}"), "corr-123");

        assertThat(result.providerResponseId()).isEqualTo("resp_42");
        assertThat(result.output().path("summary").asText()).isEqualTo("bien");
        assertThat(result.inputTokens()).isEqualTo(12);
        assertThat(handler.authorization).isEqualTo("Bearer secret-test");
        assertThat(handler.correlation).isEqualTo("corr-123");
        JsonNode sent = mapper.readTree(handler.bodies.getFirst());
        assertThat(sent.path("store").asBoolean()).isFalse();
        assertThat(sent.path("max_output_tokens").asInt()).isEqualTo(321);
        assertThat(sent.path("tools")).isEmpty();
        assertThat(sent.path("tool_choice").asText()).isEqualTo("none");
        assertThat(sent.at("/text/format/type").asText()).isEqualTo("json_schema");
        assertThat(sent.at("/text/format/strict").asBoolean()).isTrue();
    }

    @Test
    void retriesAtMostTwiceForRetryableStatusesAndHonorsRetryAfter() throws Exception {
        CapturingHandler handler = start(
                new StubResponse(429, "{}", "0"), new StubResponse(503, "{}"),
                new StubResponse(200, validResponse("ok")));
        List<Long> waits = new ArrayList<>();

        OpenAiResponsesClient.Response result = client(1000, waits::add).execute(
                "instruction", mapper.createObjectNode(), "schema", mapper.createObjectNode(), null);

        assertThat(result.output().path("summary").asText()).isEqualTo("ok");
        assertThat(handler.calls).hasValue(3);
        assertThat(waits).containsExactly(0L, 500L);
    }

    @Test
    void stopsAfterTwoRetries() throws Exception {
        CapturingHandler handler = start(new StubResponse(500, "{}"), new StubResponse(500, "{}"),
                new StubResponse(500, "{}"), new StubResponse(200, validResponse("too late")));

        assertThatThrownBy(() -> client(1000, millis -> { }).execute(
                "instruction", mapper.createObjectNode(), "schema", mapper.createObjectNode(), null))
                .isInstanceOf(OpenAiHttpException.class)
                .hasMessageContaining("HTTP 500");
        assertThat(handler.calls).hasValue(3);
    }

    @Test
    void neverRetriesAuthenticationFailureAndDoesNotExposeResponseBody() throws Exception {
        CapturingHandler handler = start(new StubResponse(401, "{\"error\":\"secret detail\"}"));

        assertThatThrownBy(() -> client(1000, millis -> { }).execute(
                "instruction", mapper.createObjectNode(), "schema", mapper.createObjectNode(), null))
                .isInstanceOf(OpenAiHttpException.class)
                .hasMessageNotContaining("secret detail");
        assertThat(handler.calls).hasValue(1);
    }

    @Test
    void rejectsOversizedInputBeforeNetworkCall() throws Exception {
        CapturingHandler handler = start(new StubResponse(200, validResponse("unused")));

        assertThatThrownBy(() -> client(5, millis -> { }).execute(
                "instruction", mapper.readTree("{\"long\":\"value\"}"),
                "schema", mapper.createObjectNode(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("limite");
        assertThat(handler.calls).hasValue(0);
    }

    @Test
    void rejectsRefusalAndMalformedStructuredOutput() throws Exception {
        start(new StubResponse(200, """
                {"id":"resp_refused","output":[{"content":[{"type":"refusal","refusal":"non"}]}]}
                """));

        assertThatThrownBy(() -> client(1000, millis -> { }).execute(
                "instruction", mapper.createObjectNode(), "schema", mapper.createObjectNode(), null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("invalide");
    }

    private OpenAiResponsesClient client(int maxInput, OpenAiResponsesClient.Sleeper sleeper) {
        OpenAiProperties properties = new OpenAiProperties(
                URI.create("http://localhost:" + server.getAddress().getPort()), "secret-test", "model-test",
                Duration.ofSeconds(1), Duration.ofSeconds(2), maxInput, 321);
        return new OpenAiResponsesClient(HttpClient.newHttpClient(), mapper, properties, sleeper);
    }

    private CapturingHandler start(StubResponse... responses) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        CapturingHandler handler = new CapturingHandler(List.of(responses));
        server.createContext("/v1/responses", handler::handle);
        server.start();
        return handler;
    }

    private static String validResponse(String summary) {
        return "{\"id\":\"resp\",\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"summary\\\":\\\""
                + summary + "\\\"}\"}]}]}";
    }

    private record StubResponse(int status, String body, String retryAfter) {
        StubResponse(int status, String body) { this(status, body, null); }
    }

    private static final class CapturingHandler {
        private final Queue<StubResponse> responses;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<String> bodies = new ArrayList<>();
        private String authorization;
        private String correlation;

        private CapturingHandler(List<StubResponse> responses) { this.responses = new ArrayDeque<>(responses); }

        private void handle(HttpExchange exchange) throws IOException {
            calls.incrementAndGet();
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            correlation = exchange.getRequestHeaders().getFirst("X-Client-Request-Id");
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            StubResponse response = responses.remove();
            if (response.retryAfter() != null) exchange.getResponseHeaders().add("Retry-After", response.retryAfter());
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }
}
