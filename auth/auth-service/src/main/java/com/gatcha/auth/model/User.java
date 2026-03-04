package com.gatcha.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data // Génère getters, setters, toString, etc. via Lombok
@Document(collection = "users") // Indique que c'est stocké dans la collection "users" de Mongo
public class User {

    @Id
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    private String username;

    private String password;
    
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String token;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String token_clear;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setToken_clear(String token_clear) {
        this.token_clear = token_clear;
    }
}