package com.example.eventsphere.controller;

import com.example.eventsphere.dto.CheckoutRequest;
import com.example.eventsphere.dto.PublicEventCardDTO;
import com.example.eventsphere.dto.PublicEventResponse;
import com.example.eventsphere.service.CheckoutService;
import com.example.eventsphere.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<String> processCheckout(
            @PathVariable UUID eventId,
            @Valid @RequestBody CheckoutRequest request) {

        // The @Transactional service handles the math, money, and database saves
        String result = checkoutService.processCheckout(eventId, request);

        // Return a 200 OK with the transaction reference
        return ResponseEntity.ok(result);
    }
}
