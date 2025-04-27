# Consumer

Le consumer consomme des messages d'un topic Pulsar (discord-messages), les insère dans MongoDB, et les distribue aux clients WebSocket abonnés.

## Lancer application

3. Lancez l'application avec SBT :
   ```bash
   sbt run
   ```

## Fonctionnalités

**Connexion à Pulsar** : Consommation des messages du topic persistent://public/default/discord-messages.
**Décodage JSON** : Les messages sont décodés en objets Scala (Message) via Circe.
**Stockage MongoDB** : Insertion automatique de chaque message dans la base de données MongoDB discordMongo, collection messages.

## Prérequis

**Scala** : version 3
**Pekko Streams** : pour consommer les flux Pulsar.
**Pulsar4s** : bibliothèque Scala pour Pulsar.
**ZIO** : utilisé pour la gestion des WebSocket, la queue de traitement parallèle et le serveur HTTP.
**MongoDB Scala Driver** : pour stocker les messages.
**Circe** : pour la désérialisation JSON.

## Structure des Messages reçus

```json
{
  "id": "serverId",
  "timestamp": "2025-04-27T14:30:00Z",
  "content": "Hello World!",
  "metadata": {
    "author": "user123",
    "channel": "general"
  }
}
```

## Configuration

Le consommateur peut être configuré à l'aide de variables d'environnement ou d'un fichier de configuration. Mettez à jour le fichier `application.conf` dans le répertoire `resources` pour définir vos préférences.
