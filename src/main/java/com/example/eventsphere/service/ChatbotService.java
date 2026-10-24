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
import java.util.UUID;

@Service
public class ChatbotService {

    private final RestClient restClient;


    public ChatbotService(
            @Value("${chatbot.service.url}")
            String chatbotServiceUrl
    ) {

        this.restClient =
                RestClient.builder()
                        .baseUrl(chatbotServiceUrl)
                        .build();
    }


    // ===========================================
    // USER CHAT
    // ===========================================

    public ChatResponse chat(
            UUID eventId,
            ChatRequest request
    ) {

        Map<String, Object> pythonRequest =
                new HashMap<>();

        pythonRequest.put(
                "event_id",
                eventId.toString()
        );

        pythonRequest.put(
                "message",
                request.getMessage()
        );

        pythonRequest.put(
                "session_id",
                request.getSessionId()
        );


        ChatResponse response =
                restClient
                        .post()
                        .uri("/chat")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
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


    // ===========================================
    // RAG INDEXING
    // ===========================================

    public void indexEvent(
            UUID eventId,
            String eventSlug,
            PublicEventResponse eventData
    ) {

        Map<String, Object> request =
                new HashMap<>();

        request.put(
                "event_id",
                eventId.toString()
        );

        // Temporary compatibility only
        request.put(
                "event_slug",
                eventSlug
        );

        request.put(
                "event_data",
                eventData
        );


        restClient
                .post()
                .uri("/index-event")
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .body(request)
                .retrieve()
                .toBodilessEntity();

    }
}