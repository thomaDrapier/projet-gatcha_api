package com.gatcha.auth.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gatcha.auth.model.User;
import com.gatcha.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class) // Active Mockito pour simuler la DB
class AuthServiceTest {

    @Mock
    private UserRepository userRepository; // On simule le Repository (fausse DB)

    @Mock
    private CryptoService cryptoService;   // On simule le CryptoService

    @InjectMocks
    private AuthService authService;       // On injecte les mocks dans le vrai Service

    // ---------------------------------------------------------
    // TEST 1 : Login avec mauvais mot de passe
    // ---------------------------------------------------------
    @Test
    void login_ShouldThrowException_WhenPasswordIsWrong() {
        // ARRANGE (Préparation)
        String username = "Ash";
        String wrongPassword = "wrong";
        String realHash = "hash123";

        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPassword(realHash);

        // On dit à Mockito : "Si on cherche Ash, renvoie cet utilisateur"
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // On dit à Mockito : "Si on vérifie le mot de passe, dis que c'est faux"
        when(cryptoService.verifyPassword(wrongPassword, realHash)).thenReturn(false);

        // ACT & ASSERT (Action et Vérification)
        // On vérifie que la méthode lance bien une Exception avec le bon message
        Exception exception = assertThrows(Exception.class, () -> {
            authService.login(username, wrongPassword);
        });

        assertEquals("Identifiants incorrects", exception.getMessage());
    }

    // ---------------------------------------------------------
    // TEST 2 : Token inconnu (non associé à un user)
    // ---------------------------------------------------------
    @Test
    void validateToken_ShouldThrowException_WhenTokenUnknown() {
        // ARRANGE
        String fakeToken = "FakeToken-123";

        // On dit à Mockito : "Si on cherche ce token, on ne trouve rien (Empty)"
        when(userRepository.findByToken(fakeToken)).thenReturn(Optional.empty());

        // ACT & ASSERT
        Exception exception = assertThrows(Exception.class, () -> {
            authService.validateToken(fakeToken);
        });

        // On vérifie que le message contient "Inconnu" ou "invalide"
        assertTrue(exception.getMessage().contains("invalide"));
    }

    // ---------------------------------------------------------
    // TEST 3 : Token expiré (Date > 1h)
    // ---------------------------------------------------------
    @Test
    void validateToken_ShouldThrowException_WhenTokenIsExpired() {
        // ARRANGE
        // On fabrique un token périmé (il y a 2 heures)
        LocalDateTime pastDate = LocalDateTime.now().minusHours(2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
        String expiredToken = "Ash-" + pastDate.format(formatter);

        // Il faut que le token soit trouvé en base pour atteindre la vérification de date
        when(userRepository.findByToken(expiredToken)).thenReturn(Optional.of(new User()));

        // ACT & ASSERT
        Exception exception = assertThrows(Exception.class, () -> {
            authService.validateToken(expiredToken);
        });

        assertTrue(exception.getMessage().contains("invalide"));
    }

    // ---------------------------------------------------------
    // TEST 4 : Token Valide (Créé il y a 30 min)
    // ---------------------------------------------------------
    @Test
    void validateToken_ShouldReturnTrue_WhenTokenIsRecent() throws Exception {
        // ARRANGE (Préparation)
        // 1. On fabrique un token daté d'il y a 30 minutes
        LocalDateTime recentDate = LocalDateTime.now().minusMinutes(30);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd-HH:mm:ss");
        String validToken = "Ash-" + recentDate.format(formatter);

        // 2. IMPORTANT : On dit à Mockito que ce token est bien présent en base
        // Si on oublie ça, la méthode renverra une erreur "Token inconnu" avant même de tester la date.
        when(userRepository.findByToken(validToken)).thenReturn(Optional.of(new User()));

        // ACT (Action)
        boolean result = authService.validateToken(validToken);

        // ASSERT (Vérification)
        assertTrue(result, "Le token devrait être valide car il a moins d'une heure (30 min)");
    }
}

