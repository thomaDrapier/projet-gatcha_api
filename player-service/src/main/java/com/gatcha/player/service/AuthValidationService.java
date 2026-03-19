package com.gatcha.player.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthValidationService {

    private final String AUTH_API_URL = "http://auth-service:8081/auth/validate";
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

            // 1. On crée les Headers HTTP
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            // On remet "Bearer " devant le token car c'est le standard attendu dans un header Authorization
            // headers.set("Authorization", "Bearer " + token);

            // 2. On crée l'entité de la requête (On y met les headers, sans body)
            org.springframework.http.HttpEntity<Void> request = new org.springframework.http.HttpEntity<>(headers);

            // 3. On utilise 'exchange' au lieu de 'postForEntity' pour pouvoir envoyer des Headers personnalisés
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                    AUTH_API_URL, 
                    org.springframework.http.HttpMethod.POST, 
                    request, 
                    Map.class
            );

            if (response.getStatusCode() != org.springframework.http.HttpStatus.OK) {
                throw new Exception("Accès refusé par le serveur d'authentification.");
            }
            
            System.out.println("Token validé avec succès par l'API Auth !");
            System.out.println("--- FIN VÉRIFICATION ---");
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("L'API Auth a rejeté la demande. Vrai motif : " + e.getResponseBodyAsString());
            throw new Exception("Token invalide : " + e.getResponseBodyAsString());
            
        } catch (Exception e) {
            System.out.println("Impossible de joindre l'API Auth : " + e.getMessage());
            throw new Exception("Erreur de communication avec le serveur d'authentification.");
        }
    }
}
