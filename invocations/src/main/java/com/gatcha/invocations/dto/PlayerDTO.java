package com.gatcha.invocations.dto;

import java.util.List;

public class PlayerDTO {
    private String username;
    private int level;
    private List<String> monsters;

    // Getters et Setters
    public String getUsername() { 
        return username; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }
    
    public int getLevel() { 
        return level; 
    }
    
    public void setLevel(int level) { 
        this.level = level; 
    }
    
    public List<String> getMonsters() { 
        return monsters; 
    }
    
    public void setMonsters(List<String> monsters) { 
        this.monsters = monsters; 
    }
}