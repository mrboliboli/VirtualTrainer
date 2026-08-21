package fr.pace.garmin.synchronization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivitySynchronizationRepository extends JpaRepository<ActivitySynchronization, UUID> {
    @EntityGraph(attributePaths = "candidates")
    Optional<ActivitySynchronization> findByIdempotencyKey(String idempotencyKey);
    @Override
    @EntityGraph(attributePaths = "candidates")
    Optional<ActivitySynchronization> findById(UUID id);
    @EntityGraph(attributePaths = "candidates")
    List<ActivitySynchronization> findTop10ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
            SynchronizationStatus status,
            Instant now
    );
}
