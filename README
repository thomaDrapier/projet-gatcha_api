# 🐉 Gatcha Battle Microservices

Ce projet est une plateforme de combat de monstres basée sur une architecture microservices. Il permet de gérer l'authentification des dresseurs, l'invocation de monstres via un système de rareté, et des combats automatisés avec un système de replay.

## 🏗️ Architecture Technique
Le projet est découpé en plusieurs microservices conteneurisés avec **Docker Compose** :
* **Auth-Service** : Gère l'inscription, la connexion et la validation des jetons de sécurité.
* **Player-Service** : Gère les profils des dresseurs, leur progression et leur inventaire de monstres.
* **Monster-Service** : Gère les instances de monstres, leurs statistiques et leur montée en niveau.
* **Invocation-Service** : Orchestre la création de nouveaux monstres via un système de tirage aléatoire.
* **Combat-Service** : Exécute la logique des duels et archive l'historique des affrontements.

---

## 1. Fonctionnalités Principales
* **Système d'Invocation** : Permet de générer un monstre aléatoire pour un joueur. Une vérification de la capacité d'inventaire est effectuée (limite fixée à `2 + niveau du joueur`).
* **Loot Rate Respecté** : Le tirage utilise un algorithme pondéré basé sur le `lootRate` défini dans les modèles de monstres pour garantir les probabilités de rareté.
* **Combats entre Monstres** : Duel entre deux créatures avec calcul dynamique des dégâts basé sur leurs statistiques respectives.
* **Système de Replay** : Chaque tour de combat est sauvegardé en base de données (`BattleStep`), permettant de rejouer l'animation complète du duel ultérieurement.

---

## 2. Logique de Combat (`Battle Engine`)
Le moteur de combat gère des affrontements automatisés au tour par tour :
* **Calcul des Dégâts** : La formule prend en compte les dégâts de base de la compétence, le ratio de la statistique associée (ATK, DEF, HP ou VIT) et la défense de la cible.
    * *Formule* : `Dégâts = Max(1, (Dégâts_Base + Stat_Attaquant * Ratio) - (Défense_Cible / 2))`.
* **Gestion des Cooldowns** : Les monstres choisissent aléatoirement parmi leurs compétences disponibles. Si une compétence est utilisée, elle entre en phase de récupération pour un nombre de tours défini.
* **Attaque de Base** : En cas de compétences en cooldown, une attaque de base par défaut est déclenchée.
* **Limites** : Le combat se termine lorsqu'un monstre n'a plus de points de vie ou après 100 tours.

---

## 3. Logique d'Augmentation des Niveaux
La progression est automatisée après chaque combat :
* **Évolution des Monstres** :
    * **XP Requis** : Le seuil pour monter de niveau est égal à `Niveau_Actuel * 1000`.
    * **Gains** : Le vainqueur gagne 1000 XP et le perdant 300 XP (30%).
    * **Statistiques** : À chaque montée de niveau, les HP, l'ATK, la DEF et la VIT augmentent de **10%**. Le monstre gagne également un point de compétence.
* **Progression du Joueur** : Le dresseur gagne de l'expérience à chaque combat (100 XP par victoire, 30 XP par défaite), ce qui augmente son niveau global et sa capacité maximale de monstres.

---

## 4. Service d'Authentification & Sécurité
Le système assure la protection des données et l'intégrité des échanges entre microservices :
* **Sécurité des Mots de Passe** : Utilisation de l'algorithme **SHA-256** pour hasher les mots de passe en base de données.
* **Token AES Personnalisé** : Au lieu d'un simple UUID, le système génère un jeton crypté en **AES** (mode ECB, PKCS5Padding). Ce jeton contient le nom d'utilisateur et un horodatage précis.
* **Validation Temporelle** : Les jetons sont valides pour une durée d'**une heure**. Passé ce délai, le service refuse la validation et demande une reconnexion.
* **Communication Interne** : Les microservices valident systématiquement le jeton via l'endpoint `/auth/validate` pour sécuriser les opérations sensibles.

---

## 🚀 Lancement Rapide

```bash
# Construire et lancer tous les services
docker-compose up --build
