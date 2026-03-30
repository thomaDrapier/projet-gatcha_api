package combat.combat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import combat.combat.model.Battle;
import combat.combat.repository.BattleRepository;
import combat.combat.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@CrossOrigin("*")
@RestController
@RequestMapping("/battle")
@Tag(name = "API Combat ⚔️", description = "Moteur de simulation de combats et gestion des rediffusions (replays).")
public class BattleController {

    private final BattleService battleService;
    private final BattleRepository battleRepository;

    public BattleController(BattleService battleService, BattleRepository battleRepository) {
        this.battleService = battleService;
        this.battleRepository = battleRepository;
    }

    @Operation(
        summary = "Lancer un nouveau combat", 
        description = "Simule un combat complet tour par tour entre deux monstres en gérant les cooldowns et l'aléatoire. Retourne le film complet du combat."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Le combat s'est déroulé avec succès et a été sauvegardé."),
        @ApiResponse(responseCode = "400", description = "Impossible de lancer le combat (ex: Monstre introuvable).")
    })
    @PostMapping("/start")
    public ResponseEntity<?> startBattle(
            @Parameter(description = "L'ID du premier monstre dans la base Monster", required = true) @RequestParam String monster1Id, 
            @Parameter(description = "L'ID du second monstre dans la base Monster", required = true) @RequestParam String monster2Id) {
        try {
            Battle result = battleService.startBattle(monster1Id, monster2Id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors du combat : " + e.getMessage());
        }
    }

    @Operation(
        summary = "Rediffusion d'un combat (Replay)", 
        description = "Récupère les détails d'un combat passé, incluant chaque action de chaque tour, les dégâts infligés et l'état des temps de recharge."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Battle> getBattleReplay(
            @Parameter(description = "L'ID unique du combat (généré par MongoDB)", required = true) @PathVariable String id) {
        return battleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Consulter l'historique global", 
        description = "Renvoie la liste absolue de tous les combats ayant eu lieu sur le serveur."
    )
    @GetMapping("/history")
    public ResponseEntity<List<Battle>> getBattleHistory() {
        return ResponseEntity.ok(battleRepository.findAll());
    }

    // Récupère juste la liste pour le menu déroulant
    @GetMapping("/history")
    public List<Battle> getAllBattles() {
        return battleRepository.findAll(); 
    }

    // Récupère un combat spécifique avec tous ses logs
    @GetMapping("/{id}")
    public ResponseEntity<Battle> getBattleById(@PathVariable String id) {
        return battleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}