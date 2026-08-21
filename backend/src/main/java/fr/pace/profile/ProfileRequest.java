package fr.pace.profile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProfileRequest(
        @NotBlank @Size(max = 100) String prenom,
        @Min(1900) @Max(2100) Integer anneeNaissance,
        @Min(80) @Max(250) Integer tailleCentimetres,
        @DecimalMin("20.0") BigDecimal poidsKilogrammes,
        @Min(80) @Max(250) Integer frequenceCardiaqueMaximale,
        @Min(20) @Max(150) Integer frequenceCardiaqueRepos,
        @Min(60) @Max(240) Integer seuilCardiaque,
        @Min(50) @Max(2000) Integer seuilPuissance,
        @DecimalMin("0.0") BigDecimal volumeHebdomadaireHabituelKilometres,
        List<@Size(max = 100) String> joursDisponibles,
        @Min(1) @Max(1440) Integer dureeMaximaleSeanceMinutes,
        List<@Size(max = 100) String> terrainsAccessibles,
        @Size(max = 2000) String contraintesEtBlessures,
        @Size(max = 2000) String preferencesEntrainement,
        @Size(max = 4000) String commentaire
) {
    AthleteProfileData toData() {
        return new AthleteProfileData(
                prenom,
                anneeNaissance,
                tailleCentimetres,
                poidsKilogrammes,
                frequenceCardiaqueMaximale,
                frequenceCardiaqueRepos,
                seuilCardiaque,
                seuilPuissance,
                volumeHebdomadaireHabituelKilometres,
                join(joursDisponibles),
                dureeMaximaleSeanceMinutes,
                join(terrainsAccessibles),
                contraintesEtBlessures,
                preferencesEntrainement,
                commentaire
        );
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("|", values);
    }
}
