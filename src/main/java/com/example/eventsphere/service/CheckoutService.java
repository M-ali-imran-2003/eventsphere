package com.example.eventsphere.service;

import com.example.eventsphere.dto.CheckoutRequest;
import com.example.eventsphere.entity.TicketTier;
import com.example.eventsphere.repository.*;
import com.example.eventsphere.service.PaymentProcessor;
import com.example.eventsphere.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    // The Mock Gateway we just built
    private final PaymentProcessor paymentProcessor;

    // Repositories mapped to your DDL tables
    private final OrderRepository orderRepository;
    private final TicketTierRepository tierRepository;
    private final AttendeeTicketRepository attendeeTicketRepository;
    private final SubEventRegistrationRepository subEventRegRepository;
    private final WalletTransactionRepository walletRepository;

    /**
     * @Transactional ensures that if any database save fails or throws an error,
     * the entire process rolls back. No half-saved orders!
     */
    @Transactional
    public String processCheckout(UUID eventId, CheckoutRequest request) {
        log.info("Starting checkout for user {} on event {}", request.getBuyerEmail(), eventId);

        // ==========================================
        // PHASE 1: PRE-CHECKS & MATH
        // ==========================================

        // 1. Calculate Total Amount & Check Capacity
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CheckoutRequest.TicketSelection selection : request.getTicketSelections()) {
            TicketTier tier = tierRepository.findById(selection.getTierId())
                    .orElseThrow(() -> new RuntimeException("Ticket Tier Not Found"));

            // Check if there are enough tickets available
            int availableTickets = tier.getTotalCapacity() - tier.getQuantitySold();
            if (availableTickets < selection.getQuantity()) {
                throw new RuntimeException("Not enough tickets available for tier: " + tier.getTierName());
            }

            // BigDecimal immutable addition and multiplication
            BigDecimal selectionQuantity = BigDecimal.valueOf(selection.getQuantity());
            BigDecimal selectionCost = tier.getPrice().multiply(selectionQuantity);
            totalAmount = totalAmount.add(selectionCost);
        }

        // 2. Apply Discount Code (If provided)
        if(request.getPromoCode() != null && !request.getPromoCode().isBlank()) {

            // TODO: Fetch discount code from DB. Validate it. Subtract from totalAmount.
        }

        // ==========================================
        // PHASE 2: TAKE THE MONEY
        // ==========================================

        PaymentResult payment = paymentProcessor.processPayment(totalAmount, null);

        if (!payment.isSuccessful()) {
            log.warn("Payment failed for {}. Reason: {}", request.getBuyerEmail(), payment.getErrorMessage());
            throw new RuntimeException("Checkout failed: " + payment.getErrorMessage());
        }

        // ==========================================
        // PHASE 3: FULFILLMENT (Database Writes)
        // ==========================================

        // 3. Create the Order
        // TODO: Insert row into `orders` table (buyerId, totalAmount, "PAID", payment.getTransactionId())

        // 4. Generate the Tickets
        for (CheckoutRequest.TicketSelection selection : request.getTicketSelections()) {
            for (int i = 0; i < selection.getQuantity(); i++) {

                // TODO: Insert row into `attendee_tickets` table
                // Generate a random UUID for the QR code hash
                // Set assigned_name to request.getBuyerFirstName() by default

                // 5. Reserve Sub-Events (If any were selected)
                if (request.getSelectedSubEventIds() != null) {
                    for (UUID subEventId : request.getSelectedSubEventIds()) {
                        // TODO: Insert row into `ticket_sub_event_registrations`
                    }
                }
            }
        }

        // 6. Update Organizer's Wallet
        // TODO: Insert row into `wallet_transactions` to credit the Organizer's workspace

        log.info("Checkout successful! Order ID generated.");

        // 7. Send the Email! (You can do this asynchronously later)

        return "Checkout complete! Transaction ID: " + payment.getTransactionId();
    }
}