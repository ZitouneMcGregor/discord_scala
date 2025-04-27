# AdminAPI

AdminAPI est une application basée sur Akka HTTP qui permet de gérer des utilisateurs, des serveurs, des salons, et des chats privés. Elle utilise une base de données PostgreSQL pour le stockage des données et est conçue pour fonctionner sur Clever Cloud.

## Fonctionnalités

- **Gestion des utilisateurs** :

  - Lister tous les utilisateurs.
  - Récupérer un utilisateur spécifique.
  - Créer un utilisateur.
  - Supprimer un utilisateur.
  - Mettre à jour les informations d'un utilisateur.
  - Rechercher des utilisateurs par préfixe.

- **Gestion des serveurs** :

  - Lister tous les serveurs.
  - Récupérer un serveur spécifique.
  - Créer un serveur.
  - Supprimer un serveur.
  - Mettre à jour les informations d'un serveur.
  - Récupérer les salons associés à un serveur.

- **Gestion des salons** :

  - Lister tous les salons.
  - Récupérer un salon spécifique.
  - Créer un salon.
  - Supprimer un salon.
  - Mettre à jour les informations d'un salon.s

- **Gestion des chats privés** :

  - Lister les chats privés d'un utilisateur.
  - Créer un chat privé.
  - Supprimer un chat privé pour un utilisateur.

- **Authentification** :

  - Authentification basique avec des identifiants (`foo:bar`).
  - Lecture des paramètres d'authentification depuis le fichier `application.conf`.

- **Base de données** :
  - Stockage des données dans une base PostgreSQL.
  - Lecture des paramètres de connexion depuis les variables d'environnement Clever Cloud.

## Configuration

Les paramètres de configuration sont situés dans le fichier [`application.conf`](src/main/resources/application.conf). Vous pouvez y configurer :

- Les paramètres d'authentification basique.
- Les paramètres de connexion à la base de données PostgreSQL.

### Variables d'environnement

L'application utilise les variables d'environnement suivantes pour se connecter à la base de données :

- `POSTGRESQL_ADDON_HOST` : Hôte de la base de données (par défaut : `localhost`).
- `POSTGRESQL_ADDON_PORT` : Port de la base de données (par défaut : `5432`).
- `POSTGRESQL_ADDON_DB` : Nom de la base de données (par défaut : `AdminDiscord`).
- `POSTGRESQL_ADDON_USER` : Nom d'utilisateur de la base de données (par défaut : `admin`).
- `POSTGRESQL_ADDON_PASSWORD` : Mot de passe de la base de données (par défaut : `1234567aA*`).

## Dépendances

L'application utilise les bibliothèques suivantes :

### Akka HTTP

- `pekko-http`
- `pekko-http-cors`
- `pekko-http-spray-json`
- `pekko-actor-typed`
- `pekko-stream`

### Base de données

- `doobie-core`
- `doobie-postgres`

## Registries

Les _registries_ sont des composants responsables de la gestion des entités principales de l'application. Voici les principaux _registries_ utilisés :

- **UserRegistry** : Gère les utilisateurs (création, suppression, mise à jour, recherche).
- **ServerRegistry** : Gère les serveurs (création, suppression, mise à jour, récupération).
- **RoomRegistry** : Gère les salons (création, suppression, mise à jour, récupération).
- **PrivateChatRegistry** : Gère les chats privés (création, suppression, récupération).
- **UserServerRegistry** : Gère les relations entre utilisateurs et serveurs (ajout, suppression, mise à jour des droits d'administration).

## Routes disponibles

### Utilisateurs

- `GET /users` : Liste tous les utilisateurs.
- `GET /users/:username` : Récupère un utilisateur par son nom.
- `GET /users/id/:id` : Récupère un utilisateur par son ID.
- `POST /users` : Crée un nouvel utilisateur.
- `PUT /users/:username` : Met à jour un utilisateur existant.
- `DELETE /users/:username` : Supprime un utilisateur.
- `GET /users/search?username=<prefix>` : Recherche des utilisateurs par préfixe.

### Serveurs

- `GET /servers` : Liste tous les serveurs.
- `GET /servers/:id` : Récupère un serveur par son ID.
- `POST /servers` : Crée un nouveau serveur.
- `PUT /servers/:id` : Met à jour un serveur existant.
- `DELETE /servers/:id` : Supprime un serveur.
- `GET /servers/:id/rooms` : Liste les salons d'un serveur.

### Salons

- `GET /rooms` : Liste tous les salons.
- `GET /rooms/:id` : Récupère un salon par son ID.
- `POST /rooms` : Crée un nouveau salon.
- `PUT /rooms/:id` : Met à jour un salon existant.
- `DELETE /rooms/:id` : Supprime un salon.

### Chats privés

- `GET /private-chats/:userId` : Liste les chats privés d'un utilisateur.
- `POST /private-chats` : Crée un nouveau chat privé.
- `DELETE /private-chats/:id` : Supprime un chat privé.
