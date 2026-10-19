package com.example.eventsphere.entity;

import com.example.eventsphere.enums.ChatJobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chatbot_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ChatbotJob {

    @Id
    private UUID id;

    @Column(name = "event_slug", nullable = false)
    private String eventSlug;

    @Column(name = "session_id")
    private String sessionId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatJobStatus status;

    @Column(columnDefinition = "text")
    private String answer;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}