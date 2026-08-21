# Revue indépendante du code — tranche initiale Pace

Date : 21 août 2026  
Périmètre examiné : `backend/`, `frontend/`, cahier des charges enrichi et conception d’expérience.  
Règle de lecture : P0 critique, P1 bloquant ou majeur, P2 important, P3 amélioration.

## Synthèse

Aucun P0 ni P1 ne reste ouvert et aucun secret Garmin ou IA n’est présent dans le dépôt. Le socle Java compile et ses cinq tests passent. Les cinq tests frontend passent également, la construction de production réussit et les contrats visibles sont désormais alignés. La tranche reste volontairement incomplète : elle ne constitue ni la V1 intégrale ni une application NAS installable.

La direction « sentier calme », le français, les cibles tactiles, le focus visible, le lien d’évitement et la réduction des mouvements sont cohérents avec la conception. L’étude Garmin identifie honnêtement le blocage externe et n’introduit aucun connecteur non officiel.

## Constats P1 corrigés pendant la revue

- Construction frontend : corrigée par l’ajout des types Vite et la suppression de l’option TypeScript incompatible. `npm run build` est vert.
- Profil : noms et tableaux jours/terrains alignés ; un test de contrôleur verrouille le JSON français.
- Objectifs : `url`, `objectifTempsSecondes`, `deniveleCibleMetres` et l’action POST d’archivage sont alignés.
- Routes absentes : l’Accueil compose les ressources réellement disponibles et l’écran Garmin présente honnêtement l’attente d’approbation, sans bouton factice.

## P2 — importants

### P2.1 — Les tests restent trop limités pour la suite et ne couvrent pas PostgreSQL

- Emplacements : `frontend/src/api.test.ts:1-15`, `backend/src/test/java/fr/pace/profile/AthleteProfileServiceTest.java:20-62`, `backend/src/test/resources/application.yml:1-10`.
- Constat : deux tests de contrôleur verrouillent maintenant une partie des contrats Profil et archivage. Les tests frontend restent des tests unitaires du client/navigation ; les tests backend utilisent H2 avec Flyway désactivé. Aucun test Testcontainers, synchronisation, idempotence, reprise réseau, FIT, calcul métier, accessibilité automatisée ou parcours de bout en bout n’existe dans la tranche.
- Impact : le vrai schéma PostgreSQL et l’aller-retour complet navigateur/API/base ne sont pas éprouvés.
- Correction recommandée : ajouter en priorité un test Testcontainers exécutant Flyway et un parcours profil/objectifs de bout en bout ; ajouter ensuite les tests métier au rythme des tranches.

### P2.2 — Le socle ne limite pas encore les requêtes ni l’exposition réseau

- Emplacement : `backend/src/main/resources/application.yml:1-25`.
- Constat : seule la taille des en-têtes HTTP est limitée. Aucun plafond explicite de corps de requête, aucune stratégie CORS restrictive, aucune séparation entre callback Garmin publiquement exposable et API contenant les données sportives n’est implémentée. Aucune protection d’accès locale n’est présente.
- Impact : dès qu’un mandataire inverse exposera le callback OAuth/webhook, une mauvaise règle de routage pourrait exposer profil et données sportives ; les payloads ne disposent pas des garde-fous demandés.
- Correction recommandée : avant toute exposition, définir une origine autorisée configurable, des limites de corps au serveur/mandataire, et une route publique minimale distincte. Documenter le modèle de menace local et protéger l’interface/API (au minimum authentification simple adaptée à un usage personnel) si elles peuvent sortir du réseau de confiance.

### P2.3 — Le mot de passe PostgreSQL possède une valeur de repli triviale

- Emplacement : `backend/src/main/resources/application.yml:5-7`.
- Constat : l’utilisateur, le nom de base et surtout le mot de passe retombent sur `pace` lorsque les variables sont absentes.
- Impact : une configuration NAS incomplète démarre avec des identifiants prévisibles, ce qui fragilise la base si son port est exposé par erreur.
- Correction recommandée : rendre le secret obligatoire hors profil de développement, conserver les valeurs commodes dans un profil local explicite uniquement, et s’assurer que Compose n’expose pas PostgreSQL au réseau hôte.

### P2.4 — Le modèle mono-utilisateur n’est pas garanti atomiquement

