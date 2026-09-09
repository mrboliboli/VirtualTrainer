# Plan d’implémentation — phase 9, gestion des séances planifiées

## Objectif

Permettre de gérer concrètement une séance acceptée depuis l’accueil, jusqu’à sa
réalisation ou son abandon, en conservant un historique explicite de chaque décision.

## Périmètre fonctionnel

- ajouter « Entraînement effectué » sur la tuile d’une séance planifiée ;
- lancer depuis cette action la recherche des activités Garmin à rapprocher ;
- permettre de choisir une activité lorsque le rapprochement automatique ne suffit pas ;
- permettre d’indiquer qu’une séance différente a été réalisée ;
- reporter une séance avec nouvelle date et raison facultative ;
- conserver la date initiale et l’historique des reports successifs ;
- modifier les informations utiles d’une séance planifiée ;
- annuler une séance avec confirmation ;
- présenter les états planifiée, réalisée, reportée et annulée en français clair ;
- relier la comparaison prévu/réalisé au compte rendu de la sortie concernée.

## Hors périmètre

- envoi ou programmation d’un entraînement dans Garmin ;
- plan d’entraînement complet sur plusieurs semaines ;
- modification d’une activité Garmin source ;
- diagnostic médical ou décision automatique en cas de douleur.

## Données et API

- ajouter un historique immuable des reports ;
- conserver date initiale, date courante, date du report et raison ;
- exposer les actions de report, modification, annulation et rapprochement manuel ;
- empêcher qu’une même activité réalise plusieurs séances ;
- refuser les transitions de statut incohérentes avec une erreur métier lisible.

## Interface

- garder la prochaine action immédiatement visible dans la tuile principale ;
- utiliser une fenêtre ou un panneau court pour reporter/modifier ;
- demander confirmation avant l’annulation ;
- proposer les activités Garmin candidates sans afficher celles déjà rapprochées ;
- conserver un parcours utilisable sur téléphone avec peu d’étapes.

## Critères de validation

- un report conserve toutes les dates précédentes ;
- une annulation ne supprime aucune donnée ;
- un rapprochement manuel est explicite et réversible avant confirmation ;
- aucune activité ne peut être liée deux fois ;
- le compte rendu affiche la comparaison avec la séance prévue ;
- les transitions sont couvertes par des tests métier, HTTP et frontend ;
- les migrations s’appliquent sur PostgreSQL ;
- les suites backend/frontend et le build restent verts.

## Ordre proposé

1. modèle de transitions et historique des reports ;
2. API de report, modification et annulation ;
3. parcours « Entraînement effectué » et rapprochement manuel ;
4. actions et états dans l’interface ;
5. comparaison dans le compte rendu ;
6. recette mobile et PostgreSQL.
