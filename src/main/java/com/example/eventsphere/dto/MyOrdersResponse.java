package com.example.eventsphere.dto;

import com.example.eventsphere.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MyOrdersResponse {
    private UUID orderId;
    private String orderReference;
    private String eventName;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}