- Emplacements : `backend/src/main/java/fr/pace/profile/AthleteProfileService.java:25-43`, `backend/src/main/resources/db/migration/V1__profil_et_objectifs.sql:1-20`.
- Constat : `count()` puis `findAll()`/`save()` effectuent une décision en plusieurs requêtes, et la base n’impose aucune unicité garantissant un seul profil.
- Impact : deux requêtes simultanées peuvent créer deux profils ; les lectures suivantes prennent arbitrairement le premier, contaminant objectifs et données futures.
- Correction recommandée : utiliser un identifiant mono-utilisateur stable ou une contrainte d’unicité dédiée, puis effectuer un upsert/verrouillage transactionnel. Tester deux créations concurrentes.

### P2.5 — La version de production locale et la persistance NAS ne sont pas encore livrées

- Emplacements : racine du dépôt (absence de `docker-compose.yml`, `.env.example`, Dockerfiles et README principal).
- Impact : le scénario demandé — une commande Compose, redémarrage, volumes, sauvegarde/restauration, téléphone sur le réseau local — n’est pas vérifiable.
- Correction recommandée : livrer la tranche Docker avant de présenter le socle comme installable, avec healthchecks, réseau interne, volumes persistants et secrets obligatoires. Ce point n’est pas imputable au blocage d’approbation Garmin.

## P3 — améliorations

### P3.1 — Les dépendances frontend sont déclarées avec `latest`

- Emplacement : `frontend/package.json:13-27`.
- Impact : le verrou npm stabilise l’installation actuelle, mais toute régénération du verrou peut introduire simultanément des versions majeures incompatibles et compliquer les mises à jour/retours arrière sur NAS.
- Correction recommandée : enregistrer des plages SemVer explicites issues du verrou et utiliser `npm ci` dans les contrôles et images.

### P3.2 — L’horloge du contrôleur Objectif n’est pas injectée

- Emplacement : `backend/src/main/java/fr/pace/goal/GoalController.java:23-30`.
- Impact : le calcul de `joursRestants` dépend du fuseau système et est moins testable, alors que le fuseau doit devenir configurable.
- Correction recommandée : injecter un `Clock` configuré pour le fuseau applicatif, comme dépendance immuable, et couvrir les limites de date/fuseau.

## Vérifications exécutées

- `mvn test` : succès, 5 tests, aucune erreur.
- `npm test -- --run` : succès, 2 fichiers et 5 tests.
- `npm run build` : succès, paquet Vite produit.
- `git diff --check` : succès.
- Recherche statique ciblée : contrôleurs/routes, secrets usuels, CORS, configuration, dépendances et fichiers de déploiement.
- Inspection visuelle statique des jetons et règles adaptatives : cohérentes avec « sentier calme » ; aucune donnée décorative factice repérée. Une validation réelle aux largeurs 320/375/768/1280 px, zoom 200 %, clavier et lecteur d’écran reste à effectuer après correction de la construction.

## Blocage externe distinct des défauts de code

L’approbation du programme Garmin Connect Developer, ses identifiants OAuth et sa documentation privée empêchent légitimement de déclarer le connecteur officiel opérationnel. `docs/etude-garmin.md` le formule correctement. Le frontend ne simule plus cette fonction et présente l’attente explicitement. Ce blocage n’explique toutefois pas l’absence du socle Docker, qui reste une tranche réalisable indépendamment.

---

# Revue indépendante — phase connecteur Garmin personnel

Date : 21 août 2026  
État relu : écritures Python, Java, frontend et Compose annoncées figées.  
Verdict final : aucun P0/P1 ouvert après corrections et contre-vérification. L’activation sur un compte réel reste conditionnée à un accès HTTPS/VPN et à la décision explicite d’accepter les risques du connecteur non officiel.

## P1 détectés puis corrigés pendant la revue

### P1.1 — Le mot de passe Garmin traverse une API locale sans authentification applicative ni HTTPS

- Emplacements : `frontend/src/api.ts:54`, `backend/src/main/java/fr/pace/garmin/api/GarminConnectionController.java:18-41`, `docker-compose.yml:63-68`.
- Preuve : le téléphone poste `motDePasse` vers le backend ; toutes les routes `/api/v1/garmin/**` sont publiques dans Spring et le port 8080 est publié en HTTP. Aucun filtre d’authentification, jeton utilisateur, TLS ou restriction d’adresse n’est présent.
- Impact : un acteur du réseau local peut intercepter le mot de passe/MFA sur un réseau compromis, remplacer ou supprimer la session Garmin, déclencher des synchronisations et lire les données retournées par les API Pace. Le caractère mono-utilisateur ne constitue pas un contrôle d’accès.
- Résolution : le backend n’est plus publié ; Nginx protège l’interface par authentification HTTP et injecte une clé interne vers un filtre Java avec comparaison en temps constant. Les écritures exigent en plus le marqueur d’interface, ce qui bloque les formulaires CSRF ordinaires. Le service écoute seulement sur `127.0.0.1` par défaut et la documentation impose VPN ou reverse proxy HTTPS avant accès téléphone. Tests du filtre : réussis. L’absence de TLS intégré reste documentée en P2.1.

