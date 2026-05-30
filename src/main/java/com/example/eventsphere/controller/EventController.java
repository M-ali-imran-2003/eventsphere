package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.LandingPage;
import com.example.eventsphere.entity.SubEvent;
import com.example.eventsphere.entity.TicketTier;
import com.example.eventsphere.service.CheckoutService;
import com.example.eventsphere.service.EventService;
import com.example.eventsphere.service.OrderService;
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
    private final OrderService orderService;

    public EventController(EventService eventService, OrderService orderService) {
        this.eventService = eventService;
        this.orderService = orderService;
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
    public ResponseEntity<String> createDraftEvent(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateEventRequest request) {

        eventService.createDraftEvent(organizationId, request);
        return ResponseEntity.ok("Event Draft Created");
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

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping("/add-ticket-tier/{eventId}")
    public ResponseEntity<TicketTier> addTicketTier(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateTicketTierRequest request) {

        TicketTier newTier = eventService.addTicketTier(eventId, request);
        return ResponseEntity.status(HttpStatus.OK).body(newTier);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("get-all-ticket-tiers/{eventId}")
    public ResponseEntity<List<TicketTier>> getAllTickets(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getTicketTiersByEventId(eventId));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/find-ticket-tier-by-id/{ticketId}")
    public ResponseEntity<TicketTier> getTicketById(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(eventService.getTicketTierById(ticketId));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PatchMapping("/update-ticket-tier/{eventId}/{ticketId}")
    public ResponseEntity<TicketTier> updateTicket(
            @PathVariable UUID eventId,
            @PathVariable UUID ticketId,
            @RequestBody UpdateTicketTierRequest request) {
        return ResponseEntity.ok(eventService.updateTicketTier(eventId, ticketId, request));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @DeleteMapping("/delete-ticket-tier/{eventId}/{ticketId}")
    public ResponseEntity<String> deleteTicket(@PathVariable UUID eventId, @PathVariable UUID ticketId) {
        eventService.deleteTicketTier(eventId, ticketId);
        return ResponseEntity.ok("Deleted ticket successfully");
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping(value = "/add-sub-event/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubEvent> addSubEvent(
            @PathVariable UUID eventId,
            @Valid @ModelAttribute CreateSubEventRequest request) {

        SubEvent newSubEvent = eventService.addSubEvent(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSubEvent);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("get-sub-events/{eventId}")
    public ResponseEntity<List<SubEvent>> getAllAgendaItems(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getSubEventsByEventId(eventId));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/get-sub-event-by-id/{subEventId}")
    public ResponseEntity<SubEvent> getAgendaItemById(@PathVariable UUID subEventId) {
        return ResponseEntity.ok(eventService.getSubEventById(subEventId));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PatchMapping(value = "/update-sub-event/{eventId}/{subEventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubEvent> updateAgendaItem(
            @PathVariable UUID eventId,
            @PathVariable UUID subEventId,
            @ModelAttribute UpdateSubEventRequest request) {
        return ResponseEntity.ok(eventService.updateSubEvent(eventId, subEventId, request));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @DeleteMapping("/delete-sub-event/{eventId}/{subEventId}")
    public ResponseEntity<String> deleteAgendaItem(@PathVariable UUID eventId, @PathVariable UUID subEventId) {
        eventService.deleteSubEvent(eventId, subEventId);
        return ResponseEntity.ok("Sub Event Deleted");
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/get-landing-page-settings/{eventId}")
    public ResponseEntity<LandingPage> getLandingPageSettings(@PathVariable UUID eventId) {
        LandingPage landingPage = eventService.getLandingPageByEventId(eventId);

        if (landingPage == null) {
            // Tells the frontend: "All good, but the user hasn't created a page yet."
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(landingPage);
    }

    /**
     * PATCH /api/events/{eventId}/landing-page
     * Used when the Organizer clicks "Save". Handles both the first creation and partial updates.
     */
    @PreAuthorize("hasRole('ORGANIZER')")
    @PatchMapping("/configure-landing-page/{eventId}")
    public ResponseEntity<LandingPage> configureLandingPageSettings(
            @PathVariable UUID eventId,
            @Valid @RequestBody ConfigureLandingPageRequest request) {

        LandingPage savedPage = eventService.configureLandingPage(eventId, request);
        return ResponseEntity.ok(savedPage);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping("/publish/{eventId}")
    public ResponseEntity<String> publishEvent(@PathVariable UUID eventId) {
        eventService.publishEvent(eventId);
        return ResponseEntity.ok("Event Published Successfully");
    }

    /**
     * POST /api/events/{eventId}/unpublish
     * Hides the event from the public and returns it to PENDING (Draft) status.
     */
    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping("/unpublish/{eventId}")
    public ResponseEntity<String> unpublishEvent(@PathVariable UUID eventId) {
        eventService.unpublishEvent(eventId);
        return ResponseEntity.ok("Event Unpublished Successfully");
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/orders/{eventId}")
    public ResponseEntity<List<OrderSummaryResponse>> getEventOrders(@PathVariable UUID eventId) {
        List<OrderSummaryResponse> orders = orderService.getEventOrders(eventId);
        return ResponseEntity.ok(orders);
    }
}
