package com.gatcha.invocations.model;

import java.util.List; // Import important

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@Document(collection = "invocation_pool")
public class MonsterTemplate {

    @Id
    @JsonProperty("_id") // Indique à Jackson que "_id" dans le JSON = "id" en Java
    private int id;

    private String element;
    private int hp;
    private int atk;
    private int def;
    private int vit;
    private List<SkillTemplate> skills;

    @JsonProperty("lootRate") // Bonne pratique pour être explicite
    private double lootRate;
}

