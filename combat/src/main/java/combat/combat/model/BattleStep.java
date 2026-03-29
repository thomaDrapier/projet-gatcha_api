package combat.combat.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BattleStep {
    private int turn;                  // Le numéro du tour
    private String attackerName;       // ID ou Nom du monstre qui attaque
    private String skillUsed;          // Nom de la compétence utilisée
    private int damage;                // Dégâts infligés
    private int targetRemainingHp;     // PV restants de la cible après l'attaque
    private Map<String, Integer> currentCooldowns; // État des cooldowns de l'attaquant à la fin de son tour
    private String description;        // Phrase descriptive de l'action pour le Front-end
}