package fr.pace.garmin;

/** Indique un échec Garmin qui pourra être retenté automatiquement. */
public class TemporaryGarminConnectorException extends GarminConnectorException {
    public TemporaryGarminConnectorException(String message) {
        super(message);
    }

    public TemporaryGarminConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
