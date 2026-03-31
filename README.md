Monster Battle Platform

Ce projet utilise majoritairement du code généré avec l’aide d’une IA, mais la conception et la logique restent très largement issues de choix personnels.

Mon projet est une plateforme de combat de monstres basée sur une architecture microservices.

Architecture Technique

Le projet est découpé en plusieurs microservices conteneurisés avec Docker Compose.

Auth Service

Ce service gère l'inscription, la connexion et la validation des jetons de sécurité.

Le fichier CryptoService.java contient les fonctions permettant de gérer plusieurs aspects liés à la sécurité : le hash du mot de passe, la génération d’un token utilisé par les autres APIs, la vérification du mot de passe, ainsi que des méthodes de chiffrement et de déchiffrement.

J’ai choisi d’implémenter un système de chiffrement du token.

Lorsqu’un utilisateur se connecte, un token chiffré est généré puis stocké en base de données. Pour vérifier sa validité, le token est ensuite déchiffré. Une fois en clair, il respecte le format suivant :
NOM-AAAA/MM/JJ-HH:MM:ss.

Le service vérifie alors si le token est toujours valide, avec une durée de vie limitée à 15 minutes.

Les routes du service sont les suivantes :

/register : processus d’inscription d’un utilisateur
/login : connexion
/validate : validation d’un token
Player Service

Le Player Service gère le profil de chaque joueur : expérience, liste de monstres, date de création, niveau, nombre de combats, etc.

Le controller expose de nombreuses routes permettant d’effectuer différentes actions. Elles sont toutes disponibles via OpenAPI à l’adresse suivante : XXXXXX.

Monster Service

Le Monster Service est responsable de la gestion des monstres. Toutes les actions relatives aux monstres passent par ce service, que ce soit l’ajout d’expérience, la récupération des informations ou encore la création de nouveaux monstres.

Invocation Service

Cette API est appelée directement par le joueur lorsqu’il souhaite invoquer un monstre.

La base de données est volontairement simple et ne contient que les 4 monstres fournis dans le fichier JSON du TP.

L’invocation se déroule en plusieurs étapes. Le service commence par vérifier la limite d’inventaire du joueur. Ensuite, un tirage aléatoire est effectué pour sélectionner un monstre depuis la base de données. Ce tirage respecte un système de probabilités basé sur un loot rate.

Il est possible de vérifier ce fonctionnement en exécutant le fichier de test suivant :
gatcha/invocations/src/test/java/com/gatcha/invocations/InvocationService.java

Combat Service

Le service de combat gère à la fois la logique des affrontements et l’enregistrement des actions afin de permettre un replay complet.

Chaque combat est enregistré en base de données et peut être rejoué.

Logique de combat

Le système gère des affrontements automatisés au tour par tour entre deux créatures.

Lors de l’initialisation, les statistiques des monstres (PV, Attaque, Défense) sont récupérées via l’API Monster, et les temps de recharge des compétences sont mis à zéro.

Le combat se déroule ensuite tour par tour. Les deux monstres attaquent alternativement jusqu’à ce que l’un tombe à 0 PV, ou jusqu’à atteindre une limite de 100 tours.

À chaque tour, le monstre choisit aléatoirement une compétence disponible. Si toutes les compétences sont en recharge, il effectue une attaque de base.

Le calcul des dégâts prend en compte la puissance du sort, un ratio basé sur une statistique précise (ATK, DEF, HP ou VIT), ainsi que la défense de l’adversaire :

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
Stat_Attaquant
×
Ratio
)
)
−
D
e
ˊ
fense_Cible
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
ts_Sort+(Stat_Attaquant×Ratio))−
2
D
e
ˊ
fense_Cible
	​

)

À l’issue du combat, l’expérience est distribuée aux monstres et aux joueurs. Chaque action est également enregistrée sous forme de BattleStep, ce qui permet de rejouer le combat de manière fidèle.
