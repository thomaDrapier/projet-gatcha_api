package combat.combat.dto;
import lombok.Data;

@Data
public class SkillDTO {
    private int num;           // Correspond à num
    private int dmg;           // Correspond à dmg (au lieu de baseDamage)
    private SkillRatio ratio;  // Attention : Tu dois aussi créer le DTO SkillRatio !
    private int cooldown;
    private int lvlMax;
    private int currentLevel;
    
    // On ajoute le nom pour l'affichage (si MonsterSkill n'a pas de String 'name', 
    // on utilisera "Skill " + num dans le service)
    private String name; 

    public SkillDTO() {}
}