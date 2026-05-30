package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WalletTransactionDto {
    private UUID transactionId;
    private BigDecimal amount;
    private String transactionType; // CREDIT or DEBIT
    private String description;
    private LocalDateTime createdAt;
}