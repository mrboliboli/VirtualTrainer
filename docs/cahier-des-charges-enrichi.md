# Cahier des charges enrichi — Pace

Ce document reproduit le cahier des charges fourni et y ajoute les exigences normatives de langue, de terminologie et d’organisation du travail. En cas d’ambiguïté, les sections normatives ci-dessous complètent les exigences d’origine sans en réduire la portée.

# 0. Langue et terminologie — exigence normative

Le français est la langue principale de toute l’application.

Utilise autant que possible des termes français naturels et compréhensibles pour :

- les textes de l’interface ;
- les boutons, menus, titres et messages ;
- les états et notifications ;
- les messages d’erreur ;
- les comptes rendus de coaching ;
- la documentation destinée à l’utilisateur ;
- les journaux et commentaires utiles.

Évite les anglicismes lorsqu’un équivalent français courant et précis existe.

Un terme anglais reste acceptable uniquement lorsqu’il constitue le terme technique ou sportif couramment employé, lorsque sa traduction serait artificielle ou ambiguë, ou lorsqu’il est imposé par une technologie, un protocole, une API ou un nom de produit. Exemples acceptables dans leur contexte : Garmin Connect, FIT, API REST, OpenAPI, webhook, Docker Compose, backend, frontend et mobile-first.

Dans l’interface utilisateur, privilégie notamment :

- « tableau de bord » ou « accueil » plutôt que « dashboard » ;
- « compte rendu » plutôt que « reporting » ;
- « synchronisation » plutôt que « sync » ;
- « réglages » plutôt que « settings » ;
- « état de récupération » plutôt que « readiness » ;
- « allure » plutôt que « pace », sauf lorsque « Pace » désigne le nom du produit ;
- « en cours de chargement » plutôt que « loading » ;
- « réessayer » plutôt que « retry » ;
- « enregistrer » plutôt que « save ».

Les noms imposés par le code, les bibliothèques, les formats et les protocoles peuvent rester en anglais. Le code source peut employer les conventions techniques anglaises habituelles lorsqu’elles améliorent sa cohérence et sa maintenabilité. Les textes visibles et les messages métier restent en français.

Maintiens une terminologie française cohérente dans toute l’application. Ne mélange pas arbitrairement français et anglais dans un même parcours. Le ton éditorial tutoie l’utilisateur, emploie des phrases courtes et une voix active, sans culpabilisation ni promesse médicale.

La référence détaillée des parcours, composants, textes, couleurs, adaptations aux écrans et critères d’accessibilité est `docs/conception-experience.md`. Elle est normative pour l’interface de la V1.

# 0 bis. Organisation multi-agent — exigence normative

Le travail est organisé autour de rôles aux responsabilités explicites :

1. **Orchestrateur** : coordonne le plan, distribue les responsabilités, arbitre les interfaces entre lots, suit les blocages externes, rassemble les résultats et ne remplace pas les spécialistes dans leurs périmètres réservés.
2. **Développeur Java** : possède le backend Spring Boot, Maven, PostgreSQL, Flyway, la synchronisation, les calculs métier, le décodage FIT et les tests associés. Il lit intégralement et applique le skill Java obligatoire mentionné à la section 1 avant toute intervention backend.
3. **Spécialiste de conception d’expérience** : possède les parcours, l’architecture de l’information, la direction artistique, le système visuel, le vocabulaire, l’adaptation à partir de 320 px et les exigences WCAG 2.2 AA. Ses décisions sont consignées dans `docs/conception-experience.md`.
4. **Spécialiste frontend** : possède l’interface React, TypeScript et Vite, les composants, l’intégration de l’API, les tests d’interface et le parcours de bout en bout. Il applique la référence de conception sans créer de second système visuel concurrent.
5. **Relecteur indépendant** : ne produit pas la première implémentation. Il challenge le code et l’architecture, recherche les blocages, régressions, mauvaises pratiques, défauts d’accessibilité et vulnérabilités, puis classe ses constats par gravité avec des preuves reproductibles.

