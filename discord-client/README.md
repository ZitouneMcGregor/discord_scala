# Discord Client

Front de l'application Discord en Scala. Ce front est fait à l'aide de Vue.js.

## Lancer application en développement

    npm run dev

## Fonctionnalités

**Gestion des utilisateurs** :
Connexion et inscription via un système d'authentification basique.
Mise à jour du profil utilisateur (nom d'utilisateur, mot de passe).
Suppression de compte.

**Gestion des serveurs** :

Création de serveurs avec un nom et une image.
Affichage des serveurs disponibles et possibilité de les rejoindre.
Gestion des utilisateurs dans un serveur (promotion en administrateur, expulsion).
Suppression de serveurs par les administrateurs.

**Gestion des salons** :

Création, modification et suppression de salons dans un serveur.
Affichage des messages d'un salon en temps réel.
Envoi de messages dans un salon.

**Chats privés** :

Création de conversations privées entre utilisateurs.
Envoi et réception de messages en temps réel dans des chats privés.

**Interface utilisateur moderne** :

Navigation intuitive avec une barre latérale pour accéder aux serveurs et aux chats privés.
Design inspiré de Discord avec des couleurs sombres et des transitions fluides.

## Prérequis

- **Node.js** : Version 14 ou supérieure.
- **npm** : Version 6 ou supérieure.
- **Vue.js** : Framework utilisé pour le développement de l'interface.
- **Pinia** : Gestionnaire d'état pour Vue.js.
- **Backend** : Une API REST fonctionnelle (voir le README de `adminapi` pour les détails).

## Structure du projet

**public/** : Contient les fichiers statiques accessibles directement par le navigateur, comme index.html (le fichier HTML principal) et les ressources publiques (images, icônes, etc.).

**components/** : Composants Vue.js réutilisables, comme des boutons ou des formulaires.

**router/** : Configuration du routeur Vue.js pour gérer la navigation entre les pages.

**store/**: Gestion de l'état global de l'application avec Pinia.

**App.vue**: Le composant racine de l'application Vue.js.

**main.js** : Le point d'entrée principal où l'application Vue.js est initialisée.

**.env**: Fichier pour définir les variables d'environnement (ex. : clés API, URL backend).

**package.json**: Contient les dépendances npm et les scripts pour gérer le projet.
