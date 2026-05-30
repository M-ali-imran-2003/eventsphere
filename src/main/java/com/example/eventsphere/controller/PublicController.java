package com.example.eventsphere.controller;

import com.example.eventsphere.dto.CheckoutRequest;
import com.example.eventsphere.dto.CheckoutResponse;
import com.example.eventsphere.dto.PublicEventCardDTO;
import com.example.eventsphere.dto.PublicEventResponse;
import com.example.eventsphere.service.CheckoutService;
import com.example.eventsphere.service.EventService;
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

    public PublicController(EventService eventService, CheckoutService checkoutService) {
        this.eventService = eventService;
        this.checkoutService = checkoutService;
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
}
