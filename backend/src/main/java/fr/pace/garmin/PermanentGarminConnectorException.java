package fr.pace.garmin;

/** Indique un échec Garmin qui nécessite une action humaine. */
public class PermanentGarminConnectorException extends GarminConnectorException {
    public PermanentGarminConnectorException(String message) {
        super(message);
    }
}
