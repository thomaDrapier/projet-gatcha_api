package com.gatcha.monster.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "player_monsters")
public class MonsterInstance {
    @Id
    private String id; // ID unique auto-généré par MongoDB
    private int templateId; // ID du monstre (1, 2, 3 ou 4)
    private String owner; // Le pseudo du joueur
    private int level = 1;
    private int experience = 0;
}