# Plan d'implémentation — phase 6, intégration IA

Date de préparation : 3 septembre 2026.

## Objectif de la tranche

Transformer le compte rendu factuel de la phase 5 en une interprétation de coaching
structurée, traçable et relançable, sans rendre l'activité dépendante de l'IA. La
génération de la prochaine séance reste la phase 7 ; la phase 6 prépare son port
Java et sa configuration, mais n'invente pas encore une séance planifiée.

## Décisions d'architecture

### Port métier

Créer dans un domaine `ai` :

- `AiProvider`, interface indépendante du fournisseur ;
- `ActivityAnalysisRequest`, composé uniquement du profil utile, du ressenti Garmin,
  du résumé FIT et des agrégats factuels ;
- `ActivityAnalysisResult`, résultat structuré validé ;
- `WorkoutGenerationRequest` et `WorkoutGenerationResult` comme contrats réservés à
  la phase 7, sans les appeler dans cette tranche ;
- `AiUnavailableException`, `AiTemporaryException` et `AiInvalidResponseException`.

Le domaine ne connaît ni URL OpenAI, ni clé API, ni nom de modèle.

### Adaptateur OpenAI

Implémenter `OpenAiProvider` dans l'infrastructure Java avec l'API Responses et une
sortie `json_schema` stricte. Le mode JSON historique ne suffit pas : le JSON reçu
doit respecter le schéma avant conversion en objet métier.

L'appel doit utiliser :

- `store: false` côté fournisseur ;
- un délai de connexion et un délai global courts et configurables ;
- aucun outil distant, recherche web ou accès à des fichiers ;
- une taille d'entrée bornée ;
- une sortie bornée ;
- une clé transmise uniquement dans l'en-tête d'autorisation ;
- un identifiant de corrélation non sensible ;
- au maximum deux reprises automatiques pour `429`, `408` et `5xx`, avec délai
  progressif et prise en compte de `Retry-After` ;
- aucune reprise automatique pour `400`, `401`, `403` ou réponse structurellement
  invalide.

Le modèle d'analyse reste une propriété de configuration. Au moment de cette
préparation, `gpt-5.6-luna` est le candidat économique, mais sa valeur ne doit jamais
être codée dans le domaine et doit pouvoir être changée par environnement ou depuis
les réglages.

## Contrat de sortie du compte rendu

Le schéma strict doit produire :

- `resume` : texte court ;
- `interpretations[]` : titre, texte, niveau de confiance et faits sources ;
- `pointsPositifs[]` ;
- `pointsVigilance[]` ;
- `recuperation` : recommandation prudente et justification ;
- `hypotheses[]` : hypothèses explicitement nommées ;
- `donneesManquantes[]` ;
- `impactProchaineSeance` : constat préparatoire, sans créer la séance elle-même ;
- `avertissementSante` uniquement lorsqu'une douleur ou une donnée préoccupante est
  mentionnée, sans diagnostic.

Chaque interprétation référence les identifiants des faits fournis dans la requête.
Le backend rejette les références inconnues, les niveaux de confiance hors contrat,
les champs trop longs et toute réponse contenant une section obligatoire absente.

## Données envoyées

Envoyer seulement :

- le profil sportif utile et les valeurs manquantes pertinentes ;
- l'objectif principal lorsqu'il existe ;
- le sport, la date, la durée, la distance, le relief et les mesures résumées ;
- le RPE et le ressenti avec leur provenance ;
- les tours agrégés ;
- les pourcentages de zones ;
- les évolutions et niveaux de confiance calculés en phase 5 ;
- les limites et incohérences détectées localement.

Ne jamais envoyer le prénom, le courriel Garmin, les identifiants techniques, le
fichier FIT, la trace GPS ni les milliers d'échantillons bruts. Les contraintes ou
blessures du profil ne sont envoyées qu'après validation de leur nécessité et sont
traitées comme texte non fiable, jamais comme instructions.

## Persistance

Ajouter une migration Flyway avec :

### `ai_configuration`

- fournisseur actif ;
- URL de base ;
- modèles d'analyse et de planification ;
- température et longueur maximale ;
- instructions personnalisées bornées ;
- état et date du dernier test ;
- aucune clé API en clair.

