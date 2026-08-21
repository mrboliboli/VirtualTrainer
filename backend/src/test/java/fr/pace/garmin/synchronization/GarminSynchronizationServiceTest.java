package fr.pace.garmin.synchronization;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.ExternalActivitySummary;
import fr.pace.garmin.GarminActivityClient;
import fr.pace.garmin.GarminConnectionExpiredException;
import fr.pace.garmin.PermanentGarminConnectorException;
import fr.pace.garmin.TemporaryGarminConnectorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarminSynchronizationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");
    @Mock private ActivitySynchronizationRepository synchronizationRepository;
    @Mock private SynchronizedActivityRepository activityRepository;
    @Mock private GarminActivityClient client;

    @Test
    @DisplayName("Devrait conserver une seule synchronisation pour une même clé d'idempotence")
    void start_shouldReuseSynchronization_whenIdempotencyKeyAlreadyExists() {
        // GIVEN
        ActivitySynchronization existing = ActivitySynchronization.create("meme-cle", NOW);
        when(synchronizationRepository.findByIdempotencyKey("meme-cle")).thenReturn(Optional.of(existing));

        // WHEN
        ActivitySynchronization result = service().start("meme-cle");

        // THEN
        assertThat(result).isSameAs(existing);
        verify(client, never()).pullActivities(any(), any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Devrait planifier une reprise progressive quand Garmin est temporairement indisponible")
    void start_shouldScheduleRetry_whenGarminFailsTemporarily() {
        // GIVEN
        when(synchronizationRepository.findByIdempotencyKey("nouvelle-cle")).thenReturn(Optional.empty());
        when(synchronizationRepository.save(any(ActivitySynchronization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(client.pullActivities(any(LocalDate.class), any(LocalDate.class), anyInt(), anyString()))
                .thenThrow(new TemporaryGarminConnectorException("Garmin indisponible"));

        // WHEN
        ActivitySynchronization result = service().start("nouvelle-cle");

        // THEN
        assertThat(result.getStatus()).isEqualTo(SynchronizationStatus.TEMPORARY_ERROR);
        assertThat(result.getAttemptCount()).isEqualTo(1);
        assertThat(result.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(15 * 60));
    }

    @Test
    @DisplayName("Ne devrait jamais reprendre après une protection Garmin")
    void start_shouldNeverScheduleRetry_whenGarminProtectionIsDetected() {
        // GIVEN
        prepareNewSynchronization("protection");
        when(client.pullActivities(any(LocalDate.class), any(LocalDate.class), anyInt(), anyString()))
                .thenThrow(new PermanentGarminConnectorException("Protection Garmin détectée"));

        // WHEN
        ActivitySynchronization result = service().start("protection");

        // THEN
        assertThat(result.getStatus()).isEqualTo(SynchronizationStatus.PERMANENT_ERROR);
        assertThat(result.getNextAttemptAt()).isNull();
    }

    @Test
    @DisplayName("Devrait terminer la synchronisation quand la connexion Garmin a expiré")
    void start_shouldFinishWithPermanentError_whenGarminConnectionHasExpired() {
        // GIVEN
        prepareNewSynchronization("session-expiree");
        when(client.pullActivities(any(LocalDate.class), any(LocalDate.class), anyInt(), anyString()))
                .thenThrow(new GarminConnectionExpiredException());

        // WHEN
        ActivitySynchronization result = service().start("session-expiree");

        // THEN
        assertThat(result.getStatus()).isEqualTo(SynchronizationStatus.PERMANENT_ERROR);
        assertThat(result.getNextAttemptAt()).isNull();
        assertThat(result.getErrorMessage()).contains("Reconnecte ton compte");
    }

    @Test
    @DisplayName("Devrait proposer les activités trouvées sans association silencieuse")
    void start_shouldExposeLowConfidenceCandidates_whenActivitiesAreFoundWithoutWorkoutMatching() {
        // GIVEN
        when(synchronizationRepository.findByIdempotencyKey("recherche")).thenReturn(Optional.empty());
        when(synchronizationRepository.save(any(ActivitySynchronization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(client.pullActivities(any(LocalDate.class), any(LocalDate.class), anyInt(), anyString()))
                .thenReturn(List.of(new ExternalActivitySummary("123", NOW, "running", 10_000L, 3_600L)));

        // WHEN
        ActivitySynchronization result = service().start("recherche");

        // THEN
        assertThat(result.getCandidates()).singleElement()
                .extracting(SynchronizationCandidate::getConfidence)
                .isEqualTo(CandidateConfidence.LOW);
    }

    private GarminSynchronizationService service() {
        return new GarminSynchronizationService(
                synchronizationRepository,
                activityRepository,
                client,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private void prepareNewSynchronization(String idempotencyKey) {
        when(synchronizationRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(synchronizationRepository.save(any(ActivitySynchronization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
