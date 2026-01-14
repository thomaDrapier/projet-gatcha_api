package com.gatcha.auth.service;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CryptoService {

    // 1. Méthode pour HASHER le mot de passe (Sens unique)
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de hachage", e);
        }
    }

    // 2. Méthode pour générer un Token aléatoire (UUID)
    public String generateToken(String username) {
        // 1. On prépare le format de la date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");

        // 2. On récupère la date/heure actuelle
        String timestamp = LocalDateTime.now().format(formatter);

        // 3. On assemble le tout
        return username + "-" + timestamp;
    }

    // 3. Méthode pour vérifier un mot de passe
    // On hash le mot de passe reçu et on regarde s'il est identique à celui en base
    public boolean verifyPassword(String rawPassword, String storedHash) {
        return hashPassword(rawPassword).equals(storedHash);
    }
}