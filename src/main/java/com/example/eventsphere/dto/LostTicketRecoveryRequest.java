package com.example.eventsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LostTicketRecoveryRequest {
    @Email(message = "A valid email is required")
    @NotBlank(message = "Email cannot be blank")
    private String email;
}