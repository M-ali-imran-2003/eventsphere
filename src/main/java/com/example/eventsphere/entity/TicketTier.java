package com.example.eventsphere.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_tiers")
@Data
public class TicketTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tier_id")
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "tier_name", nullable = false)
    private String tierName; // e.g., "Early Bird", "VIP", "General Admission"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Using BigDecimal for precise financial math

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "quantity_sold")
    private Integer quantitySold = 0; // Defaults to 0 when created

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
        if (this.quantitySold == null) {
            this.quantitySold = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
}