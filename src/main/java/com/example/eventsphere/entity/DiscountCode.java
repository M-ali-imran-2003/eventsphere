package com.example.eventsphere.entity;

import com.example.eventsphere.enums.AppStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="discount_codes")
@Data
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "code_id")
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "code")
    private String code;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "max_uses")
    private int maxUses;

    @Column(name = "times_used")
    private int timesUsed = 0;

    @Column(name = "status")
    private AppStatus status;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

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
