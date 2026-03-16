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

        // 1. NETTOYAGE : Swagger ou les navigateurs ajoutent souvent "Bearer " devant le token.
        // Si c'est le cas, on le retire pour ne garder que le texte crypté.
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            System.out.println("--- DÉBUT VÉRIFICATION TOKEN ---");
            System.out.println("Envoi du token à l'API Auth : " + token);

            // On prépare le body JSON : { "token": "ton_super_token" }
            Map<String, String> body = Map.of("token", token);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body);

            // On appelle l'API Auth en méthode POST
            ResponseEntity<Map> response = restTemplate.postForEntity(AUTH_API_URL, request, Map.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception("Accès refusé par le serveur d'authentification.");
            }
            
            System.out.println("Token validé avec succès par l'API Auth !");
            System.out.println("--- FIN VÉRIFICATION ---");
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // C'EST ICI LA MAGIE : On récupère le vrai message d'erreur de l'API Auth !
            System.out.println("L'API Auth a rejeté la demande. Vrai motif : " + e.getResponseBodyAsString());
            throw new Exception("Token invalide : " + e.getResponseBodyAsString());
            
        } catch (Exception e) {
            System.out.println("Impossible de joindre l'API Auth : " + e.getMessage());
            throw new Exception("Erreur de communication avec le serveur d'authentification.");
        }
    }
}
