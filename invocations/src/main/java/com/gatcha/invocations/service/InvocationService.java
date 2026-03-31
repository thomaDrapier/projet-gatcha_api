package com.gatcha.invocations.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.gatcha.invocations.dto.PlayerDTO;
import com.gatcha.invocations.model.InvocationLog;
import com.gatcha.invocations.model.MonsterTemplate;
import com.gatcha.invocations.repository.InvocationLogRepository;
import com.gatcha.invocations.repository.MonsterTemplateRepository;

@Service
public class InvocationService {

    private final RestTemplate restTemplate;
    private final InvocationLogRepository logRepository;
    private final MonsterTemplateRepository templateRepository;

    public InvocationService(RestTemplate restTemplate, 
                             InvocationLogRepository logRepository, 
                             MonsterTemplateRepository templateRepository) {
        this.restTemplate = restTemplate;
        this.logRepository = logRepository;
        this.templateRepository = templateRepository;
    }

    public Map<String, Object> performCompleteInvocation(String username, String token) throws Exception {
        
        // --- VÉRIFICATION DE LA LIMITE D'INVENTAIRE ---
        System.out.println(">>> [DEBUG INVENTAIRE] Début de la vérification pour le joueur : " + username);
        try {
            String playerUrl = "http://player-service:8082/player/{username}";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<PlayerDTO> response = restTemplate.exchange(
                playerUrl, 
                HttpMethod.GET, 
                requestEntity, 
                PlayerDTO.class, 
                username
            );
            
            PlayerDTO player = response.getBody();

            if (player != null) {
                int maxCapacity = 2 + player.getLevel();
                int currentMonsterCount = (player.getMonsters() != null) ? player.getMonsters().size() : 0;
                
                System.out.println("========== DEBUG INVOCATION ==========");
                System.out.println("Joueur : " + player.getUsername());
                System.out.println("Compte : " + currentMonsterCount + " / " + maxCapacity);
                System.out.println("======================================");

                if (currentMonsterCount >= maxCapacity) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Inventaire plein ! Limite actuelle : " + maxCapacity + " monstres.");
                }
            }
        } catch (ResponseStatusException e) {
            throw e; 
        } catch (Exception e) {
            System.err.println(">>> [DEBUG INVENTAIRE] CRASH : " + e.getMessage());
            throw new Exception("Erreur de communication avec le service Joueur.");
        }

        // A. Tirage aléatoire
        MonsterTemplate selected = this.drawMonster(); 

        // B. Log Initial
        InvocationLog log = saveInitialLog(username, selected.getId());

        try {
            // --- C. Appel API Monster ---
            String monsterUrl = "http://monster-service:8084/monsters/create";

            // 1. Préparation des données JSON (Map<String, Object> pour supporter les nombres et les listes)
            Map<String, Object> monsterPayload = new java.util.HashMap<>();
            monsterPayload.put("templateId", String.valueOf(selected.getId()));
            monsterPayload.put("ownerUsername", username);
            
            // On ajoute TOUTES les vraies stats et les attaques
            monsterPayload.put("element", selected.getElement());
            monsterPayload.put("hp", selected.getHp());
            monsterPayload.put("atk", selected.getAtk());
            monsterPayload.put("def", selected.getDef());
            monsterPayload.put("vit", selected.getVit());
            monsterPayload.put("skills", selected.getSkills());

            // 2. Préparation des Headers
            HttpHeaders monsterHeaders = new HttpHeaders();
            monsterHeaders.setContentType(MediaType.APPLICATION_JSON);
            monsterHeaders.set("Authorization", token); 

            // 3. Création de la requête finale
            HttpEntity<Map<String, Object>> monsterRequest = new HttpEntity<>(monsterPayload, monsterHeaders);

            // 4. Envoi de la requête
            String monsterInstanceId = restTemplate.postForObject(monsterUrl, monsterRequest, String.class);
            // -----------------------------------------------------------

            log.setStatus("MONSTER_CREATED");
            log.setMonsterInstanceId(monsterInstanceId);
            logRepository.save(log);

            // D. Appel API Player
            String addMonsterUrl = "http://player-service:8082/player/" + username + "/add-monster-internal/" + monsterInstanceId;
            restTemplate.put(addMonsterUrl, null);

            log.setStatus("COMPLETED");
            logRepository.save(log);

            return Map.of(
                "templateId", selected.getId(),
                "instanceId", monsterInstanceId
            );

        } catch (Exception e) {
            throw new Exception("Invocation interrompue : " + e.getMessage());
        }
    }

    private MonsterTemplate drawMonster() throws Exception {
        List<MonsterTemplate> pool = templateRepository.findAll();
        if (pool.isEmpty()) throw new Exception("Le catalogue de monstres est vide !");

        double totalWeight = pool.stream().mapToDouble(MonsterTemplate::getLootRate).sum();
        double randomValue = Math.random() * totalWeight;
        double cumulativeSum = 0;

        for (MonsterTemplate template : pool) {
            cumulativeSum += template.getLootRate();
            if (randomValue <= cumulativeSum) return template;
        }
        return pool.get(0);
    }

    private InvocationLog saveInitialLog(String username, int templateId) {
        InvocationLog log = new InvocationLog();
        log.setTransactionId(UUID.randomUUID().toString());
        log.setUsername(username);
        log.setSelectedTemplateId(templateId);
        log.setStatus("STARTED");
        log.setTimestamp(LocalDateTime.now());
        return logRepository.save(log);
    }
}