Les frontières sont respectées pour éviter les modifications concurrentes. Chaque spécialiste documente ses hypothèses et vérifications. Le relecteur intervient après l’intégration des lots significatifs, et ses constats bloquants ou importants sont corrigés ou explicitement acceptés avant validation.

---

Je veux que tu conçoives et développes intégralement une application web locale de coaching running personnalisé. Je n’ai encore créé aucun projet : pars du dossier courant et initialise tout ce qui est nécessaire.

Le nom provisoire du produit est « Pace ». Il doit rester facile à changer.

# 1. Skill Java obligatoire

Pour tout le backend Java, commence par lire intégralement puis applique le skill suivant :

`/Users/fabien/Documents/Workspace/VirtualDiapo/.agents/skills/virtualdiapo-java-developer/SKILL.md`

Ajoute explicitement son utilisation au plan de travail.

Applique toutes ses conventions pertinentes au backend Spring Boot, notamment :

- code simple, explicite et lisible ;
- classes avec une responsabilité claire ;
- dépendances immuables et injection par constructeur ;
- aucune injection de champ ;
- exceptions métier dédiées ;
- usages raisonnés de Lombok et MapStruct ;
- logs SLF4J paramétrés, généralement en français ;
- fermeture déterministe des ressources ;
- gestion robuste des erreurs réseau ;
- JUnit 5, Mockito et AssertJ ;
- tests structurés avec `// GIVEN`, `// WHEN` et `// THEN` ;
- noms de tests selon `methodUnderTest_shouldExpectedBehavior_whenCondition` ;
- `@DisplayName` en français ;
- compilation, tests et inspection des différences après chaque changement important.

Ignore uniquement les règles propres à JavaFX, au desktop et à VirtualDiapo lorsqu’elles ne concernent pas cette application web.

En cas de conflit :

1. les exigences fonctionnelles de ce prompt sont prioritaires ;
2. le skill gouverne le style et la qualité du code Java ;
3. une architecture existante et fonctionnelle doit être préservée.

# 2. Vision du produit

L’application doit automatiser cette boucle :

1. Je renseigne mon profil et mon objectif sportif.
2. L’application prépare ma prochaine séance.
3. Je réalise la séance avec ma montre Garmin.
4. La montre envoie normalement l’activité à Garmin Connect.
5. J’ouvre l’application et clique sur « Entraînement effectué ».
6. L’application recherche automatiquement la nouvelle activité dans Garmin.
7. Je confirme la bonne activité si nécessaire.
8. Le backend récupère automatiquement ses données détaillées ou son FIT.
9. L’application analyse la séance et produit un compte rendu de coaching.
10. Elle adapte et propose la séance suivante.
11. Je peux effectuer, modifier, reporter, remplacer ou annuler cette séance.

La récupération automatique depuis Garmin constitue la valeur principale du produit.

L’utilisateur ne doit pas avoir à :

- exporter une activité depuis Garmin Connect ;
- télécharger un FIT ;
- importer manuellement un fichier ;
- effectuer des captures d’écran.

Il ne doit exister aucun écran d’import manuel dans le parcours utilisateur de la V1.

# 3. Cible d’hébergement

L’application est personnelle et locale.

Elle doit :

- fonctionner sur mon NAS avec Docker Compose ;
- être utilisable depuis un téléphone sur mon réseau local ;
- conserver durablement toutes les données ;
- ne dépendre d’aucun cloud en dehors de Garmin et du fournisseur d’IA configuré ;
- fonctionner après un redémarrage du NAS ;
- être facilement sauvegardable et restaurable ;
- supporter autant que possible les NAS `amd64` et `arm64`.

Ne déploie pas l’application sur un hébergeur public.

Une URL publique limitée pourra être nécessaire pour un callback ou webhook Garmin. Dans ce cas, prévois une configuration sécurisée derrière un reverse proxy HTTPS, sans rendre obligatoirement toute l’interface publique.

# 4. Choix techniques

Utilise :

