# Module Consommateur

Ce module fait partie du projet `discord_scala` et est responsable de la consommation et du traitement des messages provenant d'une source spécifiée.

## Fonctionnalités

- Se connecte à un courtier de messages ou une API.
- Traite les messages entrants.
- Gère les erreurs de manière élégante.
- Offre une extensibilité pour la gestion personnalisée des messages.

## Utilisation

Pour démarrer le consommateur, exécutez :

```bash
sbt run
```

## Fonctionnalités principales

**Connexion à Pulsar** : Consommation des messages du topic persistent://public/default/discord-messages.
**Décodage JSON** : Les messages sont décodés en objets Scala (Message) via Circe.
**Stockage MongoDB** : Insertion automatique de chaque message dans la base de données MongoDB discordMongo, collection messages.
**WebSocket Server** :

- Démarrage d'un serveur WebSocket sur ws://localhost:8082/subscriptions.
- Gestion dynamique des abonnements des clients aux serveurs spécifiques (SubscriptionRegistry).
- Diffusion en temps réel aux clients abonnés aux bons serveurs.
  **Gestion d'erreurs robuste** : Tolérance aux erreurs réseau et de parsing.
  **Traitement concurrentiel** : Utilisation de ZIO Queue pour diffuser les messages efficacement en parallèle aux WebSocket Channels.

## Structure des Messages

{
"id": "serverId",
"timestamp": "2025-04-27T14:30:00Z",
"content": "Hello World!",
"metadata": {
"author": "user123",
"channel": "general"
}
}

## Détails techniques

**Akka Streams** : pour consommer les flux Pulsar.
**Pulsar4s** : bibliothèque Scala pour Pulsar.
**ZIO** : utilisé pour la gestion des WebSocket, la queue de traitement parallèle et le serveur HTTP.
**MongoDB Scala Driver** : pour stocker les messages.
**Circe** : pour la désérialisation JSON.

## Configuration

Le consommateur peut être configuré à l'aide de variables d'environnement ou d'un fichier de configuration. Mettez à jour le fichier `application.conf` dans le répertoire `resources` pour définir vos préférences.