### P1.2 — CAPTCHA/Cloudflare est transformé en erreur temporaire puis retenté

- Emplacements : `garmin-connector/src/pace_garmin/adaptateur.py:217-228`, `garmin-connector/src/pace_garmin/erreurs.py:49-52`, `backend/src/main/java/fr/pace/garmin/HttpGarminActivityClient.java:191-202`, `backend/src/main/java/fr/pace/garmin/synchronization/GarminSynchronizationService.java:115-135`.
- Preuve : Python retourne pour une protection Garmin un HTTP 503 avec type `DEFINITIVE`. Java classe néanmoins tout statut 5xx comme `TemporaryGarminConnectorException`, sans prioriser `DEFINITIVE`. Le service planifie alors cinq tentatives à 15 min, 1 h, 4 h, 12 h et 24 h.
- Impact : violation directe de la règle « aucun contournement, arrêt ferme » ; des requêtes répétées peuvent prolonger un blocage de compte ou de réseau.
- Résolution : Java priorise désormais `AUTHENTIFICATION`, puis `DEFINITIVE`/`PROTECTION`, avant 429/5xx. Les tests de contrat et de synchronisation prouvent qu’aucune prochaine tentative n’est planifiée.

### P1.3 — Docker Compose ne livre pas l’application web demandée

- Emplacement : `docker-compose.yml:1-83`.
- Preuve : Compose contient PostgreSQL, le connecteur et le backend, mais aucun service frontend et le backend ne sert pas `frontend/dist`.
- Impact : la commande Compose ne permet pas d’ouvrir l’interface depuis le téléphone ; le scénario NAS « une seule commande » reste impossible même si le connecteur est désactivé.
- Résolution : ajout d’une construction Vite multi-étape et de Nginx non privilégié, healthchecks et dépendances saines. Le défaut découvert en contre-revue — génération de configuration sur racine en lecture seule — a été corrigé par des `tmpfs` ciblés appartenant à l’UID/GID Nginx. Le démarrage réel reste non vérifié faute de démon Docker.

## Autres P1 corrigés pendant la revue

### Expiration et effacement du contexte MFA

Le correctif final injecte une horloge monotone, purge le contexte dès que `etat()` constate l’expiration et centralise l’effacement du mot de passe avant le retrait des références. L’abandon explicite est également couvert. Contre-vérification : pytest **12/12**, Ruff et mypy strict réussis.

### Sortie réseau du connecteur

L’état initial attachait le connecteur uniquement à `garmin-interne` avec `internal: true`, ce qui bloquait aussi Garmin. L’état final ajoute `sortie-garmin` sans publier le port du connecteur (`docker-compose.yml:32-34`, `docker-compose.yml:79-83`). `docker compose config --quiet` confirme la topologie. Le blocage fonctionnel est corrigé ; l’absence de liste blanche réellement appliquée reste en P2.3.

## P2 ouverts

### P2.1 — HTTPS dépend encore de l’infrastructure NAS

- Emplacements : `docker-compose.yml:90-121`, `.env.example:2`, `docs/architecture-backend.md`.
- Constat : la configuration sûre par défaut lie Nginx à `127.0.0.1`. L’accès depuis un téléphone exige donc un VPN ou un reverse proxy HTTPS externe ; Compose ne fournit pas lui-même le certificat/TLS.
- Impact : régler simplement `PACE_LISTEN_ADDRESS=0.0.0.0` rendrait Basic Auth et le mot de passe Garmin interceptables en HTTP sur le LAN.
- Correction recommandée : conserver la valeur locale par défaut et fournir une recette NAS HTTPS/VPN vérifiée. Refuser explicitement l’activation du connecteur personnel lorsque l’origine publique n’est pas HTTPS, sauf boucle locale.

### P2.2 — Le contrat OpenAPI statique est plus strict que l’exécution réelle

