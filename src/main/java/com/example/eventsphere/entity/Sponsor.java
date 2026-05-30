package com.example.eventsphere.entity;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.SponsorTier;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sponsors")
@Data
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sponsor_id")
    private UUID id;

    // Links this sponsor to a specific event
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "sponsor_name")
    private String name;

    // e.g., "PLATINUM", "GOLD", "SILVER", "CO-HOST"
    @Enumerated(EnumType.STRING)
    @Column(name = "sponsor_tier")
    private SponsorTier sponsorTier;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private UUID modifiedBy;

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