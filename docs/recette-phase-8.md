# Recette manuelle — phase 8

## Préparation

1. Redémarrer le backend dans IntelliJ pour appliquer les migrations V10 et V11.
2. Laisser l'IA activée et vérifier que la clé OpenAI est détectée.
3. Vérifier qu'un objectif futur et au moins un jour disponible existent dans le profil.

## Réglages IA

1. Ouvrir « Réglages ».
2. Vérifier que les tuiles « Clé d'accès », « Fournisseur et modèles »,
   « Comportement du coach » et « Tester la configuration » ont la même largeur.
3. Activer puis désactiver « Générer automatiquement… », enregistrer et recharger
   la page : la valeur choisie doit être conservée.
4. Laisser l'option désactivée pour vérifier le parcours manuel par défaut.

## Proposition manuelle

1. Sur l'accueil, cliquer sur « Générer la prochaine séance » ou « Régénérer ».
2. Vérifier le badge « Proposition », la date, la durée, l'intensité, la
   justification et le déroulé détaillé.
3. Vérifier que la date correspond à l'un des jours disponibles du profil et que
   la durée ne dépasse pas sa limite.
4. Recharger la page : la proposition doit rester visible.

## Décision

1. Cliquer sur « Accepter » : le badge devient « Planifiée ».
2. Régénérer : une nouvelle proposition doit apparaître sans supprimer la séance
   précédemment planifiée.
3. Cliquer sur « Refuser » : la proposition disparaît et l'ancienne séance
   planifiée réapparaît.
4. Accepter une nouvelle proposition : elle remplace alors explicitement
   l'ancienne séance dans la tuile.

## Génération après import

1. Activer l'option automatique dans les réglages et enregistrer.
2. Importer une nouvelle course Garmin.
3. Revenir à l'accueil après quelques secondes : une proposition doit apparaître.
4. Réimporter une activité déjà connue ne doit pas créer une nouvelle proposition.

## Prévu et réalisé

1. Accepter une séance, puis importer une course située à plus ou moins un jour de
   sa date.
2. Une correspondance unique doit marquer la séance comme réalisée côté backend.
3. Deux candidates équidistantes ou une activité d'un autre sport ne doivent pas
   être associées automatiquement.
