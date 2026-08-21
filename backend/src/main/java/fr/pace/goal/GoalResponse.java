package fr.pace.goal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String nom,
        LocalDate date,
        BigDecimal distance,
        DistanceUnit unite,
        GoalType type,
        String url,
        int priorite,
        Integer objectifTempsSecondes,
        Integer deniveleCibleMetres,
        String notes,
        GoalStatus statut,
        boolean principal,
        long joursRestants
) {
    static GoalResponse from(Goal goal, Clock clock) {
        return new GoalResponse(
                goal.getId(), goal.getName(), goal.getEventDate(), goal.getDistance(), goal.getDistanceUnit(),
                goal.getType(), goal.getOfficialUrl(), goal.getPriority(), goal.getTargetTimeSeconds(),
                goal.getTargetElevationGainMeters(), goal.getNotes(), goal.getStatus(), goal.isPrimary(),
                Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(clock), goal.getEventDate()))
        );
    }
}
