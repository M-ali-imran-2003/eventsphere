package com.example.eventsphere.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_broadcast_history")
@Data
public class EmailBroadcastHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "email_id")
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "message_body")
    private String messageBody;

    @Column(name = "recipient_count")
    private int recipientCount;

    @Column(name = "sent_at")
    private LocalDateTime sentAt = LocalDateTime.now();
}