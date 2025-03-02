sbt project compiled with Scala 3
Usage

Ce projet implémente l'API REST de la partie admin de notre application. L'API utilise Pekko HTTP et les Futures pour la gestion asynchrone, et implémente les opérations CRUD (Create, Read, Update, Delete) sur la base de données admin.
Prérequis

    Java 11 ou supérieur
    sbt (Scala Build Tool) installé
    Scala 3

Compilation et Exécution

Pour compiler le projet :

sbt compile

Pour exécuter l'application :

sbt run

L'API sera exposée sur le port 8080 (modifiable via la configuration ou une variable d'environnement).

Technologies Utilisées

    Scala 3
    sbt
    Pekko HTTP (pour l'API REST)
    Futures (pour la gestion asynchrone)
    HikariCP (gestion du pool de connexions)
   
