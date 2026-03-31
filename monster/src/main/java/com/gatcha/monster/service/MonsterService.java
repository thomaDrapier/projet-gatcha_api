package com.gatcha.monster.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gatcha.monster.model.Monster;
import com.gatcha.monster.repository.MonsterRepository;

@Service
public class MonsterService {

    private final MonsterRepository monsterRepository;

    public MonsterService(MonsterRepository monsterRepository) {
        this.monsterRepository = monsterRepository;
    }

    public Monster createMonster(Monster newMonster) {
        newMonster.setId(null); // Sécurité
        newMonster.setLevel(1);
        newMonster.setXp(0);
        newMonster.setSkillPoints(0);
        
        // CORRECTION : On a supprimé la génération aléatoire.
        // newMonster possède DEJA ses vrais PV, ATK et SKILLS grâce au JSON reçu !
        
        return monsterRepository.save(newMonster);
    }

    public Monster getMonsterById(String id) {
        return monsterRepository.findById(id).orElse(null);
    }

    public List<Monster> getAllMonsters() {
        return monsterRepository.findAll();
    }

    public Monster addXp(String monsterId, int xpAmount) throws Exception {
        Monster monster = getMonsterById(monsterId);
        
        if (monster == null) {
            throw new Exception("Monstre introuvable avec l'ID : " + monsterId);
        }

        int currentXp = monster.getXp() + xpAmount;
        int currentLevel = monster.getLevel();
        int xpRequired = currentLevel * 1000;

        while (currentXp >= xpRequired) {
            currentXp -= xpRequired;
            currentLevel++;
            
            monster.setHp((int) (monster.getHp() * 1.10));
            monster.setAtk((int) (monster.getAtk() * 1.10));
            monster.setDef((int) (monster.getDef() * 1.10));
            monster.setVit((int) (monster.getVit() * 1.10));
            
            monster.setSkillPoints(monster.getSkillPoints() + 1);
            xpRequired = currentLevel * 1000;
        }

        monster.setXp(currentXp);
        monster.setLevel(currentLevel);

        return monsterRepository.save(monster);
    }
}