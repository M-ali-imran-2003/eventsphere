package com.example.eventsphere.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "landing_pages")
@Data
public class LandingPage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "page_id")
    private UUID id;

    // 1-to-1 relationship strictly enforced at the database level
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    // The public URL path (e.g., "future-tech-26"). Must be globally unique!
    @Column(name = "slug_url", nullable = false, unique = true)
    private String slug;

    // Holds ALL visual choices: templates, colors, fonts, toggles.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb",name = "theme_config_json")
    private String themeConfigJson;

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