package com.gatcha.invocations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.gatcha.invocations.model.MonsterTemplate;
import com.gatcha.invocations.repository.InvocationLogRepository;
import com.gatcha.invocations.service.InvocationService;

@SpringBootTest
class InvocationServiceTest {

    @Autowired
    private InvocationService invocationService;

    // On "moque" le repository pour ne pas écrire réellement en base 10 000 fois
    @MockitoBean
    private InvocationLogRepository logRepository;

    @Test
    void testInvocationProbabilities() throws Exception {
        // On dit au mock de ne rien faire (ou de simuler une sauvegarde réussie)
        when(logRepository.save(any())).thenReturn(null);

        int totalRuns = 10000;
        Map<Integer, Integer> results = new HashMap<>();

        for (int i = 0; i < totalRuns; i++) {
            MonsterTemplate monster = invocationService.performCompleteInvocation("testUser");
            results.put(monster.getId(), results.getOrDefault(monster.getId(), 0) + 1);
        }

        System.out.println("--- Résultats de l'invocation (10 000 tirages) ---");
        results.forEach((id, count) -> {
            double percentage = (count / (double) totalRuns) * 100;
            System.out.println("Monstre ID " + id + " (" + count + " fois): " + percentage + "%");
        });

        // Vérification du monstre rare (ID 4 - 10%)
        double rareRate = (results.get(4) / (double) totalRuns);
        assertTrue(rareRate > 0.08 && rareRate < 0.12, 
            "Le taux du rare est de " + (rareRate * 100) + "%, il devrait être proche de 10%");
    }
}