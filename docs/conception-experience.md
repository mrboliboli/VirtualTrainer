# Pace — Référence de conception d’expérience

Version : 1.0  
Statut : normative pour la V1  
Portée : interface web, textes visibles, parcours et états

## 1. Intention

Pace est un compagnon de course personnel, calme et fiable. L’interface doit donner envie d’agir sans mettre l’utilisateur sous pression. Elle privilégie la prochaine décision utile — réaliser, synchroniser, confirmer, commenter ou adapter une séance — plutôt que l’accumulation de chiffres.

Principes directeurs :

1. **Une action principale par écran.** Elle est immédiatement repérable et formulée par un verbe.
2. **Les faits avant l’interprétation.** Données mesurées, calculs, interprétations et recommandations restent distincts.
3. **La confiance est explicite.** Une association ou interprétation incertaine n’est jamais présentée comme certaine.
4. **Aucun cul-de-sac.** Chaque état vide ou erreur propose une explication et une suite possible.
5. **Le téléphone est la référence.** Le parcours complet fonctionne dès 320 px, sans défilement horizontal.
6. **Le français est la norme.** L’anglais n’est retenu que pour un nom propre ou un terme réellement courant.

## 2. Direction artistique : « sentier calme »

L’univers associe la chaleur du papier d’un carnet d’entraînement à la précision d’un instrument sportif. Il ne reprend ni l’esthétique clinique d’une application médicale, ni celle, dense, d’un tableau de bord administratif.

- Fond crème chaleureux, surfaces blanches légèrement teintées.
- Vert forêt pour la confiance et les actions importantes.
- Vert citron doux comme accent rare : progression, sélection, point saillant.
- Courbes simples, angles généreux, ombres discrètes.
- Photographies et illustrations non nécessaires en V1 ; aucune décoration fondée sur de fausses données.
- Icônes au trait homogène, toujours accompagnées d’un libellé quand leur sens n’est pas universel.

La cohérence prime sur la nouveauté : tout nouvel écran réemploie les mêmes couleurs, rayons, espacements, composants et formulations.

## 3. Architecture de l’information

### Navigation principale

Sur téléphone, barre inférieure persistante :

1. **Accueil** — objectif principal, prochaine séance, récupération, alertes et sorties récentes.
2. **Sorties** — historique, états de traitement, détail et comptes rendus.
3. **Synchroniser** — action centrale ouvrant le parcours Garmin ; jamais un import de fichier.
4. **Objectifs** — liste, objectif principal, création, modification et archivage.
5. **Réglages** — Profil, Garmin, Intelligence artificielle et Application.

À partir de 768 px, cette navigation peut devenir latérale. Les noms, l’ordre et les destinations restent identiques. Le logo renvoie à l’Accueil.

### Hiérarchie des écrans

```text
Accueil
├── Objectif principal
├── Prochaine séance
│   ├── Entraînement effectué
│   ├── Reporter
│   ├── Modifier
│   ├── Séance différente réalisée
│   └── Annuler
├── Recommandation actuelle
├── Progression hebdomadaire
└── Dernières sorties
Sorties
├── Liste
└── Détail
    ├── Mesures
    ├── Calculs
    ├── Ressenti
    └── Compte rendu
Synchroniser
└── Recherche → choix/confirmation → ressenti → analyse → séance suivante
Objectifs
├── Liste
└── Fiche / formulaire
Réglages
├── Profil
├── Connexion Garmin
├── Intelligence artificielle
└── Application
```

## 4. Parcours essentiels

### Première utilisation

1. Écran de bienvenue : proposition « Configurer mon profil ».
2. Profil en sections courtes ; les champs facultatifs sont marqués « Facultatif ».
3. Création du premier objectif.
4. Connexion Garmin, avec explication honnête du statut et des prérequis.
5. Configuration de l’IA ou choix de la faire plus tard si l’application le permet.
6. Retour à l’Accueil avec une prochaine action claire.

La progression est enregistrée entre les étapes. Un retour ne fait perdre aucune saisie.

