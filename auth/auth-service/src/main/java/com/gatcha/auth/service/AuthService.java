package com.gatcha.auth.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gatcha.auth.model.User;
import com.gatcha.auth.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    public AuthService(UserRepository userRepository, CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    // INSCRIPTION
    public User register(String username, String password) throws Exception {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new Exception("Ce pseudo est déjà pris !");
        }
        // 1. On HASH le mot de passe avant de le donner à l'objet
        String hashedPassword = cryptoService.hashPassword(password);

        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword); // On sauvegarde le hash, pas le clair !

        // 2. Génération du token dès l'inscription (Auto-login)
        String token = cryptoService.generateToken(username);
        String tokenClear = cryptoService.decrypt(token);
        
        user.setToken(token);
        user.setToken_clear(tokenClear);

        return userRepository.save(user);
    }

    // LOGIN
    public String login(String username, String password) throws Exception {
        // 1. On cherche l'utilisateur
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // 2. On vérifie le mot de passe (en comparant les HASH)
            if (cryptoService.verifyPassword(password, user.getPassword())) {

                // 2.1. On génère le token crypté
                String token = cryptoService.generateToken(user.getUsername());
                
                // 2.2. On récupére la version en clair
                String tokenClear = cryptoService.decrypt(token);

                // 2.3. On enregistre en base de données
                user.setToken(token);
                user.setToken_clear(tokenClear); 
                userRepository.save(user);;

                return token;
            }
        }
        throw new Exception("Identifiants incorrects");
    }
    // VALIDATION DU TOKEN (Utilisateur + Durée de 1h)
    public Boolean validateToken (String token) throws Exception {
        // 1. Vérifier si le token existe en base (Affilié à un utilisateur)
        Optional<User> userOptional = userRepository.findByToken(token);
        if (userOptional.isEmpty()) {
            throw new Exception("Token invalide");
        }
        // 2. Vérifier la durée de validité (1 heure max)
        try {
            // Le format est : PSEUDO-YYYY/MM/DD-HH:mm:ss
            // La date fait toujours 19 caractères à la fin.
            if (token.length() < 19) {
                throw new Exception("Token invalide");
            }

            String datePart = token.substring(token.length() - 19);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
            LocalDateTime tokenDate = LocalDateTime.parse(datePart, formatter);
            LocalDateTime expirationDate = tokenDate.plusHours(1);

            // Si MAINTENANT est APRES la date limite -> Erreur
            if (LocalDateTime.now().isAfter(expirationDate)) {
                throw new Exception("Token invalide : Le token a expiré (plus d'une heure).");
            }

        } catch (Exception e) {
            // Si le parsing de la date échoue ou si c'est expiré
            throw new Exception("Token invalide : " + e.getMessage());
        }

        // Si on arrive ici, tout est bon !
        return true;
    }
}