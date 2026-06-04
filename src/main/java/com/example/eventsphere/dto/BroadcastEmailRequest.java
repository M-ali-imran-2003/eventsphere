package com.example.eventsphere.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastEmailRequest {
    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Message body is required")
    private String messageBody;
}