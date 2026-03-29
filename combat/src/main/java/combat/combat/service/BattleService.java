package combat.combat.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import combat.combat.dto.MonsterDTO;
import combat.combat.dto.SkillDTO;
import combat.combat.model.Battle;
import combat.combat.model.BattleStep;
import combat.combat.repository.BattleRepository;

@Service
public class BattleService {

    private final BattleRepository battleRepository;
    private final RestTemplate restTemplate;
    // URL du service monstre dans le réseau Docker
    private final String MONSTER_API_URL = "http://monster-service:8084/monsters/";
    private final Random random = new Random();

    public BattleService(BattleRepository battleRepository) {
        this.battleRepository = battleRepository;
        this.restTemplate = new RestTemplate();
    }

    public Battle startBattle(String monster1Id, String monster2Id) throws Exception {
        // 1. Récupérer les stats fraîches des deux monstres
        MonsterDTO m1 = restTemplate.getForObject(MONSTER_API_URL + monster1Id, MonsterDTO.class);
        MonsterDTO m2 = restTemplate.getForObject(MONSTER_API_URL + monster2Id, MonsterDTO.class);

        if (m1 == null || m2 == null) {
            throw new Exception("Un des monstres est introuvable");
        }

        Battle battle = new Battle();
        battle.setMonster1Id(monster1Id);
        battle.setMonster2Id(monster2Id);

        // 2. Initialiser les PV et les Cooldowns (tous à 0 au tour 1)
        int hp1 = m1.getHp();
        int hp2 = m2.getHp();
        Map<String, Integer> cd1 = initCooldowns(m1.getSkills());
        Map<String, Integer> cd2 = initCooldowns(m2.getSkills());

        int turn = 1;
        
        // 3. Boucle du combat
        while (hp1 > 0 && hp2 > 0) {
            // Tour du Monstre 1
            hp2 = executeTurn(turn, m1, m2, hp2, cd1, battle.getReplayLogs());
            if (hp2 <= 0) {
                battle.setWinnerMonsterId(m1.getId());
                break;
            }

            // Tour du Monstre 2
            hp1 = executeTurn(turn, m2, m1, hp1, cd2, battle.getReplayLogs());
            if (hp1 <= 0) {
                battle.setWinnerMonsterId(m2.getId());
                break;
            }
            turn++;
        }

        // 4. Sauvegarder l'historique complet pour la rediffusion
        return battleRepository.save(battle);
    }

    private int executeTurn(int turn, MonsterDTO attacker, MonsterDTO defender, int defenderHp, Map<String, Integer> cooldowns, List<BattleStep> logs) {
        // A. Diminuer tous les temps de recharge de 1
        cooldowns.replaceAll((k, v) -> Math.max(0, v - 1));

        // B. Trouver les attaques disponibles (CD == 0)
        List<SkillDTO> availableSkills = attacker.getSkills().stream()
                .filter(s -> cooldowns.getOrDefault(s.getName(), 0) == 0)
                .collect(Collectors.toList());

        // C. Choisir une attaque (aléatoire parmi celles dispo, ou attaque de base si tout est en CD)
        SkillDTO chosenSkill = new SkillDTO();
        if (availableSkills.isEmpty()) {
            chosenSkill.setName("Attaque Basique");
            chosenSkill.setPower(5);
            chosenSkill.setCooldown(0);
        } else {
            chosenSkill = availableSkills.get(random.nextInt(availableSkills.size()));
            // Remettre le cooldown au max pour cette attaque
            cooldowns.put(chosenSkill.getName(), chosenSkill.getCooldown());
        }

        // D. Calcul des dégâts
        int finalDamage = Math.max(1, chosenSkill.getPower() + attacker.getAtk() - defender.getDef());
        defenderHp = Math.max(0, defenderHp - finalDamage);

        // E. Créer le log pour l'historique
        String desc = "Monstre " + attacker.getId() + " utilise " + chosenSkill.getName() + " et inflige " + finalDamage + " dégâts.";
        logs.add(new BattleStep(turn, attacker.getId(), chosenSkill.getName(), finalDamage, defenderHp, new HashMap<>(cooldowns), desc));

        return defenderHp;
    }

    private Map<String, Integer> initCooldowns(List<SkillDTO> skills) {
        Map<String, Integer> cds = new HashMap<>();
        if (skills != null) {
            for (SkillDTO s : skills) {
                cds.put(s.getName(), 0);
            }
        }
        return cds;
    }
}