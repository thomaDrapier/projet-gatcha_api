package com.gatcha.invocations.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.gatcha.invocations.service.InvocationService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/invocation")
public class InvocationController {

    private final InvocationService invocationService;

    public InvocationController(InvocationService invocationService) {
        this.invocationService = invocationService;
    }

    @PostMapping("/{username}")
    public ResponseEntity<?> summonMonster(
            @PathVariable String username,
            @RequestHeader("Authorization") String token) {

        System.out.println("\n--- DÉBUT DE L'INVOCATION POUR : " + username + " ---");

        try {
            // CORRECTION ICI : On passe BIEN les deux arguments : username ET token
            // C'est la ligne [38] qui faisait planter ton build Maven
            Map<String, Object> result = invocationService.performCompleteInvocation(username, token);

            System.out.println("--- FIN DE L'INVOCATION RÉUSSIE ---");
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "templateId", result.get("templateId"),
                    "instanceId", result.get("instanceId")
            ));

        } catch (ResponseStatusException e) {
            System.err.println("❌ INVOCATION REFUSÉE : " + e.getReason());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "error", "Invocation impossible",
                "message", e.getReason()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ EXCEPTION GLOBALE : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Crash de l'invocation",
                "message", e.getMessage()
            ));
        }
    }
}