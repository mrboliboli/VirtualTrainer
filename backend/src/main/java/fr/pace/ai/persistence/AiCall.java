package fr.pace.ai.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_call")
public class AiCall {
    @Id private UUID id;
    @Column(name = "activity_id") private UUID activityId;
    @Enumerated(EnumType.STRING) @Column(name = "operation_type", nullable = false, length = 30) private AiOperationType operationType;
    @Column(nullable = false, length = 50) private String provider;
    @Column(nullable = false, length = 100) private String model;
    @Column(name = "prompt_version", nullable = false, length = 30) private String promptVersion;
    @Column(name = "schema_version", nullable = false, length = 30) private String schemaVersion;
    @Column(name = "analysis_version", nullable = false) private Integer analysisVersion;
    @Column(name = "input_json", nullable = false, columnDefinition = "TEXT") private String inputJson;
    @Column(name = "response_json", columnDefinition = "TEXT") private String responseJson;
    @Column(name = "provider_response_id", length = 200) private String providerResponseId;
    @Column(name = "duration_millis") private Long durationMillis;
    @Column(name = "input_tokens") private Integer inputTokens;
    @Column(name = "output_tokens") private Integer outputTokens;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AiCallStatus status;
    @Column(name = "attempt_count", nullable = false) private Integer attemptCount;
    @Column(name = "controlled_error", length = 1000) private String controlledError;
    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AiCall() { }

    public static AiCall pending(UUID activityId, AiOperationType operationType, String provider, String model,
                                 String promptVersion, String schemaVersion, int analysisVersion,
                                 String inputJson, Instant now) {
        if (operationType == null || provider == null || provider.isBlank() || model == null || model.isBlank()
                || promptVersion == null || promptVersion.isBlank() || schemaVersion == null || schemaVersion.isBlank()
                || analysisVersion < 1 || inputJson == null || inputJson.isBlank()) throw new IllegalArgumentException("Appel IA incomplet");
        AiCall call = new AiCall(); call.id = UUID.randomUUID(); call.activityId = activityId; call.operationType = operationType;
        call.provider = provider; call.model = model; call.promptVersion = promptVersion; call.schemaVersion = schemaVersion;
        call.analysisVersion = analysisVersion; call.inputJson = inputJson; call.status = AiCallStatus.EN_ATTENTE;
        call.attemptCount = 0; call.createdAt = now; call.updatedAt = now; return call;
    }

    public void start(Instant now) { status = AiCallStatus.EN_COURS; attemptCount++; controlledError = null; nextAttemptAt = null; updatedAt = now; }
    public void succeed(String responseJson, String responseId, long durationMillis, Integer inputTokens, Integer outputTokens, Instant now) {
        if (responseJson == null || responseJson.isBlank()) throw new IllegalArgumentException("Réponse IA absente");
        this.responseJson = responseJson; providerResponseId = responseId; this.durationMillis = durationMillis;
        this.inputTokens = inputTokens; this.outputTokens = outputTokens; status = AiCallStatus.REUSSIE; updatedAt = now;
    }
    public void failTemporarily(String error, Instant nextAttempt, Instant now) { fail(error, AiCallStatus.ERREUR_TEMPORAIRE, nextAttempt, now); }
    public void failPermanently(String error, Instant now) { fail(error, AiCallStatus.ERREUR_DEFINITIVE, null, now); }
    private void fail(String error, AiCallStatus target, Instant nextAttempt, Instant now) {
        controlledError = error == null ? null : error.substring(0, Math.min(error.length(), 1_000));
        status = target; nextAttemptAt = nextAttempt; updatedAt = now;
    }

    public UUID getId() { return id; } public UUID getActivityId() { return activityId; }
    public AiOperationType getOperationType() { return operationType; } public int getAnalysisVersion() { return analysisVersion; }
    public AiCallStatus getStatus() { return status; } public int getAttemptCount() { return attemptCount; }
    public String getInputJson() { return inputJson; } public String getResponseJson() { return responseJson; }
    public String getControlledError() { return controlledError; } public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public String getProvider() { return provider; } public String getModel() { return model; }
}
