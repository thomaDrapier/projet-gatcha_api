package com.gatcha.player.service;

import com.gatcha.player.model.Player;
import com.gatcha.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // 1. Initialiser un joueur (appelé à la première connexion par exemple)
    public Player createPlayer(String username) {
        // Si le joueur existe déjà, on le retourne, sinon on le crée
        return playerRepository.findByUsername(username).orElseGet(() -> {
            Player newPlayer = new Player();
            newPlayer.setUsername(username);
            return playerRepository.save(newPlayer);
        });
    }

    // 2. Récupérer un joueur
    public Player getPlayer(String username) throws Exception {
        return playerRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Joueur introuvable"));
    }

    // 3. Gagner de l'expérience (Logique complexe ici !)
    public Player gainExperience(String username, double amount) throws Exception {
        Player player = getPlayer(username);

        // On ajoute l'XP
        player.setExperience(player.getExperience() + amount);

        // Boucle de Level Up (au cas où on gagne beaucoup d'XP d'un coup et qu'on prend 2 niveaux)
        while (player.getExperience() >= player.getXpThreshold() && player.getLevel() < 50) {
            levelUpLogic(player);
        }

        return playerRepository.save(player);
    }

    // 4. Forcer un Level Up (Ta demande spécifique)
    public Player forceLevelUp(String username) throws Exception {
        Player player = getPlayer(username);
        if (player.getLevel() < 50) {
            levelUpLogic(player);
            return playerRepository.save(player);
        }
        throw new Exception("Niveau max atteint !");
    }

    // --- Logique privée de montée de niveau ---
    private void levelUpLogic(Player player) {
        // 1. On consomme l'XP (L'excédent est gardé, ou reset à 0 selon ta règle ?)
        // Ta règle disait : "reset l'expérience". Donc on remet à 0.
        player.setExperience(0.0);

        // 2. On augmente le niveau
        player.setLevel(player.getLevel() + 1);

        // 3. On augmente le seuil (x1.1)
        player.setXpThreshold(player.getXpThreshold() * 1.1);

        // Note : La taille de la liste monstres est calculée dynamiquement (10 + level), pas besoin de stocker la limite.
    }

    // 5. Ajouter un monstre
    public Player addMonster(String username, String monsterId) throws Exception {
        Player player = getPlayer(username);

        // Vérification de la taille max (10 + level)
        int maxMonsters = 10 + player.getLevel();

        if (player.getMonsters().size() >= maxMonsters) {
            throw new Exception("Inventaire de monstres plein ! (Max: " + maxMonsters + ")");
        }

        player.getMonsters().add(monsterId);
        return playerRepository.save(player);
    }

    // 6. Supprimer un monstre
    public Player removeMonster(String username, String monsterId) throws Exception {
        Player player = getPlayer(username);
        player.getMonsters().remove(monsterId);
        return playerRepository.save(player);
    }

    // 7. Récupérer uniquement le niveau
    public Integer getPlayerLevel(String username) throws Exception {
        return getPlayer(username).getLevel();
    }

    // 8. Récupérer uniquement la liste des monstres
    public java.util.List<String> getPlayerMonsters(String username) throws Exception {
        return getPlayer(username).getMonsters();
    }
}