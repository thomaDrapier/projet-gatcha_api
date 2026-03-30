package com.gatcha.invocations.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "invocation_logs")
public class InvocationLog {

    @Id
    private String transactionId; // UUID
    private String username;
    private int selectedTemplateId;
    
    // Le champ manquant
    private String monsterInstanceId; 
    
    private String status; // STARTED, MONSTER_CREATED, COMPLETED
    private LocalDateTime timestamp;
}