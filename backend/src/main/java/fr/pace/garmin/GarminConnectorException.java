package fr.pace.garmin;

/** Erreur maîtrisée lors d'un échange avec le service auxiliaire Garmin. */
public abstract class GarminConnectorException extends RuntimeException {
    protected GarminConnectorException(String message) {
        super(message);
    }

    protected GarminConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
