package com.gatcha.monster.model;

import lombok.Data;

@Data
public class MonsterSkill {
    private int num;
    private int dmg; 
    private SkillRatio ratio; 
    private int cooldown;
    private int lvlMax;
    private int currentLevel; 
}