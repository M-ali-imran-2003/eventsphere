package com.example.eventsphere.service;
import com.example.eventsphere.dto.PaymentResult;

import java.math.BigDecimal;

public interface PaymentProcessor {

    /**
     * Processes a payment for the given amount.
     * * @param amount The total amount to charge.
     * @param paymentToken The token from the frontend (e.g., Stripe token). Null for mock.
     * @return PaymentResult indicating success or failure.
     */
    PaymentResult processPayment(BigDecimal amount, String paymentToken);
}