package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.SubEvent;
import com.example.eventsphere.entity.TicketTier;
import com.example.eventsphere.service.EventService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @PreAuthorize("hasRole('ORGANIZER')")
    @PatchMapping(value = "update-event/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateEventDetails(
            @PathVariable UUID eventId,
            @Valid @ModelAttribute UpdateEventRequest request) {

        eventService.updateEventDetails(eventId, request);
        return ResponseEntity.ok("Event Updated Successfully");
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/get-org-events/{organizationId}")
    public ResponseEntity<List<EventDTO>> getEvents(
            @PathVariable UUID organizationId) {

        List<EventDTO> events = eventService.getCurrentOrganizationEvents(organizationId);
        return ResponseEntity.ok(events);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/get-org-event-by-id/{eventId}")
    public ResponseEntity<EventDTO> getOrgEventById(
            @PathVariable UUID eventId) {

        EventDTO event = eventService.findEvent(eventId);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/add-ticket/{eventId}")
    public ResponseEntity<TicketTier> addTicketTier(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateTicketTierRequest request) {

        TicketTier newTier = eventService.addTicketTier(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newTier);
    }

    /**
     * POST /api/events/{eventId}/agenda
     * Adds a new sub-event (e.g., "Keynote Speech") to the event's schedule.
     */
    @PostMapping(value = "/add-sub-event/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubEvent> addSubEvent(
            @PathVariable UUID eventId,
            @Valid @ModelAttribute CreateSubEventRequest request) {

        SubEvent newSubEvent = eventService.addSubEvent(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSubEvent);
    }
}
