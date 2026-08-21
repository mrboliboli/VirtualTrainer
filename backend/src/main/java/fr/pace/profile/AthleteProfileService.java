package fr.pace.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AthleteProfileService {

    private final AthleteProfileRepository repository;
    private final Clock clock;

    @Autowired
    public AthleteProfileService(AthleteProfileRepository repository) {
        this(repository, Clock.systemUTC());
    }

    AthleteProfileService(AthleteProfileRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AthleteProfile get() {
        return repository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(ProfileNotConfiguredException::new);
    }

    @Transactional
    public AthleteProfile save(AthleteProfileData data) {
        Instant now = clock.instant();
        AthleteProfile profile;
        if (repository.count() == 0) {
            profile = AthleteProfile.create(UUID.randomUUID(), data, now);
        } else {
            profile = repository.findAll().getFirst();
            profile.update(data, now);
        }
        return repository.save(profile);
    }
}
