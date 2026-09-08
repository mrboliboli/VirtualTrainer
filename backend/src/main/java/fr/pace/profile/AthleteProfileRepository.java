package fr.pace.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {
    Optional<AthleteProfile> findFirstByOrderByCreatedAtAsc();
}
