package combat.combat.dto;

import java.util.List;

import lombok.Data;

@Data
public class MonsterDTO {
    private String id;
    private String ownerUsername; // <-- Ajout de ce champ pour récupérer le nom du joueur
    private String templateId; 
    private int hp;
    private int atk;
    private int def;
    private int vit;
    private int level;
    private List<SkillDTO> skills;
}