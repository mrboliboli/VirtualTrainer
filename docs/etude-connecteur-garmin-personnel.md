# Étude des connecteurs Garmin personnels non officiels

Vérification réalisée le **21 août 2026**. Cette étude repose uniquement sur les dépôts des projets, leurs registres de paquets officiels et la documentation Garmin. Elle ne constitue ni une autorisation d'utilisation, ni une validation des conditions de Garmin.

## Résumé de la recommandation

Ne pas intégrer directement un protocole Garmin privé dans le backend Java. Si l'utilisateur autorise explicitement la voie non officielle, le meilleur compromis actuel est un **petit service auxiliaire Python isolé**, fondé sur `garminconnect` de cyberjunky, avec une API interne très étroite correspondant à `GarminActivityClient`.

Ce service resterait facultatif et remplaçable par l'API officielle. Il ne devrait être ajouté qu'après validation explicite des risques : rupture sans préavis, limitation ou blocage du compte ou de l'adresse IP, traitement des identifiants Garmin, dépendance à des mécanismes d'authentification privés et possibilité de non-conformité aux conditions du service.

`garth` ne doit pas être retenu : son auteur l'a déclaré obsolète le 28 mars 2026, les nouvelles connexions ne fonctionnent plus. Les serveurs MCP examinés sont utiles pour une expérimentation assistée par un modèle, mais MCP ajoute ici une couche inutile entre deux services déterministes; ce n'est pas le bon contrat de production pour Pace.

## Comparaison

| Option | État au 21 août 2026 | Activités et FIT | Authentification | Licence | Adéquation à Pace |
|---|---|---|---|---|---|
| `cyberjunky/python-garminconnect` | 0.3.11 publiée sur PyPI le 19 août 2026; activité soutenue | liste, plages de dates, détail, tours/zones et téléchargement original FIT/ZIP | MFA en deux étapes, jetons persistants et rafraîchissement; mot de passe nécessaire seulement à la connexion initiale si les jetons restent valides | MIT | meilleure base non officielle, à isoler dans un service auxiliaire |
| `matin/garth` | 0.8.0 finale du 28 mars 2026, explicitement abandonnée | client générique, mais nouvelles connexions cassées | anciens jetons OAuth1 potentiellement valides environ un an; nouveau login indisponible | MIT | à exclure |
| client Java privé | aucune option Java actuelle, maintenue et suffisamment documentée identifiée dans Maven Central ou les dépôts examinés | variable | réimplémentation fragile du SSO privé | variable | à exclure; coût et risque trop élevés |
| `ndeloof/go-garmin` | projet très récent, seulement 10 commits lors de la vérification | API fortement typée, recherche/détails/splits/zones, téléchargement original | MFA en deux phases, jetons DI et rotation | Apache-2.0 | prometteur, mais maturité insuffisante pour devenir la dépendance principale |
| MCP TypeScript `etweisberg` | projet actif, 38 étoiles et publication npm automatisée lors de la vérification | détails et téléchargement FIT | navigateur Playwright, cookies et jeton CSRF; session de quelques heures | AGPL-3.0 | lourd, sessions courtes, Chromium et licence contraignante |
| MCP Python `nbaradar` | seulement 2 commits et aucune version lors de la vérification | surtout données de santé et activités en lecture | réutilise les jetons de `python-garminconnect` | licence non clairement affichée dans le dépôt consulté | démonstrateur crédible, pas une base de production |

## `python-garminconnect` de cyberjunky

### Capacités utiles

Le paquet expose la recherche paginée et par dates, le résumé et le détail d'activité, les splits, zones cardiaques et de puissance, ainsi que le téléchargement original via `download-service/files/activity/{id}`. Le fichier original est généralement une archive contenant le FIT. Un backfill applicatif est donc possible en paginant ou en interrogeant une plage de dates, puis en dédupliquant sur l'identifiant Garmin; il n'existe pas de webhook ni de push garanti dans cette API privée.

