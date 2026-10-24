package com.example.eventsphere.dto;

import com.example.eventsphere.enums.ChatRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class ChatMessagesResponse {

    private String content;

    private ChatRole role;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
