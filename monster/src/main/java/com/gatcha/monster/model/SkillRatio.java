package com.gatcha.monster.model;

import lombok.Data;

@Data
public class SkillRatio {
    private String stat;     // Doit correspondre à "stat" (ex: "atk")
    private double percent;  // Doit correspondre à "percent" (ex: 25.0)
}