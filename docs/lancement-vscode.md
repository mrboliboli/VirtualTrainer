# Lancer Pace depuis VS Code avec Docker

## Prérequis

- Docker Desktop installé et démarré sur le Mac ;
- VS Code installé ;
- le dossier racine `VirtualTrainer` ouvert dans VS Code, et non l'un de ses sous-dossiers.

À la première ouverture, VS Code peut proposer les extensions recommandées par le projet. L'extension Docker est utile pour voir les conteneurs et leurs journaux, mais les tâches décrites ci-dessous fonctionnent aussi depuis la palette de commandes.

## Préparer la configuration locale

Depuis le dossier racine, créer le fichier `.env` à partir de `.env.example`, puis remplacer chaque valeur marquée :

```sh
cp .env.example .env
```

Le fichier `.env` contient des secrets locaux : il ne doit pas être envoyé ni ajouté au dépôt Git. Le connecteur Garmin est désactivé par défaut avec `PACE_CONNECTOR_ENABLED=false`. Il vaut mieux le laisser ainsi jusqu'au moment où une connexion Garmin réelle doit être essayée.

## Démarrer l'application

1. Ouvrir la palette avec `⇧⌘P`.
2. Choisir **Tâches : Exécuter la tâche** (`Tasks: Run Task` si VS Code est en anglais).
3. Choisir **Docker : démarrer l'application**.
4. Attendre que la construction et le démarrage se terminent.
5. Exécuter **Docker : afficher l'état** pour confirmer que les quatre services sont sains.

La tâche de démarrage est aussi la tâche de construction par défaut : le raccourci `⇧⌘B` permet de la lancer directement.

Ouvrir ensuite [http://127.0.0.1:8080](http://127.0.0.1:8080). Le nom d'utilisateur est `pace`. Le mot de passe est la valeur de `PACE_ACCESS_PASSWORD` dans votre fichier `.env` local.

## Tâches disponibles

- **Docker : démarrer l'application** construit les images si nécessaire et démarre les services en arrière-plan.
- **Docker : reconstruire et relancer** force la recréation des conteneurs après une modification importante.
- **Docker : redémarrer l'application** redémarre les conteneurs existants sans reconstruire les images.
- **Docker : afficher l'état** montre l'état et la santé des services.
- **Docker : journaux…** suit les journaux de tous les services ou d'un service précis. Arrêter le suivi avec `Ctrl+C` ; cela n'arrête pas l'application.
- **Docker : arrêter l'application (conserver les données)** arrête les conteneurs sans supprimer les données.
- Les tâches **Tests : …** lancent séparément les tests Java, frontend et Python avec les outils installés sur le Mac.

Les tests locaux nécessitent respectivement Java 21, Node.js avec les dépendances installées dans `frontend`, et Python 3.12 avec les dépendances de développement installées dans `garmin-connector`. Ils ne sont pas nécessaires pour simplement lancer l'application avec Docker.

## Arrêt et conservation des données

Utiliser la tâche **Docker : arrêter l'application (conserver les données)** au quotidien. Elle exécute `docker compose stop` : les conteneurs et les volumes persistent, notamment la base PostgreSQL et la session Garmin chiffrée.

La commande `docker compose down` retire les conteneurs et les réseaux, mais conserve encore les volumes tant que l'option `--volumes` n'est pas ajoutée. Ne pas utiliser `docker compose down --volumes` sauf si vous souhaitez réellement effacer la base locale et la session Garmin enregistrée.

## En cas de problème

1. Vérifier que Docker Desktop est bien démarré.
2. Lancer **Docker : afficher l'état**.
3. Ouvrir **Docker : journaux de tous les services** ou les journaux du service en erreur.
4. Après une modification du code ou des dépendances, lancer **Docker : reconstruire et relancer**.

Si le port `8080` est déjà occupé, modifier `PACE_HTTP_PORT` dans `.env`, reconstruire, puis ouvrir l'adresse avec le nouveau port.
