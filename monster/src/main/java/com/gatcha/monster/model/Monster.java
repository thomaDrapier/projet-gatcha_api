package com.gatcha.monster.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Document(collection = "monsters")
@Schema(description = "Instance réelle d'un monstre possédé par un joueur")
public class Monster {
    @Id
    private String id;
    private String ownerUsername; // Lien avec le joueur [cite: 221, 268]
    private String element;
    private int level; // Commence au niveau 1 [cite: 256]
    private double experience;
    
    // Stats actuelles
    private int hp;
    private int atk;
    private int def;
    private int vit;
    
    private List<MonsterSkill> skills;
}

@Data
class MonsterSkill {
    private int num;
    private int currentLevel; // Niveau d'amélioration [cite: 266]
    private int maxLevel;
    // Les dégâts et ratios sont recalculés en fonction du niveau
}