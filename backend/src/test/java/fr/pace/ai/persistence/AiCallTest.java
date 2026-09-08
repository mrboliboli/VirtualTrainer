package fr.pace.ai.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiCallTest {
    @Test
    void devraitConserverLesTransitionsEtLeNombreDeTentatives() {
        Instant now = Instant.parse("2026-09-07T10:00:00Z");
        AiCall call = AiCall.pending(UUID.randomUUID(), AiOperationType.ANALYSE_ACTIVITE,
                "OPENAI", "modele", "p1", "s1", 1, "{}", now);

        call.start(now.plusSeconds(1));
        call.failTemporarily("indisponible", now.plusSeconds(60), now.plusSeconds(2));
        call.start(now.plusSeconds(60));
        call.succeed("{\"resume\":\"ok\"}", "resp_1", 100, 20, 10, now.plusSeconds(61));

        assertEquals(AiCallStatus.REUSSIE, call.getStatus());
        assertEquals(2, call.getAttemptCount());
    }
}
