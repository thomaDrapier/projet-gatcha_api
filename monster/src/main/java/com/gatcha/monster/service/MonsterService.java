package com.gatcha.monster.service;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.gatcha.monster.model.Monster;
import com.gatcha.monster.repository.MonsterRepository;

@Service
public class MonsterService {

    private final MonsterRepository monsterRepository;
    private final Random random = new Random();

    public MonsterService(MonsterRepository monsterRepository) {
        this.monsterRepository = monsterRepository;
    }

    public Monster createMonster(String templateId) {
        Monster monster = new Monster();
        monster.setTemplateId(templateId);
        monster.setLevel(1);
        // Génération de stats aléatoires de base
        monster.setHp(50 + random.nextInt(51)); // 50-100
        monster.setAtk(10 + random.nextInt(11)); // 10-20
        monster.setDef(5 + random.nextInt(6));   // 5-10
        monster.setVit(5 + random.nextInt(16));  // 5-20
        return monsterRepository.save(monster);
    }

    public Monster getMonsterById(String id) {
        return monsterRepository.findById(id).orElse(null);
    }

    public List<Monster> getAllMonsters() {
        return monsterRepository.findAll();
    }
}