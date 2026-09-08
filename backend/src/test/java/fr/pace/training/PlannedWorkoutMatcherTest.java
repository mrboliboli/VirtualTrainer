package fr.pace.training;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlannedWorkoutMatcherTest {
    private final PlannedWorkoutMatcher matcher = new PlannedWorkoutMatcher();
    private final LocalDate date = LocalDate.of(2026, 9, 10);

    @Test
    void matchesTheUniqueClosestRunningWorkout() {
        var exact = new PlannedWorkoutMatcher.Candidate(UUID.randomUUID(), date);
        var previous = new PlannedWorkoutMatcher.Candidate(UUID.randomUUID(), date.minusDays(1));
        assertThat(matcher.match(date, "running", List.of(previous, exact))).contains(exact);
    }

    @Test
    void refusesANonRunningActivityAndAnActivityOutsideTheWindow() {
        var candidate = new PlannedWorkoutMatcher.Candidate(UUID.randomUUID(), date);
        assertThat(matcher.match(date, "cycling", List.of(candidate))).isEmpty();
        assertThat(matcher.match(date.plusDays(2), "running", List.of(candidate))).isEmpty();
    }

    @Test
    void refusesTwoEquallyCloseCandidates() {
        var previous = new PlannedWorkoutMatcher.Candidate(UUID.randomUUID(), date.minusDays(1));
        var next = new PlannedWorkoutMatcher.Candidate(UUID.randomUUID(), date.plusDays(1));
        assertThat(matcher.match(date, "trail_running", List.of(previous, next))).isEmpty();
    }
}