- Java 21 ;
- Spring Boot pour le backend ;
- Maven ;
- React avec TypeScript et Vite pour le frontend ;
- PostgreSQL ;
- Flyway pour les migrations ;
- Docker Compose ;
- API REST versionnée sous `/api/v1` ;
- OpenAPI/Swagger ;
- JUnit 5, Mockito, AssertJ et Testcontainers ;
- une solution raisonnable de tests frontend et end-to-end.

Crée un monorepo clair, par exemple :

```text
/
  backend/
  frontend/
  docker/
  docs/
  docker-compose.yml
  .env.example
  README.md
```

Tu peux améliorer cette structure si tu as une raison concrète.

L’application est mono-utilisateur en V1. Ne construis pas un système complexe d’inscription ou de gestion d’équipes. Prépare seulement le modèle pour qu’une évolution multi-utilisateur reste possible.

# 5. Étude Garmin obligatoire avant l’intégration

Avant de coder le connecteur Garmin, réalise un spike technique court à partir de la documentation officielle actuelle de Garmin.

Vérifie et documente :

- les conditions d’accès à Garmin Activity API ;
- la procédure d’approbation ;
- les identifiants nécessaires ;
- le mécanisme d’autorisation et de consentement ;
- les architectures Push et Ping/Pull ;
- la récupération des activités détaillées ;
- la disponibilité des fichiers FIT ;
- le backfill des activités récentes ;
- la révocation de l’accès ;
- les limites et obligations d’attribution ;
- la nécessité éventuelle d’une URL HTTPS publique ;
- la compatibilité avec une application personnelle hébergée sur un NAS ;
- les possibilités de test avant l’approbation de production.

Pour les capacités officielles, utilise uniquement la documentation Garmin comme source de vérité, notamment :

`https://developer.garmin.com/gc-developer-program/activity-api/`

Ne prétends jamais que l’intégration officielle fonctionne si elle requiert une approbation ou des identifiants que je ne possède pas.

Si une décision de ma part est indispensable entre API officielle et connecteur personnel non officiel, arrête-toi après le spike, présente-moi clairement les conséquences et pose-moi une seule question précise.

Ne remplace jamais silencieusement la synchronisation Garmin par un import manuel.

# 6. Architecture du connecteur Garmin

Définis une abstraction métier, par exemple :

```java
public interface ActivitySource {
    // contrat indépendant de Garmin
}
```

et une abstraction d’infrastructure dédiée à Garmin, par exemple :

```java
public interface GarminActivityClient {
    // connexion, synchronisation et récupération
}
```

Le connecteur doit couvrir :

- connexion et consentement ;
- vérification de l’état de connexion ;
- récupération des activités récentes ;
- synchronisation incrémentale ;
- récupération d’une activité détaillée ;
- récupération automatique du FIT lorsqu’il est disponible ;
- backfill initial ;
- renouvellement ou restauration de session ;
- déconnexion ;
- révocation ;
- gestion des erreurs temporaires et définitives.

Les détails Garmin ne doivent pas contaminer le domaine métier.

Prévois une implémentation officielle Garmin Activity API comme cible principale.

# 7. Connecteur non officiel éventuel

Si l’API officielle n’est pas accessible à un projet personnel, ne passe pas automatiquement à une solution non officielle.

Présente-moi d’abord :

- les risques ;
- les limites ;
- les implications de sécurité ;
- la fragilité potentielle ;
- les conditions d’utilisation ;
- les solutions techniques actuelles raisonnables.

Si j’autorise explicitement un connecteur personnel non officiel :

- isole-le entièrement dans un module remplaçable ;
- ne contourne jamais un CAPTCHA ou une protection de sécurité ;
- gère la double authentification par un parcours utilisateur explicite ;
- ne stocke pas le mot de passe si une session renouvelable suffit ;
- chiffre les jetons et sessions au repos ;
- n’écris aucun secret dans les logs ;
- limite la fréquence des requêtes ;
- gère les expirations et indisponibilités proprement ;
- vérifie la maintenance, la licence et la sécurité de toute dépendance ;
- conserve la possibilité de le remplacer par l’API officielle.

