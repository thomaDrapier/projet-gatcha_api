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
    private String ownerUsername; // Lien avec le joueur
    private String element;
    private int level; // Commence au niveau 1
    
    // --- MODIFICATIONS ICI ---
    private int xp; // On passe en 'int' (entier) et on le renomme 'xp'
    private String templateId;
    private int skillPoints; // On le renomme 'skillPoints' pour correspondre au Service
    
    // Stats actuelles
    private int hp;
    private int atk;
    private int def;
    private int vit;
    
    private List<MonsterSkill> skills;
}
