package com.gatcha.invocations.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Document(collection = "invocation_logs")
@Schema(description = "Base tampon pour tracer chaque étape de l'invocation")
public class InvocationLog {
    @Id
    private String transactionId;
    private String username;
    private int selectedTemplateId;
    private String generatedMonsterId; 
    
    @Schema(description = "Statuts possibles : STARTED, MONSTER_CREATED, COMPLETED")
    private String status;
    
    private LocalDateTime timestamp;
}