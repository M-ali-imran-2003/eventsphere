package com.example.eventsphere.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PythonChatRequest {

    private String eventSlug;

    private PublicEventResponse eventData;

    private String message;

    private String sessionId;
}