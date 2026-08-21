package fr.pace.garmin.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GarminConnectionResponse(
        String statut,
        String mode,
        GarminAccountResponse compte,
        Instant derniereSynchronisation,
        Instant derniereActivite,
        String defiMfaId
) {
    public record GarminAccountResponse(String nomAffiche) { }
}
