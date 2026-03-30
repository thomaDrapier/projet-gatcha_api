package com.gatcha.invocations.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatcha.invocations.model.MonsterTemplate;
import com.gatcha.invocations.repository.MonsterTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MonsterTemplateRepository repository;
    private final ObjectMapper objectMapper;

    // Spring va chercher l'ObjectMapper défini dans JacksonConfig et l'injecter ici
    public DataInitializer(MonsterTemplateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        // On vérifie si la collection est vide avant d'importer [cite: 184]
        if (repository.count() == 0) {
            // Utilisation de getClass() pour garantir la lecture dans le dossier resources
            InputStream inputStream = getClass().getResourceAsStream("/monsters.json");
            
            if (inputStream == null) {
                System.err.println("ERREUR : Le fichier monsters.json est introuvable dans src/main/resources/");
                return;
            }

            try {
                List<MonsterTemplate> templates = objectMapper.readValue(inputStream, new TypeReference<List<MonsterTemplate>>(){});
                repository.saveAll(templates);
                System.out.println(">>> Base d'invocations initialisée avec " + templates.size() + " monstres.");
            } catch (Exception e) {
                System.err.println("Erreur lors de la lecture ou de l'enregistrement du JSON : " + e.getMessage());
            }
        } else {
            System.out.println(">>> La base d'invocations contient déjà des données (" + repository.count() + " monstres).");
        }
    }
}