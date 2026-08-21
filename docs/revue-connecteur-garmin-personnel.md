# Contre-étude indépendante — connecteur Garmin personnel non officiel

Date de vérification : 21 août 2026  
Portée : analyse uniquement, aucune implémentation ni connexion à un compte Garmin.  
Sources : conditions Garmin et sources primaires des projets communautaires (dépôts, manifestes, avis de sécurité).

## Décision synthétique

**NO-GO par défaut.** Les [Conditions d’utilisation Garmin](https://www.garmin.com/en-XD/legal/terms-of-use/) interdisent l’accès, la copie ou l’extraction automatisés ou manuels par des moyens non expressément fournis par le Site et prévoient notamment la suspension ou la résiliation du compte. Les solutions étudiées utilisent précisément des points d’accès consommateurs privés et non documentés. Une licence MIT autorise l’usage du code ; elle n’accorde aucun droit sur le service Garmin et ne neutralise pas ses conditions.

**GO expérimental conditionnel seulement** si l’utilisateur :

1. autorise explicitement cette voie après avoir accepté le risque contractuel, le risque de blocage du compte et l’absence de garantie de continuité ;
2. accepte l’ajout d’un petit conteneur auxiliaire non-Java, puisque les clients actuels crédibles sont Python ou Node ;
3. utilise un compte dont la perte temporaire d’accès est acceptable ;
4. valide une architecture locale, lecture seule, isolée et désactivée par défaut ;
5. accepte qu’un CAPTCHA, un défi Cloudflare, une modification SSO ou un doute de révocation arrête le connecteur au lieu d’être contourné.

Même dans ce cas, l’API officielle reste la cible de production et le connecteur personnel doit rester remplaçable.

## Constats prioritaires

### P1 — Risque contractuel et de suspension

- Tous les MCP examinés déclarent être non officiels et employer les services privés Garmin Connect. [Garmin Open MCP](https://github.com/limited/garmin-connect-mcp) indique explicitement que les points d’accès consommateurs sont non documentés et peuvent cesser de fonctionner. Le [MCP de kgabryje](https://github.com/kgabryje/garmin-mcp) tient le même discours.
- Les conditions Garmin prohibent l’extraction automatisée par des moyens non fournis et les charges déraisonnables. Une utilisation personnelle et faible fréquence réduit l’impact technique, pas l’incertitude contractuelle.
- Impact : suspension du compte, arrêt brutal du parcours principal, maintenance urgente après changement Garmin.
- Garde-fou : consentement séparé, avertissement persistant dans les réglages, interrupteur d’arrêt global et aucune promesse de disponibilité.

### P1 — Les jetons persistants sont des secrets à fort impact

- [`python-garminconnect`](https://github.com/cyberjunky/python-garminconnect/blob/master/README.md) reçoit le mot de passe et le code MFA, puis conserve un couple accès/rafraîchissement. Sa propre documentation précise que certaines méthodes peuvent lire **ou modifier** le compte et que `logout()` supprime le fichier local sans révoquer le jeton côté Garmin.
- L’avis [GHSA-wjhr-76vg-2hvc / CVE-2026-54447](https://github.com/cyberjunky/python-garminconnect/security/advisories/GHSA-wjhr-76vg-2hvc) montre un précédent concret : jusqu’à la version 0.3.4, le jeton de rafraîchissement pouvait être créé en `0644`, donc lisible par d’autres utilisateurs locaux. Le correctif est en 0.3.5 ; la version actuelle déclarée par le projet est 0.3.11.
- Les permissions `0600` actuelles sont utiles mais **ne constituent pas un chiffrement au repos**. Une sauvegarde du volume, un administrateur NAS compromis ou un processus dans le même conteneur peut encore lire le jeton.
- Impact : accès durable aux données de santé, activités, informations du compte et éventuellement fonctions mutantes.
- Garde-fou : chiffrement applicatif par enveloppe AES-256-GCM avec clé maîtresse uniquement fournie par secret Docker/NAS, volume et sauvegardes chiffrés, déchiffrement uniquement en mémoire, rotation après soupçon de fuite, aucun jeton dans les journaux, erreurs, traces IA ou sauvegardes non chiffrées.

### P1 — Révocation distante non garantie

- Le client communautaire documente que sa déconnexion est locale. La suppression du jeton dans Pace ne prouve donc pas son invalidation chez Garmin.
- La [politique de confidentialité Garmin Connect](https://www.garmin.com/en-GB/privacy/connect/) permet de gérer/supprimer les données et consentements dans le centre de gestion, mais elle ne documente pas un mécanisme stable de révocation propre à ces sessions privées.
- Impact : un jeton copié peut rester exploitable après « Déconnecter ».
- Garde-fou : le bouton doit être nommé honnêtement (« Supprimer la session locale » si la révocation distante n’est pas vérifiable), supprimer atomiquement jeton déchiffré/cache, conseiller une action de sécurité Garmin et une reconnexion ; ne jamais afficher « accès révoqué » sans preuve serveur.

## Solutions communautaires et MCP observés

| Projet primaire | Technique et points positifs | Limites décisives | Avis pour Pace |
|---|---|---|---|
| [`python-garminconnect`](https://github.com/cyberjunky/python-garminconnect) | MIT, actif, MFA, jetons `0600`/dossier `0700`, liste de domaines Garmin autorisés, tests de permissions, détection 429/403 | API privée ; mot de passe transite dans le processus d’authentification ; méthodes mutantes présentes ; révocation distante absente ; précédent CVE élevé | Meilleure base technique repérée, mais uniquement derrière un adaptateur lecture seule audité et version `>=0.3.5` précisément verrouillée |
| [`garmin-mcp-unofficial`](https://github.com/davidmosiah/garmin-mcp) | MIT, Node local-first, auth et MFA locaux, ne conserve pas le mot de passe, détecte 429/Cloudflare, batterie de tests, surcharges de dépendances de sécurité | Exécution suggérée via `npx -y`, surface MCP/HTTP inutile pour le besoin interne, variables de mot de passe/MFA possibles en CI, jeton seulement protégé par permissions | Ne pas utiliser directement dans Pace ; éventuellement source de comparaison pour l’authentification et les erreurs |
| [`Garmin Open MCP`](https://github.com/limited/garmin-connect-mcp) | Auto-hébergé, MFA, API protégée par clé, cache local, permissions privées, lecture seule annoncée | Écoute HTTP par défaut sur `0.0.0.0`, clé statique, cache santé supplémentaire, exposition publique proposée pour GPT Actions | Surface et exposition excessives pour un connecteur interne ; no-go comme dépendance directe |
| [`kgabryje/garmin-mcp`](https://github.com/kgabryje/garmin-mcp) | Lecture seule annoncée, bearer obligatoire en HTTP, utilisateur non-root en conteneur, séparation authentification/service | Docker écoute `0.0.0.0`, volume de jetons persistants, maturité/maintenance à confirmer, couche MCP inutile | Ne pas intégrer directement ; certaines idées d’isolation sont réutilisables |
| [`JohanBellander/GarminMCP`](https://github.com/JohanBellander/GarminMCP) | MIT, séparation de l’authentification interactive et du service | Projet très peu éprouvé (aucune étoile/fork/release observés), dépend de la même API privée | No-go |
| [`garth`](https://github.com/matin/garth) | MIT, historiquement répandu | Officiellement déprécié : Garmin a modifié l’authentification et les nouvelles connexions ne fonctionnent plus | No-go absolu |

Un serveur MCP n’est pas nécessaire à la boucle métier Pace. Il augmente la surface (outils invocables, transport HTTP, clé MCP, cache secondaire et risque d’envoyer davantage de données à une IA). Un adaptateur minimal avec deux ou trois opérations strictement typées est préférable.

## Identifiants, MFA, CAPTCHA et défis anti-robot

- Le courriel, le mot de passe et le code MFA ne doivent être saisis que dans un processus interactif local éphémère. Ne jamais accepter le mot de passe par API REST, fichier `.env`, ligne de commande, journal, variable CI ou message adressé à une IA.
- Le mot de passe doit être libéré de la mémoire après échange ; il ne doit jamais être persisté. Seule une session renouvelable est conservée.
- La documentation primaire de [`garmin-mcp-unofficial`](https://github.com/davidmosiah/garmin-mcp/blob/main/docs/auth.md) confirme que les connexions sans navigateur peuvent recevoir HTTP 429, pages Cloudflare ou réponses inattendues. Elle recommande d’arrêter les nouvelles tentatives.
- Pace ne doit ni résoudre, ni externaliser, ni contourner un CAPTCHA/Cloudflare. Un tel événement devient une **erreur définitive nécessitant une action humaine**, avec ouverture manuelle de Garmin Connect et temporisation longue.
- Le code MFA est à usage unique : jamais enregistré, jamais inclus dans une trace de diagnostic, et le contexte MFA doit expirer rapidement et n’accepter qu’une soumission.

## Limitation de débit et robustesse

Aucune limite officielle des points d’accès privés n’est publiée : il est impossible de promettre un quota sûr. Politique prudente recommandée :

- une seule synchronisation active par compte ;
- synchronisation incrémentale avec curseur et cache local, jamais de balayage complet répétitif ;
- au plus une recherche déclenchée par l’utilisateur à la fois ; pas de boucle de sondage rapide ;
- respecter `Retry-After` ; sinon reprise exponentielle avec gigue (par exemple 15 min, 1 h, 4 h, 12 h, 24 h) ;
- circuit ouvert immédiat sur 401, 403, CAPTCHA/Cloudflare et après un 429 répété ;
- aucun nouvel essai d’authentification automatique avec mot de passe ;
- maximum quotidien configurable et interrupteur d’arrêt global ;
- idempotence par identifiant externe et empreinte FIT afin que toute reprise soit sans doublon.

## Chaîne logistique, versions et licences

- `python-garminconnect` et les MCP examinés annoncent généralement MIT. Il faut conserver les avis de licence dans l’image et le dossier de distribution.
- Le manifeste actuel de [`python-garminconnect`](https://raw.githubusercontent.com/cyberjunky/python-garminconnect/master/pyproject.toml) déclare notamment `curl_cffi`, `requests` et `ua-generator` avec des bornes minimales, pas un ensemble immuable à lui seul.
- Interdire `npx -y ...`, `pip install` ou `uv sync` au démarrage du NAS. Construire une image reproductible depuis un verrou vérifié, pinner la version et le condensat de l’image de base, produire une nomenclature logicielle, lancer `pip-audit`/analyse d’image et revoir les changements avant mise à jour.
- Exiger au minimum : projet activement maintenu, politique de sécurité, tests MFA/jetons/429, licence compatible, aucun téléchargement dynamique et délai de qualification avant chaque mise à niveau.
- Ne pas repartir de `garth`, officiellement abandonné à la suite d’une rupture Garmin : c’est une preuve de la fragilité structurelle de l’approche.

## Architecture d’isolation minimale si l’utilisateur autorise l’expérimentation

```text
Téléphone → interface Pace → backend Java
                              │ contrat interne minimal, lecture seule
                              ▼
                     conteneur garmin-personnel
                              │ HTTPS sortant exclusivement
                              ▼
                         domaines Garmin
```

Le conteneur auxiliaire doit :

- être dans un réseau Docker interne, sans port publié ni accès direct du téléphone, d’Internet ou du fournisseur d’IA ;
- recevoir uniquement des appels authentifiés du backend Java via secret inter-service rotatif, idéalement socket Unix ou mTLS interne ;
- fonctionner non-root, système de fichiers racine en lecture seule, capacités Linux supprimées, `no-new-privileges`, limites CPU/mémoire/processus et répertoire temporaire en `tmpfs` ;
- n’avoir en sortie que DNS contrôlé et HTTPS vers la liste stricte des domaines Garmin constatés ;
- exposer une liste blanche d’opérations de lecture : état, activités récentes, détail, téléchargement FIT et suppression locale de session ; aucune méthode d’écriture Garmin ;
- ne jamais recevoir la clé IA, la base PostgreSQL ou les autres secrets Pace ;
- conserver les jetons dans un volume distinct chiffré, non monté dans le backend général, et retourner seulement des DTO minimaux ;
- journaliser identifiant de corrélation, opération, durée, statut et compteur de tentative, sans paramètres santé détaillés ni secrets ;
- démarrer désactivé et rester remplaçable par l’implémentation officielle de `GarminActivityClient`.

L’exposition publique du NAS n’est pas nécessaire pour ce connecteur personnel. Si un accès distant à Pace est souhaité, il doit passer par VPN ; ne jamais publier le port du conteneur Garmin. Le reverse proxy public éventuel destiné à un futur callback officiel doit router uniquement ce callback, pas l’adaptateur personnel.

## Critères de GO expérimental

Tous les critères suivants sont obligatoires :

- décision utilisateur explicite et révocable ;
- validation juridique personnelle des conditions applicables au compte/pays ;
- version et dépendances verrouillées, audit de la version exacte, licence et avis conservés ;
- lecture seule vérifiée dans le code, tests contractuels avec fixtures anonymisées, aucun test automatique sur le vrai compte ;
- mot de passe et MFA exclusivement interactifs et éphémères ;
- chiffrement au repos vérifié, restauration de sauvegarde testée sans fuite ;
- limites/reprises/circuit breaker testés ;
- conteneur interne durci et aucune exposition publique ;
- déconnexion locale présentée honnêtement et procédure de compromission documentée ;
- mode officiel conservé comme cible et migration possible sans contaminer le domaine.

L’échec d’un seul de ces critères maintient le **NO-GO**.

## Conclusion indépendante

Il existe des briques communautaires techniquement capables de récupérer des activités et des FIT, et `python-garminconnect` est la base la plus sérieuse observée. Cela ne rend pas l’intégration fiable ni autorisée. Le précédent de jetons exposés, la révocation distante incertaine, les défis anti-robot et la dépréciation récente de `garth` confirment que le risque n’est pas théorique.

Pour Pace, la décision raisonnable est donc : attendre d’abord la réponse Garmin officielle ; en cas de refus, ne proposer qu’un prototype personnel opt-in, lecture seule, fortement isolé, sans MCP public et sans promesse de pérennité.
