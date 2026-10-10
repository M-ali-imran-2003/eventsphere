package com.example.eventsphere.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatResponse {

    private String answer;

    @JsonProperty("session_id")
    private String sessionId;
}