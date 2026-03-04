package com.gatcha.auth.service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class CryptoService {
    // Clé secrète
    private static final String SECRET_KEY = "MySuperSecretKey";
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
        try {
            // 1. On génère la date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);

            // 2. On assemble le texte en clair (ex: "Sacha-2026/03/04-14:30:00")
            String rawToken = username + "-" + timestamp;

            // 3. On retourne la version cryptée
            return encrypt(rawToken);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du token", e);
        }
    }

    // 3. Méthode pour vérifier un mot de passe
    // On hash le mot de passe reçu et on regarde s'il est identique à celui en base
    public boolean verifyPassword(String rawPassword, String storedHash) {
        return hashPassword(rawPassword).equals(storedHash);
    }

    private String encrypt(String data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encryptedData) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        
        return new String(decryptedBytes);
    }
}