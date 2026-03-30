package com.gatcha.invocations.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.gatcha.invocations.model.InvocationLog;
import com.gatcha.invocations.model.MonsterTemplate;
import com.gatcha.invocations.repository.InvocationLogRepository;
import com.gatcha.invocations.repository.MonsterTemplateRepository;

@Service
public class InvocationService {

    private final RestTemplate restTemplate;
    private final InvocationLogRepository logRepository;
    private final MonsterTemplateRepository templateRepository;

    // Constructeur pour l'injection de dépendances
    public InvocationService(RestTemplate restTemplate, 
                             InvocationLogRepository logRepository, 
                             MonsterTemplateRepository templateRepository) {
        this.restTemplate = restTemplate;
        this.logRepository = logRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Orchestre l'invocation complète : Tirage -> Log -> Création Monstre -> Liaison Joueur
     */
    public MonsterTemplate performCompleteInvocation(String username) throws Exception {
        // A. Tirage aléatoire pondéré (ton algorithme validé)
        MonsterTemplate selected = this.drawMonster(); 

        // B. Création du Log Initial (Statut: STARTED) pour la résilience
        InvocationLog log = saveInitialLog(username, selected.getId());

        try {
            // C. Appel API Monster (Port 8083)
            // On envoie le templateId pour générer une instance unique
            String monsterUrl = "http://localhost:8083/monsters/create?templateId=" + selected.getId() + "&owner=" + username;
            
            // On récupère l'ID de l'instance créée par l'API Monster
            String monsterInstanceId = restTemplate.postForObject(monsterUrl, null, String.class);
            
            // Mise à jour du log : étape 1 réussie
            log.setStatus("MONSTER_CREATED");
            log.setMonsterInstanceId(monsterInstanceId);
            logRepository.save(log);

            // D. Appel API Player (Port 8082)
            // On ajoute l'ID de l'instance dans la liste du joueur
            String playerUrl = "http://localhost:8082/players/" + username + "/add-monster/" + monsterInstanceId;
            restTemplate.put(playerUrl, null);

            // Mise à jour du log : Terminé
            log.setStatus("COMPLETED");
            logRepository.save(log);

            return selected;

        } catch (Exception e) {
            // En cas d'erreur (réseau ou autre), le log garde son dernier statut (STARTED ou MONSTER_CREATED)
            // Cela permet de reprendre le processus plus tard sans refaire le tirage.
            throw new Exception("Invocation interrompue : " + e.getMessage());
        }
    }

    /**
     * Logique de tirage aléatoire pondéré
     */
    private MonsterTemplate drawMonster() throws Exception {
        List<MonsterTemplate> pool = templateRepository.findAll();
        if (pool.isEmpty()) throw new Exception("Le catalogue de monstres est vide !");

        double totalWeight = pool.stream().mapToDouble(MonsterTemplate::getLootRate).sum();
        double randomValue = Math.random() * totalWeight;
        double cumulativeSum = 0;

        for (MonsterTemplate template : pool) {
            cumulativeSum += template.getLootRate();
            if (randomValue <= cumulativeSum) {
                return template;
            }
        }
        return pool.get(0);
    }

    /**
     * Initialise le journal d'invocation dans la base tampon
     */
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