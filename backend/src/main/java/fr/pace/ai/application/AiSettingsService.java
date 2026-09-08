package fr.pace.ai.application;

import fr.pace.ai.api.AiSettingsRequest;
import fr.pace.ai.domain.ActivityAnalysisRequest;
import fr.pace.ai.domain.AiProvider;
import fr.pace.ai.domain.ConfidenceLevel;
import fr.pace.ai.openai.OpenAiProperties;
import fr.pace.ai.persistence.AiConfiguration;
import fr.pace.ai.persistence.AiConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AiSettingsService {
    private final AiConfigurationRepository repository;
    private final OpenAiProperties properties;
    private final AiProvider provider;
    private final Clock clock = Clock.systemUTC();

    public AiSettingsService(AiConfigurationRepository repository, OpenAiProperties properties, AiProvider provider) {
        this.repository = repository; this.properties = properties; this.provider = provider;
    }

    @Transactional
    public AiConfiguration getOrCreate() {
        AiConfiguration value = repository.findFirstByOrderByCreatedAtAsc().orElseGet(() -> repository.save(AiConfiguration.create(
                false, "OPENAI", properties.baseUrl().toString(), properties.model(), properties.model(),
                0.2, properties.maxOutputTokens(), null, clock.instant())));
        if (!properties.model().equals(value.getAnalysisModel()) || !properties.model().equals(value.getPlanningModel())) {
            value.update(value.isEnabled(), value.getActiveProvider(), value.getBaseUrl(),
                    properties.model(), properties.model(), value.getTemperature(), value.getMaxOutputTokens(),
                    value.getCustomInstructions(), clock.instant());
            return repository.save(value);
        }
        return value;
    }

    @Transactional
    public AiConfiguration update(AiSettingsRequest request) {
        AiConfiguration value = getOrCreate();
        value.update(request.active(), request.fournisseur(), request.urlBase(), properties.model(), properties.model(),
                request.temperature(), request.jetonsMaximum(), request.instructionsPersonnalisees(), clock.instant());
        value.configureWorkoutAutomation(request.generationSeanceApresImport(), clock.instant());
        return repository.save(value);
    }

    @Transactional
    public AiConfiguration test() {
        AiConfiguration value = getOrCreate();
        try {
            provider.analyzeActivity(syntheticRequest());
            value.recordTest("REUSSI", clock.instant());
        } catch (RuntimeException exception) {
            value.recordTest("ECHEC", clock.instant());
        }
        return repository.save(value);
    }

    public boolean keyConfigured() { return properties.apiKey() != null && !properties.apiKey().isBlank(); }

    private static ActivityAnalysisRequest syntheticRequest() {
        return new ActivityAnalysisRequest(
                new ActivityAnalysisRequest.AthleteContext(null, null, 3, List.of()), null,
                new ActivityAnalysisRequest.ActivityContext(Instant.EPOCH, "test", 1000d, 360d, 360d, 0, Map.of()),
                null, List.of(), List.of(),
                List.of(new ActivityAnalysisRequest.AnalysisFact("test_fact", "Test technique", "valide", ConfidenceLevel.ELEVE)),
                List.of(), List.of("Données synthétiques de test, sans donnée sportive réelle"));
    }
}
