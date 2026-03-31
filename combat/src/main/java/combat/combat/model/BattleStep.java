package combat.combat.model;

import java.util.Map;

import lombok.Data;

@Data // Génère automatiquement Getters, Setters, toString
public class BattleStep {
    private int turn;
    private String attackerId; // --- NOUVEAU : L'ID unique du monstre ---
    private String attackerName;
    private String skillName;
    private int damage;
    private int targetRemainingHp;
    private Map<String, Integer> cooldowns;
    private String description;

    // Constructeur vide (Obligatoire pour MongoDB/JSON)
    public BattleStep() {}

    // Constructeur complet mis à jour
    public BattleStep(int turn, String attackerId, String attackerName, String skillName, int damage, 
                      int targetRemainingHp, Map<String, Integer> cooldowns, String description) {
        this.turn = turn;
        this.attackerId = attackerId; // On sauvegarde l'ID
        this.attackerName = attackerName;
        this.skillName = skillName;
        this.damage = damage;
        this.targetRemainingHp = targetRemainingHp;
        this.cooldowns = cooldowns;
        this.description = description;
    }
}