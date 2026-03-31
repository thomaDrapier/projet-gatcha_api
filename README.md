# Gatcha Battle Microservices

> **Note sur le développement** : Ce projet utilise majoritairement du code généré avec l’aide d’une IA, mais la conception, l'architecture et la logique globale restent issues de ma réfléxion !

Ce projet est une plateforme de combat de monstres basée sur une **architecture microservices**. L'ensemble du système est orchestré pour simuler une expérience de jeu Gatcha complète, de l'inscription du dresseur à l'évolution de ses créatures.

---

## Architecture Technique

Le projet est découpé en plusieurs microservices conteneurisés avec **Docker Compose** :

### Auth-Service
Gère l'inscription, la connexion et la validation des jetons utilisés dans les headers lors des appels API's. La sécurité repose sur le composant `CryptoService.java`, qui permet :
* **Le hashage des mots de passe** (SHA-256) pour un stockage sécurisé.
* **La génération de tokens** uniques pour l'authentification inter-services.
* **La vérification** de l'intégrité des identifiants lors du login.
* **L'encryption et la décryption** des données sensibles.

**Logique de l'encryptage du Token :**
Lors de la connexion, un token crypté est généré et stocké en base de données. 
* **Format** : Une fois décrypté, le token suit le format `NOM-AAAA/MM/JJ-HH:MM:ss`.
* **Validation** : Le service décrypte le jeton pour vérifier son format et sa validité temporelle (TTL).

**Routes du service :**
* `/register` : Inscription d'un nouvel utilisateur.
* `/login` : Authentification et génération du token.
* `/validate` : Route sécurisée permettant aux autres services de valider un token entrant.

---

### Player Service
Gère le profil de chaque dresseur :
* Suivi de l'expérience, du niveau, de l'inventaire de monstres et des statistiques de combat.
* **Documentation** : Toutes les routes sont disponibles via **OpenAPI** (Swagger).

---

### Monster Service
Ce service centralise toute la gestion des instances de monstres :
* Création de monstres avec statistiques initiales.
* Gestion de l'ajout d'expérience et de la montée en niveau.
* Récupération des informations détaillées pour les autres services.

---

### Invocation Service
C'est l'API sollicitée par le joueur pour obtenir de nouvelles créatures :
* **Base de données** : Tirage effectué parmi les 4 monstres fournis dans le catalogue initial.
* **Processus en 4 étapes** :
    1. **Vérification de l'inventaire** : Le dresseur doit avoir une place disponible (limite basée sur son niveau).
    2. **Tirage aléatoire** : Sélection d'un monstre en respectant strictement le **lootrate** défini.
    3. **Création** de l'instance via le Monster Service.
    4. **Attribution** au joueur via le Player Service.

> **Test** : La logique est vérifiable via le fichier de test : `gatcha\invocations\src\test\java\com\gatcha\invocations\InvocationService.java`.

---

## Service de Combat
API gérant la logique de duel et le stockage des historiques.

**La logique de combat :**
Le système gère des affrontements automatisés au tour par tour entre deux créatures :
1. **Initialisation** : Récupération des statistiques (PV, ATK, DEF) via l'API Monstre et réinitialisation des temps de recharge (*cooldowns*).
2. **Déroulement du tour** :
    * Les monstres attaquent alternativement jusqu'à ce que l'un tombe à 0 PV.
    * Le monstre choisit aléatoirement une compétence disponible. Si toutes sont en recharge, il effectue une **attaque de base**.
3. **Récompenses et Archivage** :
    * **XP** : Distribution automatique d'expérience aux monstres et aux joueurs (1000 XP/100 XP pour le gagnant, 30% pour le perdant).
    * **Replay** : Chaque action est enregistrée (BattleStep) pour permettre une relecture exacte du combat.

---

## 🚀 Lancement Rapide

```bash
# Construire et lancer l'ensemble des microservices
docker-compose up --build