Le backend principal reste en Java. Si une dépendance non Java ou un conteneur auxiliaire est véritablement indispensable, explique-le et demande mon accord avant de l’introduire.

# 8. Synchronisation automatique

La synchronisation doit être :

- automatique ;
- incrémentale ;
- idempotente ;
- relançable ;
- résistante aux redémarrages ;
- observable ;
- protégée contre les doublons.

Conserve notamment :

- l’identifiant Garmin de l’activité ;
- l’identifiant de la source ;
- la date de découverte ;
- la date de dernière synchronisation ;
- le curseur de reprise ;
- le statut de traitement ;
- l’empreinte du FIT ;
- le nombre de tentatives ;
- l’erreur éventuelle ;
- la prochaine tentative.

Utilise des reprises avec délai progressif pour les erreurs temporaires.

Prévois les statuts suivants :

- découverte ;
- téléchargement ;
- décodage ;
- prête à analyser ;
- analysée ;
- erreur temporaire ;
- erreur définitive.

Un même événement Garmin ou un même FIT reçu plusieurs fois ne doit créer qu’une seule activité.

# 9. Parcours « Entraînement effectué »

Le bouton principal doit déclencher ce parcours :

1. Afficher « Recherche de ta dernière activité Garmin… ».
2. Interroger Garmin.
3. Rechercher les activités compatibles avec la séance.
4. Présenter l’activité trouvée :
   - date et heure ;
   - sport ;
   - distance ;
   - durée ;
   - tracé simplifié si disponible.
5. Demander confirmation si nécessaire.
6. Télécharger automatiquement les détails ou le FIT.
7. Décoder et enregistrer l’activité.
8. Recueillir mon ressenti.
9. Générer le compte rendu.
10. Générer la séance suivante.

L’association entre activité et séance doit tenir compte de :

- la date ;
- l’heure ;
- le type de sport ;
- la durée ;
- la distance ;
- la cohérence avec la séance prévue.

Si une activité correspond avec un niveau de confiance élevé, proposer sa confirmation immédiatement.

Si plusieurs activités sont possibles, afficher une liste compacte.

Ne jamais effectuer une association silencieuse avec une confiance insuffisante.

# 10. Gestion des erreurs Garmin

Afficher des messages simples pour les situations suivantes :

- la montre n’a pas encore synchronisé ;
- aucune activité récente n’a été trouvée ;
- plusieurs activités sont candidates ;
- la connexion Garmin a expiré ;
- Garmin est temporairement indisponible ;
- l’activité existe mais ses détails ne sont pas encore prêts ;
- le téléchargement doit être retenté ;
- le FIT est incomplet ou invalide.

Exemple :

> Ta course n’est pas encore arrivée dans Garmin Connect. Vérifie la synchronisation de ta montre, puis réessaie.

Ne montre jamais directement une erreur technique ou une stack trace à l’utilisateur.

L’échec de l’IA ne doit jamais entraîner la perte d’une activité Garmin déjà récupérée.

# 11. Décodage FIT interne

Même sans import manuel, le backend doit savoir décoder les fichiers FIT récupérés automatiquement auprès de Garmin.

Utilise une bibliothèque Java fiable. Vérifie sa maintenance, sa licence et sa compatibilité avec Java 21. Explique le choix dans le README.

Extrais autant que possible :

- date et heure ;
- sport et sous-sport ;
- distance ;
- durée écoulée ;
- durée active ;
- allure moyenne ;
- vitesse moyenne et maximale ;
- fréquence cardiaque moyenne et maximale ;
- série temporelle de fréquence cardiaque ;
- zones cardiaques et leurs limites ;
- cadence moyenne et maximale ;
- puissance moyenne, maximale et normalisée ;
- zones de puissance ;
- seuil de puissance ;
- calories ;
- dénivelé positif et négatif ;
- altitude ;
- tours, laps et splits ;
- longueur de foulée ;
- oscillation verticale ;
- ratio vertical ;
- temps de contact au sol ;
- équilibre gauche/droite lorsqu’il existe ;
- Training Effect aérobie et anaérobie ;
- charge d’entraînement ;
- RPE et ressenti Garmin lorsqu’ils existent ;
- température ;
- respiration ;
- toute autre métrique pertinente réellement disponible.

