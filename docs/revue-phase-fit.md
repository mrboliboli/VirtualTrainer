# Revue indépendante — phase FIT

Date de revue : 22 août 2026.

## Verdict

La tranche fournit un décodage FIT interne utile et rejouable, une persistance normalisée et une restitution factuelle. Les premiers défauts P1 détectés pendant la revue ont été transmis immédiatement au responsable des correctifs : téléchargement Java non borné, décompression ZIP non bornée, collections FIT non bornées, chargement inutile des binaires dans la liste, mauvais usage de l'horodatage de fin comme début et activités historiques sans FIT bloquées dans un état d'attente.

Les correctifs observés bornent désormais le téléchargement Java et les archives ZIP, bornent sessions/tours/mesures/zones, utilisent `start_time` avec repli explicite et distinguent les activités historiques dépourvues de FIT. La liste et le détail utilisent maintenant des requêtes JDBC ciblées qui ne sélectionnent jamais `original_fit`. Après contre-revue du gel et nouvelle exécution des tests, **aucun P0 ni P1 ne reste ouvert** dans cette tranche.

## Contrôles effectués

- SDK : SDK FIT Java officiel Garmin `21.205.0`, compatible Java 21 selon la compatibilité Java 8+ annoncée. La dépendance est maintenue et provient de Maven Central.
- Licence : licence FIT propriétaire correctement signalée dans `docs/decodage-fit.md`. Elle n'est pas assimilée à une licence libre ; toute distribution à un tiers demande une nouvelle validation juridique.
- Entrées hostiles : contrôle d'intégrité FIT, plafonds de taille et de nombre de messages ; ZIP limité en taille, nombre d'entrées, taille décompressée et ratio, avec lecture décompressée par blocs.
- Exactitude : les valeurs absentes restent nulles. Les distances sont en mètres, durées en secondes, vitesses en mètres par seconde, puissance en watts et altitude en mètres. Le début de session et de tour provient de `start_time`, pas du `timestamp` de fin.
- Persistance : original FIT conservé, résumé, tours, zones et série enregistrés séparément. Le remplacement est transactionnel et rejouable.
- Concurrence : sélection par lot avec `FOR UPDATE SKIP LOCKED`, puis remplacement et changement d'état dans la même transaction.
- API : liste et détail sont exposés en français derrière les protections générales de l'application. Les erreurs de décodage affichées proviennent d'un vocabulaire contrôlé, sans trace technique ni secret.

## Réserves

### P2 — Couverture PostgreSQL insuffisante

Les tests du traitement FIT exécutent la migration V3 sur H2 en mode PostgreSQL. Ils ne prouvent pas le comportement réel de PostgreSQL, notamment `SKIP LOCKED`, deux travailleurs concurrents, les types `BYTEA` et `TIMESTAMP WITH TIME ZONE`, le verrouillage ou le redémarrage en cours de traitement. Ajouter un test Testcontainers PostgreSQL avec migration Flyway réelle, concurrence et reprise après interruption.

### P2 — Décodeur insuffisamment éprouvé

La fixture synthétique couvre surtout une session et quelques champs, plus un contenu manifestement invalide. Il manque des tests de limites (taille, sessions, tours, mesures, zones), fichier tronqué mais structuré, plusieurs sessions, tours et zones, valeurs nulles, unités et valeurs extrêmes. Le fichier réel annoncé par l'utilisateur n'a pas été fourni et ne doit pas être ajouté au dépôt sans autorisation.

### P2 — Modèle plus large que l'extraction effective

Le schéma réserve des colonnes pour oscillation verticale, ratio vertical, temps de contact au sol, longueur de foulée, température/respiration moyennes et plusieurs métriques de tour, mais le décodeur ne les alimente pas encore. Le seuil de puissance, l'équilibre gauche/droite, le RPE et le ressenti Garmin ne sont pas modélisés. Cette absence reste honnête, mais la tranche ne couvre donc pas encore toute la liste « autant que possible » du cahier des charges.

### P2 — Transactions et volume d'écriture

Un verrou de ligne reste détenu pendant deux parcours du FIT et jusqu'à 200 000 insertions JDBC unitaires. L'atomicité est bonne, mais le coût et la durée de verrou peuvent devenir élevés sur NAS. Mesurer sur PostgreSQL, utiliser les écritures par lots et documenter le budget mémoire/temps avant activation avec de gros fichiers.

### P2 — Série temporelle partielle dans l'API

La base conserve jusqu'à 200 000 échantillons alors que le détail n'en retourne que 10 000. Le contrat annonce désormais `totalEchantillons` et `serieTronquee`, et l'interface distingue le nombre affiché du total enregistré. Une pagination restera préférable lorsque la série temporelle devra être réellement explorée.

### P2 — Cadence dépendante du sport

La cadence FIT ne doit pas être libellée systématiquement « pas/min » sans interprétation du sport et des champs FIT associés. Conserver une unité neutre dans l'interface tant que la conversion course/cyclisme et la cadence fractionnaire ne sont pas gérées.

### P2 — Documentation demandée dans le README

Le choix et la licence du SDK sont expliqués dans `docs/decodage-fit.md`, mais le cahier des charges demande explicitement une explication dans le README. Ajouter au minimum un lien visible depuis le README et résumer la restriction de distribution.

## Tests indépendants

- `backend`: `mvn -q test` — 47 tests réussis, aucun échec, après gel des correctifs.
- `garmin-connector`: `.venv/bin/pytest -q` avec configuration de test et connecteur actif — 24 tests réussis, aucun échec.
- `frontend`: 21 tests et construction de production réussis, selon la contre-vérification spécialisée.
- Aucun compte Garmin réel n'a été contacté.
- Aucun fichier FIT personnel n'a été ajouté au dépôt.
