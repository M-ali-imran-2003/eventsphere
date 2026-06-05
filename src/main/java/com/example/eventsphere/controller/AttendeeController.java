package com.example.eventsphere.controller;

import com.example.eventsphere.dto.MyEventsResponse;
import com.example.eventsphere.dto.MyOrdersResponse;
import com.example.eventsphere.dto.MyTicketResponse;
import com.example.eventsphere.dto.TicketTransferRequest;
import com.example.eventsphere.service.EventService;
import com.example.eventsphere.service.OrderService;
import com.example.eventsphere.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendee")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ATTENDEE')")
public class AttendeeController {

    private final TicketService ticketService;
    private final OrderService orderService;
    private final EventService eventService;


    /**
     * Arsam will hit this endpoint when the dashboard loads.
     * The Principal object automatically contains the logged-in user's email
     * (provided by your Spring Security / JWT configuration).
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<List<MyTicketResponse>> getMyTickets() {

        List<MyTicketResponse> tickets = ticketService.getMyTickets();
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<MyOrdersResponse>> getMyOrders() {

        List<MyOrdersResponse> orders = orderService.getMyOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/my-events")
    public ResponseEntity<List<MyEventsResponse>> getMyEvents() {

        List<MyEventsResponse> events = eventService.getMyEvents();
        return ResponseEntity.ok(events);
    }

    @PostMapping("/tickets/transfer-ticket/{ticketId}")
    public ResponseEntity<String> transferTicket(
            @PathVariable UUID ticketId,
            @RequestBody @Valid TicketTransferRequest request) {

        String response = ticketService.transferTicket(ticketId, request);
        return ResponseEntity.ok(response);
    }
}