La synchronisation Pace devrait rester un **polling modéré** : à la demande après « Entraînement effectué », puis éventuellement une vérification périodique espacée. Le projet traite les réponses 429 comme des limites de fréquence et recommande de ne pas multiplier les reconnexions. Le backend doit conserver son propre curseur, ses tentatives et son délai progressif.

### MFA, sessions et secrets

La bibliothèque sait suspendre une connexion lorsqu'un code MFA est requis, puis la reprendre. Cela convient à un parcours en deux appels dans l'interface. Elle persiste des jetons DI et rafraîchit la session; le mot de passe ne devrait pas être stocké après l'obtention des jetons.

Un avis de sécurité de sévérité élevée publié le 4 juin 2026 concerne les versions jusqu'à 0.3.4 : le fichier de jetons pouvait être lisible par d'autres utilisateurs locaux. La correction est annoncée en 0.3.5. Pace devrait néanmoins monter un volume dédié, chiffrer le contenu avec la clé maîtresse de l'application et imposer des permissions minimales. La version doit être verrouillée par empreinte et rester au moins égale à 0.3.5.

La version 0.3.0 avait introduit plusieurs stratégies imitant des navigateurs et `curl_cffi` pour composer avec Cloudflare. Cela confirme la fragilité et le caractère non contractuel de la solution. Aucun CAPTCHA ni protection interactive ne doit être contourné; si Garmin en présente un, la synchronisation doit s'arrêter et demander une action explicite.

### Déploiement NAS

Le paquet demande Python 3.12 ou plus et publie un paquet Python indépendant de la plateforme. Sa dépendance `curl_cffi` fournit des roues Linux glibc et musl pour x86-64 et aarch64, ce qui rend techniquement possibles des images `amd64` et `arm64`. Il faudra tout de même construire et tester les deux architectures en intégration continue; la seule présence des roues ne garantit pas tous les NAS.

Architecture proposée, sans l'implémenter à ce stade :

```text
Backend Java -> HTTP privé authentifié -> service auxiliaire Garmin Python -> Garmin Connect privé
                                         |
                                         +-> volume chiffré de jetons
Backend Java -> SDK FIT Java officiel -> stockage et métriques normalisées
```

Le service auxiliaire ne devrait exposer que : début/reprise MFA, état de session, activités récentes ou par plage, détail, téléchargement original, déconnexion et suppression des jetons. Il ne doit pas exposer les quelque 130 méthodes de la bibliothèque. L'accès HTTP doit rester sur le réseau interne Docker, protégé par un secret de service, avec limites de taille et délais courts.

## `garth`

`garth` a longtemps fourni l'authentification SSO Garmin et a inspiré plusieurs ports. Son dépôt et PyPI indiquent désormais sans ambiguïté qu'il est abandonné : Garmin a changé le parcours d'authentification mobile, les nouvelles connexions ne fonctionnent plus et seuls d'anciens jetons OAuth1 peuvent survivre jusqu'à leur expiration. L'utiliser ou le bifurquer reviendrait à reprendre la maintenance d'un protocole que son auteur a cessé de suivre.

## Options Java et décodage FIT

Aucun client Java non officiel actuel ne présente la combinaison nécessaire de maintenance récente, MFA, rafraîchissement de jetons, activité détaillée et téléchargement FIT. Réécrire le SSO privé en Java dupliquerait la partie la plus fragile de `python-garminconnect` sans apporter de valeur métier.

Le décodage, en revanche, doit rester en Java avec le **SDK FIT Java officiel Garmin** (`com.garmin:fit`). Garmin le publie dans Maven Central, documente le décodage des fichiers Activity et indique Java 8 ou supérieur, donc Java 21 est compatible. Le SDK traite le format FIT, pas l'accès à Garmin Connect : il ne réduit pas les risques du connecteur privé.

## Serveurs MCP Garmin

