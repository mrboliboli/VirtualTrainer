package fr.pace.garmin.synchronization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.pace.activity.ExternalActivityDetails;
import fr.pace.activity.ExternalActivitySummary;
import fr.pace.garmin.GarminActivityClient;
import fr.pace.garmin.GarminConnectionExpiredException;
import fr.pace.garmin.GarminSynchronizationProperties;
import fr.pace.garmin.PermanentGarminConnectorException;
import fr.pace.garmin.TemporaryGarminConnectorException;
import fr.pace.profile.AthleteProfileRepository;
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
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final AthleteProfileRepository profileRepository;
    private final GarminSynchronizationProperties properties;

    @Autowired
    public GarminSynchronizationService(
            ActivitySynchronizationRepository synchronizationRepository,
            SynchronizedActivityRepository activityRepository,
            GarminActivityClient client,
            ObjectMapper objectMapper,
            AthleteProfileRepository profileRepository,
            GarminSynchronizationProperties properties
    ) {
        this(synchronizationRepository, activityRepository, client, objectMapper, profileRepository, properties, Clock.systemUTC());
    }

    GarminSynchronizationService(
            ActivitySynchronizationRepository synchronizationRepository,
            SynchronizedActivityRepository activityRepository,
            GarminActivityClient client,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this(synchronizationRepository, activityRepository, client, objectMapper, null,
                new GarminSynchronizationProperties("", 2, 7, java.time.ZoneId.of("Europe/Paris")), clock);
    }

    GarminSynchronizationService(
            ActivitySynchronizationRepository synchronizationRepository,
            SynchronizedActivityRepository activityRepository,
            GarminActivityClient client,
            ObjectMapper objectMapper,
            AthleteProfileRepository profileRepository,
            GarminSynchronizationProperties properties,
            Clock clock
    ) {
        this.synchronizationRepository = synchronizationRepository;
        this.activityRepository = activityRepository;
        this.client = client;
        this.objectMapper = objectMapper;
        this.profileRepository = profileRepository;
        this.properties = properties;
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
        ExternalActivityDetails details = client.getActivity(
                externalId,
                candidate.getStartedAt(),
                candidate.getSport()
        );
        var existing = activityRepository.findBySourceAndSourceExternalId("GARMIN_PERSONNEL", externalId);
        if (existing.isPresent()) {
            existing.get().refreshFeedback(
                    GarminSubjectiveFeedback.from(details, objectMapper),
                    serialize(details),
                    clock.instant()
            );
            activityRepository.save(existing.get());
            return;
        }
        byte[] fit = client.downloadFit(externalId);
        activityRepository.save(SynchronizedActivity.create(
                externalId,
                serialize(details),
                fit,
                sha256(fit),
                GarminSubjectiveFeedback.from(details, objectMapper),
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
            LocalDate today = LocalDate.ofInstant(now, properties.zoneId());
            LocalDate startDate = importStartDate(today);
            List<ExternalActivitySummary> newActivities = findNewActivities(startDate, today, synchronization.getId().toString());
            List<SynchronizationCandidate> candidates = newActivities.stream()
                    .limit(properties.candidateLimit())
                    .map(activity -> SynchronizationCandidate.create(synchronization, activity))
                    .toList();
            synchronization.complete(candidates, startDate, newActivities.size() <= properties.candidateLimit(), clock.instant());
        } catch (TemporaryGarminConnectorException exception) {
            handleTemporaryFailure(synchronization, exception);
        } catch (GarminConnectionExpiredException | PermanentGarminConnectorException exception) {
            synchronization.permanentFailure(exception.getMessage(), clock.instant());
        }
        return synchronizationRepository.save(synchronization);
    }

    private LocalDate importStartDate(LocalDate today) {
        LocalDate configured = properties.configuredImportStartDate();
        if (configured != null) return configured.isAfter(today) ? today : configured;
        if (profileRepository == null) return today.minusDays(3);
        return profileRepository.findFirstByOrderByCreatedAtAsc()
                .map(profile -> LocalDate.ofInstant(profile.getCreatedAt(), properties.zoneId()))
                .orElse(today);
    }

    private List<ExternalActivitySummary> findNewActivities(LocalDate startDate, LocalDate today, String correlationId) {
        List<ExternalActivitySummary> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        LocalDate windowEnd = today;
        while (!windowEnd.isBefore(startDate) && result.size() <= properties.candidateLimit()) {
            LocalDate windowStart = windowEnd.minusDays(properties.searchWindowDays() - 1L);
            if (windowStart.isBefore(startDate)) windowStart = startDate;
            List<ExternalActivitySummary> activities = client.pullActivities(windowStart, windowEnd, 100, correlationId)
                    .stream()
                    .sorted(Comparator.comparing(ExternalActivitySummary::startedAt).reversed())
                    .filter(activity -> seen.add(activity.sourceActivityId()))
                    .toList();
            List<String> identifiers = activities.stream().map(ExternalActivitySummary::sourceActivityId).toList();
            Set<String> imported = identifiers.isEmpty() ? Set.of() : activityRepository
                    .findAllBySourceAndSourceExternalIdIn("GARMIN_PERSONNEL", identifiers)
                    .stream()
                    .map(SynchronizedActivity::getSourceExternalId)
                    .collect(java.util.stream.Collectors.toSet());
            activities.stream().filter(activity -> !imported.contains(activity.sourceActivityId())).forEach(result::add);
            windowEnd = windowStart.minusDays(1);
        }
        result.sort(Comparator.comparing(ExternalActivitySummary::startedAt).reversed());
        return result;
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
