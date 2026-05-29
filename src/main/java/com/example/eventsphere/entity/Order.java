package com.example.eventsphere.entity;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID id;

    @NotBlank(message = "Buyer is required")
    @Column(name = "buyer_id")
    private UUID buyerId;

    @NotBlank(message = "Event is required")
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "order_ref")
    private String orderReference;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "gateway_id")
    private UUID gatewayId;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}