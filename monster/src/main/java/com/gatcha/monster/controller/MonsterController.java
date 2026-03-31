package com.gatcha.monster.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin; 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gatcha.monster.model.Monster;
import com.gatcha.monster.repository.MonsterRepository;
import com.gatcha.monster.service.MonsterService;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
@RestController
@RequestMapping("/monsters")
public class MonsterController {

    private final MonsterRepository repository;
    private final MonsterService monsterService;

    public MonsterController(MonsterRepository repository, MonsterService monsterService) {
        this.repository = repository;
        this.monsterService = monsterService;
    }

    @PostMapping("/create")
    public String createMonster(@RequestBody Monster monster) {
        // --- CORRECTION : On délègue la création au Service pour générer les stats ! ---
        Monster saved = monsterService.createMonster(monster);
        return saved.getId();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Monster> getMonsterById(@PathVariable String id) {
        Optional<Monster> monster = repository.findById(id);
        return monster.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Iterable<Monster>> getAllMonsters() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/{id}/add-xp")
    public ResponseEntity<?> addXp(@PathVariable String id, @RequestParam int xp) {
        try {
            Monster updatedMonster = monsterService.addXp(id, xp);
            return ResponseEntity.ok(updatedMonster);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout d'XP : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Erreur: " + e.getMessage());
        }
    }
}