En V1 locale, la clé est fournie par `PACE_AI_API_KEY`. L'interface indique seulement
si elle est configurée. L'enregistrement chiffré depuis l'interface sera une tranche
distincte s'il devient nécessaire.

### `ai_call`

- identifiant, activité et type d'opération ;
- fournisseur, modèle, version du prompt et du schéma ;
- entrée JSON réduite réellement envoyée ;
- réponse JSON validée ;
- identifiant de réponse du fournisseur ;
- durée et compteurs de jetons lorsqu'ils existent ;
- statut `EN_ATTENTE`, `EN_COURS`, `REUSSIE`, `ERREUR_TEMPORAIRE` ou
  `ERREUR_DEFINITIVE` ;
- nombre de tentatives, erreur contrôlée et prochaine tentative ;
- dates de création et de mise à jour.

Ajouter une unicité empêchant deux analyses actives pour la même activité et la même
version de prompt. Une régénération crée une nouvelle version et conserve
l'historique.

## API Pace

- `GET /api/v1/reglages/ia` : configuration non sensible et état du dernier test ;
- `PUT /api/v1/reglages/ia` : paramètres bornés, jamais la clé en réponse ;
- `POST /api/v1/reglages/ia/test` : appel minimal sans donnée sportive ;
- `POST /api/v1/sorties/{id}/analyse` : lancer ou reprendre l'analyse ;
- `GET /api/v1/sorties/{id}/analyse` : dernier résultat et statut ;
- `POST /api/v1/sorties/{id}/analyse/regeneration` : nouvelle version explicite.

Les écritures conservent les protections API et anti-CSRF existantes. Une erreur IA
ne modifie ni l'activité, ni son FIT, ni son compte rendu factuel.

## Interface

Dans Réglages :

- fournisseur et URL ;
- modèle d'analyse et futur modèle de planification ;
- température, longueur maximale et instructions du coach ;
- indicateur « clé configurée » sans jamais afficher sa valeur ;
- bouton de test et résultat daté.

Dans le détail d'une sortie :

- conserver le compte rendu factuel visible en permanence ;
- ajouter une section « Interprétation du coach » avec les états attente, génération,
  succès, erreur temporaire et erreur définitive ;
- séparer interprétations, hypothèses et recommandations ;
- afficher la confiance de chaque interprétation ;
- proposer « Réessayer » ou « Régénérer » explicitement ;
- ne jamais présenter le texte IA comme une mesure Garmin.

## Ordre de réalisation

1. Contrats `AiProvider`, requête réduite et résultat structuré.
2. Migration et journal d'appels idempotent.
3. Adaptateur OpenAI Responses avec faux serveur HTTP dans les tests.
4. Validation JSON stricte et défenses contre les instructions contenues dans les
   données utilisateur.
5. Service asynchrone, reprises et reprise après redémarrage.
6. Routes d'analyse et de réglages.
7. Interface de réglages et section d'interprétation.
8. Tests d'intégration PostgreSQL, contrats HTTP et parcours frontend.
9. Test manuel facultatif avec une clé de test et des données anonymisées.

## Critères de validation

- l'application démarre et les sorties restent lisibles sans clé IA ;
- aucune requête réseau n'est effectuée quand l'IA est désactivée ;
- une réponse non conforme au schéma est rejetée et journalisée sans être affichée ;
- un `429` ou `5xx` est relançable après redémarrage ;
- une régénération ne détruit pas l'analyse précédente ;
- la clé n'apparaît ni en base, ni dans les logs, ni dans une réponse API ;
- les entrées persistées ne contiennent ni FIT brut, ni série temporelle complète,
  ni donnée d'authentification Garmin ;
- chaque affirmation IA distingue fait source, interprétation, confiance et
  hypothèse ;
- aucun diagnostic médical n'est généré ;
- tests backend, frontend, migration PostgreSQL et build de production réussis.

## Décisions nécessaires avant l'implémentation

Deux choix seulement doivent être confirmés :

1. autoriser l'envoi à OpenAI des agrégats sportifs et du ressenti Garmin ;
2. choisir si la clé reste exclusivement dans `.env` pour la V1, option recommandée,
   ou si elle doit être enregistrable depuis l'interface avec chiffrement local.
