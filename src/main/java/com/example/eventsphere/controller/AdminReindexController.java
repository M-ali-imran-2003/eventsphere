package com.example.eventsphere.controller;

import com.example.eventsphere.event.RagIndexRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Manual re-index. SecurityConfig already requires ROLE_ADMIN for /api/admin/**.
 * It only QUEUES the work (the listener runs on the "indexExecutor" pool) and answers instantly.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReindexController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/event/{eventId}/reindex")
    public ResponseEntity<Map<String, String>> reindexEvent(@PathVariable UUID eventId) {
        eventPublisher.publishEvent(new RagIndexRequestedEvent(eventId));

        return ResponseEntity.accepted().body(Map.of(
                "status", "queued",
                "event_id", eventId.toString()
        ));
    }
}
