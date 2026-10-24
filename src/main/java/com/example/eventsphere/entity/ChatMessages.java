package com.example.eventsphere.entity;

import com.example.eventsphere.enums.ChatRole;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessages {
    @Id
    @Column(
            name = "id"
    )
    private UUID id;


    @Column(
            name = "event_id",
            nullable = false
    )
    private UUID eventId;


    @Column(
            name = "session_id",
            nullable = false
    )
    private UUID sessionId;


    @Column(
            nullable = false,
            columnDefinition = "text",
            name = "content"
    )
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChatRole role;

    @Column(
            name = "created_at",
            nullable = false
    )
    private OffsetDateTime createdAt;
}
