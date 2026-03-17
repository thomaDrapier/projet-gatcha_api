package com.gatcha.invocations.model;
import lombok.Data;

@Data
public class SkillTemplate {
    private int num;
    private int dmg;
    private SkillRatio ratio;
    private int cooldown;
    private int lvlMax;
}
