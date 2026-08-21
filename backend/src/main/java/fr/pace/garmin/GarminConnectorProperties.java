package fr.pace.garmin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pace.garmin.connector")
public record GarminConnectorProperties(
        String baseUrl,
        String token,
        Duration connectionTimeout,
        Duration readTimeout
) {
    public GarminConnectorProperties {
        connectionTimeout = connectionTimeout == null ? Duration.ofSeconds(3) : connectionTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(20) : readTimeout;
    }
}
