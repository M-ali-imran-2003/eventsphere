package com.example.eventsphere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 500, message = "Message is too long (max 500 characters)")
    private String message;

    // The chatbot service stores history by UUID, so reject anything else here.
    @NotBlank(message = "session_id is required")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "session_id must be a UUID"
    )
    @JsonProperty("session_id")
    private String sessionId;
}
