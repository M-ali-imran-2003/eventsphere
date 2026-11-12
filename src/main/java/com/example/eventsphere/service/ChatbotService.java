////package com.example.eventsphere.service;
////
////import com.example.eventsphere.dto.ChatMessagesResponse;
////import com.example.eventsphere.dto.ChatRequest;
////import com.example.eventsphere.dto.ChatResponse;
////import com.example.eventsphere.dto.PublicEventResponse;
////import com.example.eventsphere.repository.ChatMessagesRepository;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.http.MediaType;
////import org.springframework.stereotype.Service;
////import org.springframework.web.client.RestClient;
////
////import java.util.HashMap;
////import java.util.List;
////import java.util.Map;
////import java.util.UUID;
////
////@Service
////public class ChatbotService {
////
////    private final RestClient restClient;
////    private final ChatMessagesRepository chatMessagesRepository;
////
////
////    @Autowired
////    public ChatbotService(
////            @Value("${chatbot.service.url}")
////            String chatbotServiceUrl, ChatMessagesRepository chatMessagesRepository
////    ) {
////
////        this.restClient =
////                RestClient.builder()
////                        .baseUrl(chatbotServiceUrl)
////                        .build();
////        this.chatMessagesRepository = chatMessagesRepository;
////    }
////
////    public List<ChatMessagesResponse> getChatHistory(UUID sessionId) {
////
////        return chatMessagesRepository.findBySessionId(sessionId).stream()
////                .map(message -> new ChatMessagesResponse(
////                        message.getContent(),
////                        message.getRole(),
////                        message.getCreatedAt()
////                ))
////                .toList();
////    }
////
////
////    // ===========================================
////    // USER CHAT
////    // ===========================================
////
////    public ChatResponse chat(
////            UUID eventId,
////            ChatRequest request
////    ) {
////
////        Map<String, Object> pythonRequest =
////                new HashMap<>();
////
////        pythonRequest.put(
////                "event_id",
////                eventId.toString()
////        );
////
////        pythonRequest.put(
////                "message",
////                request.getMessage()
////        );
////
////        pythonRequest.put(
////                "session_id",
////                request.getSessionId()
////        );
////
////
////        ChatResponse response =
////                restClient
////                        .post()
////                        .uri("/chat")
////                        .contentType(
////                                MediaType.APPLICATION_JSON
////                        )
////                        .body(pythonRequest)
////                        .retrieve()
////                        .body(ChatResponse.class);
////
////
////        if (response == null) {
////
////            throw new RuntimeException(
////                    "Chatbot service returned an empty response."
////            );
////        }
////
////
////        return response;
////    }
////
////
////    // ===========================================
////    // RAG INDEXING
////    // ===========================================
////
////    public void indexEvent(
////            UUID eventId,
////            String eventSlug,
////            PublicEventResponse eventData
////    ) {
////
////        Map<String, Object> request =
////                new HashMap<>();
////
////        request.put(
////                "event_id",
////                eventId.toString()
////        );
////
////        // Temporary compatibility only
////        request.put(
////                "event_slug",
////                eventSlug
////        );
////
////        request.put(
////                "event_data",
////                eventData
////        );
////
////
////        restClient
////                .post()
////                .uri("/index-event")
////                .contentType(
////                        MediaType.APPLICATION_JSON
////                )
////                .body(request)
////                .retrieve()
////                .toBodilessEntity();
////
////    }
////}
//
//
//package com.example.eventsphere.service;
//
//import com.example.eventsphere.dto.ChatMessagesResponse;
//import com.example.eventsphere.dto.ChatRequest;
//import com.example.eventsphere.dto.ChatResponse;
//import com.example.eventsphere.dto.PublicEventResponse;
//import com.example.eventsphere.repository.ChatMessagesRepository;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.http.client.SimpleClientHttpRequestFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestClient;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.function.Consumer;
//import java.util.stream.Stream;
//
///**
// * Talks to the Python chatbot service.
// *
// * Changes:
// *  - connect/read TIMEOUTS (the old RestClient could wait forever and block a thread)
// *  - X-Internal-Key header, so only this backend can call the Python service
// *  - streamChat(): reads Server-Sent Events from Python and hands each one to the caller
// *  - history is returned in the correct order
// */
//@Slf4j
//@Service
//public class ChatbotService {
//
//    private static final String API_KEY_HEADER = "X-Internal-Key";
//
//    private final RestClient chatClient;    // normal chat call: 30 s
//    private final RestClient indexClient;   // indexing embeds many chunks: 90 s
//    private final HttpClient streamClient;  // streaming (kept open while words arrive)
//    private final String baseUrl;
//    private final String apiKey;
//    private final ObjectMapper objectMapper;
//    private final ChatMessagesRepository chatMessagesRepository;
//
//    public ChatbotService(
//            @Value("${chatbot.service.url}") String chatbotServiceUrl,
//            @Value("${chatbot.service.api-key:}") String apiKey,
//            ChatMessagesRepository chatMessagesRepository,
//            ObjectMapper objectMapper
//    ) {
//        this.baseUrl = chatbotServiceUrl.endsWith("/")
//                ? chatbotServiceUrl.substring(0, chatbotServiceUrl.length() - 1)
//                : chatbotServiceUrl;
//        this.apiKey = apiKey == null ? "" : apiKey;
//        this.chatMessagesRepository = chatMessagesRepository;
//        this.objectMapper = objectMapper;
//
//        this.chatClient = buildClient(3_000, 30_000);
//        this.indexClient = buildClient(3_000, 90_000);
//
//        this.streamClient = HttpClient.newBuilder()
//                .version(HttpClient.Version.HTTP_1_1)   // uvicorn speaks HTTP/1.1
//                .connectTimeout(Duration.ofSeconds(3))
//                .build();
//    }
//
//    private RestClient buildClient(int connectTimeoutMs, int readTimeoutMs) {
//        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
//        factory.setConnectTimeout(connectTimeoutMs);
//        factory.setReadTimeout(readTimeoutMs);
//
//        RestClient.Builder builder = RestClient.builder()
//                .baseUrl(baseUrl)
//                .requestFactory(factory);
//
//        if (!apiKey.isBlank()) {
//            builder.defaultHeader(API_KEY_HEADER, apiKey);
//        }
//        return builder.build();
//    }
//
//    // ===========================================
//    // HISTORY
//    // ===========================================
//
////    public List<ChatResponse> getChatHistory(UUID sessionId) {
////        return chatMessagesRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
////                .map(m -> new ChatResponse(m.getContent(), m.getRole(), m.getCreatedAt()))
////                .toList();
////    }
//
//    public List<ChatMessagesResponse> getChatHistory(UUID sessionId) {
//
//        return chatMessagesRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
//                .map(message -> new ChatMessagesResponse(
//                        message.getContent(),
//                        message.getRole(),
//                        message.getCreatedAt()
//                ))
//                .toList();
//    }
//
//
//
//
//
//
//    // ===========================================
//    // CHAT - full answer (legacy / fallback path)
//    // ===========================================
//
//    public ChatResponse chat(UUID eventId, ChatRequest request) {
//
//        Map<String, Object> pythonRequest = new HashMap<>();
//        pythonRequest.put("event_id", eventId.toString());
//        pythonRequest.put("message", request.getMessage());
//        pythonRequest.put("session_id", request.getSessionId());
//
//        ChatResponse response = chatClient
//                .post()
//                .uri("/chat")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(pythonRequest)
//                .retrieve()
//                .body(ChatResponse.class);
//
//        if (response == null) {
//            throw new RuntimeException("Chatbot service returned an empty response.");
//        }
//        return response;
//    }
//
//    // ===========================================
//    // CHAT - streaming
//    //
//    // Python sends lines like:   data: {"t":"Hello"}
//    // Every payload (the part after "data:") is passed to onData as-is.
//    // Whatever onData throws (for example "visitor closed the page") stops the stream.
//    // ===========================================
//
//    public void streamChat(UUID eventId, ChatRequest request, Consumer<String> onData) {
//
//        String body;
//        try {
//            body = objectMapper.writeValueAsString(Map.of(
//                    "event_id", eventId.toString(),
//                    "message", request.getMessage(),
//                    "session_id", request.getSessionId()
//            ));
//        } catch (JsonProcessingException e) {
//            throw new IllegalStateException("Could not build chatbot request", e);
//        }
//
//        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/stream"))
//                .timeout(Duration.ofSeconds(45))
//                .header("Content-Type", "application/json")
//                .header("Accept", "text/event-stream")
//                .POST(HttpRequest.BodyPublishers.ofString(body));
//
//        if (!apiKey.isBlank()) {
//            builder.header(API_KEY_HEADER, apiKey);
//        }
//
//        try {
//            HttpResponse<Stream<String>> response =
//                    streamClient.send(builder.build(), HttpResponse.BodyHandlers.ofLines());
//
//            if (response.statusCode() != 200) {
//                response.body().close();
//                throw new IllegalStateException("Chatbot stream returned HTTP " + response.statusCode());
//            }
//
//            try (Stream<String> lines = response.body()) {
//                lines.forEach(line -> {
//                    if (line.startsWith("data:")) {
//                        onData.accept(line.substring(5).trim());
//                    }
//                });
//            }
//
//        } catch (IOException e) {
//            throw new IllegalStateException("Chatbot service unreachable", e);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new IllegalStateException("Chatbot stream interrupted", e);
//        }
//    }
//
//    // ===========================================
//    // RAG INDEXING
//    // ===========================================
//
//    public void indexEvent(UUID eventId, String eventSlug, PublicEventResponse eventData) {
//
//        Map<String, Object> request = new HashMap<>();
//        request.put("event_id", eventId.toString());
//        request.put("event_slug", eventSlug);   // Python uses it for the old event_slug column
//        request.put("event_data", eventData);
//
//        indexClient
//                .post()
//                .uri("/index-event")
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(request)
//                .retrieve()
//                .toBodilessEntity();
//    }
//}

















