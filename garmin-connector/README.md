# Connecteur Garmin personnel de Pace

Prototype local, expérimental et **strictement en lecture seule**. Il utilise les
interfaces privées et non documentées de Garmin Connect. Garmin peut les modifier,
limiter ou bloquer sans préavis. Son utilisation peut contrevenir aux conditions du
service et entraîner une suspension du compte.

Le composant ne contourne ni CAPTCHA, ni Cloudflare, ni protection de sécurité. Il
s'arrête sur 401, 403 ou 429. Il ne remplace pas l'API Garmin officielle.

## Ce que le service fait

- connexion éphémère, avec reprise MFA explicite ;
- session locale chiffrée par AES-256-GCM ;
- état de session, activités récentes, backfill par dates et détail ;
- téléchargement et extraction du FIT si la bibliothèque le permet réellement ;
- suppression de la session **locale** (aucune révocation distante prétendue).

Il n'expose aucune méthode Garmin d'écriture. Le contrat complet est dans
[`openapi.yaml`](openapi.yaml).

## Configuration locale

Python 3.12 ou 3.13 est requis. Copier `.env.example`, produire deux secrets distincts
et laisser `PACE_GARMIN_CONNECTEUR_ACTIF=false` jusqu'à l'acceptation des risques.

```bash
python3.12 -m venv .venv
.venv/bin/pip install -e '.[dev]'
.venv/bin/uvicorn pace_garmin.main:app --host 127.0.0.1 --port 8081
```

Le mot de passe et le code MFA traversent uniquement l'API interne pendant leur
échange. Ils ne sont ni journalisés ni persistés. L'API doit rester inaccessible au
téléphone et à Internet : seul le backend Java peut l'appeler avec `X-Cle-Service`.

## Docker et NAS

L'image de base officielle Python est multi-architecture `linux/amd64` et
`linux/arm64`. Le conteneur doit être raccordé à un réseau Docker `internal: true`,
sans section `ports`, avec un volume dédié `/donnees` et des secrets fournis par le
NAS. Durcissement recommandé dans Compose :

```yaml
read_only: true
cap_drop: [ALL]
security_opt: [no-new-privileges:true]
tmpfs: [/tmp]
pids_limit: 64
```

Le volume `/donnees` est nécessairement inscriptible. La clé AES-GCM ne doit jamais
être sauvegardée avec ce volume.

## Vérifications

```bash
.venv/bin/ruff format --check .
.venv/bin/ruff check .
.venv/bin/mypy src
.venv/bin/pytest
docker build --platform linux/amd64 .
docker build --platform linux/arm64 .
```

`pytest` charge uniquement des secrets factices depuis `tests/conftest.py`. Aucun
fichier `.env`, compte Garmin ou secret réel n'est nécessaire pour exécuter les
tests.

Les tests utilisent uniquement des doubles et des données anonymisées. Aucun test ne
se connecte à Garmin. Avant toute mise à jour, auditer les versions exactes et leurs
avis de sécurité. Les téléchargements de dépendances n'ont lieu qu'à la construction,
jamais au démarrage.

`requirements.lock` verrouille aussi les dépendances transitives utilisées dans
l'image. Il doit être régénéré et audité délibérément lors d'une mise à niveau.
