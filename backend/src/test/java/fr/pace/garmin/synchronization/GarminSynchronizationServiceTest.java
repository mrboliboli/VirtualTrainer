package fr.pace.garmin.synchronization;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.ExternalActivitySummary;
import fr.pace.activity.ExternalActivityDetails;
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
import org.mockito.ArgumentCaptor;

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

    @Test
    @DisplayName("Devrait exclure les sorties importées et ne proposer que les deux nouvelles plus récentes")
    void start_shouldExcludeImportedActivitiesAndLimitCandidates() {
        prepareNewSynchronization("historique");
        ExternalActivitySummary imported = new ExternalActivitySummary("1", NOW, "running", 10_000L, 3_600L);
        ExternalActivitySummary recent = new ExternalActivitySummary("2", NOW.minusSeconds(60), "running", 8_000L, 2_800L);
        ExternalActivitySummary older = new ExternalActivitySummary("3", NOW.minusSeconds(120), "running", 7_000L, 2_500L);
        ExternalActivitySummary ignored = new ExternalActivitySummary("4", NOW.minusSeconds(180), "running", 6_000L, 2_100L);
        when(client.pullActivities(any(LocalDate.class), any(LocalDate.class), anyInt(), anyString()))
                .thenReturn(List.of(imported, older, ignored, recent));
        SynchronizedActivity existing = org.mockito.Mockito.mock(SynchronizedActivity.class);
        when(existing.getSourceExternalId()).thenReturn("1");
        when(activityRepository.findAllBySourceAndSourceExternalIdIn(anyString(), any()))
                .thenReturn(List.of(existing));

        ActivitySynchronization result = service().start("historique");

        assertThat(result.getCandidates()).extracting(SynchronizationCandidate::getExternalId)
                .containsExactly("2", "3");
    }

    @Test
    @DisplayName("Devrait confirmer avec la date et le sport de la candidate puis persister le FIT")
    void confirm_shouldPersistFitAndCandidateMetadata_whenDetailHasNoRootDate() {
        // GIVEN
        ActivitySynchronization synchronization = ActivitySynchronization.create("confirmation", NOW);
        synchronization.complete(
                List.of(SynchronizationCandidate.create(
                        synchronization,
                        new ExternalActivitySummary("123", NOW, "running", 10_000L, 3_600L)
                )),
                NOW
        );
        when(synchronizationRepository.findById(synchronization.getId())).thenReturn(Optional.of(synchronization));
        when(activityRepository.findBySourceAndSourceExternalId("GARMIN_PERSONNEL", "123"))
                .thenReturn(Optional.empty());
        when(client.getActivity("123", NOW, "running"))
                .thenReturn(new ExternalActivityDetails("123", NOW, "running", java.util.Map.of("summaryDTO", java.util.Map.of())));
        when(client.downloadFit("123")).thenReturn(new byte[]{1, 2, 3});

        // WHEN
        service().confirm(synchronization.getId(), "123");

        // THEN
        verify(client).getActivity("123", NOW, "running");
        verify(client).downloadFit("123");
        verify(activityRepository).save(any(SynchronizedActivity.class));
    }

    @Test
    @DisplayName("Devrait persister le ressenti Garmin sans demander de seconde saisie")
    void confirm_shouldPersistGarminFeedback_whenAvailable() {
        ActivitySynchronization synchronization = ActivitySynchronization.create("ressenti", NOW);
        synchronization.complete(List.of(SynchronizationCandidate.create(
                synchronization, new ExternalActivitySummary("456", NOW, "running", 5_000L, 1_500L)
        )), NOW);
        when(synchronizationRepository.findById(synchronization.getId())).thenReturn(Optional.of(synchronization));
        when(activityRepository.findBySourceAndSourceExternalId("GARMIN_PERSONNEL", "456"))
                .thenReturn(Optional.empty());
        when(client.getActivity("456", NOW, "running")).thenReturn(new ExternalActivityDetails(
                "456", NOW, "running", java.util.Map.of(
                        "summaryDTO", java.util.Map.of("perceivedExertion", 70, "activityFeel", 75)
                )
        ));
        when(client.downloadFit("456")).thenReturn(new byte[]{1, 2, 3});

        service().confirm(synchronization.getId(), "456");

        ArgumentCaptor<SynchronizedActivity> captor = ArgumentCaptor.forClass(SynchronizedActivity.class);
        verify(activityRepository).save(captor.capture());
        assertThat(captor.getValue().getPerceivedExertionRpe()).isEqualTo(7);
        assertThat(captor.getValue().getGarminFeelingScore()).isEqualTo(75);
        assertThat(captor.getValue().getSubjectiveFeedbackSource()).isEqualTo("GARMIN");
    }

    private GarminSynchronizationService service() {
        return new GarminSynchronizationService(
                synchronizationRepository,
                activityRepository,
                client,
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private void prepareNewSynchronization(String idempotencyKey) {
        when(synchronizationRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(synchronizationRepository.save(any(ActivitySynchronization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
