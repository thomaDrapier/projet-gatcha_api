package combat.combat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import combat.combat.dto.MonsterDTO;
import combat.combat.dto.SkillDTO;
import combat.combat.dto.SkillRatio;
import combat.combat.model.Battle;
import combat.combat.model.BattleStep;
import combat.combat.repository.BattleRepository;

@Service
public class BattleService {

    private final BattleRepository battleRepository;
    private final RestTemplate restTemplate;
    private final String MONSTER_API_URL = "http://monster-service:8084/monsters/";
    private final Random random = new Random();
    private final String PLAYER_API_URL = "http://player-service:8082/player/"; // AJOUT ICI

    public BattleService(BattleRepository battleRepository) {
        this.battleRepository = battleRepository;
        this.restTemplate = new RestTemplate();
    }

    // --- CORRECTION : Ajout du paramètre "token" ---
    // ... (début du fichier identique) ...

    public Battle startBattle(String monster1Id, String monster2Id, String token) throws Exception {
        System.out.println("[BATTLE] Tentative de combat entre " + monster1Id + " et " + monster2Id);

        MonsterDTO m1, m2;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<MonsterDTO> res1 = restTemplate.exchange(
                MONSTER_API_URL + monster1Id, HttpMethod.GET, entity, MonsterDTO.class);
            m1 = res1.getBody();

            ResponseEntity<MonsterDTO> res2 = restTemplate.exchange(
                MONSTER_API_URL + monster2Id, HttpMethod.GET, entity, MonsterDTO.class);
            m2 = res2.getBody();

        } catch (Exception e) {
            throw new Exception("Impossible de contacter le service Monstre : " + e.getMessage());
        }

        if (m1 == null || m2 == null) {
            throw new Exception("Un des monstres est introuvable");
        }

        if (m1.getHp() <= 0) m1.setHp(100);
        if (m2.getHp() <= 0) m2.setHp(100);
        if (m1.getAtk() <= 0) m1.setAtk(10);
        if (m2.getAtk() <= 0) m2.setAtk(10);

        Battle battle = new Battle();
        battle.setMonster1Id(monster1Id);
        battle.setMonster2Id(monster2Id);
        battle.setReplayLogs(new ArrayList<>()); 

        int hp1 = m1.getHp();
        int hp2 = m2.getHp();
        
        String m1DisplayName = "Niv " + m1.getLevel() + " - " + m1.getTemplateId() + " - " + m1.getOwnerUsername();
        String m2DisplayName = "Niv " + m2.getLevel() + " - " + m2.getTemplateId() + " - " + m2.getOwnerUsername();

        Map<String, Integer> cd1 = initCooldowns(m1.getSkills());
        Map<String, Integer> cd2 = initCooldowns(m2.getSkills());

        int turn = 1;
        while (hp1 > 0 && hp2 > 0 && turn < 100) {
            // CORRECTION : On passe m1.getId()
            hp2 = executeTurn(turn, m1, m2, m1.getId(), m1DisplayName, m2DisplayName, hp2, cd1, battle.getReplayLogs());
            if (hp2 <= 0) {
                battle.setWinnerMonsterId(m1DisplayName);
                break;
            }

            // CORRECTION : On passe m2.getId()
            hp1 = executeTurn(turn, m2, m1, m2.getId(), m2DisplayName, m1DisplayName, hp1, cd2, battle.getReplayLogs());
            if (hp1 <= 0) {
                battle.setWinnerMonsterId(m2DisplayName);
                break;
            }
            turn++;
        }
        // --- LOGIQUE DE FIN DE COMBAT : DISTRIBUTION D'XP ---
        boolean m1Wins = hp1 > 0;
        
        // Configuration des gains (à équilibrer selon tes envies)
        int baseMonsterXp = 1000;
        double basePlayerXp = 100.0;
        
        int winnerMonsterXp = baseMonsterXp;
        int loserMonsterXp = (int)(baseMonsterXp * 0.3); // Le perdant gagne 30% d'XP
        
        double winnerPlayerXp = basePlayerXp;
        double loserPlayerXp = basePlayerXp * 0.3; // Le joueur perdant gagne 30% d'XP
        
        String winnerPlayerName = m1Wins ? m1.getOwnerUsername() : m2.getOwnerUsername();
        String loserPlayerName = m1Wins ? m2.getOwnerUsername() : m1.getOwnerUsername();

        // 1. Envoi de l'XP aux monstres
        sendMonsterXp(m1Wins ? monster1Id : monster2Id, winnerMonsterXp, token);
        sendMonsterXp(m1Wins ? monster2Id : monster1Id, loserMonsterXp, token);
        