### « Entraînement effectué »

1. Afficher immédiatement « Recherche de ta dernière activité Garmin… » et une progression déterminée quand elle est connue.
2. L’utilisateur peut quitter l’écran ; la synchronisation continue côté serveur.
3. En confiance élevée, présenter une carte candidate et « Confirmer cette activité ».
4. En présence de plusieurs candidates, afficher une liste compacte avec date, heure, sport, distance, durée et tracé réel si disponible.
5. Après confirmation, afficher le formulaire de ressenti court.
6. Enregistrer l’activité avant tout appel à l’IA.
7. Afficher le compte rendu, puis la prochaine séance proposée.

Ne jamais associer silencieusement une activité si la confiance est insuffisante. Ne jamais proposer un import manuel en solution de repli.

### Ressenti

Ordre : ressenti global, RPE sur 10, douleur, sommeil, météo ressentie, hydratation, commentaire. Le ressenti global et le RPE peuvent être requis ; tous les autres champs sont facultatifs. Utiliser des choix tactiles rapides, complétés par du texte visible, sans dépendre de la seule couleur ou d’émojis.

### Reporter une séance

Ouvrir une feuille de dialogue sur téléphone, une boîte de dialogue sur grand écran : nouvelle date obligatoire, raison facultative, rappel de la date initiale. Après validation, annoncer la réussite et actualiser la tuile sans perdre l’historique.

### Erreurs et reprise

Structure constante : titre humain, explication concise, action principale, action secondaire éventuelle, identifiant de diagnostic copiable si utile. Exemple : « Ta course n’est pas encore arrivée dans Garmin Connect. Vérifie la synchronisation de ta montre, puis réessaie. » Une erreur technique brute ou une trace d’exécution n’est jamais affichée.

## 5. Système visuel

### Couleurs

| Jeton | Valeur | Usage |
|---|---:|---|
| `fond` | `#F7F3E8` | fond général |
| `surface` | `#FFFDF7` | cartes, feuilles, champs |
| `surface-secondaire` | `#ECE8DC` | zones secondaires |
| `texte` | `#17251D` | texte principal |
| `texte-secondaire` | `#536158` | texte complémentaire |
| `bordure` | `#CBD2C9` | séparateurs et champs |
| `primaire` | `#174C3C` | boutons principaux, navigation active |
| `primaire-survol` | `#10392D` | survol du primaire |
| `sur-primaire` | `#FFFFFF` | texte sur primaire |
| `accent` | `#B7D83D` | repère de progression, sélection rare |
| `sur-accent` | `#17251D` | texte sur accent |
| `information` | `#155E75` | information et synchronisation |
| `succès` | `#276749` | confirmation |
| `attention` | `#8A5200` | vigilance, confiance moyenne |
| `danger` | `#A12B2B` | erreur, action destructive |

Les couples texte/fond doivent atteindre WCAG 2.2 AA : 4,5:1 pour le texte courant, 3:1 pour le grand texte et les éléments graphiques utiles. L’accent citron n’accueille jamais de texte blanc. Vérifier automatiquement les contrastes dans les tests d’accessibilité.

### Typographie

Pile sans téléchargement externe : `Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif`. Si Inter n’est pas empaquetée localement, la police système est utilisée ; aucune dépendance à un service de polices distant.

- Titre de page : 28/34 px, graisse 700 (24/30 px à 320 px).
- Titre de section : 20/26 px, graisse 700.
- Titre de carte : 17/23 px, graisse 650.
- Corps : 16/24 px, graisse 400.
- Secondaire : 14/20 px, graisse 400.
- Donnée clé : chiffres tabulaires, 24/30 px, graisse 700.
- Minimum absolu : 12 px uniquement pour une métadonnée non essentielle.

### Espacement et géométrie

Échelle : 4, 8, 12, 16, 24, 32, 48 et 64 px. Marge de page : 16 px à 320–479 px, 20 px à 480–767 px, 32 px au-delà. Largeur de lecture maximale : 1 200 px ; formulaires : 720 px. Espacement vertical standard entre sections : 24 px sur téléphone, 32 px ailleurs.

