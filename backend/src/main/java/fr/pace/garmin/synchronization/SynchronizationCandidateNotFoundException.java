package fr.pace.garmin.synchronization;

public class SynchronizationCandidateNotFoundException extends RuntimeException {
    public SynchronizationCandidateNotFoundException() {
        super("L'activité choisie ne fait pas partie des candidates de cette synchronisation.");
    }
}
