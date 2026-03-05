package com.gatcha.player.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthValidationService {

    private final String AUTH_API_URL = "http://localhost:8081/auth/validate";
    private final RestTemplate restTemplate = new RestTemplate();

    public void checkToken(String token) throws Exception {
        if (token == null || token.isEmpty()) {
            throw new Exception("Token d'authentification manquant.");
        }

        try {
            // 1. On prépare le body JSON : { "token": "ton_super_token" }
            Map<String, String> body = Map.of("token", token);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body);

            // 2. On appelle l'API Auth en méthode POST
            ResponseEntity<Map> response = restTemplate.postForEntity(AUTH_API_URL, request, Map.class);

            // 3. Si l'API Auth ne répond pas 200 OK, on rejette
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("Accès refusé par le serveur d'authentification.");
            }
            
        } catch (Exception e) {
            // Si le RestTemplate attrape une erreur 401 (ex: token expiré ou utilisateur supprimé)
            throw new Exception("Token invalide ou expiré. Veuillez vous reconnecter.");
        }
    }
}
