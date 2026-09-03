# État du projet

Dernière mise à jour : 3 septembre 2026.

## Tranches réalisées

1. Socle Docker, backend Java, frontend React et PostgreSQL.
2. Profil et objectifs.
3. Connexion et synchronisation Garmin par connecteur personnel expérimental.
4. Récupération, conservation et décodage automatique des fichiers FIT.
5. Consultation factuelle initiale : liste des sorties, détail, métriques, tours,
   zones, série temporelle et sorties récentes sur l'accueil.

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

Achever le compte rendu factuel local avant l'intégration IA :

- importer et conserver le RPE et le ressenti Garmin avec leur provenance ;
- calculer les indicateurs objectifs à partir des données FIT normalisées ;
- distinguer données mesurées, valeurs calculées et données absentes ;
- afficher un compte rendu utile même sans IA ;
- permettre une correction facultative du ressenti sans l'ajouter au parcours
  principal.

L'intégration IA vient ensuite et reçoit des agrégats validés, jamais la série FIT
brute complète.

## Vérification de gel

- backend : `mvn -q test` ;
- frontend : `npm test -- --run` puis `npm run build` ;
- connecteur : `.venv/bin/pytest -q` ;
- aucun appel à un compte Garmin réel pendant les tests.
