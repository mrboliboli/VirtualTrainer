# Plan d'implémentation — phase 7, prochaine séance

## Objectif

Proposer une seule prochaine séance cohérente avec l'objectif futur, le profil,
les disponibilités et les activités récentes. La proposition reste modifiable et
n'est jamais envoyée vers Garmin automatiquement.

## Parcours utilisateur

1. Le tableau de bord affiche l'objectif retenu et l'état de la prochaine séance.
2. L'utilisateur lance explicitement la génération afin de maîtriser le coût IA.
3. Pace affiche la séance structurée : date, objectif, durée ou distance,
   intensité, étapes et justification.
4. L'utilisateur accepte, régénère ou refuse la proposition.
5. Une proposition acceptée devient la prochaine séance du tableau de bord.

## Données autorisées

- objectif futur retenu ;
- disponibilités, durée maximale, terrains et préférences du profil ;
- résumés factuels des dernières activités ;
- dernières interprétations IA validées, lorsqu'elles existent ;
- charge, ressenti et limites de données utiles.

Les fichiers FIT, traces GPS, identifiants techniques et données Garmin brutes ne
sont jamais envoyés au fournisseur IA.

## Garde-fous

- aucune génération sans objectif futur ni activation explicite de l'IA ;
- date proposée future et compatible avec les jours disponibles ;
- durée plafonnée par le profil ;
- intensité et étapes issues d'un vocabulaire borné ;
- pas de diagnostic médical ; en présence d'une douleur déclarée, recommandation
  prudente et possibilité de repos ;
- aucune modification silencieuse d'une séance déjà acceptée ;
- versionnement des propositions et conservation de l'historique ;
- génération manuelle, sans appel IA automatique au chargement de l'accueil.

## Réalisation

1. Renforcer les contrats `WorkoutGenerationRequest` et
   `WorkoutGenerationResult` avec validations métier.
2. Ajouter la persistance des séances proposées et acceptées.
3. Implémenter la sortie structurée OpenAI pour la génération de séance.
4. Construire une entrée réduite depuis le profil, l'objectif et l'historique.
5. Exposer les routes de lecture, génération, acceptation et refus.
6. Raccorder la carte « Prochaine séance » du tableau de bord.
7. Ajouter les tests métier, HTTP, PostgreSQL et frontend.

## Critères de validation

- aucune requête IA n'est faite sans clic utilisateur ;
- une réponse hors contraintes est rejetée ;
- une erreur IA n'efface aucune proposition précédente ;
- la séance acceptée réapparaît après redémarrage ;
- le tableau de bord fonctionne toujours lorsque l'IA est désactivée ;
- les tests et le build de production réussissent.
