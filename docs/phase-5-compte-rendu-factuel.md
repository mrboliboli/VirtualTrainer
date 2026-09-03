# Phase 5 — activités et compte rendu factuel

Date : 3 septembre 2026.

## Parcours

Après confirmation d'une activité, Pace conserve automatiquement le RPE et le score
de ressenti présents dans le détail Garmin. Aucune nouvelle saisie n'est requise.
Une activité dépourvue de ressenti reste entièrement consultable et analysable.

Garmin peut fournir `perceivedExertion` sur une échelle de 0 à 100 ; Pace la
normalise alors sur 10. Une valeur déjà comprise entre 0 et 10 est conservée. Le
champ `activityFeel` est affiché comme un score Garmin sur 100 : aucun libellé
subjectif n'est inventé sans contrat source stable.

Une nouvelle confirmation d'une activité déjà connue actualise son détail et son
ressenti sans télécharger ni dupliquer à nouveau le FIT.

## Calculs locaux

Le détail d'une sortie expose, sans appel IA :

- l'allure moyenne calculée depuis la distance et la durée active ;
- les moyennes de fréquence cardiaque, puissance et cadence dans chaque moitié,
  ainsi que leur évolution relative ;
- le coefficient de variation de la puissance ;
- le pourcentage du temps enregistré dans chaque zone ;
- un niveau de confiance fondé sur le nombre de relevés utilisables ;
- la liste explicite des calculs impossibles lorsque des données manquent.

Les moitiés suivent l'ordre chronologique des échantillons FIT. Les calculs
d'évolution exigent au moins 20 valeurs valides par moitié. La régularité de
puissance exige au moins 20 valeurs. Ces résultats restent des valeurs calculées,
visuellement séparées des mesures Garmin et de toute future interprétation IA.

## Limites assumées

- aucun diagnostic médical ni recommandation de coaching n'est produit ;
- les pauses et une fréquence d'échantillonnage irrégulière peuvent limiter la
  portée d'une comparaison par moitié ;
- le relief n'est pas encore utilisé pour interpréter une évolution ;
- la comparaison avec la séance prévue attend le modèle des séances planifiées ;
- une correction locale facultative du ressenti pourra être ajoutée plus tard sans
  devenir une étape du parcours principal.
