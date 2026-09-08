# État du projet

Dernière mise à jour : 3 septembre 2026.

## Tranches réalisées

1. Socle Docker, backend Java, frontend React et PostgreSQL.
2. Profil et objectifs.
3. Connexion et synchronisation Garmin par connecteur personnel expérimental.
4. Récupération, conservation et décodage automatique des fichiers FIT.
5. Activités et compte rendu factuel local : liste et détail des sorties, métriques,
   tours, zones, série temporelle, récupération du ressenti Garmin, allure calculée,
   évolution entre les deux moitiés, régularité de puissance et répartition des
   zones. Le rattrapage historique exclut les sorties déjà importées, présente deux
   candidates à la fois et avance automatiquement jusqu'à la date de départ.

La revue de la tranche FIT ne signale plus aucun défaut P0 ou P1. Les réserves P2
restent consignées dans `docs/revue-phase-fit.md`.

## Décision sur le ressenti

L'objectif de Pace est de rendre l'information accessible avec le moins d'étapes
possible. La prochaine extension de la tranche 5 récupérera donc en priorité le RPE
et le ressenti enregistrés dans Garmin lorsqu'ils existent. Pace ne bloquera jamais
le compte rendu pour demander une seconde saisie. Une saisie ou correction locale
restera facultative et secondaire lorsque la donnée Garmin manque ou doit être
corrigée.

## Prochaine tranche fonctionnelle

La prochaine tranche est l'intégration IA. Elle reçoit des agrégats validés, jamais
la série FIT brute complète. La comparaison prévu/réalisé, la prise en compte fine
du relief et les conséquences sur la séance suivante seront ajoutées lorsque les
séances planifiées seront disponibles.

## Vérification de gel

- backend : `mvn -q test` ;
- frontend : `npm test -- --run` puis `npm run build` ;
- connecteur : `.venv/bin/pytest -q` ;
- aucun appel à un compte Garmin réel pendant les tests.
