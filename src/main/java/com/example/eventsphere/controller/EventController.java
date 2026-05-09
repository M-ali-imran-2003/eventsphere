package com.example.eventsphere.controller;

import com.example.eventsphere.dto.CreateEventRequest;
import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventListDTO;
import com.example.eventsphere.dto.EventMapMarkerDTO;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.service.EventService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/event")
@Slf4j
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-events")
    public ResponseEntity<List<EventListDTO>> getAllEvents() {
        List<EventListDTO> events = eventService.getAllEventsForAdmin(); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(events);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-event-by-id/{id}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable UUID id) {
        EventDTO event = eventService.getEventByIdForAdmin(id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(event);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-events-location-map")
    public ResponseEntity<List<EventMapMarkerDTO>> getAllEventsLocationMap() {
        List<EventMapMarkerDTO> events = eventService.getAllEventsLocation(); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(events);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping("/create-draft/{organizationId}")
    public ResponseEntity<Event> createDraftEvent(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateEventRequest request) {

        Event draftEvent = eventService.createDraftEvent(organizationId, request);
        return ResponseEntity.ok(draftEvent);
    }
}
