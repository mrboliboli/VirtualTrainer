package fr.pace.ai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiConfigurationRepository extends JpaRepository<AiConfiguration, UUID> {
    Optional<AiConfiguration> findFirstByOrderByCreatedAtAsc();
}
