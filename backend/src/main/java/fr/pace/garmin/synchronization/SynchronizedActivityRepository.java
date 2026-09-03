package fr.pace.garmin.synchronization;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SynchronizedActivityRepository extends JpaRepository<SynchronizedActivity, UUID> {
    Optional<SynchronizedActivity> findBySourceAndSourceExternalId(String source, String sourceExternalId);
    List<SynchronizedActivity> findAllByOrderByDiscoveredAtDesc(Pageable pageable);
}
