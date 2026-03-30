package combat.combat.dto;

import lombok.Data;

@Data
public class SkillRatio {
    private String stat;    // ex: "atk"
    private int percent;    // ex: 25 (pour 25%)
    
    public SkillRatio() {}
}