package com.example.eventsphere.service;

import com.example.eventsphere.dto.ChatMessagesResponse;
import com.example.eventsphere.dto.ChatRequest;
import com.example.eventsphere.dto.ChatResponse;
import com.example.eventsphere.dto.PublicEventResponse;
import com.example.eventsphere.repository.ChatMessagesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Talks to the Python chatbot service.
 *
 * Changes:
 *  - connect/read TIMEOUTS (the old RestClient could wait forever and block a thread)
 *  - X-Internal-Key header, so only this backend can call the Python service
 *  - streamChat(): reads Server-Sent Events from Python and hands each one to the caller
 *  - history is returned in the correct order
 */
@Slf4j
@Service
public class ChatbotService {

    private static final String API_KEY_HEADER = "X-Internal-Key";

    private final RestClient chatClient;    // normal chat call: 30 s
    private final RestClient indexClient;   // indexing embeds many chunks: 90 s
    private final HttpClient streamClient;  // streaming (kept open while words arrive)
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final ChatMessagesRepository chatMessagesRepository;

    public ChatbotService(
            @Value("${chatbot.service.url}") String chatbotServiceUrl,
            @Value("${chatbot.service.api-key:}") String apiKey,
            ChatMessagesRepository chatMessagesRepository,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = chatbotServiceUrl.endsWith("/")
                ? chatbotServiceUrl.substring(0, chatbotServiceUrl.length() - 1)
                : chatbotServiceUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.chatMessagesRepository = chatMessagesRepository;
        this.objectMapper = objectMapper;

        this.chatClient = buildClient(3_000, 30_000);
        this.indexClient = buildClient(3_000, 90_000);

        this.streamClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)   // uvicorn speaks HTTP/1.1
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    private RestClient buildClient(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);

