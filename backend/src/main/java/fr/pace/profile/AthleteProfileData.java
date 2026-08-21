package fr.pace.profile;

import java.math.BigDecimal;

public record AthleteProfileData(
        String firstName,
        Integer birthYear,
        Integer heightCm,
        BigDecimal weightKg,
        Integer maximumHeartRate,
        Integer restingHeartRate,
        Integer heartRateThreshold,
        Integer powerThreshold,
        BigDecimal usualWeeklyVolumeKm,
        String availableDays,
        Integer maximumSessionDurationMinutes,
        String availableTerrains,
        String constraintsAndInjuries,
        String trainingPreferences,
        String notes
) {
}