- Emplacements : `garmin-connector/openapi.yaml:135-147`, `garmin-connector/openapi.yaml:108-112`, `garmin-connector/src/pace_garmin/modeles.py:21-28`, `garmin-connector/src/pace_garmin/api.py:188-197`.
- Preuve : le contrat interdit les propriétés supplémentaires pour connexion/MFA et borne l’identifiant d’activité à 128 caractères. Les modèles Pydantic n’emploient pas `extra="forbid"` et les paramètres de chemin ne portent aucune borne. Les réponses automatiques 422 ne sont pas documentées.
- Impact : dérive possible entre client généré, tests et service ; entrées plus larges que le contrat annoncé.
- Correction recommandée : faire du fichier la source testée, aligner les contraintes à l’exécution et comparer automatiquement l’OpenAPI FastAPI générée au contrat canonique.

### P2.3 — La sortie Garmin est générale, pas limitée aux domaines Garmin

- Emplacements : `docker-compose.yml:32-34`, `docker-compose.yml:81-83`.
- Preuve : `sortie-garmin` est un réseau Docker ordinaire. Le commentaire reporte la liste blanche au pare-feu/mandataire du NAS, sans composant ni règle livrée. `python-garminconnect` limite ses domaines au niveau applicatif, mais un composant compromis disposerait d’un accès sortant général.
- Impact : exfiltration possible depuis le conteneur après compromission de la chaîne Python ; la stratégie SSRF/egress n’est pas reproductible avec Compose seul.
- Correction recommandée : fournir un mandataire sortant avec liste stricte des domaines/ports Garmin ou documenter et tester une règle NAS concrète. Interdire les redirections vers des hôtes hors liste.

### P2.4 — Les tailles de corps, réponses et archives FIT ne sont pas bornées

- Emplacements : `garmin-connector/src/pace_garmin/api.py:138-161`, `garmin-connector/src/pace_garmin/adaptateur.py:141-159`, `garmin-connector/src/pace_garmin/adaptateur.py:205-214`, `backend/src/main/resources/application.yml:15-17`.
- Preuve : aucune limite de corps HTTP n’est configurée ; le téléchargement est chargé en mémoire ; une archive ZIP est ouverte puis son premier FIT entièrement décompressé sans contrôler taille compressée/non compressée ni ratio.
- Impact : épuisement mémoire/CPU du connecteur (limité à 256 Mio) ou du backend avec une réponse Garmin anormale ; redémarrages en boucle possibles.
- Correction recommandée : plafonds au serveur et au client, lecture en flux, taille maximale FIT/ZIP, ratio de compression, nombre d’entrées et délai total. Tester dépassement et archive malveillante.

### P2.5 — Images non immuables et démarrage réel non vérifié

- Emplacements : `docker-compose.yml:21-69`, `backend/Dockerfile:8-14`.
- Preuve : healthchecks, conditions `service_healthy`, racines en lecture seule, capacités supprimées et limites de ressources sont maintenant présents. Les images de base restent des étiquettes mutables sans digest et aucun conteneur n’a pu être réellement démarré dans cet environnement.
- Impact : reproductibilité et compatibilité NAS non encore démontrées malgré une configuration Compose valide.
- Correction recommandée : maîtriser versions/digests et exécuter les tests de démarrage et de santé sur amd64/arm64.

### P2.6 — L’idempotence applicative reste vulnérable aux courses

- Emplacements : `backend/src/main/java/fr/pace/garmin/synchronization/GarminSynchronizationService.java:60-63`, `backend/src/main/java/fr/pace/garmin/synchronization/GarminSynchronizationService.java:72-88`, `backend/src/main/resources/db/migration/V2__synchronisation_garmin.sql:1-36`.
- Preuve : « chercher puis insérer » est réalisé sans verrou/upsert pour la clé d’idempotence et pour `(source, source_external_id)`. Les contraintes uniques protègent les données, mais une course remonte une erreur SQL au lieu de retourner l’objet existant.
- Impact : double clic ou deux reprises concurrentes peuvent produire une erreur utilisateur malgré l’absence finale de doublon.
- Correction recommandée : insertion atomique/upsert ou capture ciblée de violation unique avec relecture ; test concurrent PostgreSQL.

### P2.7 — L’audit automatisé des CVE et les constructions multi-architecture ne sont pas démontrés

