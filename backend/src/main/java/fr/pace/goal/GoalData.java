package fr.pace.goal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalData(
        String name,
        LocalDate eventDate,
        BigDecimal distance,
        DistanceUnit distanceUnit,
        GoalType type,
        String officialUrl,
        int priority,
        Integer targetTimeSeconds,
        Integer targetElevationGainMeters,
        String notes,
        GoalStatus status,
        boolean primary
) {
}