- Rayon de carte : 16 px ; champ et petit bouton : 10 px ; pilule : 999 px.
- Bordure : 1 px ; ombre : `0 4px 16px rgb(23 37 29 / 8%)` seulement pour distinguer une surface.
- Hauteur minimale des cibles : 44 px, recommandée 48 px.
- Distance minimale entre cibles voisines : 8 px.

### Iconographie et mouvement

Employer une seule bibliothèque d’icônes au trait (Lucide React convient), taille habituelle 20–24 px, `aria-hidden` si décorative. Durées : 120–200 ms pour les retours simples, 200–300 ms pour feuilles et panneaux. Respecter `prefers-reduced-motion`; aucun mouvement essentiel à la compréhension.

## 6. Composants de référence

- **En-tête de page** : titre, aide courte, action éventuelle.
- **Tuile Prochaine séance** : date, titre, objectif, prescription lisible, explication repliable, action principale pleine largeur sur téléphone, actions secondaires dans un menu nommé.
- **Carte Objectif** : nom, échéance, jours restants, statut et progression factuelle.
- **Carte Activité** : date/heure, type, distance, durée, statut de traitement ; aucune valeur inventée.
- **Étiquette d’état** : icône + texte + couleur. Jamais la couleur seule.
- **Indicateur de confiance** : « Confiance élevée/moyenne/faible » avec explication accessible.
- **Bouton** : primaire, secondaire, discret, destructif ; état survol, focus, actif, chargement et désactivé.
- **Champ** : libellé persistant au-dessus, aide avant erreur, erreur liée avec `aria-describedby` ; jamais le seul espace réservé comme libellé.
- **Choix segmenté** : 2 à 4 options maximum ; sinon boutons radio.
- **Boîte de dialogue/feuille** : focus piégé, titre explicite, fermeture clavier, retour du focus à l’appelant.
- **Notification** : annoncée par région vive adaptée, sans disparaître avant lecture.
- **Squelette** : reproduit la structure attendue ; l’alternative textuelle annonce le chargement.
- **État vide** : raison + action utile ; ni mascotte ni graphique trompeur.
- **Graphique** : titre, unité, légende, résumé textuel et tableau accessible ou description équivalente.

### États obligatoires

Tout composant connecté à des données prévoit : initial, chargement, succès, vide, incomplet, erreur temporaire, erreur définitive et nouvelle tentative. Le bouton déclencheur reste stable en largeur durant le chargement. Toute action destructive demande confirmation et indique ses conséquences.

## 7. Adaptation aux écrans

- **320–479 px** : une colonne, actions principales pleine largeur, navigation inférieure, aucun tableau large, cartes sans sous-grille contrainte.
- **480–767 px** : une colonne enrichie ; certaines paires de champs peuvent partager une ligne si leurs libellés restent lisibles.
- **768–1 023 px** : navigation latérale possible, grille de deux cartes, contenu principal fluide.
- **1 024 px et plus** : grille jusqu’à 12 colonnes, contenu plafonné à 1 200 px ; les lignes de texte restent autour de 65–75 caractères.

À 320 px, les longues valeurs et URL reviennent à la ligne. Les graphiques défilent dans leur propre zone seulement si une simplification est impossible. Aucun contrôle essentiel n’est masqué selon la largeur.

## 8. Accessibilité normative

Cible : WCAG 2.2 niveau AA.

