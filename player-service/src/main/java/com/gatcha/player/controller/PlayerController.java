package com.gatcha.player.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gatcha.player.model.Player;
import com.gatcha.player.model.XpRequest;
import com.gatcha.player.service.PlayerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/player")
@Tag(name = "Joueur", description = "API de gestion des profils des joueurs, de leur expérience (XP) et de leur inventaire de monstres.")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // --- INIT ---
    @Operation(
            summary = "Initialiser un profil joueur",
            description = "Crée un nouveau profil vierge (Niveau 1, XP 0, Inventaire vide) pour un nouvel utilisateur."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil joueur créé avec succès")
    })
    @PostMapping("/init/{username}")
    public ResponseEntity<Player> initPlayer(
            @Parameter(description = "Le pseudo du joueur") @PathVariable String username) {
        return ResponseEntity.ok(playerService.createPlayer(username));
    }

    // --- VOIR LE PROFIL ---
    @Operation(
            summary = "Récupérer le profil complet",
            description = "Renvoie toutes les informations du joueur (niveau, expérience actuelle, et liste des monstres)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil récupéré avec succès"),
            @ApiResponse(responseCode = "400", description = "Joueur introuvable")
    })
    @GetMapping("/{username}")
    public ResponseEntity<?> getProfile(
            @Parameter(description = "Le pseudo du joueur") @PathVariable String username) {
        try {
            return ResponseEntity.ok(playerService.getPlayer(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

// --- GAGNER DE L'XP ---
    @Operation(
            summary = "Ajouter de l'expérience (XP)",
            description = "Ajoute une quantité d'XP au joueur en passant la valeur dans le corps de la requête (Body). Gère automatiquement la montée en niveau."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "XP ajoutée avec succès, retourne le profil mis à jour"),
            @ApiResponse(responseCode = "400", description = "Joueur introuvable ou erreur de calcul")
    })
    @PostMapping("/{username}/xp")
    public ResponseEntity<?> gainXp(
            @Parameter(description = "Le pseudo du joueur") @PathVariable String username,
            @RequestBody XpRequest request) { 
        try {
            // On récupère le montant depuis l'objet request
            return ResponseEntity.ok(playerService.gainExperience(username, request.getAmount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- AJOUTER UN MONSTRE ---
    @Operation(
            summary = "Ajouter un monstre à l'inventaire",
            description = "Ajoute l'identifiant unique d'un monstre à la liste du joueur. Vérifie que la taille maximale de l'inventaire (10 + niveau) n'est pas dépassée."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Monstre ajouté avec succès"),
            @ApiResponse(responseCode = "400", description = "Inventaire plein ou joueur introuvable")
    })
    @PostMapping("/{username}/monsters/{monsterId}")
    public ResponseEntity<?> addMonster(
            @Parameter(description = "Le pseudo du joueur") @PathVariable String username,
            @Parameter(description = "L'ID unique du monstre à ajouter") @PathVariable String monsterId) {
        try {
            return ResponseEntity.ok(playerService.addMonster(username, monsterId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- GET LEVEL ---
    @Operation(
            summary = "Obtenir le niveau du joueur",
            description = "Renvoie uniquement le niveau actuel du joueur de 1 à 50."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Niveau récupéré avec succès"),
            @ApiResponse(responseCode = "400", description = "Joueur introuvable")
    })
    @GetMapping("/{username}/level")
    public ResponseEntity<?> getLevel(
            @Parameter(description = "Le pseudo du joueur") @PathVariable String username) {
        try {
            Integer level = playerService.getPlayerLevel(username);
            // On renvoie un petit JSON pour que ce soit propre
            // return ResponseEntity.ok(Map.of("level", level));
            return ResponseEntity.ok(level);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- GET MONSTERS ---
    @Operation(
            summary = "Obtenir la liste des monstres",
            description = "Renvoie la liste des identifiants des monstres actuellement possédés par le joueur."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès"),
            @ApiResponse(responseCode = "400", description = "Joueur introuvable")
    })
    @GetMapping("/{username}/monsters")
    public ResponseEntity<?> getMonsters(
            @Parameter(description = "Le pseudo du joueur") @PathVariable String username) {
        try {
            return ResponseEntity.ok(playerService.getPlayerMonsters(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}