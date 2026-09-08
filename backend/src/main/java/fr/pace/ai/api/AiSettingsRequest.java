package fr.pace.ai.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiSettingsRequest(
        boolean active,
        @NotBlank @Pattern(regexp = "OPENAI") String fournisseur,
        @NotBlank @Pattern(regexp = "https://.*") @Size(max = 500) String urlBase,
        @NotBlank @Size(max = 100) String modeleAnalyse,
        @Size(max = 100) String modelePlanification,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
        @Min(128) @Max(16384) int jetonsMaximum,
        @Size(max = 4000) String instructionsPersonnalisees,
        boolean generationSeanceApresImport
) { }
