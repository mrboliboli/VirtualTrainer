package fr.pace.garmin.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GarminConnectionRequest(
        @Email @Size(max = 254) String identifiant,
        @NotBlank @Size(max = 512) String motDePasse,
        @AssertTrue boolean consentementRisques
) {
}
