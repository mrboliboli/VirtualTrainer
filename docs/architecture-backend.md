# Architecture du backend

Le backend utilise Java 21, Spring Boot et Maven. La structure reste volontairement organisée par domaine fonctionnel : `profile`, `goal`, `activity` et `garmin`. Les contrôleurs exposent des DTO en français sous `/api/v1`; les entités JPA ne sont jamais renvoyées directement.

## Frontières principales

- `ActivitySource` est le contrat métier indépendant du fournisseur. La planification et l'analyse ne doivent connaître ni OAuth, ni webhook, ni format de réponse Garmin.
- `GarminActivityClient` est le port d'infrastructure réservé à l'API Garmin officielle. Il décrit l'autorisation, la synchronisation, le détail, le FIT, le backfill et la révocation.
- Aucune implémentation réseau Garmin n'est enregistrée tant que l'accès au programme, les identifiants et la documentation privée du portail ne sont pas disponibles.
- Le profil et les objectifs forment la première tranche verticale persistée avec PostgreSQL et versionnée par Flyway.

## Contrat HTTP initial

- `GET|PUT /api/v1/profil`
- `GET|POST /api/v1/objectifs`
- `PUT|DELETE /api/v1/objectifs/{id}`
- `POST /api/v1/objectifs/{id}/archivage`
- documentation OpenAPI : `/documentation-api`
- santé technique : `/actuator/health`

Les noms JSON destinés à l'interface sont en français. Les termes établis par les protocoles et le code (`OAuth`, `FIT`, `API`, noms de classes Java) restent dans leur forme courante.

Les collections `joursDisponibles` et `terrainsAccessibles` sont exposées comme des tableaux JSON. Les noms canoniques des mesures sont `tailleCentimetres`, `poidsKilogrammes` et `volumeHebdomadaireHabituelKilometres`. Un objectif expose son lien sous le nom `url`.

Les routes `/api/v1/tableau-de-bord` et `/api/v1/garmin/synchronisations` ne font pas partie de cette première tranche et ne doivent pas être appelées comme si elles étaient disponibles. L'accueil se compose pour l'instant à partir du profil et des objectifs. La synchronisation doit annoncer son indisponibilité tant que l'adaptateur Garmin officiel n'est pas configuré.

## Suite prévue

La prochaine tranche ajoutera les séances planifiées et leur historique de reports, puis leur comparaison avec les candidates Garmin. Ce séquencement permet de tester le métier sans prétendre que le connecteur personnel possède la stabilité ou les garanties d'une API officielle.

## Connecteur Garmin personnel expérimental

Le backend appelle le service auxiliaire exclusivement via `HttpGarminActivityClient`, derrière le port `GarminActivityClient`. Le contrat interne de référence est `garmin-connector/openapi.yaml`. La clé `X-Cle-Service`, les délais de connexion et de lecture et l'adresse interne sont fournis par configuration; aucun secret n'est journalisé.

Routes applicatives protégées ajoutées :

- `GET|POST|DELETE /api/v1/garmin/connexion`;
- `POST /api/v1/garmin/connexion/mfa`;
- `POST /api/v1/garmin/synchronisations` avec `X-Cle-Idempotence` facultative;
- `GET /api/v1/garmin/synchronisations/{id}`;
- `POST /api/v1/garmin/synchronisations/{id}/confirmation`.

Les erreurs réseau, limites de fréquence et indisponibilités sont temporaires et planifient des reprises à 15 minutes, 1 heure, 4 heures, 12 heures puis 24 heures. Les erreurs d'authentification et les capacités absentes exigent une action humaine. Une activité confirmée est persistée avec ses détails, son FIT original et son empreinte SHA-256; l'unicité `(source, identifiant externe)` empêche les doublons.

Les candidates sont pour l'instant présentées avec une confiance faible : aucune association silencieuse n'est faite tant que les séances planifiées et leur comparaison métier ne sont pas disponibles.

Dans Compose, `garmin-interne` transporte uniquement les appels backend vers le connecteur et reste `internal: true`. Le connecteur possède en plus le réseau `sortie-garmin`, non publié, nécessaire aux appels HTTPS vers Garmin. Docker Compose ne sait pas imposer seul une liste blanche de noms de domaine : celle-ci doit être configurée sur le pare-feu ou le mandataire sortant du NAS. Ni PostgreSQL ni le backend n'utilisent ce réseau de sortie dédié.

## Accès privé à l'application

Compose publie uniquement le frontal web, lié par défaut à `127.0.0.1`. Le backend,
PostgreSQL et le connecteur ne publient aucun port. Le frontal demande une
authentification HTTP et injecte vers le backend une clé interne distincte ; toutes
les routes `/api/**` refusent les appels qui ne portent pas cette clé. Les deux
secrets sont obligatoires et ne sont jamais écrits dans les journaux. Les écritures
exigent en plus un en-tête propre à l'interface : un formulaire provenant d'un site
tiers ne peut donc pas exploiter l'authentification HTTP mémorisée par le navigateur.

Le mot de passe Garmin et le code MFA ne doivent jamais circuler en HTTP sur un
réseau local. Pour un téléphone, conserver l'écoute locale et accéder à Pace par un
VPN qui chiffre jusqu'au NAS, ou placer le frontal derrière un mandataire inverse
HTTPS avec un certificat reconnu par le téléphone. Ne définir
`PACE_LISTEN_ADDRESS=0.0.0.0` que derrière l'une de ces protections. Le port 8080
seul n'est pas une configuration sûre pour activer le connecteur Garmin.