- Emplacements : `garmin-connector/requirements.lock:1-33`, `garmin-connector/Dockerfile:1-21`, `backend/Dockerfile:1-14`.
- Preuve : les versions directes et transitives sont verrouillées et `garminconnect==0.3.11` est postérieur au correctif 0.3.5 de CVE-2026-54447. Cependant `pip-audit` n’est pas installé dans l’environnement, aucun rapport SBOM/scan d’image n’est livré et le démon Docker local est indisponible ; les images amd64/arm64 n’ont pas été construites.
- Impact : absence de preuve sur les vulnérabilités transitives et sur les roues natives (`curl_cffi`, `cryptography`, `uvloop`) pour les deux architectures NAS.
- Correction recommandée : ajouter en CI `pip-audit`, audit Maven/npm, SBOM et scan d’images ; construire et tester les deux plateformes avant livraison NAS.

### P2.8 — La couverture ne valide pas encore le système réel

- Emplacements : `garmin-connector/tests/`, `backend/src/test/`, `frontend/src/pages/SynchronisationPage.test.tsx`.
- Preuve : les tests unitaires sont verts, mais il n’existe pas de test PostgreSQL/Testcontainers pour V2, de test d’intégration Java↔FastAPI contre le contrat, de test Compose, de test de reprise après redémarrage, ni de test E2E du consentement→MFA→synchronisation→confirmation. Les tests d’accessibilité automatisés axe/clavier/lecteur d’écran restent absents.
- Impact : les P1.2/P1.3 et dérives de contrat échappent précisément aux suites actuelles.
- Correction recommandée : ajouter ces tests avant activation avec compte réel, toujours à partir de doubles et fixtures anonymisées.

## P3 ouverts

### P3.1 — La documentation réseau du connecteur est devenue ambiguë

- Emplacements : `garmin-connector/README.md:37-53`, `docker-compose.yml:32-34`.
- Constat : le README dit que le conteneur « doit être raccordé à un réseau Docker internal: true » sans expliquer le second réseau de sortie désormais nécessaire.
- Correction recommandée : décrire les deux interfaces et distinguer absence d’entrée publique et sortie HTTPS contrôlée.

### P3.2 — Le frontend sonde toutes les 1,5 seconde sans plafond temporel

- Emplacement : `frontend/src/pages/SynchronisationPage.tsx:52-57`.
- Impact : une synchronisation bloquée peut provoquer un sondage indéfini, de la consommation inutile et un état trompeur.
- Correction recommandée : arrêt après délai/plafond, reprise manuelle et pause lorsque la page est cachée ou hors ligne.

## Points conformes observés

- aucune méthode Garmin mutante n’est exposée dans le protocole de l’adaptateur ; connexion, lecture, FIT et suppression **locale** seulement ;
- session chiffrée par AES-256-GCM avec nonce aléatoire, données associées et clé de 32 octets exigée (`garmin-connector/src/pace_garmin/chiffrement.py`) ;
- clé de chiffrement et clé inter-service obligatoires, distinctes, absentes du dépôt et masquées par `SecretStr` ;
- service Python sans port publié, utilisateur non-root, racine en lecture seule, capacités supprimées et `no-new-privileges` ;
- délais de connexion/lecture Java définis ; limitation interne prudente et une seule tentative de bibliothèque par appel ;
- 401, 403, 429, CAPTCHA et Cloudflare sont identifiés côté Python sans mécanisme de contournement ;
- consentement explicite et avertissement français visibles avant connexion ; champs mot de passe et MFA correctement typés pour le navigateur ;
- le connecteur est désactivé par défaut ;
- le verrou inclut `garminconnect==0.3.11`, donc pas la version affectée par CVE-2026-54447 ;
- aucune valeur de mot de passe, MFA, jeton ou charge Garmin n’est volontairement journalisée ; journaux d’accès Uvicorn désactivés.

## Vérifications finales exécutées

- backend : `mvn test` — **17 tests réussis** après corrections ;
- frontend : `npm test -- --run` — **9 tests réussis** ; `npm run build` — **réussi** ;
- connecteur : `pytest` — **12 tests réussis** après correction MFA ; Ruff format/lint — **réussis** ; mypy strict — **réussi** ;
- infrastructure : `docker compose config --quiet` avec secrets factices — **réussi** ; syntaxe du script d’entrée Nginx — **réussie** ;
- hygiène : `git diff --check` — **réussi** ;
- `pip-audit` — **non exécuté**, outil absent ;
- constructions Docker amd64/arm64 — **non exécutées**, démon Docker local indisponible.
