package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.service.CheckoutService;
import com.example.eventsphere.service.EventService;
import com.example.eventsphere.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final EventService eventService;
    private final CheckoutService checkoutService;
    private final TicketService ticketService;

    public PublicController(EventService eventService, CheckoutService checkoutService, TicketService ticketService) {
        this.eventService = eventService;
        this.checkoutService = checkoutService;
        this.ticketService = ticketService;
    }

    @GetMapping("/event/{slug}")
    public ResponseEntity<PublicEventResponse> getPublicLandingPage(@PathVariable String slug) {
        PublicEventResponse response = eventService.getPublicEventDetails(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-published-events")
    public ResponseEntity<List<PublicEventCardDTO>> getAllActiveEvents() {
        return ResponseEntity.ok(eventService.getExplorePageEvents());
    }

    @PostMapping("/checkout/{eventId}")
    public ResponseEntity<CheckoutResponse> processCheckout(
            @PathVariable UUID eventId,
            @Valid @RequestBody CheckoutRequest request) {

        // The @Transactional service handles the math, money, and database saves
        CheckoutResponse result = checkoutService.processCheckout(eventId, request);

        // Return a 200 OK with the transaction reference
        return ResponseEntity.ok(result);
    }

    @GetMapping("/discount-code/validate/{eventId}")
    public ResponseEntity<Map<String, Object>> validatePromoCode(
            @PathVariable UUID eventId,
            @RequestParam String code) {

        try {
            BigDecimal discountPercentage = eventService.validatePromoCodeForPublic(eventId, code);

            // Return it in a nice JSON object for the frontend
            return ResponseEntity.ok(Map.of(
                    "isValid", true,
                    "percentage", discountPercentage
            ));
        } catch (RuntimeException e) {
            // Return a 400 Bad Request if the code is expired, full, or invalid
            return ResponseEntity.badRequest().body(Map.of(
                    "isValid", false,
                    "message", e.getMessage()
            ));
        }
    }
    @PostMapping("/tickets/recover/{eventId}")
    public ResponseEntity<Map<String, String>> recoverLostTicket(
            @PathVariable UUID eventId,
            @Valid @RequestBody LostTicketRecoveryRequest request) {

        String message = ticketService.recoverLostTickets(eventId,request);
        return ResponseEntity.ok(Map.of("message", message));
    }
}
