package com.example.eventsphere.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CheckoutRequest {

    // --- 1. Buyer Information (Silent Registration Data) ---
    @NotBlank(message = "Name is required")
    private String buyerName;

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    private String buyerEmail;

    // NEW: Highly recommended for the Buyer!
    @NotBlank(message = "Phone number is required")
    private String buyerPhone;

    @NotBlank(message = "CNIC is required")
    private String buyerCnic;

    // --- 2. Ticket Selections ---
    @NotEmpty(message = "You must select at least one ticket")
    @Valid
    private List<TicketSelection> ticketSelections;

    // --- 3. Sub-Event RSVPs (Optional) ---
    // A list of IDs for the workshops/keynotes they want to attend
    private List<UUID> selectedSubEventIds;

    // --- 4. Discounts & Payments ---
    private String promoCode; // Can be null if no discount is applied

    // For a real gateway, we would need a Stripe PaymentMethodId here.
    // For our Mock MVP, we can leave the payment token out for now.
    // private String paymentToken;

    /**
     * Nested class to represent how many of each tier they are buying.
     * Example: 2 x General Admission, 1 x VIP.
     */
    @Data
    public static class TicketSelection {

        private UUID tierId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
    }
}