        if (!apiKey.isBlank()) {
            builder.defaultHeader(API_KEY_HEADER, apiKey);
        }
        return builder.build();
    }

    // ===========================================
    // HISTORY
    // ===========================================

    public List<ChatMessagesResponse> getChatHistory(UUID sessionId) {
        return chatMessagesRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> new ChatMessagesResponse(m.getContent(), m.getRole(), m.getCreatedAt()))
                .toList();
    }

    // ===========================================
    // CHAT - full answer (legacy / fallback path)
    // ===========================================

    public ChatResponse chat(UUID eventId, ChatRequest request) {

        Map<String, Object> pythonRequest = new HashMap<>();
        pythonRequest.put("event_id", eventId.toString());
        pythonRequest.put("message", request.getMessage());
        pythonRequest.put("session_id", request.getSessionId());

        ChatResponse response = chatClient
                .post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(pythonRequest)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null) {
            throw new RuntimeException("Chatbot service returned an empty response.");
        }
        return response;
    }

    // ===========================================
    // CHAT - streaming
    //
    // Python sends lines like:   data: {"t":"Hello"}
    // Every payload (the part after "data:") is passed to onData as-is.
    // Whatever onData throws (for example "visitor closed the page") stops the stream.
    // ===========================================

    public void streamChat(UUID eventId, ChatRequest request, Consumer<String> onData) {

        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "event_id", eventId.toString(),
                    "message", request.getMessage(),
                    "session_id", request.getSessionId()
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not build chatbot request", e);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/stream"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (!apiKey.isBlank()) {
            builder.header(API_KEY_HEADER, apiKey);
        }

        try {
            HttpResponse<Stream<String>> response =
                    streamClient.send(builder.build(), HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                response.body().close();
                throw new IllegalStateException("Chatbot stream returned HTTP " + response.statusCode());
            }

            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> {
                    if (line.startsWith("data:")) {
                        onData.accept(line.substring(5).trim());
                    }
                });
            }

        } catch (IOException e) {
            throw new IllegalStateException("Chatbot service unreachable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chatbot stream interrupted", e);
        }
    }

    // ===========================================
    // RAG INDEXING
    // ===========================================

    public void indexEvent(UUID eventId, String eventSlug, PublicEventResponse eventData) {

        Map<String, Object> request = new HashMap<>();
        request.put("event_id", eventId.toString());
        request.put("event_slug", eventSlug);   // Python uses it for the old event_slug column
        request.put("event_data", eventData);

        indexClient
                .post()
                .uri("/index-event")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
