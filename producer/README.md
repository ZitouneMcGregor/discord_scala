# Producer

**Producer** est un service backend basé sur Apache Pekko HTTP (anciennement Akka HTTP), conçu pour produire des messages dans une file d'attente Pulsar. Il expose une API REST sécurisée avec une authentification basique et utilise `pulsar4s` pour communiquer avec Pulsar.

## Lancer application

3. Lancez l'application avec SBT :
   ```bash
   sbt run
   ```

## Fonctionnalités

**Envoi de messages à Pulsar** : Chaque message reçu via l'API est transformé et envoyé vers un topic Pulsar spécifique (persistent://public/default/discord-messages).
**Ajout automatique d'un timestamp**

## Prérequis

**Scala** : Version 3
**Java**
**SBT**
**Apache Pulsar** : Un cluster Pulsar fonctionnel
**Pekko**
**Circe**

## Routes

### **POST /message**

Permet d'envoyer un message dans un topic Pulsar.

#### Structure des messages envoyés :

```json
{
  "id": "unique-id-123",
  "content": "Hello World!",
  "metadata": {
    "author": "John Doe",
    "channel": "general"
  }
}
```
