package com.example.eventsphere.controller;

import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.dto.EventListDTO;
import com.example.eventsphere.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