Conserve :

- le fichier FIT original récupéré automatiquement ;
- les métriques normalisées ;
- les laps ;
- les zones ;
- les séries temporelles utiles.

N’invente jamais une métrique absente.

Un outil d’import FIT réservé aux tests ou au support peut exister hors de l’interface principale, mais il doit être désactivé en production et ne doit jamais devenir le parcours normal.

# 12. Profil du coureur

Ajoute une page Profil avec :

- prénom ;
- année de naissance facultative ;
- taille et poids facultatifs ;
- FC maximale ;
- FC au repos ;
- seuil cardiaque ;
- seuil de puissance ;
- zones personnalisées ;
- volume hebdomadaire habituel ;
- jours disponibles ;
- durée maximale par séance ;
- types de terrain accessibles ;
- contraintes ou blessures ;
- préférences d’entraînement ;
- commentaire libre.

Toute recommandation utilisant une donnée manquante doit le signaler.

Ne formule jamais de diagnostic médical.

# 13. Objectifs sportifs

Permets d’ajouter, modifier, archiver et supprimer un objectif.

Champs :

- nom ;
- date ;
- distance ;
- unité ;
- type : running, trail, route, 5 km, 10 km, semi-marathon, marathon ou autre ;
- URL officielle ou lien d’inscription ;
- priorité ;
- objectif de temps facultatif ;
- dénivelé cible facultatif ;
- notes ;
- statut : prévu, actif, atteint ou abandonné.

Permets de choisir un objectif principal.

Affiche le nombre de jours restants.

L’URL doit être cliquable depuis la fiche de l’objectif.

# 14. Séances planifiées

Une séance doit contenir :

- titre ;
- date prévue ;
- type ;
- objectif visé ;
- durée ou distance ;
- intensité ;
- cible d’allure, de FC ou de puissance ;
- échauffement ;
- corps de séance ;
- récupérations ;
- retour au calme ;
- explication du coach ;
- statut.

Statuts :

- proposée ;
- planifiée ;
- effectuée ;
- reportée ;
- annulée ;
- remplacée.

Depuis la tuile de la prochaine séance, je dois pouvoir :

- cliquer sur « Entraînement effectué » ;
- lancer la recherche Garmin ;
- reporter la séance ;
- modifier la séance ;
- indiquer qu’une séance différente a été réalisée ;
- annuler la séance.

Un report doit conserver :

- la date initiale ;
- la nouvelle date ;
- la date du report ;
- une raison facultative ;
- l’historique des reports successifs.

# 15. Ressenti après la course

Avant de générer l’analyse, permets de saisir rapidement :

- ressenti global ;
- RPE sur 10 ;
- douleur éventuelle ;
- qualité du sommeil ;
- météo ressentie ;
- hydratation ;
- commentaire libre.

La saisie doit être rapide sur téléphone et facultative, à l’exception du ressenti global et du RPE si cela reste ergonomique.

# 16. Compte rendu de coaching

Après chaque activité, crée un compte rendu structuré :

1. résumé ;
2. faits mesurés ;
3. laps et splits ;
4. évolution de la fréquence cardiaque ;
5. temps dans les zones ;
6. puissance ;
7. relief ;
8. dynamique de course ;
9. comparaison avec la séance prévue ;
10. interprétation du coach ;
11. points positifs ;
12. points de vigilance ;
13. récupération recommandée ;
14. conséquences sur la prochaine séance.

Sépare visuellement et sémantiquement :

- les données mesurées ;
- les valeurs calculées ;
- les interprétations ;
- les recommandations.

Chaque interprétation doit avoir un niveau de confiance :

