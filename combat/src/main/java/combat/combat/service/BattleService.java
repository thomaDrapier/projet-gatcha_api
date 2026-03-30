package combat.combat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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

    public BattleService(BattleRepository battleRepository) {
        this.battleRepository = battleRepository;
        this.restTemplate = new RestTemplate();
    }

    public Battle startBattle(String monster1Id, String monster2Id) throws Exception {
        System.out.println("[BATTLE] Tentative de combat entre " + monster1Id + " et " + monster2Id);

        MonsterDTO m1, m2;
        try {
            m1 = restTemplate.getForObject(MONSTER_API_URL + monster1Id, MonsterDTO.class);
            m2 = restTemplate.getForObject(MONSTER_API_URL + monster2Id, MonsterDTO.class);
        } catch (Exception e) {
            throw new Exception("Impossible de contacter le service Monstre : " + e.getMessage());
        }

        if (m1 == null || m2 == null) {
            throw new Exception("Un des monstres est introuvable");
        }

        Battle battle = new Battle();
        battle.setMonster1Id(monster1Id);
        battle.setMonster2Id(monster2Id);
        battle.setReplayLogs(new ArrayList<>()); 

        int hp1 = m1.getHp();
        int hp2 = m2.getHp();
        
        // --- CRÉATION DES NOMS D'AFFICHAGE FORMATÉS ---
        // Ex: "Niv 5 - Dragon - Player123"
        String m1DisplayName = "Niv " + m1.getLevel() + " - " + m1.getTemplateId() + " - " + m1.getOwnerUsername();
        String m2DisplayName = "Niv " + m2.getLevel() + " - " + m2.getTemplateId() + " - " + m2.getOwnerUsername();

        // Initialisation des cooldowns basée sur le numéro du skill
        Map<String, Integer> cd1 = initCooldowns(m1.getSkills());
        Map<String, Integer> cd2 = initCooldowns(m2.getSkills());

        int turn = 1;
        while (hp1 > 0 && hp2 > 0 && turn < 100) {
            // Tour Monstre 1
            hp2 = executeTurn(turn, m1, m2, m1DisplayName, m2DisplayName, hp2, cd1, battle.getReplayLogs());
            if (hp2 <= 0) {
                battle.setWinnerMonsterId(m1DisplayName); // On sauvegarde le beau nom comme vainqueur
                break;
            }

            // Tour Monstre 2
            hp1 = executeTurn(turn, m2, m1, m2DisplayName, m1DisplayName, hp1, cd2, battle.getReplayLogs());
            if (hp1 <= 0) {
                battle.setWinnerMonsterId(m2DisplayName); // On sauvegarde le beau nom comme vainqueur
                break;
            }
            turn++;
        }

        // --- SÉCURITÉ CRITIQUE POUR MONGODB ---
        for (BattleStep step : battle.getReplayLogs()) {
            if (step.getCooldowns() != null) {
                step.getCooldowns().remove(null);
            }
        }

        System.out.println("[BATTLE] Combat terminé. Sauvegarde en cours...");
        return battleRepository.save(battle);
    }

    // Mise à jour de la signature : Ajout de attackerName et defenderName
    private int executeTurn(int turn, MonsterDTO attacker, MonsterDTO defender, String attackerName, String defenderName, int defenderHp, Map<String, Integer> cooldowns, List<BattleStep> logs) {
        // 1. Décrémentation des cooldowns
        cooldowns.remove(null); 
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));

        List<SkillDTO> allSkills = attacker.getSkills() != null ? attacker.getSkills() : new ArrayList<>();
        
        // 2. Filtrer les skills utilisables (CD à 0)
        List<SkillDTO> availableSkills = allSkills.stream()
                .filter(s -> cooldowns.getOrDefault(String.valueOf(s.getNum()), 0) == 0)
                .collect(Collectors.toList());

        SkillDTO chosenSkill;
        String displayName;
        String skillKey;

        if (availableSkills.isEmpty()) {
            // --- ATTAQUE DE SECOURS ---
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
            // --- UTILISATION D'UN VRAI SKILL ---
            chosenSkill = availableSkills.get(random.nextInt(availableSkills.size()));
            displayName = "Skill " + chosenSkill.getNum();
            skillKey = String.valueOf(chosenSkill.getNum());
            
            // Activer le cooldown
            cooldowns.put(skillKey, chosenSkill.getCooldown());
        }

        // --- 3. CALCUL DES DÉGÂTS ---
        String statName = "atk";
        double multiplier = 1.0;

        if (chosenSkill.getRatio() != null) {
            statName = chosenSkill.getRatio().getStat();
            multiplier = chosenSkill.getRatio().getPercent() / 100.0;
        }

        int statValue = getStatValue(attacker, statName);
        
        // Formule : dmg (base) + (stat * ratio)
        int rawDamage = (int) (chosenSkill.getDmg() + (statValue * multiplier));
        
        // Réduction par la défense adverse (min 1 dégât)
        int finalDamage = Math.max(1, rawDamage - (defender.getDef() / 2));
        defenderHp = Math.max(0, defenderHp - finalDamage);

        // --- 4. PRÉPARATION DU LOG ---
        Map<String, Integer> safeCdSnapshot = new HashMap<>(cooldowns);
        safeCdSnapshot.remove(null);

        // Utilisation de attackerName (formaté) au lieu de attacker.getId()
        String desc = String.format("Tour %d : %s utilise %s ! Scaling %s (x%.2f). Dégâts : %d. PV restants : %d", 
                        turn, attackerName, displayName, statName, multiplier, finalDamage, defenderHp);

        // Sauvegarde de l'étape avec le nom formaté
        logs.add(new BattleStep(turn, attackerName, displayName, finalDamage, defenderHp, safeCdSnapshot, desc));

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
                // On initialise le CD à 0 pour chaque numéro de skill
                cds.put(String.valueOf(s.getNum()), 0);
            }
        }
        return cds;
    }
}