package combat.combat.dto;

import lombok.Data;

@Data
public class SkillDTO {
    private String name;
    private int power;
    private int cooldown; // Le temps de recharge MAX de la compétence
}
