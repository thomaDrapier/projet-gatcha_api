package com.gatcha.monster.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Skill {
    
    @Schema(description = "Nom de la compétence")
    private String name;

    @Schema(description = "Dégâts de base")
    private int baseDamage;

    @Schema(description = "Statistique (hp, atk, def, vit) favorable pour le skill")
    private String scalingStat;

    @Schema(description = "Multiplicateur de la stat")
    private double damageRatio;

    @Schema(description = "Nombre de tours de recharge")
    private int cooldown;

    @Schema(description = "Niveau actuel d'amélioration")
    private int currentUpgradeLevel;
    

    @Schema(description = "Niveau maximum d'amélioration")
    private int maxUpgradeLevel;

    public Skill() {}
}