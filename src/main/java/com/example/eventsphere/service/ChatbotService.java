package com.example.eventsphere.service;

import com.example.eventsphere.dto.ChatRequest;
import com.example.eventsphere.dto.ChatResponse;
import com.example.eventsphere.dto.PublicEventResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatbotService {

    private final EventService eventService;
    private final RestClient restClient;

    public ChatbotService(
            EventService eventService,
            @Value("${chatbot.service.url}") String chatbotServiceUrl
    ) {

        this.eventService = eventService;

        this.restClient = RestClient.builder()
                .baseUrl(chatbotServiceUrl)
                .build();
    }

    public ChatResponse chat(
            String slug,
            ChatRequest request
    ) {

        // Get real event information using your existing EventService
        PublicEventResponse eventData =
                eventService.getPublicEventDetails(slug);


        // Data that will be sent to Python
        Map<String, Object> pythonRequest = new HashMap<>();

        pythonRequest.put(
                "event_slug",
                slug
        );

        pythonRequest.put(
                "event_data",
                eventData
        );

        pythonRequest.put(
                "message",
                request.getMessage()
        );

        pythonRequest.put(
                "session_id",
                request.getSessionId()
        );


        // Send request to Python FastAPI
        ChatResponse response =
                restClient
                        .post()
                        .uri("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(pythonRequest)
                        .retrieve()
                        .body(ChatResponse.class);


        if (response == null) {
            throw new RuntimeException(
                    "Chatbot service returned an empty response."
            );
        }


        return response;
    }
}