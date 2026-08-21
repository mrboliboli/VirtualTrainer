package fr.pace.garmin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GarminMfaRequest(
        @NotBlank @Size(max = 100) String defiMfaId,
        @NotBlank @Size(min = 4, max = 12) String code
) {
}
