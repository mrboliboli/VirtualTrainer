# Plan d'implémentation — phase 8, suivi des séances

## Objectif

Faire passer la prochaine séance d'une simple génération IA à une proposition
contrôlée par l'utilisateur, puis relier prudemment la séance acceptée à l'activité
Garmin réalisée.

## Périmètre

- générer une proposition sans remplacer silencieusement la séance acceptée ;
- accepter, refuser ou régénérer explicitement une proposition ;
- respecter la durée maximale et les jours disponibles du profil ;
- conserver les versions précédentes ;
- afficher les étapes détaillées et l'état de la proposition ;
- rechercher après import une séance planifiée compatible par date et sport ;
- exposer la comparaison prévu/réalisé sans déclarer automatiquement une
  correspondance ambiguë.

## Restes repris de la phase 7

- intégrer davantage les analyses récentes à la génération ;
- ajouter les tests HTTP, PostgreSQL et frontend dédiés ;
- distinguer clairement proposition, acceptation et remplacement ;
- valider réellement les disponibilités plutôt que de les confier au seul modèle.

## Critères de validation

- aucun appel IA au chargement de l'accueil ;
- aucune séance acceptée remplacée sans confirmation ;
- historique conservé après refus ou régénération ;
- correspondance Garmin automatique uniquement lorsqu'elle est non ambiguë ;
- absence de correspondance sans effet destructif ;
- tests backend et frontend, migration PostgreSQL et build réussis.
