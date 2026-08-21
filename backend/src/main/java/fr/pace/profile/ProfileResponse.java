package fr.pace.profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record ProfileResponse(
        UUID id,
        String prenom,
        Integer anneeNaissance,
        Integer tailleCentimetres,
        BigDecimal poidsKilogrammes,
        Integer frequenceCardiaqueMaximale,
        Integer frequenceCardiaqueRepos,
        Integer seuilCardiaque,
        Integer seuilPuissance,
        BigDecimal volumeHebdomadaireHabituelKilometres,
        List<String> joursDisponibles,
        Integer dureeMaximaleSeanceMinutes,
        List<String> terrainsAccessibles,
        String contraintesEtBlessures,
        String preferencesEntrainement,
        String commentaire,
        Instant derniereModification
) {
    static ProfileResponse from(AthleteProfile profile) {
        return new ProfileResponse(
                profile.getId(), profile.getFirstName(), profile.getBirthYear(), profile.getHeightCm(),
                profile.getWeightKg(), profile.getMaximumHeartRate(), profile.getRestingHeartRate(),
                profile.getHeartRateThreshold(), profile.getPowerThreshold(), profile.getUsualWeeklyVolumeKm(),
                split(profile.getAvailableDays()), profile.getMaximumSessionDurationMinutes(), split(profile.getAvailableTerrains()),
                profile.getConstraintsAndInjuries(), profile.getTrainingPreferences(), profile.getNotes(),
                profile.getUpdatedAt()
        );
    }

    private static List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.split("\\|"));
    }
}
