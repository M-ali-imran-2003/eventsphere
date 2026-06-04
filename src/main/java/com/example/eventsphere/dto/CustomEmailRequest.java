package com.example.eventsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CustomEmailRequest {

    @NotEmpty(message = "At least one email recipient is required")
    private List<@Email(message = "Invalid email format") String> email;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Message body is required")
    private String messageBody;
}
