package combat.combat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
@Tag(name = "API Combat ⚔️", description = "Moteur de simulation de combats et gestion des rediffusions.")
public class BattleController {

    private final BattleService battleService;
    private final BattleRepository battleRepository;

    public BattleController(BattleService battleService, BattleRepository battleRepository) {
        this.battleService = battleService;
        this.battleRepository = battleRepository;
    }

    @Operation(summary = "Lancer un nouveau combat")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Le combat s'est déroulé avec succès."),
        @ApiResponse(responseCode = "400", description = "Impossible de lancer le combat.")
    })
    @PostMapping("/start")
    public ResponseEntity<?> startBattle(
            @Parameter(description = "L'ID du premier monstre") @RequestParam String monster1Id, 
            @Parameter(description = "L'ID du second monstre") @RequestParam String monster2Id,
            @RequestHeader("Authorization") String token) { // <-- AJOUT DU TOKEN ICI
        try {
            // On passe le token au service
            Battle result = battleService.startBattle(monster1Id, monster2Id, token);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors du combat : " + e.getMessage());
        }
    }

    @Operation(summary = "Consulter l'historique global")
    @GetMapping("/history")
    public ResponseEntity<List<Battle>> getBattleHistory() {
        return ResponseEntity.ok(battleRepository.findAll());
    }

    @Operation(summary = "Rediffusion d'un combat (Replay)")
    @GetMapping("/{id}")
    public ResponseEntity<Battle> getBattleReplay(@PathVariable String id) {
        return battleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}