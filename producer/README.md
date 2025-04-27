# Producer

**Producer** est un service backend basé sur Apache Pekko HTTP (anciennement Akka HTTP), conçu pour produire des messages dans une file d'attente Pulsar. Il expose une API REST sécurisée avec une authentification basique et utilise `pulsar4s` pour communiquer avec Pulsar.

## Fonctionnalités

### API REST

- Endpoint pour envoyer des messages à une file d'attente Pulsar.
- Authentification basique pour sécuriser les requêtes.

### Intégration avec Pulsar

- Envoi de messages dans un topic Pulsar.
- Utilisation de `pulsar4s` pour interagir avec Pulsar.

### Configuration flexible

- Variables d'environnement pour configurer l'authentification et d'autres paramètres.
- Support de CORS pour permettre les requêtes cross-origin.

## Prérequis

- **Java** : Version 11 ou supérieure.
- **SBT** : Pour compiler et exécuter le projet.
- **Apache Pulsar** : Un cluster Pulsar fonctionnel (local ou distant).
- **Variables d'environnement** :
  - `BASIC_AUTH_USER` : Nom d'utilisateur pour l'authentification basique.
  - `BASIC_AUTH_PASSWORD` : Mot de passe pour l'authentification basique.

## Exécution

Pour lancer le service, utilisez la commande suivante :

```bash
sbt run
```

## Routes

### **POST /message**

Permet d'envoyer un message dans un topic Pulsar.

#### Exemple de corps de requête JSON :

```json
{
  "id": "123",
  "content": "Hello, world!",
  "metadata": {
    "key1": "value1",
    "key2": "value2"
  }
}
```
