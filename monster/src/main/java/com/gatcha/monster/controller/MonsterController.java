package com.gatcha.monster.controller;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin; // <--- On importe Monster, PAS MonsterInstance
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gatcha.monster.model.Monster;
import com.gatcha.monster.repository.MonsterRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/monsters")
public class MonsterController {

    private final MonsterRepository repository;

    public MonsterController(MonsterRepository repository) {
        this.repository = repository;
    }

    // On utilise Monster au lieu de MonsterInstance
    @PostMapping("/create")
    public String createMonster(@RequestBody Monster monster) {
        
        // Sécurité : Nouveau monstre
        monster.setId(null); 
        monster.setLevel(1);
        monster.setExperience(0.0);
        
        // Sauvegarde via le repository qui gère les objets Monster
        Monster saved = repository.save(monster);
        
        // On retourne l'ID unique
        return saved.getId();
    }

    // Endpoint utile pour le Front-end plus tard
    @GetMapping("/{id}")
    public ResponseEntity<Monster> getMonsterById(@PathVariable String id) {
        Optional<Monster> monster = repository.findById(id);
        return monster.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }
}