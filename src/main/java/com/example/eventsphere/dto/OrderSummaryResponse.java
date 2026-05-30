package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderSummaryResponse {
    private UUID orderId;
    private String orderReference; // e.g., ORD-2026-X7B9
    private String buyerName;
    private String buyerEmail;
    private String buyerCnic;
    private String buyerPhone;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private LocalDateTime createdAt;
}