package fr.pace.training;

import fr.pace.garmin.synchronization.ActivityImportedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkoutCompletionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkoutCompletionListener.class);
    private final WorkoutCompletionService service;

    public WorkoutCompletionListener(WorkoutCompletionService service) { this.service = service; }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterImport(ActivityImportedEvent event) {
        try {
            service.matchImportedActivity(event.activityId());
        } catch (RuntimeException exception) {
            LOGGER.warn("Le rapprochement de l'activité {} avec une séance prévue a échoué", event.activityId(), exception);
        }
    }
}
