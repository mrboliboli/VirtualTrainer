package fr.pace.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotBlank @Size(max = 200) String nom,
        @NotNull LocalDate date,
        @DecimalMin("0.001") BigDecimal distance,
        DistanceUnit unite,
        @NotNull GoalType type,
        @Pattern(regexp = "^https://.*", message = "L'URL doit utiliser HTTPS") String url,
        @Min(0) @Max(100) int priorite,
        @Min(1) Integer objectifTempsSecondes,
        @Min(0) Integer deniveleCibleMetres,
        @Size(max = 4000) String notes,
        @NotNull GoalStatus statut,
        boolean principal
) {
    GoalData toData() {
        return new GoalData(
                nom, date, distance, unite, type, url, priorite,
                objectifTempsSecondes, deniveleCibleMetres, notes, statut, principal
        );
    }
}
