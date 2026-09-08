# Collection Bruno — VirtualTrainer

1. Ouvrir Bruno puis **Open Collection** et sélectionner ce dossier `bruno`.
2. Choisir l'environnement **Local IntelliJ**.
3. Démarrer le backend Java dans IntelliJ sur le port 8080.

Le profil `ide` désactive la clé d'accès privée. Si un autre profil est utilisé,
ajouter l'en-tête `X-Cle-Pace` aux requêtes.

Les identifiants Garmin sont des variables secrètes Bruno et ne sont pas stockés
dans les fichiers. Les scripts mémorisent automatiquement les identifiants créés
(`objectifId`, `synchronisationId` et `defiMfaId`) dans l'environnement actif.
