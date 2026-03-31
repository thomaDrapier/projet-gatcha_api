package com.gatcha.monster.model;

import lombok.Data;

@Data
public class MonsterSkill {
    private int num;         // Doit correspondre à "num" dans ton JSON
    private int dmg;         // Doit correspondre à "dmg"
    private SkillRatio ratio; // Doit correspondre à l'objet "ratio"
    private int cooldown;    // Doit correspondre à "cooldown"
    private int lvlMax;      // Doit correspondre à "lvlMax"
}