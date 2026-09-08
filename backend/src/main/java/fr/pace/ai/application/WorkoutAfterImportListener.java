package fr.pace.ai.application;

import fr.pace.garmin.synchronization.ActivityImportedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkoutAfterImportListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkoutAfterImportListener.class);
    private final NextWorkoutService workouts;
    public WorkoutAfterImportListener(NextWorkoutService workouts) { this.workouts = workouts; }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterImport(ActivityImportedEvent event) {
        if (!workouts.automaticEnabled()) return;
        try { workouts.generate(); }
        catch (RuntimeException exception) {
            LOGGER.warn("La génération automatique de la prochaine séance a échoué après l'import {}", event.activityId());
        }
    }
}
