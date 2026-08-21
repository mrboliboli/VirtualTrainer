package fr.pace.profile;

/** Indique que le profil mono-utilisateur n'a pas encore été renseigné. */
public class ProfileNotConfiguredException extends RuntimeException {

    public ProfileNotConfiguredException() {
        super("Le profil du coureur n'est pas encore configuré.");
    }
}
