# ChatApi

ChatApi est une application développée en Scala qui fournit une API pour gérer des fonctionnalités de chat en temps réel. Elle est conçue pour être performante, extensible et facile à intégrer dans des systèmes existants.
Le ChatApi lit les messages depuis MongoDB pour fournir l'historique des messages d'une salle et récupérer les messages privés entre utilisateurs.

## Lancer application

3. Lancez l'application avec SBT :
   ```bash
   sbt run
   ```

## Fonctionnalités

- **Gestion des utilisateurs** : Création, mise à jour et suppression des utilisateurs.
- **Salles de chat** : Création et gestion de salles de discussion.
- **Messagerie en temps réel** : Envoi et réception de messages instantanés.
- **Authentification** : Support de l'authentification sécurisée pour les utilisateurs.
- **WebSocket** : Communication en temps réel via WebSocket.
- **Historique des messages** : Stockage et récupération des messages envoyés.

## Routes disponibles

### Utilisateurs

- **POST /users** : Créer un utilisateur.
- **GET /users/{id}** : Récupérer les informations d'un utilisateur.
- **PUT /users/{id}** : Mettre à jour un utilisateur.
- **DELETE /users/{id}** : Supprimer un utilisateur.

### Salles de chat

- **POST /rooms** : Créer une salle de chat.
- **GET /rooms** : Lister toutes les salles de chat.
- **GET /rooms/{id}** : Récupérer les détails d'une salle de chat.
- **DELETE /rooms/{id}** : Supprimer une salle de chat.

### Messages

- **POST /rooms/{id}/messages** : Envoyer un message dans une salle.
- **GET /rooms/{id}/messages** : Récupérer l'historique des messages d'une salle.
