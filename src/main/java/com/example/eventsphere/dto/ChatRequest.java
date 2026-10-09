package com.example.eventsphere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatRequest {

    private String message;

    @JsonProperty("session_id")
    private String sessionId;
}