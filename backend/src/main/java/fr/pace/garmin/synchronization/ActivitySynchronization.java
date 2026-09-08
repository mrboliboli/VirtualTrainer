package fr.pace.garmin.synchronization;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "activity_synchronization")
public class ActivitySynchronization {
    @Id private UUID id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100) private String idempotencyKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private SynchronizationStatus status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "error_message", length = 1000) private String errorMessage;
    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "search_start_date") private java.time.LocalDate searchStartDate;
    @Column(name = "catch_up_complete") private Boolean catchUpComplete;
    @OneToMany(mappedBy = "synchronization", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SynchronizationCandidate> candidates = new ArrayList<>();

    protected ActivitySynchronization() { }

    public static ActivitySynchronization create(String idempotencyKey, Instant now) {
        ActivitySynchronization synchronization = new ActivitySynchronization();
        synchronization.id = UUID.randomUUID();
        synchronization.idempotencyKey = idempotencyKey;
        synchronization.status = SynchronizationStatus.IN_PROGRESS;
        synchronization.createdAt = now;
        synchronization.updatedAt = now;
        return synchronization;
    }

    public void startAttempt(Instant now) {
        status = SynchronizationStatus.IN_PROGRESS;
        attemptCount++;
        errorMessage = null;
        nextAttemptAt = null;
        updatedAt = now;
        candidates.clear();
    }

    public void complete(List<SynchronizationCandidate> values, java.time.LocalDate startDate, boolean complete, Instant now) {
        candidates.addAll(values);
        searchStartDate = startDate;
        catchUpComplete = complete;
        status = SynchronizationStatus.COMPLETED;
        updatedAt = now;
    }

    public void complete(List<SynchronizationCandidate> values, Instant now) {
        complete(values, null, false, now);
    }

    public void temporaryFailure(String message, Instant nextAttempt, Instant now) {
        status = SynchronizationStatus.TEMPORARY_ERROR;
        errorMessage = message;
        nextAttemptAt = nextAttempt;
        updatedAt = now;
    }

    public void permanentFailure(String message, Instant now) {
        status = SynchronizationStatus.PERMANENT_ERROR;
        errorMessage = message;
        nextAttemptAt = null;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public SynchronizationStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public List<SynchronizationCandidate> getCandidates() { return List.copyOf(candidates); }
    public java.time.LocalDate getSearchStartDate() { return searchStartDate; }
    public boolean isCatchUpComplete() { return Boolean.TRUE.equals(catchUpComplete); }
}