- élevé ;
- moyen ;
- faible.

Toute hypothèse doit être présentée comme une hypothèse.

Ne produis aucun diagnostic médical.

# 17. Calculs locaux avant l’IA

Le backend doit calculer lui-même les métriques objectives au lieu de demander au modèle de les calculer.

Prévois notamment :

- allure ;
- vitesse ;
- temps par tour ;
- pourcentage dans chaque zone ;
- comparaison prévu/réalisé ;
- régularité de puissance ;
- évolution de la fréquence cardiaque ;
- dérive cardiaque avec limites de confiance ;
- évolution de la cadence ;
- évolution de la longueur de foulée ;
- évolution du temps de contact au sol ;
- évolution du ratio vertical ;
- différence entre première et seconde partie ;
- détection des données absentes ou incohérentes.

Le relief doit être pris en compte avant de conclure à une dégradation de performance.

# 18. Intelligence artificielle configurable

Crée une abstraction Java `AiProvider` afin que le domaine ne dépende pas d’un fournisseur ou d’un modèle précis.

Implémente initialement un fournisseur compatible avec l’API OpenAI.

Les réglages suivants doivent être configurables :

- fournisseur ;
- URL de l’API ;
- clé API ;
- modèle d’analyse d’activité ;
- modèle de génération de séance ;
- température ;
- longueur maximale de réponse ;
- instructions personnalisées du coach.

Ne code aucun nom de modèle en dur dans le domaine.

Prévois une valeur par défaut économique, mais laisse le modèle entièrement modifiable.

Utilise un format de sortie structuré et validé côté backend.

N’envoie pas les milliers de points FIT bruts au modèle si des agrégats fiables suffisent.

Conserve pour chaque appel :

- fournisseur ;
- modèle ;
- date ;
- version du prompt ;
- type d’opération ;
- données structurées envoyées ;
- réponse structurée ;
- durée ;
- statut ;
- erreur éventuelle.

Ne conserve jamais la clé API dans ces traces.

L’échec d’un appel doit pouvoir être retenté.

L’activité et ses données doivent rester consultables sans IA.

# 19. Sécurité des secrets

Ne place aucun secret dans Git, dans les images Docker ou dans `docker-compose.yml`.

Ajoute `.env.example`.

La clé d’IA et les informations Garmin doivent être :

- fournies par variable d’environnement ou enregistrées de manière chiffrée ;
- masquées dans l’interface après enregistrement ;
- exclues des logs ;
- absentes des réponses API ordinaires.

Prévois une clé de chiffrement principale fournie par l’environnement du conteneur.

Valide toutes les entrées.

Configure CORS de manière restrictive.

Limite les tailles de payload.

Protège tous les accès aux fichiers.

Ne renvoie pas de détails techniques sensibles au frontend.

# 20. Page Réglages

Ajoute au minimum :

## Connexion Garmin

- statut : connecté, déconnecté, expiré ou erreur ;
- compte connecté sans donnée sensible ;
- mode : officiel ou connecteur personnel autorisé ;
- dernière synchronisation ;
- dernière activité récupérée ;
- bouton « Connecter » ;
- bouton « Synchroniser maintenant » ;
- bouton « Reconnecter » ;
- bouton « Déconnecter » ;
- historique simplifié des synchronisations.

## Intelligence artificielle

- fournisseur ;
- URL ;
- modèle d’analyse ;
- modèle de planification ;
- paramètres ;
- bouton de test ;
- état du dernier test.

## Application

- fuseau horaire ;
- unités ;
- emplacement logique des données ;
- informations de version.

# 21. Tableau de bord

La page principale doit afficher en priorité :

- une salutation ;
- l’objectif principal ;
- le nombre de jours avant l’objectif ;
- la prochaine séance ;
- « Entraînement effectué » ;
- « Reporter » ;
- l’état de récupération ou la recommandation actuelle ;
- les dernières sorties ;
- l’accès aux comptes rendus ;
- la progression hebdomadaire ;
- les alertes réellement utiles.

