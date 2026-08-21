package fr.pace.goal;

import fr.pace.profile.AthleteProfile;
import fr.pace.profile.AthleteProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GoalService {
    private final GoalRepository repository;
    private final AthleteProfileService profileService;
    private final Clock clock;

    @Autowired
    public GoalService(GoalRepository repository, AthleteProfileService profileService) {
        this(repository, profileService, Clock.systemUTC());
    }

    GoalService(GoalRepository repository, AthleteProfileService profileService, Clock clock) {
        this.repository = repository;
        this.profileService = profileService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Goal> list() { return repository.findAllByArchivedFalseOrderByEventDateAsc(); }

    @Transactional
    public Goal create(GoalData data) {
        AthleteProfile profile = profileService.get();
        if (data.primary()) removeCurrentPrimary();
        return repository.save(Goal.create(UUID.randomUUID(), profile, data, clock.instant()));
    }

    @Transactional
    public Goal update(UUID id, GoalData data) {
        Goal goal = find(id);
        if (data.primary()) removeCurrentPrimary();
        goal.update(data, clock.instant());
        return goal;
    }

    @Transactional
    public void archive(UUID id) { find(id).archive(clock.instant()); }

    @Transactional
    public void delete(UUID id) { repository.delete(find(id)); }

    private Goal find(UUID id) { return repository.findById(id).orElseThrow(() -> new GoalNotFoundException(id)); }

    private void removeCurrentPrimary() {
        repository.findAllByPrimaryTrueAndArchivedFalse().forEach(Goal::removePrimary);
    }
}
