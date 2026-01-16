package com.gatcha.player.controller;

import com.gatcha.player.model.Player;
import com.gatcha.player.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // Créer ou récupérer un profil (Utile au premier login)
    @PostMapping("/init/{username}")
    public ResponseEntity<Player> initPlayer(@PathVariable String username) {
        return ResponseEntity.ok(playerService.createPlayer(username));
    }

    // Voir son profil
    @GetMapping("/{username}")
    public ResponseEntity<?> getProfile(@PathVariable String username) {
        try {
            return ResponseEntity.ok(playerService.getPlayer(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Gagner de l'XP : POST /player/Ash/xp?amount=100
    @PostMapping("/{username}/xp")
    public ResponseEntity<?> gainXp(@PathVariable String username, @RequestParam double amount) {
        try {
            return ResponseEntity.ok(playerService.gainExperience(username, amount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Ajouter un monstre : POST /player/Ash/monsters/pikachu-id-123
    @PostMapping("/{username}/monsters/{monsterId}")
    public ResponseEntity<?> addMonster(@PathVariable String username, @PathVariable String monsterId) {
        try {
            return ResponseEntity.ok(playerService.addMonster(username, monsterId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /player/Ash/level
    @GetMapping("/{username}/level")
    public ResponseEntity<?> getLevel(@PathVariable String username) {
        try {
            Integer level = playerService.getPlayerLevel(username);
            // On renvoie un petit JSON pour que ce soit propre
           // return ResponseEntity.ok(Map.of("level", level));
            return ResponseEntity.ok("lol");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /player/Ash/monsters
    @GetMapping("/{username}/monsters")
    public ResponseEntity<?> getMonsters(@PathVariable String username) {
        try {
            return ResponseEntity.ok(playerService.getPlayerMonsters(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}