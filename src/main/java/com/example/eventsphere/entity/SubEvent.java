package com.example.eventsphere.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sub_events")
@Data
public class SubEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sub_event_id")
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String title; // e.g., "AI Keynote Speech", "Lunch Break"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "image_url")
    private String imageUrl; // Speaker's headshot or room map

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "room_or_location")
    private String roomOrLocation; // e.g., "Hall A", "Main Stage"

    @Column(name = "capacity_limit")
    private Integer capacityLimit; // Null means unlimited

    // Handles Postgres JSONB type for custom frontend rules
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_config")
    private String rulesConfig;

    // --- Audit Fields ---
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private UUID modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
}