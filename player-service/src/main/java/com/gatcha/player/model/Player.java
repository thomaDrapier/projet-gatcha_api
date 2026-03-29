package com.gatcha.player.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "players")
public class Player {
    @Id
    private String id;

    private String username; 

    private Integer level;
    private Double experience;
    private Double xpThreshold; 

    // --- NOUVEAUX CHAMPS ---
    private LocalDateTime createdAt;
    private Integer totalBattles;

    // Liste des IDs des monstres (Instance IDs)
    private List<String> monsters;

    // Constructeur pour initialiser un nouveau joueur
    public Player() {
        this.level = 1; // Un joueur commence généralement niveau 1
        this.experience = 0.0;
        this.xpThreshold = 100.0; // Palier de départ
        this.monsters = new ArrayList<>();
        this.totalBattles = 0; // Nouveau joueur = 0 combat
        this.createdAt = LocalDateTime.now(); // Date du jour
    }

    // Setter personnalisé pour l'expérience si besoin
    public void setExperience(double experience) {
        this.experience = experience;
    }

    public double getXpThreshold() {
        return this.xpThreshold;
    }
}