package com.gatcha.monster.model;

import lombok.Data;

@Data
public class SkillRatio {
    private String stat;    // ex: "atk"
    private int percent;    // ex: 25
}