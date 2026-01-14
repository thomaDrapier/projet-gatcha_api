package com.gatcha.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data // Génère getters, setters, toString, etc. via Lombok
@Document(collection = "users") // Indique que c'est stocké dans la collection "users" de Mongo
public class User {

    @Id
    private String id;
    private String username;
    private String password;
    private String token;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}