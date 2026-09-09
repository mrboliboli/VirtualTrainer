# État du projet

Dernière mise à jour : 9 septembre 2026.

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
6. Analyses IA structurées, relançables et consultables sans rendre les activités
   dépendantes du fournisseur IA.
7. Génération manuelle ou automatique d'une prochaine séance, avec validation des
   jours disponibles, de la durée maximale et des zones cardiaques du profil.
8. Cycle de vie des propositions : acceptation, refus, régénération, historique,
   rapprochement prudent avec une sortie Garmin et comparaison prévu/réalisé sur
   l'accueil. Les analyses récentes alimentent les suggestions suivantes.

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

La phase 8 est fonctionnellement terminée. La prochaine tranche doit être définie
avant développement ; aucun périmètre de phase 9 n'est encore approuvé.

## Vérification de gel

- backend : `mvn -q test` ;
- frontend : `npm test -- --run` puis `npm run build` ;
- connecteur : `.venv/bin/pytest -q` ;
- aucun appel à un compte Garmin réel pendant les tests.
