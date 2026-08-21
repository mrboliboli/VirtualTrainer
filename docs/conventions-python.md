# Conventions Python du connecteur Garmin personnel

Ces conventions transposent au service Python les règles pertinentes du skill
`virtualdiapo-java-developer`. Les règles JavaFX et desktop ne s'appliquent pas.

## Principes

- Préférer du code simple, explicite, prévisible et ciblé.
- Donner une responsabilité courte à chaque module et chaque classe.
- Ne pas créer d'abstraction sans besoin concret et ne pas refactoriser hors sujet.
- Injecter les dépendances par constructeur. Les dépendances et configurations sont
  immuables autant que possible (`dataclass(frozen=True)`).
- Utiliser des types explicites sur les interfaces publiques. `Any` reste cantonné
  à la frontière de la bibliothèque Garmin non typée.
- Utiliser Pydantic pour les contrats HTTP et des dataclasses pour les objets
  internes simples. Éviter les modèles mutables et les constructeurs ambigus.
- Créer une exception dédiée par erreur fonctionnelle significative.
- Préférer une garde explicite à une expression compacte lorsqu'elle est plus lisible.
- Fermer fichiers, répertoires temporaires et réponses réseau avec un gestionnaire de
  contexte. Toute ressource créée doit avoir un cycle de vie visible.

## Réseau, sécurité et journaux

- Définir des délais, limiter la fréquence et ne jamais relancer automatiquement une
  authentification. Une seule opération Garmin est exécutée à la fois.
- Arrêter fermement sur CAPTCHA, Cloudflare, 401, 403 ou 429. Ne jamais contourner
  une protection ni simuler un succès.
- Utiliser des journaux paramétrés, normalement en français. Ne jamais journaliser
  courriel, mot de passe, code MFA, jeton, en-tête d'authentification, FIT ou donnée
  de santé détaillée.
- Conserver seulement un identifiant de corrélation, l'opération, sa durée et son
  résultat technique.
- Le mot de passe et le code MFA sont éphémères. Seule la session Garmin est
  persistée, dans une enveloppe AES-256-GCM authentifiée.
- L'API écoute par défaut uniquement sur `127.0.0.1`. En Docker, elle n'est joignable
  que sur un réseau interne et son port n'est jamais publié.

## Tests et vérifications

- Écrire des tests pytest nommés
  `test_methode_devrait_resultat_quand_condition` et structurés avec les commentaires
  `# ÉTANT DONNÉ`, `# QUAND`, `# ALORS`.
- Employer des doublures injectées et des données anonymisées. Aucun test automatique
  ne contacte un vrai compte Garmin.
- Après chaque changement important : lancer Ruff (format et lint), mypy, pytest,
  la construction Docker si disponible, puis inspecter `git diff --check` et le diff.
- Verrouiller les dépendances et ne jamais les télécharger au démarrage du NAS.

