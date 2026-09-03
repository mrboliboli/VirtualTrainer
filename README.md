# Pace — entraîneur de course personnel

Pace est une application locale, mobile-first, destinée à récupérer les activités
Garmin, conserver leurs fichiers FIT et construire progressivement un coaching
adaptatif. Le projet comprend un backend Java/Spring Boot, un frontend React, une
base PostgreSQL et un connecteur Garmin personnel expérimental.

## État actuel

Le socle, le profil, les objectifs, la synchronisation Garmin, le décodage FIT et la
consultation détaillée des sorties sont disponibles. La suite prévue est décrite
dans [`docs/etat-projet.md`](docs/etat-projet.md).

## Documentation utile

- architecture et sécurité : [`docs/architecture-backend.md`](docs/architecture-backend.md) ;
- contrat produit : [`docs/cahier-des-charges-enrichi.md`](docs/cahier-des-charges-enrichi.md) ;
- connecteur Garmin expérimental :
  [`garmin-connector/README.md`](garmin-connector/README.md) ;
- décodage FIT et licence du SDK Garmin :
  [`docs/decodage-fit.md`](docs/decodage-fit.md) ;
- revue de la tranche FIT : [`docs/revue-phase-fit.md`](docs/revue-phase-fit.md).

Le SDK FIT officiel de Garmin est soumis à sa licence propriétaire, et non à une
licence libre généraliste. Toute distribution de Pace à un tiers exige une nouvelle
validation de ses conditions de licence.

## Démarrage local

Copier `.env.example` vers `.env`, fournir les secrets demandés, puis vérifier la
configuration avant le démarrage :

```bash
docker compose config --quiet
docker compose up --build
```

Le connecteur Garmin reste désactivé par défaut. Ne l'activer qu'après acceptation
explicite de ses risques et avec un accès au frontal protégé par HTTPS ou VPN.
