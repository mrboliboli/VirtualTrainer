package fr.pace.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {
}
