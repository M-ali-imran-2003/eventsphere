package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResult {
    private boolean isSuccessful;
    private String transactionId;
    private String errorMessage;
}