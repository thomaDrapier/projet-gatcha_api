package com.gatcha.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gatcha.auth.model.LoginRequest;
import com.gatcha.auth.model.User;
import com.gatcha.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@CrossOrigin(origins = "*")
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

    @Operation(
            summary = "Créer un nouveau compte",
            description = "Inscrit un nouvel utilisateur en base de données avec un pseudo et un mot de passe."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Utilisateur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou pseudo déjà pris")
    })

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // On essaie d'inscrire l'utilisateur
            User newUser = authService.register(user.getUsername(), user.getPassword());
            return ResponseEntity.ok(newUser);
            
        } catch (Exception e) {
            // Si l'erreur "Ce pseudo est déjà pris : on renvoie un code 400.
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Connexion d'un joueur",
            description = "Vérifie les identifiants (username et password) et retourne un token JWT d'authentification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie, retourne le Token JWT"),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects ou compte inexistant")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials) { // <-- Changement ici
        try {
            String username = credentials.getUsername();
            String password = credentials.getPassword();

            String token = authService.login(username, password);

            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
    @Operation(
            summary = "Valider un Token JWT (Sécurité interne)",
            description = "Vérifie si un token est toujours valide et n'a pas expiré. Utilisé par les autres microservices (comme Player Service) pour vérifier les droits."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Le token est valide"),
            @ApiResponse(responseCode = "401", description = "Le token est invalide, expiré ou malformé")
    })
    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String token) {
        try {
            // On tente de valider.
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }
            System.out.println("--- DÉBUT PROCESSUS VALIDATION TOKEN PAR AUTH SERVICE ---");
            System.out.println("Token reçu : " + token);
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