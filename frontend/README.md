# Interface de Pace

Interface React, TypeScript et Vite de l’application locale Pace. Les textes visibles sont en français et la conception suit `docs/conception-experience.md`.

## Développement

Prérequis : Node.js 22 ou version LTS récente.

```bash
npm install
npm run dev
```

Vite transmet les appels `/api` au serveur local sur le port 8080. Pour viser une autre adresse :

```bash
VITE_API_URL=http://adresse:8080/api/v1 npm run dev
```

## Vérifications

```bash
npm run build
npm test
```

## Production locale

Le service `application-web` de Compose construit les fichiers statiques, relaie
`/api` vers le backend privé et demande une authentification HTTP (utilisateur
`pace`, mot de passe `PACE_ACCESS_PASSWORD`). Il est lié à `127.0.0.1` par défaut.
Un accès depuis un téléphone exige un VPN chiffré jusqu'au NAS ou un mandataire
inverse HTTPS avec certificat reconnu ; il ne faut pas publier directement le port
HTTP sur le réseau local lorsque le connecteur Garmin est actif.

## Contrat avec le serveur

Le client typé est centralisé dans `src/api.ts`. Les routes provisoires utilisées sont :

- `GET` et `PUT /api/v1/profil` ;
- `GET` et `POST /api/v1/objectifs` ;
- `POST /api/v1/objectifs/{id}/archivage` et `DELETE /api/v1/objectifs/{id}` ;

L’Accueil compose actuellement ses informations à partir du profil et des objectifs.

Le parcours Garmin personnel utilise uniquement l’API Java publique :

- `GET`, `POST` et `DELETE /api/v1/garmin/connexion` ;
- `POST /api/v1/garmin/connexion/mfa` pour la vérification en deux étapes ;
- `POST /api/v1/garmin/synchronisations` puis `GET /api/v1/garmin/synchronisations/{id}` ;
- `POST /api/v1/garmin/synchronisations/{id}/confirmation`.

L’interface ne contacte jamais directement le connecteur auxiliaire. La connexion personnelle est signalée comme expérimentale et exige un consentement explicite avant d’envoyer les identifiants au serveur Java.

Les objets attendus sont décrits dans `src/types.ts`. Ces noms de champs sont des hypothèses tant que le contrat OpenAPI du serveur n’est pas stabilisé. Aucun service fictif ne remplace le serveur en production : un serveur absent produit un état d’erreur explicite.