- Structure sémantique : un `h1`, titres hiérarchiques, régions `header`, `nav`, `main`, `footer` pertinentes.
- Tous les parcours sont réalisables au clavier, dans un ordre logique, avec focus visible d’au moins 2 px et contraste 3:1.
- Lien d’évitement vers le contenu principal.
- Zoom navigateur jusqu’à 200 % et redistribution à 320 CSS px sans perte de contenu ni fonctionnalité.
- Taille tactile minimale 44 × 44 px.
- Noms accessibles identiques ou commençant par le libellé visible.
- Erreurs de formulaire résumées en tête et associées à chaque champ ; saisies conservées.
- Changements asynchrones annoncés sans déplacement de focus intempestif.
- Temps, unités et abréviations explicités à la première occurrence ; dates non ambiguës, par exemple « 21 août 2026 ».
- Graphiques accompagnés d’une restitution textuelle des données importantes.
- Aucun contenu ne clignote ; mouvement réduit respecté.
- Tests automatisés axe et tests clavier sur les parcours critiques ; vérification manuelle avec VoiceOver ou NVDA.

## 9. Langue et terminologie

Le français est la langue principale de toute l’application. Utiliser des formulations naturelles pour les textes, boutons, menus, titres, messages, états, notifications, erreurs, comptes rendus, documentation utilisateur et journaux utiles.

Éviter les anglicismes lorsqu’un équivalent français courant et précis existe. Un terme anglais reste acceptable seulement s’il constitue le terme technique ou sportif couramment employé, si sa traduction est artificielle ou ambiguë, ou s’il est imposé par une technologie, un protocole ou un nom de produit.

| À employer | À éviter dans l’interface |
|---|---|
| Tableau de bord / Accueil | Dashboard |
| Compte rendu | Reporting |
| Synchronisation / Synchroniser | Sync |
| Réglages | Settings |
| État de récupération | Readiness |
| Allure | Pace, sauf nom du produit |
| Interface / serveur | Frontend / backend, hors documentation technique |
| Rappel applicatif | Push, sauf architecture Garmin Push |
| Point de rappel | Webhook, hors documentation technique |
| En cours de chargement | Loading |
| Réessayer | Retry |
| Enregistrer | Save |

Termes acceptés selon le contexte : Garmin Connect, FIT, API REST, OpenAPI, Docker Compose, Java, Spring Boot, React, TypeScript, Vite, PostgreSQL, webhook, backend, frontend et mobile-first dans la documentation technique. Dans l’interface, préférer « téléphone » à « mobile » lorsque le sens le permet.

### Ton éditorial

- Tutoiement cohérent, phrases courtes, voix active.
- Boutons avec un verbe précis : « Enregistrer le profil », « Confirmer cette activité », « Réessayer ».
- Ne pas culpabiliser : « Séance reportée » plutôt que « Séance manquée ».
- Ne pas surpromettre : « Interprétation de confiance moyenne » plutôt qu’une conclusion absolue.
- Ne jamais formuler de diagnostic médical ; inviter à consulter un professionnel lorsque pertinent.
- Employer les unités françaises : `km`, `m`, `min/km`, `bpm`, `W`, avec espace insécable entre valeur et unité lorsque possible.

## 10. Critères de validation de l’expérience

La conception est acceptée si :

1. À 320 px, aucune page du scénario V1 n’a de défilement horizontal.
2. Depuis l’Accueil, « Entraînement effectué » est atteignable et identifiable sans ouvrir un menu.
3. La synchronisation n’offre jamais d’import manuel dans le parcours principal.
4. Chaque état Garmin demandé possède un message français et une action appropriée.
5. Les mesures, calculs, interprétations et recommandations sont sémantiquement séparés.
6. Le niveau de confiance accompagne chaque interprétation.
7. Le parcours critique est entièrement utilisable au clavier et avec lecteur d’écran.
8. Les contrastes, cibles tactiles et redistributions respectent les règles ci-dessus.
9. Les écrans ne présentent aucune donnée factice hors mode de démonstration explicitement identifié.
10. Une même action, un même état et un même concept portent le même nom partout.

## 11. Gouvernance de cohérence

Ce document est la source normative de l’interface. Toute divergence doit être justifiée dans une décision d’architecture ou corrigée. Avant d’ajouter un composant, vérifier qu’un composant de référence ne couvre pas déjà le besoin. Les textes partagés et jetons visuels doivent être centralisés. Une revue visuelle est effectuée aux largeurs 320, 375, 768 et 1 280 px, ainsi qu’en zoom 200 %.