La prochaine action doit être immédiatement identifiable.

# 22. Navigation mobile

Prévois une navigation inférieure :

- Accueil ;
- Sorties ;
- Synchroniser ou Ajouter ;
- Objectifs ;
- Réglages.

Le bouton central ne doit pas ouvrir un import de fichier. Il doit lancer ou afficher la synchronisation Garmin.

Les zones tactiles doivent être confortables.

L’interface doit fonctionner correctement à partir de 320 px de largeur.

# 23. Direction visuelle

Crée une interface :

- sobre ;
- chaleureuse ;
- sportive ;
- mobile-first ;
- lisible ;
- rassurante ;
- différente d’un tableau de bord administratif.

Direction proposée :

- fond crème clair ;
- vert profond ;
- accent vert citron modéré ;
- cartes généreusement espacées ;
- typographie nette ;
- données hiérarchisées ;
- graphiques simples ;
- états vides utiles ;
- chargements soignés ;
- erreurs compréhensibles.

Ne crée pas de graphiques décoratifs présentant de fausses données.

Toute visualisation doit être fondée sur des données réelles.

# 24. Modèle de données minimal

Prévois au minimum :

- `AthleteProfile` ;
- `Goal` ;
- `PlannedWorkout` ;
- `WorkoutReschedule` ;
- `ActivitySourceConnection` ;
- `ActivitySynchronization` ;
- `Activity` ;
- `ActivityLap` ;
- `ActivitySample` ;
- `HeartRateZoneSummary` ;
- `PowerZoneSummary` ;
- `AthleteFeedback` ;
- `CoachingReport` ;
- `AiExecution` ;
- `ApplicationSetting`.

Ne crée pas une architecture générique excessive. Chaque table et abstraction doit correspondre à un besoin réel.

Ajoute uniquement les index justifiés par les requêtes effectivement utilisées.

# 25. API REST

Crée une API versionnée sous `/api/v1`.

Prévois notamment :

- profil ;
- objectifs ;
- séances ;
- report d’une séance ;
- connexion Garmin ;
- callback Garmin éventuel ;
- état de synchronisation ;
- synchronisation immédiate ;
- activités récentes ;
- détail d’une activité ;
- association activité/séance ;
- ressenti ;
- compte rendu ;
- régénération du compte rendu ;
- génération de la séance suivante ;
- paramètres IA ;
- test du fournisseur IA ;
- santé de l’application.

Documente l’API avec OpenAPI/Swagger.

Utilise des DTO distincts des entités de persistance.

# 26. Tests

Ajoute des tests utiles, dont :

- tests unitaires du domaine ;
- tests des calculs d’allure ;
- tests des zones ;
- tests de dérive cardiaque ;
- tests des laps ;
- tests du décodeur FIT ;
- tests de détection de doublons ;
- tests de synchronisation idempotente ;
- tests de reprise après erreur Garmin ;
- tests d’association activité/séance ;
- tests de report ;
- tests du fournisseur IA avec un faux serveur ;
- tests d’intégration PostgreSQL avec Testcontainers ;
- tests frontend ;
- au moins un parcours end-to-end représentatif.

Pour le connecteur Garmin, utilise des fixtures ou réponses enregistrées anonymisées dans les tests. Les tests automatisés ne doivent pas contacter mon vrai compte Garmin.

# 27. Fichier FIT réel de référence

Je fournirai un fichier FIT Garmin réel pour tester le décodeur. Il ne doit pas être ajouté à Git sans mon autorisation.

Les valeurs attendues sont approximativement :

- distance : 10,869 km ;
- durée : 1:10:20,7 ;
- allure : 6:28/km ;
- FC moyenne : 153 bpm ;
- FC maximale : 170 bpm ;
- puissance moyenne : 364 W ;
- puissance normalisée : 368 W ;
- dénivelé positif : 127 m ;
- dénivelé négatif : 131 m ;
- 11 tours ;
- Training Effect aérobie : 5,0.

Documente les écarts d’arrondi tolérés.