        // 2. Envoi des résultats aux joueurs (XP + Compteur)
        sendPlayerBattleResult(winnerPlayerName, winnerPlayerXp);
        sendPlayerBattleResult(loserPlayerName, loserPlayerXp);
        // ----------------------------------------------------
        for (BattleStep step : battle.getReplayLogs()) {
            if (step.getCooldowns() != null) {
                step.getCooldowns().remove(null);
            }
        }

        System.out.println("[BATTLE] Combat terminé en " + turn + " tours. Sauvegarde...");
        return battleRepository.save(battle);
    }

    // CORRECTION : Ajout du paramètre attackerId
    private int executeTurn(int turn, MonsterDTO attacker, MonsterDTO defender, String attackerId, String attackerName, String defenderName, int defenderHp, Map<String, Integer> cooldowns, List<BattleStep> logs) {
        cooldowns.remove(null); 
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));

        List<SkillDTO> allSkills = attacker.getSkills() != null ? attacker.getSkills() : new ArrayList<>();
        
        List<SkillDTO> availableSkills = allSkills.stream()
                .filter(s -> cooldowns.getOrDefault(String.valueOf(s.getNum()), 0) == 0)
                .collect(Collectors.toList());

        SkillDTO chosenSkill;
        String displayName;
        String skillKey;

        if (availableSkills.isEmpty()) {
            chosenSkill = new SkillDTO();
            chosenSkill.setNum(-1);
            chosenSkill.setDmg(10);
            SkillRatio fallbackRatio = new SkillRatio();
            fallbackRatio.setStat("atk");
            fallbackRatio.setPercent(100);
            chosenSkill.setRatio(fallbackRatio);
            chosenSkill.setCooldown(0);
            displayName = "Attaque de base";
            skillKey = "base";
        } else {
            chosenSkill = availableSkills.get(random.nextInt(availableSkills.size()));
            displayName = "Skill " + chosenSkill.getNum();
            skillKey = String.valueOf(chosenSkill.getNum());
            cooldowns.put(skillKey, chosenSkill.getCooldown());
        }

        String statName = "atk";
        double multiplier = 1.0;

        if (chosenSkill.getRatio() != null) {
            statName = chosenSkill.getRatio().getStat();
            multiplier = chosenSkill.getRatio().getPercent() / 100.0;
        }

        int statValue = getStatValue(attacker, statName);
        int rawDamage = (int) (chosenSkill.getDmg() + (statValue * multiplier));
        int finalDamage = Math.max(1, rawDamage - (defender.getDef() / 2));
        
        defenderHp = Math.max(0, defenderHp - finalDamage);

        Map<String, Integer> safeCdSnapshot = new HashMap<>(cooldowns);
        safeCdSnapshot.remove(null);

        String desc = String.format("Tour %d : %s utilise %s ! Scaling %s (x%.2f). Dégâts : %d. PV restants : %d", 
                        turn, attackerName, displayName, statName, multiplier, finalDamage, defenderHp);

        // CORRECTION : On insère l'attackerId dans la création du log
        logs.add(new BattleStep(turn, attackerId, attackerName, displayName, finalDamage, defenderHp, safeCdSnapshot, desc));

        return defenderHp;
    }

    private int getStatValue(MonsterDTO monster, String statName) {
        if (statName == null) return monster.getAtk();
        switch (statName.toLowerCase()) {
            case "hp": return monster.getHp();
            case "def": return monster.getDef();
            case "vit": return monster.getVit();
            default: return monster.getAtk();
        }
    }

    private Map<String, Integer> initCooldowns(List<SkillDTO> skills) {
        Map<String, Integer> cds = new HashMap<>();
        if (skills != null) {
            for (SkillDTO s : skills) {
                cds.put(String.valueOf(s.getNum()), 0);
            }
        }
        return cds;
    }

    // --- METHODES UTILITAIRES POUR LES REQUETES HTTP ---

    private void sendMonsterXp(String monsterId, int xp, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token); // On transmet le token s'il est requis par MonsterController
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = MONSTER_API_URL + monsterId + "/add-xp?xp=" + xp;
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("[BATTLE] " + xp + " XP envoyés au monstre " + monsterId);
        } catch (Exception e) {
            System.err.println("[BATTLE] Erreur lors de l'envoi d'XP au monstre " + monsterId + " : " + e.getMessage());
        }
    }

    private void sendPlayerBattleResult(String username, double xp) {
        try {
            // Pas de token nécessaire car notre route est marquée comme (INTERNE)
            String url = PLAYER_API_URL + username + "/battle-result?xp=" + xp;
            restTemplate.exchange(url, HttpMethod.PUT, null, String.class);
            System.out.println("[BATTLE] Résultat de combat envoyé pour le joueur " + username + " (+" + xp + " XP)");
        } catch (Exception e) {
            System.err.println("[BATTLE] Erreur lors de la mise à jour du joueur " + username + " : " + e.getMessage());
        }
    }
}