Plusieurs serveurs existent, mais aucun ne justifie MCP dans le chemin de données de Pace :

- `etweisberg/garmin-connect-mcp` est le plus structuré parmi ceux examinés et sait télécharger le FIT. Il exécute les appels depuis Chromium/Playwright pour reproduire une véritable empreinte TLS, conserve cookies et CSRF et annonce une expiration de session après quelques heures. Cela alourdit fortement une image NAS et revient explicitement à contourner la détection automatisée de Cloudflare. Sa licence AGPL-3.0 exige en outre une analyse juridique avant distribution d'un service modifié.
- `nbaradar/garmin-mcp` sépare correctement la connexion interactive et le service, sait fonctionner en HTTP authentifié et reconnaît explicitement les risques. Mais il repose déjà sur `python-garminconnect`, ne comptait que deux commits, aucune version et aucun écosystème lors de la vérification.
- `ndeloof/go-garmin` fournit un serveur MCP et un client Go très complet avec Docker, MFA sérialisable et jetons renouvelés. Son code est prometteur, sous Apache-2.0, mais le dépôt ne comptait que dix commits et aucun signal suffisant de stabilité à long terme.

Pour Pace, une API interne HTTP typée est plus simple, testable et déterministe que MCP. MCP ne devrait être envisagé que comme outil local d'exploration, jamais comme autorité de synchronisation.

## Risques et garde-fous obligatoires

- utilisation d'endpoints privés non documentés et modification possible sans préavis;
- limites de fréquence inconnues, erreurs 403/429 et blocage possible des adresses de centre de données;
- évolution de Cloudflare et rupture déjà observée en mars et juin 2026;
- risque contractuel vis-à-vis des conditions Garmin, à accepter explicitement par l'utilisateur;
- accès à des données de santé et à une session donnant un contrôle important du compte;
- dépendance envers un mainteneur principal unique pour `garminconnect`;
- aucune reconnexion en boucle, aucun CAPTCHA contourné, aucun mot de passe conservé après la session;
- chiffrement des jetons, journaux expurgés, révocation locale immédiate et possibilité de supprimer le service auxiliaire;
- tests uniquement sur fixtures anonymisées; les essais réels doivent rester manuels et rares.

## Décision proposée

1. Demander d'abord l'accès officiel Garmin et conserver cette voie comme cible.
2. Si l'accès officiel est refusé et si l'utilisateur accepte explicitement les risques, réaliser un prototype limité du service auxiliaire `python-garminconnect`.
3. Valider seulement : connexion MFA, restauration après redémarrage, dernière activité, détail et FIT, sur `amd64` et `arm64`.
4. Ne poursuivre que si le prototype fonctionne sans mot de passe persistant, sans CAPTCHA, avec une fréquence basse et une révocation vérifiable.

## Sources primaires

- [`cyberjunky/python-garminconnect`](https://github.com/cyberjunky/python-garminconnect)
- [`garminconnect` sur PyPI](https://pypi.org/project/garminconnect/)
- [Avis GHSA-wjhr-76vg-2hvc](https://github.com/cyberjunky/python-garminconnect/security/advisories/GHSA-wjhr-76vg-2hvc)
- [`matin/garth`](https://github.com/matin/garth) et [`garth` sur PyPI](https://pypi.org/project/garth/)
- [SDK FIT Java officiel Garmin](https://github.com/garmin/fit-java-sdk)
- [`curl-cffi` sur PyPI](https://pypi.org/project/curl-cffi/)
- [`etweisberg/garmin-connect-mcp`](https://github.com/etweisberg/garmin-connect-mcp)
- [`nbaradar/garmin-mcp`](https://github.com/nbaradar/garmin-mcp)
- [`ndeloof/go-garmin`](https://github.com/ndeloof/go-garmin)
- [API Activity officielle Garmin](https://developer.garmin.com/gc-developer-program/activity-api/)
