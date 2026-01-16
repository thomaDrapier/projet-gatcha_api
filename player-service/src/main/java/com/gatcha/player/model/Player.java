package com.gatcha.player.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "players")
public class Player {
    @Id
    private String id;

    private String username; // Pour faire le lien avec le Auth Service

    private Integer level;
    private Double experience;
    private Double xpThreshold; // Combien d'XP il faut pour le prochain niveau

    // Liste des IDs des monstres
    private List<String> monsters;

    // Constructeur par défaut pour initialiser un nouveau joueur
    public Player() {
        this.level = 1;
        this.experience = 0.0;
        this.xpThreshold = 50.0; // Règle : commence à 50
        this.monsters = new ArrayList<>();
    }

    public void setExperience(double v) {
    }
}