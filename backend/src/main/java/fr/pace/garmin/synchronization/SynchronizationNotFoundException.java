package fr.pace.garmin.synchronization;

import java.util.UUID;

public class SynchronizationNotFoundException extends RuntimeException {
    public SynchronizationNotFoundException(UUID id) {
        super("La synchronisation %s est introuvable.".formatted(id));
    }
}
