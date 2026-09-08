package fr.pace.ai.api;

import fr.pace.ai.persistence.AiConfiguration;

import java.time.Instant;

public record AiSettingsResponse(
        boolean active, String fournisseur, String urlBase, String modeleAnalyse,
        String modelePlanification, double temperature, int jetonsMaximum,
        String instructionsPersonnalisees, boolean cleConfiguree,
        String statutDernierTest, Instant dateDernierTest
) {
    public static AiSettingsResponse from(AiConfiguration value, boolean keyConfigured) {
        return new AiSettingsResponse(value.isEnabled(), value.getActiveProvider(), value.getBaseUrl(),
                value.getAnalysisModel(), value.getPlanningModel(), value.getTemperature(),
                value.getMaxOutputTokens(), value.getCustomInstructions(), keyConfigured,
                value.getLastTestStatus(), value.getLastTestedAt());
    }
}
