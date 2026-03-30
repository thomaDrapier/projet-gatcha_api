package combat.combat.model;

import java.util.Map;

import lombok.Data;

@Data // Génère automatiquement Getters, Setters, toString
public class BattleStep {
    private int turn;
    private String attackerName;
    private String skillName;
    private int damage;
    private int targetRemainingHp;
    private Map<String, Integer> cooldowns;
    private String description;

    // Constructeur vide (Obligatoire pour MongoDB/JSON)
    public BattleStep() {}

    // Constructeur complet (Utilisé par BattleService)
    public BattleStep(int turn, String attackerName, String skillName, int damage, 
                      int targetRemainingHp, Map<String, Integer> cooldowns, String description) {
        this.turn = turn;
        this.attackerName = attackerName;
        this.skillName = skillName;
        this.damage = damage;
        this.targetRemainingHp = targetRemainingHp;
        this.cooldowns = cooldowns;
        this.description = description;
    }
}