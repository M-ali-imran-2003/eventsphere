package com.example.eventsphere.service;

import com.example.eventsphere.dto.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class MockPaymentServiceImpl implements PaymentProcessor {

    @Override
    public PaymentResult processPayment(BigDecimal amount, String paymentToken) {
        log.info("Initiating mock payment processing for amount: Rs. {}", amount);

        try {
            // Simulate network delay to a banking API (1.5 seconds)
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Mock payment interrupted");
        }

        // Simulate a 95% success rate. (Great for testing frontend error handling!)
        // If the amount is exactly 0.00, it's a free ticket, auto-approve.
        if (amount.compareTo(BigDecimal.ZERO) == 0 || Math.random() > 0.05) {
            String fakeTransactionId = "MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("Mock payment SUCCESSFUL. Txn ID: {}", fakeTransactionId);

            return PaymentResult.builder()
                    .isSuccessful(true)
                    .transactionId(fakeTransactionId)
                    .build();
        } else {
            log.warn("Mock payment DECLINED (Simulated Bank Rejection).");

            return PaymentResult.builder()
                    .isSuccessful(false)
                    .errorMessage("Card declined by the issuing bank. Please try another card.")
                    .build();
        }
    }
}