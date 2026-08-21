package fr.pace.garmin.synchronization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SynchronizedActivityRepository extends JpaRepository<SynchronizedActivity, UUID> {
    Optional<SynchronizedActivity> findBySourceAndSourceExternalId(String source, String sourceExternalId);
}
