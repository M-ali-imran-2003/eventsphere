package com.example.eventsphere.controller;

import com.example.eventsphere.dto.PublicEventCardDTO;
import com.example.eventsphere.dto.PublicEventResponse;
import com.example.eventsphere.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final EventService eventService;

    public PublicController(EventService eventService) {
        this.eventService = eventService;
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
}
