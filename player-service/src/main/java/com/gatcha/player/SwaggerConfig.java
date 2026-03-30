package com.gatcha.player; 

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 1. On définit le schéma de sécurité (Un header nommé "Authorization")
                .components(new Components()
                        .addSecuritySchemes("TokenAPI",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")))
                // 2. On l'applique à toutes les routes
                .addSecurityItem(new SecurityRequirement().addList("TokenAPI"));
    }
}