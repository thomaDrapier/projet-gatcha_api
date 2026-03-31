package com.gatcha.invocations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import com.gatcha.invocations.dto.PlayerDTO;
import com.gatcha.invocations.repository.InvocationLogRepository;
import com.gatcha.invocations.service.InvocationService;

@SpringBootTest
class InvocationServiceTest {

    @Autowired
    private InvocationService invocationService;

    @MockitoBean
    private InvocationLogRepository logRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        // 1. Simuler la réponse du Player Service (avec ResponseEntity pour .exchange)
        PlayerDTO mockPlayer = new PlayerDTO();
        mockPlayer.setUsername("testUser");
        mockPlayer.setLevel(10);
        mockPlayer.setMonsters(new ArrayList<>()); // Inventaire vide

        ResponseEntity<PlayerDTO> responseEntity = new ResponseEntity<>(mockPlayer, HttpStatus.OK);

        // Mock de l'appel restTemplate.exchange utilisé pour la vérification d'inventaire
        when(restTemplate.exchange(
            anyString(), 
            eq(HttpMethod.GET), 
            any(HttpEntity.class), 
            eq(PlayerDTO.class), 
            anyString()
        )).thenReturn(responseEntity);

        // 2. Simuler la création du monstre (Monster Service)
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn("mock-instance-id-123");

        // 3. Simuler l'appel PUT (liaison au joueur) - ne fait rien
        // restTemplate.put n'a pas besoin de "when" car c'est un void, 
        // mais on peut utiliser doNothing() si besoin.

        // 4. Mock du repository
        when(logRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void testInvocationProbabilities() throws Exception {
        int totalRuns = 1000; // Baissé à 1000 pour la rapidité du test unitaire
        Map<Integer, Integer> results = new HashMap<>();
        String dummyToken = "Bearer fake-token";

        for (int i = 0; i < totalRuns; i++) {
            // CORRECTION : Appel avec les 2 arguments (username, token)
            Map<String, Object> result = invocationService.performCompleteInvocation("testUser", dummyToken);
            
            // On récupère le templateId
            Integer templateId = (Integer) result.get("templateId");
            results.put(templateId, results.getOrDefault(templateId, 0) + 1);
        }

        System.out.println("--- Résultats de l'invocation (" + totalRuns + " tirages) ---");
        results.forEach((id, count) -> {
            double percentage = (count / (double) totalRuns) * 100;
            System.out.println("Monstre ID " + id + ": " + percentage + "%");
        });

        // Vérification sommaire : on s'assure qu'on a reçu des IDs valides
        assertTrue(results.size() > 0);
        
        // Exemple de vérification de l'ID 4 (si ton pool contient un ID 4 à 10%)
        if(results.containsKey(4)) {
            double rareRate = (results.get(4) / (double) totalRuns);
            assertTrue(rareRate > 0.05 && rareRate < 0.15, "Le taux du rare est anormal");
        }
    }
}