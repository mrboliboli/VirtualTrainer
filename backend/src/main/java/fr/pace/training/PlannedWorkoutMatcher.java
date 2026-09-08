package fr.pace.training;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PlannedWorkoutMatcher {
    public Optional<Candidate> match(LocalDate activityDate, String activitySport, List<Candidate> candidates) {
        if (activityDate == null || !isRunning(activitySport)) return Optional.empty();
        List<Candidate> ranked = candidates.stream()
                .filter(candidate -> Math.abs(ChronoUnit.DAYS.between(candidate.plannedDate(), activityDate)) <= 1)
                .sorted(Comparator.comparingLong(candidate ->
                        Math.abs(ChronoUnit.DAYS.between(candidate.plannedDate(), activityDate))))
                .toList();
        if (ranked.isEmpty()) return Optional.empty();
        long bestDistance = Math.abs(ChronoUnit.DAYS.between(ranked.getFirst().plannedDate(), activityDate));
        long equallyClose = ranked.stream().filter(candidate ->
                Math.abs(ChronoUnit.DAYS.between(candidate.plannedDate(), activityDate)) == bestDistance).count();
        return equallyClose == 1 ? Optional.of(ranked.getFirst()) : Optional.empty();
    }

    private static boolean isRunning(String sport) {
        if (sport == null) return false;
        String normalized = sport.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("run") || normalized.equals("running") || normalized.equals("course")
                || normalized.equals("trail") || normalized.equals("trail_running");
    }

    public record Candidate(UUID id, LocalDate plannedDate) { }
}
