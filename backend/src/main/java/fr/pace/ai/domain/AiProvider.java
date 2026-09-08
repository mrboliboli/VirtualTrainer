package fr.pace.ai.domain;

public interface AiProvider {
    ActivityAnalysisResult analyzeActivity(ActivityAnalysisRequest request);

    WorkoutGenerationResult generateWorkout(WorkoutGenerationRequest request);
}
