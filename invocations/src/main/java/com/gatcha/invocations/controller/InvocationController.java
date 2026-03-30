package com.gatcha.invocations.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.gatcha.invocations.model.MonsterTemplate;
import com.gatcha.invocations.repository.MonsterTemplateRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/invocation")
public class InvocationController {

    private final MonsterTemplateRepository templateRepository;
    private final RestTemplate restTemplate;
    
    private final String MONSTER_SERVICE_URL = "http://monster-service:8084/monsters/create";
    private final String PLAYER_SERVICE_URL = "http://player-service:8082/player";

    public InvocationController(MonsterTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        this.restTemplate = new RestTemplate();
    }

    @PostMapping("/{username}")
    public ResponseEntity<?> summonMonster(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {

        System.out.println("\n--- DÉBUT DE L'INVOCATION POUR : " + username + " ---");

        try {
            // 1. Récupération du Pool
            List<MonsterTemplate> pool = templateRepository.findAll();
            System.out.println("DEBUG: Nombre de templates trouvés en base : " + pool.size());
            
            if (pool.isEmpty()) {
                System.err.println("ERREUR: La collection invocation_pool est vide !");
                return ResponseEntity.status(500).body(Map.of("error", "Base d'invocation vide"));
            }

            // 2. Tirage au sort
            MonsterTemplate wonMonster = drawMonster(pool);
            System.out.println("DEBUG: Monstre tiré : ID=" + wonMonster.getId() + ", Element=" + wonMonster.getElement());

            // 3. Préparation du payload
            Map<String, Object> newMonsterData = new HashMap<>();
            newMonsterData.put("templateId", wonMonster.getId());
            newMonsterData.put("ownerUsername", username); // Vérifie si ton Monster Service attend "owner" ou "ownerUsername"
            newMonsterData.put("element", wonMonster.getElement());
            newMonsterData.put("hp", wonMonster.getHp());
            newMonsterData.put("atk", wonMonster.getAtk());
            newMonsterData.put("def", wonMonster.getDef());
            newMonsterData.put("vit", wonMonster.getVit());
            newMonsterData.put("skills", wonMonster.getSkills());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> monsterRequest = new HttpEntity<>(newMonsterData, headers);

            // 4. Appel MONSTER SERVICE
            System.out.println("DEBUG: Appel Monster Service à : " + MONSTER_SERVICE_URL);
            String generatedInstanceId;
            try {
                generatedInstanceId = restTemplate.postForObject(MONSTER_SERVICE_URL, monsterRequest, String.class);
                System.out.println("DEBUG: Réponse Monster Service (ID généré) : " + generatedInstanceId);
                
                if (generatedInstanceId == null) {
                    throw new Exception("Le Monster Service a renvoyé un ID null !");
                }
            } catch (HttpStatusCodeException e) {
                System.err.println("ERREUR HTTP MONSTER SERVICE: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Le Monster Service a rejeté la requête", "details", e.getResponseBodyAsString()));
            }

            String playerUrl = PLAYER_SERVICE_URL + "/" + username + "/add-monster-internal/" + generatedInstanceId;
            System.out.println("DEBUG: Appel Player Service à : " + playerUrl);
            try {
                HttpEntity<Void> playerRequest = new HttpEntity<>(headers);
                restTemplate.exchange(playerUrl, HttpMethod.PUT, playerRequest, String.class);
                System.out.println("DEBUG: Ajout au joueur réussi !");
            } catch (HttpStatusCodeException e) {
                System.err.println("ERREUR HTTP PLAYER SERVICE: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "Le Player Service a rejeté l'ajout", "details", e.getResponseBodyAsString()));
            }

            System.out.println("--- FIN DE L'INVOCATION RÉUSSIE ---");
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "templateId", wonMonster.getId(),
                    "instanceId", generatedInstanceId
            ));

        } catch (Exception e) {
            System.err.println("❌ EXCEPTION GLOBALE : " + e.getClass().getSimpleName() + " -> " + e.getMessage());
            e.printStackTrace(); // Affiche la pile d'erreur complète dans la console IDE
            return ResponseEntity.status(500).body(Map.of(
                "error", "Crash de l'invocation",
                "message", e.getMessage(),
                "type", e.getClass().getSimpleName()
            ));
        }
    }

    private MonsterTemplate drawMonster(List<MonsterTemplate> pool) {
        double roll = Math.random();
        double cumulativeProbability = 0.0;
        for (MonsterTemplate template : pool) {
            cumulativeProbability += template.getLootRate();
            if (roll <= cumulativeProbability) return template;
        }
        return pool.get(pool.size() - 1); 
    }
}