package com.gatcha.auth.controller;

import com.gatcha.auth.model.User;
import com.gatcha.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// La classe sert à répondre à des requêtes Web
@RestController
// Préfixe de l'url sera http://localhost:8081/auth
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Endpoint temporaire pour créer un user facilement
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        return ResponseEntity.ok(authService.register(user.getUsername(), user.getPassword()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            String token = authService.login(username, password);

            // On renvoie le token dans un objet JSON
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String token) {
        try {
            // Petite sécurité : si le token arrive avec "Bearer ", on nettoie
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // On tente de valider.
            // Si c'est invalide, ça va sauter directement dans le "catch" plus bas.
            authService.validateToken(token);

            // Si on arrive ici, c'est que tout s'est bien passé
            return ResponseEntity.ok(Map.of("status", "valid"));

        } catch (Exception e) {
            // Si une erreur a explosé dans le Service (Date expirée, token inconnu...), on atterrit ici.
            // On renvoie une 401 (Unauthorized) avec le message d'erreur précis
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}