# Décodage FIT

Le backend utilise le SDK Java FIT officiel de Garmin, version `21.205.0`, publiée
sur Maven Central. Le dépôt officiel est actif en 2026 et annonce Java 8 ou une
version supérieure ; la compilation et les tests de Pace restent imposés en Java 21.

Le SDK est régi par la **Flexible and Interoperable Data Transfer (FIT) Protocol
License**, et non par une licence libre généraliste. Elle autorise notamment un
usage interne, personnel et sans redevance, sous réserve du respect de ses
restrictions. Pace ne copie ni ne modifie le code du SDK dans son dépôt. Toute
distribution de l'application à un tiers devra faire l'objet d'une nouvelle
vérification juridique de cette licence.

Sources officielles :

- <https://github.com/garmin/fit-java-sdk>
- <https://developer.garmin.com/fit/>
- <https://github.com/garmin/fit-java-sdk/blob/main/LICENSE.txt>

Le fichier FIT original reste la source immuable. Les valeurs normalisées ne sont
créées que lorsqu'un message FIT fournit réellement la mesure correspondante. Le
traitement doit être rejouable à partir de l'original et remplacer atomiquement les
données précédemment décodées pour la même activité et la même version de décodeur.
