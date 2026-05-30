package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CheckoutResponse {
    private boolean success;
    private String message;
    private String orderReference;
    private BigDecimal totalPaid;
    private int totalTickets;
    private String receiptEmail;
}