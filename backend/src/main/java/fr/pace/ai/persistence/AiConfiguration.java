package fr.pace.ai.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_configuration")
public class AiConfiguration {
    @Id private UUID id;
    @Column(nullable = false) private Boolean enabled;
    @Column(name = "active_provider", nullable = false, length = 50) private String activeProvider;
    @Column(name = "base_url", nullable = false, length = 500) private String baseUrl;
    @Column(name = "analysis_model", nullable = false, length = 100) private String analysisModel;
    @Column(name = "planning_model", nullable = false, length = 100) private String planningModel;
    @Column(nullable = false) private Double temperature;
    @Column(name = "max_output_tokens", nullable = false) private Integer maxOutputTokens;
    @Column(name = "custom_instructions", length = 4000) private String customInstructions;
    @Column(name = "last_test_status", length = 30) private String lastTestStatus;
    @Column(name = "last_tested_at") private Instant lastTestedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AiConfiguration() { }

    public static AiConfiguration create(boolean enabled, String provider, String baseUrl, String analysisModel,
                                         String planningModel, double temperature, int maxOutputTokens,
                                         String customInstructions, Instant now) {
        validate(provider, baseUrl, analysisModel, planningModel, temperature, maxOutputTokens, customInstructions);
        AiConfiguration value = new AiConfiguration();
        value.id = UUID.randomUUID(); value.createdAt = now;
        value.update(enabled, provider, baseUrl, analysisModel, planningModel, temperature, maxOutputTokens, customInstructions, now);
        return value;
    }

    public void update(boolean enabled, String provider, String baseUrl, String analysisModel, String planningModel,
                       double temperature, int maxOutputTokens, String customInstructions, Instant now) {
        validate(provider, baseUrl, analysisModel, planningModel, temperature, maxOutputTokens, customInstructions);
        this.enabled = enabled; this.activeProvider = provider; this.baseUrl = baseUrl; this.analysisModel = analysisModel;
        this.planningModel = planningModel; this.temperature = temperature; this.maxOutputTokens = maxOutputTokens;
        this.customInstructions = blankToNull(customInstructions); this.updatedAt = now;
    }

    public void recordTest(String status, Instant now) {
        if (status == null || status.isBlank() || status.length() > 30) throw new IllegalArgumentException("Statut de test invalide");
        lastTestStatus = status; lastTestedAt = now; updatedAt = now;
    }

    private static void validate(String provider, String baseUrl, String analysisModel, String planningModel,
                                 double temperature, int maxOutputTokens, String instructions) {
        require(provider, 50); require(baseUrl, 500); require(analysisModel, 100); require(planningModel, 100);
        if (!Double.isFinite(temperature) || temperature < 0 || temperature > 2) throw new IllegalArgumentException("Température invalide");
        if (maxOutputTokens < 128 || maxOutputTokens > 16_384) throw new IllegalArgumentException("Longueur maximale invalide");
        if (instructions != null && instructions.length() > 4_000) throw new IllegalArgumentException("Instructions trop longues");
    }
    private static void require(String value, int max) { if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException("Configuration IA invalide"); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    public UUID getId() { return id; } public boolean isEnabled() { return enabled; } public String getActiveProvider() { return activeProvider; }
    public String getBaseUrl() { return baseUrl; } public String getAnalysisModel() { return analysisModel; }
    public String getPlanningModel() { return planningModel; } public Double getTemperature() { return temperature; }
    public Integer getMaxOutputTokens() { return maxOutputTokens; } public String getCustomInstructions() { return customInstructions; }
    public String getLastTestStatus() { return lastTestStatus; } public Instant getLastTestedAt() { return lastTestedAt; }
}
