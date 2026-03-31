Monster Battle Platform

Ce projet utilise majoritairement du code généré avec l’aide d’une IA, mais la conception, les choix techniques et la logique restent très largement issus d’un travail personnel.

Il s’agit d’une plateforme de combat de monstres basée sur une architecture microservices.

Architecture technique

Le projet est découpé en plusieurs microservices conteneurisés avec Docker Compose.

Auth Service

Ce service gère l’authentification des utilisateurs.

Fonctionnalités principales :

Inscription
Connexion
Validation des tokens

Le fichier CryptoService.java regroupe les fonctionnalités suivantes :

Hash du mot de passe
Génération de token utilisé par les autres services
Vérification du mot de passe
Chiffrement et déchiffrement

Un mécanisme de chiffrement du token a été mis en place.

Lors de la connexion :

Un token chiffré est généré
Il est stocké en base de données

Pour valider un token :

Il est déchiffré
Il respecte un format de type NOM-AAAA/MM/JJ-HH:MM:ss
Le service vérifie sa validité avec une durée de vie limitée à 15 minutes

Routes disponibles :

/register : inscription
/login : connexion
/validate : validation d’un token
Player Service

Ce service gère les informations liées aux joueurs :

Expérience
Liste de monstres
Date de création
Niveau
Statistiques de combat

Les différentes routes sont disponibles via la documentation OpenAPI du service.

Monster Service

Ce service est responsable de la gestion des monstres.

Fonctionnalités principales :

Création de monstres
Récupération des informations
Gestion de l’expérience

Toutes les opérations liées aux monstres passent par ce service.

Invocation Service

Ce service permet d’invoquer des monstres.

Fonctionnement :

Vérification de la limite d’inventaire
Tirage aléatoire d’un monstre depuis une base contenant les 4 monstres fournis
Le tirage respecte un système de probabilités

Un test est disponible ici :
gatcha/invocations/src/test/java/com/gatcha/invocations/InvocationService.java

Combat Service

Ce service gère la logique de combat ainsi que l’enregistrement des actions pour permettre un replay.

Chaque combat est enregistré en base de données et peut être rejoué.

Logique de combat

Le système repose sur des affrontements automatisés au tour par tour entre deux créatures.

Initialisation :

Les statistiques (PV, Attaque, Défense) sont récupérées via le Monster Service
Les temps de recharge des compétences sont réinitialisés

Déroulement :

Les monstres attaquent à tour de rôle
Le combat se termine lorsqu’un monstre atteint 0 PV ou après 100 tours
À chaque tour, une compétence disponible est choisie aléatoirement
Si aucune compétence n’est disponible, une attaque de base est utilisée

Calcul des dégâts :

D
e
ˊ
g
a
ˆ
ts
=
max
⁡
(
1
,
(
D
e
ˊ
g
a
ˆ
ts_Sort
+
(
Stat
×
Ratio
)
)
−
D
e
ˊ
fense
2
)
D
e
ˊ
g
a
ˆ
ts=max(1,(D
e
ˊ
g
a
ˆ
ts_Sort+(Stat×Ratio))−
2
D
e
ˊ
fense
	​

)

Ce calcul prend en compte :

La puissance du sort
Une statistique du lanceur (ATK, DEF, HP ou VIT)
La défense de la cible

Fin de combat :

L’expérience est distribuée aux monstres et aux joueurs
Chaque action est enregistrée sous forme de BattleStep
Cela permet de rejouer fidèlement le combat
