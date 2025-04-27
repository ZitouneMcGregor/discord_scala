# Projet Discord Scala

## Description de l'application

Cette application est une plateforme de communication semblable à Discord, écrite en Scala. Elle permet aux utilisateurs de communiquer via des messages texte, de créer des salons de discussion, et d'interagir en temps réel.

## Prérequis

- Docker installé sur votre machine.

## Lancer l'application

1. Construisez et démarrez les conteneurs Docker :

   ```bash
   docker-compose up --build
   ```

2. Accédez à l'application via votre navigateur à l'adresse suivante :  
   [http://localhost:8081/](http://localhost:8081/)

## Arrêter et supprimer l'application

Pour arrêter et supprimer les conteneurs ainsi que les volumes associés, exécutez la commande suivante :

```bash
docker-compose down -v
```

## Équipe

- Olivier CARRERE-GEE
- Mounic CLEMENT
- Jérémi LIOGER-BUN
- Omar BENZEROUAL

## Diagramme d'architecture

![Diagramme d'architecture](architecture.png)
