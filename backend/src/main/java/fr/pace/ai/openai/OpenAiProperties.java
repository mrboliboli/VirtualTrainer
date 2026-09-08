package fr.pace.ai.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("pace.ai.openai")
public record OpenAiProperties(
        URI baseUrl,
        String apiKey,
        String model,
        Duration connectionTimeout,
        Duration requestTimeout,
        int maxInputCharacters,
        int maxOutputTokens
) {
    private static final URI DEFAULT_BASE_URL = URI.create("https://api.openai.com");

    public OpenAiProperties {
        baseUrl = baseUrl == null ? DEFAULT_BASE_URL : baseUrl;
        apiKey = apiKey == null || apiKey.isBlank() ? System.getenv("PACE_AI_API_KEY") : apiKey;
        model = model == null || model.isBlank() ? "gpt-5.6-luna" : model;
        connectionTimeout = connectionTimeout == null ? Duration.ofSeconds(3) : connectionTimeout;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
        maxInputCharacters = maxInputCharacters <= 0 ? 40_000 : maxInputCharacters;
        maxOutputTokens = maxOutputTokens <= 0 ? 2_000 : maxOutputTokens;
        if (!baseUrl.isAbsolute()) throw new IllegalArgumentException("L'URL OpenAI doit être absolue.");
        if (connectionTimeout.isNegative() || connectionTimeout.isZero()) {
            throw new IllegalArgumentException("Le délai de connexion OpenAI doit être positif.");
        }
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("Le délai de réponse OpenAI doit être positif.");
        }
        if (maxInputCharacters > 200_000) throw new IllegalArgumentException("L'entrée OpenAI est trop grande.");
        if (maxOutputTokens > 16_000) throw new IllegalArgumentException("La sortie OpenAI est trop grande.");
    }

    boolean configured() {
        return apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
    }
}
