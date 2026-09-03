package fr.pace.garmin.synchronization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.ExternalActivityDetails;
import fr.pace.activity.ExternalActivitySummary;
import fr.pace.garmin.GarminActivityClient;
import fr.pace.garmin.GarminConnectionExpiredException;
import fr.pace.garmin.PermanentGarminConnectorException;
import fr.pace.garmin.TemporaryGarminConnectorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class GarminSynchronizationService {
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(4), Duration.ofHours(12), Duration.ofDays(1)
    );

    private final ActivitySynchronizationRepository synchronizationRepository;
    private final SynchronizedActivityRepository activityRepository;
    private final GarminActivityClient client;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public GarminSynchronizationService(
            ActivitySynchronizationRepository synchronizationRepository,
            SynchronizedActivityRepository activityRepository,
            GarminActivityClient client,
            ObjectMapper objectMapper
    ) {
        this(synchronizationRepository, activityRepository, client, objectMapper, Clock.systemUTC());
    }

    GarminSynchronizationService(
            ActivitySynchronizationRepository synchronizationRepository,
            SynchronizedActivityRepository activityRepository,
            GarminActivityClient client,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.synchronizationRepository = synchronizationRepository;
        this.activityRepository = activityRepository;
        this.client = client;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ActivitySynchronization start(String idempotencyKey) {
        return synchronizationRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> execute(ActivitySynchronization.create(idempotencyKey, clock.instant())));
    }

    @Transactional(readOnly = true)
    public ActivitySynchronization get(UUID id) {
        return synchronizationRepository.findById(id)
                .orElseThrow(() -> new SynchronizationNotFoundException(id));
    }

    @Transactional
    public void confirm(UUID id, String externalId) {
        ActivitySynchronization synchronization = get(id);
        SynchronizationCandidate candidate = synchronization.getCandidates()
                .stream()
                .filter(value -> value.getExternalId().equals(externalId))
                .findFirst()
                .orElseThrow(SynchronizationCandidateNotFoundException::new);
        if (activityRepository.findBySourceAndSourceExternalId("GARMIN_PERSONNEL", externalId).isPresent()) return;

        ExternalActivityDetails details = client.getActivity(
                externalId,
                candidate.getStartedAt(),
                candidate.getSport()
        );
        byte[] fit = client.downloadFit(externalId);
        activityRepository.save(SynchronizedActivity.create(
                externalId,
                serialize(details),
                fit,
                sha256(fit),
                clock.instant()
        ));
    }

    @Scheduled(fixedDelayString = "${pace.garmin.synchronization.retry-scan-delay:60000}")
    public void retryDueSynchronizations() {
        synchronizationRepository.findTop10ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
                SynchronizationStatus.TEMPORARY_ERROR,
                clock.instant()
        ).forEach(this::execute);
    }

    private ActivitySynchronization execute(ActivitySynchronization synchronization) {
        Instant now = clock.instant();
        synchronization.startAttempt(now);
        synchronizationRepository.save(synchronization);
        try {
            LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
            List<ExternalActivitySummary> activities = client.pullActivities(
                    today.minusDays(3),
                    today,
                    20,
                    synchronization.getId().toString()
            );
            List<SynchronizationCandidate> candidates = activities.stream()
                    .map(activity -> SynchronizationCandidate.create(synchronization, activity))
                    .toList();
            synchronization.complete(candidates, clock.instant());
        } catch (TemporaryGarminConnectorException exception) {
            handleTemporaryFailure(synchronization, exception);
        } catch (GarminConnectionExpiredException | PermanentGarminConnectorException exception) {
            synchronization.permanentFailure(exception.getMessage(), clock.instant());
        }
        return synchronizationRepository.save(synchronization);
    }

    private void handleTemporaryFailure(ActivitySynchronization synchronization, TemporaryGarminConnectorException exception) {
        int delayIndex = synchronization.getAttemptCount() - 1;
        if (delayIndex >= RETRY_DELAYS.size()) {
            synchronization.permanentFailure(
                    "Garmin reste indisponible après plusieurs tentatives. Relance la synchronisation plus tard.",
                    clock.instant()
            );
            return;
        }
        synchronization.temporaryFailure(
                exception.getMessage(),
                clock.instant().plus(RETRY_DELAYS.get(delayIndex)),
                clock.instant()
        );
    }

    private String serialize(ExternalActivityDetails details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            throw new PermanentGarminConnectorException("Les détails Garmin reçus sont invalides.");
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible", exception);
        }
    }
}
