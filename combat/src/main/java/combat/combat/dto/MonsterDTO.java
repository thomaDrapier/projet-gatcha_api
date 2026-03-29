package combat.combat.dto;

import lombok.Data;
import java.util.List;

@Data
public class MonsterDTO {
    private String id;
    private String templateId; // Pour retrouver le nom ou l'image côté front
    private int hp;
    private int atk;
    private int def;
    private int vit;
    private int level;
    private List<SkillDTO> skills;
}