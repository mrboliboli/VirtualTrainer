package fr.pace.ai.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfigurationTest {
    @Test
    void devraitResterDesactiveeJusquaActivationExplicite() {
        Instant now = Instant.parse("2026-09-07T10:00:00Z");
        AiConfiguration configuration = AiConfiguration.create(false, "OPENAI", "https://api.openai.com/v1",
                "modele-analyse", "modele-planification", 0.2, 2_000, null, now);

        assertFalse(configuration.isEnabled());

        configuration.update(true, "OPENAI", "https://api.openai.com/v1",
                "modele-analyse", "modele-planification", 0.2, 2_000, null, now.plusSeconds(1));
        assertTrue(configuration.isEnabled());
    }
}
