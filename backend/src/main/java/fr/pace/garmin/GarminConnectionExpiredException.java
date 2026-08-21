package fr.pace.garmin;

/** Indique que la session Garmin n'est plus utilisable. */
public class GarminConnectionExpiredException extends GarminConnectorException {
    public GarminConnectionExpiredException() {
        super("La connexion Garmin a expiré. Reconnecte ton compte pour continuer.");
    }
}
