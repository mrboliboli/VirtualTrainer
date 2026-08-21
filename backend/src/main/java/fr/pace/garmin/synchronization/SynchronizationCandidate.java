package fr.pace.garmin.synchronization;

import fr.pace.activity.ExternalActivitySummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "synchronization_candidate")
public class SynchronizationCandidate {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "synchronization_id")
    private ActivitySynchronization synchronization;
    @Column(name = "external_id", nullable = false, length = 128) private String externalId;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(length = 100) private String sport;
    @Column(name = "distance_meters") private Long distanceMeters;
    @Column(name = "duration_seconds") private Long durationSeconds;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private CandidateConfidence confidence;

    protected SynchronizationCandidate() { }

    public static SynchronizationCandidate create(ActivitySynchronization synchronization, ExternalActivitySummary summary) {
        SynchronizationCandidate candidate = new SynchronizationCandidate();
        candidate.id = UUID.randomUUID();
        candidate.synchronization = synchronization;
        candidate.externalId = summary.sourceActivityId();
        candidate.startedAt = summary.startedAt();
        candidate.sport = summary.sport();
        candidate.distanceMeters = summary.distanceMeters();
        candidate.durationSeconds = summary.durationSeconds();
        candidate.confidence = CandidateConfidence.LOW;
        return candidate;
    }

    public String getExternalId() { return externalId; }
    public Instant getStartedAt() { return startedAt; }
    public String getSport() { return sport; }
    public Long getDistanceMeters() { return distanceMeters; }
    public Long getDurationSeconds() { return durationSeconds; }
    public CandidateConfidence getConfidence() { return confidence; }
}