Ce fichier sert uniquement aux tests et au développement du décodeur. Il ne crée pas un parcours d’import manuel dans la V1.

# 28. Docker Compose et NAS

Le projet doit démarrer avec une seule commande Docker Compose.

Inclure :

- frontend ;
- backend Java ;
- PostgreSQL ;
- volumes persistants ;
- healthchecks ;
- réseau interne ;
- configuration par variables d’environnement ;
- politiques de redémarrage adaptées.

Si possible, sers le frontend par un serveur web léger ou via une architecture simple adaptée à la production locale.

Les images doivent viser `amd64` et `arm64`.

Documente :

- prérequis ;
- installation ;
- premier démarrage ;
- adresse d’accès ;
- accès depuis un téléphone ;
- configuration Garmin ;
- configuration de l’IA ;
- reverse proxy facultatif ;
- HTTPS facultatif ou nécessaire ;
- emplacement des volumes ;
- sauvegarde ;
- restauration ;
- mise à jour ;
- rollback ;
- consultation des logs ;
- renouvellement des connexions ;
- diagnostic des problèmes courants.

# 29. Critères d’acceptation

La V1 n’est validée que si ce scénario fonctionne :

1. démarrer l’application sur le NAS ;
2. ouvrir l’application depuis un téléphone ;
3. configurer le profil ;
4. créer un objectif ;
5. connecter Garmin ;
6. réaliser une course avec une montre Garmin ;
7. laisser la montre synchroniser avec Garmin Connect ;
8. cliquer sur « Entraînement effectué » ;
9. voir l’application trouver automatiquement la course ;
10. confirmer l’activité si nécessaire ;
11. voir ses métriques détaillées ;
12. saisir le ressenti ;
13. obtenir le compte rendu ;
14. voir apparaître la prochaine séance ;
15. reporter cette séance ;
16. retrouver toutes les données après un redémarrage des conteneurs.

Une démonstration reposant uniquement sur des données factices ou sur un import manuel ne valide pas la V1.

Si l’approbation officielle Garmin bloque réellement le scénario, livre tout ce qui peut être correctement construit et testé, puis indique précisément :

- ce qui est terminé ;
- ce qui a été testé ;
- ce qui attend Garmin ;
- les informations ou décisions dont tu as besoin de ma part.

# 30. Méthode de travail

Commence par :

1. lire intégralement le skill Java ;
2. inspecter le dossier courant ;
3. vérifier qu’il n’existe pas de travail utilisateur à préserver ;
4. étudier l’accès Garmin actuel ;
5. produire un plan court ;
6. proposer l’architecture ;
7. identifier immédiatement les vrais blocages externes.

Ensuite, travaille par tranches verticales :

1. socle Docker, backend, frontend et base ;
2. profil et objectifs ;
3. connexion et synchronisation Garmin ;
4. récupération et décodage FIT automatique ;
5. activités et compte rendu factuel ;
6. intégration IA ;
7. prochaine séance ;
8. report ;
9. interface mobile complète ;
10. tests et documentation NAS.

Ne te limite pas à une maquette.

N’utilise pas de boutons factices dans le résultat final.

Ne simule pas silencieusement Garmin ou l’IA en production.

Les faux services sont autorisés uniquement dans les tests et dans un mode de démonstration clairement identifié.

Prends des décisions raisonnables sans me demander confirmation pour chaque détail. Pose-moi une question uniquement lorsqu’une décision :

- bloque réellement l’implémentation ;
- exige des identifiants ou une approbation externe ;
- implique un connecteur Garmin non officiel ;
- modifie fortement le périmètre ;
- présente un risque de sécurité significatif.

À la fin de chaque tranche importante :

- compile ;
- exécute les tests pertinents ;
- inspecte les différences ;
- vérifie les parcours concernés ;
- signale les risques encore ouverts.

Le résultat attendu est une véritable application locale de coaching adaptatif, exécutable sur mon NAS, dont la valeur centrale est la récupération automatique des activités Garmin.
