package fr.pace.garmin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.time.ZoneId;

@ConfigurationProperties(prefix = "pace.garmin.synchronization")
public record GarminSynchronizationProperties(
        String importStartDate,
        int candidateLimit,
        int searchWindowDays,
        ZoneId zoneId
) {
    public GarminSynchronizationProperties {
        candidateLimit = candidateLimit <= 0 ? 2 : Math.min(candidateLimit, 20);
        searchWindowDays = searchWindowDays <= 0 ? 7 : Math.min(searchWindowDays, 31);
        zoneId = zoneId == null ? ZoneId.of("Europe/Paris") : zoneId;
    }

    public LocalDate configuredImportStartDate() {
        return importStartDate == null || importStartDate.isBlank() ? null : LocalDate.parse(importStartDate);
    }
}
