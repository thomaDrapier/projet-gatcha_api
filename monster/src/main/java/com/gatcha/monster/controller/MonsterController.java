package com.gatcha.monster.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gatcha.monster.model.MonsterInstance;
import com.gatcha.monster.repository.MonsterRepository;

@RestController
@RequestMapping("/monsters")
public class MonsterController {

    private final MonsterRepository repository;

    public MonsterController(MonsterRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/create")
    public String createMonster(@RequestParam int templateId, @RequestParam String owner) {
        MonsterInstance instance = new MonsterInstance();
        instance.setTemplateId(templateId);
        instance.setOwner(owner);
        
        // On sauvegarde et on renvoie l'ID généré pour que l'API Invocation puisse le noter
        MonsterInstance saved = repository.save(instance);
        return saved.getId();
    }
}