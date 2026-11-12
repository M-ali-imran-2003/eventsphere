package com.example.eventsphere.service;

import com.example.eventsphere.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Connects the visitor's browser to the Python stream:
 *
 *   browser  <--SSE--  this class  <--SSE--  Python  <--stream--  Gemini
 *
 * The first words reach the visitor as soon as Gemini produces them, instead of
 * after the whole answer is finished (and instead of waiting for a polling request).
 */
@Slf4j
@Service
public class ChatStreamService {

    private static final long STREAM_TIMEOUT_MS = 60_000L;
    private static final String ERROR_PAYLOAD =
            "{\"error\":\"The assistant is unavailable right now. Please try again.\"}";
    private static final String BUSY_PAYLOAD =
            "{\"error\":\"The assistant is busy. Please try again in a moment.\"}";

    private final ChatbotService chatbotService;
    private final Executor chatExecutor;

    public ChatStreamService(
            ChatbotService chatbotService,
            @Qualifier("chatExecutor") Executor chatExecutor
    ) {
        this.chatbotService = chatbotService;
        this.chatExecutor = chatExecutor;
    }

    public SseEmitter stream(UUID eventId, ChatRequest request) {

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        try {
            chatExecutor.execute(() -> run(emitter, eventId, request));
        } catch (TaskRejectedException e) {
            log.warn("Chat executor is full - rejecting request");
            sendQuietly(emitter, BUSY_PAYLOAD);
            completeQuietly(emitter);
        }

        return emitter;
    }

    private void run(SseEmitter emitter, UUID eventId, ChatRequest request) {
        try {
            chatbotService.streamChat(eventId, request, payload -> {
                try {
                    emitter.send(SseEmitter.event().data(payload));
                } catch (IOException e) {
                    throw new ClientGoneException(e);
                }
            });
            completeQuietly(emitter);

        } catch (ClientGoneException e) {
            log.debug("Visitor closed the chat stream");
            completeQuietly(emitter);

        } catch (Exception e) {
            log.error("Chat stream failed for event {}", eventId, e);
            sendQuietly(emitter, ERROR_PAYLOAD);
            completeQuietly(emitter);
        }
    }

    private void sendQuietly(SseEmitter emitter, String payload) {
        try {
            emitter.send(SseEmitter.event().data(payload));
        } catch (Exception ignored) {
            // visitor already gone
        }
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // already completed
        }
    }

    /** Thrown when we cannot write to the visitor any more (they closed the tab). */
    private static class ClientGoneException extends RuntimeException {
        ClientGoneException(Throwable cause) {
            super(cause);
        }
    }
}
